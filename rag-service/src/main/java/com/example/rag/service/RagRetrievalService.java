package com.example.rag.service;

import com.example.rag.manager.ContextManager;
import com.example.rag.model.RagRequest;
import com.example.rag.model.RagResponse;
import com.example.rag.model.RetrievalResult;
import com.example.rag.reranker.Reranker;
import com.example.rag.retriever.Retriever;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * RAG检索服务核心实现
 * 
 * <p>实现检索器+生成器流水线架构，支持多路召回、重排序和上下文管理。</p>
 */
@Slf4j
@Service
public class RagRetrievalService {
    
    private final List<Retriever> retrievers;
    private final List<Reranker> rerankers;
    private final ContextManager contextManager;
    private final ExecutorService executorService;
    
    @Autowired
    public RagRetrievalService(
            List<Retriever> retrievers,
            List<Reranker> rerankers,
            ContextManager contextManager) {
        this.retrievers = retrievers;
        this.rerankers = rerankers;
        this.contextManager = contextManager;
        this.executorService = Executors.newFixedThreadPool(4);
    }
    
    /**
     * 执行RAG检索
     * 
     * @param request RAG请求
     * @return RAG响应
     */
    public RagResponse retrieve(RagRequest request) {
        long totalStartTime = System.currentTimeMillis();
        RagResponse.RetrieverStats stats = RagResponse.RetrieverStats.builder()
                .vectorRetrievedCount(0)
                .keywordRetrievedCount(0)
                .afterRerankCount(0)
                .afterThresholdCount(0)
                .build();
        
        try {
            log.info("开始RAG检索，查询: {}", request.getQuery());
            
            // 步骤1: 并行执行多路检索
            long retrievalStartTime = System.currentTimeMillis();
            List<RetrievalResult> allResults = executeParallelRetrieval(request);
            long vectorRetrievalTime = 0;
            long keywordRetrievalTime = 0;
            
            // 统计各检索器结果数量
            Map<String, List<RetrievalResult>> resultsByType = allResults.stream()
                    .collect(Collectors.groupingBy(RetrievalResult::getRetrieverType));
            
            stats.setVectorRetrievedCount(resultsByType.getOrDefault("vector", Collections.emptyList()).size());
            stats.setKeywordRetrievedCount(resultsByType.getOrDefault("keyword", Collections.emptyList()).size());
            
            long retrievalEndTime = System.currentTimeMillis();
            long retrievalTimeMs = retrievalEndTime - retrievalStartTime;
            
            log.debug("多路检索完成，总结果数: {} (向量: {}, 关键词: {}), 耗时: {}ms",
                    allResults.size(), stats.getVectorRetrievedCount(), 
                    stats.getKeywordRetrievedCount(), retrievalTimeMs);
            
            // 步骤2: 结果融合与去重
            List<RetrievalResult> fusedResults = fuseAndDeduplicateResults(allResults, request);
            log.debug("结果融合去重完成，融合后结果数: {}", fusedResults.size());
            
            // 步骤3: 重排序
            long rerankStartTime = System.currentTimeMillis();
            List<RetrievalResult> rerankedResults = applyReranking(request, fusedResults);
            long rerankEndTime = System.currentTimeMillis();
            long rerankTimeMs = rerankEndTime - rerankStartTime;
            
            stats.setAfterRerankCount(rerankedResults.size());
            log.debug("重排序完成，结果数: {}, 耗时: {}ms", rerankedResults.size(), rerankTimeMs);
            
            // 步骤4: 阈值过滤
            List<RetrievalResult> filteredResults = rerankedResults.stream()
                    .filter(RetrievalResult::isPassedThreshold)
                    .collect(Collectors.toList());
            
            stats.setAfterThresholdCount(filteredResults.size());
            log.debug("阈值过滤完成，过滤后结果数: {}", filteredResults.size());
            
            // 步骤5: 上下文融合
            String fusedContext = contextManager.fuseContext(request, filteredResults);
            log.debug("上下文融合完成，上下文长度: {}字符", fusedContext.length());
            
            // 步骤6: 构建响应
            long totalEndTime = System.currentTimeMillis();
            long totalTimeMs = totalEndTime - totalStartTime;
            
            RagResponse response = RagResponse.builder()
                    .success(true)
                    .retrievedDocuments(filteredResults)
                    .fusedContext(fusedContext)
                    .retrievalTimeMs(totalTimeMs)
                    .vectorRetrievalTimeMs(vectorRetrievalTime)
                    .keywordRetrievalTimeMs(keywordRetrievalTime)
                    .rerankTimeMs(rerankTimeMs)
                    .retrieverStats(stats)
                    .build();
            
            log.info("RAG检索完成，查询: {}, 最终结果数: {}, 总耗时: {}ms",
                    request.getQuery(), filteredResults.size(), totalTimeMs);
            
            return response;
            
        } catch (Exception e) {
            log.error("RAG检索失败，查询: {}, 错误: {}", request.getQuery(), e.getMessage(), e);
            
            return RagResponse.builder()
                    .success(false)
                    .errorMessage("检索失败: " + e.getMessage())
                    .retrievalTimeMs(System.currentTimeMillis() - totalStartTime)
                    .retrieverStats(stats)
                    .build();
        }
    }
    
