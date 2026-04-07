package com.example.rag.service.validation;

import com.example.rag.model.RetrievalResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 引用验证器
 * 
 * <p>验证答案中的引用是否准确、可信。</p>
 * <p>功能特点：</p>
 * <ul>
 *   <li>验证引用来源是否存在</li>
 *   <li>检查引用是否准确</li>
 *   <li>生成引用可信度评分</li>
 *   <li>标注引用位置</li>
 * </ul>
 */
@Slf4j
@Component
public class CitationValidator {
    
    // 引用模式：匹配各种引用格式
    private static final Pattern CITATION_PATTERN = Pattern.compile(
            // [1], [2], (1), (2) 等
            "\\[\\d+\\]|\\(\\d+\\)|" +
            // [来源], [文档], [参考] 等
            "\\[([^\\]]+)\\]|" +
            // 根据XX, 据XX报告, 参考XX 等
            "(根据|据|参考|引用|引自|来源)[:：]?\\s*([^，。！？\\n]+)|" +
            // 文献格式：(Author, 2020), (Author et al., 2020)
            "\\([A-Z][a-z]+(?:\\s+et\\s+al.)?,?\\s*\\d{4}[a-z]?\\)",
            Pattern.CASE_INSENSITIVE
    );
    
    // 链接模式（简化版，匹配URL直到空白字符）
    private static final Pattern URL_PATTERN = Pattern.compile(
            "https?://[^\\s]+",
            Pattern.CASE_INSENSITIVE
    );
    
    // 来源可信度权重
    private static final Map<String, Double> SOURCE_CREDIBILITY = new HashMap<String, Double>() {{
        put("官方", 1.0);
        put("政府", 1.0);
        put("学术", 0.95);
        put("研究", 0.95);
        put("报告", 0.9);
        put("新闻", 0.8);
        put("博客", 0.6);
        put("论坛", 0.5);
        put("未知", 0.3);
    }};
    
    /**
     * 验证答案中的引用
     * 
     * @param answer 生成的答案
     * @param retrievalResults 检索结果列表
     * @return 引用验证结果
     */
    public CitationValidationResult validate(String answer, List<RetrievalResult> retrievalResults) {
        if (answer == null || answer.isEmpty()) {
            return CitationValidationResult.empty("答案为空");
        }
        
        long startTime = System.currentTimeMillis();
        
        // 1. 提取答案中的所有引用
        List<Citation> citations = extractCitations(answer);
        log.debug("提取到 {} 个引用", citations.size());
        
        // 2. 验证每个引用
        List<CitationVerification> verifications = new ArrayList<>();
        for (Citation citation : citations) {
            CitationVerification verification = verifyCitation(citation, retrievalResults);
            verifications.add(verification);
        }
        
        // 3. 检查是否有未引用的重要信息
        List<UnreferencedClaim> unreferencedClaims = checkUnreferencedClaims(answer, retrievalResults);
        
        // 4. 计算引用评分
        double citationScore = calculateCitationScore(citations, verifications, retrievalResults);
        
        // 5. 生成引用标注
        String annotatedAnswer = annotateCitations(answer, verifications);
        
        long duration = System.currentTimeMillis() - startTime;
        
        log.info("引用验证完成: 引用数={}, 有效引用={}, 评分={}, 耗时={}ms",
                citations.size(), 
                verifications.stream().filter(CitationVerification::isValid).count(),
                citationScore, 
                duration);
        
        return new CitationValidationResult(
                citations,
                verifications,
                unreferencedClaims,
                citationScore,
                annotatedAnswer,
                duration
        );
    }
    
