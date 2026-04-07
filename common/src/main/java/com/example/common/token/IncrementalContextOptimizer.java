package com.example.common.token;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 增量式上下文优化器
 * 
 * <p>实现增量式上下文优化，包括新增内容增量计算、差量Token计算、智能保留策略和历史Token复用。</p>
 * <p>功能特点：</p>
 * <ul>
 *   <li>新增内容增量Token计算</li>
 *   <li>差量Token计算优化</li>
 *   <li>智能上下文保留策略</li>
 *   <li>历史Token缓存复用</li>
 *   <li>上下文滑动窗口管理</li>
 * </ul>
 */
@Slf4j
@Component
public class IncrementalContextOptimizer {
    
    private final TokenCounter tokenCounter;
    
    // 上下文缓存：sessionId -> ContextCache
    private final Map<String, ContextCache> contextCaches = new ConcurrentHashMap<>();
    
    // Token缓存：文本hash -> Token数
    private final Map<String, Integer> tokenCache = new ConcurrentHashMap<>();
    
    // 配置参数
    private static final int MAX_CACHE_SIZE = 10000;
    private static final double RETENTION_RATIO = 0.3; // 保留30%的历史上下文
    private static final int MIN_RETAINED_TOKENS = 500; // 最少保留500个Token的历史
    private static final double IMPORTANCE_THRESHOLD = 0.7; // 重要性阈值
    
    public IncrementalContextOptimizer(TokenCounter tokenCounter) {
        this.tokenCounter = tokenCounter;
    }
    
    /**
     * 初始化上下文缓存
     * 
     * @param sessionId 会话ID
     * @param modelName 模型名称
     */
    public void initContextCache(String sessionId, String modelName) {
        ContextCache cache = new ContextCache(sessionId, modelName);
        contextCaches.put(sessionId, cache);
        log.info("初始化上下文缓存: sessionId={}, model={}", sessionId, modelName);
    }
    
    /**
     * 增量计算新增内容的Token
     * 
     * @param sessionId 会话ID
     * @param newContent 新增内容
     * @param isQuestion 是否为提问
     * @return 增量Token数
     */
    public int calculateIncrementalTokens(String sessionId, String newContent, boolean isQuestion) {
        ContextCache cache = contextCaches.get(sessionId);
        if (cache == null) {
            log.warn("未找到上下文缓存: {}", sessionId);
            return tokenCounter.countTokens(newContent);
        }
        
        // 检查缓存
        String contentHash = generateHash(newContent);
        Integer cachedTokens = tokenCache.get(contentHash);
        
        int tokens;
        if (cachedTokens != null) {
            tokens = cachedTokens;
            log.debug("使用缓存的Token数: hash={}, tokens={}", contentHash, tokens);
        } else {
            tokens = tokenCounter.countTokens(newContent, cache.getModelName());
            // 更新缓存
            if (tokenCache.size() < MAX_CACHE_SIZE) {
                tokenCache.put(contentHash, tokens);
            }
        }
        
        // 更新上下文缓存
        if (isQuestion) {
            cache.addQuestion(newContent, tokens);
        } else {
            cache.addAnswer(newContent, tokens);
        }
        
        log.debug("增量Token计算: sessionId={}, isQuestion={}, tokens={}, total={}", 
                sessionId, isQuestion, tokens, cache.getTotalTokens());
        
        return tokens;
    }
    
    /**
     * 计算差量Token（优化上下文截断）
     * 
     * @param sessionId 会话ID
     * @param maxTokens 最大Token限制
     * @return 需要截断的Token数
     */
    public int calculateDeltaTokens(String sessionId, int maxTokens) {
        ContextCache cache = contextCaches.get(sessionId);
        if (cache == null) {
            return 0;
        }
        
        int currentTokens = cache.getTotalTokens();
        if (currentTokens <= maxTokens) {
            return 0;
        }
        
        return currentTokens - maxTokens;
    }
    
    /**
     * 智能优化上下文
     * 
     * @param sessionId 会话ID
     * @param maxTokens 最大Token限制
     * @return 优化后的上下文
     */
    public OptimizedContext optimizeContext(String sessionId, int maxTokens) {
        ContextCache cache = contextCaches.get(sessionId);
        if (cache == null) {
            log.warn("未找到上下文缓存: {}", sessionId);
            return new OptimizedContext("", 0, new ArrayList<>());
        }
        
        int currentTokens = cache.getTotalTokens();
        
        // 如果未超限，直接返回
        if (currentTokens <= maxTokens) {
            String fullContext = buildFullContext(cache);
            return new OptimizedContext(fullContext, currentTokens, cache.getContextSegments());
        }
        
        // 超限，需要优化
        log.info("上下文超限，开始优化: sessionId={}, current={}, max={}", 
                sessionId, currentTokens, maxTokens);
        
        return performOptimization(cache, maxTokens);
    }
    
