package com.example.rag.security.entity;

import com.example.common.security.Role;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * 认证用户持久化实体。
 */
@Getter
@Setter
@Entity
@Table(
        name = "auth_users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_auth_users_username", columnNames = "username")
        }
)
public class AuthUserEntity {

    /**
     * 用户主键ID。
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 用户名。
     */
    @Column(nullable = false, length = 64)
    private String username;

    /**
     * 加密后的密码。
     */
    @Column(nullable = false, length = 255)
    private String password;

    /**
     * 邮箱地址。
     */
    @Column(length = 128)
    private String email;

    /**
     * 手机号。
     */
    @Column(length = 32)
    private String phone;

    /**
     * 账号是否启用。
     */
    @Column(nullable = false)
    private boolean enabled;

    /**
     * 账号是否未过期。
     */
    @Column(nullable = false)
    private boolean accountNonExpired;

    /**
     * 账号是否未锁定。
     */
    @Column(nullable = false)
    private boolean accountNonLocked;

    /**
     * 凭证是否未过期。
     */
    @Column(nullable = false)
    private boolean credentialsNonExpired;

    /**
     * 角色集合。
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "auth_user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role", nullable = false, length = 32)
    @Enumerated(EnumType.STRING)
    private Set<Role> roles = new HashSet<>();

    /**
     * 创建时间。
     */
    @Column(nullable = false)
    private LocalDateTime createdAt;

    /**
     * 更新时间。
     */
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
