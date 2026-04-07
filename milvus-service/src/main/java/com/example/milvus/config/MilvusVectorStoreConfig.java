package com.example.milvus.config;

import io.milvus.client.MilvusClient;
import io.milvus.client.MilvusServiceClient;
import io.milvus.param.ConnectParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Milvus向量存储配置类
 * 
 * 注意：MilvusClient 和 VectorStore 由 Spring AI 自动配置
 * 此配置仅作为备用，当 Spring AI 自动配置未生效时使用
 * 
 * 配置项（application.yml）：
 * spring.ai.vectorstore.milvus.client.host
 * spring.ai.vectorstore.milvus.client.port
 * spring.ai.vectorstore.milvus.database-name
 * spring.ai.vectorstore.milvus.collection-name
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

    @Value("${milvus.timeout.connect:10000}")
    private long connectTimeout;
    
    @Value("${milvus.timeout.keep-alive:55}")
    private long keepAliveTime;
    
    @Value("${milvus.timeout.keep-alive-timeout:20}")
    private long keepAliveTimeout;
    
    @Value("${milvus.timeout.idle:24}")
    private long idleTimeout;

    /**
     * Milvus客户端Bean（备用配置）
     * 仅在 Spring AI 自动配置未创建时生效
     */
    @Bean
    @ConditionalOnMissingBean
    public MilvusClient milvusClient() {
        log.info("初始化Milvus客户端（自定义配置）: host={}, port={}, database={}", host, port, database);
        
        ConnectParam connectParam = ConnectParam.newBuilder()
                .withHost(host)
                .withPort(port)
                .withDatabaseName(database)
                .withAuthorization(username, password)
                .withConnectTimeout(connectTimeout, TimeUnit.MILLISECONDS)
                .withKeepAliveTime(keepAliveTime, TimeUnit.SECONDS)
                .withKeepAliveTimeout(keepAliveTimeout, TimeUnit.SECONDS)
                .withIdleTimeout(idleTimeout, TimeUnit.HOURS)
                .build();
        
        return new MilvusServiceClient(connectParam);
    }
}