    /**
     * 获取历史Token复用
     * 
     * @param sessionId 会话ID
     * @return 可复用的Token数
     */
    public int getReusableHistoricalTokens(String sessionId) {
        ContextCache cache = contextCaches.get(sessionId);
        if (cache == null) {
            return 0;
        }
        
        // 返回缓存的Token总数
        return cache.getContextSegments().stream()
                .filter(segment -> segment.getImportance() >= IMPORTANCE_THRESHOLD)
                .mapToInt(ContextSegment::getTokens)
                .sum();
    }
    
    /**
     * 清理上下文缓存
     * 
     * @param sessionId 会话ID
     */
    public void clearContextCache(String sessionId) {
        contextCaches.remove(sessionId);
        log.info("清理上下文缓存: sessionId={}", sessionId);
    }
    
    /**
     * 获取上下文统计信息
     * 
     * @param sessionId 会话ID
     * @return 上下文统计
     */
    public ContextStats getContextStats(String sessionId) {
        ContextCache cache = contextCaches.get(sessionId);
        if (cache == null) {
            return null;
        }
        
        return new ContextStats(
                sessionId,
                cache.getTotalTokens(),
                cache.getQuestionTokens(),
                cache.getAnswerTokens(),
                cache.getContextSegments().size(),
                cache.getModelName(),
                cache.getCreateTime()
        );
    }
    
    /**
     * 预测新增内容后的Token数
     * 
     * @param sessionId 会话ID
     * @param newContent 新增内容
     * @return 预测的总Token数
     */
    public int predictTotalTokens(String sessionId, String newContent) {
        ContextCache cache = contextCaches.get(sessionId);
        if (cache == null) {
            return tokenCounter.countTokens(newContent);
        }
        
        int newTokens = tokenCounter.countTokens(newContent, cache.getModelName());
        return cache.getTotalTokens() + newTokens;
    }
    
    /**
     * 检查是否需要优化
     * 
     * @param sessionId 会话ID
     * @param threshold 阈值（0-1）
     * @return true-需要优化
     */
    public boolean needsOptimization(String sessionId, double threshold) {
        ContextCache cache = contextCaches.get(sessionId);
        if (cache == null) {
            return false;
        }
        
        int maxTokens = tokenCounter.getMaxContextTokens(cache.getModelName());
        return cache.getTotalTokens() >= maxTokens * threshold;
    }
    
    // ============= 私有方法 =============
    
    /**
     * 执行上下文优化
     */
    private OptimizedContext performOptimization(ContextCache cache, int maxTokens) {
        List<ContextSegment> segments = cache.getContextSegments();
        
        // 计算需要保留的Token数
        int targetTokens = (int) (maxTokens * (1 - RETENTION_RATIO));
        int minRetained = Math.max(MIN_RETAINED_TOKENS, (int) (maxTokens * RETENTION_RATIO));
        
        // 按重要性排序
        List<ContextSegment> sortedSegments = new ArrayList<>(segments);
        sortedSegments.sort((a, b) -> Double.compare(b.getImportance(), a.getImportance()));
        
        // 选择要保留的片段
        List<ContextSegment> retainedSegments = new ArrayList<>();
        int retainedTokens = 0;
        
        for (ContextSegment segment : sortedSegments) {
            if (retainedTokens + segment.getTokens() <= targetTokens) {
                retainedSegments.add(segment);
                retainedTokens += segment.getTokens();
            }
            
            if (retainedTokens >= minRetained) {
                break;
            }
        }
        
        // 重新排序（按时间顺序）
        retainedSegments.sort(Comparator.comparingInt(ContextSegment::getIndex));
        
        // 构建优化后的上下文
        StringBuilder contextBuilder = new StringBuilder();
        for (ContextSegment segment : retainedSegments) {
            contextBuilder.append(segment.getContent()).append("\n\n");
        }
        
        String optimizedContext = contextBuilder.toString().trim();
        int finalTokens = tokenCounter.countTokens(optimizedContext, cache.getModelName());
        
        log.info("上下文优化完成: 原始segments={}, 保留segments={}, 原始tokens={}, 优化后tokens={}", 
                segments.size(), retainedSegments.size(), 
                cache.getTotalTokens(), finalTokens);
        
        return new OptimizedContext(optimizedContext, finalTokens, retainedSegments);
    }
    
    /**
     * 构建完整上下文
     */
    private String buildFullContext(ContextCache cache) {
        StringBuilder builder = new StringBuilder();
        for (ContextSegment segment : cache.getContextSegments()) {
            builder.append(segment.getContent()).append("\n\n");
        }
        return builder.toString().trim();
    }
    
