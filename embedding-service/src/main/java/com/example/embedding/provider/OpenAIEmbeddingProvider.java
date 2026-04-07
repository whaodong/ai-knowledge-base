package com.example.embedding.provider;

import com.example.embedding.config.EmbeddingProperties;
import com.example.embedding.model.EmbeddingModelType;
import com.example.embedding.model.EmbeddingResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.embedding.Embedding;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * OpenAI 编码器实现
 * 
 * @author AI Knowledge Base Team
 * @since 1.0.0
 */
@Slf4j
@Component
public class OpenAIEmbeddingProvider implements EmbeddingProvider {
    
    @Autowired
    private EmbeddingModel embeddingModel;
    
    @Autowired
    private EmbeddingProperties properties;
    
    private static final List<EmbeddingModelType> SUPPORTED_MODELS = Arrays.asList(
        EmbeddingModelType.OPENAI_TEXT_EMBEDDING_3_SMALL,
        EmbeddingModelType.OPENAI_TEXT_EMBEDDING_3_LARGE,
        EmbeddingModelType.OPENAI_TEXT_EMBEDDING_ADA_002
    );
    
    @Override
    public String getName() {
        return "OpenAI";
    }
    
    @Override
    public List<EmbeddingModelType> getSupportedModels() {
        return SUPPORTED_MODELS;
    }
    
    @Override
    public boolean supports(EmbeddingModelType modelType) {
        return SUPPORTED_MODELS.contains(modelType);
    }
    
    @Override
    public boolean isEnabled() {
        return properties.getOpenai() != null && properties.getOpenai().getApiKey() != null;
    }
    
    @Override
    public EmbeddingResult embed(String text, EmbeddingModelType modelType) {
        long startTime = System.currentTimeMillis();
        
        try {
            log.debug("OpenAI embedding with model: {}", modelType.getModelName());
            
            // 使用embedForResponse方法
            EmbeddingResponse response = embeddingModel.embedForResponse(List.of(text));
            
            if (response != null && !response.getResults().isEmpty()) {
                Embedding embeddingResult = response.getResults().get(0);
                float[] values = embeddingResult.getOutput();
                
                List<Float> embedding = new ArrayList<>();
                for (float value : values) {
                    embedding.add(value);
                }
                
                long duration = System.currentTimeMillis() - startTime;
                log.debug("OpenAI embedding completed in {}ms", duration);
                
                return EmbeddingResult.success(embedding, modelType, duration, false);
            } else {
                return EmbeddingResult.failure("Empty response from OpenAI");
            }
            
        } catch (Exception e) {
            log.error("OpenAI embedding failed: {}", e.getMessage(), e);
            return EmbeddingResult.failure("OpenAI embedding failed: " + e.getMessage());
        }
    }
    
    @Override
    public List<EmbeddingResult> embedBatch(List<String> texts, EmbeddingModelType modelType) {
        long startTime = System.currentTimeMillis();
        List<EmbeddingResult> results = new ArrayList<>();
        
        try {
            log.debug("OpenAI batch embedding with model: {}, batch size: {}", modelType.getModelName(), texts.size());
            
            // 使用embedForResponse批量处理
            EmbeddingResponse response = embeddingModel.embedForResponse(texts);
            
            if (response != null) {
                List<Embedding> embeddings = response.getResults();
                
                for (int i = 0; i < texts.size(); i++) {
                    if (i < embeddings.size()) {
                        float[] values = embeddings.get(i).getOutput();
                        List<Float> embedding = new ArrayList<>();
                        for (float value : values) {
                            embedding.add(value);
                        }
                        
                        results.add(EmbeddingResult.success(embedding, modelType, 
                            System.currentTimeMillis() - startTime, false));
                    } else {
                        results.add(EmbeddingResult.failure("Missing embedding at index " + i));
                    }
                }
            }
            
            long duration = System.currentTimeMillis() - startTime;
            log.debug("OpenAI batch embedding completed in {}ms", duration);
            
        } catch (Exception e) {
            log.error("OpenAI batch embedding failed: {}", e.getMessage(), e);
            for (int i = 0; i < texts.size(); i++) {
                results.add(EmbeddingResult.failure("Batch embedding failed: " + e.getMessage()));
            }
        }
        
        return results;
    }
    
    @Override
    public boolean healthCheck() {
        try {
            EmbeddingResult result = embed("health check", EmbeddingModelType.OPENAI_TEXT_EMBEDDING_3_SMALL);
            return result.getSuccess();
        } catch (Exception e) {
            log.error("OpenAI health check failed", e);
            return false;
        }
    }
}
