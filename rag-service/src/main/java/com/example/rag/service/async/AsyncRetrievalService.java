package com.example.rag.service.async;

import com.example.rag.model.RagRequest;
import com.example.rag.model.RetrievalResult;
import com.example.rag.retriever.Retriever;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * 异步检索服务
 * 
 * <p>基于CompletableFuture和自定义线程池实现高性能异步检索。</p>
 * <p>功能特点：</p>
 * <ul>
 *   <li>异步并行检索：提高检索吞吐量</li>
 *   <li>自定义线程池：精确控制并发资源</li>
 *   <li>超时控制：避免长时间阻塞</li>
 *   <li>结果缓存：减少重复检索</li>
 * </ul>
 */
@Slf4j
@Service
public class AsyncRetrievalService {
    
    private final List<Retriever> retrievers;
    
    @Value("${rag.async.core-pool-size:#{T(java.lang.Runtime).getRuntime().availableProcessors()}}")
    private int corePoolSize;
    
    @Value("${rag.async.max-pool-size:20}")
    private int maxPoolSize;
    
    @Value("${rag.async.queue-capacity:100}")
    private int queueCapacity;
    
    @Value("${rag.async.timeout:5000}")
    private long timeout;
    
    private ExecutorService retrievalExecutor;
    
    @Autowired
    public AsyncRetrievalService(List<Retriever> retrievers) {
        this.retrievers = retrievers;
    }
    
    @PostConstruct
    public void init() {
        // 创建自定义线程池
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                corePoolSize,
                maxPoolSize,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(queueCapacity),
                new ThreadFactory() {
                    private int counter = 0;
                    @Override
                    public Thread newThread(Runnable r) {
                        Thread thread = new Thread(r);
                        thread.setName("retrieval-async-" + counter++);
                        thread.setDaemon(true);
                        return thread;
                    }
                },
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
        
        this.retrievalExecutor = executor;
        
        log.info("异步检索服务初始化完成: corePoolSize={}, maxPoolSize={}, queueCapacity={}, timeout={}ms",
                corePoolSize, maxPoolSize, queueCapacity, timeout);
    }
    
