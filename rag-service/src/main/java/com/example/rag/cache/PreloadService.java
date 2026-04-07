package com.example.rag.cache;

import com.example.rag.cache.HotQueryDetector.RelatedQuery;
import com.example.rag.model.RagRequest;
import com.example.rag.model.RagResponse;
import com.example.rag.service.RagRetrievalService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 智能预加载服务
 * 
 * <p>基于用户行为的智能预加载机制：</p>
 * <ul>
 *   <li>用户查询历史分析</li>
 *   <li>相关查询预测（查询A后大概率查询B）</li>
 *   <li>后台异步预加载</li>
 *   <li>预加载效果追踪</li>
 * </ul>
 */
@Slf4j
@Service
@ConditionalOnBean({HotQueryDetector.class, RagCacheService.class})
public class PreloadService {

    private final HotQueryDetector hotQueryDetector;
    private final RagRetrievalService ragRetrievalService;
    private final RagCacheService ragCacheService;
    private final ExecutorService preloadExecutor;
    
    // 预加载配置
    private static final int MAX_PRELOAD_QUEUE = 50;          // 最大预加载队列长度
    private static final double PRELOAD_THRESHOLD = 0.3;      // 预加载概率阈值
    private static final long PRELOAD_TTL = 300;              // 预加载缓存TTL（秒）
    private static final int PRELOAD_DELAY_MS = 1000;         // 预加载延迟（毫秒）
    
    // 用户查询历史缓存（用户ID -> 查询列表）
    private final Map<String, LinkedList<String>> userQueryHistory = new ConcurrentHashMap<>();
    private static final int MAX_HISTORY_SIZE = 20;           // 最大历史记录数
    
    // 预加载队列和状态
    private final BlockingQueue<PreloadTask> preloadQueue = new LinkedBlockingQueue<>(MAX_PRELOAD_QUEUE);
    private final Map<String, PreloadResult> preloadResults = new ConcurrentHashMap<>();
    private final AtomicInteger preloadedCount = new AtomicInteger(0);
    private final AtomicInteger hitCount = new AtomicInteger(0);
    
    // 缓存区域
    private static final String CACHE_PRELOAD = "preload";
    
    public PreloadService(
            HotQueryDetector hotQueryDetector,
            RagRetrievalService ragRetrievalService,
            RagCacheService ragCacheService) {
        this.hotQueryDetector = hotQueryDetector;
        this.ragRetrievalService = ragRetrievalService;
        this.ragCacheService = ragCacheService;
        this.preloadExecutor = Executors.newFixedThreadPool(2);
        
        // 启动预加载消费者
        startPreloadConsumer();
    }
    
    /**
     * 分析用户查询并触发预加载
     * 
     * @param userId 用户ID
     * @param currentQuery 当前查询
     */
    public void analyzeAndPreload(String userId, String currentQuery) {
        if (currentQuery == null || currentQuery.trim().isEmpty()) {
            return;
        }
        
        try {
            // 1. 记录用户查询历史
            recordUserQuery(userId, currentQuery);
            
            // 2. 获取上一个查询
            String previousQuery = getPreviousQuery(userId);
            
            // 3. 记录查询关联
            if (previousQuery != null) {
                hotQueryDetector.recordQueryRelation(previousQuery, currentQuery);
            }
            
            // 4. 预测相关查询并预加载
            predictAndPreload(currentQuery);
            
            log.debug("查询分析完成: userId={}, query={}", userId, currentQuery);
            
        } catch (Exception e) {
            log.error("分析用户查询失败: userId={}, query={}", userId, currentQuery, e);
        }
    }
    
    /**
     * 记录用户查询历史
     */
    private void recordUserQuery(String userId, String query) {
        if (userId == null) {
            return;
        }
        
        userQueryHistory.computeIfAbsent(userId, k -> new LinkedList<>());
        LinkedList<String> history = userQueryHistory.get(userId);
        
        // 添加到历史记录
        history.addFirst(query);
        
        // 限制历史记录长度
        if (history.size() > MAX_HISTORY_SIZE) {
            history.removeLast();
        }
    }
    