    /**
     * 提取答案中的引用
     */
    private List<Citation> extractCitations(String answer) {
        List<Citation> citations = new ArrayList<>();
        
        // 提取数字引用
        Matcher numberMatcher = Pattern.compile("\\[(\\d+)\\]|\\((\\d+)\\)").matcher(answer);
        while (numberMatcher.find()) {
            String num = numberMatcher.group(1) != null ? numberMatcher.group(1) : numberMatcher.group(2);
            citations.add(new Citation(
                    numberMatcher.group(),
                    CitationType.NUMBERED,
                    Integer.parseInt(num),
                    numberMatcher.start(),
                    numberMatcher.end()
            ));
        }
        
        // 提取文本引用
        Matcher textMatcher = Pattern.compile("\\[([^\\]]+)\\]").matcher(answer);
        while (textMatcher.find()) {
            String text = textMatcher.group(1);
            // 排除已经处理的数字引用
            if (!text.matches("\\d+")) {
                citations.add(new Citation(
                        textMatcher.group(),
                        CitationType.TEXT,
                        -1,
                        textMatcher.start(),
                        textMatcher.end()
                ));
            }
        }
        
        // 提取来源引用
        Matcher sourceMatcher = Pattern.compile(
                "(根据|据|参考|引用|引自|来源)[:：]?\\s*([^，。！？\\n]+)"
        ).matcher(answer);
        while (sourceMatcher.find()) {
            citations.add(new Citation(
                    sourceMatcher.group(2).trim(),
                    CitationType.SOURCE,
                    -1,
                    sourceMatcher.start(),
                    sourceMatcher.end()
            ));
        }
        
        // 提取链接
        Matcher urlMatcher = URL_PATTERN.matcher(answer);
        while (urlMatcher.find()) {
            citations.add(new Citation(
                    urlMatcher.group(),
                    CitationType.URL,
                    -1,
                    urlMatcher.start(),
                    urlMatcher.end()
            ));
        }
        
        // 按位置排序
        citations.sort(Comparator.comparingInt(Citation::getStartPosition));
        
        return citations;
    }
    
    /**
     * 验证单个引用
     */
    private CitationVerification verifyCitation(Citation citation, List<RetrievalResult> results) {
        boolean isValid = false;
        double credibilityScore = 0;
        RetrievalResult matchedResult = null;
        String matchDetails = "";
        
        switch (citation.getType()) {
            case NUMBERED:
                // 数字引用：检查是否有对应的检索结果
                int index = citation.getNumber() - 1;
                if (index >= 0 && index < results.size()) {
                    isValid = true;
                    matchedResult = results.get(index);
                    credibilityScore = calculateSourceCredibility(matchedResult);
                    matchDetails = String.format("匹配检索结果 #%d: %s", 
                            citation.getNumber(), 
                            matchedResult.getMetadata().getOrDefault("source", "未知来源"));
                } else {
                    matchDetails = String.format("未找到对应的检索结果 #%d", citation.getNumber());
                }
                break;
                
            case TEXT:
                // 文本引用：在检索结果中搜索匹配
                for (RetrievalResult result : results) {
                    if (matchesSource(citation.getText(), result)) {
                        isValid = true;
                        matchedResult = result;
                        credibilityScore = calculateSourceCredibility(result);
                        matchDetails = String.format("匹配来源: %s", 
                                result.getMetadata().getOrDefault("source", "未知来源"));
                        break;
                    }
                }
                if (!isValid) {
                    matchDetails = String.format("未找到匹配的来源: %s", citation.getText());
                }
                break;
                
            case SOURCE:
                // 来源引用：直接匹配
                for (RetrievalResult result : results) {
                    if (matchesSource(citation.getText(), result)) {
                        isValid = true;
                        matchedResult = result;
                        credibilityScore = calculateSourceCredibility(result);
                        matchDetails = String.format("引用来源: %s", citation.getText());
                        break;
                    }
                }
                break;
                
            case URL:
                // URL引用：检查是否在检索结果中
                for (RetrievalResult result : results) {
                    String resultUrl = result.getMetadata().getOrDefault("url", "").toString();
                    if (citation.getText().equalsIgnoreCase(resultUrl)) {
                        isValid = true;
                        matchedResult = result;
                        credibilityScore = 0.9; // URL引用通常可信度较高
                        matchDetails = String.format("链接匹配: %s", citation.getText());
                        break;
                    }
                }
                if (!isValid) {
                    // 外部链接，给予中等可信度
                    isValid = true;
                    credibilityScore = 0.7;
                    matchDetails = String.format("外部链接: %s", citation.getText());
                }
                break;
        }
        
        return new CitationVerification(
                citation,
                isValid,
                credibilityScore,
                matchedResult != null ? matchedResult.getDocumentId() : null,
                matchedResult != null ? matchedResult.getMetadata() : null,
                matchDetails
        );
    }
    
