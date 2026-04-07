package com.example.rag.service.validation;

import com.example.rag.model.RetrievalResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 事实一致性检查器
 * 
 * <p>检查答案与检索文档之间的事实一致性，检测矛盾和不一致。</p>
 * <p>功能特点：</p>
 * <ul>
 *   <li>提取答案中的关键声明</li>
 *   <li>与检索文档进行事实比对</li>
 *   <li>检测矛盾和不一致</li>
 *   <li>生成一致性评分</li>
 * </ul>
 */
@Slf4j
@Component
public class FactConsistencyChecker {
    
    // 数字模式：匹配各种数字格式
    private static final Pattern NUMBER_PATTERN = Pattern.compile(
            "\\b\\d+(?:\\.\\d+)?(?:%|万|亿|千|百|十)?\\b|" +
            "\\b(?:one|two|three|four|five|six|seven|eight|nine|ten|hundred|thousand|million|billion)\\b",
            Pattern.CASE_INSENSITIVE
    );
    
    // 时间模式：匹配日期和时间表达
    private static final Pattern TIME_PATTERN = Pattern.compile(
            "\\d{4}[-/年]\\d{1,2}[-/月]\\d{1,2}[日]?|" +
            "\\d{1,2}[-/月]\\d{1,2}[日]?|" +
            "(?:昨天|今天|明天|前天|后天|上周|下周|上个月|下个月|" +
            "last\\s+(?:week|month|year)|next\\s+(?:week|month|year)|" +
            "yesterday|today|tomorrow)",
            Pattern.CASE_INSENSITIVE
    );
    
    // 实体名称模式：匹配大写字母开头的词组
    private static final Pattern ENTITY_PATTERN = Pattern.compile(
            "\\b[A-Z][a-z]+(?:\\s+[A-Z][a-z]+)*\\b|" +
            "(?:公司|企业|机构|组织|部门|系统|平台)"
    );
    
    // 否定词模式
    private static final Pattern NEGATION_PATTERN = Pattern.compile(
            "\\b(不|没有|无|非|不是|并非|未|不包含|不涉及|不会|不能|无法|" +
            "no|not|never|neither|nor|cannot|won't|don't|doesn't|isn't|aren't)\\b",
            Pattern.CASE_INSENSITIVE
    );
    
    /**
     * 检查答案与检索文档的一致性
     * 
     * @param answer 生成的答案
     * @param retrievalResults 检索结果列表
     * @return 一致性检查结果
     */
    public ConsistencyCheckResult check(String answer, List<RetrievalResult> retrievalResults) {
        if (answer == null || answer.isEmpty()) {
            return ConsistencyCheckResult.empty("答案为空");
        }
        
        if (retrievalResults == null || retrievalResults.isEmpty()) {
            return ConsistencyCheckResult.empty("无检索结果用于验证");
        }
        
        long startTime = System.currentTimeMillis();
        
        // 1. 提取答案中的关键声明
        List<FactClaim> claims = extractClaims(answer);
        log.debug("提取到 {} 个关键声明", claims.size());
        
        // 2. 对每个声明进行验证
        List<ClaimVerification> verifications = new ArrayList<>();
        for (FactClaim claim : claims) {
            ClaimVerification verification = verifyClaim(claim, retrievalResults);
            verifications.add(verification);
        }
        
        // 3. 计算一致性评分
        double consistencyScore = calculateConsistencyScore(verifications);
        
        // 4. 识别矛盾点
        List<Contradiction> contradictions = identifyContradictions(verifications, retrievalResults);
        
        long duration = System.currentTimeMillis() - startTime;
        
        log.info("事实一致性检查完成: 声明数={}, 一致性评分={}, 矛盾数={}, 耗时={}ms",
                claims.size(), consistencyScore, contradictions.size(), duration);
        
        return new ConsistencyCheckResult(
                claims,
                verifications,
                contradictions,
                consistencyScore,
                duration
        );
    }
    
