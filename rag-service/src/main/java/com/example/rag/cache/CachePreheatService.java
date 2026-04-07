package com.example.rag.cache;

import com.example.common.cache.MultiLevelCacheService;
import com.example.rag.model.RagRequest;
import com.example.rag.model.RagResponse;
import com.example.rag.service.RagRetrievalService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 缓存预热服务
 * 
 * <p>实现智能的检索结果预热缓存机制：</p>
 * <ul>
 *   <li>定时预热Top N热点查询</li>
 *   <li>TTL动态调整（热点查询缓存时间更长）</li>
 *   <li>异步预热，不影响正常查询</li>
 *   <li>预热任务执行监控</li>
 * </ul>
 */
@Slf4j
@Service
public class CachePreheatService {

    private final HotQueryDetector hotQueryDetector;
    private final RagRetrievalService ragRetrievalService;
    private final MultiLevelCacheService cacheService;
    private final ExecutorService preheatExecutor;
    
    // 预热配置
    private static final int PREHEAT_TOP_N = 20;              // 预热Top N热点查询
    private static final long BASE_TTL_SECONDS = 300;         // 基础TTL（5分钟）
    private static final long MAX_TTL_SECONDS = 3600;         // 最大TTL（1小时）
    private static final int FREQUENCY_WEIGHT = 10;           // 频率权重（每10次访问增加1分钟）
    
    // 预热任务状态
    private final Map<String, PreheatTask> runningTasks = new ConcurrentHashMap<>();
    private final AtomicInteger successCount = new AtomicInteger(0);
    private final AtomicInteger failureCount = new AtomicInteger(0);
    
    // 缓存区域
    private static final String CACHE_PREHEAT = "preheat";
    
    public CachePreheatService(
            HotQueryDetector hotQueryDetector,
            RagRetrievalService ragRetrievalService,
            MultiLevelCacheService cacheService) {
        this.hotQueryDetector = hotQueryDetector;
        this.ragRetrievalService = ragRetrievalService;
        this.cacheService = cacheService;
        this.preheatExecutor = Executors.newFixedThreadPool(3);  // 预热线程池
    }
    
    /**
     * 定时预热任务
     * 每10分钟执行一次
     */
    @Scheduled(fixedRate = 600000, initialDelay = 60000)
    public void scheduledPreheat() {
        log.info("开始执行定时预热任务...");
        
        try {
            // 获取Top N热点查询
            List<HotQueryDetector.HotQuery> hotQueries = 
                    hotQueryDetector.getTopHotQueries(PREHEAT_TOP_N);
            
            if (hotQueries.isEmpty()) {
                log.info("暂无热点查询需要预热");
                return;
            }
            
            log.info("准备预热 {} 个热点查询", hotQueries.size());
            
            // 异步预热每个热点查询
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            for (HotQueryDetector.HotQuery hotQuery : hotQueries) {
                CompletableFuture<Void> future = preheatQueryAsync(hotQuery);
                futures.add(future);
            }
            
            // 等待所有预热任务完成
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .thenRun(() -> {
                        log.info("预热任务完成，成功: {}, 失败: {}", 
                                successCount.get(), failureCount.get());
                    })
                    .exceptionally(e -> {
                        log.error("预热任务执行异常", e);
                        return null;
                    });
            
        } catch (Exception e) {
            log.error("定时预热任务执行失败", e);
        }
    }
    
    /**
     * 异步预热单个查询
     * 
     * @param hotQuery 热点查询
     * @return 预热任务Future
     */
    public CompletableFuture<Void> preheatQueryAsync(HotQueryDetector.HotQuery hotQuery) {
        String taskId = UUID.randomUUID().toString();
        String query = hotQuery.getQuery();
        
        // 创建预热任务记录
        PreheatTask task = new PreheatTask(taskId, query, LocalDateTime.now());
        runningTasks.put(taskId, task);
        
        return CompletableFuture.runAsync(() -> {
            try {
                // 检查是否已在缓存中
                if (isQueryCached(query)) {
                    log.debug("查询已在缓存中，跳过预热: {}", query);
                    task.setStatus("skipped");
                    return;
                }
                
                // 执行预热
                preheatQuery(query, hotQuery.getFrequency());
                
                task.setStatus("success");
                successCount.incrementAndGet();
                
                log.info("查询预热成功: query={}, frequency={}", query, hotQuery.getFrequency());
                
            } catch (Exception e) {
                task.setStatus("failed");
                task.setError(e.getMessage());
                failureCount.incrementAndGet();
                
                log.error("查询预热失败: query={}", query, e);
                
            } finally {
                task.setEndTime(LocalDateTime.now());
                // 延迟移除任务记录，供监控查看
                CompletableFuture.delayedExecutor(5, java.util.concurrent.TimeUnit.MINUTES)
                        .execute(() -> runningTasks.remove(taskId));
            }
        }, preheatExecutor);
    }
    
