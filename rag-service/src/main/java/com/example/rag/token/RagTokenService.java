package com.example.rag.token;

import com.example.common.token.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RAG Token管理服务
 * 
 * <p>集成Token计数、会话管理、上下文优化、使用分析和预测功能。</p>
 */
@Slf4j
@Service
public class RagTokenService {
    
    @Autowired
    private TokenCounter tokenCounter;
    
    @Autowired
    private StreamingTokenCounter streamingTokenCounter;
    
    @Autowired
    private SessionTokenManager sessionTokenManager;
    
    @Autowired
    private IncrementalContextOptimizer contextOptimizer;
    
    @Autowired
    private TokenUsageAnalyzer usageAnalyzer;
    
    @Autowired
    private TokenPredictor tokenPredictor;
    
    /**
     * 初始化RAG会话Token管理
     * 
     * @param sessionId 会话ID
     * @param userId 用户ID
     * @param modelName 模型名称
     */
    public void initSession(String sessionId, String userId, String modelName) {
        // 初始化会话Token管理
        sessionTokenManager.startSession(sessionId, userId, modelName);
        
        // 初始化上下文缓存
        contextOptimizer.initContextCache(sessionId, modelName);
        
        // 初始化流式计数
        streamingTokenCounter.startStreaming(sessionId, modelName);
        
        log.info("初始化RAG会话Token管理: sessionId={}, userId={}, model={}", 
                sessionId, userId, modelName);
    }
    
    /**
     * 处理RAG查询的Token管理
     * 
     * @param sessionId 会话ID
     * @param question 问题内容
     * @param context 检索上下文
     * @return Token预测结果
     */
    public TokenPredictor.TokenPrediction processQueryTokens(String sessionId, String question, 
                                                              String context) {
        String modelName = getModelName(sessionId);
        
        // 记录提问Token
        int questionTokens = sessionTokenManager.recordQuestionTokens(sessionId, question);
        
        // 增量计算上下文Token
        int contextTokens = 0;
        if (context != null && !context.isEmpty()) {
            contextTokens = contextOptimizer.calculateIncrementalTokens(sessionId, context, false);
        }
        
        // 预测回答Token
        TokenPredictor.TokenPrediction prediction = tokenPredictor.predictAnswerTokens(
                question, context, modelName);
        
        // 检查是否需要优化上下文
        if (contextOptimizer.needsOptimization(sessionId, 0.9)) {
            log.warn("上下文接近Token限制，建议优化: sessionId={}", sessionId);
        }
        
        log.debug("处理查询Token: sessionId={}, questionTokens={}, contextTokens={}", 
                sessionId, questionTokens, contextTokens);
        
        return prediction;
    }
    
    /**
     * 处理流式回答Token
     * 
     * @param sessionId 会话ID
     * @param answerChunk 回答片段
     * @return 当前总Token数
     */
    public int processAnswerChunk(String sessionId, String answerChunk) {
        // 流式计数
        int totalTokens = streamingTokenCounter.incrementTokens(sessionId, answerChunk);
        
        // 增量上下文更新
        contextOptimizer.calculateIncrementalTokens(sessionId, answerChunk, false);
        
        return totalTokens;
    }
    
    /**
     * 完成RAG会话
     * 
     * @param sessionId 会话ID
     * @param userId 用户ID
     * @return Token使用摘要
     */
    public TokenUsageSummary completeSession(String sessionId, String userId) {
        // 完成流式计数
        StreamingTokenCounter.StreamingTokenStats streamingStats = 
                streamingTokenCounter.finishStreaming(sessionId);
        
        // 完成会话管理
        SessionTokenManager.SessionTokenStats sessionStats = 
                sessionTokenManager.endSession(sessionId);
        
        // 记录使用情况
        if (sessionStats != null) {
            usageAnalyzer.recordUsage(
                    userId,
                    sessionId,
                    sessionStats.getModelName(),
                    sessionStats.getQuestionTokens(),
                    sessionStats.getAnswerTokens()
            );
        }
        
        // 清理上下文缓存
        contextOptimizer.clearContextCache(sessionId);
        
        // 生成摘要
        if (sessionStats != null) {
            return new TokenUsageSummary(
                    sessionId,
                    userId,
                    sessionStats.getTotalTokens(),
                    sessionStats.getQuestionTokens(),
                    sessionStats.getAnswerTokens(),
                    tokenCounter.getMaxContextTokens(sessionStats.getModelName()),
                    sessionStats.getModelName(),
                    sessionStats.getStartTime(),
                    sessionStats.getEndTime()
            );
        }
        
        return null;
    }
    
    /**
     * 智能优化上下文
     * 
     * @param sessionId 会话ID
     * @param maxTokens 最大Token限制
     * @return 优化后的上下文
     */
    public IncrementalContextOptimizer.OptimizedContext optimizeContext(String sessionId, int maxTokens) {
        return contextOptimizer.optimizeContext(sessionId, maxTokens);
    }
    
    /**
     * 动态调整检索数量
     * 
     * @param sessionId 会话ID
     * @param question 问题
     * @param requestedTopK 请求的TopK
     * @return 调整后的TopK
     */
    public int adjustRetrievalTopK(String sessionId, String question, int requestedTopK) {
        String modelName = getModelName(sessionId);
        int maxContextTokens = tokenCounter.getMaxContextTokens(modelName);
        
        return tokenPredictor.adjustRetrievalTopK(question, requestedTopK, modelName, maxContextTokens);
    }
    
    /**
     * 预测上下文窗口使用情况
     * 
     * @param sessionId 会话ID
     * @param question 问题
     * @param context 上下文
     * @return 预测结果
     */
    public TokenPredictor.ContextWindowPrediction predictContextWindow(String sessionId, 
                                                                        String question, 
                                                                        String context) {
        String modelName = getModelName(sessionId);
        return tokenPredictor.predictContextWindowExceeded(question, context, modelName);
    }
    
