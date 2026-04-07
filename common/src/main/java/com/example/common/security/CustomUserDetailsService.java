package com.example.common.security;

import org.springframework.security.core.userdetails.UserDetailsService;

/**
 * 自定义UserDetailsService接口
 */
public interface CustomUserDetailsService extends UserDetailsService {
    
    /**
     * 根据用户名加载用户信息
     * 实现类需要实现此方法，从数据库或其他存储加载用户信息
     */
    @Override
    User loadUserByUsername(String username);
}