    /**
     * 获取用户上一个查询
     */
    private String getPreviousQuery(String userId) {
        if (userId == null) {
            return null;
        }
        
        LinkedList<String> history = userQueryHistory.get(userId);
        if (history == null || history.size() < 2) {
            return null;
        }
        
        return history.get(1);  // 获取倒数第二个查询
    }
    
    /**
     * 预测相关查询并预加载
     */
    private void predictAndPreload(String currentQuery) {
        // 获取相关查询预测
        List<RelatedQuery> relatedQueries = hotQueryDetector.getRelatedQueries(currentQuery);
        
        if (relatedQueries.isEmpty()) {
            log.debug("没有找到相关查询: query={}", currentQuery);
            return;
        }
        
        log.info("预测到 {} 个相关查询: query={}", relatedQueries.size(), currentQuery);
        
        // 筛选需要预加载的查询
        for (RelatedQuery related : relatedQueries) {
            if (related.getProbability() >= PRELOAD_THRESHOLD) {
                schedulePreload(related.getQuery(), related.getProbability());
            }
        }
    }
    
    /**
     * 调度预加载任务
     * 
     * @param query 查询文本
     * @param probability 预测概率
     */
    private void schedulePreload(String query, double probability) {
        // 检查是否已在缓存中
        if (isQueryCached(query)) {
            log.debug("查询已缓存，跳过预加载: query={}", query);
            return;
        }
        
        // 创建预加载任务
        PreloadTask task = new PreloadTask(
                UUID.randomUUID().toString(),
                query,
                probability,
                LocalDateTime.now()
        );
        
        // 加入预加载队列
        if (preloadQueue.offer(task)) {
            log.debug("预加载任务已加入队列: query={}, probability={}", query, probability);
        } else {
            log.warn("预加载队列已满，丢弃任务: query={}", query);
        }
    }
    
