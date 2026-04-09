package com.example.rag.security.service;

import com.example.common.security.CustomUserDetailsService;
import com.example.common.security.Role;
import com.example.common.security.User;
import com.example.common.security.UserPersistenceService;
import com.example.rag.security.entity.AuthUserEntity;
import com.example.rag.security.repository.AuthUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * 基于PostgreSQL的用户认证服务实现。
 */
@Service
@Primary
@RequiredArgsConstructor
public class PostgresUserDetailsService implements CustomUserDetailsService, UserPersistenceService {

    private final AuthUserRepository authUserRepository;

    /**
     * 根据用户名加载用户信息。
     *
     * @param username 用户名
     * @return 用户信息
     */
    @Override
    @Transactional(readOnly = true)
    public User loadUserByUsername(String username) {
        return authUserRepository.findByUsername(username)
                .map(this::toDomainUser)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在: " + username));
    }

    /**
     * 保存用户信息。
     *
     * @param user 用户对象
     */
    @Override
    @Transactional
    public void saveUser(User user) {
        AuthUserEntity entity = authUserRepository.findByUsername(user.getUsername())
                .orElseGet(AuthUserEntity::new);
        mergeToEntity(user, entity);
        authUserRepository.save(entity);
    }

    /**
     * 根据用户名判断用户是否已存在。
     *
     * @param username 用户名
     * @return 存在返回true，否则返回false
     */
    @Override
    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        return authUserRepository.existsByUsername(username);
    }

    /**
     * 将实体对象转换为领域用户对象。
     *
     * @param entity 持久化实体
     * @return 领域用户对象
     */
    private User toDomainUser(AuthUserEntity entity) {
        return User.builder()
                .id(entity.getId())
                .username(entity.getUsername())
                .password(entity.getPassword())
                .email(entity.getEmail())
                .phone(entity.getPhone())
                .enabled(entity.isEnabled())
                .accountNonExpired(entity.isAccountNonExpired())
                .accountNonLocked(entity.isAccountNonLocked())
                .credentialsNonExpired(entity.isCredentialsNonExpired())
                .roles(entity.getRoles())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    /**
     * 将领域用户信息合并到持久化实体。
     *
     * @param user 领域用户对象
     * @param entity 持久化实体
     */
    private void mergeToEntity(User user, AuthUserEntity entity) {
        entity.setUsername(user.getUsername());
        entity.setPassword(user.getPassword());
        entity.setEmail(user.getEmail());
        entity.setPhone(user.getPhone());
        entity.setEnabled(user.isEnabled());
        entity.setAccountNonExpired(user.isAccountNonExpired());
        entity.setAccountNonLocked(user.isAccountNonLocked());
        entity.setCredentialsNonExpired(user.isCredentialsNonExpired());
        entity.setRoles(resolveRoles(user.getRoles()));

        LocalDateTime now = LocalDateTime.now();
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(user.getCreatedAt() != null ? user.getCreatedAt() : now);
        }
        entity.setUpdatedAt(now);
    }

    /**
     * 解析角色集合，确保角色非空。
     *
     * @param roles 角色集合
     * @return 可持久化角色集合
     */
    private Set<Role> resolveRoles(Set<Role> roles) {
        if (roles == null || roles.isEmpty()) {
            return Set.of(Role.VIEWER);
        }
        return roles;
    }
}
