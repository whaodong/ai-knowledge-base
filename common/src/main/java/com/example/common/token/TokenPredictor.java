package com.example.common.token;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Token预测器
 * 
 * <p>预测Token使用量，包括预估回答Token数、动态调整检索结果数量、预测是否超出上下文窗口。</p>
 * <p>功能特点：</p>
 * <ul>
 *   <li>预估回答Token数</li>
 *   <li>动态调整检索结果数量</li>
 *   <li>预测是否超出上下文窗口</li>
 *   <li>基于历史数据的预测模型</li>
 *   <li>智能提示词优化建议</li>
 * </ul>
 */
@Slf4j
@Component
public class TokenPredictor {
    
    private final TokenCounter tokenCounter;
    
    // 历史数据：用于预测模型
    private final Map<String, List<TokenPredictionSample>> predictionSamples = new ConcurrentHashMap<>();
    
    // 平均回答长度（按问题类型）
    private final Map<String, Double> avgAnswerLengths = new ConcurrentHashMap<>();
    
    // 模型上下文窗口大小
    private static final Map<String, Integer> CONTEXT_WINDOWS = Map.of(
            "gpt-3.5-turbo", 4096,
            "gpt-3.5-turbo-16k", 16384,
            "gpt-4", 8192,
            "gpt-4-32k", 32768,
            "gpt-4-turbo", 128000
    );
    
    // 预测参数
    private static final double SAFETY_MARGIN = 0.9; // 安全边际（90%）
    private static final int MIN_REMAINING_TOKENS = 500; // 最少剩余Token
    private static final int DEFAULT_ANSWER_TOKENS = 1000; // 默认回答Token数
    
    public TokenPredictor(TokenCounter tokenCounter) {
        this.tokenCounter = tokenCounter;
        initDefaultAnswerLengths();
    }
    
    /**
     * 预估回答Token数
     * 
     * @param question 问题内容
     * @param context 上下文内容
     * @param modelName 模型名称
     * @return 预测的Token数
     */
    public TokenPrediction predictAnswerTokens(String question, String context, String modelName) {
        // 计算输入Token
        int questionTokens = tokenCounter.countTokens(question, modelName);
        int contextTokens = context != null ? tokenCounter.countTokens(context, modelName) : 0;
        int totalInputTokens = questionTokens + contextTokens;
        
        // 预测输出Token
        int predictedOutputTokens = predictOutputTokens(question, modelName);
        
        // 计算总Token
        int totalTokens = totalInputTokens + predictedOutputTokens;
        
        // 获取上下文窗口
        int maxContextTokens = getMaxContextTokens(modelName);
        
        // 检查是否超限
        boolean exceedsLimit = totalTokens > maxContextTokens;
        int remainingTokens = Math.max(0, maxContextTokens - totalInputTokens);
        
        // 置信度
        double confidence = calculateConfidence(question);
        
        TokenPrediction prediction = new TokenPrediction(
                questionTokens,
                contextTokens,
                totalInputTokens,
                predictedOutputTokens,
                totalTokens,
                maxContextTokens,
                remainingTokens,
                exceedsLimit,
                confidence,
                modelName,
                LocalDateTime.now()
        );
        
        log.debug("Token预测: question={}, input={}, output={}, total={}, max={}, exceeds={}", 
                questionTokens, totalInputTokens, predictedOutputTokens, 
                totalTokens, maxContextTokens, exceedsLimit);
        
        return prediction;
    }
    
    /**
     * 动态调整检索结果数量
     * 
     * @param question 问题内容
     * @param requestedTopK 请求的TopK
     * @param modelName 模型名称
     * @param maxContextTokens 最大上下文Token
     * @return 调整后的TopK
     */
    public int adjustRetrievalTopK(String question, int requestedTopK, 
                                    String modelName, int maxContextTokens) {
        // 计算问题Token
        int questionTokens = tokenCounter.countTokens(question, modelName);
        
        // 预留回答空间
        int predictedAnswerTokens = predictOutputTokens(question, modelName);
        
        // 剩余可用Token
        int availableTokens = (int) (maxContextTokens * SAFETY_MARGIN) 
                - questionTokens - predictedAnswerTokens - MIN_REMAINING_TOKENS;
        
        if (availableTokens <= 0) {
            log.warn("无可用Token用于检索: questionTokens={}, predictedAnswer={}", 
                    questionTokens, predictedAnswerTokens);
            return 1;
        }
        
        // 假设每个文档片段平均200 Token
        int avgDocTokens = 200;
        int maxDocs = availableTokens / avgDocTokens;
        
        // 返回较小的值
        int adjustedTopK = Math.min(requestedTopK, maxDocs);
        
        log.debug("调整检索数量: requested={}, adjusted={}, available={}", 
                requestedTopK, adjustedTopK, availableTokens);
        
        return Math.max(1, adjustedTopK);
    }
    
