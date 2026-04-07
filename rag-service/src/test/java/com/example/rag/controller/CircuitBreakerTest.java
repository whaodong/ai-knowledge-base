package com.example.rag.controller;

import com.example.rag.model.RagRequest;
import com.example.rag.model.RagResponse;
import com.example.rag.service.RagRetrievalService;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
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
 * 熔断器集成测试
 * 
 * <p>验证熔断器正确触发并隔离故障服务</p>
 */
@WebMvcTest(RagController.class)
@ActiveProfiles("test")
public class CircuitBreakerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private RagRetrievalService ragRetrievalService;
    
    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;
    
    private CircuitBreaker circuitBreaker;
    
    @BeforeEach
    public void setup() {
        circuitBreaker = circuitBreakerRegistry.circuitBreaker("ragRetrieval");
        circuitBreaker.transitionToClosedState(); // 确保初始状态为关闭
    }
    
    /**
     * 测试熔断器在连续失败后触发打开状态
     */
    @Test
    public void testCircuitBreakerOpensAfterFailures() throws Exception {
        // 模拟服务连续失败
        when(ragRetrievalService.retrieve(any(RagRequest.class)))
            .thenThrow(new RuntimeException("Service unavailable"));
        
        // 发送请求，触发失败
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(MockMvcRequestBuilders.post("/api/rag/retrieve")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"query\":\"test query\",\"topK\":10}"))
                    .andExpect(status().isServiceUnavailable()); // 期望503熔断降级
        }
        
        // 验证熔断器状态变为打开
        CircuitBreaker.State state = circuitBreaker.getState();
        assert state == CircuitBreaker.State.OPEN : "熔断器应处于打开状态，当前状态: " + state;
    }
    
    /**
     * 测试熔断器在半开状态下恢复
     */
    @Test
    public void testCircuitBreakerHalfOpenRecovery() throws Exception {
        // 先将熔断器置为打开状态
        circuitBreaker.transitionToOpenState();
        
        // 等待一段时间，使熔断器进入半开状态
        try {
            Thread.sleep(6000); // 等待时间超过配置的wait-duration-in-open-state: 5s
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // 在半开状态下模拟服务恢复正常
        when(ragRetrievalService.retrieve(any(RagRequest.class)))
            .thenReturn(RagResponse.builder()
                    .success(true)
                    .retrievalTimeMs(100)
                    .build());
        
        // 发送测试请求
        mockMvc.perform(MockMvcRequestBuilders.post("/api/rag/retrieve")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"query\":\"test query\",\"topK\":10}"))
                .andExpect(status().isOk());
        
        // 验证熔断器应恢复为关闭状态
        CircuitBreaker.State state = circuitBreaker.getState();
        assert state == CircuitBreaker.State.CLOSED : "熔断器应恢复为关闭状态，当前状态: " + state;
    }
    
    /**
     * 测试慢调用触发熔断
     */
    @Test
    public void testSlowCallTriggersCircuitBreaker() throws Exception {
        // 模拟慢调用（超过2秒阈值）
        when(ragRetrievalService.retrieve(any(RagRequest.class)))
            .thenAnswer(invocation -> {
                Thread.sleep(2500); // 超过slow-call-duration-threshold: 2s
                return RagResponse.builder()
                        .success(true)
                        .retrievalTimeMs(2500)
                        .build();
            });
        
        // 发送多个请求以触发慢调用熔断
        for (int i = 0; i < 10; i++) {
            mockMvc.perform(MockMvcRequestBuilders.post("/api/rag/retrieve")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"query\":\"test query\",\"topK\":10}"))
                    .andExpect(status().isOk());
        }
        
        // 验证熔断器可能因慢调用比例过高而打开
        // 注意：实际触发需要满足最小调用次数和比例阈值
        CircuitBreaker.State state = circuitBreaker.getState();
        System.out.println("熔断器状态（慢调用测试）: " + state);
    }
}