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
 * Ollama 本地编码器实现
 * 
 * <p>支持 BGE、M3E 等本地模型</p>
 * 
 * @author AI Knowledge Base Team
 * @since 1.0.0
 */
@Slf4j
@Component
public class OllamaEmbeddingProvider implements EmbeddingProvider {
    
    @Autowired
    private EmbeddingProperties properties;
    
    @Autowired(required = false)
    private RestTemplate restTemplate;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    private static final List<EmbeddingModelType> SUPPORTED_MODELS = Arrays.asList(
        EmbeddingModelType.BGE_LARGE_ZH,
        EmbeddingModelType.BGE_SMALL_ZH,
        EmbeddingModelType.M3E_BASE,
        EmbeddingModelType.M3E_LARGE
    );
    
    @Override
    public String getName() {
        return "Ollama";
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
        return properties.getOllama() != null 
            && properties.getOllama().getEnabled() 
            && properties.getOllama().getBaseUrl() != null;
    }
    
    @Override
    public EmbeddingResult embed(String text, EmbeddingModelType modelType) {
        long startTime = System.currentTimeMillis();
        
        try {
            log.debug("Ollama embedding with model: {}", modelType.getModelName());
            
            if (restTemplate == null) {
                restTemplate = new RestTemplate();
            }
            
            String url = properties.getOllama().getBaseUrl() + "/api/embeddings";
            
            Map<String, Object> request = new HashMap<>();
            request.put("model", modelType.getModelName());
            request.put("prompt", text);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, String.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode embeddingNode = root.get("embedding");
                
                if (embeddingNode != null && embeddingNode.isArray()) {
                    List<Float> embedding = new ArrayList<>();
                    for (JsonNode node : embeddingNode) {
                        embedding.add((float) node.asDouble());
                    }
                    
                    long duration = System.currentTimeMillis() - startTime;
                    log.debug("Ollama embedding completed in {}ms", duration);
                    
                    return EmbeddingResult.success(embedding, modelType, duration, false);
                }
            }
            
            return EmbeddingResult.failure("Invalid response from Ollama");
            
        } catch (Exception e) {
            log.error("Ollama embedding failed: {}", e.getMessage(), e);
            return EmbeddingResult.failure("Ollama embedding failed: " + e.getMessage());
        }
    }
    
    @Override
    public List<EmbeddingResult> embedBatch(List<String> texts, EmbeddingModelType modelType) {
        log.debug("Ollama batch embedding with model: {}, batch size: {}", modelType.getModelName(), texts.size());
        
        List<EmbeddingResult> results = new ArrayList<>();
        
        // Ollama 不支持真正的批量API，逐个处理
        for (String text : texts) {
            results.add(embed(text, modelType));
        }
        
        return results;
    }
    
    @Override
    public boolean healthCheck() {
        try {
            if (!isEnabled()) {
                return false;
            }
            
            if (restTemplate == null) {
                restTemplate = new RestTemplate();
            }
            
            String url = properties.getOllama().getBaseUrl() + "/api/tags";
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            
            return response.getStatusCode() == HttpStatus.OK;
        } catch (Exception e) {
            log.error("Ollama health check failed", e);
            return false;
        }
    }
}
