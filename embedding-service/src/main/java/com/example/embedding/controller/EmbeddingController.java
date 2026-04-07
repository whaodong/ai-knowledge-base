package com.example.embedding.controller;

import com.example.common.dto.Result;
import com.example.embedding.dto.*;
import com.example.embedding.service.EmbeddingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 向量生成服务控制器
 * 
 * <p>提供向量生成相关的REST接口。</p>
 * 
 * @author AI Knowledge Base Team
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/embeddings")
@Tag(name = "向量化API", description = "文本向量化、批量向量化、任务状态查询")
public class EmbeddingController {

    @Autowired
    private EmbeddingService embeddingService;

    @Value("${spring.application.name}")
    private String appName;
    
    @Value("${server.port}")
    private String port;

    /**
     * 文本向量化
     */
    @PostMapping
    @Operation(
        summary = "文本向量化",
        description = "将单个文本转换为向量表示"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "向量化成功"),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "500", description = "服务器错误")
    })
    public ResponseEntity<Result<EmbeddingResponse>> embedText(
            @Valid @RequestBody EmbeddingRequest request) {
        
        log.info("收到文本向量化请求: {}", request.getText().substring(0, Math.min(50, request.getText().length())));
        
        EmbeddingResponse response = embeddingService.embedText(request);
        
        if (response.getStatus() == EmbeddingResponse.TaskStatus.COMPLETED) {
            return ResponseEntity.ok(Result.success("向量化成功", response));
        } else {
            return ResponseEntity.ok(Result.fail(500, "向量化失败"));
        }
    }

    /**
     * 批量文本向量化
     */
    @PostMapping("/batch")
    @Operation(
        summary = "批量文本向量化",
        description = "批量将多个文本转换为向量表示"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "批量向量化完成"),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "500", description = "服务器错误")
    })
    public ResponseEntity<Result<EmbeddingBatchResponse>> batchEmbedTexts(
            @Valid @RequestBody EmbeddingBatchRequest request) {
        
        log.info("收到批量文本向量化请求，数量: {}", request.getTexts().size());
        
        EmbeddingBatchResponse response = embeddingService.batchEmbedTexts(request);
        
        return ResponseEntity.ok(Result.success("批量向量化完成", response));
    }

    /**
     * 查询向量化任务状态
     */
    @GetMapping("/status/{taskId}")
    @Operation(
        summary = "查询任务状态",
        description = "根据任务ID查询向量化任务的状态"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "404", description = "任务不存在"),
        @ApiResponse(responseCode = "500", description = "服务器错误")
    })
    public ResponseEntity<Result<EmbeddingResponse>> getTaskStatus(
            @Parameter(description = "任务ID", required = true)
            @PathVariable String taskId) {
        
        log.info("收到任务状态查询请求: {}", taskId);
        
        EmbeddingResponse response = embeddingService.getTaskStatus(taskId);
        
        return ResponseEntity.ok(Result.success(response));
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
        info.put("description", "向量生成服务，提供文本向量化能力");
        info.put("endpoints", new String[] {
            "POST /api/v1/embeddings - 文本向量化",
            "POST /api/v1/embeddings/batch - 批量向量化",
            "GET /api/v1/embeddings/status/{taskId} - 查询任务状态"
        });
        info.put("supportedModels", new String[] {
            "text-embedding-3-small",
            "text-embedding-3-large",
            "text-embedding-ada-002"
        });
        info.put("timestamp", System.currentTimeMillis());
        return info;
    }
}
