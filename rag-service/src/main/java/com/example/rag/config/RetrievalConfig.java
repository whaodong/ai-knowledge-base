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
 * 
 * <p>配置检索器和重排序器的组合列表。</p>
 * 
 * <p>注意：VectorRetriever、KeywordRetriever、CrossEncoderReranker 
 * 已通过 @Component 注解自动注册为 Bean，本类仅用于组合这些 Bean。</p>
 */
@Configuration
public class RetrievalConfig {
    
    /**
     * 检索器列表
     * 注入已由 @Component 注册的 VectorRetriever 和 KeywordRetriever
     */
    @Bean
    public List<Retriever> retrievers(VectorRetriever vectorRetriever, KeywordRetriever keywordRetriever) {
        List<Retriever> retrieverList = new ArrayList<>();
        retrieverList.add(vectorRetriever);
        retrieverList.add(keywordRetriever);
        return retrieverList;
    }
    
    /**
     * 重排序器列表
     * 注入已由 @Component 注册的 CrossEncoderReranker
     */
    @Bean
    public List<Reranker> rerankers(CrossEncoderReranker crossEncoderReranker) {
        List<Reranker> rerankerList = new ArrayList<>();
        rerankerList.add(crossEncoderReranker);
        return rerankerList;
    }
}