    /**
     * 生成内容Hash
     */
    private String generateHash(String content) {
        return String.valueOf(content.hashCode());
    }
    
    // ============= 内部类 =============
    
    /**
     * 上下文缓存
     */
    public static class ContextCache {
        private final String sessionId;
        private final String modelName;
        private final LocalDateTime createTime;
        private final List<ContextSegment> contextSegments = new ArrayList<>();
        private int totalTokens = 0;
        private int questionTokens = 0;
        private int answerTokens = 0;
        private int segmentIndex = 0;
        
        public ContextCache(String sessionId, String modelName) {
            this.sessionId = sessionId;
            this.modelName = modelName;
            this.createTime = LocalDateTime.now();
        }
        
        public void addQuestion(String content, int tokens) {
            double importance = calculateImportance(content, true);
            contextSegments.add(new ContextSegment(
                    segmentIndex++, "question", content, tokens, importance, LocalDateTime.now()
            ));
            totalTokens += tokens;
            questionTokens += tokens;
        }
        
        public void addAnswer(String content, int tokens) {
            double importance = calculateImportance(content, false);
            contextSegments.add(new ContextSegment(
                    segmentIndex++, "answer", content, tokens, importance, LocalDateTime.now()
            ));
            totalTokens += tokens;
            answerTokens += tokens;
        }
        
        private double calculateImportance(String content, boolean isQuestion) {
            // 简单的重要性计算策略
            // 可以根据实际情况扩展：关键词密度、语义重要性等
            double baseImportance = isQuestion ? 0.8 : 0.6;
            
            // 长度因素
            if (content.length() > 200) {
                baseImportance += 0.1;
            }
            
            // 包含关键词
            if (content.contains("重要") || content.contains("关键") || content.contains("核心")) {
                baseImportance += 0.1;
            }
            
            return Math.min(1.0, baseImportance);
        }
        
        // Getters
        public String getSessionId() { return sessionId; }
        public String getModelName() { return modelName; }
        public LocalDateTime getCreateTime() { return createTime; }
        public List<ContextSegment> getContextSegments() { return contextSegments; }
        public int getTotalTokens() { return totalTokens; }
        public int getQuestionTokens() { return questionTokens; }
        public int getAnswerTokens() { return answerTokens; }
    }
    
    /**
     * 上下文片段
     */
    public static class ContextSegment {
        private final int index;
        private final String type; // question, answer
        private final String content;
        private final int tokens;
        private final double importance; // 0-1
        private final LocalDateTime timestamp;
        
        public ContextSegment(int index, String type, String content, 
                              int tokens, double importance, LocalDateTime timestamp) {
            this.index = index;
            this.type = type;
            this.content = content;
            this.tokens = tokens;
            this.importance = importance;
            this.timestamp = timestamp;
        }
        
        // Getters
        public int getIndex() { return index; }
        public String getType() { return type; }
        public String getContent() { return content; }
        public int getTokens() { return tokens; }
        public double getImportance() { return importance; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }
    
    /**
     * 优化后的上下文
     */
    public static class OptimizedContext {
        private final String context;
        private final int tokens;
        private final List<ContextSegment> segments;
        
        public OptimizedContext(String context, int tokens, List<ContextSegment> segments) {
            this.context = context;
            this.tokens = tokens;
            this.segments = segments;
        }
        
        // Getters
        public String getContext() { return context; }
        public int getTokens() { return tokens; }
        public List<ContextSegment> getSegments() { return segments; }
    }
    
    /**
     * 上下文统计
     */
    public static class ContextStats {
        private final String sessionId;
        private final int totalTokens;
        private final int questionTokens;
        private final int answerTokens;
        private final int segmentCount;
        private final String modelName;
        private final LocalDateTime createTime;
        
        public ContextStats(String sessionId, int totalTokens, int questionTokens, 
                           int answerTokens, int segmentCount, String modelName, 
                           LocalDateTime createTime) {
            this.sessionId = sessionId;
            this.totalTokens = totalTokens;
            this.questionTokens = questionTokens;
            this.answerTokens = answerTokens;
            this.segmentCount = segmentCount;
            this.modelName = modelName;
            this.createTime = createTime;
        }
        
        // Getters
        public String getSessionId() { return sessionId; }
        public int getTotalTokens() { return totalTokens; }
        public int getQuestionTokens() { return questionTokens; }
        public int getAnswerTokens() { return answerTokens; }
        public int getSegmentCount() { return segmentCount; }
        public String getModelName() { return modelName; }
        public LocalDateTime getCreateTime() { return createTime; }
    }
}
