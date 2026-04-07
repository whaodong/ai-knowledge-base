package com.example.rag.cache;

import com.example.common.cache.MultiLevelCacheService;
import com.example.rag.model.RetrievalResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * RAG缓存服务
 * 负责缓存RAG流程中的关键数据：
 * 1. 文档向量嵌入结果
 * 2. 相似度搜索结果
 * 3. LLM生成结果
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnBean({CacheManager.class, RedisTemplate.class})
public class RagCacheService {

    private final MultiLevelCacheService cacheService;
    
    // 缓存区域定义
    private static final String CACHE_EMBEDDING = "embedding";
    private static final String CACHE_SIMILARITY = "similarity";
    private static final String CACHE_LLM_RESPONSE = "llm_response";
    private static final String CACHE_HOTSPOT = "hotspot";
    
    // 缓存时间配置（秒）
    private static final long EMBEDDING_TTL = 3600;      // 1小时
    private static final long SIMILARITY_TTL = 300;      // 5分钟
    private static final long LLM_RESPONSE_TTL = 1800;   // 30分钟
    private static final long HOTSPOT_TTL = 600;         // 10分钟
    
    /**
     * 缓存文档向量嵌入结果
     * 
     * @param text 原始文本
     * @param embedding 向量嵌入结果
     */
    public void cacheEmbedding(String text, List<Double> embedding) {
        String key = generateTextHash(text);
        cacheService.put(CACHE_EMBEDDING, key, embedding, EMBEDDING_TTL);
        log.debug("缓存文档向量嵌入: key={}, dimension={}", key, embedding.size());
    }
    
    /**
     * 获取缓存的文档向量嵌入
     */
    public List<Double> getCachedEmbedding(String text) {
        String key = generateTextHash(text);
        List<Double> embedding = cacheService.get(CACHE_EMBEDDING, key, () -> null, EMBEDDING_TTL);
        
        if (embedding != null) {
            log.debug("命中文档向量嵌入缓存: key={}", key);
        }
        
        return embedding;
    }
    
    /**
     * 缓存相似度搜索结果
     * 
     * @param query 查询文本
     * @param results 搜索结果文档列表
     */
    public void cacheSimilarityResults(String query, List<RetrievalResult> results) {
        String key = generateQueryHash(query);
        cacheService.put(CACHE_SIMILARITY, key, results, SIMILARITY_TTL);
        log.debug("缓存相似度搜索结果: key={}, count={}", key, results.size());
    }
    
    /**
     * 获取缓存的相似度搜索结果
     */
    public List<RetrievalResult> getCachedSimilarityResults(String query) {
        String key = generateQueryHash(query);
        List<RetrievalResult> results = cacheService.get(CACHE_SIMILARITY, key, () -> null, SIMILARITY_TTL);
        
        if (results != null) {
            log.debug("命中相似度搜索缓存: key={}, count={}", key, results.size());
        }
        
        return results;
    }
    
    /**
     * 缓存LLM生成结果
     * 
     * @param context 上下文（查询+相关文档）
     * @param response LLM响应
     */
    public void cacheLlmResponse(String context, String response) {
        String key = generateContextHash(context);
        cacheService.put(CACHE_LLM_RESPONSE, key, response, LLM_RESPONSE_TTL);
        log.debug("缓存LLM响应: key={}, length={}", key, response.length());
    }
    
    /**
     * 获取缓存的LLM生成结果
     */
    public String getCachedLlmResponse(String context) {
        String key = generateContextHash(context);
        String response = cacheService.get(CACHE_LLM_RESPONSE, key, () -> null, LLM_RESPONSE_TTL);
        
        if (response != null) {
            log.debug("命中LLM响应缓存: key={}", key);
        }
        
        return response;
    }
    
    /**
     * 标记热点查询
     * 用于热点key探测和优化
     * 
     * @param query 热点查询
     * @param ttl 缓存时间
     */
    public void markHotQuery(String query, long ttl) {
        String key = generateQueryHash(query);
        cacheService.put(CACHE_HOTSPOT, key, true, ttl);
        log.info("标记热点查询: {}", query);
    }
    
    /**
     * 检查是否为热点查询
     */
    public boolean isHotQuery(String query) {
        String key = generateQueryHash(query);
        Boolean isHot = cacheService.get(CACHE_HOTSPOT, key, () -> false, HOTSPOT_TTL);
        return Boolean.TRUE.equals(isHot);
    }
    
    /**
     * 批量缓存文档向量嵌入
     * 用于初始化或批量处理场景
     */
    public void batchCacheEmbeddings(Map<String, List<Double>> embeddings) {
        Map<String, List<Double>> cacheData = embeddings.entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> generateTextHash(entry.getKey()),
                        Map.Entry::getValue
                ));
        
        // 这里需要扩展MultiLevelCacheService支持批量操作
        // 暂时循环处理
        cacheData.forEach((key, embedding) -> {
            cacheService.put(CACHE_EMBEDDING, key, embedding, EMBEDDING_TTL);
        });
        
        log.info("批量缓存文档向量嵌入: count={}", embeddings.size());
    }
    
    /**
     * 清除所有RAG相关缓存
     * 用于数据更新或维护场景
     */
    public void clearAllRagCache() {
        // 需要扩展MultiLevelCacheService支持按模式删除
        // 暂时记录操作
        log.info("清除所有RAG缓存");
    }
    
    /**
     * 生成文本哈希键
     * 使用简单的哈希算法，实际项目中可能需要更复杂的处理
     */
    private String generateTextHash(String text) {
        return String.valueOf(text.hashCode());
    }
    
    /**
     * 生成查询哈希键
     * 查询可能需要规范化处理（如去除停用词、词干提取等）
     */
    private String generateQueryHash(String query) {
        // 简单实现，实际项目中可能需要查询规范化
        return "query:" + generateTextHash(query);
    }
    
    /**
     * 生成上下文哈希键
     * 上下文包括查询和相关文档
     */
    private String generateContextHash(String context) {
        return "context:" + generateTextHash(context);
    }
    
    /**
     * 获取缓存命中率统计
     */
    public Map<String, Object> getCacheStats() {
        // 这里需要扩展MultiLevelCacheService获取详细统计
        // 暂时返回简单信息
        return Map.of(
                "status", "enabled",
                "levels", "L1(Local/Caffeine) + L2(Redis)",
                "strategy", "Cache-Aside with multi-level protection"
        );
    }
}