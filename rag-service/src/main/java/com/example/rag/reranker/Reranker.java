package com.example.rag.reranker;

import com.example.rag.model.RagRequest;
import com.example.rag.model.RetrievalResult;

import java.util.List;

/**
 * 重排序器接口
 * 对检索结果进行重新排序，提升结果相关性
 */
public interface Reranker {
    
    /**
     * 对检索结果进行重排序
     * @param request 原始请求
     * @param results 检索结果列表
     * @return 重排序后的结果
     */
    List<RetrievalResult> rerank(RagRequest request, List<RetrievalResult> results);
    
    /**
     * 重排序器名称
     * @return 名称
     */
    String getName();
    
    /**
     * 是否启用
     * @return 启用状态
     */
    boolean isEnabled();
}