package com.example.common.token;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * 流式Token计数器
 * 
 * <p>实时监听流式响应事件，增量统计Token使用量。</p>
 * <p>功能特点：</p>
 * <ul>
 *   <li>实时Token累加统计</li>
 *   <li>SSE流式输出支持</li>
 *   <li>Token使用量预警</li>
 *   <li>性能监控</li>
 * </ul>
 */
@Slf4j
@Component
public class StreamingTokenCounter {
    
    private final TokenCounter tokenCounter;
    
    // 流式会话Token统计：sessionId -> Token统计
    private final Map<String, StreamingTokenStats> streamingStats = new ConcurrentHashMap<>();
    
    // Token使用监听器
    private final Map<String, Consumer<TokenUsageEvent>> listeners = new ConcurrentHashMap<>();
    
    // 预警阈值（默认80%）
    private static final double WARNING_THRESHOLD = 0.8;
    private static final double CRITICAL_THRESHOLD = 0.95;
    
    public StreamingTokenCounter(TokenCounter tokenCounter) {
        this.tokenCounter = tokenCounter;
    }
    
    /**
     * 开始流式Token计数
     * 
     * @param sessionId 会话ID
     * @param modelName 模型名称
     * @return 流式Token统计对象
     */
    public StreamingTokenStats startStreaming(String sessionId, String modelName) {
        StreamingTokenStats stats = new StreamingTokenStats(sessionId, modelName);
        streamingStats.put(sessionId, stats);
        log.info("开始流式Token计数: sessionId={}, model={}", sessionId, modelName);
        return stats;
    }
    
    /**
     * 增量统计Token
     * 
     * @param sessionId 会话ID
     * @param content 新增内容
     * @return 当前总Token数
     */
    public int incrementTokens(String sessionId, String content) {
        StreamingTokenStats stats = streamingStats.get(sessionId);
        if (stats == null) {
            log.warn("未找到会话的流式统计: {}", sessionId);
            return 0;
        }
        
        // 计算增量Token
        int incrementalTokens = tokenCounter.countTokens(content, stats.getModelName());
        int totalTokens = stats.addTokens(incrementalTokens);
        
        // 触发监听器
        notifyListeners(sessionId, content, incrementalTokens, totalTokens, stats);
        
        // 检查预警
        checkThresholds(sessionId, stats);
        
        log.debug("增量Token统计: sessionId={}, incremental={}, total={}", 
                sessionId, incrementalTokens, totalTokens);
        
        return totalTokens;
    }
    
    /**
     * 批量增量统计（优化性能）
     * 
     * @param sessionId 会话ID
     * @param contents 内容列表
     * @return 当前总Token数
     */
    public int incrementTokensBatch(String sessionId, Iterable<String> contents) {
        StreamingTokenStats stats = streamingStats.get(sessionId);
        if (stats == null) {
            log.warn("未找到会话的流式统计: {}", sessionId);
            return 0;
        }
        
        // 批量计算Token
        int[] tokenCounts = tokenCounter.batchCountTokens(contents, stats.getModelName());
        int totalIncremental = 0;
        for (int count : tokenCounts) {
            totalIncremental += count;
        }
        
        int totalTokens = stats.addTokens(totalIncremental);
        
        log.debug("批量增量Token统计: sessionId={}, incremental={}, total={}", 
                sessionId, totalIncremental, totalTokens);
        
        return totalTokens;
    }
    
    /**
     * 完成流式计数
     * 
     * @param sessionId 会话ID
     * @return 最终Token统计
     */
    public StreamingTokenStats finishStreaming(String sessionId) {
        StreamingTokenStats stats = streamingStats.remove(sessionId);
        if (stats != null) {
            stats.finish();
            listeners.remove(sessionId);
            log.info("完成流式Token计数: sessionId={}, totalTokens={}, duration={}ms", 
                    sessionId, stats.getTotalTokens(), 
                    stats.getDuration() != null ? stats.getDuration().toMillis() : 0);
        }
        return stats;
    }
    
    /**
     * 注册Token使用监听器
     * 
     * @param sessionId 会话ID
     * @param listener 监听器
     */
    public void registerListener(String sessionId, Consumer<TokenUsageEvent> listener) {
        listeners.put(sessionId, listener);
    }
    
    /**
     * 获取当前Token统计
     * 
     * @param sessionId 会话ID
     * @return Token统计
     */
    public StreamingTokenStats getStats(String sessionId) {
        return streamingStats.get(sessionId);
    }
    
    /**
     * 实时显示Token使用量（用于SSE）
     * 
     * @param sessionId 会话ID
     * @return Token使用信息
     */
    public TokenUsageInfo getUsageInfo(String sessionId) {
        StreamingTokenStats stats = streamingStats.get(sessionId);
        if (stats == null) {
            return null;
        }
        
        int maxTokens = tokenCounter.getMaxContextTokens(stats.getModelName());
        int remainingTokens = tokenCounter.getRemainingTokens(stats.getTotalTokens(), stats.getModelName());
        double usagePercentage = (double) stats.getTotalTokens() / maxTokens * 100;
        
        return new TokenUsageInfo(
                stats.getTotalTokens(),
                maxTokens,
                remainingTokens,
                usagePercentage,
                stats.getModelName(),
                LocalDateTime.now()
        );
    }
    
