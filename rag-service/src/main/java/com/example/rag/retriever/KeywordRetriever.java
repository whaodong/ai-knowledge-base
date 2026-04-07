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

import java.util.*;
import java.util.stream.Collectors;

/**
 * 关键词检索器
 * 基于BM25算法进行关键词匹配搜索
 * 注：简化实现，实际生产环境应使用Elasticsearch或Lucene
 */
@Slf4j
@Component
public class KeywordRetriever implements Retriever {
    
    private final VectorStore vectorStore; // 用于获取文档集合
    
    // 简单的内存文档索引
    private final Map<String, Document> documentIndex = new HashMap<>();
    private final Map<String, Map<String, Double>> termFrequencies = new HashMap<>();
    private final Map<String, Double> inverseDocumentFrequencies = new HashMap<>();
    private double averageDocumentLength = 0.0;
    private boolean indexBuilt = false;
    
    @Value("${rag.keyword.weight:0.3}")
    private double weight;
    
    @Value("${rag.keyword.default-top-k:10}")
    private int defaultTopK;
    
    @Value("${rag.keyword.bm25-k1:1.2}")
    private double bm25K1;
    
    @Value("${rag.keyword.bm25-b:0.75}")
    private double bm25B;
    
    @Autowired
    public KeywordRetriever(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }
    
    @Override
    public List<RetrievalResult> retrieve(RagRequest request) {
        long startTime = System.currentTimeMillis();
        
        try {
            // 首次使用前构建索引
            if (!indexBuilt) {
                buildIndex();
            }
            
            String query = request.getQuery();
            int topK = request.getTopK() > 0 ? request.getTopK() : defaultTopK;
            
            log.debug("执行关键词检索，查询: {}, topK: {}", query, topK);
            
            // 分词处理（简化：按空格分割）
            String[] queryTerms = query.toLowerCase().split("\\s+");
            
            // 计算BM25分数
            Map<String, Double> documentScores = new HashMap<>();
            
            for (String docId : documentIndex.keySet()) {
                double score = calculateBM25Score(docId, queryTerms);
                documentScores.put(docId, score);
            }
            
            // 按分数排序，取topK
            List<RetrievalResult> results = documentScores.entrySet().stream()
                    .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                    .limit(topK)
                    .map(entry -> {
                        String docId = entry.getKey();
                        double score = entry.getValue();
                        Document doc = documentIndex.get(docId);
                        
                        return RetrievalResult.builder()
                                .documentId(docId)
                                .content(doc.getContent())
                                .metadata(doc.getMetadata())
                                .rawScore(score)
                                .rerankScore(score)
                                .passedThreshold(score > 0) // 分数大于0即通过
                                .retrieverType(getType())
                                .chunkIndex((int) doc.getMetadata().getOrDefault("chunk_index", 0))
                                .totalChunks((int) doc.getMetadata().getOrDefault("total_chunks", 1))
                                .build();
                    })
                    .collect(Collectors.toList());
            
            long endTime = System.currentTimeMillis();
            log.info("关键词检索完成，查询: {}, 返回结果数: {}, 耗时: {}ms", 
                    query, results.size(), endTime - startTime);
            
            return results;
            
        } catch (Exception e) {
            log.error("关键词检索失败，查询: {}, 错误: {}", request.getQuery(), e.getMessage(), e);
            throw new RuntimeException("关键词检索失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 构建内存索引
     */
    private synchronized void buildIndex() {
        if (indexBuilt) {
            return;
        }
        
        log.info("开始构建关键词检索索引...");
        
        // 从向量存储获取所有文档（简化：假设文档数量不多）
        // 注意：实际生产环境中应考虑分页或使用专门的文档存储
        List<Document> allDocuments = getAllDocumentsFromVectorStore();
        
        // 建立文档索引
        for (Document doc : allDocuments) {
            String docId = doc.getId();
            documentIndex.put(docId, doc);
            
            // 计算词频
            String content = doc.getContent().toLowerCase();
            String[] terms = content.split("\\s+");
            
            Map<String, Integer> termCount = new HashMap<>();
            for (String term : terms) {
                if (term.length() > 1) { // 忽略单字符词
                    termCount.put(term, termCount.getOrDefault(term, 0) + 1);
                }
            }
            
            // 转换为TF（词频）
            Map<String, Double> tfMap = new HashMap<>();
            for (Map.Entry<String, Integer> entry : termCount.entrySet()) {
                tfMap.put(entry.getKey(), (double) entry.getValue() / terms.length);
            }
            
            termFrequencies.put(docId, tfMap);
        }
        
        // 计算文档频率和逆文档频率
        int totalDocuments = documentIndex.size();
        Map<String, Integer> documentFrequency = new HashMap<>();
        
        for (Map<String, Double> tfMap : termFrequencies.values()) {
            for (String term : tfMap.keySet()) {
                documentFrequency.put(term, documentFrequency.getOrDefault(term, 0) + 1);
            }
        }
        
        // 计算IDF
        for (Map.Entry<String, Integer> entry : documentFrequency.entrySet()) {
            String term = entry.getKey();
            int df = entry.getValue();
            double idf = Math.log((totalDocuments - df + 0.5) / (df + 0.5) + 1.0);
            inverseDocumentFrequencies.put(term, idf);
        }
        
        // 计算平均文档长度
        double totalLength = 0.0;
        for (Document doc : allDocuments) {
            totalLength += doc.getContent().split("\\s+").length;
        }
        averageDocumentLength = totalLength / totalDocuments;
        
        indexBuilt = true;
        log.info("关键词检索索引构建完成，文档数: {}, 平均长度: {}", totalDocuments, averageDocumentLength);
    }
    
    /**
     * 计算BM25分数
     */
    private double calculateBM25Score(String docId, String[] queryTerms) {
        Map<String, Double> tfMap = termFrequencies.get(docId);
        if (tfMap == null) {
            return 0.0;
        }
        
        double score = 0.0;
        Document doc = documentIndex.get(docId);
        int documentLength = doc.getContent().split("\\s+").length;
        
        for (String term : queryTerms) {
            if (inverseDocumentFrequencies.containsKey(term)) {
                double idf = inverseDocumentFrequencies.get(term);
                double tf = tfMap.getOrDefault(term, 0.0);
                
                // BM25计算公式
                double numerator = tf * (bm25K1 + 1);
                double denominator = tf + bm25K1 * (1 - bm25B + bm25B * documentLength / averageDocumentLength);
                
                score += idf * numerator / denominator;
            }
        }
        
        return score;
    }
    
    /**
     * 从向量存储获取所有文档（简化实现）
     */
    private List<Document> getAllDocumentsFromVectorStore() {
        // 这里简化实现：使用一个通用查询获取所有文档
        // 实际生产环境应使用专门的文档存储或分页查询
        try {
            // 使用一个常见的查询词获取一定数量的文档 (Spring AI 1.0.0-M3+ API)
            SearchRequest searchRequest = SearchRequest.query("技术").withTopK(1000);
            return vectorStore.similaritySearch(searchRequest);
        } catch (Exception e) {
            log.warn("从向量存储获取文档失败，返回空列表: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
    
    @Override
    public String getType() {
        return "keyword";
    }
    
    @Override
    public String getName() {
        return "BM25关键词检索器";
    }
    
    @Override
    public boolean isEnabled() {
        return true;
    }
    
    @Override
    public void setEnabled(boolean enabled) {
        // 关键词检索器总是启用
    }
    
    /**
     * 获取关键词检索权重
     */
    public double getWeight() {
        return weight;
    }
}