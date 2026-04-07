package com.example.rag.metrics;

import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * 监控指标切面
 * 自动记录关键方法的执行时间和指标
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class MetricsAspect {

    private final RagMetricsConfig ragMetricsConfig;

    /**
     * 监控RAG检索服务
     */
    @Around("execution(* com.example.rag.service.RagRetrievalService.*(..))")
    public Object monitorRagRetrieval(ProceedingJoinPoint joinPoint) throws Throwable {
        ragMetricsConfig.incrementActiveQueries();
        
        try {
            long startTime = System.currentTimeMillis();
            
            Object result = joinPoint.proceed();
            
            long duration = System.currentTimeMillis() - startTime;
            ragMetricsConfig.getRagQueryCount().increment();
            ragMetricsConfig.getRagQueryLatency().record(java.time.Duration.ofMillis(duration));
            
            log.debug("RAG retrieval completed in {} ms", duration);
            
            return result;
        } finally {
            ragMetricsConfig.decrementActiveQueries();
        }
    }

    /**
     * 监控Milvus向量搜索
     */
    @Around("execution(* com.example.rag.retriever.VectorRetriever.*(..))")
    public Object monitorVectorRetrieval(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        
        try {
            Object result = joinPoint.proceed();
            
            long duration = System.currentTimeMillis() - startTime;
            ragMetricsConfig.getMilvusLatency().record(java.time.Duration.ofMillis(duration));
            
            log.debug("Vector retrieval completed in {} ms", duration);
            
            return result;
        } catch (Throwable throwable) {
            log.error("Vector retrieval failed", throwable);
            throw throwable;
        }
    }

    /**
     * 监控缓存操作
     */
    @Around("execution(* com.example.rag.cache.RagCacheService.*(..))")
    public Object monitorCache(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().getName();
        
        try {
            Object result = joinPoint.proceed();
            
            if (methodName.contains("get")) {
                if (result != null) {
                    ragMetricsConfig.recordCacheHit();
                } else {
                    ragMetricsConfig.recordCacheMiss();
                }
            }
            
            return result;
        } catch (Throwable throwable) {
            log.error("Cache operation failed: {}", methodName, throwable);
            throw throwable;
        }
    }
}
