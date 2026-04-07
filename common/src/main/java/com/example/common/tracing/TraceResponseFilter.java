package com.example.common.tracing;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Trace响应头过滤器
 * 
 * <p>将Trace ID添加到响应头中，便于前端和调试。</p>
 */
@Slf4j
@Component
@Order(2)
public class TraceResponseFilter implements Filter {

    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String SPAN_ID_HEADER = "X-Span-Id";
    private static final String PARENT_SPAN_ID_HEADER = "X-Parent-Span-Id";

    private final Tracer tracer;

    public TraceResponseFilter(Tracer tracer) {
        this.tracer = tracer;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        try {
            // 执行请求
            chain.doFilter(request, response);
            
        } finally {
            // 将Trace信息添加到响应头
            Span span = tracer.currentSpan();
            if (span != null) {
                String traceId = span.context().traceId();
                String spanId = span.context().spanId();
                
                httpResponse.setHeader(TRACE_ID_HEADER, traceId);
                httpResponse.setHeader(SPAN_ID_HEADER, spanId);
                
                log.debug("响应头添加Trace信息: traceId={}, spanId={}", traceId, spanId);
            }
        }
    }
}
