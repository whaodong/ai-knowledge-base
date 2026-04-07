package com.example.rag.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * RAG对话请求
 */
@Data
@Schema(description = "RAG对话请求")
public class ChatRequest {

    @Schema(description = "用户消息", example = "什么是人工智能？", required = true)
    @NotBlank(message = "消息不能为空")
    private String message;

    @Schema(description = "会话ID", example = "session-123")
    private String sessionId;

    @Schema(description = "是否启用流式输出", example = "true")
    private Boolean stream = true;

    @Schema(description = "是否启用RAG检索", example = "true")
    private Boolean enableRag = true;

    @Schema(description = "返回结果数量", example = "5")
    private Integer topK = 5;

    @Schema(description = "温度参数", example = "0.7")
    private Double temperature = 0.7;
}
