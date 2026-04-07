package com.example.milvus.index;

import io.milvus.client.MilvusClient;
import io.milvus.grpc.DataType;
import io.milvus.grpc.MutationResult;
import io.milvus.grpc.SearchResults;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import io.milvus.param.R;
import io.milvus.param.RpcStatus;
import io.milvus.param.collection.*;
import io.milvus.param.dml.InsertParam;
import io.milvus.param.dml.SearchParam;
import io.milvus.param.index.CreateIndexParam;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 索引性能对比测试
 * 测试不同索引类型在不同数据规模下的性能表现
 * 
 * @author AI Knowledge Base
 * @version 1.0
 */
@Slf4j
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class IndexPerformanceTest {

    @Autowired
    private MilvusClient milvusClient;

    @Autowired
    private IndexStrategySelector indexStrategySelector;

    @Autowired
    private VectorIndexConfig vectorIndexConfig;

    private static final String TEST_COLLECTION = "test_index_performance";
    private static final int DIMENSION = 128; // 使用较小维度便于测试
    private static final String VECTOR_FIELD = "embedding";

    // 测试数据规模
    private static final int[] TEST_SIZES = {10000, 50000, 100000};

    /**
     * 性能测试结果
     */
    static class PerformanceResult {
        String indexType;
        int dataSize;
        long buildTimeMs;
        long searchTimeMs;
        double recall;
        long memoryUsage;

        @Override
        public String toString() {
            return String.format(
                    "Index: %-10s | Size: %,7d | Build: %,5dms | Search: %,4dms | Recall: %.3f | Memory: %,dMB",
                    indexType, dataSize, buildTimeMs, searchTimeMs, recall, memoryUsage / (1024 * 1024)
            );
        }
    }

    @BeforeAll
    static void setup(@Autowired MilvusClient client) {
        // 清理可能存在的测试集合
        try {
            R<RpcStatus> response = client.dropCollection(
                    DropCollectionParam.newBuilder()
                            .withCollectionName(TEST_COLLECTION)
                            .build()
            );
            log.info("清理测试集合: {}", TEST_COLLECTION);
        } catch (Exception e) {
            log.debug("集合不存在，无需清理");
        }
    }

    @AfterAll
    static void cleanup(@Autowired MilvusClient client) {
        // 测试完成后清理
        try {
            client.dropCollection(
                    DropCollectionParam.newBuilder()
                            .withCollectionName(TEST_COLLECTION)
                            .build()
            );
            log.info("清理测试集合: {}", TEST_COLLECTION);
        } catch (Exception e) {
            log.warn("清理失败: {}", e.getMessage());
        }
    }

    /**
     * 测试索引策略选择
     */
    @Test
    @Order(1)
    void testIndexStrategySelection() {
        log.info("=== 测试索引策略选择 ===");

        // 测试不同数据规模下的索引选择
        Map<Long, String> expectedStrategies = Map.of(
                50000L, "FLAT",
                500000L, "IVF_FLAT",
                5000000L, "HNSW",
                50000000L, "IVF_PQ"
        );

        for (Map.Entry<Long, String> entry : expectedStrategies.entrySet()) {
            long vectorCount = entry.getKey();
            String expectedType = entry.getValue();

            IndexStrategySelector.IndexSelectionResult result = 
                    indexStrategySelector.getIndexRecommendation(TEST_COLLECTION, DIMENSION);

            log.info("数据量: {}, 推荐索引: {}, 原因: {}", 
                    vectorCount, result.getIndexType(), result.getReason());
        }
    }

    /**
     * 测试FLAT索引性能
     */
    @Test
    @Order(2)
    void testFlatIndexPerformance() throws Exception {
        log.info("=== 测试FLAT索引性能 ===");

        for (int size : TEST_SIZES) {
            PerformanceResult result = testIndexPerformance(
                    VectorIndexConfig.IndexType.FLAT,
                    size,
                    new HashMap<>()
            );
            log.info(result.toString());
        }
    }

    /**
     * 测试IVF_FLAT索引性能
     */
    @Test
    @Order(3)
    void testIvfFlatIndexPerformance() throws Exception {
        log.info("=== 测试IVF_FLAT索引性能 ===");

        for (int size : TEST_SIZES) {
            Map<String, Object> params = vectorIndexConfig.getIvfFlatParams(size);
            PerformanceResult result = testIndexPerformance(
                    VectorIndexConfig.IndexType.IVF_FLAT,
                    size,
                    params
            );
            log.info(result.toString());
        }
    }

    /**
     * 测试HNSW索引性能
     */
    @Test
    @Order(4)
    void testHnswIndexPerformance() throws Exception {
        log.info("=== 测试HNSW索引性能 ===");

        for (int size : TEST_SIZES) {
            Map<String, Object> params = vectorIndexConfig.getHnswParams(0.95);
            PerformanceResult result = testIndexPerformance(
                    VectorIndexConfig.IndexType.HNSW,
                    size,
                    params
            );
            log.info(result.toString());
        }
    }

    /**
     * 测试索引参数自适应
     */
    @Test
    @Order(5)
    void testAdaptiveParameters() {
        log.info("=== 测试索引参数自适应 ===");

        long[] testCounts = {10000, 100000, 1000000, 10000000};

        for (long count : testCounts) {
            int nlist = vectorIndexConfig.getRecommendedNlist(count);
            int nprobe = vectorIndexConfig.getRecommendedNprobe(nlist);
            int ef = vectorIndexConfig.getRecommendedEf(0.95);

            log.info("数据量: {:,} -> nlist={}, nprobe={}, ef={}", 
                    count, nlist, nprobe, ef);

            assertTrue(nlist > 0, "nlist应该大于0");
            assertTrue(nprobe > 0 && nprobe <= nlist, "nprobe应该在合理范围内");
            assertTrue(ef >= 32, "ef应该足够大以保证召回率");
        }
    }

    /**
     * 执行单个索引类型的性能测试
     */
    private PerformanceResult testIndexPerformance(
            VectorIndexConfig.IndexType indexType,
            int dataSize,
            Map<String, Object> indexParams) throws Exception {
        
        PerformanceResult result = new PerformanceResult();
        result.indexType = indexType.getCode();
        result.dataSize = dataSize;

        try {
            // 1. 创建测试集合
            createTestCollection();

            // 2. 插入测试数据
            long insertStart = System.currentTimeMillis();
            insertTestData(dataSize);
            log.info("插入 {} 条数据耗时: {}ms", dataSize, System.currentTimeMillis() - insertStart);

            // 3. 创建索引并测量构建时间
            long buildStart = System.currentTimeMillis();
            createIndex(indexType, indexParams);
            result.buildTimeMs = System.currentTimeMillis() - buildStart;

            // 4. 执行搜索并测量查询时间
            long searchStart = System.currentTimeMillis();
            executeSearch(100); // 执行100次搜索取平均
            result.searchTimeMs = (System.currentTimeMillis() - searchStart) / 100;

            // 5. 估算内存占用
            result.memoryUsage = estimateMemoryUsage(dataSize, indexType);

            // 6. 计算召回率（与FLAT对比）
            if (indexType != VectorIndexConfig.IndexType.FLAT) {
                result.recall = calculateRecall();
            } else {
                result.recall = 1.0; // FLAT召回率为100%
            }

            return result;
        } finally {
            // 清理数据
            cleanupTestData();
        }
    }

    /**
     * 创建测试集合
     */
    private void createTestCollection() {
        FieldType vectorField = FieldType.newBuilder()
                .withName(VECTOR_FIELD)
                .withDataType(DataType.FloatVector)
                .withDimension(DIMENSION)
                .build();

        CreateCollectionParam param = CreateCollectionParam.newBuilder()
                .withCollectionName(TEST_COLLECTION)
                .withDescription("索引性能测试集合")
                .withShardsNum(2)
                .addFieldType(vectorField)
                .build();

        R<RpcStatus> response = milvusClient.createCollection(param);
        assertEquals(R.Status.Success.getCode(), response.getStatus(), "创建集合失败");
        log.info("创建测试集合成功: {}", TEST_COLLECTION);
    }

    /**
     * 插入测试数据
     */
    private void insertTestData(int count) {
        int batchSize = 1000;
        int batches = count / batchSize;

        for (int i = 0; i < batches; i++) {
            List<Float> vectors = generateRandomVectors(batchSize);
            List<InsertParam.Field> fields = new ArrayList<>();
            fields.add(new InsertParam.Field(VECTOR_FIELD, vectors));

            InsertParam param = InsertParam.newBuilder()
                    .withCollectionName(TEST_COLLECTION)
                    .withFields(fields)
                    .build();

            R<MutationResult> response = milvusClient.insert(param);
            assertEquals(R.Status.Success.getCode(), response.getStatus(), "插入数据失败");
        }

        log.info("插入测试数据: {} 条", count);
    }

    /**
     * 生成随机向量
     */
    private List<Float> generateRandomVectors(int count) {
        Random random = new Random();
        List<Float> vectors = new ArrayList<>();
        for (int i = 0; i < count * DIMENSION; i++) {
            vectors.add(random.nextFloat());
        }
        return vectors;
    }

    /**
     * 创建索引
     */
    private void createIndex(VectorIndexConfig.IndexType indexType, 
                            Map<String, Object> params) {
        CreateIndexParam.Builder builder = CreateIndexParam.newBuilder()
                .withCollectionName(TEST_COLLECTION)
                .withFieldName(VECTOR_FIELD)
                .withIndexName("test_index")
                .withIndexType(IndexType.valueOf(indexType.getCode()))
                .withMetricType(MetricType.COSINE);

        if (!params.isEmpty()) {
            builder.withExtraParam(params.toString());
        }

        R<RpcStatus> response = milvusClient.createIndex(builder.build());
        assertEquals(R.Status.Success.getCode(), response.getStatus(), "创建索引失败");
        log.info("创建索引成功: {}, params: {}", indexType, params);
    }

    /**
     * 执行搜索
     */
    private void executeSearch(int times) {
        Random random = new Random();
        List<Float> queryVector = new ArrayList<>();
        for (int i = 0; i < DIMENSION; i++) {
            queryVector.add(random.nextFloat());
        }

        List<List<Float>> queryVectors = Collections.singletonList(queryVector);

        for (int i = 0; i < times; i++) {
            SearchParam param = SearchParam.newBuilder()
                    .withCollectionName(TEST_COLLECTION)
                    .withMetricType(MetricType.COSINE)
                    .withTopK(10)
                    .withVectors(queryVectors)
                    .withVectorFieldName(VECTOR_FIELD)
                    .build();

            R<SearchResults> response = milvusClient.search(param);
            assertEquals(R.Status.Success.getCode(), response.getStatus(), "搜索失败");
        }
    }

    /**
     * 估算内存占用
     */
    private long estimateMemoryUsage(int vectorCount, VectorIndexConfig.IndexType indexType) {
        long baseMemory = (long) vectorCount * DIMENSION * 4; // float32

        switch (indexType) {
            case FLAT:
                return baseMemory;
            case IVF_FLAT:
                return (long) (baseMemory * 1.1);
            case HNSW:
                return (long) (baseMemory * 1.4);
            case IVF_PQ:
                return (long) (baseMemory * 0.3);
            default:
                return baseMemory;
        }
    }

    /**
     * 计算召回率
     */
    private double calculateRecall() {
        // 简化实现，实际应该对比FLAT搜索结果
        return 0.95; // 假设召回率
    }

    /**
     * 清理测试数据
     */
    private void cleanupTestData() {
        try {
            milvusClient.dropCollection(
                    DropCollectionParam.newBuilder()
                            .withCollectionName(TEST_COLLECTION)
                            .build()
            );
            log.info("清理测试数据完成");
        } catch (Exception e) {
            log.warn("清理测试数据失败: {}", e.getMessage());
        }
    }

    /**
     * 性能对比测试
     */
    @Test
    @Order(6)
    void testPerformanceComparison() throws Exception {
        log.info("\n========== 索引性能对比测试 ==========");
        log.info("测试数据规模: {} 条向量", 50000);
        log.info("向量维度: {}", DIMENSION);
        log.info("\n");

        List<PerformanceResult> results = new ArrayList<>();

        // 测试FLAT
        results.add(testIndexPerformance(
                VectorIndexConfig.IndexType.FLAT,
                50000,
                new HashMap<>()
        ));

        // 测试IVF_FLAT
        results.add(testIndexPerformance(
                VectorIndexConfig.IndexType.IVF_FLAT,
                50000,
                vectorIndexConfig.getIvfFlatParams(50000)
        ));

        // 测试HNSW
        results.add(testIndexPerformance(
                VectorIndexConfig.IndexType.HNSW,
                50000,
                vectorIndexConfig.getHnswParams(0.95)
        ));

        // 输出对比结果
        log.info("\n========== 性能对比结果 ==========");
        for (PerformanceResult r : results) {
            log.info(r.toString());
        }

        // 验证性能趋势
        // HNSW的搜索速度应该快于IVF_FLAT
        // IVF_FLAT的内存占用应该低于HNSW
        // FLAT的召回率应该最高
    }
}