    /**
     * 并行执行多路检索
     */
    private List<RetrievalResult> executeParallelRetrieval(RagRequest request) {
        // 创建并行任务
        List<CompletableFuture<List<RetrievalResult>>> futures = retrievers.stream()
                .filter(Retriever::isEnabled)
                .map(retriever -> CompletableFuture.supplyAsync(
                        () -> {
                            try {
                                return retriever.retrieve(request);
                            } catch (Exception e) {
                                log.error("检索器 {} 执行失败: {}", retriever.getName(), e.getMessage());
                                return Collections.<RetrievalResult>emptyList();
                            }
                        },
                        executorService
                ))
                .collect(Collectors.<CompletableFuture<List<RetrievalResult>>>toList());
        
        // 等待所有任务完成并合并结果
        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0])
        );
        
        return allFutures.thenApply(v -> futures.stream()
                .map(CompletableFuture::join)
                .flatMap(List::stream)
                .collect(Collectors.toList())
        ).join();
    }
    
    /**
     * 结果融合与去重
     */
    private List<RetrievalResult> fuseAndDeduplicateResults(
            List<RetrievalResult> allResults, RagRequest request) {
        
        if (!request.isHybridSearch()) {
            // 非混合搜索：直接返回所有结果
            return allResults;
        }
        
        // 按文档ID分组，处理重复文档
        Map<String, List<RetrievalResult>> resultsByDocId = allResults.stream()
                .collect(Collectors.groupingBy(RetrievalResult::getDocumentId));
        
        List<RetrievalResult> fusedResults = new ArrayList<>();
        
        for (Map.Entry<String, List<RetrievalResult>> entry : resultsByDocId.entrySet()) {
            List<RetrievalResult> duplicateResults = entry.getValue();
            
            if (duplicateResults.size() == 1) {
                // 无重复，直接添加
                fusedResults.add(duplicateResults.get(0));
            } else {
                // 重复文档：进行分数融合
                RetrievalResult fusedResult = fuseDuplicateResults(duplicateResults, request);
                fusedResults.add(fusedResult);
            }
        }
        
        return fusedResults;
    }
    
    /**
     * 融合重复文档的分数
     */
    private RetrievalResult fuseDuplicateResults(
            List<RetrievalResult> duplicateResults, RagRequest request) {
        
        // 取第一个结果作为基准
        RetrievalResult baseResult = duplicateResults.get(0);
        
        // 计算加权平均分数
        double vectorWeight = request.getVectorWeight();
        double keywordWeight = request.getKeywordWeight();
        
        double vectorScore = 0.0;
        double keywordScore = 0.0;
        boolean hasVector = false;
        boolean hasKeyword = false;
        
        for (RetrievalResult result : duplicateResults) {
            if ("vector".equals(result.getRetrieverType())) {
                vectorScore = result.getRawScore();
                hasVector = true;
            } else if ("keyword".equals(result.getRetrieverType())) {
                keywordScore = result.getRawScore();
                hasKeyword = true;
            }
        }
        
        // 如果没有某种类型的分数，则调整权重
        double totalWeight = 0.0;
        double fusedScore = 0.0;
        
        if (hasVector) {
            fusedScore += vectorScore * vectorWeight;
            totalWeight += vectorWeight;
        }
        
        if (hasKeyword) {
            fusedScore += keywordScore * keywordWeight;
            totalWeight += keywordWeight;
        }
        
        if (totalWeight > 0) {
            fusedScore /= totalWeight;
        }
        
        // 构建融合后的结果
        return RetrievalResult.builder()
                .documentId(baseResult.getDocumentId())
                .content(baseResult.getContent())
                .metadata(baseResult.getMetadata())
                .rawScore(fusedScore)
                .rerankScore(fusedScore) // 初始重排序分数与原始分数相同
                .passedThreshold(baseResult.isPassedThreshold())
                .retrieverType("hybrid") // 标记为混合结果
                .chunkIndex(baseResult.getChunkIndex())
                .totalChunks(baseResult.getTotalChunks())
                .build();
    }
    
    /**
     * 应用重排序
     */
    private List<RetrievalResult> applyReranking(RagRequest request, List<RetrievalResult> results) {
        if (!request.isRerankEnabled() || results.isEmpty()) {
            return results;
        }
        
        List<RetrievalResult> currentResults = results;
        
        // 按顺序应用所有启用的重排序器
        for (Reranker reranker : rerankers) {
            if (reranker.isEnabled()) {
                try {
                    currentResults = reranker.rerank(request, currentResults);
                    log.debug("重排序器 {} 应用完成，结果数: {}", reranker.getName(), currentResults.size());
                } catch (Exception e) {
                    log.error("重排序器 {} 执行失败: {}", reranker.getName(), e.getMessage());
                    // 继续使用之前的结果
                }
            }
        }
        
        return currentResults;
    }
    
    /**
     * 批量检索（优化性能）
     */
    public List<RagResponse> batchRetrieve(List<RagRequest> requests) {
        return requests.stream()
                .map(this::retrieve)
                .collect(Collectors.toList());
    }
    
    /**
     * 服务关闭时清理资源
     */
    public void shutdown() {
        executorService.shutdown();
        log.info("RAG检索服务已关闭");
    }
}