package com.example.rag.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * RAG对话响应
 */
@Data
@Schema(description = "RAG对话响应")
public class ChatResponse {

    @Schema(description = "会话ID")
    private String sessionId;

    @Schema(description = "AI回复")
    private String reply;

    @Schema(description = "引用的文档")
    private List<DocumentReference> references;

    @Schema(description = "是否完成")
    private Boolean finished;

    @Schema(description = "时间戳")
    private LocalDateTime timestamp;

    /**
     * 文档引用
     */
    @Data
    @Schema(description = "文档引用")
    public static class DocumentReference {
        @Schema(description = "文档ID")
        private Long documentId;

        @Schema(description = "文档名称")
        private String documentName;

        @Schema(description = "引用片段")
        private String snippet;

        @Schema(description = "相似度分数")
        private Double score;
    }

    /**
     * 创建成功响应
     */
    public static ChatResponse success(String sessionId, String reply, List<DocumentReference> references) {
        ChatResponse response = new ChatResponse();
        response.setSessionId(sessionId);
        response.setReply(reply);
        response.setReferences(references);
        response.setFinished(true);
        response.setTimestamp(LocalDateTime.now());
        return response;
    }
}