    /**
     * 预测是否超出上下文窗口
     * 
     * @param question 问题内容
     * @param context 上下文内容
     * @param modelName 模型名称
     * @return 预测结果
     */
    public ContextWindowPrediction predictContextWindowExceeded(String question, 
                                                                 String context, 
                                                                 String modelName) {
        int questionTokens = tokenCounter.countTokens(question, modelName);
        int contextTokens = context != null ? tokenCounter.countTokens(context, modelName) : 0;
        int predictedOutputTokens = predictOutputTokens(question, modelName);
        
        int totalTokens = questionTokens + contextTokens + predictedOutputTokens;
        int maxContextTokens = getMaxContextTokens(modelName);
        
        boolean willExceed = totalTokens > maxContextTokens;
        double usagePercentage = (double) totalTokens / maxContextTokens * 100;
        
        // 优化建议
        List<String> suggestions = generateOptimizationSuggestions(
                questionTokens, contextTokens, predictedOutputTokens, 
                maxContextTokens, willExceed
        );
        
        return new ContextWindowPrediction(
                questionTokens,
                contextTokens,
                predictedOutputTokens,
                totalTokens,
                maxContextTokens,
                willExceed,
                usagePercentage,
                suggestions,
                modelName,
                LocalDateTime.now()
        );
    }
    
    /**
     * 记录预测样本（用于改进预测模型）
     * 
     * @param questionType 问题类型
     * @param questionTokens 问题Token数
     * @param actualAnswerTokens 实际回答Token数
     */
    public void recordPredictionSample(String questionType, int questionTokens, int actualAnswerTokens) {
        TokenPredictionSample sample = new TokenPredictionSample(
                questionType, questionTokens, actualAnswerTokens, LocalDateTime.now()
        );
        
        predictionSamples.computeIfAbsent(questionType, k -> new ArrayList<>()).add(sample);
        
        // 更新平均值
        updateAverageAnswerLength(questionType, actualAnswerTokens);
        
        log.debug("记录预测样本: type={}, questionTokens={}, answerTokens={}", 
                questionType, questionTokens, actualAnswerTokens);
    }
    
    /**
     * 获取Token优化建议
     * 
     * @param question 问题内容
     * @param context 上下文内容
     * @param modelName 模型名称
     * @return 优化建议
     */
    public List<TokenOptimizationSuggestion> getOptimizationSuggestions(String question, 
                                                                         String context, 
                                                                         String modelName) {
        List<TokenOptimizationSuggestion> suggestions = new ArrayList<>();
        
        int questionTokens = tokenCounter.countTokens(question, modelName);
        int contextTokens = context != null ? tokenCounter.countTokens(context, modelName) : 0;
        int predictedOutputTokens = predictOutputTokens(question, modelName);
        int totalTokens = questionTokens + contextTokens + predictedOutputTokens;
        int maxContextTokens = getMaxContextTokens(modelName);
        
        // 问题过长
        if (questionTokens > 500) {
            suggestions.add(new TokenOptimizationSuggestion(
                    "QUESTION_TOO_LONG",
                    "问题过长",
                    String.format("问题占用了%d个Token，建议精简问题描述", questionTokens),
                    "HIGH",
                    questionTokens - 300
            ));
        }
        
        // 上下文过长
        if (contextTokens > maxContextTokens * 0.6) {
            suggestions.add(new TokenOptimizationSuggestion(
                    "CONTEXT_TOO_LARGE",
                    "上下文过大",
                    String.format("上下文占用了%d个Token (%.1f%%)，建议减少检索文档数量", 
                            contextTokens, (double) contextTokens / maxContextTokens * 100),
                    "HIGH",
                    (int) (contextTokens * 0.3)
            ));
        }
        
        // 接近上下文窗口限制
        if (totalTokens > maxContextTokens * 0.8) {
            suggestions.add(new TokenOptimizationSuggestion(
                    "APPROACHING_LIMIT",
                    "接近上下文限制",
                    String.format("总Token数达到%d，占上下文窗口的%.1f%%", 
                            totalTokens, (double) totalTokens / maxContextTokens * 100),
                    "MEDIUM",
                    0
            ));
        }
        
        // 建议使用更大上下文的模型
        if (totalTokens > maxContextTokens * SAFETY_MARGIN) {
            String recommendedModel = recommendModel(totalTokens);
            if (recommendedModel != null) {
                suggestions.add(new TokenOptimizationSuggestion(
                        "RECOMMEND_LARGER_MODEL",
                        "建议使用更大上下文的模型",
                        String.format("当前Token数接近限制，建议使用%s", recommendedModel),
                        "MEDIUM",
                        0
                ));
            }
        }
        
        return suggestions;
    }
    
    // ============= 私有方法 =============
    
