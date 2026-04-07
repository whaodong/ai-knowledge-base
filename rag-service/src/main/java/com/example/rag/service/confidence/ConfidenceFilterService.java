package com.example.rag.service.confidence;

import com.example.rag.model.RetrievalResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 置信度过滤服务
 * 
 * <p>用于缓解AI幻觉，过滤低质量的检索结果。</p>
 * <p>功能特点：</p>
 * <ul>
 *   <li>相似度阈值过滤：排除低相关性结果</li>
 *   <li>置信度评分：综合多维度评估结果质量</li>
 *   <li>来源验证：标注引用来源</li>
 *   <li>无结果降级：返回友好的未找到信息</li>
 * </ul>
 */
@Slf4j
@Service
public class ConfidenceFilterService {
    
    @Value("${rag.confidence.similarity-threshold:0.7}")
    private double defaultSimilarityThreshold;
    
    @Value("${rag.confidence.min-results:1}")
    private int minResults;
    
    @Value("${rag.confidence.max-results:10}")
    private int maxResults;
    
    @Value("${rag.confidence.enable-scoring:true}")
    private boolean enableScoring;
    
    /**
     * 过滤检索结果
     * 
     * @param results 原始检索结果
     * @return 过滤后的结果
     */
    public FilteredResults filter(List<RetrievalResult> results) {
        return filter(results, defaultSimilarityThreshold);
    }
    
    /**
     * 过滤检索结果（指定阈值）
     * 
     * @param results 原始检索结果
     * @param similarityThreshold 相似度阈值
     * @return 过滤后的结果
     */
    public FilteredResults filter(List<RetrievalResult> results, double similarityThreshold) {
        if (results == null || results.isEmpty()) {
            log.debug("无检索结果，返回空列表");
            return FilteredResults.empty("未找到相关信息");
        }
        
        long startTime = System.currentTimeMillis();
        
        // 步骤1: 相似度过滤
        List<ScoredResult> scoredResults = results.stream()
                .map(result -> calculateConfidenceScore(result, similarityThreshold))
                .filter(scored -> scored.getConfidenceScore() >= similarityThreshold)
                .sorted((r1, r2) -> Double.compare(r2.getConfidenceScore(), r1.getConfidenceScore()))
                .collect(Collectors.toList());
        
        // 步骤2: 计算整体置信度
        double overallConfidence = calculateOverallConfidence(scoredResults);
        
        // 步骤3: 选择Top-N结果
        List<ScoredResult> selectedResults = scoredResults.stream()
                .limit(maxResults)
                .collect(Collectors.toList());
        
        // 步骤4: 检查是否满足最小结果数
        boolean hasEnoughResults = selectedResults.size() >= minResults;
        
        // 步骤5: 标注引用来源
        List<RetrievalResult> annotatedResults = annotateWithSources(selectedResults);
        
        long duration = System.currentTimeMillis() - startTime;
        
        log.debug("置信度过滤完成: 原始数={}, 过滤后数={}, 整体置信度={}, 耗时={}ms",
                results.size(), selectedResults.size(), overallConfidence, duration);
        
        return new FilteredResults(
                hasEnoughResults,
                annotatedResults,
                overallConfidence,
                similarityThreshold,
                results.size(),
                selectedResults.size(),
                duration
        );
    }
    
    /**
     * 验证答案质量
     * 
     * @param answer 生成的答案
     * @param retrievalResults 检索结果
     * @return 验证结果
     */
    public AnswerValidation validateAnswer(String answer, List<RetrievalResult> retrievalResults) {
        if (answer == null || answer.isEmpty()) {
            return new AnswerValidation(false, "答案为空", Collections.emptyList());
        }
        
        List<SourceReference> sources = new ArrayList<>();
        int supportedStatements = 0;
        int totalStatements = 0;
        
        // 提取答案中的陈述
        String[] statements = answer.split("[。.!?！？]");
        totalStatements = statements.length;
        
        // 检查每个陈述是否有来源支持
        for (String statement : statements) {
            if (statement.trim().isEmpty()) continue;
            
            SourceReference source = findSupportingSource(statement.trim(), retrievalResults);
            if (source != null) {
                sources.add(source);
                supportedStatements++;
            }
        }
        
        // 计算支持率
        double supportRate = totalStatements > 0 ? 
                (double) supportedStatements / totalStatements : 0;
        
        // 判断答案质量
        boolean isValid = supportRate >= 0.6; // 至少60%的陈述有来源支持
        
        String message = isValid ? 
                String.format("答案验证通过，%.1f%%的陈述有来源支持", supportRate * 100) :
                String.format("答案验证失败，仅%.1f%%的陈述有来源支持（需要≥60%%）", supportRate * 100);
        
        log.debug("答案验证: 有效={}, 支持率={}, 陈述数={}/{}", 
                isValid, supportRate, supportedStatements, totalStatements);
        
        return new AnswerValidation(isValid, message, sources);
    }
    