    /**
     * 提取答案中的关键声明
     */
    private List<FactClaim> extractClaims(String answer) {
        List<FactClaim> claims = new ArrayList<>();
        
        // 按句子分割
        String[] sentences = answer.split("[。.!?！？\\n]+");
        
        for (String sentence : sentences) {
            if (sentence.trim().isEmpty()) {
                continue;
            }
            
            // 提取包含事实信息的句子
            FactClaim claim = extractClaimFromSentence(sentence.trim());
            if (claim != null) {
                claims.add(claim);
            }
        }
        
        return claims;
    }
    
    /**
     * 从句子中提取事实声明
     */
    private FactClaim extractClaimFromSentence(String sentence) {
        List<String> numbers = extractNumbers(sentence);
        List<String> times = extractTimes(sentence);
        List<String> entities = extractEntities(sentence);
        boolean hasNegation = containsNegation(sentence);
        
        // 如果包含数字、时间或实体，认为是事实声明
        if (!numbers.isEmpty() || !times.isEmpty() || !entities.isEmpty()) {
            return new FactClaim(
                    sentence,
                    numbers,
                    times,
                    entities,
                    hasNegation,
                    ClaimType.fromContent(sentence)
            );
        }
        
        // 检查是否包含判断性词汇
        if (containsJudgmentWords(sentence)) {
            return new FactClaim(
                    sentence,
                    numbers,
                    times,
                    entities,
                    hasNegation,
                    ClaimType.JUDGMENT
            );
        }
        
        return null;
    }
    
    /**
     * 验证声明
     */
    private ClaimVerification verifyClaim(FactClaim claim, List<RetrievalResult> results) {
        List<SourceMatch> matches = new ArrayList<>();
        boolean isSupported = false;
        double maxMatchScore = 0;
        
        for (RetrievalResult result : results) {
            SourceMatch match = matchWithSource(claim, result);
            matches.add(match);
            
            if (match.isSupports()) {
                isSupported = true;
                maxMatchScore = Math.max(maxMatchScore, match.getMatchScore());
            }
        }
        
        return new ClaimVerification(
                claim,
                isSupported,
                maxMatchScore,
                matches
        );
    }
    
    /**
     * 与来源进行匹配
     */
    private SourceMatch matchWithSource(FactClaim claim, RetrievalResult result) {
        String sourceContent = result.getContent().toLowerCase();
        String claimText = claim.getText().toLowerCase();
        
        double score = 0;
        boolean supports = false;
        List<String> matchedFacts = new ArrayList<>();
        List<String> unmatchedFacts = new ArrayList<>();
        
        // 1. 数字匹配
        for (String number : claim.getNumbers()) {
            if (sourceContent.contains(number.toLowerCase())) {
                matchedFacts.add("数字: " + number);
                score += 0.2;
            } else {
                unmatchedFacts.add("数字: " + number);
            }
        }
        
        // 2. 时间匹配
        for (String time : claim.getTimes()) {
            if (sourceContent.contains(time.toLowerCase())) {
                matchedFacts.add("时间: " + time);
                score += 0.2;
            } else {
                unmatchedFacts.add("时间: " + time);
            }
        }
        
        // 3. 实体匹配
        for (String entity : claim.getEntities()) {
            if (sourceContent.contains(entity.toLowerCase())) {
                matchedFacts.add("实体: " + entity);
                score += 0.2;
            } else {
                unmatchedFacts.add("实体: " + entity);
            }
        }
        
        // 4. 语义相似度（简化版：关键词重叠）
        double keywordOverlap = calculateKeywordOverlap(claimText, sourceContent);
        score += keywordOverlap * 0.4;
        
        // 5. 检查否定是否一致
        boolean sourceHasNegation = containsNegation(sourceContent);
        if (claim.hasNegation() == sourceHasNegation) {
            score += 0.2;
        } else {
            score -= 0.3; // 否定不一致，可能是矛盾
        }
        
        // 归一化分数
        score = Math.min(1.0, Math.max(0.0, score));
        
        // 判断是否支持
        supports = score >= 0.5 && matchedFacts.size() > 0;
        
        return new SourceMatch(
                result.getDocumentId(),
                result.getMetadata().getOrDefault("source", "未知来源").toString(),
                supports,
                score,
                matchedFacts,
                unmatchedFacts
        );
    }
    
