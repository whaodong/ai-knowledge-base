package com.example.rag.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.Getter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.atomic.AtomicLong;

/**
 * RAG服务监控指标配置
 * 定义和注册自定义Prometheus指标
 */
@Configuration
public class RagMetricsConfig {

    private final MeterRegistry meterRegistry;

    @Getter
    private final Counter ragQueryCount;

    @Getter
    private final Timer ragQueryLatency;

    @Getter
    private final Counter embeddingTokens;

    @Getter
    private final Timer milvusLatency;

    @Getter
    private final AtomicLong activeQueries = new AtomicLong(0);

    @Getter
    private final AtomicLong cacheHits = new AtomicLong(0);

    @Getter
    private final AtomicLong cacheMisses = new AtomicLong(0);

    public RagMetricsConfig(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        // RAG查询次数计数器
        this.ragQueryCount = Counter.builder("rag_query_count")
            .description("Total number of RAG queries")
            .tag("service", "rag-service")
            .register(meterRegistry);

        // RAG查询延迟计时器
        this.ragQueryLatency = Timer.builder("rag_query_latency")
            .description("RAG query latency")
            .tag("service", "rag-service")
            .publishPercentiles(0.5, 0.95, 0.99)
            .publishPercentileHistogram()
            .minimumExpectedValue(java.time.Duration.ofMillis(1))
            .maximumExpectedValue(java.time.Duration.ofSeconds(30))
            .register(meterRegistry);

        // Token使用量计数器
        this.embeddingTokens = Counter.builder("embedding_tokens")
            .description("Total number of tokens used in embeddings")
            .tag("service", "rag-service")
            .register(meterRegistry);

        // Milvus查询延迟计时器
        this.milvusLatency = Timer.builder("milvus_latency")
            .description("Milvus query latency")
            .tag("service", "rag-service")
            .publishPercentiles(0.5, 0.95, 0.99)
            .publishPercentileHistogram()
            .minimumExpectedValue(java.time.Duration.ofMillis(1))
            .maximumExpectedValue(java.time.Duration.ofSeconds(10))
            .register(meterRegistry);

        // 活跃查询数量
        Gauge.builder("rag_active_queries", activeQueries, AtomicLong::get)
            .description("Number of active RAG queries")
            .tag("service", "rag-service")
            .register(meterRegistry);

        // 缓存命中率
        Gauge.builder("rag_cache_hits", cacheHits, AtomicLong::get)
            .description("Number of cache hits")
            .tag("service", "rag-service")
            .register(meterRegistry);

        Gauge.builder("rag_cache_misses", cacheMisses, AtomicLong::get)
            .description("Number of cache misses")
            .tag("service", "rag-service")
            .register(meterRegistry);
    }

    /**
     * 增加活跃查询数
     */
    public void incrementActiveQueries() {
        activeQueries.incrementAndGet();
    }

    /**
     * 减少活跃查询数
     */
    public void decrementActiveQueries() {
        activeQueries.decrementAndGet();
    }

    /**
     * 记录缓存命中
     */
    public void recordCacheHit() {
        cacheHits.incrementAndGet();
    }

    /**
     * 记录缓存未命中
     */
    public void recordCacheMiss() {
        cacheMisses.incrementAndGet();
    }

    /**
     * 获取缓存命中率
     */
    public double getCacheHitRate() {
        long hits = cacheHits.get();
        long misses = cacheMisses.get();
        long total = hits + misses;
        return total == 0 ? 0.0 : (double) hits / total;
    }
}
