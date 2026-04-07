package com.example.document.controller;

import com.example.common.dto.PageResponse;
import com.example.common.dto.Result;
import com.example.document.dto.*;
import com.example.document.entity.Document;
import com.example.document.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 文档管理服务控制器
 * 
 * <p>提供文档管理相关的REST接口。</p>
 * 
 * @author AI Knowledge Base Team
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/documents")
@Tag(name = "文档管理API", description = "文档上传、查询、删除等操作")
public class DocumentController {

    @Autowired
    private DocumentService documentService;

    @Value("${spring.application.name}")
    private String appName;
    
    @Value("${server.port}")
    private String port;

    /**
     * 上传文档
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
        summary = "上传文档",
        description = "上传单个文档到知识库"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "上传成功"),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "500", description = "服务器错误")
    })
    public ResponseEntity<Result<DocumentResponse>> uploadDocument(
            @Parameter(description = "文档文件", required = true)
            @RequestParam("file") MultipartFile file,
            
            @Parameter(description = "上传请求参数")
            @Valid @RequestPart(required = false) DocumentUploadRequest request) {
        
        log.info("收到文档上传请求: {}", file.getOriginalFilename());
        
        if (request == null) {
            request = new DocumentUploadRequest();
        }
        
        Document document = documentService.uploadDocument(file, request);
        DocumentResponse response = DocumentResponse.from(document);
        
        return ResponseEntity.ok(Result.success("文档上传成功", response));
    }

    /**
     * 批量上传文档
     */
    @PostMapping(value = "/batch", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
        summary = "批量上传文档",
        description = "批量上传多个文档到知识库"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "上传成功"),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "500", description = "服务器错误")
    })
    public ResponseEntity<Result<DocumentBatchUploadResponse>> batchUploadDocuments(
            @Parameter(description = "文档文件列表", required = true)
            @RequestParam("files") MultipartFile[] files,
            
            @Parameter(description = "批量上传请求参数")
            @Valid @RequestPart(required = false) DocumentBatchUploadRequest request) {
        
        log.info("收到批量文档上传请求，数量: {}", files.length);
        
        if (request == null) {
            request = new DocumentBatchUploadRequest();
        }
        
        DocumentBatchUploadResponse response = documentService.batchUploadDocuments(files, request);
        
        return ResponseEntity.ok(Result.success("批量上传完成", response));
    }

    /**
     * 获取文档列表
     */
    @GetMapping
    @Operation(
        summary = "获取文档列表",
        description = "分页查询文档列表"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "500", description = "服务器错误")
    })
    public ResponseEntity<Result<PageResponse<DocumentResponse>>> getDocumentList(
            @Parameter(description = "查询请求参数")
            @Valid DocumentQueryRequest request) {
        
        log.info("收到文档列表查询请求: {}", request);
        
        if (request == null) {
            request = new DocumentQueryRequest();
        }
        
        PageResponse<Document> pageResponse = documentService.queryDocuments(request);
        
        // 转换为响应DTO
        PageResponse<DocumentResponse> response = PageResponse.of(
            pageResponse.getRecords().stream()
                .map(DocumentResponse::from)
                .collect(Collectors.toList()),
            pageResponse.getTotal(),
            pageResponse.getPageNum(),
            pageResponse.getPageSize()
        );
        
        return ResponseEntity.ok(Result.success(response));
    }

    /**
     * 获取文档详情
     */
    @GetMapping("/{id}")
    @Operation(
        summary = "获取文档详情",
        description = "根据ID获取文档详细信息"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "404", description = "文档不存在"),
        @ApiResponse(responseCode = "500", description = "服务器错误")
    })
    public ResponseEntity<Result<DocumentResponse>> getDocumentDetail(
            @Parameter(description = "文档ID", required = true)
            @PathVariable Long id) {
        
        log.info("收到文档详情查询请求: {}", id);
        
        Document document = documentService.getDocumentById(id);
        DocumentResponse response = DocumentResponse.from(document);
        
        return ResponseEntity.ok(Result.success(response));
    }

    /**
     * 删除文档
     */
    @DeleteMapping("/{id}")
    @Operation(
        summary = "删除文档",
        description = "根据ID删除文档"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "删除成功"),
        @ApiResponse(responseCode = "404", description = "文档不存在"),
        @ApiResponse(responseCode = "500", description = "服务器错误")
    })
    public ResponseEntity<Result<Void>> deleteDocument(
            @Parameter(description = "文档ID", required = true)
            @PathVariable Long id) {
        
        log.info("收到文档删除请求: {}", id);
        
        documentService.deleteDocument(id);
        
        return ResponseEntity.ok(Result.success("文档删除成功"));
    }

    /**
     * 批量删除文档
     */
    @DeleteMapping("/batch")
    @Operation(
        summary = "批量删除文档",
        description = "批量删除多个文档"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "删除成功"),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "500", description = "服务器错误")
    })
    public ResponseEntity<Result<Void>> batchDeleteDocuments(
            @Parameter(description = "文档ID列表", required = true)
            @RequestBody Long[] ids) {
        
        log.info("收到批量删除文档请求，数量: {}", ids.length);
        
        documentService.batchDeleteDocuments(ids);
        
        return ResponseEntity.ok(Result.success("批量删除成功"));
    }

    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    @Operation(summary = "健康检查", description = "服务健康状态检查")
    public Map<String, Object> health() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "UP");
        result.put("service", appName);
        result.put("port", port);
        result.put("timestamp", System.currentTimeMillis());
        return result;
    }
    
    /**
     * 服务信息接口
     */
    @GetMapping("/info")
    @Operation(summary = "服务信息", description = "获取服务详细信息")
    public Map<String, Object> getServiceInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("serviceName", appName);
        info.put("instanceId", appName + ":" + port);
        info.put("status", "ACTIVE");
        info.put("description", "文档管理服务，提供文档上传、解析、存储能力");
        info.put("endpoints", new String[] {
            "POST /api/v1/documents - 上传文档",
            "POST /api/v1/documents/batch - 批量上传",
            "GET /api/v1/documents - 文档列表",
            "GET /api/v1/documents/{id} - 文档详情",
            "DELETE /api/v1/documents/{id} - 删除文档",
            "DELETE /api/v1/documents/batch - 批量删除"
        });
        info.put("timestamp", System.currentTimeMillis());
        return info;
    }
}
