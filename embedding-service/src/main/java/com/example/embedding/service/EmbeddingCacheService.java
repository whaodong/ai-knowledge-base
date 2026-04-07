package com.example.embedding.service;

import com.example.embedding.config.EmbeddingProperties;
import com.example.embedding.model.EmbeddingModelType;
import com.example.embedding.model.EmbeddingResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 编码缓存服务
 * 
 * <p>提供文本->向量的持久化缓存</p>
 * 
 * @author AI Knowledge Base Team
 * @since 1.0.0
 */
@Slf4j
@Service
public class EmbeddingCacheService {
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    @Autowired
    private EmbeddingProperties properties;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // 统计数据
    private final AtomicLong hitCount = new AtomicLong(0);
    private final AtomicLong missCount = new AtomicLong(0);
    
    /**
     * 获取缓存的向量
     */
    public EmbeddingResult get(String text, EmbeddingModelType modelType) {
        if (!properties.getCache().getEnabled()) {
            return null;
        }
        
        try {
            String key = buildCacheKey(text, modelType);
            String cached = redisTemplate.opsForValue().get(key);
            
            if (cached != null) {
                CacheEntry entry = objectMapper.readValue(cached, CacheEntry.class);
                
                EmbeddingResult result = EmbeddingResult.success(
                    entry.getEmbedding(),
                    modelType,
                    entry.getOriginalDuration(),
                    true
                );
                
                if (properties.getCache().getTrackHitRate()) {
                    hitCount.incrementAndGet();
                }
                
                log.debug("Cache hit for text (model: {})", modelType.getModelName());
                return result;
            }
            
            if (properties.getCache().getTrackHitRate()) {
                missCount.incrementAndGet();
            }
            
            return null;
            
        } catch (Exception e) {
            log.error("Failed to get cache: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 存储向量到缓存
     */
    public void put(String text, EmbeddingModelType modelType, EmbeddingResult result) {
        if (!properties.getCache().getEnabled() || !result.getSuccess()) {
            return;
        }
        
        try {
            String key = buildCacheKey(text, modelType);
            
            CacheEntry entry = new CacheEntry();
            entry.setEmbedding(result.getEmbedding());
            entry.setModel(modelType.getModelName());
            entry.setDimension(result.getDimension());
            entry.setOriginalDuration(result.getDuration());
            entry.setCreatedAt(System.currentTimeMillis());
            
            String value = objectMapper.writeValueAsString(entry);
            
            redisTemplate.opsForValue().set(
                key,
                value,
                properties.getCache().getExpireSeconds(),
                TimeUnit.SECONDS
            );
            
            log.debug("Cached embedding for text (model: {})", modelType.getModelName());
            
        } catch (JsonProcessingException e) {
            log.error("Failed to cache embedding: {}", e.getMessage());
        }
    }
    
    /**
     * 删除缓存
     */
    public void delete(String text, EmbeddingModelType modelType) {
        String key = buildCacheKey(text, modelType);
        redisTemplate.delete(key);
    }
    
    /**
     * 清空所有缓存
     */
    public void clearAll() {
        String pattern = properties.getCache().getKeyPrefix() + "*";
        redisTemplate.delete(redisTemplate.keys(pattern));
        log.info("Cleared all embedding cache");
    }
    
    /**
     * 获取缓存统计
     */
    public CacheStats getStats() {
        CacheStats stats = new CacheStats();
        
        stats.setHitCount(hitCount.get());
        stats.setMissCount(missCount.get());
        stats.setTotalRequests(stats.getHitCount() + stats.getMissCount());
        
        if (stats.getTotalRequests() > 0) {
            stats.setHitRate((double) stats.getHitCount() / stats.getTotalRequests() * 100);
        }
        
        // 获取缓存大小
        Long size = redisTemplate.execute((org.springframework.data.redis.core.RedisCallback<Long>) connection -> {
            return connection.dbSize();
        });
        stats.setCacheSize(size != null ? size : 0L);
        
        return stats;
    }
    
    /**
     * 重置统计
     */
    public void resetStats() {
        hitCount.set(0);
        missCount.set(0);
    }
    
    /**
     * 批量获取缓存
     */
    public Map<String, EmbeddingResult> batchGet(Map<String, EmbeddingModelType> textModelMap) {
        Map<String, EmbeddingResult> results = new HashMap<>();
        
        for (Map.Entry<String, EmbeddingModelType> entry : textModelMap.entrySet()) {
            EmbeddingResult cached = get(entry.getKey(), entry.getValue());
            if (cached != null) {
                results.put(entry.getKey(), cached);
            }
        }
        
        return results;
    }
    
    /**
     * 批量存储缓存
     */
    public void batchPut(Map<String, Map<EmbeddingModelType, EmbeddingResult>> results) {
        for (Map.Entry<String, Map<EmbeddingModelType, EmbeddingResult>> entry : results.entrySet()) {
            for (Map.Entry<EmbeddingModelType, EmbeddingResult> modelEntry : entry.getValue().entrySet()) {
                put(entry.getKey(), modelEntry.getKey(), modelEntry.getValue());
            }
        }
    }
    
    /**
     * 构建缓存键
     */
    private String buildCacheKey(String text, EmbeddingModelType modelType) {
        try {
            // 使用文本和模型名称生成唯一键
            String content = modelType.getModelName() + ":" + text;
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(content.getBytes(StandardCharsets.UTF_8));
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return properties.getCache().getKeyPrefix() + hexString.toString();
            
        } catch (NoSuchAlgorithmException e) {
            // 降级：使用简单的字符串哈希
            return properties.getCache().getKeyPrefix() + 
                modelType.getModelName() + ":" + text.hashCode();
        }
    }
    
    /**
     * 缓存条目
     */
    @Data
    private static class CacheEntry {
        private java.util.List<Float> embedding;
        private String model;
        private Integer dimension;
        private Long originalDuration;
        private Long createdAt;
    }
    
    /**
     * 缓存统计
     */
    @Data
    public static class CacheStats {
        private Long hitCount;
        private Long missCount;
        private Long totalRequests;
        private Double hitRate;
        private Long cacheSize;
    }
}
