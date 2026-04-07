package com.example.rag.controller;

import com.example.rag.model.RagRequest;
import com.example.rag.model.RagResponse;
import com.example.rag.service.RagRetrievalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * RAG功能演示控制器
 * 提供简单接口展示RAG检索能力
 */
@Slf4j
@RestController
@RequestMapping("/api/demo")
@Tag(name = "RAG演示接口", description = "展示RAG检索功能的演示接口")
public class DemoController {
    
    private final RagRetrievalService ragRetrievalService;
    
    @Autowired
    public DemoController(RagRetrievalService ragRetrievalService) {
        this.ragRetrievalService = ragRetrievalService;
    }
    
    /**
     * 演示标准RAG检索流程
     */
    @GetMapping("/standard-retrieval")
    @Operation(summary = "标准检索演示", description = "演示混合检索+重排序的完整流程")
    public ResponseEntity<Map<String, Object>> demoStandardRetrieval() {
        Map<String, Object> result = new HashMap<>();
        
        RagRequest request = RagRequest.builder()
                .query("Spring AI如何集成向量数据库？")
                .topK(5)
                .hybridSearch(true)
                .rerankEnabled(true)
                .build();
        
        long startTime = System.currentTimeMillis();
        RagResponse response = ragRetrievalService.retrieve(request);
        long endTime = System.currentTimeMillis();
        
        result.put("request", request);
        result.put("responseTimeMs", endTime - startTime);
        result.put("success", response.isSuccess());
        
        if (response.isSuccess()) {
            result.put("retrievedCount", response.getRetrievedDocuments().size());
            result.put("fusedContextLength", response.getFusedContext().length());
            result.put("stats", response.getRetrieverStats());
            
            // 简化的结果预览
            result.put("preview", response.getRetrievedDocuments().stream()
                    .limit(3)
                    .map(doc -> Map.of(
                            "id", doc.getDocumentId().substring(0, 8) + "...",
                            "score", String.format("%.3f", doc.getRerankScore()),
                            "contentPreview", doc.getContent().substring(0, Math.min(100, doc.getContent().length())) + "..."
                    ))
                    .toList());
        } else {
            result.put("error", response.getErrorMessage());
        }
        
        result.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 对比不同检索策略的效果
     */
    @GetMapping("/compare-strategies")
    @Operation(summary = "策略对比演示", description = "对比不同检索策略的效果")
    public ResponseEntity<Map<String, Object>> demoCompareStrategies() {
        Map<String, Object> result = new HashMap<>();
        
        String testQuery = "微服务架构中的服务注册发现机制";
        
        // 测试1: 纯向量检索
        RagRequest vectorOnly = RagRequest.builder()
                .query(testQuery)
                .topK(5)
                .hybridSearch(false)
                .rerankEnabled(false)
                .build();
        
        // 测试2: 纯关键词检索
        RagRequest keywordOnly = RagRequest.builder()
                .query(testQuery)
                .topK(5)
                .hybridSearch(false)
                .rerankEnabled(false)
                .build();
        
        // 测试3: 混合检索（无重排序）
        RagRequest hybridNoRerank = RagRequest.builder()
                .query(testQuery)
                .topK(5)
                .hybridSearch(true)
                .rerankEnabled(false)
                .build();
        
        // 测试4: 混合检索+重排序
        RagRequest hybridWithRerank = RagRequest.builder()
                .query(testQuery)
                .topK(5)
                .hybridSearch(true)
                .rerankEnabled(true)
                .build();
        
        // 执行所有测试
        Map<String, Object> comparisons = new HashMap<>();
        
        comparisons.put("vectorOnly", executeAndSummarize(vectorOnly));
        comparisons.put("keywordOnly", executeAndSummarize(keywordOnly));
        comparisons.put("hybridNoRerank", executeAndSummarize(hybridNoRerank));
        comparisons.put("hybridWithRerank", executeAndSummarize(hybridWithRerank));
        
        result.put("query", testQuery);
        result.put("comparisons", comparisons);
        result.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 演示分块策略效果
     */
    @GetMapping("/chunking-strategies")
    @Operation(summary = "分块策略演示", description = "展示不同分块策略的效果")
    public ResponseEntity<Map<String, Object>> demoChunkingStrategies() {
        Map<String, Object> result = new HashMap<>();
        
        String sampleDocument = "Spring AI是Spring官方推出的AI应用开发框架。它提供了统一的API来集成各种AI模型，包括OpenAI、Azure OpenAI、Anthropic等。Spring AI支持聊天模型、嵌入模型、图像生成模型等多种AI能力。通过Spring Boot的自动配置，开发者可以快速集成AI功能到企业级应用中。框架还提供了向量数据库集成、RAG（检索增强生成）等高级功能。";
        
        result.put("originalDocument", sampleDocument);
        result.put("originalLength", sampleDocument.length());
        result.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 执行检索并返回摘要信息
     */
    private Map<String, Object> executeAndSummarize(RagRequest request) {
        Map<String, Object> summary = new HashMap<>();
        
        long startTime = System.currentTimeMillis();
        RagResponse response = ragRetrievalService.retrieve(request);
        long endTime = System.currentTimeMillis();
        
        summary.put("strategy", request.isHybridSearch() ? 
                (request.isRerankEnabled() ? "混合+重排序" : "混合无重排序") : 
                ("vector".equals(request.getQuery()) ? "向量检索" : "关键词检索"));
        summary.put("responseTimeMs", endTime - startTime);
        summary.put("success", response.isSuccess());
        
        if (response.isSuccess()) {
            summary.put("retrievedCount", response.getRetrievedDocuments().size());
            summary.put("stats", response.getRetrieverStats());
        }
        
        return summary;
    }
}