package com.example.rag.service;

import com.example.common.tracing.RagTracingEnhancer;
import com.example.rag.model.RagRequest;
import com.example.rag.model.RagResponse;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

/**
 * RAG服务追踪装饰器
 * 
 * <p>为RAG服务添加分布式追踪支持，包括：</p>
 * <ul>
 *   <li>查询链路追踪</li>
 *   <li>检索统计记录</li>
 *   <li>Token使用追踪</li>
 *   <li>性能指标收集</li>
 * </ul>
 */
@Slf4j
@Service
public class RagTracingService {

    private final RagRetrievalService ragRetrievalService;
    private final Tracer tracer;
    private final RagTracingEnhancer ragTracingEnhancer;

    @Autowired
    public RagTracingService(
            RagRetrievalService ragRetrievalService,
            Tracer tracer,
            RagTracingEnhancer ragTracingEnhancer) {
        this.ragRetrievalService = ragRetrievalService;
        this.tracer = tracer;
        this.ragTracingEnhancer = ragTracingEnhancer;
    }

    /**
     * 执行带追踪的RAG查询
     */
    public RagResponse retrieveWithTracing(RagRequest request) {
        Span span = ragTracingEnhancer.createRagQuerySpan(
                request.getQuery(), 
                request.getTopK()
        );
        
        Instant startTime = Instant.now();
        
        try (Tracer.SpanInScope ws = tracer.withSpan(span)) {
            // 执行查询
            RagResponse response = ragRetrievalService.retrieve(request);
            
            // 记录检索统计
            if (response.getRetrieverStats() != null) {
                ragTracingEnhancer.recordRetrievalStats(
                        span,
                        response.getRetrieverStats().getVectorRetrievedCount(),
                        response.getRetrieverStats().getKeywordRetrievedCount(),
                        response.getRetrieverStats().getAfterRerankCount()
                );
            }
            
            // 添加性能指标
            span.tag("response.success", String.valueOf(response.isSuccess()));
            span.tag("response.time_ms", String.valueOf(response.getRetrievalTimeMs()));
            span.tag("result.count", String.valueOf(
                    response.getRetrievedDocuments() != null ? response.getRetrievedDocuments().size() : 0
            ));
            
            return response;
            
        } catch (Exception ex) {
            span.error(ex);
            throw ex;
        } finally {
            span.end();
            
            Duration duration = Duration.between(startTime, Instant.now());
            log.info("RAG查询完成: query={}, duration={}ms, traceId={}", 
                    request.getQuery().substring(0, Math.min(50, request.getQuery().length())),
                    duration.toMillis(),
                    span.context().traceId());
        }
    }
}
