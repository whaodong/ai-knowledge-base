package com.example.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

/**
 * 文档上传请求
 */
@Data
@Schema(description = "文档上传请求")
public class DocumentUploadRequest {

    @Schema(description = "文件名", example = "user-guide.pdf")
    @NotBlank(message = "文件名不能为空")
    private String fileName;

    @Schema(description = "文件类型", example = "pdf")
    @NotBlank(message = "文件类型不能为空")
    private String fileType;

    @Schema(description = "文档元数据")
    private Map<String, Object> metadata;

    @Schema(description = "是否立即处理", example = "true")
    private Boolean processImmediately = true;

    @Schema(description = "是否立即向量化", example = "false")
    private Boolean embedImmediately = false;
}
