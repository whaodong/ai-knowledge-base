package com.example.document.dto;

import com.example.document.entity.Document;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文档响应
 */
@Data
@Schema(description = "文档响应")
public class DocumentResponse {

    @Schema(description = "文档ID")
    private Long id;

    @Schema(description = "文件名")
    private String fileName;

    @Schema(description = "原始文件名")
    private String originalFileName;

    @Schema(description = "文件路径")
    private String filePath;

    @Schema(description = "文件类型")
    private String fileType;

    @Schema(description = "文件大小（字节）")
    private Long fileSize;

    @Schema(description = "文件校验和")
    private String checksum;

    @Schema(description = "文档状态")
    private Document.DocumentStatus status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "文档元数据")
    private String metadata;

    /**
     * 从实体转换
     */
    public static DocumentResponse from(Document document) {
        DocumentResponse response = new DocumentResponse();
        response.setId(document.getId());
        response.setFileName(document.getFileName());
        response.setOriginalFileName(document.getOriginalFileName());
        response.setFilePath(document.getFilePath());
        response.setFileType(document.getFileType());
        response.setFileSize(document.getFileSize());
        response.setChecksum(document.getChecksum());
        response.setStatus(document.getStatus());
        response.setCreateTime(document.getCreateTime());
        response.setUpdateTime(document.getUpdateTime());
        response.setMetadata(document.getMetadata());
        return response;
    }
}