    // ============= 私有方法 =============
    
    /**
     * 通知监听器
     */
    private void notifyListeners(String sessionId, String content, int incrementalTokens, 
                                  int totalTokens, StreamingTokenStats stats) {
        Consumer<TokenUsageEvent> listener = listeners.get(sessionId);
        if (listener != null) {
            try {
                TokenUsageEvent event = new TokenUsageEvent(
                        sessionId,
                        content,
                        incrementalTokens,
                        totalTokens,
                        stats.getModelName(),
                        LocalDateTime.now()
                );
                listener.accept(event);
            } catch (Exception e) {
                log.error("监听器处理失败: sessionId={}", sessionId, e);
            }
        }
    }
    
    /**
     * 检查阈值预警
     */
    private void checkThresholds(String sessionId, StreamingTokenStats stats) {
        int maxTokens = tokenCounter.getMaxContextTokens(stats.getModelName());
        double usageRate = (double) stats.getTotalTokens() / maxTokens;
        
        if (usageRate >= CRITICAL_THRESHOLD) {
            log.warn("Token使用率达到临界阈值: sessionId={}, usage={}/{} ({:.2f}%)", 
                    sessionId, stats.getTotalTokens(), maxTokens, usageRate * 100);
        } else if (usageRate >= WARNING_THRESHOLD) {
            log.warn("Token使用率达到预警阈值: sessionId={}, usage={}/{} ({:.2f}%)", 
                    sessionId, stats.getTotalTokens(), maxTokens, usageRate * 100);
        }
    }
    
    /**
     * 流式Token统计
     */
    public static class StreamingTokenStats {
        private final String sessionId;
        private final String modelName;
        private final AtomicInteger totalTokens = new AtomicInteger(0);
        private final LocalDateTime startTime;
        private LocalDateTime endTime;
        private int inputTokens = 0;
        private int outputTokens = 0;
        
        public StreamingTokenStats(String sessionId, String modelName) {
            this.sessionId = sessionId;
            this.modelName = modelName;
            this.startTime = LocalDateTime.now();
        }
        
        public int addTokens(int tokens) {
            return totalTokens.addAndGet(tokens);
        }
        
        public void finish() {
            this.endTime = LocalDateTime.now();
        }
        
        public int getTotalTokens() {
            return totalTokens.get();
        }
        
        public String getSessionId() {
            return sessionId;
        }
        
        public String getModelName() {
            return modelName;
        }
        
        public LocalDateTime getStartTime() {
            return startTime;
        }
        
        public LocalDateTime getEndTime() {
            return endTime;
        }
        
        public java.time.Duration getDuration() {
            if (endTime != null) {
                return java.time.Duration.between(startTime, endTime);
            }
            return java.time.Duration.between(startTime, LocalDateTime.now());
        }
        
        public int getInputTokens() {
            return inputTokens;
        }
        
        public void setInputTokens(int inputTokens) {
            this.inputTokens = inputTokens;
        }
        
        public int getOutputTokens() {
            return outputTokens;
        }
        
        public void setOutputTokens(int outputTokens) {
            this.outputTokens = outputTokens;
        }
    }
    
    /**
     * Token使用事件
     */
    public static class TokenUsageEvent {
        private final String sessionId;
        private final String content;
        private final int incrementalTokens;
        private final int totalTokens;
        private final String modelName;
        private final LocalDateTime timestamp;
        
        public TokenUsageEvent(String sessionId, String content, int incrementalTokens, 
                               int totalTokens, String modelName, LocalDateTime timestamp) {
            this.sessionId = sessionId;
            this.content = content;
            this.incrementalTokens = incrementalTokens;
            this.totalTokens = totalTokens;
            this.modelName = modelName;
            this.timestamp = timestamp;
        }
        
        // Getters
        public String getSessionId() { return sessionId; }
        public String getContent() { return content; }
        public int getIncrementalTokens() { return incrementalTokens; }
        public int getTotalTokens() { return totalTokens; }
        public String getModelName() { return modelName; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }
    
    /**
     * Token使用信息（用于SSE输出）
     */
    public static class TokenUsageInfo {
        private final int usedTokens;
        private final int maxTokens;
        private final int remainingTokens;
        private final double usagePercentage;
        private final String modelName;
        private final LocalDateTime timestamp;
        
        public TokenUsageInfo(int usedTokens, int maxTokens, int remainingTokens, 
                              double usagePercentage, String modelName, LocalDateTime timestamp) {
            this.usedTokens = usedTokens;
            this.maxTokens = maxTokens;
            this.remainingTokens = remainingTokens;
            this.usagePercentage = usagePercentage;
            this.modelName = modelName;
            this.timestamp = timestamp;
        }
        
        // Getters
        public int getUsedTokens() { return usedTokens; }
        public int getMaxTokens() { return maxTokens; }
        public int getRemainingTokens() { return remainingTokens; }
        public double getUsagePercentage() { return usagePercentage; }
        public String getModelName() { return modelName; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }
}
