package com.example.document;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 文档管理服务主启动类
 * 
 * <p>提供文档上传、解析、存储服务。</p>
 * 
 * @author AI Knowledge Base Team
 * @since 1.0.0
 */
@SpringBootApplication
@EnableDiscoveryClient
public class DocumentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DocumentServiceApplication.class, args);
    }
}