package com.example.common.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtConfig {
    
    /**
     * JWT密钥（Base64编码）
     */
    private String secret = "ai-knowledge-base-default-secret-key-must-be-at-least-256-bits-long";
    
    /**
     * Token过期时间（毫秒），默认24小时
     */
    private Long expiration = 86400000L;
    
    /**
     * Token前缀
     */
    private String tokenPrefix = "Bearer ";
    
    /**
     * Token请求头名称
     */
    private String header = "Authorization";
    
    /**
     * 刷新Token过期时间（毫秒），默认7天
     */
    private Long refreshExpiration = 604800000L;
    
    /**
     * Token签发者
     */
    private String issuer = "ai-knowledge-base";
}