    /**
     * 计算关键词重叠度
     */
    private double calculateKeywordOverlap(String text1, String text2) {
        Set<String> words1 = extractKeywords(text1);
        Set<String> words2 = extractKeywords(text2);
        
        if (words1.isEmpty() || words2.isEmpty()) {
            return 0.0;
        }
        
        Set<String> intersection = new HashSet<>(words1);
        intersection.retainAll(words2);
        
        return (double) intersection.size() / Math.min(words1.size(), words2.size());
    }
    
    /**
     * 提取关键词
     */
    private Set<String> extractKeywords(String text) {
        // 移除停用词
        Set<String> stopWords = new HashSet<>(Arrays.asList(
                "的", "是", "在", "有", "和", "了", "不", "这", "我", "他", "她", "它",
                "the", "a", "an", "is", "are", "was", "were", "be", "been", "being",
                "have", "has", "had", "do", "does", "did", "will", "would", "could",
                "should", "may", "might", "must", "shall", "can", "need", "dare",
                "ought", "used", "to", "of", "in", "for", "on", "with", "at", "by",
                "from", "as", "into", "through", "during", "before", "after",
                "above", "below", "between", "under", "again", "further", "then",
                "once", "here", "there", "when", "where", "why", "how", "all", "each",
                "few", "more", "most", "other", "some", "such", "no", "nor", "not",
                "only", "own", "same", "so", "than", "too", "very", "just"
        ));
        
        // 分词并过滤
        Set<String> keywords = new HashSet<>();
        String[] words = text.split("\\s+");
        
        for (String word : words) {
            word = word.replaceAll("[^\\w\\u4e00-\\u9fa5]", "").toLowerCase();
            if (word.length() > 1 && !stopWords.contains(word)) {
                keywords.add(word);
            }
        }
        
        return keywords;
    }
    
    /**
     * 识别矛盾点
     */
    private List<Contradiction> identifyContradictions(
            List<ClaimVerification> verifications, 
            List<RetrievalResult> results) {
        
        List<Contradiction> contradictions = new ArrayList<>();
        
        for (ClaimVerification verification : verifications) {
            // 如果声明与某些来源匹配但与其他来源矛盾
            List<SourceMatch> supportingSources = new ArrayList<>();
            List<SourceMatch> contradictingSources = new ArrayList<>();
            
            for (SourceMatch match : verification.getMatches()) {
                if (match.isSupports()) {
                    supportingSources.add(match);
                } else if (match.getMatchScore() > 0.3) {
                    // 有一定相关性但不支持的可能是矛盾
                    contradictingSources.add(match);
                }
            }
            
            // 如果同时存在支持和矛盾的来源
            if (!supportingSources.isEmpty() && !contradictingSources.isEmpty()) {
                contradictions.add(new Contradiction(
                        verification.getClaim().getText(),
                        supportingSources,
                        contradictingSources,
                        ContradictionType.PARTIAL
                ));
            }
            
            // 如果声明完全未找到支持
            if (!verification.isSupported() && 
                verification.getClaim().getType() != ClaimType.OPINION) {
                contradictions.add(new Contradiction(
                        verification.getClaim().getText(),
                        Collections.emptyList(),
                        verification.getMatches(),
                        ContradictionType.UNSUPPORTED
                ));
            }
        }
        
        return contradictions;
    }
    
    /**
     * 计算一致性评分
     */
    private double calculateConsistencyScore(List<ClaimVerification> verifications) {
        if (verifications.isEmpty()) {
            return 1.0; // 无声明时认为一致
        }
        
        double totalScore = 0;
        int weightSum = 0;
        
        for (ClaimVerification verification : verifications) {
            // 根据声明类型设置权重
            int weight = verification.getClaim().getType().getWeight();
            
            if (verification.isSupported()) {
                totalScore += verification.getMaxMatchScore() * weight;
            } else {
                // 未支持的声明扣分
                totalScore += 0.3 * weight;
            }
            
            weightSum += weight;
        }
        
        return weightSum > 0 ? totalScore / weightSum : 1.0;
    }
    
    // ============= 辅助方法 =============
    
