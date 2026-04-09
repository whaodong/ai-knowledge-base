package com.example.common.security;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 默认的UserDetailsService实现 - 用于测试
 * 实际项目中应该从数据库加载用户信息
 */
@Service
public class DefaultUserDetailsService implements CustomUserDetailsService, UserPersistenceService {
    
    private final Map<String, User> users = new HashMap<>();
    
    public DefaultUserDetailsService(PasswordEncoder passwordEncoder) {
        // 初始化测试用户
        // 管理员
        User admin = User.createEnabledUser(
                1L,
                "admin",
                passwordEncoder.encode("admin123"),
                Set.of(Role.ADMIN)
        );
        admin.setEmail("admin@example.com");
        users.put("admin", admin);
        
        // 普通用户
        User user = User.createEnabledUser(
                2L,
                "user",
                passwordEncoder.encode("user123"),
                Set.of(Role.USER)
        );
        user.setEmail("user@example.com");
        users.put("user", user);
        
        // 访客
        User viewer = User.createEnabledUser(
                3L,
                "viewer",
                passwordEncoder.encode("viewer123"),
                Set.of(Role.VIEWER)
        );
        viewer.setEmail("viewer@example.com");
        users.put("viewer", viewer);
    }
    
    /**
     * 根据用户名加载用户信息。
     *
     * @param username 用户名
     * @return 用户信息
     */
    @Override
    public User loadUserByUsername(String username) {
        User user = users.get(username);
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在: " + username);
        }
        return user;
    }
    
    /**
     * 添加用户（用于测试）
     */
    public void addUser(User user) {
        users.put(user.getUsername(), user);
    }

    /**
     * 保存用户信息。
     *
     * @param user 用户对象
     */
    @Override
    public void saveUser(User user) {
        addUser(user);
    }

    /**
     * 根据用户名判断用户是否已存在。
     *
     * @param username 用户名
     * @return 存在返回true，否则返回false
     */
    @Override
    public boolean existsByUsername(String username) {
        return users.containsKey(username);
    }
    
    /**
     * 获取所有用户（用于测试）
     */
    public Map<String, User> getAllUsers() {
        return new HashMap<>(users);
    }
}
