package com.example.rag.controller;

import com.example.rag.model.RagRequest;
import com.example.rag.model.RagResponse;
import com.example.rag.service.RagRetrievalService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.Map;

/**
 * RAG检索控制器
 * 
 * <p>提供RAG检索的REST API接口，支持混合搜索、重排序等功能。</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/rag")
@Tag(name = "RAG检索服务", description = "检索增强生成服务的核心检索功能")
public class RagController {
    
    private final RagRetrievalService ragRetrievalService;
    
    @Autowired
    public RagController(RagRetrievalService ragRetrievalService) {
        this.ragRetrievalService = ragRetrievalService;
    }
    
    /**
     * 执行RAG检索
     */
    @PostMapping("/retrieve")
    @Operation(
        summary = "执行RAG检索",
        description = "根据用户查询进行多路检索、重排序和上下文融合",
        responses = {
            @ApiResponse(responseCode = "200", description = "检索成功"),
            @ApiResponse(responseCode = "400", description = "请求参数错误"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
        }
    )
    @CircuitBreaker(name = "ragRetrieval", fallbackMethod = "retrieveFallback")
    @RateLimiter(name = "ragRetrieval", fallbackMethod = "rateLimitFallback")
    public ResponseEntity<RagResponse> retrieve(
            @Valid @RequestBody RagRequest request) {
        
        log.info("收到RAG检索请求: {}", request.getQuery());
        
        RagResponse response = ragRetrievalService.retrieve(request);
        
        if (response.isSuccess()) {
            log.info("RAG检索成功，查询: {}, 返回结果数: {}", 
                    request.getQuery(), response.getRetrievedDocuments().size());
            return ResponseEntity.ok(response);
        } else {
            log.error("RAG检索失败，查询: {}, 错误: {}", 
                    request.getQuery(), response.getErrorMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 简化版检索接口（GET方式）
     */
    @GetMapping("/search")
    @Operation(
        summary = "简化版检索",
        description = "使用GET参数进行RAG检索，适合简单查询场景"
    )
    public ResponseEntity<RagResponse> search(
            @Parameter(description = "查询文本", required = true)
            @RequestParam String query,
            
            @Parameter(description = "返回结果数量，默认10")
            @RequestParam(defaultValue = "10") int topK,
            
            @Parameter(description = "是否启用混合检索，默认true")
            @RequestParam(defaultValue = "true") boolean hybrid,
            
            @Parameter(description = "是否启用重排序，默认true")
            @RequestParam(defaultValue = "true") boolean rerank) {
        
        RagRequest request = RagRequest.builder()
                .query(query)
                .topK(topK)
                .hybridSearch(hybrid)
                .rerankEnabled(rerank)
                .build();
        
        return retrieve(request);
    }
    
    /**
     * 检索服务健康检查
     */
    @GetMapping("/health")
    @Operation(summary = "服务健康检查", description = "检查RAG检索服务的健康状态")
    public ResponseEntity<Map<String, Object>> health() {
        log.debug("RAG服务健康检查");
        
        Map<String, Object> healthStatus = Map.of(
                "status", "UP",
                "service", "rag-retrieval",
                "timestamp", System.currentTimeMillis(),
                "components", Map.of(
                        "vectorRetriever", "active",
                        "keywordRetriever", "active",
                        "reranker", "active",
                        "contextManager", "active"
                )
        );
        
        return ResponseEntity.ok(healthStatus);
    }
    
    /**
     * 熔断降级方法
     */
    public ResponseEntity<RagResponse> retrieveFallback(
            RagRequest request, Throwable throwable) {
        
        log.error("RAG检索熔断触发，查询: {}, 错误: {}", 
                request.getQuery(), throwable.getMessage());
        
        RagResponse response = RagResponse.builder()
                .success(false)
                .errorMessage("服务暂时不可用，请稍后重试。错误: " + throwable.getMessage())
                .retrievalTimeMs(0)
                .build();
        
        return ResponseEntity.status(503).body(response);
    }
    
    /**
     * 限流降级方法
     */
    public ResponseEntity<RagResponse> rateLimitFallback(
            RagRequest request, Throwable throwable) {
        
        log.warn("RAG检索限流触发，查询: {}", request.getQuery());
        
        RagResponse response = RagResponse.builder()
                .success(false)
                .errorMessage("请求频率过高，请稍后重试")
                .retrievalTimeMs(0)
                .build();
        
        return ResponseEntity.status(429).body(response);
    }
    
    /**
     * 性能测试接口（仅测试环境）
     */
    @PostMapping("/benchmark")
    @Operation(
        summary = "性能基准测试",
        description = "执行RAG检索性能测试，需要管理员权限"
    )
    public ResponseEntity<Map<String, Object>> benchmark(
            @RequestParam(defaultValue = "10") int iterations,
            @RequestParam(defaultValue = "Spring AI是什么？") String testQuery) {
        
        log.info("开始性能基准测试，迭代次数: {}, 测试查询: {}", iterations, testQuery);
        
        RagRequest request = RagRequest.builder()
                .query(testQuery)
                .topK(10)
                .hybridSearch(true)
                .rerankEnabled(true)
                .build();
        
        long totalTime = 0;
        long minTime = Long.MAX_VALUE;
        long maxTime = 0;
        int successCount = 0;
        
        for (int i = 0; i < iterations; i++) {
            long startTime = System.currentTimeMillis();
            RagResponse response = ragRetrievalService.retrieve(request);
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            
            totalTime += duration;
            minTime = Math.min(minTime, duration);
            maxTime = Math.max(maxTime, duration);
            
            if (response.isSuccess()) {
                successCount++;
            }
            
            // 短暂延迟，避免资源争用
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        double avgTime = (double) totalTime / iterations;
        double successRate = (double) successCount / iterations * 100;
        
        Map<String, Object> benchmarkResults = Map.of(
                "iterations", iterations,
                "totalTimeMs", totalTime,
                "averageTimeMs", avgTime,
                "minTimeMs", minTime,
                "maxTimeMs", maxTime,
                "successRate", successRate,
                "successCount", successCount,
                "testQuery", testQuery,
                "timestamp", System.currentTimeMillis()
        );
        
        log.info("性能基准测试完成，平均耗时: {}ms, 成功率: {}%", avgTime, successRate);
        
        return ResponseEntity.ok(benchmarkResults);
    }
}