    private List<String> extractNumbers(String text) {
        List<String> numbers = new ArrayList<>();
        Matcher matcher = NUMBER_PATTERN.matcher(text);
        while (matcher.find()) {
            numbers.add(matcher.group());
        }
        return numbers;
    }
    
    private List<String> extractTimes(String text) {
        List<String> times = new ArrayList<>();
        Matcher matcher = TIME_PATTERN.matcher(text);
        while (matcher.find()) {
            times.add(matcher.group());
        }
        return times;
    }
    
    private List<String> extractEntities(String text) {
        List<String> entities = new ArrayList<>();
        Matcher matcher = ENTITY_PATTERN.matcher(text);
        while (matcher.find()) {
            entities.add(matcher.group());
        }
        return entities;
    }
    
    private boolean containsNegation(String text) {
        return NEGATION_PATTERN.matcher(text).find();
    }
    
    private boolean containsJudgmentWords(String sentence) {
        String[] judgmentWords = {
                "是", "为", "等于", "属于", "包括", "表示", "意味着",
                "is", "are", "means", "indicates", "shows", "suggests"
        };
        
        String lowerSentence = sentence.toLowerCase();
        for (String word : judgmentWords) {
            if (lowerSentence.contains(word.toLowerCase())) {
                return true;
            }
        }
        
        return false;
    }
    
    // ============= 内部类 =============
    
    /**
     * 事实声明
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class FactClaim {
        private String text;
        private List<String> numbers;
        private List<String> times;
        private List<String> entities;
        private boolean hasNegation;
        private ClaimType type;
    }
    
    /**
     * 声明类型
     */
    public enum ClaimType {
        STATISTICAL(3),    // 统计数据，权重最高
        TEMPORAL(2),       // 时间相关
        ENTITY(2),         // 实体相关
        JUDGMENT(1),       // 判断性声明
        OPINION(1);        // 观点，权重最低
        
        private final int weight;
        
        ClaimType(int weight) {
            this.weight = weight;
        }
        
        public int getWeight() {
            return weight;
        }
        
        public static ClaimType fromContent(String content) {
            if (content.matches(".*\\d+.*%.*") || content.matches(".*\\d+(万|亿).*")) {
                return STATISTICAL;
            }
            if (content.matches(".*\\d{4}.*") || content.matches(".*(年|月|日).*")) {
                return TEMPORAL;
            }
            if (content.matches(".*[A-Z][a-z]+.*") || content.matches(".*(公司|企业|组织).*")) {
                return ENTITY;
            }
            if (content.matches(".*(认为|相信|觉得|should|must|need).*")) {
                return OPINION;
            }
            return JUDGMENT;
        }
    }
    
    /**
     * 声明验证结果
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class ClaimVerification {
        private FactClaim claim;
        private boolean supported;
        private double maxMatchScore;
        private List<SourceMatch> matches;
    }
    
    /**
     * 来源匹配
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class SourceMatch {
        private String documentId;
        private String sourceName;
        private boolean supports;
        private double matchScore;
        private List<String> matchedFacts;
        private List<String> unmatchedFacts;
    }
    
    /**
     * 矛盾点
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class Contradiction {
        private String claim;
        private List<SourceMatch> supportingSources;
        private List<SourceMatch> contradictingSources;
        private ContradictionType type;
    }
    
    /**
     * 矛盾类型
     */
    public enum ContradictionType {
        DIRECT,      // 直接矛盾
        PARTIAL,     // 部分矛盾
        UNSUPPORTED  // 无依据
    }
    
    /**
     * 一致性检查结果
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class ConsistencyCheckResult {
        private List<FactClaim> claims;
        private List<ClaimVerification> verifications;
        private List<Contradiction> contradictions;
        private double consistencyScore;
        private long durationMs;
        
        public static ConsistencyCheckResult empty(String reason) {
            return new ConsistencyCheckResult(
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList(),
                    0.0,
                    0
            );
        }
        
        public boolean hasContradictions() {
            return !contradictions.isEmpty();
        }
        
        public int getSupportedClaimsCount() {
            return (int) verifications.stream().filter(ClaimVerification::isSupported).count();
        }
        
        public int getUnsupportedClaimsCount() {
            return (int) verifications.stream().filter(v -> !v.isSupported()).count();
        }
    }
}