    /**
     * 生成无结果降级响应
     * 
     * @param query 用户查询
     * @param suggestions 改进建议
     * @return 降级响应
     */
    public FallbackResponse generateFallback(String query, List<String> suggestions) {
        StringBuilder message = new StringBuilder();
        message.append("抱歉，我没有找到与您的问题相关的信息。\n\n");
        
        if (suggestions != null && !suggestions.isEmpty()) {
            message.append("您可以尝试：\n");
            for (int i = 0; i < suggestions.size(); i++) {
                message.append(String.format("%d. %s\n", i + 1, suggestions.get(i)));
            }
        }
        
        message.append("\n如果您需要进一步帮助，请联系人工客服。");
        
        return new FallbackResponse(query, message.toString(), suggestions);
    }
    
    // ============= 私有方法 =============
    
    /**
     * 计算置信度评分
     */
    private ScoredResult calculateConfidenceScore(
            RetrievalResult result, double threshold) {
        
        double baseScore = result.getRerankScore() > 0 ? 
                result.getRerankScore() : result.getRawScore();
        
        if (!enableScoring) {
            return new ScoredResult(result, baseScore, baseScore);
        }
        
        // 多维度评分
        double similarityScore = baseScore;
        double contentQualityScore = evaluateContentQuality(result.getContent());
        double metadataScore = evaluateMetadata(result.getMetadata());
        double lengthScore = evaluateLength(result.getContent());
        
        // 加权平均
        double confidenceScore = similarityScore * 0.5 +
                contentQualityScore * 0.2 +
                metadataScore * 0.2 +
                lengthScore * 0.1;
        
        return new ScoredResult(result, confidenceScore, baseScore);
    }
    
    /**
     * 评估内容质量
     */
    private double evaluateContentQuality(String content) {
        if (content == null || content.isEmpty()) {
            return 0.0;
        }
        
        double score = 1.0;
        
        // 检查是否包含不确定的表达
        String[] uncertainPhrases = {"可能", "也许", "大概", "不确定", "或许", "maybe", "might", "possibly"};
        for (String phrase : uncertainPhrases) {
            if (content.toLowerCase().contains(phrase.toLowerCase())) {
                score -= 0.1;
            }
        }
        
        // 检查是否包含具体数据
        if (content.matches(".*\\d+.*")) {
            score += 0.1;
        }
        
        // 检查是否包含专业术语
        if (content.matches(".*[A-Z]{2,}.*")) {
            score += 0.05;
        }
        
        return Math.max(0, Math.min(1, score));
    }
    
