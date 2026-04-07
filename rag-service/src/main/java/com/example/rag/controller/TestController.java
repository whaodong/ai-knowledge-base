package com.example.rag.controller;

import com.example.rag.client.EmbeddingServiceClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 服务间调用测试控制器
 * 
 * <p>演示RAG服务通过Feign客户端调用Embedding服务，验证服务注册发现功能。</p>
 * 
 * @author AI Knowledge Base Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/test")
public class TestController {

    @Value("${spring.application.name}")
    private String appName;
    
    @Value("${server.port}")
    private String port;
    
    @Autowired
    private EmbeddingServiceClient embeddingServiceClient;
    
    /**
     * 测试Feign客户端调用
     * 
     * @return 调用结果
     */
    @GetMapping("/feign-call")
    public Map<String, Object> testFeignCall() {
        Map<String, Object> result = new HashMap<>();
        result.put("caller", appName + ":" + port);
        result.put("timestamp", System.currentTimeMillis());
        
        try {
            // 调用Embedding服务的健康检查接口
            Map<String, Object> healthResponse = embeddingServiceClient.getHealth();
            result.put("healthResponse", healthResponse);
            
            // 调用Embedding服务的信息接口
            Map<String, Object> infoResponse = embeddingServiceClient.getServiceInfo();
            result.put("infoResponse", infoResponse);
            
            // 调用向量生成接口（演示）
            Map<String, Object> embeddingResponse = embeddingServiceClient.generateEmbedding("测试RAG服务调用");
            result.put("embeddingResponse", embeddingResponse);
            
            result.put("status", "SUCCESS");
            result.put("message", "Feign客户端调用成功");
        } catch (Exception e) {
            result.put("status", "FAILED");
            result.put("message", "Feign客户端调用失败: " + e.getMessage());
            result.put("error", e.getClass().getName());
        }
        
        return result;
    }
    
    /**
     * 测试负载均衡（需要启动多个Embedding服务实例）
     * 
     * <p>通过多次调用观察是否轮询到不同的实例。</p>
     * 
     * @param count 调用次数
     * @return 调用结果统计
     */
    @GetMapping("/load-balance-test")
    public Map<String, Object> testLoadBalancing(@RequestParam(defaultValue = "5") int count) {
        Map<String, Object> result = new HashMap<>();
        result.put("testName", "负载均衡测试");
        result.put("caller", appName + ":" + port);
        result.put("targetService", "embedding-service");
        result.put("callCount", count);
        
        Map<String, Integer> instanceCallCount = new HashMap<>();
        
        for (int i = 0; i < count; i++) {
            try {
                Map<String, Object> healthResponse = embeddingServiceClient.getHealth();
                String instanceId = (String) healthResponse.get("service") + ":" + healthResponse.get("port");
                instanceCallCount.put(instanceId, instanceCallCount.getOrDefault(instanceId, 0) + 1);
                
                // 添加延迟以观察轮询
                Thread.sleep(100);
            } catch (Exception e) {
                // 忽略错误继续测试
            }
        }
        
        result.put("instanceCallDistribution", instanceCallCount);
        result.put("uniqueInstancesCalled", instanceCallCount.size());
        result.put("timestamp", System.currentTimeMillis());
        
        return result;
    }
    
    /**
     * RAG服务健康检查
     * 
     * @return 健康状态
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
}