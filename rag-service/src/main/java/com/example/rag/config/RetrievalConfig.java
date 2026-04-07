package com.example.rag.config;

import com.example.rag.retriever.KeywordRetriever;
import com.example.rag.retriever.Retriever;
import com.example.rag.retriever.VectorRetriever;
import com.example.rag.reranker.CrossEncoderReranker;
import com.example.rag.reranker.Reranker;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * 检索服务配置
 * 配置所有检索器、重排序器等组件
 */
@Configuration
public class RetrievalConfig {
    
    /**
     * 向量检索器Bean
     */
    @Bean
    public VectorRetriever vectorRetriever() {
        return new VectorRetriever(null); // VectorStore将在构造函数中注入
    }
    
    /**
     * 关键词检索器Bean
     */
    @Bean
    public KeywordRetriever keywordRetriever() {
        return new KeywordRetriever(null); // VectorStore将在构造函数中注入
    }
    
    /**
     * 检索器列表
     */
    @Bean
    public List<Retriever> retrievers(VectorRetriever vectorRetriever, KeywordRetriever keywordRetriever) {
        List<Retriever> retrieverList = new ArrayList<>();
        retrieverList.add(vectorRetriever);
        retrieverList.add(keywordRetriever);
        return retrieverList;
    }
    
    /**
     * 重排序器Bean
     */
    @Bean
    public CrossEncoderReranker crossEncoderReranker() {
        return new CrossEncoderReranker(null); // EmbeddingModel将在构造函数中注入
    }
    
    /**
     * 重排序器列表
     */
    @Bean
    public List<Reranker> rerankers(CrossEncoderReranker crossEncoderReranker) {
        List<Reranker> rerankerList = new ArrayList<>();
        rerankerList.add(crossEncoderReranker);
        return rerankerList;
    }
}