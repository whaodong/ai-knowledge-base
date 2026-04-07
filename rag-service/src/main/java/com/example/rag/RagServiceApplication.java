package com.example.rag;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.ComponentScan;

/**
 * 检索增强生成服务主启动类
 * 
 * <p>提供RAG检索服务，集成Milvus向量数据库和LangChain4j。</p>
 * 
 * @author AI Knowledge Base Team
 * @since 1.0.0
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.example.rag.client")
@ComponentScan(basePackages = {"com.example.rag", "com.example.common"})
public class RagServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(RagServiceApplication.class, args);
    }
}