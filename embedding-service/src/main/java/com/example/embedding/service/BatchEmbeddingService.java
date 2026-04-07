package com.example.embedding.service;

import com.example.embedding.config.EmbeddingProperties;
import com.example.embedding.model.EmbeddingModelType;
import com.example.embedding.model.EmbeddingResult;
import com.example.embedding.registry.EmbeddingModelRegistry;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 批量编码服务
 * 
 * <p>提供高性能的批量编码能力</p>
 * 
 * @author AI Knowledge Base Team
 * @since 1.0.0
 */
@Slf4j
@Service
public class BatchEmbeddingService {
    
    @Autowired
    private EmbeddingModelRegistry registry;
    
    @Autowired
    private EmbeddingCacheService cacheService;
    
    @Autowired
    private EmbeddingProperties properties;
    
    private final ExecutorService executorService;
    
    public BatchEmbeddingService() {
        // 创建线程池
        this.executorService = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors(),
            new ThreadFactory() {
                private final AtomicInteger counter = new AtomicInteger(0);
                
                @Override
                public Thread newThread(Runnable r) {
                    Thread thread = new Thread(r);
                    thread.setName("embedding-batch-" + counter.incrementAndGet());
                    thread.setDaemon(true);
                    return thread;
                }
            }
        );
    }
    
    /**
     * 批量编码（同步）
     */
    public BatchResult batchEmbed(List<String> texts, EmbeddingModelType modelType) {
        return batchEmbed(texts, modelType, properties.getBatch().getDefaultBatchSize());
    }
    
    /**
     * 批量编码（指定批大小）
     */
    public BatchResult batchEmbed(List<String> texts, EmbeddingModelType modelType, int batchSize) {
        long startTime = System.currentTimeMillis();
        
        BatchResult result = new BatchResult();
        result.setTotal(texts.size());
        result.setModel(modelType.getModelName());
        
        log.info("Starting batch embedding: {} texts, batch size: {}, model: {}", 
            texts.size(), batchSize, modelType.getModelName());
        
        try {
            // 动态调整批大小
            batchSize = adjustBatchSize(batchSize, texts);
            
            // 分批处理
            List<List<String>> batches = partition(texts, batchSize);
            
            List<EmbeddingResult> allResults = new ArrayList<>();
            
            for (int i = 0; i < batches.size(); i++) {
                List<String> batch = batches.get(i);
                log.debug("Processing batch {}/{}", i + 1, batches.size());
                
                List<EmbeddingResult> batchResults = processBatch(batch, modelType);
                allResults.addAll(batchResults);
                
                // 更新进度
                result.setProcessed((i + 1) * batchSize);
                result.setProgress((double) (i + 1) / batches.size() * 100);
            }
            
            // 统计结果
            for (EmbeddingResult embeddingResult : allResults) {
                if (embeddingResult.getSuccess()) {
                    result.setSuccessCount(result.getSuccessCount() + 1);
                    if (embeddingResult.getFromCache()) {
                        result.setCacheHitCount(result.getCacheHitCount() + 1);
                    }
                } else {
                    result.setFailCount(result.getFailCount() + 1);
                }
            }
            
            result.setResults(allResults);
            
            long duration = System.currentTimeMillis() - startTime;
            result.setDuration(duration);
            
            log.info("Batch embedding completed: success={}, fail={}, cacheHit={}, duration={}ms", 
                result.getSuccessCount(), result.getFailCount(), result.getCacheHitCount(), duration);
            
        } catch (Exception e) {
            log.error("Batch embedding failed", e);
            result.setErrorMessage(e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 批量编码（异步）
     */
    @Async
    public CompletableFuture<BatchResult> batchEmbedAsync(List<String> texts, EmbeddingModelType modelType) {
        return CompletableFuture.completedFuture(batchEmbed(texts, modelType));
    }
    
    /**
     * 并行批量编码
     */
    public BatchResult batchEmbedParallel(List<String> texts, EmbeddingModelType modelType, int parallelism) {
        long startTime = System.currentTimeMillis();
        
        BatchResult result = new BatchResult();
        result.setTotal(texts.size());
        result.setModel(modelType.getModelName());
        
        log.info("Starting parallel batch embedding: {} texts, parallelism: {}, model: {}", 
            texts.size(), parallelism, modelType.getModelName());
        
        try {
            // 分批
            int batchSize = properties.getBatch().getDefaultBatchSize();
            List<List<String>> batches = partition(texts, batchSize);
            
            // 并行处理
            List<CompletableFuture<List<EmbeddingResult>>> futures = new ArrayList<>();
            
            for (List<String> batch : batches) {
                CompletableFuture<List<EmbeddingResult>> future = CompletableFuture.supplyAsync(
                    () -> processBatch(batch, modelType),
                    executorService
                );
                futures.add(future);
            }
            
            // 等待所有任务完成
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            
            // 收集结果
            List<EmbeddingResult> allResults = new ArrayList<>();
            for (CompletableFuture<List<EmbeddingResult>> future : futures) {
                allResults.addAll(future.get());
            }
            
            // 统计
            for (EmbeddingResult embeddingResult : allResults) {
                if (embeddingResult.getSuccess()) {
                    result.setSuccessCount(result.getSuccessCount() + 1);
                    if (embeddingResult.getFromCache()) {
                        result.setCacheHitCount(result.getCacheHitCount() + 1);
                    }
                } else {
                    result.setFailCount(result.getFailCount() + 1);
                }
            }
            
            result.setResults(allResults);
            
            long duration = System.currentTimeMillis() - startTime;
            result.setDuration(duration);
            
            log.info("Parallel batch embedding completed: success={}, fail={}, cacheHit={}, duration={}ms", 
                result.getSuccessCount(), result.getFailCount(), result.getCacheHitCount(), duration);
            
        } catch (Exception e) {
            log.error("Parallel batch embedding failed", e);
            result.setErrorMessage(e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 处理单个批次
     */
    private List<EmbeddingResult> processBatch(List<String> batch, EmbeddingModelType modelType) {
        List<EmbeddingResult> results = new ArrayList<>();
        
        // 先检查缓存
        Map<String, EmbeddingResult> cachedResults = new HashMap<>();
        List<String> uncachedTexts = new ArrayList<>();
        
        if (properties.getCache().getEnabled()) {
            for (String text : batch) {
                EmbeddingResult cached = cacheService.get(text, modelType);
                if (cached != null && cached.getSuccess()) {
                    cachedResults.put(text, cached);
                } else {
                    uncachedTexts.add(text);
                }
            }
        } else {
            uncachedTexts.addAll(batch);
        }
        
        // 编码未缓存的文本
        List<EmbeddingResult> newResults = new ArrayList<>();
        if (!uncachedTexts.isEmpty()) {
            newResults = registry.getProvider(modelType).embedBatch(uncachedTexts, modelType);
            
            // 存入缓存
            if (properties.getCache().getEnabled()) {
                for (int i = 0; i < uncachedTexts.size(); i++) {
                    if (i < newResults.size() && newResults.get(i).getSuccess()) {
                        cacheService.put(uncachedTexts.get(i), modelType, newResults.get(i));
                    }
                }
            }
        }
        
        // 组装结果（保持原始顺序）
        Map<String, EmbeddingResult> uncachedMap = new HashMap<>();
        for (int i = 0; i < uncachedTexts.size(); i++) {
            uncachedMap.put(uncachedTexts.get(i), newResults.get(i));
        }
        
        for (String text : batch) {
            EmbeddingResult cached = cachedResults.get(text);
            if (cached != null) {
                results.add(cached);
            } else {
                results.add(uncachedMap.getOrDefault(text, 
                    EmbeddingResult.failure("Missing result")));
            }
        }
        
        return results;
    }
    
    /**
     * 动态调整批大小
     */
    private int adjustBatchSize(int batchSize, List<String> texts) {
        int maxSize = properties.getBatch().getMaxBatchSize();
        
        // 根据文本长度调整
        double avgLength = texts.stream().mapToInt(String::length).average().orElse(100);
        
        if (avgLength > 1000) {
            // 长文本减少批大小
            batchSize = Math.max(batchSize / 2, 5);
        } else if (avgLength < 100) {
            // 短文本可以增大批大小
            batchSize = Math.min(batchSize * 2, maxSize);
        }
        
        return Math.min(batchSize, maxSize);
    }
    
    /**
     * 分区
     */
    private <T> List<List<T>> partition(List<T> list, int size) {
        List<List<T>> partitions = new ArrayList<>();
        
        for (int i = 0; i < list.size(); i += size) {
            partitions.add(list.subList(i, Math.min(i + size, list.size())));
        }
        
        return partitions;
    }
    
    /**
     * 批量编码结果
     */
    @Data
    public static class BatchResult {
        private Integer total;
        private Integer processed = 0;
        private Double progress = 0.0;
        private Integer successCount = 0;
        private Integer failCount = 0;
        private Integer cacheHitCount = 0;
        private String model;
        private Long duration;
        private String errorMessage;
        private List<EmbeddingResult> results;
    }
}
