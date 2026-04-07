package com.example.milvus.index;

import io.milvus.client.MilvusClient;
import io.milvus.grpc.GetCollectionStatisticsResponse;
import io.milvus.param.R;
import io.milvus.param.collection.GetCollectionStatisticsParam;
import io.milvus.param.collection.HasCollectionParam;
import io.milvus.param.index.CreateIndexParam;
import io.milvus.param.index.DropIndexParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 索引策略选择器
 * 根据数据规模、内存限制、召回率要求自动选择最优索引类型和参数
 * 
 * @author AI Knowledge Base
 * @version 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IndexStrategySelector {

    private final MilvusClient milvusClient;
    private final VectorIndexConfig vectorIndexConfig;

    /**
     * 索引选择结果
     */
    public static class IndexSelectionResult {
        private final VectorIndexConfig.IndexType indexType;
        private final Map<String, Object> indexParams;
        private final Map<String, Object> searchParams;
        private final String reason;

        public IndexSelectionResult(VectorIndexConfig.IndexType indexType,
                                    Map<String, Object> indexParams,
                                    Map<String, Object> searchParams,
                                    String reason) {
            this.indexType = indexType;
            this.indexParams = indexParams;
            this.searchParams = searchParams;
            this.reason = reason;
        }

        public VectorIndexConfig.IndexType getIndexType() {
            return indexType;
        }

        public Map<String, Object> getIndexParams() {
            return indexParams;
        }

        public Map<String, Object> getSearchParams() {
            return searchParams;
        }

        public String getReason() {
            return reason;
        }
    }

    /**
     * 自动选择最优索引策略
     * 
     * @param collectionName 集合名称
     * @param dimension 向量维度
     * @return 索引选择结果
     */
    public IndexSelectionResult selectOptimalIndex(String collectionName, int dimension) {
        if (!vectorIndexConfig.getAutoSelect().isEnabled()) {
            log.info("自动索引选择未启用，使用默认配置");
            return getDefaultIndexSelection(dimension);
        }

        try {
            // 1. 检查集合是否存在
            if (!collectionExists(collectionName)) {
                log.info("集合 {} 不存在，使用默认索引策略", collectionName);
                return getDefaultIndexSelection(dimension);
            }

            // 2. 获取集合统计信息
            long vectorCount = getCollectionVectorCount(collectionName);
            log.info("集合 {} 当前向量数量: {}", collectionName, vectorCount);

            // 3. 根据数据规模选择索引类型
            IndexSelectionResult result = selectByDataScale(vectorCount, dimension);
            
            log.info("为集合 {} 选择索引类型: {}, 原因: {}", 
                    collectionName, result.getIndexType(), result.getReason());
            
            return result;

        } catch (Exception e) {
            log.error("自动选择索引失败，使用默认配置", e);
            return getDefaultIndexSelection(dimension);
        }
    }

    /**
     * 根据数据规模选择索引类型
     */
    private IndexSelectionResult selectByDataScale(long vectorCount, int dimension) {
        VectorIndexConfig.AutoSelectProperties autoSelect = vectorIndexConfig.getAutoSelect();
        long smallThreshold = autoSelect.getSmallDatasetThreshold();
        long mediumThreshold = autoSelect.getMediumDatasetThreshold();
        long largeThreshold = autoSelect.getLargeDatasetThreshold();

        // 小数据集：使用FLAT
        if (vectorCount < smallThreshold) {
            return createFlatResult(vectorCount);
        }

        // 中等数据集：使用IVF_FLAT
        if (vectorCount < mediumThreshold) {
            // 检查内存限制
            if (autoSelect.isConsiderMemory() && isMemoryConstrained(vectorCount, dimension)) {
                log.info("内存受限，考虑使用压缩索引");
                return createIvfPqResult(vectorCount, dimension);
            }
            return createIvfFlatResult(vectorCount);
        }

        // 大数据集：使用HNSW
        if (vectorCount < largeThreshold) {
            // 检查内存限制
            if (autoSelect.isConsiderMemory() && isMemoryConstrained(vectorCount, dimension)) {
                log.info("内存受限，使用IVF_PQ索引");
                return createIvfPqResult(vectorCount, dimension);
            }
            
            // 检查召回率要求
            double recallRequirement = autoSelect.getRecallRequirement();
            if (recallRequirement >= 0.98) {
                log.info("高召回率要求，使用HNSW索引");
                return createHnswResult(recallRequirement);
            }
            
            return createIvfFlatResult(vectorCount);
        }

        // 超大数据集：使用IVF_PQ
        log.info("超大数据集，使用IVF_PQ压缩索引");
        return createIvfPqResult(vectorCount, dimension);
    }

    /**
     * 创建FLAT索引选择结果
     */
    private IndexSelectionResult createFlatResult(long vectorCount) {
        Map<String, Object> indexParams = new HashMap<>();
        Map<String, Object> searchParams = new HashMap<>();
        
        String reason = String.format("数据量 %d < %d (小数据集阈值)，使用FLAT暴力搜索", 
                vectorCount, vectorIndexConfig.getAutoSelect().getSmallDatasetThreshold());
        
        return new IndexSelectionResult(
                VectorIndexConfig.IndexType.FLAT,
                indexParams,
                searchParams,
                reason
        );
    }

    /**
     * 创建IVF_FLAT索引选择结果
     */
    private IndexSelectionResult createIvfFlatResult(long vectorCount) {
        Map<String, Object> indexParams = vectorIndexConfig.getIvfFlatParams(vectorCount);
        Map<String, Object> searchParams = new HashMap<>();
        searchParams.put("nprobe", indexParams.get("nprobe"));
        
        String reason = String.format("数据量 %d 在中等规模范围，使用IVF_FLAT平衡速度和精度", vectorCount);
        
        return new IndexSelectionResult(
                VectorIndexConfig.IndexType.IVF_FLAT,
                indexParams,
                searchParams,
                reason
        );
    }

    /**
     * 创建HNSW索引选择结果
     */
    private IndexSelectionResult createHnswResult(double recallRequirement) {
        Map<String, Object> indexParams = vectorIndexConfig.getHnswParams(recallRequirement);
        Map<String, Object> searchParams = new HashMap<>();
        searchParams.put("ef", indexParams.get("ef"));
        
        String reason = String.format("高召回率要求 %.2f，使用HNSW索引", recallRequirement);
        
        return new IndexSelectionResult(
                VectorIndexConfig.IndexType.HNSW,
                indexParams,
                searchParams,
                reason
        );
    }

    /**
     * 创建IVF_PQ索引选择结果
     */
    private IndexSelectionResult createIvfPqResult(long vectorCount, int dimension) {
        Map<String, Object> indexParams = vectorIndexConfig.getIvfPqParams(vectorCount, dimension);
        Map<String, Object> searchParams = new HashMap<>();
        searchParams.put("nprobe", indexParams.get("nprobe"));
        
        String reason = String.format("数据量 %d 或内存受限，使用IVF_PQ压缩索引", vectorCount);
        
        return new IndexSelectionResult(
                VectorIndexConfig.IndexType.IVF_PQ,
                indexParams,
                searchParams,
                reason
        );
    }

    /**
     * 检查是否内存受限
     */
    private boolean isMemoryConstrained(long vectorCount, int dimension) {
        // 估算内存占用（字节）
        // 每个向量: dimension * 4 bytes (float32)
        long vectorMemory = vectorCount * dimension * 4L;
        
        // 索引额外开销估算
        // IVF_FLAT: ~10-20%
        // HNSW: ~30-50%
        // IVF_PQ: ~5-10%
        long estimatedMemory = (long) (vectorMemory * 1.5); // 使用HNSW的估算
        
        double memoryLimitBytes = vectorIndexConfig.getAutoSelect().getMemoryLimitGB() * 1024 * 1024 * 1024;
        
        log.debug("估算内存占用: {} GB, 限制: {} GB", 
                estimatedMemory / (1024.0 * 1024 * 1024),
                vectorIndexConfig.getAutoSelect().getMemoryLimitGB());
        
        return estimatedMemory > memoryLimitBytes;
    }

    /**
     * 获取默认索引选择
     */
    private IndexSelectionResult getDefaultIndexSelection(int dimension) {
        // 默认使用HNSW，适合大多数场景
        Map<String, Object> indexParams = vectorIndexConfig.getHnswParams(0.95);
        Map<String, Object> searchParams = new HashMap<>();
        searchParams.put("ef", indexParams.get("ef"));
        
        return new IndexSelectionResult(
                VectorIndexConfig.IndexType.HNSW,
                indexParams,
                searchParams,
                "使用默认索引配置（HNSW）"
        );
    }

    /**
     * 检查集合是否存在
     */
    private boolean collectionExists(String collectionName) {
        try {
            R<Boolean> response = milvusClient.hasCollection(
                    HasCollectionParam.newBuilder()
                            .withCollectionName(collectionName)
                            .build()
            );
            return response.getData() != null && response.getData();
        } catch (Exception e) {
            log.error("检查集合存在性失败: {}", collectionName, e);
            return false;
        }
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
            
            if (response.getStatus() != R.Status.Success.getCode()) {
                log.warn("获取集合统计信息失败: {}", response.getMessage());
                return 0;
            }
            
            // 解析统计信息
            GetCollectionStatisticsResponse stats = response.getData();
            for (io.milvus.grpc.KeyValuePair pair : stats.getStatsList()) {
                if ("row_count".equals(pair.getKey())) {
                    return Long.parseLong(pair.getValue());
                }
            }
            
            return 0;
        } catch (Exception e) {
            log.error("获取集合向量数量失败: {}", collectionName, e);
            return 0;
        }
    }

    /**
     * 应用索引选择结果
     * 创建或重建索引
     * 
     * @param collectionName 集合名称
     * @param fieldName 向量字段名
     * @param indexName 索引名称
     * @param result 索引选择结果
     * @return 是否成功
     */
    public boolean applyIndexSelection(String collectionName, String fieldName, 
                                       String indexName, IndexSelectionResult result) {
        try {
            // 1. 先尝试删除旧索引
            try {
                milvusClient.dropIndex(DropIndexParam.newBuilder()
                        .withCollectionName(collectionName)
                        .withIndexName(indexName)
                        .build());
                log.info("已删除旧索引: {}", indexName);
            } catch (Exception e) {
                log.debug("删除索引失败（可能不存在）: {}", e.getMessage());
            }

            // 2. 创建新索引
            CreateIndexParam.Builder builder = CreateIndexParam.newBuilder()
                    .withCollectionName(collectionName)
                    .withFieldName(fieldName)
                    .withIndexName(indexName)
                    .withIndexType(io.milvus.param.IndexType.valueOf(result.getIndexType().getCode()))
                    .withMetricType(io.milvus.param.MetricType.COSINE)
                    .withExtraParam(result.getIndexParams().toString());

            R<Boolean> response = milvusClient.createIndex(builder.build());
            
            if (response.getStatus() == R.Status.Success.getCode()) {
                log.info("成功创建索引: {}, 类型: {}", indexName, result.getIndexType());
                return true;
            } else {
                log.error("创建索引失败: {}", response.getMessage());
                return false;
            }
        } catch (Exception e) {
            log.error("应用索引选择失败", e);
            return false;
        }
    }

    /**
     * 获取索引选择建议（不实际应用）
     */
    public IndexSelectionResult getIndexRecommendation(String collectionName, int dimension) {
        return selectOptimalIndex(collectionName, dimension);
    }

    /**
     * 批量获取多个集合的索引建议
     */
    public Map<String, IndexSelectionResult> batchGetIndexRecommendations(
            Map<String, Integer> collectionDimensions) {
        Map<String, IndexSelectionResult> recommendations = new HashMap<>();
        
        for (Map.Entry<String, Integer> entry : collectionDimensions.entrySet()) {
            try {
                IndexSelectionResult result = selectOptimalIndex(entry.getKey(), entry.getValue());
                recommendations.put(entry.getKey(), result);
            } catch (Exception e) {
                log.error("获取集合 {} 的索引建议失败", entry.getKey(), e);
            }
        }
        
        return recommendations;
    }
}
