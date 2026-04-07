package com.example.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 批量文档上传请求
 */
@Data
@Schema(description = "批量文档上传请求")
public class DocumentBatchUploadRequest {

    @Schema(description = "文档列表")
    @NotEmpty(message = "文档列表不能为空")
    @Valid
    private List<DocumentUploadRequest> documents;

    @Schema(description = "是否立即处理", example = "true")
    private Boolean processImmediately = true;

    @Schema(description = "是否立即向量化", example = "false")
    private Boolean embedImmediately = false;
}
