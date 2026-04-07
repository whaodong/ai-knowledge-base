package com.example.milvus.service;

import org.springframework.ai.document.Document;

import java.util.List;
import java.util.Map;

/**
 * 向量搜索服务接口
 * 提供向量存储与检索的核心功能
 */
public interface VectorSearchService {

    /**
     * 插入单个文档
     * @param document 文档对象
     * @return 文档ID
     */
    String insertDocument(Document document);

    /**
     * 批量插入文档
     * @param documents 文档列表
     * @return 成功插入的数量
     */
    int batchInsertDocuments(List<Document> documents);

    /**
     * 相似度搜索
     * @param queryVector 查询向量
     * @param topK 返回结果数量
     * @return 相似文档列表
     */
    List<Document> similaritySearch(List<Float> queryVector, int topK);

    /**
     * 带过滤条件的相似度搜索
     * @param queryVector 查询向量
     * @param topK 返回结果数量
     * @param filter 过滤条件（metadata键值对）
     * @return 相似文档列表
     */
    List<Document> similaritySearchWithFilter(
            List<Float> queryVector, 
            int topK, 
            Map<String, Object> filter
    );

    /**
     * 带相似度阈值的搜索
     * @param queryVector 查询向量
     * @param topK 返回结果数量
     * @param similarityThreshold 相似度阈值（0-1）
     * @return 相似文档列表
     */
    List<Document> similaritySearchWithThreshold(
            List<Float> queryVector, 
            int topK, 
            double similarityThreshold
    );

    /**
     * 删除文档
     * @param documentId 文档ID
     * @return 是否删除成功
     */
    boolean deleteDocument(String documentId);

    /**
     * 批量删除文档
     * @param documentIds 文档ID列表
     * @return 成功删除的数量
     */
    int batchDeleteDocuments(List<String> documentIds);

    /**
     * 根据metadata过滤删除文档
     * @param filter metadata过滤条件
     * @return 删除的数量
     */
    int deleteByFilter(Map<String, Object> filter);

    /**
     * 获取向量存储中文档数量
     * @return 文档总数
     */
    long count();

    /**
     * 健康检查
     * @return 是否健康
     */
    boolean healthCheck();

    /**
     * 清空集合（谨慎使用）
     * @return 是否成功
     */
    boolean clearCollection();
}