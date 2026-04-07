package com.example.embedding.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 文本向量化请求
 */
@Data
@Schema(description = "文本向量化请求")
public class EmbeddingRequest {

    @Schema(description = "待向量化的文本", example = "这是一个测试文本", required = true)
    @NotBlank(message = "文本不能为空")
    private String text;

    @Schema(description = "向量化模型", example = "text-embedding-3-small", defaultValue = "text-embedding-3-small")
    private String model = "text-embedding-3-small";

    @Schema(description = "文档ID（可选）", example = "1")
    private Long documentId;

    @Schema(description = "是否异步处理", example = "false")
    private Boolean async = false;
}
