package com.example.embedding.service;

import com.example.embedding.dto.*;

import java.util.List;

/**
 * 向量化服务接口
 */
public interface EmbeddingService {

    /**
     * 文本向量化
     */
    EmbeddingResponse embedText(EmbeddingRequest request);

    /**
     * 批量文本向量化
     */
    EmbeddingBatchResponse batchEmbedTexts(EmbeddingBatchRequest request);

    /**
     * 查询向量化任务状态
     */
    EmbeddingResponse getTaskStatus(String taskId);
}
