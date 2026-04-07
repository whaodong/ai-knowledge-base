package com.example.common.security.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 认证响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    
    /**
     * 访问Token
     */
    private String accessToken;
    
    /**
     * 刷新Token
     */
    private String refreshToken;
    
    /**
     * Token类型
     */
    @Builder.Default
    private String tokenType = "Bearer";
    
    /**
     * 过期时间（毫秒）
     */
    private Long expiresIn;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 用户角色
     */
    private String role;
}
