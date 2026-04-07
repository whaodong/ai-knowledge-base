package com.example.milvus.controller;

import com.example.milvus.service.VectorSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 向量搜索REST API控制器
 * 适配 Spring AI 1.0.0-M3 API
 */
@RestController
@RequestMapping("/api/v1/vectors")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "向量搜索", description = "向量存储与检索API")
public class VectorSearchController {

    private final VectorSearchService vectorSearchService;

    @PostMapping("/documents")
    @Operation(summary = "插入单个文档", description = "将单个文档及其向量插入到向量数据库中")
    public ResponseEntity<InsertResponse> insertDocument(@RequestBody InsertRequest request) {
        try {
            // Spring AI 1.0.0-M3: Document 构造函数为 (id, content, metadata)
            Document document = new Document(
                    request.getId() != null ? request.getId() : java.util.UUID.randomUUID().toString(),
                    request.getContent(),
                    request.getMetadata() != null ? request.getMetadata() : new HashMap<>()
            );
            
            String documentId = vectorSearchService.insertDocument(document);
            
            return ResponseEntity.ok(new InsertResponse(
                    documentId,
                    "文档插入成功",
                    System.currentTimeMillis()
            ));
        } catch (Exception e) {
            log.error("插入文档失败: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new InsertResponse(
                            null,
                            "文档插入失败: " + e.getMessage(),
                            System.currentTimeMillis()
                    ));
        }
    }

    @PostMapping("/documents/batch")
    @Operation(summary = "批量插入文档", description = "批量插入多个文档到向量数据库中")
    public ResponseEntity<BatchInsertResponse> batchInsertDocuments(@RequestBody List<InsertRequest> requests) {
        try {
            List<Document> documents = requests.stream()
                    .map(req -> new Document(
                            req.getId() != null ? req.getId() : java.util.UUID.randomUUID().toString(),
                            req.getContent(),
                            req.getMetadata() != null ? req.getMetadata() : new HashMap<>()
                    ))
                    .toList();
            
            int insertedCount = vectorSearchService.batchInsertDocuments(documents);
            
            return ResponseEntity.ok(new BatchInsertResponse(
                    insertedCount,
                    "批量插入成功",
                    System.currentTimeMillis()
            ));
        } catch (Exception e) {
            log.error("批量插入文档失败: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new BatchInsertResponse(
                            0,
                            "批量插入失败: " + e.getMessage(),
                            System.currentTimeMillis()
                    ));
        }
    }

    @PostMapping("/search")
    @Operation(summary = "相似度搜索", description = "根据查询向量执行相似度搜索")
    public ResponseEntity<SearchResponse> similaritySearch(@RequestBody SearchRequestDto request) {
        try {
            List<Document> documents = vectorSearchService.similaritySearch(
                    request.getQueryVector(),
                    request.getTopK()
            );
            
            // Spring AI 1.0.0-M3: Document 没有 getScore()，从 metadata 获取
            List<SearchResult> results = documents.stream()
                    .map(doc -> {
                        double score = 0.0;
                        Object scoreObj = doc.getMetadata().get("similarity_score");
                        if (scoreObj instanceof Number) {
                            score = ((Number) scoreObj).doubleValue();
                        }
                        return new SearchResult(
                                doc.getId(),
                                doc.getContent(),
                                doc.getMetadata(),
                                score
                        );
                    })
                    .toList();
            
            return ResponseEntity.ok(new SearchResponse(
                    results,
                    "搜索成功",
                    System.currentTimeMillis()
            ));
        } catch (Exception e) {
            log.error("相似度搜索失败: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new SearchResponse(
                            List.of(),
                            "搜索失败: " + e.getMessage(),
                            System.currentTimeMillis()
                    ));
        }
    }

    @PostMapping("/search/filter")
    @Operation(summary = "带过滤条件的相似度搜索", description = "根据查询向量和过滤条件执行相似度搜索")
    public ResponseEntity<SearchResponse> similaritySearchWithFilter(
            @RequestBody FilteredSearchRequest request
    ) {
        try {
            List<Document> documents = vectorSearchService.similaritySearchWithFilter(
                    request.getQueryVector(),
                    request.getTopK(),
                    request.getFilter()
            );
            
            List<SearchResult> results = documents.stream()
                    .map(doc -> {
                        double score = 0.0;
                        Object scoreObj = doc.getMetadata().get("similarity_score");
                        if (scoreObj instanceof Number) {
                            score = ((Number) scoreObj).doubleValue();
                        }
                        return new SearchResult(
                                doc.getId(),
                                doc.getContent(),
                                doc.getMetadata(),
                                score
                        );
                    })
                    .toList();
            
            return ResponseEntity.ok(new SearchResponse(
                    results,
                    "搜索成功",
                    System.currentTimeMillis()
            ));
        } catch (Exception e) {
            log.error("带过滤条件的相似度搜索失败: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new SearchResponse(
                            List.of(),
                            "搜索失败: " + e.getMessage(),
                            System.currentTimeMillis()
                    ));
        }
    }

    @DeleteMapping("/documents/{documentId}")
    @Operation(summary = "删除文档", description = "根据文档ID删除向量数据库中的文档")
    public ResponseEntity<DeleteResponse> deleteDocument(@PathVariable String documentId) {
        try {
            boolean success = vectorSearchService.deleteDocument(documentId);
            
            if (success) {
                return ResponseEntity.ok(new DeleteResponse(
                        documentId,
                        "文档删除成功",
                        System.currentTimeMillis()
                ));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new DeleteResponse(
                                documentId,
                                "文档不存在或删除失败",
                                System.currentTimeMillis()
                        ));
            }
        } catch (Exception e) {
            log.error("删除文档失败: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new DeleteResponse(
                            documentId,
                            "删除失败: " + e.getMessage(),
                            System.currentTimeMillis()
                    ));
        }
    }

    @DeleteMapping("/documents/filter")
    @Operation(summary = "根据过滤条件删除文档", description = "根据metadata过滤条件批量删除文档")
    public ResponseEntity<DeleteByFilterResponse> deleteByFilter(@RequestBody Map<String, Object> filter) {
        try {
            int deletedCount = vectorSearchService.deleteByFilter(filter);
            
            return ResponseEntity.ok(new DeleteByFilterResponse(
                    deletedCount,
                    "删除成功",
                    System.currentTimeMillis()
            ));
        } catch (Exception e) {
            log.error("根据过滤条件删除文档失败: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new DeleteByFilterResponse(
                            0,
                            "删除失败: " + e.getMessage(),
                            System.currentTimeMillis()
                    ));
        }
    }

    @GetMapping("/stats")
    @Operation(summary = "获取统计信息", description = "获取向量数据库的统计信息")
    public ResponseEntity<StatsResponse> getStatistics() {
        try {
            long documentCount = vectorSearchService.count();
            
            return ResponseEntity.ok(new StatsResponse(
                    documentCount,
                    "统计信息获取成功",
                    System.currentTimeMillis()
            ));
        } catch (Exception e) {
            log.error("获取统计信息失败: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new StatsResponse(
                            0,
                            "获取失败: " + e.getMessage(),
                            System.currentTimeMillis()
                    ));
        }
    }

    @GetMapping("/health")
    @Operation(summary = "健康检查", description = "检查向量数据库服务的健康状态")
    public ResponseEntity<HealthResponse> healthCheck() {
        try {
            boolean healthy = vectorSearchService.healthCheck();
            
            if (healthy) {
                return ResponseEntity.ok(new HealthResponse(
                        "healthy",
                        "服务运行正常",
                        System.currentTimeMillis()
                ));
            } else {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(new HealthResponse(
                                "unhealthy",
                                "服务异常",
                                System.currentTimeMillis()
                        ));
            }
        } catch (Exception e) {
            log.error("健康检查失败: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new HealthResponse(
                            "unhealthy",
                            "健康检查异常: " + e.getMessage(),
                            System.currentTimeMillis()
                    ));
        }
    }

    // ========== 请求/响应DTO ==========

    @lombok.Data
    public static class InsertRequest {
        private String id;
        private String content;
        private Map<String, Object> metadata;
        private List<Float> embedding;
    }

    @lombok.Data
    public static class InsertResponse {
        private final String documentId;
        private final String message;
        private final long timestamp;
    }

    @lombok.Data
    public static class BatchInsertResponse {
        private final int insertedCount;
        private final String message;
        private final long timestamp;
    }

    @lombok.Data
    public static class SearchRequestDto {
        private List<Float> queryVector;
        private int topK = 10;
    }

    @lombok.Data
    public static class FilteredSearchRequest {
        private List<Float> queryVector;
        private int topK = 10;
        private Map<String, Object> filter;
    }

    @lombok.Data
    public static class SearchResponse {
        private final List<SearchResult> results;
        private final String message;
        private final long timestamp;
    }

    @lombok.Data
    public static class SearchResult {
        private final String id;
        private final String content;
        private final Map<String, Object> metadata;
        private final double score;
    }

    @lombok.Data
    public static class DeleteResponse {
        private final String documentId;
        private final String message;
        private final long timestamp;
    }

    @lombok.Data
    public static class DeleteByFilterResponse {
        private final int deletedCount;
        private final String message;
        private final long timestamp;
    }

    @lombok.Data
    public static class StatsResponse {
        private final long documentCount;
        private final String message;
        private final long timestamp;
    }

    @lombok.Data
    public static class HealthResponse {
        private final String status;
        private final String message;
        private final long timestamp;
    }
}
