package com.example.rag.service.validation;

import com.example.rag.model.RetrievalResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 答案评分服务
 * 
 * <p>综合多个维度对答案进行评分，生成综合置信度。</p>
 * <p>评分维度：</p>
 * <ul>
 *   <li>相关性：答案与问题的相关程度</li>
 *   <li>一致性：答案与检索文档的事实一致性</li>
 *   <li>引用评分：引用的准确性和可信度</li>
 *   <li>完整性：答案的完整程度</li>
 *   <li>综合置信度：加权平均后的总体评分</li>
 * </ul>
 */
@Slf4j
@Service
public class AnswerScorer {
    
    @Autowired
    private FactConsistencyChecker factConsistencyChecker;
    
    @Autowired
    private CitationValidator citationValidator;
    
    @Value("${rag.scoring.relevance-weight:0.25}")
    private double relevanceWeight;
    
    @Value("${rag.scoring.consistency-weight:0.30}")
    private double consistencyWeight;
    
    @Value("${rag.scoring.citation-weight:0.15}")
    private double citationWeight;
    
    @Value("${rag.scoring.completeness-weight:0.15}")
    private double completenessWeight;
    
    @Value("${rag.scoring.hallucination-weight:0.15}")
    private double hallucinationWeight;
    
    /**
     * 对答案进行多维度评分
     * 
     * @param answer 生成的答案
     * @param query 用户问题
     * @param retrievalResults 检索结果列表
     * @return 答案评分结果
     */
    public AnswerScore score(String answer, String query, List<RetrievalResult> retrievalResults) {
        if (answer == null || answer.isEmpty()) {
            return createEmptyScore("答案为空");
        }
        
        long startTime = System.currentTimeMillis();
        log.info("开始多维度评分，答案长度: {}, 检索结果数: {}", answer.length(), retrievalResults.size());
        
        Map<String, Double> scoreDetails = new HashMap<>();
        
        // 1. 计算相关性评分
        double relevanceScore = calculateRelevanceScore(answer, query, retrievalResults);
        scoreDetails.put("relevance", relevanceScore);
        log.debug("相关性评分: {}", relevanceScore);
        
        // 2. 计算一致性评分
        double consistencyScore = calculateConsistencyScore(answer, retrievalResults);
        scoreDetails.put("consistency", consistencyScore);
        log.debug("一致性评分: {}", consistencyScore);
        
        // 3. 计算引用评分
        double citationScore = calculateCitationScore(answer, retrievalResults);
        scoreDetails.put("citation", citationScore);
        log.debug("引用评分: {}", citationScore);
        
        // 4. 计算完整性评分
        double completenessScore = calculateCompletenessScore(answer, query, retrievalResults);
        scoreDetails.put("completeness", completenessScore);
        log.debug("完整性评分: {}", completenessScore);
        
        // 5. 计算幻觉评分（分数越高越好，表示幻觉越少）
        double hallucinationScore = calculateHallucinationScore(answer, retrievalResults);
        scoreDetails.put("hallucination", hallucinationScore);
        log.debug("幻觉评分: {}", hallucinationScore);
        
        // 6. 计算综合置信度
        double confidence = calculateWeightedConfidence(
                relevanceScore,
                consistencyScore,
                citationScore,
                completenessScore,
                hallucinationScore
        );
        
        long duration = System.currentTimeMillis() - startTime;
        log.info("多维度评分完成: 综合置信度={}, 耗时={}ms", confidence, duration);
        
        // 7. 构建并返回评分结果
        return AnswerScore.builder()
                .relevance(relevanceScore)
                .consistency(consistencyScore)
                .citationScore(citationScore)
                .completeness(completenessScore)
                .hallucinationScore(hallucinationScore)
                .confidence(confidence)
                .scoreDetails(scoreDetails)
                .level(AnswerScore.ScoreLevel.fromScore(confidence))
                .build();
    }
    
