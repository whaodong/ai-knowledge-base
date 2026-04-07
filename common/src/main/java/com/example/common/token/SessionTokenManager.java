package com.example.common.token;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 会话Token管理器
 * 
 * <p>管理会话级别的Token统计，包括提问和回答的Token累计、配额管理、超限预警等功能。</p>
 * <p>功能特点：</p>
 * <ul>
 *   <li>会话Token累计统计</li>
 *   <li>Token配额管理</li>
 *   <li>超限预警和截断</li>
 *   <li>会话Token历史记录</li>
 *   <li>多用户配额隔离</li>
 * </ul>
 */
@Slf4j
@Component
public class SessionTokenManager {
    
    private final TokenCounter tokenCounter;
    
    // 会话Token统计：sessionId -> SessionTokenStats
    private final Map<String, SessionTokenStats> sessionStats = new ConcurrentHashMap<>();
    
    // 用户Token配额：userId -> UserTokenQuota
    private final Map<String, UserTokenQuota> userQuotas = new ConcurrentHashMap<>();
    
    // Token使用历史：sessionId -> 历史记录列表
    private final Map<String, List<TokenUsageRecord>> tokenHistory = new ConcurrentHashMap<>();
    
    // 默认配额设置
    private static final int DEFAULT_DAILY_QUOTA = 100000; // 每日10万Token
    private static final int DEFAULT_SESSION_QUOTA = 8000; // 每会话8000Token
    private static final double WARNING_THRESHOLD = 0.8; // 80%预警
    private static final int MAX_HISTORY_RECORDS = 1000; // 最大历史记录数
    
    public SessionTokenManager(TokenCounter tokenCounter) {
        this.tokenCounter = tokenCounter;
    }
    
    /**
     * 开始新会话
     * 
     * @param sessionId 会话ID
     * @param userId 用户ID
     * @param modelName 模型名称
     * @return 会话Token统计
     */
    public SessionTokenStats startSession(String sessionId, String userId, String modelName) {
        // 检查用户配额
        UserTokenQuota quota = getUserQuota(userId);
        if (!quota.hasRemainingQuota()) {
            log.warn("用户Token配额已用完: userId={}, used={}, limit={}", 
                    userId, quota.getUsedTokens(), quota.getDailyLimit());
            throw new TokenQuotaExceededException("用户Token配额已用完");
        }
        
        // 创建会话统计
        SessionTokenStats stats = new SessionTokenStats(sessionId, userId, modelName);
        sessionStats.put(sessionId, stats);
        
        // 初始化历史记录
        tokenHistory.put(sessionId, new ArrayList<>());
        
        log.info("开始会话Token管理: sessionId={}, userId={}, model={}", 
                sessionId, userId, modelName);
        
        return stats;
    }
    
    /**
     * 记录提问Token
     * 
     * @param sessionId 会话ID
     * @param question 提问内容
     * @return 提问Token数
     */
    public int recordQuestionTokens(String sessionId, String question) {
        SessionTokenStats stats = sessionStats.get(sessionId);
        if (stats == null) {
            log.warn("未找到会话统计: {}", sessionId);
            return 0;
        }
        
        // 计算Token
        int tokens = tokenCounter.countTokens(question, stats.getModelName());
        
        // 更新统计
        stats.addQuestionTokens(tokens);
        stats.addTotalTokens(tokens);
        
        // 更新用户配额
        UserTokenQuota quota = getUserQuota(stats.getUserId());
        quota.useTokens(tokens);
        
        // 记录历史
        addHistoryRecord(sessionId, "question", question, tokens);
        
        // 检查配额
        checkQuotaWarning(sessionId, stats, quota);
        
        log.debug("记录提问Token: sessionId={}, tokens={}, total={}", 
                sessionId, tokens, stats.getTotalTokens());
        
        return tokens;
    }
    
