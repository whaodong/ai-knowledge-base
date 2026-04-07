package com.example.common.tracing;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.exporter.SpanExportingPredicate;
import io.micrometer.tracing.propagation.Propagator;
import io.micrometer.tracing.sampler.Sampler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

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
     * 配置采样器
     * 生产环境默认10%采样率，可通过配置调整
     */
    @Bean
    @ConditionalOnMissingBean
    public Sampler defaultSampler() {
        log.info("配置分布式追踪采样率: {}%", samplingProbability * 100);
        return new Sampler() {
            @Override
            public boolean isSampled(Span span) {
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
            }
        };
    }

    /**
     * 自定义Span导出过滤器
     * 过滤不需要导出的Span
     */
    @Bean
    public SpanExportingPredicate spanExportingPredicate() {
        return span -> {
            // 过滤健康检查和指标采集
            String spanName = span.getName();
            return !spanName.contains("actuator") 
                && !spanName.contains("prometheus")
                && !spanName.contains("health");
        };
    }

    /**
     * 追踪属性配置
     */
    @Bean
    public TracingProperties tracingProperties() {
        return new TracingProperties();
    }

    /**
     * 追踪上下文工具类
     */
    @Bean
    public TracingContext tracingContext(Tracer tracer) {
        return new TracingContext(tracer);
    }
}
