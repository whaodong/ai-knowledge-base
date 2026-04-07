package com.example.apigateway.filter;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;

/**
 * 请求日志过滤器
 * 
 * 记录每个请求的基本信息：路径、方法、客户端IP、处理时间等
 * 支持配置是否记录请求头信息
 */
@Slf4j
@Component
public class RequestLoggingFilter extends AbstractGatewayFilterFactory<RequestLoggingFilter.Config> {

    public RequestLoggingFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return new InnerFilter(config);
    }

    private static class InnerFilter implements GatewayFilter, Ordered {
        
        private final Config config;
        
        public InnerFilter(Config config) {
            this.config = config;
        }

        @Override
        public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
            if (!config.isEnabled()) {
                return chain.filter(exchange);
            }
            
            Instant startTime = Instant.now();
            ServerHttpRequest request = exchange.getRequest();
            
            String requestId = exchange.getRequest().getId();
            String path = request.getPath().value();
            String method = request.getMethod().name();
            String clientIp = getClientIp(request);
            
            // 记录请求开始日志
            if (log.isDebugEnabled()) {
                log.debug("[Request Start] ID: {}, Method: {}, Path: {}, ClientIP: {}",
                        requestId, method, path, clientIp);
                
                if (config.isLogHeaders()) {
                    log.debug("[Request Headers] ID: {}, Headers: {}",
                            requestId, request.getHeaders());
                }
            }
            
            return chain.filter(exchange).then(Mono.fromRunnable(() -> {
                Instant endTime = Instant.now();
                Duration duration = Duration.between(startTime, endTime);
                
                int statusCode = exchange.getResponse().getStatusCode() != null 
                        ? exchange.getResponse().getStatusCode().value() 
                        : 0;
                
                log.info("[Request Completed] ID: {}, Method: {}, Path: {}, Status: {}, Duration: {}ms, ClientIP: {}",
                        requestId, method, path, statusCode, duration.toMillis(), clientIp);
            }));
        }

        @Override
        public int getOrder() {
            return Ordered.HIGHEST_PRECEDENCE;
        }
    }

    /**
     * 获取客户端真实IP地址
     */
    private static String getClientIp(ServerHttpRequest request) {
        String xForwardedFor = request.getHeaders().getFirst("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // 如果经过多个代理，取第一个IP
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeaders().getFirst("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddress() != null 
                ? request.getRemoteAddress().getAddress().getHostAddress()
                : "unknown";
    }

    @Data
    public static class Config {
        /** 是否启用过滤器 */
        private boolean enabled = true;
        
        /** 是否记录请求头信息 */
        private boolean logHeaders = false;
        
        /** 需要排除的路径模式 */
        private String excludePatterns;
    }
}