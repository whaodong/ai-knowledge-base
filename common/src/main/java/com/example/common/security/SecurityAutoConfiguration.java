package com.example.common.security;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 安全自动配置类
 */
@AutoConfiguration
@ComponentScan(basePackages = "com.example.common.security")
public class SecurityAutoConfiguration {
    
    /**
     * 默认UserDetailsService - 仅在没有其他实现时使用
     */
    @Bean
    @ConditionalOnMissingBean(CustomUserDetailsService.class)
    public DefaultUserDetailsService defaultUserDetailsService(PasswordEncoder passwordEncoder) {
        return new DefaultUserDetailsService(passwordEncoder);
    }
}