    /**
     * 计算相关性评分
     */
    private double calculateRelevanceScore(String answer, String query, List<RetrievalResult> results) {
        if (query == null || query.isEmpty()) {
            return 0.5; // 无查询时返回中等评分
        }
        
        // 1. 查询关键词覆盖度
        double keywordCoverage = calculateKeywordCoverage(query, answer);
        
        // 2. 检索结果利用度
        double sourceUtilization = calculateSourceUtilization(answer, results);
        
        // 3. 主题一致性
        double topicConsistency = calculateTopicConsistency(query, answer);
        
        // 加权平均
        return keywordCoverage * 0.4 + sourceUtilization * 0.3 + topicConsistency * 0.3;
    }
    
    /**
     * 计算关键词覆盖度
     */
    private double calculateKeywordCoverage(String query, String answer) {
        // 提取查询关键词
        Set<String> queryKeywords = extractKeywords(query);
        Set<String> answerKeywords = extractKeywords(answer);
        
        if (queryKeywords.isEmpty()) {
            return 1.0;
        }
        
        // 计算交集
        Set<String> intersection = new HashSet<>(queryKeywords);
        intersection.retainAll(answerKeywords);
        
        return (double) intersection.size() / queryKeywords.size();
    }
    
    /**
     * 计算检索结果利用度
     */
    private double calculateSourceUtilization(String answer, List<RetrievalResult> results) {
        if (results.isEmpty()) {
            return 1.0;
        }
        
        String lowerAnswer = answer.toLowerCase();
        int utilizedCount = 0;
        
        for (RetrievalResult result : results) {
            // 检查答案是否使用了检索结果中的关键词
            Set<String> resultKeywords = extractKeywords(result.getContent());
            Set<String> answerKeywords = extractKeywords(answer);
            
            Set<String> intersection = new HashSet<>(resultKeywords);
            intersection.retainAll(answerKeywords);
            
            if (!intersection.isEmpty()) {
                utilizedCount++;
            }
        }
        
        return (double) utilizedCount / results.size();
    }
    
    /**
     * 计算主题一致性
     */
    private double calculateTopicConsistency(String query, String answer) {
        // 简化实现：检查问答是否包含相似的名词短语
        Set<String> queryNouns = extractNouns(query);
        Set<String> answerNouns = extractNouns(answer);
        
        if (queryNouns.isEmpty()) {
            return 1.0;
        }
        
        Set<String> intersection = new HashSet<>(queryNouns);
        intersection.retainAll(answerNouns);
        
        return (double) intersection.size() / queryNouns.size();
    }
    
    /**
     * 计算一致性评分
     */
    private double calculateConsistencyScore(String answer, List<RetrievalResult> results) {
        FactConsistencyChecker.ConsistencyCheckResult checkResult = 
                factConsistencyChecker.check(answer, results);
        
        return checkResult.getConsistencyScore();
    }
    
    /**
     * 计算引用评分
     */
    private double calculateCitationScore(String answer, List<RetrievalResult> results) {
        CitationValidator.CitationValidationResult validationResult = 
                citationValidator.validate(answer, results);
        
        return validationResult.getCitationScore();
    }
    
    /**
     * 计算完整性评分
     */
    private double calculateCompletenessScore(String answer, String query, List<RetrievalResult> results) {
        // 1. 答案长度评分
        double lengthScore = evaluateAnswerLength(answer);
        
        // 2. 信息完整度
        double infoCompleteness = evaluateInfoCompleteness(answer, query);
        
        // 3. 结构完整性
        double structureScore = evaluateStructure(answer);
        
        return lengthScore * 0.3 + infoCompleteness * 0.4 + structureScore * 0.3;
    }
    
    /**
     * 评估答案长度
     */
    private double evaluateAnswerLength(String answer) {
        int length = answer.length();
        
        // 理想长度：100-500字
        if (length >= 100 && length <= 500) {
            return 1.0;
        } else if (length < 100) {
            return length / 100.0;
        } else if (length <= 1000) {
            return 1.0 - (length - 500) / 1000.0;
        } else {
            return 0.5; // 过长扣分
        }
    }
    
