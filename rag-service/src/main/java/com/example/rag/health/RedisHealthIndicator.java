package com.example.rag.health;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Properties;

/**
 * Redis健康检查指示器
 * 检查Redis连接状态和性能指标
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisHealthIndicator implements HealthIndicator {

    private final RedisConnectionFactory redisConnectionFactory;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public Health health() {
        try {
            // 测试Redis连接
            String result = redisTemplate.getConnectionFactory()
                .getConnection()
                .ping();
            
            if (result != null && result.equalsIgnoreCase("PONG")) {
                Health.Builder builder = Health.up()
                    .withDetail("connection", "connected")
                    .withDetail("ping_response", result);
                
                // 获取Redis信息
                try {
                    Properties info = redisTemplate.getConnectionFactory()
                        .getConnection()
                        .info();
                    
                    if (info != null) {
                        builder.withDetail("version", info.getProperty("redis_version"))
                               .withDetail("connected_clients", info.getProperty("connected_clients"))
                               .withDetail("used_memory", info.getProperty("used_memory_human"))
                               .withDetail("uptime_in_seconds", info.getProperty("uptime_in_seconds"));
                        
                        // 检查内存使用情况
                        String maxMemory = info.getProperty("maxmemory");
                        if (maxMemory != null && !maxMemory.equals("0")) {
                            builder.withDetail("max_memory", maxMemory);
                        }
                    }
                } catch (Exception e) {
                    log.warn("Failed to get Redis info", e);
                }
                
                // 测试缓存读写
                try {
                    String testKey = "health:check:test";
                    String testValue = "test_value_" + System.currentTimeMillis();
                    
                    redisTemplate.opsForValue().set(testKey, testValue);
                    String retrieved = (String) redisTemplate.opsForValue().get(testKey);
                    redisTemplate.delete(testKey);
                    
                    if (testValue.equals(retrieved)) {
                        builder.withDetail("cache_operations", "working");
                    } else {
                        builder.withDetail("cache_operations", "failed");
                    }
                } catch (Exception e) {
                    log.warn("Failed to test cache operations", e);
                    builder.withDetail("cache_operations", "error: " + e.getMessage());
                }
                
                return builder.build();
            } else {
                return Health.down()
                    .withDetail("error", "Unexpected ping response")
                    .withDetail("response", result)
                    .build();
            }
        } catch (Exception e) {
            log.error("Redis health check failed", e);
            return Health.down()
                .withDetail("error", e.getMessage())
                .withException(e)
                .build();
        }
    }
}
