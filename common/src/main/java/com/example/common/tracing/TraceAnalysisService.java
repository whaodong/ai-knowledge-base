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
    public void recordSpanCompletion(Span span, Duration duration) {
        if (span == null) return;
        
        String spanName = span.getName();
        long durationMs = duration.toMillis();
        
        // 更新统计信息
        spanStatsMap.compute(spanName, (key, stats) -> {
            if (stats == null) {
                stats = new SpanStats();
                stats.setSpanName(spanName);
            }
            stats.recordCall(durationMs, span.getError() != null);
            return stats;
        });
        
        // 记录慢请求
        if (durationMs > SLOW_THRESHOLD_MS) {
            recordSlowTrace(span, durationMs);
        }
        
        // 记录错误
        if (span.getError() != null) {
            recordErrorTrace(span, durationMs);
        }
    }

    /**
     * 记录服务调用关系
     */
    public void recordServiceDependency(String fromService, String toService) {
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

    /**
     * 记录慢请求
     */
    private void recordSlowTrace(Span span, long durationMs) {
        if (slowTraces.size() >= MAX_RECORDS) {
            slowTraces.remove(0);
        }
        
        SlowTraceRecord record = new SlowTraceRecord();
        record.setTraceId(span.context().traceId());
        record.setSpanId(span.context().spanId());
        record.setSpanName(span.getName());
        record.setDurationMs(durationMs);
        record.setTimestamp(Instant.now());
        record.setTags(span.getAllTags());
        
        slowTraces.add(record);
        log.warn("检测到慢请求: span={}, duration={}ms, traceId={}", 
                span.getName(), durationMs, span.context().traceId());
    }

    /**
     * 记录错误追踪
     */
    private void recordErrorTrace(Span span, long durationMs) {
        if (errorTraces.size() >= MAX_RECORDS) {
            errorTraces.remove(0);
        }
        
        ErrorTraceRecord record = new ErrorTraceRecord();
        record.setTraceId(span.context().traceId());
        record.setSpanId(span.context().spanId());
        record.setSpanName(span.getName());
        record.setDurationMs(durationMs);
        record.setTimestamp(Instant.now());
        record.setErrorType(span.getError().getClass().getSimpleName());
        record.setErrorMessage(span.getError().getMessage());
        record.setTags(span.getAllTags());
        
        errorTraces.add(record);
        log.error("检测到错误请求: span={}, error={}, traceId={}", 
                span.getName(), record.getErrorType(), span.context().traceId());
    }

    /**
     * 获取所有Span的统计信息
     */
    public List<SpanStats> getAllSpanStats() {
        return new ArrayList<>(spanStatsMap.values());
    }

    /**
     * 获取P99延迟统计
     */
    public Map<String, Long> getP99Latencies() {
        return spanStatsMap.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().getP99Latency()
                ));
    }

    /**
     * 获取慢请求列表
     */
    public List<SlowTraceRecord> getSlowTraces(int limit) {
        return slowTraces.stream()
                .sorted((a, b) -> Long.compare(b.getDurationMs(), a.getDurationMs()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * 获取错误追踪列表
     */
    public List<ErrorTraceRecord> getErrorTraces(int limit) {
        return errorTraces.stream()
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * 获取服务依赖图
     */
    public Map<String, ServiceDependency> getServiceDependencies() {
        return new HashMap<>(dependencyMap);
    }

    /**
     * 获取性能瓶颈分析
     */
    public List<BottleneckAnalysis> analyzeBottlenecks() {
        return spanStatsMap.values().stream()
                .filter(stats -> stats.getP99Latency() > SLOW_THRESHOLD_MS)
                .map(stats -> {
                    BottleneckAnalysis analysis = new BottleneckAnalysis();
                    analysis.setSpanName(stats.getSpanName());
                    analysis.setAvgDurationMs(stats.getAvgDuration());
                    analysis.setP99DurationMs(stats.getP99Latency());
                    analysis.setCallCount(stats.getTotalCalls());
                    analysis.setErrorRate(stats.getErrorRate());
                    return analysis;
                })
                .sorted((a, b) -> Long.compare(b.getP99DurationMs(), a.getP99DurationMs()))
                .collect(Collectors.toList());
    }

    /**
     * 清空统计数据
     */
    public void clearStats() {
        spanStatsMap.clear();
        dependencyMap.clear();
        slowTraces.clear();
        errorTraces.clear();
        log.info("追踪统计数据已清空");
    }

    // ============ 内部数据类 ============

    @Data
    public static class SpanStats {
        private String spanName;
        private final AtomicLong totalCalls = new AtomicLong(0);
        private final AtomicLong errorCalls = new AtomicLong(0);
        private final AtomicLong totalDuration = new AtomicLong(0);
        private final List<Long> durations = Collections.synchronizedList(new ArrayList<>());
        
        public void recordCall(long durationMs, boolean isError) {
            totalCalls.incrementAndGet();
            totalDuration.addAndGet(durationMs);
            if (isError) errorCalls.incrementAndGet();
            
            // 保留最近1000个样本用于计算P99
            synchronized (durations) {
                if (durations.size() >= 1000) {
                    durations.remove(0);
                }
                durations.add(durationMs);
            }
        }
        
        public long getAvgDuration() {
            long calls = totalCalls.get();
            return calls > 0 ? totalDuration.get() / calls : 0;
        }
        
        public long getP99Latency() {
            synchronized (durations) {
                if (durations.isEmpty()) return 0;
                
                List<Long> sorted = durations.stream()
                        .sorted()
                        .collect(Collectors.toList());
                
                int p99Index = (int) (sorted.size() * 0.99);
                return sorted.get(p99Index);
            }
        }
        
        public double getErrorRate() {
            long calls = totalCalls.get();
            return calls > 0 ? (double) errorCalls.get() / calls : 0;
        }
    }

    @Data
    public static class SlowTraceRecord {
        private String traceId;
        private String spanId;
        private String spanName;
        private long durationMs;
        private Instant timestamp;
        private Map<String, String> tags;
    }

    @Data
    public static class ErrorTraceRecord {
        private String traceId;
        private String spanId;
        private String spanName;
        private long durationMs;
        private Instant timestamp;
        private String errorType;
        private String errorMessage;
        private Map<String, String> tags;
    }

    @Data
    public static class ServiceDependency {
        private String fromService;
        private String toService;
        private final AtomicLong callCount = new AtomicLong(0);
        
        public void incrementCallCount() {
            callCount.incrementAndGet();
        }
    }

    @Data
    public static class BottleneckAnalysis {
        private String spanName;
        private long avgDurationMs;
        private long p99DurationMs;
        private long callCount;
        private double errorRate;
    }
}
