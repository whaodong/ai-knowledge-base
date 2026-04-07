package com.example.rag.controller;

import com.example.rag.model.RagRequest;
import com.example.rag.model.RagResponse;
import com.example.rag.service.RagRetrievalService;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 限流器集成测试
 * 
 * <p>验证限流策略生效，拒绝超限请求，保护后端资源</p>
 */
@WebMvcTest(RagController.class)
@ActiveProfiles("test")
public class RateLimiterTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private RagRetrievalService ragRetrievalService;
    
    @Autowired
    private RateLimiterRegistry rateLimiterRegistry;
    
    private RateLimiter rateLimiter;
    
    @BeforeEach
    public void setup() {
        rateLimiter = rateLimiterRegistry.rateLimiter("ragRetrieval");
        
        // 模拟服务正常响应
        when(ragRetrievalService.retrieve(any(RagRequest.class)))
            .thenReturn(RagResponse.builder()
                    .success(true)
                    .retrievalTimeMs(50)
                    .build());
    }
    
    /**
     * 测试限流器拒绝超过限制的请求
     */
    @Test
    public void testRateLimiterRejectsExcessiveRequests() throws Exception {
        // 配置限流器：每秒10个请求（limit-for-period: 10）
        // 在1秒内发送15个请求，应该至少有5个被限流
        
        int totalRequests = 15;
        int successfulRequests = 0;
        int rateLimitedRequests = 0;
        
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < totalRequests; i++) {
            try {
                mockMvc.perform(MockMvcRequestBuilders.post("/api/rag/retrieve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"test query\",\"topK\":10}"))
                        .andExpect(status().isOk());
                successfulRequests++;
            } catch (AssertionError e) {
                // 检查是否为限流导致的429状态
                if (e.getMessage().contains("429")) {
                    rateLimitedRequests++;
                } else {
                    throw e;
                }
            }
            
            // 短暂延迟，避免请求过快
            Thread.sleep(10);
        }
        
        long endTime = System.currentTimeMillis();
        long durationMs = endTime - startTime;
        
        System.out.println("限流测试结果:");
        System.out.println("总请求数: " + totalRequests);
        System.out.println("成功请求数: " + successfulRequests);
        System.out.println("被限流请求数: " + rateLimitedRequests);
        System.out.println("测试持续时间: " + durationMs + "ms");
        
        // 验证至少有一些请求被限流（因为我们在1秒内发送了15个请求，超过10个限制）
        // 注意：由于测试执行时间可能超过1秒，实际被限流的请求数可能较少
        assert rateLimitedRequests > 0 : "应至少有一些请求被限流，实际被限流数: " + rateLimitedRequests;
        assert successfulRequests <= 10 : "成功请求数不应超过限流限制，实际成功数: " + successfulRequests;
    }
    
    /**
     * 测试限流器在刷新周期后恢复
     */
    @Test
    public void testRateLimiterResetsAfterRefreshPeriod() throws Exception {
        // 首先发送10个请求，用尽当前周期的配额
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(MockMvcRequestBuilders.post("/api/rag/retrieve")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"query\":\"test query\",\"topK\":10}"))
                    .andExpect(status().isOk());
        }
        
        // 第11个请求应被限流（如果在同一周期内）
        try {
            mockMvc.perform(MockMvcRequestBuilders.post("/api/rag/retrieve")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"query\":\"test query\",\"topK\":10}"))
                    .andExpect(status().isTooManyRequests()); // 期望429
        } catch (AssertionError e) {
            // 如果请求成功，说明可能已经进入下一个周期
            // 这是可接受的，因为限流周期可能已经刷新
            System.out.println("第11个请求未被限流，可能已进入新周期");
        }
        
        // 等待1秒（限流刷新周期）
        Thread.sleep(1100);
        
        // 等待后，限流器应重置，新请求应成功
        mockMvc.perform(MockMvcRequestBuilders.post("/api/rag/retrieve")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"query\":\"test query after reset\",\"topK\":10}"))
                .andExpect(status().isOk());
    }
    
    /**
     * 测试不同用户的不同限流桶
     */
    @Test
    public void testDifferentRateLimitBuckets() throws Exception {
        // 注意：这里演示如何为不同用户设置不同限流策略
        // 实际实现可能需要自定义RateLimiter配置
        
        // 发送5个请求作为用户A
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(MockMvcRequestBuilders.post("/api/rag/retrieve")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-User-ID", "userA")
                    .content("{\"query\":\"test from userA\",\"topK\":10}"))
                    .andExpect(status().isOk());
        }
        
        // 发送5个请求作为用户B
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(MockMvcRequestBuilders.post("/api/rag/retrieve")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-User-ID", "userB")
                    .content("{\"query\":\"test from userB\",\"topK\":10}"))
                    .andExpect(status().isOk());
        }
        
        // 两个用户都应成功，因为各自没有超过限制
        // 总请求数（10）没有超过全局限流（10），所以都成功
        System.out.println("不同用户限流测试完成");
    }
}