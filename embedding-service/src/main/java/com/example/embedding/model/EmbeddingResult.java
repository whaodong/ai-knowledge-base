package com.example.embedding.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 编码结果封装
 * 
 * @author AI Knowledge Base Team
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmbeddingResult {
    
    /**
     * 向量数据
     */
    private List<Float> embedding;
    
    /**
     * 向量维度
     */
    private Integer dimension;
    
    /**
     * 使用的模型
     */
    private EmbeddingModelType modelType;
    
    /**
     * 是否来自缓存
     */
    private Boolean fromCache;
    
    /**
     * 处理耗时（毫秒）
     */
    private Long duration;
    
    /**
     * 提供者（用于多模型降级）
     */
    private String provider;
    
    /**
     * 成功标记
     */
    private Boolean success;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * 创建成功结果
     */
    public static EmbeddingResult success(List<Float> embedding, EmbeddingModelType modelType, Long duration, Boolean fromCache) {
        return EmbeddingResult.builder()
                .embedding(embedding)
                .dimension(embedding.size())
                .modelType(modelType)
                .fromCache(fromCache)
                .duration(duration)
                .provider(modelType.getProvider())
                .success(true)
                .build();
    }
    
    /**
     * 创建失败结果
     */
    public static EmbeddingResult failure(String errorMessage) {
        return EmbeddingResult.builder()
                .success(false)
                .errorMessage(errorMessage)
                .fromCache(false)
                .build();
    }
}