    /**
     * 异步执行单次检索
     * 
     * @param request RAG请求
     * @return 异步检索结果
     */
    public CompletableFuture<AsyncRetrievalResult> retrieveAsync(RagRequest request) {
        long startTime = System.currentTimeMillis();
        String requestId = UUID.randomUUID().toString();
        
        log.debug("开始异步检索: requestId={}, query={}", requestId, request.getQuery());
        
        // 创建并行检索任务
        List<CompletableFuture<RetrieverResult>> retrieverFutures = retrievers.stream()
                .filter(Retriever::isEnabled)
                .map(retriever -> executeRetrieverAsync(retriever, request, requestId))
                .collect(Collectors.toList());
        
        // 等待所有检索器完成
        return CompletableFuture.allOf(retrieverFutures.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    // 收集所有结果
                    List<RetrievalResult> allResults = retrieverFutures.stream()
                            .map(CompletableFuture::join)
                            .flatMap(rr -> rr.getResults().stream())
                            .collect(Collectors.toList());
                    
                    // 统计信息
                    Map<String, RetrieverStats> statsMap = new HashMap<>();
                    for (CompletableFuture<RetrieverResult> future : retrieverFutures) {
                        RetrieverResult rr = future.join();
                        statsMap.put(rr.getRetrieverName(), rr.getStats());
                    }
                    
                    long duration = System.currentTimeMillis() - startTime;
                    
                    return new AsyncRetrievalResult(
                            requestId,
                            true,
                            allResults,
                            statsMap,
                            duration
                    );
                })
                .exceptionally(throwable -> {
                    log.error("异步检索失败: requestId={}, error={}", requestId, throwable.getMessage());
                    long duration = System.currentTimeMillis() - startTime;
                    
                    return new AsyncRetrievalResult(
                            requestId,
                            false,
                            Collections.emptyList(),
                            Collections.emptyMap(),
                            duration,
                            throwable.getMessage()
                    );
                });
    }
    
    /**
     * 异步执行多个检索请求
     * 
     * @param requests RAG请求列表
     * @return 异步检索结果列表
     */
    public CompletableFuture<List<AsyncRetrievalResult>> batchRetrieveAsync(List<RagRequest> requests) {
        List<CompletableFuture<AsyncRetrievalResult>> futures = requests.stream()
                .map(this::retrieveAsync)
                .collect(Collectors.toList());
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.toList()));
    }
    
    /**
     * 带超时的异步检索
     * 
     * @param request RAG请求
     * @param timeoutMs 超时时间（毫秒）
     * @return 异步检索结果
     */
    public CompletableFuture<AsyncRetrievalResult> retrieveAsyncWithTimeout(
            RagRequest request, long timeoutMs) {
        
        return retrieveAsync(request)
                .orTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                .exceptionally(throwable -> {
                    if (throwable instanceof TimeoutException) {
                        log.warn("检索超时: query={}, timeout={}ms", request.getQuery(), timeoutMs);
                        return new AsyncRetrievalResult(
                                null,
                                false,
                                Collections.emptyList(),
                                Collections.emptyMap(),
                                timeoutMs,
                                "检索超时"
                        );
                    }
                    return new AsyncRetrievalResult(
                            null,
                            false,
                            Collections.emptyList(),
                            Collections.emptyMap(),
                            0,
                            throwable.getMessage()
                    );
                });
    }
    
    /**
     * 流式检索结果（适用于长时间检索）
     * 
     * @param request RAG请求
     * @param consumer 结果消费者
     */
    public void retrieveStreaming(
            RagRequest request, 
            java.util.function.Consumer<RetrievalResult> consumer) {
        
        retrievers.stream()
                .filter(Retriever::isEnabled)
                .forEach(retriever -> {
                    try {
                        CompletableFuture.supplyAsync(() -> retriever.retrieve(request), retrievalExecutor)
                                .thenAccept(results -> results.forEach(consumer))
                                .exceptionally(throwable -> {
                                    log.error("流式检索失败: retriever={}, error={}", 
                                            retriever.getName(), throwable.getMessage());
                                    return null;
                                });
                    } catch (Exception e) {
                        log.error("提交流式检索任务失败: {}", e.getMessage());
                    }
                });
    }
    
    /**
     * 获取线程池状态
     */
    public ThreadPoolStats getPoolStats() {
        if (retrievalExecutor instanceof ThreadPoolExecutor) {
            ThreadPoolExecutor executor = (ThreadPoolExecutor) retrievalExecutor;
            return new ThreadPoolStats(
                    executor.getActiveCount(),
                    executor.getCorePoolSize(),
                    executor.getMaximumPoolSize(),
                    executor.getPoolSize(),
                    executor.getQueue().size(),
                    executor.getCompletedTaskCount()
            );
        }
        return null;
    }
    
    // ============= 私有方法 =============
    
    /**
     * 异步执行单个检索器
     */
    private CompletableFuture<RetrieverResult> executeRetrieverAsync(
            Retriever retriever, 
            RagRequest request, 
            String requestId) {
        
        return CompletableFuture.supplyAsync(() -> {
            long startTime = System.currentTimeMillis();
            
            try {
                List<RetrievalResult> results = retriever.retrieve(request);
                long duration = System.currentTimeMillis() - startTime;
                
                RetrieverStats stats = new RetrieverStats(
                        true,
                        results.size(),
                        duration,
                        null
                );
                
                log.debug("检索器 {} 完成: requestId={}, count={}, duration={}ms",
                        retriever.getName(), requestId, results.size(), duration);
                
                return new RetrieverResult(retriever.getName(), results, stats);
                
            } catch (Exception e) {
                long duration = System.currentTimeMillis() - startTime;
                log.error("检索器 {} 失败: requestId={}, error={}", 
                        retriever.getName(), requestId, e.getMessage());
                
                RetrieverStats stats = new RetrieverStats(
                        false,
                        0,
                        duration,
                        e.getMessage()
                );
                
                return new RetrieverResult(retriever.getName(), Collections.emptyList(), stats);
            }
        }, retrievalExecutor);
    }
    
    @PreDestroy
    public void shutdown() {
        log.info("关闭异步检索服务线程池...");
        retrievalExecutor.shutdown();
        try {
            if (!retrievalExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                retrievalExecutor.shutdownNow();
                if (!retrievalExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                    log.error("线程池未完全关闭");
                }
            }
        } catch (InterruptedException e) {
            retrievalExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("异步检索服务已关闭");
    }
    
    // ============= 内部类 =============
    
    /**
     * 检索器结果
     */
    private static class RetrieverResult {
        private final String retrieverName;
        private final List<RetrievalResult> results;
        private final RetrieverStats stats;
        
        RetrieverResult(String retrieverName, List<RetrievalResult> results, RetrieverStats stats) {
            this.retrieverName = retrieverName;
            this.results = results;
            this.stats = stats;
        }
        
        public String getRetrieverName() { return retrieverName; }
        public List<RetrievalResult> getResults() { return results; }
        public RetrieverStats getStats() { return stats; }
    }
    
    /**
     * 检索器统计
     */
    public static class RetrieverStats {
        private final boolean success;
        private final int resultCount;
        private final long duration;
        private final String errorMessage;
        
        RetrieverStats(boolean success, int resultCount, long duration, String errorMessage) {
            this.success = success;
            this.resultCount = resultCount;
            this.duration = duration;
            this.errorMessage = errorMessage;
        }
        
        public boolean isSuccess() { return success; }
        public int getResultCount() { return resultCount; }
        public long getDuration() { return duration; }
        public String getErrorMessage() { return errorMessage; }
    }
    
    /**
     * 异步检索结果
     */
    public static class AsyncRetrievalResult {
        private final String requestId;
        private final boolean success;
        private final List<RetrievalResult> results;
        private final Map<String, RetrieverStats> retrieverStats;
        private final long duration;
        private final String errorMessage;
        
        public AsyncRetrievalResult(
                String requestId,
                boolean success,
                List<RetrievalResult> results,
                Map<String, RetrieverStats> retrieverStats,
                long duration) {
            this(requestId, success, results, retrieverStats, duration, null);
        }
        
        public AsyncRetrievalResult(
                String requestId,
                boolean success,
                List<RetrievalResult> results,
                Map<String, RetrieverStats> retrieverStats,
                long duration,
                String errorMessage) {
            this.requestId = requestId;
            this.success = success;
            this.results = results;
            this.retrieverStats = retrieverStats;
            this.duration = duration;
            this.errorMessage = errorMessage;
        }
        
        public String getRequestId() { return requestId; }
        public boolean isSuccess() { return success; }
        public List<RetrievalResult> getResults() { return results; }
        public Map<String, RetrieverStats> getRetrieverStats() { return retrieverStats; }
        public long getDuration() { return duration; }
        public String getErrorMessage() { return errorMessage; }
    }
    
    /**
     * 线程池统计
     */
    public static class ThreadPoolStats {
        private final int activeThreads;
        private final int corePoolSize;
        private final int maxPoolSize;
        private final int currentPoolSize;
        private final int queueSize;
        private final long completedTasks;
        
        ThreadPoolStats(int activeThreads, int corePoolSize, int maxPoolSize, 
                        int currentPoolSize, int queueSize, long completedTasks) {
            this.activeThreads = activeThreads;
            this.corePoolSize = corePoolSize;
            this.maxPoolSize = maxPoolSize;
            this.currentPoolSize = currentPoolSize;
            this.queueSize = queueSize;
            this.completedTasks = completedTasks;
        }
        
        public int getActiveThreads() { return activeThreads; }
        public int getCorePoolSize() { return corePoolSize; }
        public int getMaxPoolSize() { return maxPoolSize; }
        public int getCurrentPoolSize() { return currentPoolSize; }
        public int getQueueSize() { return queueSize; }
        public long getCompletedTasks() { return completedTasks; }
        public double getUtilization() {
            return corePoolSize > 0 ? (double) activeThreads / corePoolSize : 0;
        }
        
        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("activeThreads", activeThreads);
            map.put("corePoolSize", corePoolSize);
            map.put("maxPoolSize", maxPoolSize);
            map.put("currentPoolSize", currentPoolSize);
            map.put("queueSize", queueSize);
            map.put("completedTasks", completedTasks);
            map.put("utilization", String.format("%.2f%%", getUtilization() * 100));
            return map;
        }
    }
}
