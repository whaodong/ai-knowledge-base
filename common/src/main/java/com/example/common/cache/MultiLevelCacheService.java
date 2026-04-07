package com.example.common.cache;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 多级缓存服务类
 * 实现Cache-Aside模式，包含防穿透、防击穿、防雪崩的防护机制
 * 支持本地缓存（Caffeine）和分布式缓存（Redis）的多级缓存架构
 */
@Slf4j
@Component
public class MultiLevelCacheService {

    private final CacheManager localCacheManager;
    private final CacheManager redisCacheManager;
    private final RedisTemplate<String, Object> redisTemplate;
    
    // 互斥锁池，用于防击穿（分布式锁的本地化简化版）
    private final ConcurrentHashMap<String, ReentrantLock> lockPool = new ConcurrentHashMap<>();
    
    // 空值标记，用于防穿透
    private static final String NULL_VALUE = "__NULL__";
    
    // 随机数生成器，用于防雪崩（过期时间随机化）
    private final Random random = new Random();
    
    // 热点key探测器
    private final Map<String, AtomicAccessCounter> hotKeyDetector = new ConcurrentHashMap<>();
    private final ScheduledExecutorService hotKeyScheduler = Executors.newSingleThreadScheduledExecutor();
    
    // 热点key配置
    private static final int HOT_KEY_THRESHOLD = 100;  // 单位时间内访问次数阈值
    private static final Duration HOT_KEY_WINDOW = Duration.ofMinutes(1);  // 统计时间窗口
    
    public MultiLevelCacheService(
            CacheManager localCacheManager,
            CacheManager redisCacheManager,
            RedisTemplate<String, Object> redisTemplate) {
        this.localCacheManager = localCacheManager;
        this.redisCacheManager = redisCacheManager;
        this.redisTemplate = redisTemplate;
        
        // 启动热点key检测任务
        startHotKeyDetection();
    }
    
    /**
     * 获取缓存数据（多级缓存查询）
     * 遵循Cache-Aside模式：先查缓存，缓存不存在则查数据库，再写回缓存
     * 
     * @param cacheName 缓存名称
     * @param key 缓存键
     * @param valueLoader 数据加载器（缓存不存在时调用）
     * @param ttl 过期时间（秒）
     * @return 缓存数据
     */
    public <T> T get(String cacheName, String key, Supplier<T> valueLoader, long ttl) {
        // 1. 先查本地缓存（L1）
        T value = getFromLocalCache(cacheName, key);
        if (value != null) {
            recordHotKeyAccess(cacheName, key);
            return value instanceof NullWrapper ? null : value;
        }
        
        // 2. 查分布式缓存（L2）
        value = getFromRedisCache(cacheName, key);
        if (value != null) {
            // 回写到本地缓存
            putToLocalCache(cacheName, key, value, ttl / 2);  // 本地缓存时间减半
            recordHotKeyAccess(cacheName, key);
            return value instanceof NullWrapper ? null : value;
        }
        
        // 3. 缓存未命中，执行防击穿逻辑
        return getWithMutexLock(cacheName, key, valueLoader, ttl);
    }
    
