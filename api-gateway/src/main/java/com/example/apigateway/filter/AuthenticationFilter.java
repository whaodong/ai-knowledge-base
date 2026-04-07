package com.example.apigateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * 认证校验过滤器
 * 
 * 基于JWT的简单验证机制，检查请求头中的Authorization字段
 * 支持配置是否要求认证、白名单路径等
 */
@Slf4j
@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    /** JWT密钥（生产环境应从配置中心获取） */
    private static final String JWT_SECRET = "your-secret-key-should-be-at-least-32-characters-long";
    
    /** 白名单路径（不需要认证的路径） */
    private static final List<String> WHITELIST_PATHS = Arrays.asList(
            "/actuator/health",
            "/actuator/info",
            "/swagger-ui.html",
            "/v3/api-docs",
            "/webjars",
            "/swagger-resources",
            "/eureka"
    );

    public AuthenticationFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return new InnerFilter(config);
    }

    private static class InnerFilter implements GatewayFilter, Ordered {
        
        private final Config config;
        private final SecretKey secretKey;
        
        public InnerFilter(Config config) {
            this.config = config;
            this.secretKey = Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8));
        }

        @Override
        public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getPath().value();
            
            // 检查是否在白名单中
            if (isWhitelisted(path)) {
                if (log.isDebugEnabled()) {
                    log.debug("[Authentication Skip] Path: {} is in whitelist", path);
                }
                return chain.filter(exchange);
            }
            
            // 检查是否启用认证
            if (!config.isEnabled() || !config.isRequireAuth()) {
                return chain.filter(exchange);
            }
            
            // 获取Authorization头
            String authHeader = request.getHeaders().getFirst("Authorization");
            
            if (!StringUtils.hasText(authHeader)) {
                log.warn("[Authentication Failed] No Authorization header found, Path: {}", path);
                return unauthorizedResponse(exchange, "Missing Authorization header");
            }
            
            // 检查Bearer Token格式
            if (!authHeader.startsWith("Bearer ")) {
                log.warn("[Authentication Failed] Invalid Authorization format, Path: {}", path);
                return unauthorizedResponse(exchange, "Invalid Authorization format, expected 'Bearer <token>'");
            }
            
            String token = authHeader.substring(7);
            
            try {
                // 验证JWT令牌
                Claims claims = Jwts.parser()
                        .verifyWith(secretKey)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();
                
                // 检查令牌是否过期
                if (claims.getExpiration() != null && claims.getExpiration().before(new java.util.Date())) {
                    log.warn("[Authentication Failed] Token expired, Path: {}", path);
                    return unauthorizedResponse(exchange, "Token expired");
                }
                
                // 提取用户信息并添加到请求头
                String username = claims.getSubject();
                String userId = claims.get("userId", String.class);
                String roles = claims.get("roles", String.class);
                
                if (log.isDebugEnabled()) {
                    log.debug("[Authentication Success] User: {}, Path: {}", username, path);
                }
                
                // 将用户信息添加到请求头，传递给下游服务
                ServerHttpRequest mutatedRequest = request.mutate()
                        .header("X-User-Id", userId != null ? userId : "")
                        .header("X-Username", username != null ? username : "")
                        .header("X-Roles", roles != null ? roles : "")
                        .build();
                
                return chain.filter(exchange.mutate().request(mutatedRequest).build());
                
            } catch (Exception e) {
                log.warn("[Authentication Failed] Token validation error: {}, Path: {}", 
                        e.getMessage(), path);
                return unauthorizedResponse(exchange, "Invalid token: " + e.getMessage());
            }
        }

        @Override
        public int getOrder() {
            return Ordered.HIGHEST_PRECEDENCE + 1;
        }
        
        /**
         * 检查路径是否在白名单中
         */
        private boolean isWhitelisted(String path) {
            return WHITELIST_PATHS.stream().anyMatch(path::startsWith);
        }
        
        /**
         * 返回401未授权响应
         */
        private Mono<Void> unauthorizedResponse(ServerWebExchange exchange, String message) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            exchange.getResponse().getHeaders().add("Content-Type", "application/json");
            
            String body = String.format("{\"code\": 401, \"message\": \"%s\"}", message);
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            
            return exchange.getResponse().writeWith(Mono.just(
                    exchange.getResponse().bufferFactory().wrap(bytes)));
        }
    }

    @Data
    public static class Config {
        /** 是否启用过滤器 */
        private boolean enabled = true;
        
        /** 是否要求认证 */
        private boolean requireAuth = true;
        
        /** JWT密钥（可覆盖默认值） */
        private String jwtSecret;
        
        /** 自定义白名单路径（逗号分隔） */
        private String customWhitelist;
    }
}