    /**
     * 获取Token使用统计
     * 
     * @param userId 用户ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 用户Token统计
     */
    public TokenUsageAnalyzer.UserTokenStatistics getUserTokenStats(String userId, 
                                                                     LocalDateTime startTime, 
                                                                     LocalDateTime endTime) {
        return usageAnalyzer.getUserStatistics(userId, startTime, endTime);
    }
    
    /**
     * 获取使用趋势分析
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 趋势分析
     */
    public TokenUsageAnalyzer.UsageTrendAnalysis getUsageTrend(LocalDateTime startTime, 
                                                                LocalDateTime endTime) {
        return usageAnalyzer.analyzeTrend(startTime, endTime);
    }
    
    /**
     * 获取Token使用报告
     * 
     * @param userId 用户ID（可选）
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 使用报告
     */
    public TokenUsageAnalyzer.UsageReport getUsageReport(String userId, 
                                                          LocalDateTime startTime, 
                                                          LocalDateTime endTime) {
        return usageAnalyzer.generateReport(userId, startTime, endTime);
    }
    
    /**
     * 检测异常Token使用
     * 
     * @param userId 用户ID
     * @return 异常记录列表
     */
    public List<TokenUsageAnalyzer.AnomalyRecord> detectAnomalies(String userId) {
        return usageAnalyzer.detectAnomalies(userId);
    }
    
    /**
     * 获取Token优化建议
     * 
     * @param question 问题
     * @param context 上下文
     * @param modelName 模型名称
     * @return 优化建议列表
     */
    public List<TokenPredictor.TokenOptimizationSuggestion> getTokenOptimizationSuggestions(
            String question, String context, String modelName) {
        return tokenPredictor.getOptimizationSuggestions(question, context, modelName);
    }
    
    /**
     * 获取实时Token使用信息
     * 
     * @param sessionId 会话ID
     * @return Token使用信息
     */
    public StreamingTokenCounter.TokenUsageInfo getRealTimeTokenInfo(String sessionId) {
        return streamingTokenCounter.getUsageInfo(sessionId);
    }
    
    /**
     * 注册流式Token监听器
     * 
     * @param sessionId 会话ID
     * @param listener 监听器
     */
    public void registerStreamingListener(String sessionId, 
                                          java.util.function.Consumer<StreamingTokenCounter.TokenUsageEvent> listener) {
        streamingTokenCounter.registerListener(sessionId, listener);
    }
    
    /**
     * 获取会话Token统计
     * 
     * @param sessionId 会话ID
     * @return 会话Token统计
     */
    public SessionTokenManager.SessionTokenStats getSessionStats(String sessionId) {
        return sessionTokenManager.getSessionStats(sessionId);
    }
    
    /**
     * 设置用户Token配额
     * 
     * @param userId 用户ID
     * @param dailyLimit 每日限制
     */
    public void setUserTokenQuota(String userId, int dailyLimit) {
        sessionTokenManager.setUserQuota(userId, dailyLimit);
    }
    
    /**
     * 检查会话Token限制
     * 
     * @param sessionId 会话ID
     * @return true-超出限制
     */
    public boolean isSessionTokenLimitExceeded(String sessionId) {
        return sessionTokenManager.isSessionTokenLimitExceeded(sessionId);
    }
    
    // ============= 私有方法 =============
    
    private String getModelName(String sessionId) {
        SessionTokenManager.SessionTokenStats stats = sessionTokenManager.getSessionStats(sessionId);
        return stats != null ? stats.getModelName() : "gpt-3.5-turbo";
    }
    
    /**
     * Token使用摘要
     */
    public static class TokenUsageSummary {
        private final String sessionId;
        private final String userId;
        private final int totalTokens;
        private final int questionTokens;
        private final int answerTokens;
        private final int maxContextTokens;
        private final String modelName;
        private final LocalDateTime startTime;
        private final LocalDateTime endTime;
        
        public TokenUsageSummary(String sessionId, String userId, int totalTokens, 
                                int questionTokens, int answerTokens, int maxContextTokens,
                                String modelName, LocalDateTime startTime, LocalDateTime endTime) {
            this.sessionId = sessionId;
            this.userId = userId;
            this.totalTokens = totalTokens;
            this.questionTokens = questionTokens;
            this.answerTokens = answerTokens;
            this.maxContextTokens = maxContextTokens;
            this.modelName = modelName;
            this.startTime = startTime;
            this.endTime = endTime;
        }
        
        // Getters
        public String getSessionId() { return sessionId; }
        public String getUserId() { return userId; }
        public int getTotalTokens() { return totalTokens; }
        public int getQuestionTokens() { return questionTokens; }
        public int getAnswerTokens() { return answerTokens; }
        public int getMaxContextTokens() { return maxContextTokens; }
        public String getModelName() { return modelName; }
        public LocalDateTime getStartTime() { return startTime; }
        public LocalDateTime getEndTime() { return endTime; }
        
        public double getUsagePercentage() {
            return maxContextTokens > 0 ? (double) totalTokens / maxContextTokens * 100 : 0;
        }
        
        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("sessionId", sessionId);
            map.put("userId", userId);
            map.put("totalTokens", totalTokens);
            map.put("questionTokens", questionTokens);
            map.put("answerTokens", answerTokens);
            map.put("maxContextTokens", maxContextTokens);
            map.put("modelName", modelName);
            map.put("usagePercentage", String.format("%.2f%%", getUsagePercentage()));
            map.put("startTime", startTime);
            map.put("endTime", endTime);
            return map;
        }
    }
}
