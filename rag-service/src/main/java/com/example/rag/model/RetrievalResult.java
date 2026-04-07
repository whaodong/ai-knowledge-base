package com.example.rag.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 检索结果封装
 * 包含文档内容、相关性分数、来源信息等
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetrievalResult {
    
    /**
     * 文档ID
     */
    private String documentId;
    
    /**
     * 文档内容
     */
    private String content;
    
    /**
     * 元数据
     */
    private Map<String, Object> metadata;
    
    /**
     * 原始相关性分数（来自检索器）
     */
    private double rawScore;
    
    /**
     * 重排序后的分数
     */
    private double rerankScore;
    
    /**
     * 是否通过阈值过滤
     */
    private boolean passedThreshold;
    
    /**
     * 检索器类型（vector/keyword）
     */
    private String retrieverType;
    
    /**
     * 分块索引
     */
    private int chunkIndex;
    
    /**
     * 总块数
     */
    private int totalChunks;
}