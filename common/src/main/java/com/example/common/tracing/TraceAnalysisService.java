package com.example.common.tracing;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 追踪分析服务
 * 
 * <p>提供追踪数据的分析和统计功能：</p>
 * <ul>
 *   <li>慢请求分析</li>
 *   <li>错误链路追踪</li>
 *   <li>性能瓶颈定位</li>
 *   <li>服务依赖关系分析</li>
 * </ul>
 */
@Slf4j
@Service
public class TraceAnalysisService {

    private final Tracer tracer;
    
    // 统计数据存储
    private final Map<String, SpanStats> spanStatsMap = new ConcurrentHashMap<>();
    private final Map<String, ServiceDependency> dependencyMap = new ConcurrentHashMap<>();
    private final List<SlowTraceRecord> slowTraces = Collections.synchronizedList(new ArrayList<>());
    private final List<ErrorTraceRecord> errorTraces = Collections.synchronizedList(new ArrayList<>());
    
    // 配置
    private static final int MAX_RECORDS = 1000;
    private static final long SLOW_THRESHOLD_MS = 1000;

    public TraceAnalysisService(Tracer tracer) {
        this.tracer = tracer;
    }

    /**
     * 记录Span完成
     */
    public void recordSpanCompletion(Span span, Duration duration, String spanName) {
        if (span == null) return;
        
        long durationMs = duration.toMillis();
        
        // 更新统计信息
        spanStatsMap.compute(spanName, (key, stats) -> {
            if (stats == null) {
                stats = new SpanStats();
                stats.setSpanName(spanName);
            }
            stats.recordCall(durationMs, false);
            return stats;
        });
        
        // 记录慢请求
        if (durationMs > SLOW_THRESHOLD_MS) {
            recordSlowTrace(span, spanName, durationMs);
        }
    }

    /**
     * 记录错误Span
     */
    public void recordSpanError(Span span, String spanName, Duration duration, Throwable error) {
        if (span == null) return;
        
        long durationMs = duration.toMillis();
        
        // 更新统计信息
        spanStatsMap.compute(spanName, (key, stats) -> {
            if (stats == null) {
                stats = new SpanStats();
                stats.setSpanName(spanName);
            }
            stats.recordCall(durationMs, true);
            return stats;
        });
        
        // 记录错误
        recordErrorTrace(span, spanName, durationMs, error);
    }

    /**
     * 记录服务调用关系
     */
    public void recordServiceCall(String fromService, String toService) {
        String key = fromService + "->" + toService;
        dependencyMap.compute(key, (k, dep) -> {
            if (dep == null) {
                dep = new ServiceDependency();
                dep.setFromService(fromService);
                dep.setToService(toService);
            }
            dep.incrementCallCount();
            return dep;
        });
    }

    private void recordSlowTrace(Span span, String spanName, long durationMs) {
        if (slowTraces.size() >= MAX_RECORDS) {
            slowTraces.remove(0);
        }
        
        SlowTraceRecord record = new SlowTraceRecord();
        record.setSpanName(spanName);
        record.setTraceId(span.context().traceId());
        record.setSpanId(span.context().spanId());
        record.setDurationMs(durationMs);
        record.setTimestamp(Instant.now());
        record.setTags(new HashMap<>());
        
        slowTraces.add(record);
    }

    private void recordErrorTrace(Span span, String spanName, long durationMs, Throwable error) {
        if (errorTraces.size() >= MAX_RECORDS) {
            errorTraces.remove(0);
        }
        
        ErrorTraceRecord record = new ErrorTraceRecord();
        record.setSpanName(spanName);
        record.setTraceId(span.context().traceId());
        record.setSpanId(span.context().spanId());
        record.setDurationMs(durationMs);
        record.setTimestamp(Instant.now());
        record.setTags(new HashMap<>());
        record.setErrorMessage(error != null ? error.getMessage() : "Unknown error");
        
        errorTraces.add(record);
    }

    /**
     * 获取慢请求统计
     */
    public List<SlowTraceRecord> getSlowTraces(int limit) {
        return slowTraces.stream()
            .limit(limit)
            .collect(Collectors.toList());
    }

