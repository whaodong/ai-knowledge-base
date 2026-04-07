package com.example.common.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 多级缓存配置类
 * 配置本地缓存（Caffeine）和分布式缓存（Redis）的多级缓存架构
 */
@Configuration
@EnableCaching
public class MultiLevelCacheConfig {

    /**
     * 本地缓存配置 - Caffeine
     */
    @Bean
    @Primary
    public CacheManager localCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .initialCapacity(100)  // 初始容量
                .maximumSize(1000)     // 最大容量
                .expireAfterWrite(Duration.ofMinutes(5))  // 写入后5分钟过期
                .recordStats()         // 开启统计
        );
        return cacheManager;
    }

    /**
     * 分布式缓存配置 - Redis
     * 支持不同缓存区域的差异化配置
     */
    @Bean
    public CacheManager redisCacheManager(RedisConnectionFactory redisConnectionFactory) {
        // 默认配置
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(30))  // 默认30分钟过期
                .disableCachingNullValues()        // 不缓存null值
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()));

        // 不同缓存区域的特定配置
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        
        // 热点数据缓存 - 短期高频访问
        cacheConfigurations.put("hotspot", defaultConfig.entryTtl(Duration.ofMinutes(5)));
        
        // 静态数据缓存 - 长期不变
        cacheConfigurations.put("static", defaultConfig.entryTtl(Duration.ofHours(24)));
        
        // 向量数据缓存 - 中等时长
        cacheConfigurations.put("embedding", defaultConfig.entryTtl(Duration.ofMinutes(15)));
        
        // 文档数据缓存 - 中等时长
        cacheConfigurations.put("document", defaultConfig.entryTtl(Duration.ofMinutes(20)));

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()  // 支持事务
                .build();
    }

    /**
     * 多级缓存管理器（组合本地和分布式缓存）
     * 实际项目中可能需要自定义实现CompositeCacheManager
     * 这里先配置两个独立的CacheManager，由应用层决定使用哪个
     */
    @Bean
    public CacheManager multiLevelCacheManager(
            CacheManager localCacheManager,
            CacheManager redisCacheManager) {
        
        // Spring Boot默认支持多个CacheManager
        // 可以通过@CacheConfig或@Cacheable的cacheManager属性指定使用哪个
        // 也可以自定义CompositeCacheManager实现更复杂的多级缓存逻辑
        return localCacheManager;  // 默认使用本地缓存
    }
}