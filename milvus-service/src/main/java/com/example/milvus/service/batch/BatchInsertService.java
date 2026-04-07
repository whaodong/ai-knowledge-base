package com.example.milvus.service.batch;

import io.milvus.client.MilvusClient;
import io.milvus.grpc.MutationResult;
import io.milvus.param.dml.InsertParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 批量向量插入优化服务
 * 
 * <p>提供高性能的批量向量插入功能，适用于大规模数据导入场景。</p>
 * <p>优化策略：</p>
 * <ul>
 *   <li>批量插入：减少网络开销</li>
 *   <li>异步处理：不阻塞主线程</li>
 *   <li>并行插入：提高吞吐量</li>
 *   <li>错误重试：保证数据完整性</li>
 * </ul>
 */
@Slf4j
@Service
public class BatchInsertService {
    
    private final MilvusClient milvusClient;
    
    @Value("${milvus.batch.insert-batch-size:1000}")
    private int insertBatchSize;
    
    @Value("${milvus.batch.max-retries:3}")
    private int maxRetries;
    
    @Value("${milvus.batch.parallel-threads:4}")
    private int parallelThreads;
    
    private final ExecutorService batchExecutor;
    
    @Autowired
    public BatchInsertService(MilvusClient milvusClient) {
        this.milvusClient = milvusClient;
        this.batchExecutor = Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors(),
                r -> {
                    Thread t = new Thread(r);
                    t.setName("batch-insert-" + t.getId());
                    t.setDaemon(true);
                    return t;
                }
        );
    }
    
    /**
     * 批量插入向量数据
     * 
     * @param collectionName 集合名称
     * @param fields 字段数据列表
     * @return 插入结果
     */
    public BatchInsertResult batchInsert(String collectionName, List<InsertParam.Field> fields) {
        return batchInsert(collectionName, fields, InsertStrategy.SEQUENTIAL);
    }
    
    /**
     * 批量插入向量数据（指定策略）
     * 
     * @param collectionName 集合名称
     * @param fields 字段数据列表
     * @param strategy 插入策略
     * @return 插入结果
     */
    public BatchInsertResult batchInsert(
            String collectionName, 
            List<InsertParam.Field> fields, 
            InsertStrategy strategy) {
        
        long startTime = System.currentTimeMillis();
        
        try {
            // 获取数据总量
            int totalCount = fields.isEmpty() ? 0 : fields.get(0).getValues().size();
            
            if (totalCount == 0) {
                log.warn("没有数据需要插入");
                return BatchInsertResult.success(0, 0);
            }
            
            log.info("开始批量插入: collection={}, totalCount={}, batchSize={}, strategy={}",
                    collectionName, totalCount, insertBatchSize, strategy);
            
            // 分批处理
            List<List<InsertParam.Field>> batches = splitIntoBatches(fields, insertBatchSize);
            
            BatchInsertResult result;
            
            switch (strategy) {
                case PARALLEL:
                    result = insertParallel(collectionName, batches);
                    break;
                case ASYNC:
                    result = insertAsync(collectionName, batches);
                    break;
                case SEQUENTIAL:
                default:
                    result = insertSequential(collectionName, batches);
            }
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("批量插入完成: 总数={}, 成功={}, 失败={}, 耗时={}ms",
                    totalCount, result.getSuccessCount(), result.getFailedCount(), duration);
            
            return result;
            
        } catch (Exception e) {
            log.error("批量插入失败: {}", e.getMessage(), e);
            return BatchInsertResult.failure(e.getMessage());
        }
    }
    
    /**
     * 异步批量插入
     * 
     * @param collectionName 集合名称
     * @param fields 字段数据列表
     * @return 异步结果
     */
    @Async
    public CompletableFuture<BatchInsertResult> batchInsertAsync(
            String collectionName, 
            List<InsertParam.Field> fields) {
        
        return CompletableFuture.supplyAsync(
                () -> batchInsert(collectionName, fields),
                batchExecutor
        );
    }
    
    /**
     * 流式插入（适用于持续数据流）
     * 
     * @param collectionName 集合名称
     * @param dataStream 数据流
     * @return 插入统计
     */
    public InsertStats streamInsert(
            String collectionName,
            Iterable<List<InsertParam.Field>> dataStream) {
        
        InsertStats stats = new InsertStats();
        
        for (List<InsertParam.Field> batch : dataStream) {
            try {
                BatchInsertResult result = batchInsert(
                        collectionName, batch, InsertStrategy.SEQUENTIAL);
                stats.addResult(result);
            } catch (Exception e) {
                log.error("流式插入批次失败: {}", e.getMessage());
                stats.incrementFailed();
            }
        }
        
        return stats;
    }
    
    // ============= 私有方法 =============
    
    /**
     * 将数据分割成批次
     */
    private List<List<InsertParam.Field>> splitIntoBatches(
            List<InsertParam.Field> fields, int batchSize) {
        
        List<List<InsertParam.Field>> batches = new ArrayList<>();
        
        if (fields.isEmpty()) {
            return batches;
        }
        
        int totalCount = fields.get(0).getValues().size();
        int batchCount = (int) Math.ceil((double) totalCount / batchSize);
        
        for (int i = 0; i < batchCount; i++) {
            int start = i * batchSize;
            int end = Math.min(start + batchSize, totalCount);
            
            List<InsertParam.Field> batchFields = new ArrayList<>();
            for (InsertParam.Field field : fields) {
                List<Object> batchValues = field.getValues().subList(start, end);
                batchFields.add(new InsertParam.Field(field.getName(), batchValues));
            }
            
            batches.add(batchFields);
        }
        
        return batches;
    }
    
    /**
     * 顺序插入
     */
    private BatchInsertResult insertSequential(
            String collectionName, 
            List<List<InsertParam.Field>> batches) {
        
        int successCount = 0;
        int failedCount = 0;
        List<String> errors = new ArrayList<>();
        
        for (int i = 0; i < batches.size(); i++) {
            List<InsertParam.Field> batch = batches.get(i);
            
            try {
                insertBatchWithRetry(collectionName, batch);
                successCount += batch.get(0).getValues().size();
                log.debug("批次 {}/{} 插入成功", i + 1, batches.size());
                
            } catch (Exception e) {
                failedCount += batch.get(0).getValues().size();
                String error = String.format("批次 %d 插入失败: %s", i + 1, e.getMessage());
                errors.add(error);
                log.warn(error);
            }
        }
        
        return new BatchInsertResult(
                successCount > 0,
                successCount,
                failedCount,
                errors
        );
    }
    
    /**
     * 并行插入
     */
    private BatchInsertResult insertParallel(
            String collectionName,
            List<List<InsertParam.Field>> batches) {
        
        List<CompletableFuture<BatchInsertResult>> futures = new ArrayList<>();
        
        for (List<InsertParam.Field> batch : batches) {
            CompletableFuture<BatchInsertResult> future = CompletableFuture.supplyAsync(
                    () -> {
                        try {
                            insertBatchWithRetry(collectionName, batch);
                            return BatchInsertResult.success(
                                    batch.get(0).getValues().size(), 0);
                        } catch (Exception e) {
                            return BatchInsertResult.failure(e.getMessage());
                        }
                    },
                    batchExecutor
            );
            futures.add(future);
        }
        
        // 等待所有任务完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        
        // 合并结果
        int successCount = 0;
        int failedCount = 0;
        List<String> errors = new ArrayList<>();
        
        for (CompletableFuture<BatchInsertResult> future : futures) {
            BatchInsertResult result = future.join();
            successCount += result.getSuccessCount();
            failedCount += result.getFailedCount();
            if (!result.isSuccess()) {
                errors.addAll(result.getErrors());
            }
        }
        
        return new BatchInsertResult(
                successCount > 0,
                successCount,
                failedCount,
                errors
        );
    }
    
    /**
     * 异步插入（立即返回）
     */
    private BatchInsertResult insertAsync(
            String collectionName,
            List<List<InsertParam.Field>> batches) {
        
        // 提交所有批次到线程池
        for (List<InsertParam.Field> batch : batches) {
            batchExecutor.submit(() -> {
                try {
                    insertBatchWithRetry(collectionName, batch);
                    log.debug("异步批次插入成功");
                } catch (Exception e) {
                    log.error("异步批次插入失败: {}", e.getMessage());
                }
            });
        }
        
        return BatchInsertResult.accepted(batches.size());
    }
    
    /**
     * 带重试的单批次插入
     */
    private void insertBatchWithRetry(
            String collectionName, 
            List<InsertParam.Field> fields) throws Exception {
        
        Exception lastException = null;
        
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                InsertParam insertParam = InsertParam.newBuilder()
                        .withCollectionName(collectionName)
                        .withFields(fields)
                        .build();
                
                MutationResult result = milvusClient.insert(insertParam);
                
                if (result.getInsertCount() > 0) {
                    return;
                } else {
                    throw new RuntimeException("插入计数为0");
                }
                
            } catch (Exception e) {
                lastException = e;
                log.warn("插入失败，第 {} 次重试: {}", attempt, e.getMessage());
                
                // 指数退避
                if (attempt < maxRetries) {
                    try {
                        Thread.sleep((long) Math.pow(2, attempt) * 100);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("插入被中断", ie);
                    }
                }
            }
        }
        
        throw new RuntimeException("插入失败，已达最大重试次数", lastException);
    }
    
    /**
     * 关闭线程池
     */
    public void shutdown() {
        batchExecutor.shutdown();
        try {
            if (!batchExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                batchExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            batchExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("批量插入服务已关闭");
    }
    
    /**
     * 插入策略枚举
     */
    public enum InsertStrategy {
        /** 顺序插入 */
        SEQUENTIAL,
        /** 并行插入 */
        PARALLEL,
        /** 异步插入 */
        ASYNC
    }
    
    /**
     * 批量插入结果
     */
    public static class BatchInsertResult {
        private final boolean success;
        private final int successCount;
        private final int failedCount;
        private final List<String> errors;
        private final boolean accepted;
        
        private BatchInsertResult(boolean success, int successCount, 
                                  int failedCount, List<String> errors) {
            this(success, successCount, failedCount, errors, false);
        }
        
        private BatchInsertResult(boolean success, int successCount, 
                                  int failedCount, List<String> errors, boolean accepted) {
            this.success = success;
            this.successCount = successCount;
            this.failedCount = failedCount;
            this.errors = errors;
            this.accepted = accepted;
        }
        
        public static BatchInsertResult success(int successCount, int failedCount) {
            return new BatchInsertResult(true, successCount, failedCount, new ArrayList<>());
        }
        
        public static BatchInsertResult failure(String error) {
            List<String> errors = new ArrayList<>();
            errors.add(error);
            return new BatchInsertResult(false, 0, 0, errors);
        }
        
        public static BatchInsertResult accepted(int batchCount) {
            return new BatchInsertResult(true, 0, 0, new ArrayList<>(), true);
        }
        
        public boolean isSuccess() { return success; }
        public int getSuccessCount() { return successCount; }
        public int getFailedCount() { return failedCount; }
        public List<String> getErrors() { return errors; }
        public boolean isAccepted() { return accepted; }
    }
    
    /**
     * 插入统计
     */
    public static class InsertStats {
        private int totalBatches;
        private int successBatches;
        private int failedBatches;
        private int totalRecords;
        private long duration;
        private long startTime;
        
        public InsertStats() {
            this.startTime = System.currentTimeMillis();
        }
        
        public void addResult(BatchInsertResult result) {
            totalBatches++;
            if (result.isSuccess()) {
                successBatches++;
                totalRecords += result.getSuccessCount();
            } else {
                failedBatches++;
            }
            duration = System.currentTimeMillis() - startTime;
        }
        
        public void incrementFailed() {
            failedBatches++;
            totalBatches++;
            duration = System.currentTimeMillis() - startTime;
        }
        
        public int getTotalBatches() { return totalBatches; }
        public int getSuccessBatches() { return successBatches; }
        public int getFailedBatches() { return failedBatches; }
        public int getTotalRecords() { return totalRecords; }
        public long getDuration() { return duration; }
        public double getThroughput() {
            return duration > 0 ? (double) totalRecords / (duration / 1000.0) : 0;
        }
    }
}
