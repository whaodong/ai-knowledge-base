package com.example.common.security;

import com.example.common.security.dto.AuthResponse;
import com.example.common.security.dto.LoginRequest;
import com.example.common.security.dto.RefreshTokenRequest;
import com.example.common.security.dto.RegisterRequest;
import org.springframework.security.core.Authentication;

/**
 * 认证服务接口
 */
public interface AuthService {
    
    /**
     * 用户登录
     */
    AuthResponse login(LoginRequest request);
    
    /**
     * 用户注册
     */
    AuthResponse register(RegisterRequest request);
    
    /**
     * 刷新Token
     */
    AuthResponse refreshToken(RefreshTokenRequest request);
    
    /**
     * 获取当前认证信息
     */
    Authentication getCurrentAuthentication();
    
    /**
     * 获取当前用户名
     */
    String getCurrentUsername();
    
    /**
     * 获取当前用户
     */
    User getCurrentUser();
}
