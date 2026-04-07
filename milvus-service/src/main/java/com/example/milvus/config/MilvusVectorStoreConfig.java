package com.example.milvus.config;

import io.milvus.client.MilvusClient;
import io.milvus.client.MilvusServiceClient;
import io.milvus.param.ConnectParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Milvus向量存储配置类
 * 提供Milvus客户端连接
 * 
 * 注意：VectorStore Bean 由 Spring AI 自动配置
 * 只需在 application.yml 中配置 spring.ai.vectorstore.milvus.* 即可
 */
@Configuration
@Slf4j
public class MilvusVectorStoreConfig {

    @Value("${milvus.host:localhost}")
    private String host;

    @Value("${milvus.port:19530}")
    private int port;

    @Value("${milvus.database:default}")
    private String database;

    @Value("${milvus.username:root}")
    private String username;

    @Value("${milvus.password:milvus}")
    private String password;

    /**
     * Milvus客户端Bean
     * 用于直接操作Milvus（如集合管理、统计等）
     */
    @Bean
    public MilvusClient milvusClient() {
        log.info("初始化Milvus客户端: host={}, port={}, database={}", host, port, database);
        
        return new MilvusServiceClient(ConnectParam.newBuilder()
                .withHost(host)
                .withPort(port)
                .withDatabaseName(database)
                .withAuthorization(username, password)
                .build());
    }
    
    // VectorStore Bean 由 Spring AI 自动配置创建
    // 配置项：
    // spring.ai.vectorstore.milvus.client.host
    // spring.ai.vectorstore.milvus.client.port
    // spring.ai.vectorstore.milvus.database-name
    // spring.ai.vectorstore.milvus.collection-name
    // spring.ai.vectorstore.milvus.embedding-dimension
    // spring.ai.vectorstore.milvus.index-type
    // spring.ai.vectorstore.milvus.metric-type
}
