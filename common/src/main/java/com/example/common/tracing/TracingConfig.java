package com.example.common.tracing;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.exporter.SpanExportingPredicate;
import io.micrometer.tracing.propagation.Propagator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * 分布式追踪配置
 * 
 * <p>配置Micrometer Tracing，支持Zipkin和Jaeger导出器。</p>
 * <p>提供统一的Span标签配置和采样策略。</p>
 */
@Slf4j
@Configuration
public class TracingConfig {

    @Value("${spring.application.name:unknown}")
    private String applicationName;

    @Value("${tracing.sampling.probability:0.1}")
    private double samplingProbability;

    @Value("${tracing.enabled:true}")
    private boolean tracingEnabled;

    /**
     * 配置采样函数
     * 生产环境默认10%采样率，可通过配置调整
     */
    @Bean
    @ConditionalOnMissingBean
    public Function<Span, Boolean> samplingFunction() {
        log.info("配置分布式追踪采样率: {}%", samplingProbability * 100);
        return span -> {
            // 错误请求全部采样
            if (span.getError() != null) {
                return true;
            }
            
            // 慢请求优先采样（超过1秒）
            Long duration = span.getDuration();
            if (duration != null && duration > 1000000000L) { // 纳秒
                return true;
            }
            
            // 按配置概率采样
            return Math.random() < samplingProbability;
        };
    }
}