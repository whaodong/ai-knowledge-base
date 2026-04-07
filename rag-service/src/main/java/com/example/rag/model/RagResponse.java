package com.example.rag.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * RAG检索响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RagResponse {
    
    /**
     * 是否成功
     */
    private boolean success;
    
    /**
     * 错误信息（如果失败）
     */
    private String errorMessage;
    
    /**
     * 检索到的文档结果
     */
    private List<RetrievalResult> retrievedDocuments;
    
    /**
     * 融合后的上下文（拼接后的文本）
     */
    private String fusedContext;
    
    /**
     * 总检索时间（毫秒）
     */
    private long retrievalTimeMs;
    
    /**
     * 向量检索时间
     */
    private long vectorRetrievalTimeMs;
    
    /**
     * 关键词检索时间
     */
    private long keywordRetrievalTimeMs;
    
    /**
     * 重排序时间
     */
    private long rerankTimeMs;
    
    /**
     * 检索器统计
     */
    private RetrieverStats retrieverStats;
    
    /**
     * 评估指标（如果有）
     */
    private EvaluationMetrics evaluationMetrics;
    
    /**
     * 是否来自缓存
     */
    private boolean fromCache;
    
    /**
     * 检索器统计信息
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RetrieverStats {
        private int vectorRetrievedCount;
        private int keywordRetrievedCount;
        private int afterRerankCount;
        private int afterThresholdCount;
    }
    
    /**
     * 评估指标
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EvaluationMetrics {
        private double mrr;  // 平均倒数排名
        private double ndcgAt3;  // NDCG@3
        private double ndcgAt5;  // NDCG@5
        private double precisionAt3;
        private double precisionAt5;
        private double recallAt5;
        private double averageRelevanceScore;
    }
}