    /**
     * 预热单个查询
     * 
     * @param query 查询文本
     * @param frequency 查询频率
     */
    private void preheatQuery(String query, int frequency) {
        long startTime = System.currentTimeMillis();
        
        // 1. 构建RAG请求
        RagRequest request = RagRequest.builder()
                .query(query)
                .topK(10)
                .hybridSearch(true)
                .rerankEnabled(true)
                .build();
        
        // 2. 执行RAG检索
        RagResponse response = ragRetrievalService.retrieve(request);
        
        if (!response.isSuccess()) {
            throw new RuntimeException("RAG检索失败: " + response.getErrorMessage());
        }
        
        // 3. 计算动态TTL（基于查询频率）
        long dynamicTtl = calculateDynamicTtl(frequency);
        
        // 4. 缓存检索结果
        String cacheKey = "query:" + query.hashCode();
        cacheService.put(CACHE_PREHEAT, cacheKey, response, dynamicTtl);
        
        long duration = System.currentTimeMillis() - startTime;
        log.debug("预热完成: query={}, ttl={}s, duration={}ms", query, dynamicTtl, duration);
    }
    
    /**
     * 计算动态TTL
     * 根据查询频率动态调整缓存时间
     * 
     * @param frequency 查询频率
     * @return TTL（秒）
     */
    private long calculateDynamicTtl(int frequency) {
        // 基础TTL + 频率加成（每10次访问增加60秒）
        long ttl = BASE_TTL_SECONDS + (frequency / FREQUENCY_WEIGHT) * 60;
        
        // 限制最大TTL
        return Math.min(ttl, MAX_TTL_SECONDS);
    }
    
    /**
     * 检查查询是否已缓存
     * 
     * @param query 查询文本
     * @return 是否已缓存
     */
    private boolean isQueryCached(String query) {
        String cacheKey = "query:" + query.hashCode();
        Object cached = cacheService.get(CACHE_PREHEAT, cacheKey, () -> null, 1);
        return cached != null;
    }
    
    /**
     * 手动预热指定查询
     * 
     * @param query 查询文本
     * @return 预热任务ID
     */
    public String manualPreheat(String query) {
        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("查询文本不能为空");
        }
        
        // 构造热点查询对象
        int frequency = hotQueryDetector.getQueryFrequency(query);
        HotQueryDetector.HotQuery hotQuery = new HotQueryDetector.HotQuery(
                query, frequency, LocalDateTime.now());
        
        // 异步预热
        CompletableFuture<Void> future = preheatQueryAsync(hotQuery);
        
        // 返回任务ID
        Optional<String> taskId = runningTasks.entrySet().stream()
                .filter(entry -> entry.getValue().getQuery().equals(query))
                .map(Map.Entry::getKey)
                .findFirst();
        
        return taskId.orElse("unknown");
    }
    
    /**
     * 批量预热查询
     * 
     * @param queries 查询列表
     * @return 预热任务ID列表
     */
    public List<String> batchPreheat(List<String> queries) {
        if (queries == null || queries.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<String> taskIds = new ArrayList<>();
        
        for (String query : queries) {
            try {
                String taskId = manualPreheat(query);
                taskIds.add(taskId);
            } catch (Exception e) {
                log.error("批量预热查询失败: query={}", query, e);
            }
        }
        
        return taskIds;
    }
    
    /**
     * 获取预热任务状态
     * 
     * @param taskId 任务ID
     * @return 预热任务信息
     */
    public PreheatTask getTaskStatus(String taskId) {
        return runningTasks.get(taskId);
    }
    
    /**
     * 获取所有运行中的预热任务
     * 
     * @return 预热任务列表
     */
    public List<PreheatTask> getAllRunningTasks() {
        return new ArrayList<>(runningTasks.values());
    }
    
    /**
     * 获取预热统计信息
     */
    public PreheatStats getStats() {
        return PreheatStats.builder()
                .totalTasks(runningTasks.size())
                .successCount(successCount.get())
                .failureCount(failureCount.get())
                .preheatTopN(PREHEAT_TOP_N)
                .baseTtlSeconds(BASE_TTL_SECONDS)
                .maxTtlSeconds(MAX_TTL_SECONDS)
                .build();
    }
    
    /**
     * 清空预热统计
     */
    public void resetStats() {
        successCount.set(0);
        failureCount.set(0);
        log.info("预热统计已重置");
    }
    
    /**
     * 服务关闭时清理资源
     */
    public void shutdown() {
        preheatExecutor.shutdown();
        log.info("缓存预热服务已关闭");
    }
    
    /**
     * 预热任务信息
     */
    @Data
    public static class PreheatTask {
        private final String taskId;
        private final String query;
        private final LocalDateTime startTime;
        private LocalDateTime endTime;
        private String status;
        private String error;
        
        public PreheatTask(String taskId, String query, LocalDateTime startTime) {
            this.taskId = taskId;
            this.query = query;
            this.startTime = startTime;
            this.status = "running";
        }
    }
    
    /**
     * 预热统计信息
     */
    @Data
    @lombok.Builder
    public static class PreheatStats {
        private int totalTasks;
        private int successCount;
        private int failureCount;
        private int preheatTopN;
        private long baseTtlSeconds;
        private long maxTtlSeconds;
    }
}
