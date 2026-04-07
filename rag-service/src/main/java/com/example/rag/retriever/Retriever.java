package com.example.rag.retriever;

import com.example.rag.model.RagRequest;
import com.example.rag.model.RetrievalResult;

import java.util.List;

/**
 * 检索器接口
 * 定义所有检索器必须实现的方法
 */
public interface Retriever {
    
    /**
     * 检索相关文档
     * @param request RAG请求
     * @return 检索结果列表
     */
    List<RetrievalResult> retrieve(RagRequest request);
    
    /**
     * 检索器类型
     * @return 类型标识
     */
    String getType();
    
    /**
     * 检索器名称
     * @return 名称
     */
    String getName();
    
    /**
     * 是否启用
     * @return 启用状态
     */
    boolean isEnabled();
    
    /**
     * 设置启用状态
     * @param enabled 启用状态
     */
    void setEnabled(boolean enabled);
}