    /**
     * 获取错误追踪记录
     */
    public List<ErrorTraceRecord> getErrorTraces(int limit) {
        return errorTraces.stream()
            .limit(limit)
            .collect(Collectors.toList());
    }

    /**
     * 获取Span统计信息
     */
    public List<SpanStats> getSpanStats() {
        return new ArrayList<>(spanStatsMap.values());
    }

    /**
     * 获取服务依赖关系
     */
    public List<ServiceDependency> getServiceDependencies() {
        return new ArrayList<>(dependencyMap.values());
    }

    /**
     * 获取性能瓶颈
     */
    public List<Map<String, Object>> getPerformanceBottlenecks() {
        return spanStatsMap.values().stream()
            .filter(stats -> stats.getP99LatencyMs() > SLOW_THRESHOLD_MS)
            .sorted((a, b) -> Long.compare(b.getP99LatencyMs(), a.getP99LatencyMs()))
            .limit(10)
            .map(stats -> {
                Map<String, Object> result = new HashMap<>();
                result.put("spanName", stats.getSpanName());
                result.put("callCount", stats.getCallCount());
                result.put("avgLatencyMs", stats.getAvgLatencyMs());
                result.put("p99LatencyMs", stats.getP99LatencyMs());
                result.put("errorRate", stats.getErrorRate());
                return result;
            })
            .collect(Collectors.toList());
    }

    /**
     * 生成追踪报告
     */
    public Map<String, Object> generateReport() {
        Map<String, Object> report = new HashMap<>();
        report.put("generatedAt", Instant.now().toString());
        report.put("totalSpans", spanStatsMap.size());
        report.put("totalSlowTraces", slowTraces.size());
        report.put("totalErrorTraces", errorTraces.size());
        report.put("spanStats", getSpanStats());
        report.put("bottlenecks", getPerformanceBottlenecks());
        report.put("dependencies", getServiceDependencies());
        return report;
    }

    // 数据类
    @Data
    public static class SpanStats {
        private String spanName;
        private final AtomicLong callCount = new AtomicLong(0);
        private final AtomicLong totalLatencyMs = new AtomicLong(0);
        private final AtomicLong errorCount = new AtomicLong(0);
        private final List<Long> latencies = Collections.synchronizedList(new ArrayList<>());

        public void recordCall(long latencyMs, boolean isError) {
            callCount.incrementAndGet();
            totalLatencyMs.addAndGet(latencyMs);
            if (isError) {
                errorCount.incrementAndGet();
            }
            latencies.add(latencyMs);
            if (latencies.size() > 1000) {
                latencies.remove(0);
            }
        }

        public long getCallCount() {
            return callCount.get();
        }

        public double getAvgLatencyMs() {
            long count = callCount.get();
            return count > 0 ? (double) totalLatencyMs.get() / count : 0;
        }

        public long getP99LatencyMs() {
            if (latencies.isEmpty()) return 0;
            List<Long> sorted = latencies.stream().sorted().collect(Collectors.toList());
            int index = (int) (sorted.size() * 0.99);
            return sorted.get(Math.min(index, sorted.size() - 1));
        }

        public double getErrorRate() {
            long count = callCount.get();
            return count > 0 ? (double) errorCount.get() / count : 0;
        }
    }

    @Data
    public static class ServiceDependency {
        private String fromService;
        private String toService;
        private final AtomicLong callCount = new AtomicLong(0);

        public void incrementCallCount() {
            callCount.incrementAndGet();
        }

        public long getCallCount() {
            return callCount.get();
        }
    }

    @Data
    public static class SlowTraceRecord {
        private String spanName;
        private String traceId;
        private String spanId;
        private long durationMs;
        private Instant timestamp;
        private Map<String, String> tags;
    }

    @Data
    public static class ErrorTraceRecord {
        private String spanName;
        private String traceId;
        private String spanId;
        private long durationMs;
        private Instant timestamp;
        private Map<String, String> tags;
        private String errorMessage;
    }
}
