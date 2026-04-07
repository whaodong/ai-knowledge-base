package com.example.rag.controller;

import com.example.common.dto.Result;
import com.example.rag.dto.ChatHistoryResponse;
import com.example.rag.dto.ChatRequest;
import com.example.rag.dto.ChatResponse;
import com.example.rag.model.RagRequest;
import com.example.rag.model.RagResponse;
import com.example.rag.service.RagRetrievalService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
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
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.validation.Valid;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RAG检索控制器
 * 
 * <p>提供RAG检索的REST API接口，支持混合搜索、重排序、流式对话等功能。</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/rag")
@Tag(name = "RAG检索服务", description = "检索增强生成服务的核心检索功能")
public class RagController {
    
    private final RagRetrievalService ragRetrievalService;

    // 模拟会话存储（实际应使用Redis或数据库）
    private final Map<String, ChatHistoryResponse> sessionStore = new ConcurrentHashMap<>();
    
    @Autowired
    public RagController(RagRetrievalService ragRetrievalService) {
        this.ragRetrievalService = ragRetrievalService;
    }

    @Value("${spring.application.name}")
    private String appName;
    
    @Value("${server.port}")
    private String port;
    
    /**
     * RAG查询
     */
    @PostMapping("/query")
    @Operation(
        summary = "RAG查询",
        description = "执行RAG检索，根据查询返回相关文档片段"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @CircuitBreaker(name = "ragRetrieval", fallbackMethod = "queryFallback")
    @RateLimiter(name = "ragRetrieval")
    public ResponseEntity<Result<RagResponse>> query(
            @Valid @RequestBody RagRequest request) {
        
        log.info("收到RAG查询请求: {}", request.getQuery());
        
        RagResponse response = ragRetrievalService.retrieve(request);
        
        if (response.isSuccess()) {
            log.info("RAG查询成功，查询: {}, 返回结果数: {}", 
                    request.getQuery(), response.getRetrievedDocuments().size());
            return ResponseEntity.ok(Result.success("查询成功", response));
        } else {
            log.error("RAG查询失败，查询: {}, 错误: {}", 
                    request.getQuery(), response.getErrorMessage());
            return ResponseEntity.ok(Result.fail(500, response.getErrorMessage()));
        }
    }

    /**
     * 流式对话
     */
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(
        summary = "流式对话",
        description = "基于RAG的流式对话，实时返回AI回复"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "对话成功"),
        @ApiResponse(responseCode = "400", description = "请求参数错误"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public SseEmitter chat(@Valid @RequestBody ChatRequest request) {
        log.info("收到RAG对话请求: {}", request.getMessage());

        // 创建SSE发射器，设置超时时间为60秒
        SseEmitter emitter = new SseEmitter(60000L);

        // 异步处理
        new Thread(() -> {
            try {
                // 如果没有sessionId，创建新的
                String sessionId = request.getSessionId();
                if (sessionId == null || sessionId.isEmpty()) {
                    sessionId = UUID.randomUUID().toString();
                }

                // 模拟流式输出
                String reply = "基于您的查询，我找到了以下相关信息：\n\n" +
                        "1. 文档A中提到...\n" +
                        "2. 文档B中说明...\n" +
                        "3. 文档C中指出...\n\n" +
                        "总结：这是一个模拟的RAG回复。";

                // 分段发送
                String[] words = reply.split("");
                StringBuilder sb = new StringBuilder();
                for (String word : words) {
                    sb.append(word);
                    ChatResponse chunk = new ChatResponse();
                    chunk.setSessionId(sessionId);
                    chunk.setReply(sb.toString());
                    chunk.setFinished(false);
                    chunk.setTimestamp(LocalDateTime.now());

                    emitter.send(SseEmitter.event()
                            .name("message")
                            .data(chunk));

                    Thread.sleep(50); // 模拟延迟
                }

                // 发送完成事件
                ChatResponse finalResponse = new ChatResponse();
                finalResponse.setSessionId(sessionId);
                finalResponse.setReply(reply);
                finalResponse.setFinished(true);
                finalResponse.setTimestamp(LocalDateTime.now());

                emitter.send(SseEmitter.event()
                        .name("done")
                        .data(finalResponse));
                emitter.complete();

                // 保存会话历史
                saveMessage(sessionId, "user", request.getMessage());
                saveMessage(sessionId, "assistant", reply);

            } catch (IOException | InterruptedException e) {
                log.error("流式对话失败", e);
                emitter.completeWithError(e);
            }
        }).start();

        return emitter;
    }

    /**
     * 获取会话历史
     */
    @GetMapping("/history/{sessionId}")
    @Operation(
        summary = "获取会话历史",
        description = "根据会话ID获取完整的对话历史"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "404", description = "会话不存在"),
        @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    public ResponseEntity<Result<ChatHistoryResponse>> getHistory(
            @Parameter(description = "会话ID", required = true)
            @PathVariable String sessionId) {
        
        log.info("收到会话历史查询请求: {}", sessionId);
        
        ChatHistoryResponse history = sessionStore.get(sessionId);
        if (history == null) {
            return ResponseEntity.ok(Result.fail(404, "会话不存在"));
        }
        
        return ResponseEntity.ok(Result.success(history));
    }

    /**
     * 简化版检索接口（GET方式）
     */
    @GetMapping("/search")
    @Operation(
        summary = "简化版检索",
        description = "使用GET参数进行RAG检索，适合简单查询场景"
    )
    public ResponseEntity<Result<RagResponse>> search(
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
        
        return query(request);
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
        info.put("description", "RAG检索服务，提供检索增强生成能力");
        info.put("endpoints", new String[] {
            "POST /api/v1/rag/query - RAG查询",
            "POST /api/v1/rag/chat - 流式对话",
            "GET /api/v1/rag/history/{sessionId} - 会话历史",
            "GET /api/v1/rag/search - 简化版检索"
        });
        info.put("timestamp", System.currentTimeMillis());
        return info;
    }

    /**
     * 查询降级方法
     */
    public ResponseEntity<Result<RagResponse>> queryFallback(RagRequest request, Throwable t) {
        log.error("RAG查询降级，查询: {}", request.getQuery(), t);
        
        RagResponse response = new RagResponse();
        response.setSuccess(false);
        response.setErrorMessage("服务暂时不可用，请稍后重试");
        
        return ResponseEntity.ok(Result.fail(503, "服务暂时不可用"));
    }

    /**
     * 保存消息到会话历史
     */
    private void saveMessage(String sessionId, String role, String content) {
        ChatHistoryResponse history = sessionStore.computeIfAbsent(sessionId, id -> {
            ChatHistoryResponse h = new ChatHistoryResponse();
            h.setSessionId(id);
            h.setMessages(new ArrayList<>());
            h.setCreateTime(LocalDateTime.now());
            return h;
        });

        ChatHistoryResponse.Message message = new ChatHistoryResponse.Message();
        message.setRole(role);
        message.setContent(content);
        message.setTimestamp(LocalDateTime.now());

        history.getMessages().add(message);
        history.setUpdateTime(LocalDateTime.now());
    }
}
