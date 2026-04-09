package com.example.rag.security.repository;

import com.example.rag.security.entity.AuthUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 认证用户仓储接口。
 */
@Repository
public interface AuthUserRepository extends JpaRepository<AuthUserEntity, Long> {

    /**
     * 根据用户名查询用户。
     *
     * @param username 用户名
     * @return 用户实体
     */
    Optional<AuthUserEntity> findByUsername(String username);

    /**
     * 根据用户名判断用户是否存在。
     *
     * @param username 用户名
     * @return 存在返回true，否则返回false
     */
    boolean existsByUsername(String username);
}
