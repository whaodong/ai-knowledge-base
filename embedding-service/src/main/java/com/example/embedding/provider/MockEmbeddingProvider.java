package com.example.embedding.provider;

import com.example.embedding.model.EmbeddingModelType;
import com.example.embedding.model.EmbeddingResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Mock 编码器实现
 * 
 * <p>用于测试和演示，返回随机向量</p>
 * 
 * @author AI Knowledge Base Team
 * @since 1.0.0
 */
@Slf4j
@Component
public class MockEmbeddingProvider implements EmbeddingProvider {
    
    private static final List<EmbeddingModelType> SUPPORTED_MODELS = Arrays.asList(
        EmbeddingModelType.values()
    );
    
    @Override
    public String getName() {
        return "Mock";
    }
    
    @Override
    public List<EmbeddingModelType> getSupportedModels() {
        return SUPPORTED_MODELS;
    }
    
    @Override
    public boolean supports(EmbeddingModelType modelType) {
        return true;
    }
    
    @Override
    public boolean isEnabled() {
        return true;
    }
    
    @Override
    public EmbeddingResult embed(String text, EmbeddingModelType modelType) {
        long startTime = System.currentTimeMillis();
        
        log.debug("Mock embedding with model: {}", modelType.getModelName());
        
        // 模拟延迟
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // 生成随机向量
        List<Float> embedding = generateRandomEmbedding(modelType.getDimension());
        
        long duration = System.currentTimeMillis() - startTime;
        
        return EmbeddingResult.success(embedding, modelType, duration, false);
    }
    
    @Override
    public List<EmbeddingResult> embedBatch(List<String> texts, EmbeddingModelType modelType) {
        log.debug("Mock batch embedding with model: {}, batch size: {}", modelType.getModelName(), texts.size());
        
        List<EmbeddingResult> results = new ArrayList<>();
        
        for (String text : texts) {
            results.add(embed(text, modelType));
        }
        
        return results;
    }
    
    @Override
    public boolean healthCheck() {
        return true;
    }
    
    /**
     * 生成随机向量
     */
    private List<Float> generateRandomEmbedding(int dimension) {
        Random random = new Random();
        List<Float> embedding = new ArrayList<>(dimension);
        
        for (int i = 0; i < dimension; i++) {
            embedding.add(random.nextFloat() * 2 - 1); // -1 到 1 之间的随机数
        }
        
        return embedding;
    }
}
