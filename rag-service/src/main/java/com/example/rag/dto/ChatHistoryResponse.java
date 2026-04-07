package com.example.rag.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 会话历史响应
 */
@Data
@Schema(description = "会话历史响应")
public class ChatHistoryResponse {

    @Schema(description = "会话ID")
    private String sessionId;

    @Schema(description = "消息列表")
    private List<Message> messages;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    /**
     * 消息
     */
    @Data
    @Schema(description = "消息")
    public static class Message {
        @Schema(description = "角色", example = "user")
        private String role;

        @Schema(description = "内容")
        private String content;

        @Schema(description = "时间戳")
        private LocalDateTime timestamp;
    }
}
