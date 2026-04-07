package com.example.milvus;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Milvus向量数据库服务应用
 * 提供向量存储与检索能力，集成Spring AI VectorStore抽象
 */
@SpringBootApplication
@EnableDiscoveryClient
public class MilvusServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MilvusServiceApplication.class, args);
    }
}