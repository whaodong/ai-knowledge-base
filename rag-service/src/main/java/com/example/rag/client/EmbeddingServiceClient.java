package com.example.rag.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

/**
 * Embedding服务Feign客户端
 * 
 * <p>用于从RAG服务调用Embedding服务的REST接口，演示服务间通信。</p>
 * 
 * <p>企业级配置说明：</p>
 * <ul>
 *   <li>使用服务名（embedding-service）而非IP地址进行调用</li>
 *   <li>集成Ribbon负载均衡，支持多实例轮询</li>
 *   <li>支持熔断降级（需额外配置Hystrix或Resilience4j）</li>
 *   <li>支持请求超时和重试配置</li>
 * </ul>
 * 
 * @author AI Knowledge Base Team
 * @since 1.0.0
 */
@FeignClient(name = "embedding-service", path = "/api/v1/embeddings")
public interface EmbeddingServiceClient {

    /**
     * 获取Embedding服务健康状态
     * 
     * @return 健康状态信息
     */
    @GetMapping("/health")
    Map<String, Object> getHealth();
    
    /**
     * 获取Embedding服务信息
     * 
     * @return 服务信息
     */
    @GetMapping("/info")
    Map<String, Object> getServiceInfo();
    
    /**
     * 生成文本向量
     * 
     * @param text 待向量化的文本
     * @return 向量生成结果
     */
    @GetMapping("/generate")
    Map<String, Object> generateEmbedding(@RequestParam("text") String text);
}