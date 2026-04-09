package com.example.rag.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * 缓存配置类
 * 
 * <p>配置多级缓存架构：</p>
 * <ul>
 *   <li>L1: Caffeine本地缓存（毫秒级访问）</li>
 *   <li>L2: Redis分布式缓存（共享数据）</li>
 * </ul>
 */
@Configuration
@EnableCaching
@EnableScheduling
public class CacheConfig {

    /**
     * Caffeine本地缓存管理器（L1缓存）
     */
    @Bean
    public CacheManager caffeineCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        
        cacheManager.setCaffeine(Caffeine.newBuilder()
                // 初始容量
                .initialCapacity(100)
                // 最大容量
                .maximumSize(1000)
                // 写入后过期时间
                .expireAfterWrite(300, TimeUnit.SECONDS)
                // 访问后过期时间
                .expireAfterAccess(180, TimeUnit.SECONDS)
                // 开启统计
                .recordStats()
        );
        
        // 预定义缓存区域
        cacheManager.setCacheNames(java.util.Arrays.asList(
                "embedding",      // 向量嵌入缓存
                "similarity",     // 相似度搜索缓存
                "llm_response",   // LLM响应缓存
                "hotspot",        // 热点查询缓存
                "preheat",        // 预热缓存
                "preload"         // 预加载缓存
        ));
        
        return cacheManager;
    }
    
    /**
     * Redis分布式缓存管理器（L2缓存）
     */
    @Bean
    @ConditionalOnBean(RedisConnectionFactory.class)
    public CacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        // 默认缓存配置
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                // 默认过期时间
                .entryTtl(Duration.ofMinutes(10))
                // 键序列化
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                // 值序列化
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()))
                // 键前缀
                .prefixCacheNameWith("rag:cache:");
        
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .transactionAware()
                .build();
    }
}
