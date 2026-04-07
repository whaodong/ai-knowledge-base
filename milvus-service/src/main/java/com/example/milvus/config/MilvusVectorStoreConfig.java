package com.example.milvus.config;

import io.milvus.client.MilvusClient;
import io.milvus.client.MilvusServiceClient;
import io.milvus.param.ConnectParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Milvus向量存储配置类
 * 提供Milvus客户端连接，优化高并发场景
 * 
 * 注意：VectorStore Bean 由 Spring AI 自动配置
 * 只需在 application.yml 中配置 spring.ai.vectorstore.milvus.* 即可
 * 
 * 优化配置：
 * - GrpcChannelPoolSize: 连接池大小
 * - ConnectTimeout: 连接超时
 * - KeepAliveTime: 保活时间
 * - KeepAliveTimeout: 保活超时
 * - IdleTimeout: 空闲超时
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

    // 连接池配置
    @Value("${milvus.pool.channel-pool-size:10}")
    private int channelPoolSize;
    
    @Value("${milvus.pool.max-idle-per-channel:20}")
    private int maxIdlePerChannel;
    
    @Value("${milvus.pool.max-total:100}")
    private int maxTotal;
    
    // 超时配置
    @Value("${milvus.timeout.connect:10000}")
    private long connectTimeout;
    
    @Value("${milvus.timeout.keep-alive:55}")
    private long keepAliveTime;
    
    @Value("${milvus.timeout.keep-alive-timeout:20}")
    private long keepAliveTimeout;
    
    @Value("${milvus.timeout.idle:24}")
    private long idleTimeout;

    /**
     * Milvus客户端Bean（优化版）
     * 用于直接操作Milvus（如集合管理、统计等）
     * 
     * <p>性能优化：</p>
     * <ul>
     *   <li>连接池：避免频繁创建连接，提高并发性能</li>
     *   <li>保活机制：防止连接断开</li>
     *   <li>超时控制：避免长时间阻塞</li>
     * </ul>
     */
    @Bean
    public MilvusClient milvusClient() {
        log.info("初始化Milvus客户端: host={}, port={}, database={}", host, port, database);
        log.info("连接池配置: channelPoolSize={}, maxIdlePerChannel={}, maxTotal={}", 
                channelPoolSize, maxIdlePerChannel, maxTotal);
        log.info("超时配置: connectTimeout={}ms, keepAliveTime={}s, idleTimeout={}h", 
                connectTimeout, keepAliveTime, idleTimeout);
        
        ConnectParam connectParam = ConnectParam.newBuilder()
                .withHost(host)
                .withPort(port)
                .withDatabaseName(database)
                .withAuthorization(username, password)
                // 连接池配置
                .withGrpcChannelPoolSize(channelPoolSize)
                .withMaxIdlePerChannel(maxIdlePerChannel)
                .withMaxTotal(maxTotal)
                // 超时配置
                .withConnectTimeout(connectTimeout, TimeUnit.MILLISECONDS)
                .withKeepAliveTime(keepAliveTime, TimeUnit.SECONDS)
                .withKeepAliveTimeout(keepAliveTimeout, TimeUnit.SECONDS)
                .withIdleTimeout(idleTimeout, TimeUnit.HOURS)
                .build();
        
        return new MilvusServiceClient(connectParam);
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
