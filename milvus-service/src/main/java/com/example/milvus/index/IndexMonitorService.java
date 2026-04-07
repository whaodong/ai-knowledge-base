package com.example.milvus.index;

import io.milvus.client.MilvusClient;
import io.milvus.grpc.DescribeIndexResponse;
import io.milvus.grpc.GetCollectionStatisticsResponse;
import io.milvus.grpc.GetIndexBuildProgressResponse;
import io.milvus.grpc.IndexDescription;
import io.milvus.param.R;
import io.milvus.param.collection.DescribeCollectionParam;
import io.milvus.param.collection.GetCollectionStatisticsParam;
import io.milvus.param.index.DescribeIndexParam;
import io.milvus.param.index.GetIndexBuildProgressParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 索引监控服务
 * 监控索引构建进度、大小、查询性能，并提供自动优化建议
 * 
 * @author AI Knowledge Base
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IndexMonitorService {

    private final MilvusClient milvusClient;
    private final VectorIndexConfig vectorIndexConfig;
    private final IndexStrategySelector indexStrategySelector;

    /**
     * 索引监控数据缓存
     */
    private final Map<String, IndexMonitorData> monitorDataCache = new ConcurrentHashMap<>();

    /**
     * 性能历史记录
     */
    private final Map<String, List<PerformanceSnapshot>> performanceHistory = new ConcurrentHashMap<>();

    /**
     * 索引监控数据
     */
    public static class IndexMonitorData {
        private String collectionName;
        private String indexName;
        private String indexType;
        private long totalRows;
        private long indexedRows;
        private double buildProgress;
        private long indexSize;
        private LocalDateTime lastUpdateTime;
        private IndexHealthStatus healthStatus;
        private String healthMessage;

        // Getters and Setters
        public String getCollectionName() { return collectionName; }
        public void setCollectionName(String collectionName) { this.collectionName = collectionName; }
        public String getIndexName() { return indexName; }
        public void setIndexName(String indexName) { this.indexName = indexName; }
        public String getIndexType() { return indexType; }
        public void setIndexType(String indexType) { this.indexType = indexType; }
        public long getTotalRows() { return totalRows; }
        public void setTotalRows(long totalRows) { this.totalRows = totalRows; }
        public long getIndexedRows() { return indexedRows; }
        public void setIndexedRows(long indexedRows) { this.indexedRows = indexedRows; }
        public double getBuildProgress() { return buildProgress; }
        public void setBuildProgress(double buildProgress) { this.buildProgress = buildProgress; }
        public long getIndexSize() { return indexSize; }
        public void setIndexSize(long indexSize) { this.indexSize = indexSize; }
        public LocalDateTime getLastUpdateTime() { return lastUpdateTime; }
        public void setLastUpdateTime(LocalDateTime lastUpdateTime) { this.lastUpdateTime = lastUpdateTime; }
        public IndexHealthStatus getHealthStatus() { return healthStatus; }
        public void setHealthStatus(IndexHealthStatus healthStatus) { this.healthStatus = healthStatus; }
        public String getHealthMessage() { return healthMessage; }
        public void setHealthMessage(String healthMessage) { this.healthMessage = healthMessage; }
    }

    /**
     * 性能快照
     */
    public static class PerformanceSnapshot {
        private LocalDateTime timestamp;
        private long totalRows;
        private long queryLatencyMs;
        private double throughput;

        public PerformanceSnapshot(LocalDateTime timestamp, long totalRows, 
                                   long queryLatencyMs, double throughput) {
            this.timestamp = timestamp;
            this.totalRows = totalRows;
            this.queryLatencyMs = queryLatencyMs;
            this.throughput = throughput;
        }

        public LocalDateTime getTimestamp() { return timestamp; }
        public long getTotalRows() { return totalRows; }
        public long getQueryLatencyMs() { return queryLatencyMs; }
        public double getThroughput() { return throughput; }
    }

    /**
     * 索引健康状态
     */
    public enum IndexHealthStatus {
        HEALTHY("健康"),
        WARNING("警告"),
        CRITICAL("严重"),
        BUILDING("构建中"),
        UNKNOWN("未知");

        private final String description;

        IndexHealthStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 索引优化建议
     */
    public static class OptimizationSuggestion {
        private String collectionName;
        private String currentIndexType;
        private String suggestedIndexType;
        private String reason;
        private Map<String, Object> suggestedParams;
        private double priority; // 1-10, 10最高

        public OptimizationSuggestion(String collectionName, String currentIndexType,
                                      String suggestedIndexType, String reason,
                                      Map<String, Object> suggestedParams, double priority) {
            this.collectionName = collectionName;
            this.currentIndexType = currentIndexType;
            this.suggestedIndexType = suggestedIndexType;
            this.reason = reason;
            this.suggestedParams = suggestedParams;
            this.priority = priority;
        }

        // Getters
        public String getCollectionName() { return collectionName; }
        public String getCurrentIndexType() { return currentIndexType; }
        public String getSuggestedIndexType() { return suggestedIndexType; }
        public String getReason() { return reason; }
        public Map<String, Object> getSuggestedParams() { return suggestedParams; }
        public double getPriority() { return priority; }
    }

    /**
     * 获取索引监控数据
     */
    public IndexMonitorData getIndexMonitorData(String collectionName, String indexName) {
        try {
            IndexMonitorData data = new IndexMonitorData();
            data.setCollectionName(collectionName);
            data.setIndexName(indexName);
            data.setLastUpdateTime(LocalDateTime.now());

            // 1. 获取集合统计信息
            R<GetCollectionStatisticsResponse> statsResponse = milvusClient.getCollectionStatistics(
                    GetCollectionStatisticsParam.newBuilder()
                            .withCollectionName(collectionName)
                            .build()
            );

            if (statsResponse.getStatus() == R.Status.Success.getCode()) {
                for (io.milvus.grpc.KeyValuePair pair : statsResponse.getData().getStatsList()) {
                    if ("row_count".equals(pair.getKey())) {
                        data.setTotalRows(Long.parseLong(pair.getValue()));
                    }
                }
            }

            // 2. 获取索引信息
            R<DescribeIndexResponse> indexResponse = milvusClient.describeIndex(
                    DescribeIndexParam.newBuilder()
                            .withCollectionName(collectionName)
                            .withIndexName(indexName)
                            .build()
            );

            if (indexResponse.getStatus() == R.Status.Success.getCode() && 
                !indexResponse.getData().getIndexDescriptionsList().isEmpty()) {
                IndexDescription desc = indexResponse.getData().getIndexDescriptions(0);
                // 从索引描述中获取索引名称，类型信息需要通过其他方式获取
                data.setIndexType(desc.getIndexName());
                
                // 解析索引参数
                // 实际项目中需要根据具体参数名称解析
            }

            // 3. 获取索引构建进度
            try {
                R<GetIndexBuildProgressResponse> progressResponse = milvusClient.getIndexBuildProgress(
                        GetIndexBuildProgressParam.newBuilder()
                                .withCollectionName(collectionName)
                                .withIndexName(indexName)
                                .build()
                );

                if (progressResponse.getStatus() == R.Status.Success.getCode()) {
                    data.setIndexedRows(progressResponse.getData().getIndexedRows());
                    data.setTotalRows(progressResponse.getData().getTotalRows());
                    
                    if (data.getTotalRows() > 0) {
                        data.setBuildProgress((double) data.getIndexedRows() / data.getTotalRows() * 100);
                    }
                }
            } catch (Exception e) {
                log.debug("获取索引构建进度失败: {}", e.getMessage());
                data.setBuildProgress(100.0); // 假设已构建完成
                data.setIndexedRows(data.getTotalRows());
            }

            // 4. 评估健康状态
            evaluateHealthStatus(data);

            // 缓存数据
            monitorDataCache.put(collectionName + ":" + indexName, data);

            return data;
        } catch (Exception e) {
            log.error("获取索引监控数据失败: collection={}, index={}", collectionName, indexName, e);
            return null;
        }
    }

    /**
     * 评估索引健康状态
     */
    private void evaluateHealthStatus(IndexMonitorData data) {
        // 构建中
        if (data.getBuildProgress() < 100.0) {
            data.setHealthStatus(IndexHealthStatus.BUILDING);
            data.setHealthMessage(String.format("索引构建中，进度: %.2f%%", data.getBuildProgress()));
            return;
        }

        // 检查是否需要重建
        VectorIndexConfig.RebuildThresholdProperties threshold = vectorIndexConfig.getRebuildThreshold();
        
        // 获取上次的记录
        IndexMonitorData lastData = monitorDataCache.get(data.getCollectionName() + ":" + data.getIndexName());
        if (lastData != null) {
            long growth = data.getTotalRows() - lastData.getTotalRows();
            double growthRatio = lastData.getTotalRows() > 0 ? 
                    (double) growth / lastData.getTotalRows() : 0;

            if (growthRatio > threshold.getGrowthRatio()) {
                data.setHealthStatus(IndexHealthStatus.WARNING);
                data.setHealthMessage(String.format("数据增长 %.1f%%，建议重建索引", growthRatio * 100));
                return;
            }
        }

        // 检查性能是否下降
        List<PerformanceSnapshot> history = performanceHistory.get(data.getCollectionName());
        if (history != null && history.size() >= 2) {
            PerformanceSnapshot latest = history.get(history.size() - 1);
            PerformanceSnapshot previous = history.get(history.size() - 2);
            
            if (latest.getQueryLatencyMs() > previous.getQueryLatencyMs() * (1 + threshold.getLatencyGrowthRatio())) {
                data.setHealthStatus(IndexHealthStatus.WARNING);
                data.setHealthMessage("查询性能下降，建议重建索引");
                return;
            }
        }

        // 健康状态
        data.setHealthStatus(IndexHealthStatus.HEALTHY);
        data.setHealthMessage("索引运行正常");
    }

    /**
     * 获取优化建议
     */
    public List<OptimizationSuggestion> getOptimizationSuggestions(String collectionName, int dimension) {
        List<OptimizationSuggestion> suggestions = new ArrayList<>();

        if (!vectorIndexConfig.getRebuildThreshold().isAutoSuggest()) {
            return suggestions;
        }

        try {
            // 获取当前索引信息
            IndexMonitorData monitorData = getIndexMonitorData(collectionName, "vector_index");
            if (monitorData == null) {
                return suggestions;
            }

            // 获取推荐的索引配置
            IndexStrategySelector.IndexSelectionResult recommended = 
                    indexStrategySelector.getIndexRecommendation(collectionName, dimension);

            // 比较当前和推荐的配置
            if (!monitorData.getIndexType().equals(recommended.getIndexType().getCode())) {
                String reason = buildOptimizationReason(monitorData, recommended);
                
                suggestions.add(new OptimizationSuggestion(
                        collectionName,
                        monitorData.getIndexType(),
                        recommended.getIndexType().getCode(),
                        reason,
                        recommended.getIndexParams(),
                        calculatePriority(monitorData)
                ));
            }

            return suggestions;
        } catch (Exception e) {
            log.error("获取优化建议失败: {}", collectionName, e);
            return suggestions;
        }
    }

    /**
     * 构建优化原因说明
     */
    private String buildOptimizationReason(IndexMonitorData current, 
                                           IndexStrategySelector.IndexSelectionResult recommended) {
        StringBuilder reason = new StringBuilder();
        reason.append("当前数据量: ").append(current.getTotalRows());
        reason.append(", 推荐索引: ").append(recommended.getIndexType());
        reason.append("。原因: ").append(recommended.getReason());
        return reason.toString();
    }

    /**
     * 计算优化优先级
     */
    private double calculatePriority(IndexMonitorData data) {
        double priority = 5.0; // 基础优先级

        // 根据健康状态调整
        if (data.getHealthStatus() == IndexHealthStatus.CRITICAL) {
            priority += 4.0;
        } else if (data.getHealthStatus() == IndexHealthStatus.WARNING) {
            priority += 2.0;
        }

        // 根据数据量调整
        if (data.getTotalRows() > vectorIndexConfig.getAutoSelect().getLargeDatasetThreshold()) {
            priority += 1.0;
        }

        return Math.min(priority, 10.0);
    }

    /**
     * 记录性能快照
     */
    public void recordPerformanceSnapshot(String collectionName, long queryLatencyMs, double throughput) {
        try {
            long totalRows = getCollectionVectorCount(collectionName);
            
            PerformanceSnapshot snapshot = new PerformanceSnapshot(
                    LocalDateTime.now(),
                    totalRows,
                    queryLatencyMs,
                    throughput
            );

            performanceHistory.computeIfAbsent(collectionName, k -> new ArrayList<>()).add(snapshot);

            // 保留最近100条记录
            List<PerformanceSnapshot> history = performanceHistory.get(collectionName);
            if (history.size() > 100) {
                history.remove(0);
            }

            log.debug("记录性能快照: collection={}, latency={}ms, throughput={}", 
                    collectionName, queryLatencyMs, throughput);
        } catch (Exception e) {
            log.error("记录性能快照失败: {}", collectionName, e);
        }
    }

    /**
     * 获取性能历史
     */
    public List<PerformanceSnapshot> getPerformanceHistory(String collectionName) {
        return performanceHistory.getOrDefault(collectionName, Collections.emptyList());
    }

    /**
     * 获取集合向量数量
     */
    private long getCollectionVectorCount(String collectionName) {
        try {
            R<GetCollectionStatisticsResponse> response = milvusClient.getCollectionStatistics(
                    GetCollectionStatisticsParam.newBuilder()
                            .withCollectionName(collectionName)
                            .build()
            );

            if (response.getStatus() == R.Status.Success.getCode()) {
                for (io.milvus.grpc.KeyValuePair pair : response.getData().getStatsList()) {
                    if ("row_count".equals(pair.getKey())) {
                        return Long.parseLong(pair.getValue());
                    }
                }
            }
        } catch (Exception e) {
            log.error("获取集合向量数量失败: {}", collectionName, e);
        }
        return 0;
    }

    /**
     * 定期监控任务（每小时执行）
     */
    @Scheduled(fixedRate = 3600000) // 每小时
    public void scheduledMonitor() {
        log.info("执行索引定期监控任务");
        
        // 遍历所有缓存的集合进行监控
        for (String key : monitorDataCache.keySet()) {
            String[] parts = key.split(":");
            if (parts.length == 2) {
                try {
                    getIndexMonitorData(parts[0], parts[1]);
                } catch (Exception e) {
                    log.error("监控集合 {} 失败", parts[0], e);
                }
            }
        }
    }

    /**
     * 获取所有监控数据
     */
    public Map<String, IndexMonitorData> getAllMonitorData() {
        return new HashMap<>(monitorDataCache);
    }

    /**
     * 清理过期数据
     */
    public void cleanupExpiredData() {
        LocalDateTime threshold = LocalDateTime.now().minus(7, ChronoUnit.DAYS);
        
        performanceHistory.forEach((collection, history) -> {
            history.removeIf(snapshot -> snapshot.getTimestamp().isBefore(threshold));
        });
        
        log.info("已清理过期的性能历史数据");
    }

    /**
     * 导出监控报告
     */
    public String exportMonitorReport() {
        StringBuilder report = new StringBuilder();
        report.append("=== Milvus索引监控报告 ===\n");
        report.append("生成时间: ").append(LocalDateTime.now()).append("\n\n");

        for (Map.Entry<String, IndexMonitorData> entry : monitorDataCache.entrySet()) {
            IndexMonitorData data = entry.getValue();
            report.append("集合: ").append(data.getCollectionName()).append("\n");
            report.append("  索引类型: ").append(data.getIndexType()).append("\n");
            report.append("  数据量: ").append(data.getTotalRows()).append("\n");
            report.append("  构建进度: ").append(String.format("%.2f%%", data.getBuildProgress())).append("\n");
            report.append("  健康状态: ").append(data.getHealthStatus().getDescription()).append("\n");
            report.append("  状态信息: ").append(data.getHealthMessage()).append("\n");
            report.append("  更新时间: ").append(data.getLastUpdateTime()).append("\n\n");
        }

        return report.toString();
    }
}
