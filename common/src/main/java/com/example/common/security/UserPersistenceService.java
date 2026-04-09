package com.example.common.security;

/**
 * 用户持久化服务接口。
 *
 * <p>用于认证模块在注册等场景中读写用户数据，具体实现可基于内存、关系型数据库等。</p>
 */
public interface UserPersistenceService {

    /**
     * 保存用户信息。
     *
     * @param user 用户对象
     */
    void saveUser(User user);

    /**
     * 根据用户名判断用户是否已存在。
     *
     * @param username 用户名
     * @return 存在返回true，否则返回false
     */
    boolean existsByUsername(String username);
}
