package com.example.document.dto;

import com.example.common.dto.PageRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文档查询请求
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "文档查询请求")
public class DocumentQueryRequest extends PageRequest {

    @Schema(description = "文件名关键词", example = "user")
    private String fileName;

    @Schema(description = "文件类型", example = "pdf")
    private String fileType;

    @Schema(description = "文档状态", example = "UPLOADED")
    private String status;

    @Schema(description = "开始时间", example = "2024-01-01T00:00:00")
    private String startTime;

    @Schema(description = "结束时间", example = "2024-12-31T23:59:59")
    private String endTime;
}