    /**
     * 评估信息完整度
     */
    private double evaluateInfoCompleteness(String answer, String query) {
        // 检查答案是否回答了问题的关键部分
        // 简化实现：检查是否包含疑问词对应的回答
        
        double score = 1.0;
        
        // 如果是"是什么"问题，检查是否定义
        if (query.contains("是什么") || query.toLowerCase().contains("what is")) {
            if (!answer.contains("是") && !answer.contains("指") && !answer.contains("意思是")) {
                score -= 0.3;
            }
        }
        
        // 如果是"如何"问题，检查是否有步骤或方法
        if (query.contains("如何") || query.contains("怎么") || query.toLowerCase().contains("how")) {
            if (!answer.contains("步骤") && !answer.contains("方法") && !answer.contains("首先") && !answer.contains("然后")) {
                score -= 0.3;
            }
        }
        
        // 如果是"为什么"问题，检查是否有原因解释
        if (query.contains("为什么") || query.toLowerCase().contains("why")) {
            if (!answer.contains("因为") && !answer.contains("原因是") && !answer.contains("由于")) {
                score -= 0.3;
            }
        }
        
        return Math.max(0, score);
    }
    
    /**
     * 评估结构完整性
     */
    private double evaluateStructure(String answer) {
        double score = 1.0;
        
        // 检查是否有开头
        if (!answer.matches("^[a-zA-Z0-9\\u4e00-\\u9fa5].*[。.!?！？]")) {
            score -= 0.1;
        }
        
        // 检查是否有段落分隔（长答案应该分段）
        if (answer.length() > 200 && answer.split("\\n").length < 2) {
            score -= 0.2;
        }
        
        // 检查是否有总结
        if (answer.length() > 300 && 
            !answer.contains("总之") && 
            !answer.contains("综上所述") && 
            !answer.contains("总结")) {
            score -= 0.1;
        }
        
        return Math.max(0.5, score);
    }
    
    /**
     * 计算幻觉评分
     */
    private double calculateHallucinationScore(String answer, List<RetrievalResult> results) {
        // 检测幻觉特征
        double hallucinationIndicators = 0;
        int totalChecks = 5;
        
        // 1. 无依据的陈述
        if (hasUnsupportedClaims(answer, results)) {
            hallucinationIndicators += 1;
        }
        
        // 2. 过度推断
        if (hasOverInference(answer)) {
            hallucinationIndicators += 1;
        }
        
        // 3. 时间/数字不一致
        if (hasInconsistentNumbers(answer, results)) {
            hallucinationIndicators += 1;
        }
        
        // 4. 模糊表达
        if (hasVagueExpressions(answer)) {
            hallucinationIndicators += 0.5;
        }
        
        // 5. 矛盾陈述
        if (hasContradictions(answer)) {
            hallucinationIndicators += 1.5;
        }
        
        // 转换为评分（幻觉越少分数越高）
        return 1.0 - (hallucinationIndicators / totalChecks);
    }
    