    /**
     * 检查是否匹配来源
     */
    private boolean matchesSource(String citationText, RetrievalResult result) {
        String source = result.getMetadata().getOrDefault("source", "").toString().toLowerCase();
        String title = result.getMetadata().getOrDefault("title", "").toString().toLowerCase();
        String citation = citationText.toLowerCase();
        
        return source.contains(citation) || citation.contains(source) ||
               title.contains(citation) || citation.contains(title);
    }
    
    /**
     * 计算来源可信度
     */
    private double calculateSourceCredibility(RetrievalResult result) {
        String source = result.getMetadata().getOrDefault("source", "未知").toString().toLowerCase();
        
        // 检查来源类型
        for (Map.Entry<String, Double> entry : SOURCE_CREDIBILITY.entrySet()) {
            if (source.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        
        // 检查是否有时间戳（较新的信息更可信）
        if (result.getMetadata().containsKey("timestamp")) {
            return 0.8;
        }
        
        // 检查是否有作者信息
        if (result.getMetadata().containsKey("author")) {
            return 0.75;
        }
        
        return SOURCE_CREDIBILITY.getOrDefault("未知", 0.3);
    }
    
    /**
     * 检查未引用的重要信息
     */
    private List<UnreferencedClaim> checkUnreferencedClaims(String answer, List<RetrievalResult> results) {
        List<UnreferencedClaim> unreferencedClaims = new ArrayList<>();
        
        // 提取答案中的句子
        String[] sentences = answer.split("[。.!?！？\\n]+");
        
        for (String sentence : sentences) {
            if (sentence.trim().isEmpty()) {
                continue;
            }
            
            // 检查是否包含重要信息（数字、日期、专业术语等）
            if (containsImportantInfo(sentence)) {
                // 检查是否有引用
                boolean hasCitation = CITATION_PATTERN.matcher(sentence).find() ||
                                     URL_PATTERN.matcher(sentence).find();
                
                if (!hasCitation) {
                    // 尝试在检索结果中找到支持
                    RetrievalResult supportingDoc = findSupportingDocument(sentence, results);
                    
                    unreferencedClaims.add(new UnreferencedClaim(
                            sentence.trim(),
                            supportingDoc != null,
                            supportingDoc != null ? supportingDoc.getDocumentId() : null,
                            supportingDoc != null ? supportingDoc.getMetadata() : null
                    ));
                }
            }
        }
        
        return unreferencedClaims;
    }
    
    /**
     * 检查是否包含重要信息
     */
    private boolean containsImportantInfo(String text) {
        // 包含数字
        if (text.matches(".*\\d+.*")) {
            return true;
        }
        
        // 包含日期
        if (text.matches(".*\\d{4}[-/年]\\d{1,2}.*")) {
            return true;
        }
        
        // 包含专业术语（大写缩写）
        if (text.matches(".*[A-Z]{2,}.*")) {
            return true;
        }
        
        // 包含断言性词汇
        String[] assertionWords = {"是", "为", "包括", "表示", "显示", "证明", "说明"};
        for (String word : assertionWords) {
            if (text.contains(word)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 查找支持文档
     */
    private RetrievalResult findSupportingDocument(String sentence, List<RetrievalResult> results) {
        String lowerSentence = sentence.toLowerCase();
        
        for (RetrievalResult result : results) {
            String content = result.getContent().toLowerCase();
            
            // 简单的关键词匹配
            String[] keywords = lowerSentence.split("\\s+");
            int matchCount = 0;
            
            for (String keyword : keywords) {
                if (keyword.length() > 2 && content.contains(keyword)) {
                    matchCount++;
                }
            }
            
            if (matchCount > keywords.length * 0.5) {
                return result;
            }
        }
        
        return null;
    }
    
    /**
     * 计算引用评分
     */
    private double calculateCitationScore(
            List<Citation> citations,
            List<CitationVerification> verifications,
            List<RetrievalResult> results) {
        
        if (citations.isEmpty()) {
            // 无引用时，检查是否应该有引用
            if (!results.isEmpty()) {
                return 0.5; // 有检索结果但无引用，中等评分
            }
            return 1.0; // 无检索结果也无引用，满分
        }
        
        // 有效引用比例
        long validCitations = verifications.stream()
                .filter(CitationVerification::isValid)
                .count();
        double validityRate = (double) validCitations / citations.size();
        
        // 平均可信度
        double avgCredibility = verifications.stream()
                .filter(CitationVerification::isValid)
                .mapToDouble(CitationVerification::getCredibilityScore)
                .average()
                .orElse(0.0);
        
        // 综合评分
        return validityRate * 0.6 + avgCredibility * 0.4;
    }
    
    /**
     * 标注引用
     */
    private String annotateCitations(String answer, List<CitationVerification> verifications) {
        // 按位置倒序排序，以便从后向前插入不影响位置
        List<CitationVerification> sorted = new ArrayList<>(verifications);
        sorted.sort((a, b) -> Integer.compare(
                b.getCitation().getStartPosition(),
                a.getCitation().getStartPosition()
        ));
        
        StringBuilder annotated = new StringBuilder(answer);
        
        for (CitationVerification verification : sorted) {
            Citation citation = verification.getCitation();
            int end = citation.getEndPosition();
            
            // 构建标注文本
            String annotation;
            if (verification.isValid()) {
                annotation = String.format("✓[可信度:%.0f%%]", 
                        verification.getCredibilityScore() * 100);
            } else {
                annotation = "✗[引用无效]";
            }
            
            // 插入标注
            annotated.insert(end, annotation);
        }
        
        return annotated.toString();
    }
    
    // ============= 内部类 =============
    
    /**
     * 引用
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class Citation {
        private String text;
        private CitationType type;
        private int number;  // 用于数字引用
        private int startPosition;
        private int endPosition;
    }
    
    /**
     * 引用类型
     */
    public enum CitationType {
        NUMBERED,  // 数字引用 [1]
        TEXT,      // 文本引用 [来源名称]
        SOURCE,    // 来源引用 "根据XX"
        URL        // 链接引用
    }
    
    /**
     * 引用验证结果
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class CitationVerification {
        private Citation citation;
        private boolean valid;
        private double credibilityScore;
        private String matchedDocumentId;
        private Map<String, Object> matchedMetadata;
        private String matchDetails;
    }
    
    /**
     * 未引用的重要信息
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class UnreferencedClaim {
        private String claim;
        private boolean hasSupport;
        private String supportingDocumentId;
        private Map<String, Object> supportingMetadata;
    }
    
    /**
     * 引用验证结果
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class CitationValidationResult {
        private List<Citation> citations;
        private List<CitationVerification> verifications;
        private List<UnreferencedClaim> unreferencedClaims;
        private double citationScore;
        private String annotatedAnswer;
        private long durationMs;
        
        public static CitationValidationResult empty(String reason) {
            return new CitationValidationResult(
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList(),
                    1.0,
                    "",
                    0
            );
        }
        
        public int getValidCitationsCount() {
            return (int) verifications.stream().filter(CitationVerification::isValid).count();
        }
        
        public int getInvalidCitationsCount() {
            return (int) verifications.stream().filter(v -> !v.isValid()).count();
        }
    }
}