    /**
     * 预测输出Token
     */
    private int predictOutputTokens(String question, String modelName) {
        // 问题类型识别
        String questionType = classifyQuestion(question);
        
        // 使用历史数据
        Double avgLength = avgAnswerLengths.get(questionType);
        if (avgLength != null) {
            // 考虑问题长度的影响
            int questionTokens = tokenCounter.countTokens(question, modelName);
            double factor = 1.0 + (questionTokens > 100 ? 0.2 : 0);
            
            return (int) (avgLength * factor);
        }
        
        // 默认预测
        return DEFAULT_ANSWER_TOKENS;
    }
    
    /**
     * 分类问题
     */
    private String classifyQuestion(String question) {
        if (question == null || question.isEmpty()) {
            return "general";
        }
        
        String lowerQuestion = question.toLowerCase();
        
        if (lowerQuestion.contains("代码") || lowerQuestion.contains("code") || 
            lowerQuestion.contains("实现") || lowerQuestion.contains("implement")) {
            return "code";
        } else if (lowerQuestion.contains("解释") || lowerQuestion.contains("explain") || 
                   lowerQuestion.contains("什么是") || lowerQuestion.contains("what is")) {
            return "explanation";
        } else if (lowerQuestion.contains("列表") || lowerQuestion.contains("list") || 
                   lowerQuestion.contains("列举") || lowerQuestion.contains("哪些")) {
            return "list";
        } else if (lowerQuestion.contains("总结") || lowerQuestion.contains("summary") || 
                   lowerQuestion.contains("概述")) {
            return "summary";
        } else if (lowerQuestion.contains("对比") || lowerQuestion.contains("比较") || 
                   lowerQuestion.contains("difference")) {
            return "comparison";
        } else if (lowerQuestion.contains("如何") || lowerQuestion.contains("how to") || 
                   lowerQuestion.contains("怎么")) {
            return "howto";
        }
        
        return "general";
    }
    
    /**
     * 计算预测置信度
     */
    private double calculateConfidence(String question) {
        String questionType = classifyQuestion(question);
        
        // 有历史数据的类型，置信度更高
        if (avgAnswerLengths.containsKey(questionType)) {
            return 0.8;
        }
        
        return 0.6;
    }
    
    /**
     * 获取最大上下文Token
     */
    private int getMaxContextTokens(String modelName) {
        return CONTEXT_WINDOWS.getOrDefault(modelName, 4096);
    }
    
    /**
     * 初始化默认回答长度
     */
    private void initDefaultAnswerLengths() {
        avgAnswerLengths.put("code", 800.0);
        avgAnswerLengths.put("explanation", 600.0);
        avgAnswerLengths.put("list", 400.0);
        avgAnswerLengths.put("summary", 300.0);
        avgAnswerLengths.put("comparison", 500.0);
        avgAnswerLengths.put("howto", 700.0);
        avgAnswerLengths.put("general", 500.0);
    }
    
    /**
     * 更新平均回答长度
     */
    private void updateAverageAnswerLength(String questionType, int answerTokens) {
        avgAnswerLengths.compute(questionType, (type, current) -> {
            if (current == null) {
                return (double) answerTokens;
            }
            // 移动平均
            return current * 0.9 + answerTokens * 0.1;
        });
    }
    
    /**
     * 生成优化建议
     */
    private List<String> generateOptimizationSuggestions(int questionTokens, int contextTokens, 
                                                          int predictedOutputTokens, 
                                                          int maxContextTokens, boolean willExceed) {
        List<String> suggestions = new ArrayList<>();
        
        if (willExceed) {
            suggestions.add("上下文将超出限制，请考虑以下优化：");
            
            if (contextTokens > maxContextTokens * 0.5) {
                suggestions.add("- 减少检索文档数量或使用更精准的检索策略");
            }
            
            if (questionTokens > 200) {
                suggestions.add("- 精简问题描述");
            }
            
            suggestions.add("- 使用具有更大上下文窗口的模型");
            suggestions.add("- 启用上下文压缩功能");
        } else if (questionTokens + contextTokens + predictedOutputTokens > maxContextTokens * 0.8) {
            suggestions.add("Token使用接近上下文窗口限制，建议关注以下方面：");
            suggestions.add("- 监控实际回答长度");
            suggestions.add("- 准备降级方案（如截断上下文）");
        }
        
        return suggestions;
    }
    
    /**
     * 推荐模型
     */
    private String recommendModel(int totalTokens) {
        if (totalTokens < 4096) {
            return "gpt-3.5-turbo";
        } else if (totalTokens < 16384) {
            return "gpt-3.5-turbo-16k";
        } else if (totalTokens < 32768) {
            return "gpt-4-32k";
        } else if (totalTokens < 128000) {
            return "gpt-4-turbo";
        }
        return null;
    }
    