    /**
     * 检查是否有无依据的陈述
     */
    private boolean hasUnsupportedClaims(String answer, List<RetrievalResult> results) {
        // 检查答案中是否有检索结果不支持的具体数据
        String[] sentences = answer.split("[。.!?！？]");
        
        for (String sentence : sentences) {
            // 如果包含具体数字但没有引用
            if (sentence.matches(".*\\d+.*") && !sentence.contains("[")) {
                // 检查是否在检索结果中
                boolean found = false;
                for (RetrievalResult result : results) {
                    if (result.getContent().contains(sentence.trim())) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * 检查是否有过度推断
     */
    private boolean hasOverInference(String answer) {
        String[] inferenceWords = {
                "肯定", "一定", "必然", "绝对",
                "所有", "全部", "每个", "任何",
                "always", "never", "all", "every", "must"
        };
        
        String lowerAnswer = answer.toLowerCase();
        for (String word : inferenceWords) {
            if (lowerAnswer.contains(word.toLowerCase())) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 检查是否有数字不一致
     */
    private boolean hasInconsistentNumbers(String answer, List<RetrievalResult> results) {
        // 提取答案中的数字
        Set<String> answerNumbers = extractNumbers(answer);
        
        // 提取检索结果中的数字
        Set<String> sourceNumbers = new HashSet<>();
        for (RetrievalResult result : results) {
            sourceNumbers.addAll(extractNumbers(result.getContent()));
        }
        
        // 检查是否有关键数字不匹配
        // 简化实现：只检查年份
        for (String num : answerNumbers) {
            if (num.matches("\\d{4}")) { // 年份
                // 如果年份差异太大，认为不一致
                try {
                    int year = Integer.parseInt(num);
                    if (year > 2030 || year < 1900) {
                        return true;
                    }
                } catch (NumberFormatException e) {
                    // 忽略
                }
            }
        }
        
        return false;
    }
    
    /**
     * 检查是否有模糊表达
     */
    private boolean hasVagueExpressions(String answer) {
        String[] vagueWords = {
                "可能", "也许", "大概", "或许", "似乎",
                "maybe", "might", "possibly", "perhaps"
        };
        
        String lowerAnswer = answer.toLowerCase();
        int vagueCount = 0;
        
        for (String word : vagueWords) {
            if (lowerAnswer.contains(word.toLowerCase())) {
                vagueCount++;
            }
        }
        
        // 如果模糊词过多，返回true
        return vagueCount > 3;
    }
    
    /**
     * 检查是否有矛盾陈述
     */
    private boolean hasContradictions(String answer) {
        // 检查是否同时存在肯定和否定
        boolean hasPositive = false;
        boolean hasNegative = false;
        
        String[] positiveWords = {"是", "有", "能", "可以", "会"};
        String[] negativeWords = {"不是", "没有", "不能", "不可以", "不会"};
        
        for (String word : positiveWords) {
            if (answer.contains(word)) {
                hasPositive = true;
                break;
            }
        }
        
        for (String word : negativeWords) {
            if (answer.contains(word)) {
                hasNegative = true;
                break;
            }
        }
        
        return hasPositive && hasNegative;
    }
    
    /**
     * 计算加权置信度
     */
    private double calculateWeightedConfidence(
            double relevance,
            double consistency,
            double citation,
            double completeness,
            double hallucination) {
        
        return relevance * relevanceWeight +
               consistency * consistencyWeight +
               citation * citationWeight +
               completeness * completenessWeight +
               hallucination * hallucinationWeight;
    }
    
    /**
     * 创建空评分
     */
    private AnswerScore createEmptyScore(String reason) {
        return AnswerScore.builder()
                .relevance(0)
                .consistency(0)
                .citationScore(0)
                .completeness(0)
                .hallucinationScore(0)
                .confidence(0)
                .level(AnswerScore.ScoreLevel.UNACCEPTABLE)
                .scoreDetails(Collections.singletonMap("error", 0.0))
                .build();
    }
    
    // ============= 辅助方法 =============
    
    private Set<String> extractKeywords(String text) {
        Set<String> stopWords = new HashSet<>(Arrays.asList(
                "的", "是", "在", "有", "和", "了", "不", "这", "我", "他", "她", "它",
                "the", "a", "an", "is", "are", "was", "were", "be", "been", "being",
                "have", "has", "had", "do", "does", "did", "will", "would", "could",
                "should", "may", "might", "must", "shall", "can", "need"
        ));
        
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
    
    private Set<String> extractNouns(String text) {
        // 简化实现：提取大写字母开头的词和中文名词
        Set<String> nouns = new HashSet<>();
        
        // 英文专有名词
        String[] words = text.split("\\s+");
        for (String word : words) {
            if (word.matches("[A-Z][a-z]+")) {
                nouns.add(word.toLowerCase());
            }
        }
        
        // 中文名词（简化：提取包含特定后缀的词）
        String[] nounSuffixes = {"公司", "企业", "系统", "平台", "技术", "服务"};
        for (String suffix : nounSuffixes) {
            int index = text.indexOf(suffix);
            if (index > 0) {
                // 提取前缀
                int start = Math.max(0, index - 10);
                String context = text.substring(start, index + suffix.length());
                nouns.add(context.trim());
            }
        }
        
        return nouns;
    }
    
    private Set<String> extractNumbers(String text) {
        Set<String> numbers = new HashSet<>();
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\b\\d+(?:\\.\\d+)?(?:%|万|亿)?\\b");
        java.util.regex.Matcher matcher = pattern.matcher(text);
        
        while (matcher.find()) {
            numbers.add(matcher.group());
        }
        
        return numbers;
    }
}
