package com.example.embedding.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 文本向量化响应
 */
@Data
@Schema(description = "文本向量化响应")
public class EmbeddingResponse {

    @Schema(description = "任务ID")
    private String taskId;

    @Schema(description = "原始文本")
    private String text;

    @Schema(description = "向量数据")
    private List<Float> embedding;

    @Schema(description = "向量维度", example = "1536")
    private Integer dimension;

    @Schema(description = "使用的模型")
    private String model;

    @Schema(description = "任务状态")
    private TaskStatus status;

    @Schema(description = "错误信息")
    private String errorMessage;

    @Schema(description = "处理耗时（毫秒）")
    private Long duration;

    /**
     * 任务状态枚举
     */
    public enum TaskStatus {
        PENDING,     // 待处理
        PROCESSING,  // 处理中
        COMPLETED,   // 已完成
        FAILED       // 失败
    }

    /**
     * 创建成功响应
     */
    public static EmbeddingResponse success(String taskId, String text, List<Float> embedding, String model, Long duration) {
        EmbeddingResponse response = new EmbeddingResponse();
        response.setTaskId(taskId);
        response.setText(text);
        response.setEmbedding(embedding);
        response.setDimension(embedding.size());
        response.setModel(model);
        response.setStatus(TaskStatus.COMPLETED);
        response.setDuration(duration);
        return response;
    }

    /**
     * 创建处理中响应
     */
    public static EmbeddingResponse processing(String taskId) {
        EmbeddingResponse response = new EmbeddingResponse();
        response.setTaskId(taskId);
        response.setStatus(TaskStatus.PROCESSING);
        return response;
    }

    /**
     * 创建失败响应
     */
    public static EmbeddingResponse failed(String taskId, String errorMessage) {
        EmbeddingResponse response = new EmbeddingResponse();
        response.setTaskId(taskId);
        response.setStatus(TaskStatus.FAILED);
        response.setErrorMessage(errorMessage);
        return response;
    }
}