    /**
     * 启动预加载消费者线程
     */
    private void startPreloadConsumer() {
        CompletableFuture.runAsync(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    // 从队列取出任务
                    PreloadTask task = preloadQueue.take();
                    
                    // 延迟执行，避免对系统造成压力
                    Thread.sleep(PRELOAD_DELAY_MS);
                    
                    // 执行预加载
                    executePreload(task);
                    
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.info("预加载消费者线程被中断");
                    break;
                } catch (Exception e) {
                    log.error("预加载任务执行失败", e);
                }
            }
        }, preloadExecutor);
    }
    
    /**
     * 执行预加载任务
     */
    private void executePreload(PreloadTask task) {
        long startTime = System.currentTimeMillis();
        String query = task.getQuery();
        
        try {
            log.info("开始预加载: query={}, probability={}", query, task.getProbability());
            
            // 构建RAG请求
            RagRequest request = RagRequest.builder()
                    .query(query)
                    .topK(10)
                    .hybridSearch(true)
                    .rerankEnabled(true)
                    .build();
            
            // 执行检索
            RagResponse response = ragRetrievalService.retrieve(request);
            
            if (!response.isSuccess()) {
                throw new RuntimeException("RAG检索失败: " + response.getErrorMessage());
            }
            
            // 缓存结果
            String cacheKey = "preload:" + query.hashCode();
            ragCacheService.cacheSimilarityResults(query, response.getRetrievedDocuments());
            
            // 记录预加载结果
            long duration = System.currentTimeMillis() - startTime;
            PreloadResult result = new PreloadResult(
                    task.getTaskId(),
                    query,
                    "success",
                    duration,
                    LocalDateTime.now()
            );
            preloadResults.put(task.getTaskId(), result);
            
            preloadedCount.incrementAndGet();
            
            log.info("预加载完成: query={}, duration={}ms", query, duration);
            
            // 清理旧结果
            cleanupOldResults();
            
        } catch (Exception e) {
            log.error("预加载失败: query={}", query, e);
            
            PreloadResult result = new PreloadResult(
                    task.getTaskId(),
                    query,
                    "failed",
                    System.currentTimeMillis() - startTime,
                    LocalDateTime.now()
            );
            result.setError(e.getMessage());
            preloadResults.put(task.getTaskId(), result);
        }
    }
    
    /**
     * 检查查询是否已缓存
     */
    private boolean isQueryCached(String query) {
        List<?> cached = ragCacheService.getCachedSimilarityResults(query);
        return cached != null && !cached.isEmpty();
    }
    
    /**
     * 标记预加载命中
     * 当实际查询命中预加载的缓存时调用
     * 
     * @param query 查询文本
     */
    public void markPreloadHit(String query) {
        if (isPreloaded(query)) {
            hitCount.incrementAndGet();
            log.debug("预加载命中: query={}", query);
        }
    }
    
    /**
     * 检查查询是否已预加载
     */
    private boolean isPreloaded(String query) {
        return preloadResults.values().stream()
                .anyMatch(r -> r.getQuery().equals(query) && "success".equals(r.getStatus()));
    }
    
    /**
     * 获取用户查询历史
     * 
     * @param userId 用户ID
     * @return 查询历史列表
     */
    public List<String> getUserQueryHistory(String userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        
        LinkedList<String> history = userQueryHistory.get(userId);
        return history != null ? new ArrayList<>(history) : Collections.emptyList();
    }
    
    /**
     * 清除用户查询历史
     * 
     * @param userId 用户ID
     */
    public void clearUserHistory(String userId) {
        if (userId != null) {
            userQueryHistory.remove(userId);
            log.info("用户查询历史已清除: userId={}", userId);
        }
    }
    
    /**
     * 获取预加载统计信息
     */
    public PreloadStats getStats() {
        return PreloadStats.builder()
                .totalPreloaded(preloadedCount.get())
                .totalHits(hitCount.get())
                .hitRate(preloadedCount.get() > 0 ? 
                        (double) hitCount.get() / preloadedCount.get() : 0.0)
                .queueSize(preloadQueue.size())
                .maxQueueSize(MAX_PRELOAD_QUEUE)
                .preloadThreshold(PRELOAD_THRESHOLD)
                .build();
    }
    
    /**
     * 获取预加载结果列表
     */
    public List<PreloadResult> getPreloadResults() {
        return new ArrayList<>(preloadResults.values());
    }
    
    /**
     * 清理旧的预加载结果
     * 保留最近100条记录
     */
    private void cleanupOldResults() {
        if (preloadResults.size() > 100) {
            // 按时间排序，删除最旧的记录
            List<String> oldKeys = preloadResults.entrySet().stream()
                    .sorted(Comparator.comparing(e -> e.getValue().getTimestamp()))
                    .limit(preloadResults.size() - 100)
                    .map(Map.Entry::getKey)
                    .toList();
            
            oldKeys.forEach(preloadResults::remove);
        }
    }
    
    /**
     * 重置统计
     */
    public void resetStats() {
        preloadedCount.set(0);
        hitCount.set(0);
        preloadResults.clear();
        log.info("预加载统计已重置");
    }
    
    /**
     * 服务关闭时清理资源
     */
    public void shutdown() {
        preloadExecutor.shutdownNow();
        log.info("智能预加载服务已关闭");
    }
    
    /**
     * 预加载任务
     */
    @Data
    public static class PreloadTask {
        private final String taskId;
        private final String query;
        private final double probability;
        private final LocalDateTime createTime;
        
        public PreloadTask(String taskId, String query, double probability, LocalDateTime createTime) {
            this.taskId = taskId;
            this.query = query;
            this.probability = probability;
            this.createTime = createTime;
        }
    }
    
    /**
     * 预加载结果
     */
    @Data
    public static class PreloadResult {
        private final String taskId;
        private final String query;
        private final String status;
        private final long duration;
        private final LocalDateTime timestamp;
        private String error;
        
        public PreloadResult(String taskId, String query, String status, long duration, LocalDateTime timestamp) {
            this.taskId = taskId;
            this.query = query;
            this.status = status;
            this.duration = duration;
            this.timestamp = timestamp;
        }
    }
    
    /**
     * 预加载统计信息
     */
    @Data
    @lombok.Builder
    public static class PreloadStats {
        private int totalPreloaded;
        private int totalHits;
        private double hitRate;
        private int queueSize;
        private int maxQueueSize;
        private double preloadThreshold;
    }
}