    /**
     * 记录回答Token
     * 
     * @param sessionId 会话ID
     * @param answer 回答内容
     * @return 回答Token数
     */
    public int recordAnswerTokens(String sessionId, String answer) {
        SessionTokenStats stats = sessionStats.get(sessionId);
        if (stats == null) {
            log.warn("未找到会话统计: {}", sessionId);
            return 0;
        }
        
        // 计算Token
        int tokens = tokenCounter.countTokens(answer, stats.getModelName());
        
        // 更新统计
        stats.addAnswerTokens(tokens);
        stats.addTotalTokens(tokens);
        
        // 更新用户配额
        UserTokenQuota quota = getUserQuota(stats.getUserId());
        quota.useTokens(tokens);
        
        // 记录历史
        addHistoryRecord(sessionId, "answer", answer, tokens);
        
        log.debug("记录回答Token: sessionId={}, tokens={}, total={}", 
                sessionId, tokens, stats.getTotalTokens());
        
        return tokens;
    }
    
    /**
     * 结束会话
     * 
     * @param sessionId 会话ID
     * @return 最终会话统计
     */
    public SessionTokenStats endSession(String sessionId) {
        SessionTokenStats stats = sessionStats.remove(sessionId);
        if (stats != null) {
            stats.endSession();
            
            // 限制历史记录数
            List<TokenUsageRecord> history = tokenHistory.get(sessionId);
            if (history != null && history.size() > MAX_HISTORY_RECORDS) {
                tokenHistory.put(sessionId, history.subList(
                        history.size() - MAX_HISTORY_RECORDS, history.size()));
            }
            
            log.info("结束会话Token管理: sessionId={}, totalTokens={}, questionTokens={}, answerTokens={}", 
                    sessionId, stats.getTotalTokens(), 
                    stats.getQuestionTokens(), stats.getAnswerTokens());
        }
        return stats;
    }
    
    /**
     * 获取会话Token统计
     * 
     * @param sessionId 会话ID
     * @return 会话Token统计
     */
    public SessionTokenStats getSessionStats(String sessionId) {
        return sessionStats.get(sessionId);
    }
    
    /**
     * 获取用户Token配额
     * 
     * @param userId 用户ID
     * @return 用户Token配额
     */
    public UserTokenQuota getUserQuota(String userId) {
        return userQuotas.computeIfAbsent(userId, id -> 
                new UserTokenQuota(userId, DEFAULT_DAILY_QUOTA));
    }
    
    /**
     * 设置用户Token配额
     * 
     * @param userId 用户ID
     * @param dailyLimit 每日限制
     */
    public void setUserQuota(String userId, int dailyLimit) {
        UserTokenQuota quota = userQuotas.getOrDefault(userId, 
                new UserTokenQuota(userId, dailyLimit));
        quota.setDailyLimit(dailyLimit);
        userQuotas.put(userId, quota);
        log.info("设置用户Token配额: userId={}, dailyLimit={}", userId, dailyLimit);
    }
    
    /**
     * 检查是否超出会话Token限制
     * 
     * @param sessionId 会话ID
     * @return true-超出限制，false-未超出
     */
    public boolean isSessionTokenLimitExceeded(String sessionId) {
        SessionTokenStats stats = sessionStats.get(sessionId);
        if (stats == null) {
            return false;
        }
        
        int maxContextTokens = tokenCounter.getMaxContextTokens(stats.getModelName());
        return stats.getTotalTokens() >= maxContextTokens * WARNING_THRESHOLD;
    }
    
    /**
     * 获取会话剩余Token数
     * 
     * @param sessionId 会话ID
     * @return 剩余Token数
     */
    public int getRemainingSessionTokens(String sessionId) {
        SessionTokenStats stats = sessionStats.get(sessionId);
        if (stats == null) {
            return DEFAULT_SESSION_QUOTA;
        }
        
        int maxContextTokens = tokenCounter.getMaxContextTokens(stats.getModelName());
        return Math.max(0, maxContextTokens - stats.getTotalTokens());
    }
    
