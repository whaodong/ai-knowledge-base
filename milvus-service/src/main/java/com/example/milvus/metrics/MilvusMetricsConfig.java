package com.example.milvus.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.Getter;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Milvus服务监控指标配置
 */
@Configuration
public class MilvusMetricsConfig {

    private final MeterRegistry meterRegistry;

    @Getter
    private final Counter vectorInsertCount;

    @Getter
    private final Counter vectorSearchCount;

    @Getter
    private final Timer vectorSearchLatency;

    @Getter
    private final Timer vectorInsertLatency;

    @Getter
    private final AtomicLong totalVectors = new AtomicLong(0);

    @Getter
    private final AtomicLong collectionCount = new AtomicLong(0);

    public MilvusMetricsConfig(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        // 向量插入计数器
        this.vectorInsertCount = Counter.builder("milvus_vector_insert_count")
            .description("Total number of vectors inserted")
            .tag("service", "milvus-service")
            .register(meterRegistry);

        // 向量搜索计数器
        this.vectorSearchCount = Counter.builder("milvus_vector_search_count")
            .description("Total number of vector searches")
            .tag("service", "milvus-service")
            .register(meterRegistry);

        // 向量搜索延迟
        this.vectorSearchLatency = Timer.builder("milvus_vector_search_latency")
            .description("Vector search latency")
            .tag("service", "milvus-service")
            .publishPercentiles(0.5, 0.95, 0.99)
            .publishPercentileHistogram()
            .minimumExpectedValue(java.time.Duration.ofMillis(1))
            .maximumExpectedValue(java.time.Duration.ofSeconds(10))
            .register(meterRegistry);

        // 向量插入延迟
        this.vectorInsertLatency = Timer.builder("milvus_vector_insert_latency")
            .description("Vector insert latency")
            .tag("service", "milvus-service")
            .publishPercentiles(0.5, 0.95, 0.99)
            .publishPercentileHistogram()
            .minimumExpectedValue(java.time.Duration.ofMillis(1))
            .maximumExpectedValue(java.time.Duration.ofSeconds(30))
            .register(meterRegistry);

        // 总向量数
        Gauge.builder("milvus_total_vectors", totalVectors, AtomicLong::get)
            .description("Total number of vectors stored")
            .tag("service", "milvus-service")
            .register(meterRegistry);

        // Collection数量
        Gauge.builder("milvus_collection_count", collectionCount, AtomicLong::get)
            .description("Number of collections")
            .tag("service", "milvus-service")
            .register(meterRegistry);
    }

    /**
     * 更新向量总数
     */
    public void updateTotalVectors(long count) {
        totalVectors.set(count);
    }

    /**
     * 更新Collection数量
     */
    public void updateCollectionCount(long count) {
        collectionCount.set(count);
    }
}
