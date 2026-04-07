package com.example.rag.retriever;

import com.example.rag.model.RagRequest;
import com.example.rag.model.RetrievalResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 向量检索器
 * 基于Milvus向量数据库进行相似度搜索
 */
@Slf4j
@Component
public class VectorRetriever implements Retriever {
    
    private final VectorStore vectorStore;
    
    @Value("${rag.vector.weight:0.7}")
    private double weight;
    
    @Value("${rag.vector.default-top-k:10}")
    private int defaultTopK;
    
    @Value("${rag.vector.similarity-threshold:0.5}")
    private double similarityThreshold;
    
    @Autowired
    public VectorRetriever(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }
    
    @Override
    public List<RetrievalResult> retrieve(RagRequest request) {
        long startTime = System.currentTimeMillis();
        
        try {
            String query = request.getQuery();
            int topK = request.getTopK() > 0 ? request.getTopK() : defaultTopK;
            double threshold = request.getSimilarityThreshold() != null ? 
                    request.getSimilarityThreshold() : similarityThreshold;
            
            log.debug("执行向量检索，查询: {}, topK: {}, 阈值: {}", query, topK, threshold);
            
            // 使用VectorStore进行相似度搜索 (Spring AI 1.0.0-M3+ API)
            SearchRequest searchRequest = SearchRequest.query(query)
                    .withTopK(topK)
                    .withSimilarityThreshold(threshold);
            List<Document> documents = vectorStore.similaritySearch(searchRequest);
            
            // 转换为RetrievalResult
            List<RetrievalResult> results = documents.stream()
                    .map(doc -> {
                        // 计算归一化相似度分数（假设Document中已包含分数）
                        Object scoreObj = doc.getMetadata().getOrDefault("similarity", 0.8);
                        double similarity = 0.0;
                        try {
                            if (scoreObj instanceof Number) {
                                similarity = ((Number) scoreObj).doubleValue();
                            } else {
                                similarity = Double.parseDouble(scoreObj.toString());
                            }
                        } catch (NumberFormatException e) {
                            similarity = 0.8; // 默认值
                        }
                        
                        // 应用阈值过滤
                        boolean passed = similarity >= threshold;
                        
                        return RetrievalResult.builder()
                                .documentId(doc.getId())
                                .content(doc.getContent())
                                .metadata(doc.getMetadata())
                                .rawScore(similarity)
                                .rerankScore(similarity)
                                .passedThreshold(passed)
                                .retrieverType(getType())
                                .chunkIndex((int) doc.getMetadata().getOrDefault("chunk_index", 0))
                                .totalChunks((int) doc.getMetadata().getOrDefault("total_chunks", 1))
                                .build();
                    })
                    .filter(result -> result.isPassedThreshold())
                    .collect(Collectors.toList());
            
            long endTime = System.currentTimeMillis();
            log.info("向量检索完成，查询: {}, 返回结果数: {}, 耗时: {}ms", 
                    query, results.size(), endTime - startTime);
            
            return results;
            
        } catch (Exception e) {
            log.error("向量检索失败，查询: {}, 错误: {}", request.getQuery(), e.getMessage(), e);
            throw new RuntimeException("向量检索失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public String getType() {
        return "vector";
    }
    
    @Override
    public String getName() {
        return "Milvus向量检索器";
    }
    
    @Override
    public boolean isEnabled() {
        return true;
    }
    
    @Override
    public void setEnabled(boolean enabled) {
        // 向量检索器总是启用
    }
    
    /**
     * 获取向量检索权重
     */
    public double getWeight() {
        return weight;
    }
}