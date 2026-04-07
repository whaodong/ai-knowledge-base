package com.example.common.tracing;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 追踪日志过滤器
 * 
 * <p>将Trace ID注入到日志上下文中，实现日志与追踪的关联。</p>
 */
@Slf4j
@Component
@Order(1)
public class TraceLoggingFilter implements Filter {

    private static final String TRACE_ID_KEY = "traceId";
    private static final String SPAN_ID_KEY = "spanId";
    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String SPAN_ID_HEADER = "X-Span-Id";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        // 从请求头获取Trace ID（如果存在）
        String traceId = httpRequest.getHeader(TRACE_ID_HEADER);
        String spanId = httpRequest.getHeader(SPAN_ID_HEADER);
        
        try {
            // 将Trace ID放入MDC，日志可以自动输出
            if (traceId != null) {
                MDC.put(TRACE_ID_KEY, traceId);
            }
            if (spanId != null) {
                MDC.put(SPAN_ID_KEY, spanId);
            }
            
            // 执行请求
            chain.doFilter(request, response);
            
        } finally {
            // 清理MDC
            MDC.remove(TRACE_ID_KEY);
            MDC.remove(SPAN_ID_KEY);
        }
    }
}