    /**
     * 带互斥锁的缓存获取（防击穿）
     */
    private <T> T getWithMutexLock(String cacheName, String key, Supplier<T> valueLoader, long ttl) {
        ReentrantLock lock = lockPool.computeIfAbsent(getFullKey(cacheName, key), k -> new ReentrantLock());
        
        try {
            // 尝试获取锁，超时时间100ms，防止线程堆积
            if (lock.tryLock(100, TimeUnit.MILLISECONDS)) {
                try {
                    // 双重检查：再次检查缓存（可能已被其他线程写入）
                    T value = getFromRedisCache(cacheName, key);
                    if (value != null) {
                        putToLocalCache(cacheName, key, value, ttl / 2);
                        return value instanceof NullWrapper ? null : value;
                    }
                    
                    // 执行数据加载
                    value = valueLoader.get();
                    
                    // 防穿透：缓存空值
                    if (value == null) {
                        log.debug("缓存空值防止缓存穿透: cache={}, key={}", cacheName, key);
                        value = (T) new NullWrapper();
                        ttl = Math.min(ttl, 60);  // 空值缓存时间缩短
                    }
                    
                    // 防雪崩：过期时间随机化（±10%的随机波动）
                    long randomTtl = applyRandomJitter(ttl);
                    
                    // 写入多级缓存
                    putToRedisCache(cacheName, key, value, randomTtl);
                    putToLocalCache(cacheName, key, value, randomTtl / 2);
                    
                    return value instanceof NullWrapper ? null : value;
                } finally {
                    lock.unlock();
                    // 清理锁对象（防止内存泄漏）
                    lockPool.remove(getFullKey(cacheName, key));
                }
            } else {
                // 获取锁失败，降级策略：直接调用加载器或返回默认值
                log.warn("获取缓存锁失败，降级处理: cache={}, key={}", cacheName, key);
                try {
                    return valueLoader.get();
                } catch (Exception e) {
                    log.error("降级处理失败: cache={}, key={}", cacheName, key, e);
                    return null;
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("缓存锁获取被中断: cache={}, key={}", cacheName, key, e);
            return null;
        }
    }
    
    /**
     * 从本地缓存获取数据
     */
    private <T> T getFromLocalCache(String cacheName, String key) {
        try {
            Cache cache = localCacheManager.getCache(cacheName);
            if (cache != null) {
                Cache.ValueWrapper wrapper = cache.get(key);
                if (wrapper != null) {
                    return (T) wrapper.get();
                }
            }
        } catch (Exception e) {
            log.warn("本地缓存读取失败: cache={}, key={}", cacheName, key, e);
        }
        return null;
    }
    
    /**
     * 从Redis缓存获取数据
     */
    private <T> T getFromRedisCache(String cacheName, String key) {
        try {
            String fullKey = getFullKey(cacheName, key);
            return (T) redisTemplate.opsForValue().get(fullKey);
        } catch (Exception e) {
            log.warn("Redis缓存读取失败: cache={}, key={}", cacheName, key, e);
            return null;
        }
    }
    
    /**
     * 写入本地缓存
     */
    private void putToLocalCache(String cacheName, String key, Object value, long ttl) {
        try {
            Cache cache = localCacheManager.getCache(cacheName);
            if (cache != null) {
                cache.put(key, value);
            }
        } catch (Exception e) {
            log.warn("本地缓存写入失败: cache={}, key={}", cacheName, key, e);
        }
    }
    
    /**
     * 写入Redis缓存
     */
    private void putToRedisCache(String cacheName, String key, Object value, long ttl) {
        try {
            String fullKey = getFullKey(cacheName, key);
            redisTemplate.opsForValue().set(fullKey, value, Duration.ofSeconds(ttl));
        } catch (Exception e) {
            log.warn("Redis缓存写入失败: cache={}, key={}", cacheName, key, e);
        }
    }
    
    /**
     * 写入缓存数据（多级缓存写入）
     * 
     * @param cacheName 缓存名称
     * @param key 缓存键
     * @param value 缓存值
     * @param ttl 过期时间（秒）
     */
    public void put(String cacheName, String key, Object value, long ttl) {
        // 防雪崩：过期时间随机化
        long randomTtl = applyRandomJitter(ttl);
        
        // 写入分布式缓存（L2）
        putToRedisCache(cacheName, key, value, randomTtl);
        
        // 写入本地缓存（L1）
        putToLocalCache(cacheName, key, value, randomTtl / 2);
        
        log.debug("多级缓存写入: cache={}, key={}, ttl={}s", cacheName, key, randomTtl);
    }
    
    /**
     * 删除缓存（多级缓存同步删除）
     */
    public void evict(String cacheName, String key) {
        // 删除本地缓存
        Cache localCache = localCacheManager.getCache(cacheName);
        if (localCache != null) {
            localCache.evict(key);
        }
        
        // 删除Redis缓存
        try {
            String fullKey = getFullKey(cacheName, key);
            redisTemplate.delete(fullKey);
        } catch (Exception e) {
            log.warn("Redis缓存删除失败: cache={}, key={}", cacheName, key, e);
        }
        
        log.debug("多级缓存删除: cache={}, key={}", cacheName, key);
    }
    
    /**
     * 批量获取缓存数据
     */
    public <T> Map<String, T> batchGet(String cacheName, List<String> keys, Function<String, T> valueLoader, long ttl) {
        Map<String, T> result = new HashMap<>();
        List<String> missingKeys = new ArrayList<>();
        
        // 1. 批量查询本地缓存
        for (String key : keys) {
            T value = getFromLocalCache(cacheName, key);
            if (value != null) {
                result.put(key, value instanceof NullWrapper ? null : value);
                recordHotKeyAccess(cacheName, key);
            } else {
                missingKeys.add(key);
            }
        }
        
        // 2. 批量查询Redis缓存
        if (!missingKeys.isEmpty()) {
            List<String> redisKeys = missingKeys.stream()
                    .map(key -> getFullKey(cacheName, key))
                    .collect(Collectors.toList());
            
            List<Object> redisValues = redisTemplate.opsForValue().multiGet(redisKeys);
            
            List<String> stillMissingKeys = new ArrayList<>();
            for (int i = 0; i < missingKeys.size(); i++) {
                String key = missingKeys.get(i);
                Object value = redisValues.get(i);
                
                if (value != null) {
                    result.put(key, (T) (value instanceof NullWrapper ? null : value));
                    // 回写本地缓存
                    putToLocalCache(cacheName, key, value, ttl / 2);
                    recordHotKeyAccess(cacheName, key);
                } else {
                    stillMissingKeys.add(key);
                }
            }
            
            // 3. 批量加载缺失数据
            if (!stillMissingKeys.isEmpty()) {
                Map<String, T> loadedValues = stillMissingKeys.stream()
                        .collect(Collectors.toMap(
                                Function.identity(),
                                k -> valueLoader.apply(k)
                        ));
                
                for (Map.Entry<String, T> entry : loadedValues.entrySet()) {
                    String key = entry.getKey();
                    T value = entry.getValue();
                    
                    // 防穿透：缓存空值
                    Object cacheValue = value;
                    long actualTtl = ttl;
                    if (value == null) {
                        cacheValue = new NullWrapper();
                        actualTtl = Math.min(ttl, 60);
                    }
                    
                    // 防雪崩：过期时间随机化
                    long randomTtl = applyRandomJitter(actualTtl);
                    
                    // 写入多级缓存
                    putToRedisCache(cacheName, key, cacheValue, randomTtl);
                    putToLocalCache(cacheName, key, cacheValue, randomTtl / 2);
                    
                    result.put(key, value);
                }
            }
        }
        
        return result;
    }
    
    /**
     * 获取缓存统计信息
     */
    public CacheStats getStats(String cacheName) {
        CacheStats stats = new CacheStats();
        
        try {
            Cache cache = localCacheManager.getCache(cacheName);
            if (cache != null && cache.getNativeCache() instanceof com.github.benmanes.caffeine.cache.Cache) {
                com.github.benmanes.caffeine.cache.Cache nativeCache = 
                        (com.github.benmanes.caffeine.cache.Cache) cache.getNativeCache();
                com.github.benmanes.caffeine.cache.stats.CacheStats caffeineStats = nativeCache.stats();
                
                stats.setHitCount(caffeineStats.hitCount());
                stats.setMissCount(caffeineStats.missCount());
                stats.setHitRate(caffeineStats.hitRate());
                stats.setMissRate(caffeineStats.missRate());
                stats.setRequestCount(caffeineStats.requestCount());
                stats.setEvictionCount(caffeineStats.evictionCount());
                stats.setAverageLoadPenalty(caffeineStats.averageLoadPenalty());
            }
        } catch (Exception e) {
            log.warn("获取缓存统计失败: cache={}", cacheName, e);
        }
        
        return stats;
    }
    
    /**
     * 防雪崩：应用随机抖动（±10%）
     */
    private long applyRandomJitter(long ttl) {
        if (ttl <= 0) return ttl;
        double jitter = 0.9 + random.nextDouble() * 0.2;  // 0.9 ~ 1.1
        return (long) (ttl * jitter);
    }
    
    /**
     * 记录热点key访问
     */
    private void recordHotKeyAccess(String cacheName, String key) {
        String fullKey = getFullKey(cacheName, key);
        AtomicAccessCounter counter = hotKeyDetector.computeIfAbsent(fullKey, 
                k -> new AtomicAccessCounter(HOT_KEY_WINDOW));
        counter.increment();
    }
    
    /**
     * 启动热点key检测任务
     */
    private void startHotKeyDetection() {
        hotKeyScheduler.scheduleAtFixedRate(() -> {
            try {
                detectAndHandleHotKeys();
            } catch (Exception e) {
                log.error("热点key检测任务执行失败", e);
            }
        }, 30, 30, TimeUnit.SECONDS);  // 每30秒检测一次
    }
    
    /**
     * 检测并处理热点key
     */
    private void detectAndHandleHotKeys() {
        List<String> hotKeys = hotKeyDetector.entrySet().stream()
                .filter(entry -> entry.getValue().getCount() >= HOT_KEY_THRESHOLD)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        
        if (!hotKeys.isEmpty()) {
            log.info("检测到热点key: {}", hotKeys);
            
            // 热点key处理策略：本地缓存预热
            for (String fullKey : hotKeys) {
                try {
                    // 从Redis获取数据并预热到本地缓存
                    Object value = redisTemplate.opsForValue().get(fullKey);
                    if (value != null) {
                        // 解析cacheName和key
                        String[] parts = fullKey.split(":");
                        if (parts.length >= 2) {
                            String cacheName = parts[0];
                            String key = parts[1];
                            putToLocalCache(cacheName, key, value, 300);  // 预热5分钟
                        }
                    }
                } catch (Exception e) {
                    log.warn("热点key预热失败: key={}", fullKey, e);
                }
            }
        }
        
        // 清理过期计数器
        hotKeyDetector.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }
    
    /**
     * 构建完整的缓存键
     */
    private String getFullKey(String cacheName, String key) {
        return cacheName + ":" + key;
    }
    
    /**
     * 空值包装类（用于防穿透）
     */
    private static class NullWrapper {
        @Override
        public String toString() {
            return "NullWrapper";
        }
    }
    
    /**
     * 原子访问计数器（用于热点key检测）
     */
    private static class AtomicAccessCounter {
        private final AtomicInteger count = new AtomicInteger(0);
        private final long windowStart;
        private final long windowDuration;
        
        AtomicAccessCounter(Duration window) {
            this.windowStart = System.currentTimeMillis();
            this.windowDuration = window.toMillis();
        }
        
        void increment() {
            count.incrementAndGet();
        }
        
        int getCount() {
            if (isExpired()) {
                return 0;
            }
            return count.get();
        }
        
        boolean isExpired() {
            return System.currentTimeMillis() - windowStart > windowDuration;
        }
    }
    
    /**
     * 缓存统计信息
     */
    @Data
    public static class CacheStats {
        private long hitCount;
        private long missCount;
        private double hitRate;
        private double missRate;
        private long requestCount;
        private long evictionCount;
        private double averageLoadPenalty;
    }
}