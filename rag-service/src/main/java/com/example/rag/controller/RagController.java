package com.example.rag.controller;

import com.example.common.dto.Result;
import com.example.rag.dto.ChatHistoryResponse;
import com.example.rag.dto.ChatRequest;
import com.example.rag.dto.ChatResponse;
import com.example.rag.model.RagRequest;
import com.example.rag.model.RagResponse;
import com.example.rag.model.RetrievalResult;
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
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

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
    private final ChatModel chatModel;
    
    // 会话存储（实际生产环境应使用Redis）
    private final Map<String, ChatHistoryResponse> sessionStore = new ConcurrentHashMap<>();
    
    // RAG 系统提示词
    private static final String RAG_SYSTEM_PROMPT = """
        你是一个智能助手，基于提供的上下文信息回答用户问题。
        
        请遵循以下原则：
        1. 仅基于提供的上下文信息回答问题
        2. 如果上下文中没有相关信息，请诚实告知用户
        3. 回答要准确、简洁、有帮助
        4. 引用具体的文档来源增强可信度
        
        上下文信息：
        {context}
        """;
    
    @Autowired
    public RagController(RagRetrievalService ragRetrievalService, ChatModel chatModel) {
        this.ragRetrievalService = ragRetrievalService;
        this.chatModel = chatModel;
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
     * 流式对话（真实RAG）
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

        // 创建SSE发射器，设置超时时间为120秒
        SseEmitter emitter = new SseEmitter(120000L);

        // 异步处理
        new Thread(() -> {
            try {
                // 如果没有sessionId，创建新的
                String sessionId = request.getSessionId();
                if (sessionId == null || sessionId.isEmpty()) {
                    sessionId = UUID.randomUUID().toString();
                }

                // 1. RAG 检索获取相关文档
                RagRequest ragRequest = RagRequest.builder()
                        .query(request.getMessage())
                        .topK(5)
                        .build();
                
                RagResponse ragResponse = ragRetrievalService.retrieve(ragRequest);
                
                // 2. 构建上下文
                String context = buildContext(ragResponse);
                
                // 3. 使用 ChatModel 进行流式对话
                String systemPrompt = RAG_SYSTEM_PROMPT.replace("{context}", context);
                
                StringBuilder fullReply = new StringBuilder();
                
                ChatClient chatClient = ChatClient.builder(chatModel)
                        .defaultSystem(systemPrompt)
                        .build();
                
                Flux<String> responseFlux = chatClient.prompt()
                        .user(request.getMessage())
                        .call()
                        .content();
                
                // 4. 流式输出
                responseFlux.subscribe(
                    chunk -> {
                        try {
                            fullReply.append(chunk);
                            
                            ChatResponse chatChunk = new ChatResponse();
                            chatChunk.setSessionId(sessionId);
                            chatChunk.setReply(fullReply.toString());
                            chatChunk.setFinished(false);
                            chatChunk.setTimestamp(LocalDateTime.now());

                            emitter.send(SseEmitter.event()
                                    .name("message")
                                    .data(chatChunk));
                        } catch (IOException e) {
                            log.error("SSE发送失败", e);
                        }
                    },
                    error -> {
                        log.error("流式对话失败", error);
                        emitter.completeWithError(error);
                    },
                    () -> {
                        try {
                            // 发送完成事件
                            ChatResponse finalResponse = new ChatResponse();
                            finalResponse.setSessionId(sessionId);
                            finalResponse.setReply(fullReply.toString());
                            finalResponse.setFinished(true);
                            finalResponse.setTimestamp(LocalDateTime.now());

                            emitter.send(SseEmitter.event()
                                    .name("done")
                                    .data(finalResponse));
                            emitter.complete();

                            // 保存会话历史
                            saveMessage(sessionId, "user", request.getMessage());
                            saveMessage(sessionId, "assistant", fullReply.toString());
                            
                            log.info("RAG对话完成，sessionId: {}, 回复长度: {}", 
                                    sessionId, fullReply.length());
                        } catch (IOException e) {
                            log.error("发送完成事件失败", e);
                        }
                    }
                );

            } catch (Exception e) {
                log.error("流式对话异常", e);
                emitter.completeWithError(e);
            }
        }).start();

        return emitter;
    }
    
    /**
     * 构建RAG上下文
     */
    private String buildContext(RagResponse ragResponse) {
        if (ragResponse == null || !ragResponse.isSuccess() || 
            ragResponse.getRetrievedDocuments() == null || 
            ragResponse.getRetrievedDocuments().isEmpty()) {
            return "未找到相关上下文信息。";
        }
        
        StringBuilder context = new StringBuilder();
        List<RetrievalResult> docs = ragResponse.getRetrievedDocuments();
        
        for (int i = 0; i < docs.size(); i++) {
            RetrievalResult doc = docs.get(i);
            context.append(String.format("[文档%d] (来源: %s, 相关度: %.2f)\n%s\n\n",
                    i + 1,
                    doc.getDocumentId() != null ? doc.getDocumentId() : "未知",
                    doc.getScore() != null ? doc.getScore() : 0.0,
                    doc.getContent() != null ? doc.getContent() : ""));
        }
        
        return context.toString();
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
        info.put("timestamp", System.currentTimeMillis());
        info.put("features", Arrays.asList(
            "hybrid-search", 
            "cross-encoder-rerank", 
            "streaming-chat",
            "token-management"
        ));
        return info;
    }
    
    /**
     * 查询降级方法
     */
    public ResponseEntity<Result<RagResponse>> queryFallback(
            RagRequest request, Throwable t) {
        log.warn("RAG查询降级: {}", t.getMessage());
        
        RagResponse response = RagResponse.builder()
                .success(false)
                .errorMessage("服务暂时不可用，请稍后重试")
                .query(request.getQuery())
                .build();
        
        return ResponseEntity.ok(Result.fail(503, "服务降级", response));
    }
    
    /**
     * 保存消息到会话历史
     */
    private void saveMessage(String sessionId, String role, String content) {
        ChatHistoryResponse history = sessionStore.computeIfAbsent(
            sessionId, 
            k -> {
                ChatHistoryResponse h = new ChatHistoryResponse();
                h.setSessionId(sessionId);
                h.setMessages(new ArrayList<>());
                h.setCreatedAt(LocalDateTime.now());
                return h;
            }
        );
        
        ChatHistoryResponse.Message msg = new ChatHistoryResponse.Message();
        msg.setRole(role);
        msg.setContent(content);
        msg.setTimestamp(LocalDateTime.now());
        
        history.getMessages().add(msg);
        history.setUpdatedAt(LocalDateTime.now());
    }
}
