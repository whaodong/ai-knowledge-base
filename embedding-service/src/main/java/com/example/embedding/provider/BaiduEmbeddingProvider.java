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
 * 百度编码器实现
 * 
 * <p>支持 Embedding-V1 模型</p>
 * 
 * @author AI Knowledge Base Team
 * @since 1.0.0
 */
@Slf4j
@Component
public class BaiduEmbeddingProvider implements EmbeddingProvider {
    
    @Autowired
    private EmbeddingProperties properties;
    
    @Autowired(required = false)
    private RestTemplate restTemplate;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    private static final String BAIDU_EMBEDDING_API = "https://aip.baidubce.com/rpc/2.0/ai_custom/v1/wenxinworkshop/embeddings/embedding-v1";
    
    private static final List<EmbeddingModelType> SUPPORTED_MODELS = Arrays.asList(
        EmbeddingModelType.BAIDU_EMBEDDING_V1
    );
    
    @Override
    public String getName() {
        return "Baidu";
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
        return properties.getBaidu() != null 
            && properties.getBaidu().getEnabled()
            && properties.getBaidu().getApiKey() != null
            && properties.getBaidu().getSecretKey() != null;
    }
    
    /**
     * 获取百度 Access Token
     */
    private String getAccessToken() {
        try {
            if (restTemplate == null) {
                restTemplate = new RestTemplate();
            }
            
            String url = String.format(
                "https://aip.baidubce.com/oauth/2.0/token?grant_type=client_credentials&client_id=%s&client_secret=%s",
                properties.getBaidu().getApiKey(),
                properties.getBaidu().getSecretKey()
            );
            
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                return root.get("access_token").asText();
            }
        } catch (Exception e) {
            log.error("Failed to get Baidu access token", e);
        }
        return null;
    }
    
    @Override
    public EmbeddingResult embed(String text, EmbeddingModelType modelType) {
        long startTime = System.currentTimeMillis();
        
        try {
            log.debug("Baidu embedding with model: {}", modelType.getModelName());
            
            String accessToken = getAccessToken();
            if (accessToken == null) {
                return EmbeddingResult.failure("Failed to get Baidu access token");
            }
            
            if (restTemplate == null) {
                restTemplate = new RestTemplate();
            }
            
            String url = BAIDU_EMBEDDING_API + "?access_token=" + accessToken;
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("input", Arrays.asList(text));
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, String.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode dataNode = root.get("data");
                
                if (dataNode != null && dataNode.isArray() && dataNode.size() > 0) {
                    JsonNode embeddingNode = dataNode.get(0).get("embedding");
                    
                    if (embeddingNode != null && embeddingNode.isArray()) {
                        List<Float> embedding = new ArrayList<>();
                        for (JsonNode node : embeddingNode) {
                            embedding.add((float) node.asDouble());
                        }
                        
                        long duration = System.currentTimeMillis() - startTime;
                        log.debug("Baidu embedding completed in {}ms", duration);
                        
                        return EmbeddingResult.success(embedding, modelType, duration, false);
                    }
                }
            }
            
            return EmbeddingResult.failure("Invalid response from Baidu");
            
        } catch (Exception e) {
            log.error("Baidu embedding failed: {}", e.getMessage(), e);
            return EmbeddingResult.failure("Baidu embedding failed: " + e.getMessage());
        }
    }
    
    @Override
    public List<EmbeddingResult> embedBatch(List<String> texts, EmbeddingModelType modelType) {
        long startTime = System.currentTimeMillis();
        List<EmbeddingResult> results = new ArrayList<>();
        
        try {
            log.debug("Baidu batch embedding with model: {}, batch size: {}", modelType.getModelName(), texts.size());
            
            String accessToken = getAccessToken();
            if (accessToken == null) {
                for (int i = 0; i < texts.size(); i++) {
                    results.add(EmbeddingResult.failure("Failed to get Baidu access token"));
                }
                return results;
            }
            
            if (restTemplate == null) {
                restTemplate = new RestTemplate();
            }
            
            String url = BAIDU_EMBEDDING_API + "?access_token=" + accessToken;
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("input", texts);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.POST, entity, String.class
            );
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode dataNode = root.get("data");
                
                if (dataNode != null && dataNode.isArray()) {
                    for (int i = 0; i < texts.size(); i++) {
                        if (i < dataNode.size()) {
                            JsonNode embeddingNode = dataNode.get(i).get("embedding");
                            
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
            
            long duration = System.currentTimeMillis() - startTime;
            log.debug("Baidu batch embedding completed in {}ms", duration);
            
        } catch (Exception e) {
            log.error("Baidu batch embedding failed: {}", e.getMessage(), e);
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
            
            String accessToken = getAccessToken();
            return accessToken != null;
        } catch (Exception e) {
            log.error("Baidu health check failed", e);
            return false;
        }
    }
}
