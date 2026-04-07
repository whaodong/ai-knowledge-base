package com.example.rag.service;

import com.example.rag.cache.CacheMonitorService;
import com.example.rag.cache.HotQueryDetector;
import com.example.rag.cache.PreloadService;
import com.example.rag.cache.RagCacheService;
import com.example.rag.model.RagRequest;
import com.example.rag.model.RagResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 带缓存的RAG检索服务装饰器
 * 
 * <p>在原有RagRetrievalService基础上增加缓存功能：</p>
 * <ul>
 *   <li>查询缓存检查</li>
 *   <li>热点查询识别</li>
 *   <li>预加载触发</li>
 *   <li>缓存监控统计</li>
 * </ul>
 */
@Slf4j
@Service
public class CachedRagRetrievalService {

    private final RagRetrievalService ragRetrievalService;
    private final RagCacheService ragCacheService;
    private final HotQueryDetector hotQueryDetector;
    private final PreloadService preloadService;
    private final CacheMonitorService cacheMonitorService;
    
    public CachedRagRetrievalService(
            RagRetrievalService ragRetrievalService,
            RagCacheService ragCacheService,
            HotQueryDetector hotQueryDetector,
            PreloadService preloadService,
            CacheMonitorService cacheMonitorService) {
        this.ragRetrievalService = ragRetrievalService;
        this.ragCacheService = ragCacheService;
        this.hotQueryDetector = hotQueryDetector;
        this.preloadService = preloadService;
        this.cacheMonitorService = cacheMonitorService;
    }
    
    /**
     * 执行带缓存的RAG检索
     * 
     * @param request RAG请求
     * @return RAG响应
     */
    public RagResponse retrieve(RagRequest request) {
        String query = request.getQuery();
        String userId = extractUserId(request);
        long startTime = System.currentTimeMillis();
        
        try {
            // 1. 记录查询访问（热点检测）
            hotQueryDetector.recordQuery(query, userId);
            
            // 2. 分析查询并触发预加载
            preloadService.analyzeAndPreload(userId, query);
            
            // 3. 尝试从缓存获取
            RagResponse cachedResponse = tryGetFromCache(request);
            if (cachedResponse != null) {
                log.info("命中缓存: query={}, duration={}ms", query, 
                        System.currentTimeMillis() - startTime);
                
                // 记录缓存命中
                cacheMonitorService.recordCacheAccess("similarity", true);
                preloadService.markPreloadHit(query);
                
                return cachedResponse;
            }
            
            // 4. 缓存未命中，执行实际检索
            log.debug("缓存未命中，执行实际检索: query={}", query);
            cacheMonitorService.recordCacheAccess("similarity", false);
            
            RagResponse response = ragRetrievalService.retrieve(request);
            
            // 5. 缓存结果
            if (response.isSuccess()) {
                cacheResponse(request, response);
                
                // 如果是热点查询，标记热点并延长缓存时间
                if (hotQueryDetector.isHotQuery(query)) {
                    long extendedTtl = 1800; // 30分钟
                    ragCacheService.markHotQuery(query, extendedTtl);
                    log.info("热点查询缓存已延长: query={}, ttl={}s", query, extendedTtl);
                }
            }
            
            return response;
            
        } catch (Exception e) {
            log.error("带缓存的RAG检索失败: query={}", query, e);
            
            // 降级：直接执行检索
            return ragRetrievalService.retrieve(request);
        }
    }
    
    /**
     * 尝试从缓存获取结果
     */
    private RagResponse tryGetFromCache(RagRequest request) {
        try {
            // 尝试获取相似度搜索结果
            var cachedResults = ragCacheService.getCachedSimilarityResults(request.getQuery());
            
            if (cachedResults != null && !cachedResults.isEmpty()) {
                // 构建缓存的响应
                return RagResponse.builder()
                        .success(true)
                        .retrievedDocuments(cachedResults)
                        .fusedContext("") // 上下文需要重新融合
                        .fromCache(true)
                        .build();
            }
            
        } catch (Exception e) {
            log.warn("从缓存获取结果失败", e);
        }
        
        return null;
    }
    
    /**
     * 缓存检索结果
     */
    private void cacheResponse(RagRequest request, RagResponse response) {
        try {
            // 缓存相似度搜索结果
            ragCacheService.cacheSimilarityResults(request.getQuery(), response.getRetrievedDocuments());
            
            log.debug("检索结果已缓存: query={}", request.getQuery());
            
        } catch (Exception e) {
            log.warn("缓存检索结果失败", e);
        }
    }
    
    /**
     * 从请求中提取用户ID
     */
    private String extractUserId(RagRequest request) {
        // 从请求的metadata中获取用户ID
        if (request.getMetadata() != null) {
            Object userId = request.getMetadata().get("userId");
            return userId != null ? userId.toString() : null;
        }
        return null;
    }
    
    /**
     * 批量检索（优化性能）
     */
    public java.util.List<RagResponse> batchRetrieve(java.util.List<RagRequest> requests) {
        return requests.stream()
                .map(this::retrieve)
                .collect(java.util.stream.Collectors.toList());
    }
}
