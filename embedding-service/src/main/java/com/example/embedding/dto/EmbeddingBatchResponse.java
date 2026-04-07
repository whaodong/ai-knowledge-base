package com.example.embedding.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 批量文本向量化响应
 */
@Data
@Schema(description = "批量文本向量化响应")
public class EmbeddingBatchResponse {

    @Schema(description = "批量任务ID")
    private String batchTaskId;

    @Schema(description = "总数量")
    private Integer total;

    @Schema(description = "成功数量")
    private Integer successCount;

    @Schema(description = "失败数量")
    private Integer failedCount;

    @Schema(description = "结果列表")
    private List<EmbeddingResponse> results;

    public EmbeddingBatchResponse() {
        this.total = 0;
        this.successCount = 0;
        this.failedCount = 0;
    }
}
