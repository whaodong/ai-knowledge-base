package com.example.embedding.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 向量生成服务控制器
 * 
 * <p>提供向量生成相关的REST接口。</p>
 * 
 * @author AI Knowledge Base Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/embedding")
public class EmbeddingController {

    @Value("${spring.application.name}")
    private String appName;
    
    @Value("${server.port}")
    private String port;
    
    /**
     * 健康检查接口
     * 
     * @return 服务状态信息
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "UP");
        result.put("service", appName);
        result.put("port", port);
        result.put("timestamp", System.currentTimeMillis());
        return result;
    }
    
    /**
     * 测试向量生成接口（简化版）
     * 
     * <p>实际企业级实现会调用OpenAI Embedding API，这里返回模拟数据。</p>
     * 
     * @param text 待向量化的文本
     * @return 向量生成结果
     */
    @GetMapping("/generate")
    public Map<String, Object> generateEmbedding(@RequestParam String text) {
        Map<String, Object> result = new HashMap<>();
        result.put("text", text);
        result.put("embedding", "模拟向量数据（实际为1536维浮点数数组）");
        result.put("dimension", 1536);
        result.put("model", "text-embedding-3-small");
        result.put("service", appName);
        result.put("timestamp", System.currentTimeMillis());
        return result;
    }
    
    /**
     * 服务间调用测试接口
     * 
     * <p>用于验证Feign客户端调用，返回服务信息。</p>
     * 
     * @return 服务信息
     */
    @GetMapping("/info")
    public Map<String, Object> getServiceInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("serviceName", appName);
        info.put("instanceId", appName + ":" + port);
        info.put("status", "ACTIVE");
        info.put("endpoints", new String[] {"/api/embedding/health", "/api/embedding/generate", "/api/embedding/info"});
        info.put("description", "向量生成服务，提供文档向量化能力");
        info.put("timestamp", System.currentTimeMillis());
        return info;
    }
}