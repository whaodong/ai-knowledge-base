package com.example.embedding.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 批量文本向量化请求
 */
@Data
@Schema(description = "批量文本向量化请求")
public class EmbeddingBatchRequest {

    @Schema(description = "文本列表", required = true)
    @NotEmpty(message = "文本列表不能为空")
    @Valid
    private List<EmbeddingRequest> texts;

    @Schema(description = "向量化模型", example = "text-embedding-3-small")
    private String model = "text-embedding-3-small";

    @Schema(description = "是否异步处理", example = "true")
    private Boolean async = true;
}
