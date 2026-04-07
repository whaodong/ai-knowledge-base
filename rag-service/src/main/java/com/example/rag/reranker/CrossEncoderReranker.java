package com.example.rag.reranker;

import com.example.rag.model.RagRequest;
import com.example.rag.model.RetrievalResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 交叉编码器重排序器
 * 
 * <p>模拟Cross-Encoder重排序，使用嵌入模型计算查询与文档的相似度，
 * 对混合检索结果进行精细化重排序。</p>
 * 
 * <p>注：实际生产环境中应使用真正的Cross-Encoder模型（如sentence-transformers），
 * 本实现使用嵌入模型的余弦相似度作为近似。</p>
 */
@Slf4j
@Component
public class CrossEncoderReranker implements Reranker {
    
    private final EmbeddingModel embeddingModel;
    
    @Value("${rag.reranker.enabled:true}")
    private boolean enabled;
    
    @Value("${rag.reranker.top-k:20}")
    private int rerankTopK;
    
    @Value("${rag.reranker.similarity-threshold:0.3}")
    private double similarityThreshold;
    
    @Autowired
    public CrossEncoderReranker(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }
    
    @Override
    public List<RetrievalResult> rerank(RagRequest request, List<RetrievalResult> results) {
        if (!isEnabled() || results.isEmpty()) {
            return results;
        }
        
        long startTime = System.currentTimeMillis();
        
        try {
            String query = request.getQuery();
            
            log.debug("开始重排序，查询: {}, 输入结果数: {}", query, results.size());
            
            // 限制重排序的文档数量，避免性能问题
            List<RetrievalResult> candidates = results.stream()
                    .limit(rerankTopK)
                    .collect(Collectors.toList());
            
            if (candidates.isEmpty()) {
                return candidates;
            }
            
            // 为查询生成嵌入向量
            List<Double> queryEmbedding = generateEmbedding(query);
            
            // 计算查询与每个候选文档的相似度
            List<RetrievalResult> rerankedResults = candidates.parallelStream()
                    .map(result -> {
                        try {
                            // 为文档内容生成嵌入向量
                            List<Double> docEmbedding = generateEmbedding(result.getContent());
                            
                            // 计算余弦相似度
                            double similarity = calculateCosineSimilarity(queryEmbedding, docEmbedding);
                            
                            // 更新重排序分数
                            RetrievalResult updatedResult = RetrievalResult.builder()
                                    .documentId(result.getDocumentId())
                                    .content(result.getContent())
                                    .metadata(result.getMetadata())
                                    .rawScore(result.getRawScore())
                                    .rerankScore(similarity)
                                    .passedThreshold(similarity >= similarityThreshold)
                                    .retrieverType(result.getRetrieverType())
                                    .chunkIndex(result.getChunkIndex())
                                    .totalChunks(result.getTotalChunks())
                                    .build();
                            
                            return updatedResult;
                            
                        } catch (Exception e) {
                            log.warn("计算文档相似度失败，文档ID: {}, 错误: {}", 
                                    result.getDocumentId(), e.getMessage());
                            return result; // 返回原始结果
                        }
                    })
                    .sorted((r1, r2) -> Double.compare(r2.getRerankScore(), r1.getRerankScore()))
                    .collect(Collectors.toList());
            
            long endTime = System.currentTimeMillis();
            log.info("重排序完成，查询: {}, 输入结果数: {}, 输出结果数: {}, 耗时: {}ms", 
                    query, results.size(), rerankedResults.size(), endTime - startTime);
            
            return rerankedResults;
            
        } catch (Exception e) {
            log.error("重排序失败，查询: {}, 错误: {}", request.getQuery(), e.getMessage(), e);
            return results; // 返回原始结果
        }
    }
    
    /**
     * 生成文本嵌入向量
     */
    private List<Double> generateEmbedding(String text) {
        try {
            // 使用Spring AI的EmbeddingModel生成嵌入向量
            // Spring AI 1.0.0-M3+ API返回float[]
            float[] embedding = embeddingModel.embed(text);
            List<Double> result = new ArrayList<>(embedding.length);
            for (float f : embedding) {
                result.add((double) f);
            }
            return result;
        } catch (Exception e) {
            log.warn("生成嵌入向量失败，文本: {}, 错误: {}", text, e.getMessage());
            
            // 返回随机向量作为降级方案（生产环境应有更好的降级策略）
            return generateRandomVector(1536);
        }
    }
    
    /**
     * 生成随机向量（降级方案）
     */
    private List<Double> generateRandomVector(int dimension) {
        Random random = new Random();
        List<Double> vector = new ArrayList<>(dimension);
        for (int i = 0; i < dimension; i++) {
            vector.add(random.nextDouble() * 2 - 1); // 生成-1到1之间的随机数
        }
        return vector;
    }
    
    /**
     * 计算余弦相似度
     */
    private double calculateCosineSimilarity(List<Double> vector1, List<Double> vector2) {
        if (vector1.size() != vector2.size()) {
            throw new IllegalArgumentException("向量维度不一致");
        }
        
        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;
        
        for (int i = 0; i < vector1.size(); i++) {
            double v1 = vector1.get(i);
            double v2 = vector2.get(i);
            dotProduct += v1 * v2;
            norm1 += v1 * v1;
            norm2 += v2 * v2;
        }
        
        if (norm1 == 0.0 || norm2 == 0.0) {
            return 0.0;
        }
        
        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }
    
    @Override
    public String getName() {
        return "交叉编码器重排序器";
    }
    
    @Override
    public boolean isEnabled() {
        return enabled;
    }
    
    /**
     * 设置启用状态
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}