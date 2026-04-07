package com.example.rag.controller;

import com.example.rag.model.RagRequest;
import com.example.rag.model.RagResponse;
import com.example.rag.service.RagRetrievalService;
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
 * 分布式追踪集成测试
 * 
 * <p>验证调用链路完整可追踪，trace ID正确传递</p>
 */
@WebMvcTest(RagController.class)
@ActiveProfiles("test")
public class TracingTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private RagRetrievalService ragRetrievalService;
    
    /**
     * 测试请求生成trace ID
     */
    @Test
    public void testRequestGeneratesTraceId() throws Exception {
        // 模拟服务正常响应
        when(ragRetrievalService.retrieve(any(RagRequest.class)))
            .thenReturn(RagResponse.builder()
                    .success(true)
                    .retrievalTimeMs(100)
                    .build());
        
        // 发送请求
        mockMvc.perform(MockMvcRequestBuilders.post("/api/rag/retrieve")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"query\":\"test tracing\",\"topK\":10}"))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    // 验证响应头中包含trace ID（Sleuth默认添加）
                    String traceIdHeader = result.getResponse().getHeader("X-B3-TraceId");
                    assert traceIdHeader != null : "响应头应包含X-B3-TraceId";
                    assert !traceIdHeader.isEmpty() : "Trace ID不应为空";
                    
                    System.out.println("生成的Trace ID: " + traceIdHeader);
                });
    }
    
    /**
     * 测试跨服务调用trace ID传递
     */
    @Test
    public void testTraceIdPropagationAcrossServices() throws Exception {
        // 模拟服务调用链
        when(ragRetrievalService.retrieve(any(RagRequest.class)))
            .thenAnswer(invocation -> {
                // 在实际场景中，这里可能会调用其他微服务
                // 验证当前线程的trace ID存在
                String traceId = org.slf4j.MDC.get("traceId");
                assert traceId != null : "MDC中应包含traceId";
                
                return RagResponse.builder()
                        .success(true)
                        .retrievalTimeMs(150)
                        .build();
            });
        
        // 发送请求
        mockMvc.perform(MockMvcRequestBuilders.post("/api/rag/retrieve")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"query\":\"test propagation\",\"topK\":10}"))
                .andExpect(status().isOk());
        
        // 注意：实际跨服务追踪需要在集成测试中启动多个服务
        // 这里主要验证trace ID的生成和MDC传递
    }
    
    /**
     * 测试异常情况下的追踪
     */
    @Test
    public void testTracingWithServiceFailure() throws Exception {
        // 模拟服务异常
        when(ragRetrievalService.retrieve(any(RagRequest.class)))
            .thenThrow(new RuntimeException("Simulated service failure"));
        
        // 发送请求
        mockMvc.perform(MockMvcRequestBuilders.post("/api/rag/retrieve")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"query\":\"test failure\",\"topK\":10}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(result -> {
                    // 即使服务失败，trace ID也应生成
                    String traceIdHeader = result.getResponse().getHeader("X-B3-TraceId");
                    assert traceIdHeader != null : "失败请求也应生成Trace ID";
                    
                    System.out.println("异常请求Trace ID: " + traceIdHeader);
                });
    }
}