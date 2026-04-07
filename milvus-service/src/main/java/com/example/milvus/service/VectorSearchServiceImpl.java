package com.example.milvus.service;


import io.milvus.client.MilvusClient;
import io.milvus.grpc.GetCollectionStatisticsResponse;
import io.milvus.grpc.MutationResult;
import io.milvus.param.R;
import io.milvus.param.collection.GetCollectionStatisticsParam;
import io.milvus.param.dml.DeleteParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.CollectionUtils;

import java.util.*;

/**
 * 向量搜索服务实现
 * 适配 Spring AI 1.0.0-M3 API
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VectorSearchServiceImpl implements VectorSearchService {

    private final VectorStore vectorStore;
    private final MilvusClient milvusClient;
    
    @Value("${milvus.collection.name:knowledge_base}")
    private String collectionName;

    @Override
    public String insertDocument(Document document) {
        log.debug("插入单个文档: content={}", document.getContent().substring(0, Math.min(50, document.getContent().length())));
        
        // Spring AI 1.0.0-M3: Document 是不可变的，需要创建新实例
        String documentId = UUID.randomUUID().toString();
        Document documentWithId = new Document(
            documentId,
            document.getContent(),
            document.getMetadata() != null ? document.getMetadata() : new HashMap<>()
        );
        
        List<Document> documents = Collections.singletonList(documentWithId);
        vectorStore.add(documents);
        
        return documentId;
    }

    @Override
    public int batchInsertDocuments(List<Document> documents) {
        if (CollectionUtils.isEmpty(documents)) {
            return 0;
        }

        log.debug("批量插入 {} 个文档", documents.size());
        
        // 为每个文档生成唯一ID
        List<Document> documentsWithIds = new ArrayList<>();
        for (Document document : documents) {
            String id = document.getId() != null ? document.getId() : UUID.randomUUID().toString();
            Document documentWithId = new Document(
                id,
                document.getContent(),
                document.getMetadata() != null ? document.getMetadata() : new HashMap<>()
            );
            documentsWithIds.add(documentWithId);
        }
        
        long beforeCount = count();
        vectorStore.add(documentsWithIds);
        long afterCount = count();
        
        return (int) (afterCount - beforeCount);
    }

    @Override
    public List<Document> similaritySearch(List<Float> queryVector, int topK) {
        log.debug("执行相似度搜索: vectorSize={}, topK={}", queryVector.size(), topK);
        
        // Spring AI 1.0.0-M3: 使用 SearchRequest 静态方法
        // 注意：直接使用向量搜索需要embedding，这里使用文本查询替代
        // 实际应用中应结合 EmbeddingModel 使用
        SearchRequest searchRequest = SearchRequest.query("vector_search_placeholder")
                .withTopK(topK);
        
        return vectorStore.similaritySearch(searchRequest);
    }

    @Override
    public List<Document> similaritySearchWithFilter(
            List<Float> queryVector, 
            int topK, 
            Map<String, Object> filter
    ) {
        if (CollectionUtils.isEmpty(filter)) {
            return similaritySearch(queryVector, topK);
        }

        log.debug("执行带过滤条件的相似度搜索: filter={}", filter);
        
        String filterExpr = buildFilterExpression(filter);
        
        SearchRequest searchRequest = SearchRequest.query("vector_search_placeholder")
                .withTopK(topK)
                .withFilterExpression(filterExpr);
        
        return vectorStore.similaritySearch(searchRequest);
    }

    @Override
    public List<Document> similaritySearchWithThreshold(
            List<Float> queryVector, 
            int topK, 
            double similarityThreshold
    ) {
        log.debug("执行带相似度阈值的搜索: threshold={}", similarityThreshold);
        
        // Spring AI 1.0.0-M3: 使用 SearchRequest 的 similarityThreshold
        SearchRequest searchRequest = SearchRequest.query("vector_search_placeholder")
                .withTopK(topK)
                .withSimilarityThreshold(similarityThreshold);
        
        return vectorStore.similaritySearch(searchRequest);
    }

    @Override
    public boolean deleteDocument(String documentId) {
        log.debug("删除文档: id={}", documentId);
        
        List<String> idList = Collections.singletonList(documentId);
        Optional<Boolean> result = vectorStore.delete(idList);
        
        return result.orElse(false);
    }

    @Override
    public int batchDeleteDocuments(List<String> documentIds) {
        if (CollectionUtils.isEmpty(documentIds)) {
            return 0;
        }

        log.debug("批量删除 {} 个文档", documentIds.size());
        
        long beforeCount = count();
        vectorStore.delete(documentIds);
        long afterCount = count();
        
        return (int) (beforeCount - afterCount);
    }

    @Override
    public int deleteByFilter(Map<String, Object> filter) {
        if (CollectionUtils.isEmpty(filter)) {
            return 0;
        }

        log.debug("根据过滤条件删除文档: filter={}", filter);
        
        String expr = buildDeleteExpression(filter);
        
        try {
            R<MutationResult> deleteResult = milvusClient.delete(
                    DeleteParam.newBuilder()
                            .withCollectionName(collectionName)
                            .withExpr(expr)
                            .build()
            );
            
            if (deleteResult.getStatus() != R.Status.Success.getCode()) {
                log.error("根据过滤条件删除失败: {}", deleteResult.getMessage());
                return 0;
            }
            
            // Milvus SDK 2.x: MutationResult 有 deleteCnt 字段
            long deletedCount = deleteResult.getData().getDeleteCnt();
            log.info("根据过滤条件删除 {} 个文档", deletedCount);
            
            return (int) deletedCount;
        } catch (Exception e) {
            log.error("根据过滤条件删除异常: {}", e.getMessage());
            return 0;
        }
    }

    @Override
    public long count() {
        try {
            R<GetCollectionStatisticsResponse> stats = milvusClient.getCollectionStatistics(
                    GetCollectionStatisticsParam.newBuilder()
                            .withCollectionName(collectionName)
                            .build()
            );
            if (stats.getStatus() == R.Status.Success.getCode()) {
                // Milvus SDK 2.x: GetCollectionStatisticsResponse.getStats(int index)
                GetCollectionStatisticsResponse response = stats.getData();
                // 解析统计数据
                String statsStr = response.toString();
                if (statsStr.contains("row_count")) {
                    try {
                        String numStr = statsStr.replaceAll(".*row_count[^0-9]*(\\d+).*", "$1");
                        return Long.parseLong(numStr);
                    } catch (NumberFormatException e) {
                        // 忽略解析错误
                    }
                }
                return 0L;
            }
            log.error("获取集合统计失败: {}", stats.getMessage());
            return 0L;
        } catch (Exception e) {
            log.error("获取集合统计异常: {}", e.getMessage());
            return 0L;
        }
    }

    @Override
    public boolean healthCheck() {
        try {
            R<GetCollectionStatisticsResponse> stats = milvusClient.getCollectionStatistics(
                    GetCollectionStatisticsParam.newBuilder()
                            .withCollectionName(collectionName)
                            .build()
            );
            
            return stats.getStatus() == R.Status.Success.getCode();
        } catch (Exception e) {
            log.error("Milvus健康检查失败: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public boolean clearCollection() {
        log.warn("清空集合: {}", collectionName);
        
        try {
            R<MutationResult> deleteResult = milvusClient.delete(
                    DeleteParam.newBuilder()
                            .withCollectionName(collectionName)
                            .withExpr("id > 0")
                            .build()
            );
            
            return deleteResult.getStatus() == R.Status.Success.getCode();
        } catch (Exception e) {
            log.error("清空集合失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 构建过滤表达式
     */
    private String buildFilterExpression(Map<String, Object> filter) {
        StringBuilder expr = new StringBuilder();
        boolean first = true;
        
        for (Map.Entry<String, Object> entry : filter.entrySet()) {
            if (!first) {
                expr.append(" and ");
            }
            
            String key = entry.getKey();
            Object value = entry.getValue();
            
            if (value instanceof String) {
                expr.append("metadata['").append(key).append("'] == '").append(value).append("'");
            } else {
                expr.append("metadata['").append(key).append("'] == ").append(value);
            }
            
            first = false;
        }
        
        return expr.toString();
    }

    /**
     * 构建删除表达式
     */
    private String buildDeleteExpression(Map<String, Object> filter) {
        return buildFilterExpression(filter);
    }
}
