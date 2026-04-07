package com.example.document.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 文档实体
 */
@Data
@Entity
@Table(name = "documents")
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String originalFileName;

    @Column(nullable = false)
    private String filePath;

    private String fileType;

    private Long fileSize;

    private String checksum;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(length = 50)
    @Enumerated(EnumType.STRING)
    private DocumentStatus status = DocumentStatus.UPLOADED;

    @Column(columnDefinition = "TEXT")
    private String metadata;

    @CreationTimestamp
    private LocalDateTime createTime;

    @UpdateTimestamp
    private LocalDateTime updateTime;

    /**
     * 文档状态枚举
     */
    public enum DocumentStatus {
        UPLOADED,      // 已上传
        PROCESSING,    // 处理中
        PARSED,        // 已解析
        EMBEDDED,      // 已向量化
        FAILED,        // 失败
        DELETED        // 已删除
    }
}
