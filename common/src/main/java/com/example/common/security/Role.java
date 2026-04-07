package com.example.common.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.Collections;

/**
 * 用户角色枚举
 */
public enum Role {
    /**
     * 管理员 - 拥有所有权限
     */
    ADMIN("管理员", "ROLE_ADMIN"),
    
    /**
     * 普通用户 - 可以管理自己的文档
     */
    USER("普通用户", "ROLE_USER"),
    
    /**
     * 访客 - 只能查看公开文档
     */
    VIEWER("访客", "ROLE_VIEWER");

    private final String description;
    private final String authority;

    Role(String description, String authority) {
        this.description = description;
        this.authority = authority;
    }

    public String getDescription() {
        return description;
    }

    public String getAuthority() {
        return authority;
    }

    /**
     * 转换为Spring Security权限集合
     */
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority(this.authority));
    }

    /**
     * 根据权限名称获取角色
     */
    public static Role fromAuthority(String authority) {
        for (Role role : values()) {
            if (role.authority.equals(authority)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Unknown authority: " + authority);
    }
}