    /**
     * 获取会话Token历史
     * 
     * @param sessionId 会话ID
     * @return Token使用历史
     */
    public List<TokenUsageRecord> getSessionHistory(String sessionId) {
        return tokenHistory.getOrDefault(sessionId, Collections.emptyList());
    }
    
    /**
     * 重置用户每日配额
     * 
     * @param userId 用户ID
     */
    public void resetUserDailyQuota(String userId) {
        UserTokenQuota quota = userQuotas.get(userId);
        if (quota != null) {
            quota.reset();
            log.info("重置用户每日Token配额: userId={}", userId);
        }
    }
    
    /**
     * 重置所有用户每日配额（定时任务调用）
     */
    public void resetAllDailyQuotas() {
        userQuotas.values().forEach(UserTokenQuota::reset);
        log.info("重置所有用户每日Token配额");
    }
    
    /**
     * 获取Token使用摘要
     * 
     * @param sessionId 会话ID
     * @return Token使用摘要
     */
    public TokenUsageSummary getUsageSummary(String sessionId) {
        SessionTokenStats stats = sessionStats.get(sessionId);
        if (stats == null) {
            return null;
        }
        
        int maxContextTokens = tokenCounter.getMaxContextTokens(stats.getModelName());
        double usagePercentage = (double) stats.getTotalTokens() / maxContextTokens * 100;
        
        return new TokenUsageSummary(
                sessionId,
                stats.getUserId(),
                stats.getTotalTokens(),
                stats.getQuestionTokens(),
                stats.getAnswerTokens(),
                maxContextTokens,
                usagePercentage,
                stats.getModelName(),
                stats.getStartTime(),
                stats.getEndTime()
        );
    }
    
    // ============= 私有方法 =============
    
    /**
     * 添加历史记录
     */
    private void addHistoryRecord(String sessionId, String type, String content, int tokens) {
        List<TokenUsageRecord> history = tokenHistory.computeIfAbsent(
                sessionId, k -> new ArrayList<>());
        
        history.add(new TokenUsageRecord(
                sessionId,
                type,
                content.length() > 100 ? content.substring(0, 100) + "..." : content,
                tokens,
                LocalDateTime.now()
        ));
    }
    
    /**
     * 检查配额预警
     */
    private void checkQuotaWarning(String sessionId, SessionTokenStats stats, UserTokenQuota quota) {
        // 检查会话配额
        int maxContextTokens = tokenCounter.getMaxContextTokens(stats.getModelName());
        double sessionUsageRate = (double) stats.getTotalTokens() / maxContextTokens;
        
        if (sessionUsageRate >= WARNING_THRESHOLD) {
            log.warn("会话Token使用达到预警阈值: sessionId={}, usage={}/{} ({:.2f}%)", 
                    sessionId, stats.getTotalTokens(), maxContextTokens, sessionUsageRate * 100);
        }
        
        // 检查用户配额
        double userUsageRate = (double) quota.getUsedTokens() / quota.getDailyLimit();
        
        if (userUsageRate >= WARNING_THRESHOLD) {
            log.warn("用户Token配额使用达到预警阈值: userId={}, used={}/{} ({:.2f}%)", 
                    stats.getUserId(), quota.getUsedTokens(), quota.getDailyLimit(), userUsageRate * 100);
        }
    }
    
    // ============= 内部类 =============
    
    /**
     * 会话Token统计
     */
    public static class SessionTokenStats {
        private final String sessionId;
        private final String userId;
        private final String modelName;
        private final LocalDateTime startTime;
        private LocalDateTime endTime;
        private final AtomicInteger totalTokens = new AtomicInteger(0);
        private final AtomicInteger questionTokens = new AtomicInteger(0);
        private final AtomicInteger answerTokens = new AtomicInteger(0);
        private int messageCount = 0;
        
        public SessionTokenStats(String sessionId, String userId, String modelName) {
            this.sessionId = sessionId;
            this.userId = userId;
            this.modelName = modelName;
            this.startTime = LocalDateTime.now();
        }
        
