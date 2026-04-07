package com.example.embedding.provider;

import com.example.embedding.config.EmbeddingProperties;
import com.example.embedding.model.EmbeddingModelType;
import com.example.embedding.model.EmbeddingResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * 阿里云编码器实现
 * 
 * <p>支持 text-embedding-v3 模型</p>
 * 
 * @author AI Knowledge Base Team
 * @since 1.0.0
 */
@Slf4j
@Component
public class AlibabaEmbeddingProvider implements EmbeddingProvider {
    
    @Autowired
    private EmbeddingProperties properties;
    
    @Autowired(required = false)
    private RestTemplate restTemplate;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    private static final String ALIBABA_EMBEDDING_API = "https://dashscope.aliyuncs.com/api/v1/services/embeddings/text-embedding/text-embedding";
    
    private static final List<EmbeddingModelType> SUPPORTED_MODELS = Arrays.asList(
        EmbeddingModelType.ALIBABA_TEXT_EMBEDDING_V3
    );
    
    @Override
    public String getName() {
        return "Alibaba";
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
        return properties.getAlibaba() != null 
            && properties.getAlibaba().getEnabled()
            && properties.getAlibaba().getApiKey() != null;
    }
    
    @Override
    public EmbeddingResult embed(String text, EmbeddingModelType modelType) {
        long startTime = System.currentTimeMillis();
        
        try {
            log.debug("Alibaba embedding with model: {}", modelType.getModelName());
            
            if (restTemplate == null) {
                restTemplate = new RestTemplate();
            }
            
            Map<String, Object> input = new HashMap<>();
            input.put("texts", Arrays.asList(text));
            
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("text_type", "query");
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", modelType.getModelName());
            requestBody.put("input", input);
            requestBody.put("parameters", parameters);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + properties.getAlibaba().getApiKey());
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                ALIBABA_EMBEDDING_API, HttpMethod.POST, entity, String.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode outputNode = root.get("output");
                
                if (outputNode != null) {
                    JsonNode embeddingsNode = outputNode.get("embeddings");
                    if (embeddingsNode != null && embeddingsNode.isArray() && embeddingsNode.size() > 0) {
                        JsonNode embeddingNode = embeddingsNode.get(0).get("embedding");
                        
                        if (embeddingNode != null && embeddingNode.isArray()) {
                            List<Float> embedding = new ArrayList<>();
                            for (JsonNode node : embeddingNode) {
                                embedding.add((float) node.asDouble());
                            }
                            
                            long duration = System.currentTimeMillis() - startTime;
                            log.debug("Alibaba embedding completed in {}ms", duration);
                            
                            return EmbeddingResult.success(embedding, modelType, duration, false);
                        }
                    }
                }
            }
            
            return EmbeddingResult.failure("Invalid response from Alibaba");
            
        } catch (Exception e) {
            log.error("Alibaba embedding failed: {}", e.getMessage(), e);
            return EmbeddingResult.failure("Alibaba embedding failed: " + e.getMessage());
        }
    }
    
    @Override
    public List<EmbeddingResult> embedBatch(List<String> texts, EmbeddingModelType modelType) {
        long startTime = System.currentTimeMillis();
        List<EmbeddingResult> results = new ArrayList<>();
        
        try {
            log.debug("Alibaba batch embedding with model: {}, batch size: {}", modelType.getModelName(), texts.size());
            
            if (restTemplate == null) {
                restTemplate = new RestTemplate();
            }
            
            Map<String, Object> input = new HashMap<>();
            input.put("texts", texts);
            
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("text_type", "query");
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", modelType.getModelName());
            requestBody.put("input", input);
            requestBody.put("parameters", parameters);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + properties.getAlibaba().getApiKey());
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                ALIBABA_EMBEDDING_API, HttpMethod.POST, entity, String.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode outputNode = root.get("output");
                
                if (outputNode != null) {
                    JsonNode embeddingsNode = outputNode.get("embeddings");
                    if (embeddingsNode != null && embeddingsNode.isArray()) {
                        for (int i = 0; i < texts.size(); i++) {
                            if (i < embeddingsNode.size()) {
                                JsonNode embeddingNode = embeddingsNode.get(i).get("embedding");
                                
                                if (embeddingNode != null && embeddingNode.isArray()) {
                                    List<Float> embedding = new ArrayList<>();
                                    for (JsonNode node : embeddingNode) {
                                        embedding.add((float) node.asDouble());
                                    }
                                    
                                    results.add(EmbeddingResult.success(embedding, modelType, 
                                        System.currentTimeMillis() - startTime, false));
                                } else {
                                    results.add(EmbeddingResult.failure("Invalid embedding at index " + i));
                                }
                            } else {
                                results.add(EmbeddingResult.failure("Missing embedding at index " + i));
                            }
                        }
                    }
                }
            }
            
            long duration = System.currentTimeMillis() - startTime;
            log.debug("Alibaba batch embedding completed in {}ms", duration);
            
        } catch (Exception e) {
            log.error("Alibaba batch embedding failed: {}", e.getMessage(), e);
            for (int i = 0; i < texts.size(); i++) {
                results.add(EmbeddingResult.failure("Batch embedding failed: " + e.getMessage()));
            }
        }
        
        return results;
    }
    
    @Override
    public boolean healthCheck() {
        try {
            if (!isEnabled()) {
                return false;
            }
            
            EmbeddingResult result = embed("health check", EmbeddingModelType.ALIBABA_TEXT_EMBEDDING_V3);
            return result.getSuccess();
        } catch (Exception e) {
            log.error("Alibaba health check failed", e);
            return false;
        }
    }
}
