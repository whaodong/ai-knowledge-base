package com.example.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 批量文档上传响应
 */
@Data
@Schema(description = "批量文档上传响应")
public class DocumentBatchUploadResponse {

    @Schema(description = "总数量")
    private Integer total;

    @Schema(description = "成功数量")
    private Integer successCount;

    @Schema(description = "失败数量")
    private Integer failedCount;

    @Schema(description = "成功的文档ID列表")
    private List<Long> successIds;

    @Schema(description = "失败的文件名列表")
    private List<String> failedFiles;

    @Schema(description = "错误信息列表")
    private List<String> errorMessages;

    public DocumentBatchUploadResponse() {
        this.total = 0;
        this.successCount = 0;
        this.failedCount = 0;
    }
}
