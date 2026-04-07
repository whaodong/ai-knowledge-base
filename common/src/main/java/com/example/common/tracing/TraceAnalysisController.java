package com.example.common.tracing;

import com.example.common.dto.Result;
import io.micrometer.tracing.Tracer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 追踪分析接口
 * 
 * <p>提供追踪数据的查询和分析接口。</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/tracing")
@Tag(name = "分布式追踪", description = "追踪数据查询和分析接口")
public class TraceAnalysisController {

    @Autowired
    private TraceAnalysisService traceAnalysisService;

    @Autowired
    private Tracer tracer;

    @GetMapping("/stats")
    @Operation(summary = "获取追踪统计信息", description = "获取所有Span的统计信息")
    public Result<List<TraceAnalysisService.SpanStats>> getStats() {
        return Result.success(traceAnalysisService.getAllSpanStats());
    }

    @GetMapping("/stats/p99")
    @Operation(summary = "获取P99延迟", description = "获取各Span的P99延迟统计")
    public Result<Map<String, Long>> getP99Latencies() {
        return Result.success(traceAnalysisService.getP99Latencies());
    }

    @GetMapping("/slow-traces")
    @Operation(summary = "获取慢请求列表", description = "获取慢请求追踪记录")
    public Result<List<TraceAnalysisService.SlowTraceRecord>> getSlowTraces(
            @RequestParam(defaultValue = "20") int limit) {
        return Result.success(traceAnalysisService.getSlowTraces(limit));
    }

    @GetMapping("/error-traces")
    @Operation(summary = "获取错误追踪列表", description = "获取错误请求追踪记录")
    public Result<List<TraceAnalysisService.ErrorTraceRecord>> getErrorTraces(
            @RequestParam(defaultValue = "20") int limit) {
        return Result.success(traceAnalysisService.getErrorTraces(limit));
    }

    @GetMapping("/dependencies")
    @Operation(summary = "获取服务依赖关系", description = "获取服务间调用依赖关系")
    public Result<Map<String, TraceAnalysisService.ServiceDependency>> getDependencies() {
        return Result.success(traceAnalysisService.getServiceDependencies());
    }

    @GetMapping("/bottlenecks")
    @Operation(summary = "分析性能瓶颈", description = "分析系统性能瓶颈")
    public Result<List<TraceAnalysisService.BottleneckAnalysis>> analyzeBottlenecks() {
        return Result.success(traceAnalysisService.analyzeBottlenecks());
    }

    @GetMapping("/current-trace")
    @Operation(summary = "获取当前追踪信息", description = "获取当前请求的Trace信息")
    public Result<Map<String, String>> getCurrentTrace() {
        Map<String, String> traceInfo = new HashMap<>();
        
        io.micrometer.tracing.Span span = tracer.currentSpan();
        if (span != null) {
            traceInfo.put("traceId", span.context().traceId());
            traceInfo.put("spanId", span.context().spanId());
            traceInfo.put("spanName", span.getName());
        } else {
            traceInfo.put("message", "No active trace");
        }
        
        return Result.success(traceInfo);
    }

    @DeleteMapping("/stats")
    @Operation(summary = "清空统计数据", description = "清空所有追踪统计数据")
    public Result<String> clearStats() {
        traceAnalysisService.clearStats();
        return Result.success("统计数据已清空");
    }

    @GetMapping("/health")
    @Operation(summary = "追踪服务健康检查", description = "检查追踪服务状态")
    public Result<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("spanCount", traceAnalysisService.getAllSpanStats().size());
        health.put("slowTraceCount", traceAnalysisService.getSlowTraces(1000).size());
        health.put("errorTraceCount", traceAnalysisService.getErrorTraces(1000).size());
        health.put("dependencyCount", traceAnalysisService.getServiceDependencies().size());
        
        return Result.success(health);
    }
}
