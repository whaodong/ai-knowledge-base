package com.example.embedding;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 向量生成服务主启动类
 * 
 * <p>提供文档向量化服务，集成OpenAI Embedding API和Redis缓存。</p>
 * 
 * @author AI Knowledge Base Team
 * @since 1.0.0
 */
@SpringBootApplication
@EnableDiscoveryClient
public class EmbeddingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(EmbeddingServiceApplication.class, args);
    }
}