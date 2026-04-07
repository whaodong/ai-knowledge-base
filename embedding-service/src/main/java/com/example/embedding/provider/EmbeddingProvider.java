package com.example.embedding.provider;

import com.example.embedding.model.EmbeddingModelType;
import com.example.embedding.model.EmbeddingResult;

import java.util.List;

/**
 * 编码器提供者接口
 * 
 * <p>所有编码器都需要实现此接口</p>
 * 
 * @author AI Knowledge Base Team
 * @since 1.0.0
 */
public interface EmbeddingProvider {
    
    /**
     * 获取提供者名称
     */
    String getName();
    
    /**
     * 支持的模型列表
     */
    List<EmbeddingModelType> getSupportedModels();
    
    /**
     * 是否支持指定模型
     */
    boolean supports(EmbeddingModelType modelType);
    
    /**
     * 是否已启用（配置检查）
     */
    boolean isEnabled();
    
    /**
     * 单个文本编码
     * 
     * @param text 待编码文本
     * @param modelType 模型类型
     * @return 编码结果
     */
    EmbeddingResult embed(String text, EmbeddingModelType modelType);
    
    /**
     * 批量文本编码
     * 
     * @param texts 待编码文本列表
     * @param modelType 模型类型
     * @return 编码结果列表
     */
    List<EmbeddingResult> embedBatch(List<String> texts, EmbeddingModelType modelType);
    
    /**
     * 健康检查
     */
    boolean healthCheck();
}