    // ============= 内部类 =============
    
    /**
     * Token预测结果
     */
    public static class TokenPrediction {
        private final int questionTokens;
        private final int contextTokens;
        private final int totalInputTokens;
        private final int predictedOutputTokens;
        private final int totalTokens;
        private final int maxContextTokens;
        private final int remainingTokens;
        private final boolean exceedsLimit;
        private final double confidence;
        private final String modelName;
        private final LocalDateTime timestamp;
        
        public TokenPrediction(int questionTokens, int contextTokens, int totalInputTokens,
                               int predictedOutputTokens, int totalTokens, int maxContextTokens,
                               int remainingTokens, boolean exceedsLimit, double confidence,
                               String modelName, LocalDateTime timestamp) {
            this.questionTokens = questionTokens;
            this.contextTokens = contextTokens;
            this.totalInputTokens = totalInputTokens;
            this.predictedOutputTokens = predictedOutputTokens;
            this.totalTokens = totalTokens;
            this.maxContextTokens = maxContextTokens;
            this.remainingTokens = remainingTokens;
            this.exceedsLimit = exceedsLimit;
            this.confidence = confidence;
            this.modelName = modelName;
            this.timestamp = timestamp;
        }
        
        // Getters
        public int getQuestionTokens() { return questionTokens; }
        public int getContextTokens() { return contextTokens; }
        public int getTotalInputTokens() { return totalInputTokens; }
        public int getPredictedOutputTokens() { return predictedOutputTokens; }
        public int getTotalTokens() { return totalTokens; }
        public int getMaxContextTokens() { return maxContextTokens; }
        public int getRemainingTokens() { return remainingTokens; }
        public boolean isExceedsLimit() { return exceedsLimit; }
        public double getConfidence() { return confidence; }
        public String getModelName() { return modelName; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }
    
    /**
     * 上下文窗口预测
     */
    public static class ContextWindowPrediction {
        private final int questionTokens;
        private final int contextTokens;
        private final int predictedOutputTokens;
        private final int totalTokens;
        private final int maxContextTokens;
        private final boolean willExceed;
        private final double usagePercentage;
        private final List<String> suggestions;
        private final String modelName;
        private final LocalDateTime timestamp;
        
        public ContextWindowPrediction(int questionTokens, int contextTokens, 
                                       int predictedOutputTokens, int totalTokens,
                                       int maxContextTokens, boolean willExceed,
                                       double usagePercentage, List<String> suggestions,
                                       String modelName, LocalDateTime timestamp) {
            this.questionTokens = questionTokens;
            this.contextTokens = contextTokens;
            this.predictedOutputTokens = predictedOutputTokens;
            this.totalTokens = totalTokens;
            this.maxContextTokens = maxContextTokens;
            this.willExceed = willExceed;
            this.usagePercentage = usagePercentage;
            this.suggestions = suggestions;
            this.modelName = modelName;
            this.timestamp = timestamp;
        }
        
        // Getters
        public int getQuestionTokens() { return questionTokens; }
        public int getContextTokens() { return contextTokens; }
        public int getPredictedOutputTokens() { return predictedOutputTokens; }
        public int getTotalTokens() { return totalTokens; }
        public int getMaxContextTokens() { return maxContextTokens; }
        public boolean isWillExceed() { return willExceed; }
        public double getUsagePercentage() { return usagePercentage; }
        public List<String> getSuggestions() { return suggestions; }
        public String getModelName() { return modelName; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }
    
    /**
     * Token预测样本
     */
    public static class TokenPredictionSample {
        private final String questionType;
        private final int questionTokens;
        private final int answerTokens;
        private final LocalDateTime timestamp;
        
        public TokenPredictionSample(String questionType, int questionTokens, 
                                     int answerTokens, LocalDateTime timestamp) {
            this.questionType = questionType;
            this.questionTokens = questionTokens;
            this.answerTokens = answerTokens;
            this.timestamp = timestamp;
        }
        
        // Getters
        public String getQuestionType() { return questionType; }
        public int getQuestionTokens() { return questionTokens; }
        public int getAnswerTokens() { return answerTokens; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }
    
    /**
     * Token优化建议
     */
    public static class TokenOptimizationSuggestion {
        private final String code;
        private final String title;
        private final String description;
        private final String priority; // HIGH, MEDIUM, LOW
        private final int estimatedSavings;
        
        public TokenOptimizationSuggestion(String code, String title, String description, 
                                           String priority, int estimatedSavings) {
            this.code = code;
            this.title = title;
            this.description = description;
            this.priority = priority;
            this.estimatedSavings = estimatedSavings;
        }
        
        // Getters
        public String getCode() { return code; }
        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public String getPriority() { return priority; }
        public int getEstimatedSavings() { return estimatedSavings; }
    }
}
