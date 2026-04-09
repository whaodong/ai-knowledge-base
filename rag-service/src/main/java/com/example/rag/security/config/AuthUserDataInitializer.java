package com.example.rag.security.config;

import com.example.common.security.Role;
import com.example.common.security.User;
import com.example.common.security.UserPersistenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

/**
 * 认证用户初始化器。
 *
 * <p>用于在首次启动时创建默认账号，避免迁移到持久化后无可用登录账户。</p>
 */
@Configuration
@RequiredArgsConstructor
public class AuthUserDataInitializer implements ApplicationRunner {

    private final UserPersistenceService userPersistenceService;
    private final PasswordEncoder passwordEncoder;

    /**
     * 应用启动后初始化默认用户。
     *
     * @param args 启动参数
     */
    @Override
    public void run(ApplicationArguments args) {
        initUserIfAbsent("admin", "admin123", "admin@example.com", Set.of(Role.ADMIN));
        initUserIfAbsent("user", "user123", "user@example.com", Set.of(Role.USER));
        initUserIfAbsent("viewer", "viewer123", "viewer@example.com", Set.of(Role.VIEWER));
    }

    /**
     * 在用户不存在时创建默认用户。
     *
     * @param username 用户名
     * @param rawPassword 明文密码
     * @param email 邮箱
     * @param roles 角色集合
     */
    private void initUserIfAbsent(String username, String rawPassword, String email, Set<Role> roles) {
        if (userPersistenceService.existsByUsername(username)) {
            return;
        }
        User user = User.createEnabledUser(null, username, passwordEncoder.encode(rawPassword), roles);
        user.setEmail(email);
        userPersistenceService.saveUser(user);
    }
}
