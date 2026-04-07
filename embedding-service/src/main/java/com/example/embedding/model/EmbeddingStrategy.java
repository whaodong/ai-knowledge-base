package com.example.embedding.model;

import lombok.Getter;

/**
 * 编码器选择策略
 * 
 * @author AI Knowledge Base Team
 * @since 1.0.0
 */
@Getter
public enum EmbeddingStrategy {
    
    /**
     * 成本优先 - 优先选择免费或低成本模型
     */
    COST_FIRST("成本优先", "优先选择免费或低成本模型，适合大规模批量处理"),
    
    /**
     * 质量优先 - 优先选择高质量模型
     */
    QUALITY_FIRST("质量优先", "优先选择高质量模型，适合精确匹配场景"),
    
    /**
     * 速度优先 - 优先选择快速模型
     */
    SPEED_FIRST("速度优先", "优先选择处理速度快的模型，适合实时处理场景"),
    
    /**
     * 平衡模式 - 综合考虑成本、质量、速度
     */
    BALANCED("平衡模式", "综合考虑成本、质量、速度，选择综合评分最高的模型"),
    
    /**
     * 固定模型 - 使用配置中指定的模型
     */
    FIXED("固定模型", "使用配置中指定的固定模型，不进行自动选择");
    
    private final String name;
    private final String description;
    
    EmbeddingStrategy(String name, String description) {
        this.name = name;
        this.description = description;
    }
}
