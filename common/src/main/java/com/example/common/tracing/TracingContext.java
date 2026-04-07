package com.example.common.tracing;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.Callable;

/**
 * 追踪上下文工具类
 * 
 * <p>提供便捷的Span操作方法，简化追踪代码。</p>
 */
@Slf4j
public class TracingContext {

    private final Tracer tracer;

    public TracingContext(Tracer tracer) {
        this.tracer = tracer;
    }

    /**
     * 获取当前Span
     */
    public Span currentSpan() {
        return tracer.currentSpan();
    }

    /**
     * 获取当前Trace ID
     */
    public String currentTraceId() {
        Span span = currentSpan();
        return span != null ? span.context().traceId() : null;
    }

    /**
     * 获取当前Span ID
     */
    public String currentSpanId() {
        Span span = currentSpan();
        return span != null ? span.context().spanId() : null;
    }

    /**
     * 创建新的Span
     */
    public Span createSpan(String name) {
        return tracer.spanBuilder().name(name).start();
    }

    /**
     * 在新Span中执行操作
     */
    public <T> T withSpan(String spanName, Callable<T> operation) {
        Span span = createSpan(spanName);
        try (Tracer.SpanInScope ws = tracer.withSpan(span)) {
            return operation.call();
        } catch (Exception e) {
            span.error(e);
            throw new RuntimeException(e);
        } finally {
            span.end();
        }
    }

    /**
     * 在新Span中执行操作（无返回值）
     */
    public void withSpan(String spanName, Runnable operation) {
        Span span = createSpan(spanName);
        try (Tracer.SpanInScope ws = tracer.withSpan(span)) {
            operation.run();
        } catch (Exception e) {
            span.error(e);
            throw new RuntimeException(e);
        } finally {
            span.end();
        }
    }

    /**
     * 添加标签到当前Span
     */
    public void addTag(String key, String value) {
        Span span = currentSpan();
        if (span != null) {
            span.tag(key, value);
        }
    }

    /**
     * 批量添加标签到当前Span
     */
    public void addTags(Map<String, String> tags) {
        Span span = currentSpan();
        if (span != null) {
            tags.forEach(span::tag);
        }
    }

    /**
     * 添加事件到当前Span
     */
    public void addEvent(String eventName) {
        Span span = currentSpan();
        if (span != null) {
            span.event(eventName);
        }
    }

    /**
     * 记录异常到当前Span
     */
    public void recordException(Throwable throwable) {
        Span span = currentSpan();
        if (span != null) {
            span.error(throwable);
        }
    }

    /**
     * 设置远程服务名称
     */
    public void setRemoteServiceName(String serviceName) {
        Span span = currentSpan();
        if (span != null) {
            span.remoteServiceName(serviceName);
        }
    }
}
