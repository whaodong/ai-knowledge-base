package com.example.rag.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * RAG服务配置类
 * 启用缓存支持
 * 
 * 注意：VectorStore 和 EmbeddingModel 由 Spring AI 自动配置提供
 */
@Configuration
@EnableCaching
public class RagConfig {
    // VectorStore 和 EmbeddingModel 由 Spring AI 自动配置提供
    // 无需手动定义 Bean
}