package com.example.rag.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * RAG检索请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RagRequest {
    
    /**
     * 用户查询
     */
    private String query;
    
    /**
     * 检索的top-k数量
     */
    private int topK;
    
    /**
     * 相似度阈值（0-1）
     */
    private Double similarityThreshold;
    
    /**
     * 是否启用混合检索
     */
    @Builder.Default
    private boolean hybridSearch = true;
    
    /**
     * 是否启用重排序
     */
    @Builder.Default
    private boolean rerankEnabled = true;
    
    /**
     * 向量检索权重（混合检索时使用）
     */
    @Builder.Default
    private double vectorWeight = 0.7;
    
    /**
     * 关键词检索权重（混合检索时使用）
     */
    @Builder.Default
    private double keywordWeight = 0.3;
    
    /**
     * 最大上下文长度（字符数）
     */
    @Builder.Default
    private int maxContextLength = 4000;
    
    /**
     * 分块策略
     * semantic: 语义分块
     * fixed: 固定长度分块
     */
    @Builder.Default
    private String chunkingStrategy = "semantic";
    
    /**
     * 额外的元数据（如用户ID、会话ID等）
     */
    private java.util.Map<String, Object> metadata;
}