package com.example.document.entity;

import com.example.document.multimodal.enums.DocumentType;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 文档实体
 * 支持多模态文档：文本、图像、音频、视频
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
    
    /**
     * 文档类型（多模态支持）
     */
    @Column(length = 30)
    @Enumerated(EnumType.STRING)
    private DocumentType documentType;

    private Long fileSize;

    private String checksum;

    @Column(columnDefinition = "TEXT")
    private String content;
    
    /**
     * 多模态内容类型
     */
    @Column(length = 20)
    private String modalityType;
    
    /**
     * OCR识别文本（图像）
     */
    @Column(columnDefinition = "TEXT")
    private String ocrText;
    
    /**
     * 图像描述（图像）
     */
    @Column(columnDefinition = "TEXT")
    private String imageDescription;
    
    /**
     * 转录文本（音频）
     */
    @Column(columnDefinition = "TEXT")
    private String transcriptText;
    
    /**
     * 关键帧信息（视频）
     */
    @Column(columnDefinition = "TEXT")
    private String keyFrames;
    
    /**
     * 字幕文本（视频）
     */
    @Column(columnDefinition = "TEXT")
    private String subtitleText;
    
    /**
     * 向量ID（在向量数据库中的ID）
     */
    private String vectorId;
    
    /**
     * 向量化模型
     */
    private String embeddingModel;

    @Column(length = 50)
    @Enumerated(EnumType.STRING)
    private DocumentStatus status = DocumentStatus.UPLOADED;

    @Column(columnDefinition = "TEXT")
    private String metadata;
    
    /**
     * 多模态元数据（JSON格式）
     */
    @Column(columnDefinition = "TEXT")
    private String multiModalMetadata;
    
    /**
     * 处理时长（毫秒）
     */
    private Long processingTime;
    
    /**
     * 错误信息
     */
    @Column(columnDefinition = "TEXT")
    private String errorMessage;

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
    
    /**
     * 判断是否为多模态文档
     */
    public boolean isMultiModal() {
        return documentType != null && documentType.isMultiModal();
    }
    
    /**
     * 获取可用于向量化的文本内容
     */
    public String getEmbeddableContent() {
        StringBuilder sb = new StringBuilder();
        
        if (content != null && !content.trim().isEmpty()) {
            sb.append(content);
        }
        
        if (ocrText != null && !ocrText.trim().isEmpty()) {
            if (sb.length() > 0) sb.append("\n");
            sb.append("OCR文本: ").append(ocrText);
        }
        
        if (imageDescription != null && !imageDescription.trim().isEmpty()) {
            if (sb.length() > 0) sb.append("\n");
            sb.append("图片描述: ").append(imageDescription);
        }
        
        if (transcriptText != null && !transcriptText.trim().isEmpty()) {
            if (sb.length() > 0) sb.append("\n");
            sb.append("转录文本: ").append(transcriptText);
        }
        
        if (subtitleText != null && !subtitleText.trim().isEmpty()) {
            if (sb.length() > 0) sb.append("\n");
            sb.append("字幕内容: ").append(subtitleText);
        }
        
        return sb.toString();
    }
}