        public void addTotalTokens(int tokens) {
            totalTokens.addAndGet(tokens);
        }
        
        public void addQuestionTokens(int tokens) {
            questionTokens.addAndGet(tokens);
            messageCount++;
        }
        
        public void addAnswerTokens(int tokens) {
            answerTokens.addAndGet(tokens);
        }
        
        public void endSession() {
            this.endTime = LocalDateTime.now();
        }
        
        // Getters
        public String getSessionId() { return sessionId; }
        public String getUserId() { return userId; }
        public String getModelName() { return modelName; }
        public LocalDateTime getStartTime() { return startTime; }
        public LocalDateTime getEndTime() { return endTime; }
        public int getTotalTokens() { return totalTokens.get(); }
        public int getQuestionTokens() { return questionTokens.get(); }
        public int getAnswerTokens() { return answerTokens.get(); }
        public int getMessageCount() { return messageCount; }
    }
    
    /**
     * 用户Token配额
     */
    public static class UserTokenQuota {
        private final String userId;
        private int dailyLimit;
        private final AtomicInteger usedTokens = new AtomicInteger(0);
        private LocalDateTime lastResetTime;
        
        public UserTokenQuota(String userId, int dailyLimit) {
            this.userId = userId;
            this.dailyLimit = dailyLimit;
            this.lastResetTime = LocalDateTime.now();
        }
        
        public void useTokens(int tokens) {
            usedTokens.addAndGet(tokens);
        }
        
        public void reset() {
            usedTokens.set(0);
            lastResetTime = LocalDateTime.now();
        }
        
        public boolean hasRemainingQuota() {
            return usedTokens.get() < dailyLimit;
        }
        
        public int getRemainingTokens() {
            return Math.max(0, dailyLimit - usedTokens.get());
        }
        
        // Getters and Setters
        public String getUserId() { return userId; }
        public int getDailyLimit() { return dailyLimit; }
        public void setDailyLimit(int dailyLimit) { this.dailyLimit = dailyLimit; }
        public int getUsedTokens() { return usedTokens.get(); }
        public LocalDateTime getLastResetTime() { return lastResetTime; }
    }
    
    /**
     * Token使用记录
     */
    public static class TokenUsageRecord {
        private final String sessionId;
        private final String type; // question, answer
        private final String content;
        private final int tokens;
        private final LocalDateTime timestamp;
        
        public TokenUsageRecord(String sessionId, String type, String content, 
                                int tokens, LocalDateTime timestamp) {
            this.sessionId = sessionId;
            this.type = type;
            this.content = content;
            this.tokens = tokens;
            this.timestamp = timestamp;
        }
        
        // Getters
        public String getSessionId() { return sessionId; }
        public String getType() { return type; }
        public String getContent() { return content; }
        public int getTokens() { return tokens; }
        public LocalDateTime getTimestamp() { return timestamp; }
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
        private final int maxTokens;
        private final double usagePercentage;
        private final String modelName;
        private final LocalDateTime startTime;
        private final LocalDateTime endTime;
        
        public TokenUsageSummary(String sessionId, String userId, int totalTokens, 
                                 int questionTokens, int answerTokens, int maxTokens, 
                                 double usagePercentage, String modelName, 
                                 LocalDateTime startTime, LocalDateTime endTime) {
            this.sessionId = sessionId;
            this.userId = userId;
            this.totalTokens = totalTokens;
            this.questionTokens = questionTokens;
            this.answerTokens = answerTokens;
            this.maxTokens = maxTokens;
            this.usagePercentage = usagePercentage;
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
        public int getMaxTokens() { return maxTokens; }
        public double getUsagePercentage() { return usagePercentage; }
        public String getModelName() { return modelName; }
        public LocalDateTime getStartTime() { return startTime; }
        public LocalDateTime getEndTime() { return endTime; }
    }
    
    /**
     * Token配额超出异常
     */
    public static class TokenQuotaExceededException extends RuntimeException {
        public TokenQuotaExceededException(String message) {
            super(message);
        }
    }
}