    /**
     * 评估元数据
     */
    private double evaluateMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return 0.5;
        }
        
        double score = 0.5;
        
        // 有来源信息加分
        if (metadata.containsKey("source")) {
            score += 0.3;
        }
        
        // 有时间信息加分
        if (metadata.containsKey("timestamp") || metadata.containsKey("date")) {
            score += 0.1;
        }
        
        // 有作者信息加分
        if (metadata.containsKey("author")) {
            score += 0.1;
        }
        
        return Math.min(1, score);
    }
    
    /**
     * 评估长度
     */
    private double evaluateLength(String content) {
        if (content == null) {
            return 0.0;
        }
        
        int length = content.length();
        
        // 过短或过长都扣分
        if (length < 50) {
            return 0.5;
        } else if (length > 1000) {
            return 0.8;
        } else {
            return 1.0;
        }
    }
    
    /**
     * 计算整体置信度
     */
    private double calculateOverallConfidence(List<ScoredResult> results) {
        if (results.isEmpty()) {
            return 0.0;
        }
        
        // 加权平均，前几个结果权重更高
        double totalWeight = 0;
        double weightedSum = 0;
        
        for (int i = 0; i < results.size(); i++) {
            double weight = 1.0 / (i + 1); // 递减权重
            weightedSum += results.get(i).getConfidenceScore() * weight;
            totalWeight += weight;
        }
        
        return totalWeight > 0 ? weightedSum / totalWeight : 0;
    }
    
    /**
     * 标注引用来源
     */
    private List<RetrievalResult> annotateWithSources(List<ScoredResult> scoredResults) {
        return scoredResults.stream()
                .map(scored -> {
                    RetrievalResult result = scored.getOriginalResult();
                    
                    // 添加置信度标记到元数据
                    if (result.getMetadata() == null) {
                        result.setMetadata(new HashMap<>());
                    }
                    result.getMetadata().put("confidenceScore", scored.getConfidenceScore());
                    result.getMetadata().put("similarityScore", scored.getSimilarityScore());
                    
                    // 添加来源标注
                    String source = result.getMetadata().getOrDefault("source", "未知来源").toString();
                    result.setContent(String.format("[来源: %s, 置信度: %.2f]\n%s",
                            source, scored.getConfidenceScore(), result.getContent()));
                    
                    return result;
                })
                .collect(Collectors.toList());
    }
    
    /**
     * 查找支持来源
     */
    private SourceReference findSupportingSource(String statement, List<RetrievalResult> results) {
        if (results == null || results.isEmpty()) {
            return null;
        }
        
        // 简单的关键词匹配（实际应用中可使用更复杂的语义匹配）
        String[] keywords = statement.split("\\s+");
        
        for (RetrievalResult result : results) {
            String content = result.getContent().toLowerCase();
            int matchCount = 0;
            
            for (String keyword : keywords) {
                if (keyword.length() > 2 && content.contains(keyword.toLowerCase())) {
                    matchCount++;
                }
            }
            
            // 如果超过50%的关键词匹配，认为有来源支持
            if (matchCount > keywords.length * 0.5) {
                return new SourceReference(
                        result.getDocumentId(),
                        result.getMetadata().getOrDefault("source", "未知来源").toString(),
                        (double) matchCount / keywords.length
                );
            }
        }
        
        return null;
    }
    
    // ============= 内部类 =============
    
    /**
     * 评分后的结果
     */
    private static class ScoredResult {
        private final RetrievalResult originalResult;
        private final double confidenceScore;
        private final double similarityScore;
        
        ScoredResult(RetrievalResult originalResult, double confidenceScore, double similarityScore) {
            this.originalResult = originalResult;
            this.confidenceScore = confidenceScore;
            this.similarityScore = similarityScore;
        }
        
        public RetrievalResult getOriginalResult() { return originalResult; }
        public double getConfidenceScore() { return confidenceScore; }
        public double getSimilarityScore() { return similarityScore; }
    }
    
    /**
     * 过滤后的结果
     */
    public static class FilteredResults {
        private final boolean hasResults;
        private final List<RetrievalResult> results;
        private final double overallConfidence;
        private final double threshold;
        private final int originalCount;
        private final int filteredCount;
        private final long duration;
        private final String message;
        
        public FilteredResults(boolean hasResults, List<RetrievalResult> results,
                               double overallConfidence, double threshold,
                               int originalCount, int filteredCount, long duration) {
            this(hasResults, results, overallConfidence, threshold, 
                    originalCount, filteredCount, duration, null);
        }
        
        public FilteredResults(boolean hasResults, List<RetrievalResult> results,
                               double overallConfidence, double threshold,
                               int originalCount, int filteredCount, long duration,
                               String message) {
            this.hasResults = hasResults;
            this.results = results;
            this.overallConfidence = overallConfidence;
            this.threshold = threshold;
            this.originalCount = originalCount;
            this.filteredCount = filteredCount;
            this.duration = duration;
            this.message = message;
        }
        
        public static FilteredResults empty(String message) {
            return new FilteredResults(false, Collections.emptyList(), 
                    0, 0, 0, 0, 0, message);
        }
        
        public boolean hasResults() { return hasResults; }
        public List<RetrievalResult> getResults() { return results; }
        public double getOverallConfidence() { return overallConfidence; }
        public double getThreshold() { return threshold; }
        public int getOriginalCount() { return originalCount; }
        public int getFilteredCount() { return filteredCount; }
        public long getDuration() { return duration; }
        public String getMessage() { return message; }
    }
    
    /**
     * 答案验证结果
     */
    public static class AnswerValidation {
        private final boolean valid;
        private final String message;
        private final List<SourceReference> sources;
        
        public AnswerValidation(boolean valid, String message, List<SourceReference> sources) {
            this.valid = valid;
            this.message = message;
            this.sources = sources;
        }
        
        public boolean isValid() { return valid; }
        public String getMessage() { return message; }
        public List<SourceReference> getSources() { return sources; }
    }
    
    /**
     * 来源引用
     */
    public static class SourceReference {
        private final String documentId;
        private final String source;
        private final double matchScore;
        
        public SourceReference(String documentId, String source, double matchScore) {
            this.documentId = documentId;
            this.source = source;
            this.matchScore = matchScore;
        }
        
        public String getDocumentId() { return documentId; }
        public String getSource() { return source; }
        public double getMatchScore() { return matchScore; }
    }
    
    /**
     * 降级响应
     */
    public static class FallbackResponse {
        private final String query;
        private final String message;
        private final List<String> suggestions;
        
        public FallbackResponse(String query, String message, List<String> suggestions) {
            this.query = query;
            this.message = message;
            this.suggestions = suggestions;
        }
        
        public String getQuery() { return query; }
        public String getMessage() { return message; }
        public List<String> getSuggestions() { return suggestions; }
    }
}
