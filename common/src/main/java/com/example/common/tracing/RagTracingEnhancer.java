package com.example.common.tracing;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

/**
 * AI服务追踪增强器
 * 
 * <p>为AI相关操作提供专用的追踪支持。</p>
 */
@Slf4j
@Component
@ConditionalOnBean(Tracer.class)
public class RagTracingEnhancer {

    private final Tracer tracer;
    private final TraceAnalysisService traceAnalysisService;

    public RagTracingEnhancer(Tracer tracer, TraceAnalysisService traceAnalysisService) {
        this.tracer = tracer;
        this.traceAnalysisService = traceAnalysisService;
    }

    /**
     * 创建文档处理链路Span
     */
    public Span createDocumentProcessingSpan(String documentId, String fileName) {
        Span span = tracer.spanBuilder()
                .name("document.process")
                .kind(Span.Kind.SERVER)
                .start();
        
        span.tag("document.id", documentId);
        span.tag("document.name", fileName);
        span.tag("process.stage", "init");
        
        return span;
    }

    /**
     * 创建RAG查询Span
     */
    public Span createRagQuerySpan(String question, int topK) {
        Span span = tracer.spanBuilder()
                .name("rag.query")
                .kind(Span.Kind.SERVER)
                .start();
        
        span.tag("question.length", String.valueOf(question.length()));
        span.tag("question.preview", question.length() > 100 ? question.substring(0, 100) + "..." : question);
        span.tag("top.k", String.valueOf(topK));
        
        return span;
    }

    /**
     * 创建向量检索Span
     */
    public Span createVectorRetrievalSpan(String query, int k) {
        Span span = tracer.spanBuilder()
                .name("vector.retrieval")
                .kind(Span.Kind.CLIENT)
                .start();
        
        span.remoteServiceName("milvus");
        span.tag("query.length", String.valueOf(query.length()));
        span.tag("k", String.valueOf(k));
        
        return span;
    }

    /**
     * 创建关键词检索Span
     */
    public Span createKeywordRetrievalSpan(String query, int k) {
        Span span = tracer.spanBuilder()
                .name("keyword.retrieval")
                .kind(Span.Kind.CLIENT)
                .start();
        
        span.tag("query.length", String.valueOf(query.length()));
        span.tag("k", String.valueOf(k));
        
        return span;
    }

    /**
     * 创建重排序Span
     */
    public Span createRerankingSpan(int candidateCount) {
        Span span = tracer.spanBuilder()
                .name("reranking")
                .kind(Span.Kind.SERVER)
                .start();
        
        span.tag("candidates.count", String.valueOf(candidateCount));
        
        return span;
    }

    /**
     * 创建Embedding生成Span
     */
    public Span createEmbeddingSpan(String model, int textCount) {
        Span span = tracer.spanBuilder()
                .name("embedding.generate")
                .kind(Span.Kind.CLIENT)
                .start();
        
        span.remoteServiceName("openai");
        span.tag("model", model);
        span.tag("text.count", String.valueOf(textCount));
        
        return span;
    }

    /**
     * 创建LLM生成Span
     */
    public Span createLlmGenerationSpan(String model, int promptTokens) {
        Span span = tracer.spanBuilder()
                .name("llm.generate")
                .kind(Span.Kind.CLIENT)
                .start();
        
        span.remoteServiceName("openai");
        span.tag("model", model);
        span.tag("prompt.tokens", String.valueOf(promptTokens));
        
        return span;
    }

    /**
     * 记录Token使用量
     */
    public void recordTokenUsage(Span span, int promptTokens, int completionTokens, int totalTokens) {
        if (span != null) {
            span.tag("tokens.prompt", String.valueOf(promptTokens));
            span.tag("tokens.completion", String.valueOf(completionTokens));
            span.tag("tokens.total", String.valueOf(totalTokens));
            
            log.debug("Token使用: prompt={}, completion={}, total={}", 
                    promptTokens, completionTokens, totalTokens);
        }
    }

    /**
     * 记录检索结果统计
     */
    public void recordRetrievalStats(Span span, int vectorCount, int keywordCount, int afterRerank) {
        if (span != null) {
            span.tag("retrieval.vector.count", String.valueOf(vectorCount));
            span.tag("retrieval.keyword.count", String.valueOf(keywordCount));
            span.tag("retrieval.after_rerank", String.valueOf(afterRerank));
            
            log.debug("检索统计: 向量={}, 关键词={}, 重排序后={}", 
                    vectorCount, keywordCount, afterRerank);
        }
    }

    /**
     * 记录外部API调用
     */
    public void recordExternalApiCall(Span span, String serviceName, String apiName, long durationMs) {
        if (span != null) {
            span.tag("external.service", serviceName);
            span.tag("external.api", apiName);
            span.tag("external.duration_ms", String.valueOf(durationMs));
            
            traceAnalysisService.recordServiceCall(
                    tracer.currentSpan() != null ? "current-service" : "unknown",
                    serviceName
            );
        }
    }
}
