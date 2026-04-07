package com.example.rag.cache;

import com.example.common.cache.MultiLevelCacheService;
import com.example.common.cache.MultiLevelCacheService.CacheStats;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisServerCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 缓存监控服务
 * 
 * <p>实现全面的缓存监控机制：</p>
 * <ul>
 *   <li>缓存命中率统计</li>
 *   <li>预热任务执行状态</li>
 *   <li>缓存大小监控</li>
 *   <li>性能指标收集</li>
 *   <li>告警机制</li>
 * </ul>
 */
@Slf4j
@Service
public class CacheMonitorService {

    private final MultiLevelCacheService cacheService;
    private final RedisTemplate<String, Object> redisTemplate;
    private final HotQueryDetector hotQueryDetector;
    private final CachePreheatService cachePreheatService;
    private final PreloadService preloadService;
    
    // 缓存区域
    private static final String CACHE_EMBEDDING = "embedding";
    private static final String CACHE_SIMILARITY = "similarity";
    private static final String CACHE_PREHEAT = "preheat";
    private static final String CACHE_PRELOAD = "preload";
    
    // 监控指标
    private final Map<String, CacheMetrics> cacheMetricsMap = new ConcurrentHashMap<>();
    private final AtomicLong totalCacheRequests = new AtomicLong(0);
    private final AtomicLong totalCacheHits = new AtomicLong(0);
    private final AtomicLong totalCacheMisses = new AtomicLong(0);
    
    // 告警阈值
    private static final double LOW_HIT_RATE_THRESHOLD = 0.5;    // 低命中率阈值
    private static final long HIGH_MEMORY_THRESHOLD = 10000;      // 高内存使用阈值（MB）
    private static final int ALERT_COOLDOWN_MINUTES = 10;         // 告警冷却时间
    
    // 告警记录
    private final Map<String, LocalDateTime> lastAlertTime = new ConcurrentHashMap<>();
    
    public CacheMonitorService(
            MultiLevelCacheService cacheService,
            RedisTemplate<String, Object> redisTemplate,
            HotQueryDetector hotQueryDetector,
            CachePreheatService cachePreheatService,
            PreloadService preloadService) {
        this.cacheService = cacheService;
        this.redisTemplate = redisTemplate;
        this.hotQueryDetector = hotQueryDetector;
        this.cachePreheatService = cachePreheatService;
        this.preloadService = preloadService;
    }
    
    /**
     * 定时收集缓存指标
     * 每5分钟执行一次
     */
    @Scheduled(fixedRate = 300000)
    public void collectMetrics() {
        log.info("开始收集缓存指标...");
        
        try {
            // 收集各缓存区域的指标
            collectCacheMetrics(CACHE_EMBEDDING, "向量嵌入缓存");
            collectCacheMetrics(CACHE_SIMILARITY, "相似度搜索缓存");
            collectCacheMetrics(CACHE_PREHEAT, "预热缓存");
            collectCacheMetrics(CACHE_PRELOAD, "预加载缓存");
            
            // 收集Redis统计
            collectRedisMetrics();
            
            // 检查告警条件
            checkAlertConditions();
            
            log.info("缓存指标收集完成");
            
        } catch (Exception e) {
            log.error("收集缓存指标失败", e);
        }
    }
    
    /**
     * 收集单个缓存区域的指标
     */
    private void collectCacheMetrics(String cacheName, String displayName) {
        try {
            CacheStats stats = cacheService.getStats(cacheName);
            
            CacheMetrics metrics = CacheMetrics.builder()
                    .cacheName(cacheName)
                    .displayName(displayName)
                    .hitCount(stats.getHitCount())
                    .missCount(stats.getMissCount())
                    .hitRate(stats.getHitRate())
                    .requestCount(stats.getRequestCount())
                    .evictionCount(stats.getEvictionCount())
                    .averageLoadPenalty(stats.getAverageLoadPenalty())
                    .timestamp(LocalDateTime.now())
                    .build();
            
            cacheMetricsMap.put(cacheName, metrics);
            
            // 更新全局统计
            totalCacheRequests.addAndGet(stats.getRequestCount());
            totalCacheHits.addAndGet(stats.getHitCount());
            totalCacheMisses.addAndGet(stats.getMissCount());
            
            log.debug("缓存指标收集完成: cache={}, hitRate={}", cacheName, stats.getHitRate());
            
        } catch (Exception e) {
            log.warn("收集缓存指标失败: cache={}", cacheName, e);
        }
    }
    
    /**
     * 收集Redis统计信息
     */
    private void collectRedisMetrics() {
        try {
            // 获取Redis信息
            Properties info = redisTemplate.execute((RedisCallback<Properties>) connection -> connection.serverCommands().info());
            
            if (info != null) {
                long usedMemory = parseMemory(info.getProperty("used_memory", "0"));
                long totalMemory = parseMemory(info.getProperty("total_system_memory", "0"));
                long keysCount = parseLong(info.getProperty("db0", "keys=0").split(",")[0].split("=")[1]);
                
                log.info("Redis统计: usedMemory={}MB, keysCount={}", 
                        usedMemory / 1024 / 1024, keysCount);
            }
            
        } catch (Exception e) {
            log.warn("收集Redis统计信息失败", e);
        }
    }
    
    /**
     * 检查告警条件
     */
    private void checkAlertConditions() {
        // 检查各缓存区域的命中率
        for (CacheMetrics metrics : cacheMetricsMap.values()) {
            if (metrics.getHitRate() < LOW_HIT_RATE_THRESHOLD && metrics.getRequestCount() > 100) {
                sendAlert("LOW_HIT_RATE", 
                        String.format("缓存 %s 命中率过低: %.2f%%", 
                                metrics.getDisplayName(), metrics.getHitRate() * 100));
            }
        }
        
        // 检查Redis内存使用
        try {
            Properties info = redisTemplate.execute((RedisCallback<Properties>) connection -> connection.serverCommands().info());
            if (info != null) {
                long usedMemoryMB = parseMemory(info.getProperty("used_memory", "0")) / 1024 / 1024;
                if (usedMemoryMB > HIGH_MEMORY_THRESHOLD) {
                    sendAlert("HIGH_MEMORY", 
                            String.format("Redis内存使用过高: %dMB", usedMemoryMB));
                }
            }
        } catch (Exception e) {
            log.debug("检查Redis内存使用失败", e);
        }
    }
    
    /**
     * 发送告警
     */
    private void sendAlert(String alertType, String message) {
        // 检查告警冷却时间
        LocalDateTime lastTime = lastAlertTime.get(alertType);
        if (lastTime != null && 
            lastTime.plusMinutes(ALERT_COOLDOWN_MINUTES).isAfter(LocalDateTime.now())) {
            return;
        }
        
        log.warn("【缓存告警】{}", message);
        lastAlertTime.put(alertType, LocalDateTime.now());
    }
    
    /**
     * 获取缓存总览报告
     */
    public CacheOverview getCacheOverview() {
        // 计算总体命中率
        long totalRequests = totalCacheRequests.get();
        long totalHits = totalCacheHits.get();
        double overallHitRate = totalRequests > 0 ? (double) totalHits / totalRequests : 0.0;
        
        // 获取各缓存区域指标
        List<CacheMetrics> allMetrics = new ArrayList<>(cacheMetricsMap.values());
        
        // 获取预热统计
        CachePreheatService.PreheatStats preheatStats = cachePreheatService.getStats();
        
        // 获取预加载统计
        PreloadService.PreloadStats preloadStats = preloadService.getStats();
        
        // 获取热点查询统计
        HotQueryDetector.HotQueryStats hotQueryStats = hotQueryDetector.getStats();
        
        return CacheOverview.builder()
                .overallHitRate(overallHitRate)
                .totalCacheRequests(totalRequests)
                .totalCacheHits(totalHits)
                .totalCacheMisses(totalCacheMisses.get())
                .cacheMetrics(allMetrics)
                .preheatStats(preheatStats)
                .preloadStats(preloadStats)
                .hotQueryStats(hotQueryStats)
                .timestamp(LocalDateTime.now())
                .build();
    }
    
    /**
     * 获取特定缓存的指标
     */
    public CacheMetrics getCacheMetrics(String cacheName) {
        return cacheMetricsMap.get(cacheName);
    }
    
    /**
     * 获取所有缓存指标
     */
    public List<CacheMetrics> getAllCacheMetrics() {
        return new ArrayList<>(cacheMetricsMap.values());
    }
    
    /**
     * 记录缓存访问
     * 用于手动记录缓存命中/未命中
     */
    public void recordCacheAccess(String cacheName, boolean hit) {
        totalCacheRequests.incrementAndGet();
        if (hit) {
            totalCacheHits.incrementAndGet();
        } else {
            totalCacheMisses.incrementAndGet();
        }
    }
    
    /**
     * 重置统计数据
     */
    public void resetStats() {
        totalCacheRequests.set(0);
        totalCacheHits.set(0);
        totalCacheMisses.set(0);
        cacheMetricsMap.clear();
        lastAlertTime.clear();
        
        log.info("缓存监控统计已重置");
    }
    
    /**
     * 导出监控报告
     */
    public String exportReport() {
        StringBuilder report = new StringBuilder();
        report.append("=== 缓存监控报告 ===\n");
        report.append("生成时间: ").append(LocalDateTime.now()).append("\n\n");
        
        CacheOverview overview = getCacheOverview();
        
        report.append("【总体统计】\n");
        report.append(String.format("总请求数: %d\n", overview.getTotalCacheRequests()));
        report.append(String.format("总命中数: %d\n", overview.getTotalCacheHits()));
        report.append(String.format("总未命中数: %d\n", overview.getTotalCacheMisses()));
        report.append(String.format("总体命中率: %.2f%%\n\n", overview.getOverallHitRate() * 100));
        
        report.append("【各缓存区域指标】\n");
        for (CacheMetrics metrics : overview.getCacheMetrics()) {
            report.append(String.format("- %s: 命中率=%.2f%%, 请求数=%d, 淘汰数=%d\n",
                    metrics.getDisplayName(),
                    metrics.getHitRate() * 100,
                    metrics.getRequestCount(),
                    metrics.getEvictionCount()));
        }
        
        report.append("\n【预热统计】\n");
        report.append(String.format("成功: %d, 失败: %d\n",
                overview.getPreheatStats().getSuccessCount(),
                overview.getPreheatStats().getFailureCount()));
        
        report.append("\n【预加载统计】\n");
        report.append(String.format("总预加载数: %d, 命中数: %d, 命中率: %.2f%%\n",
                overview.getPreloadStats().getTotalPreloaded(),
                overview.getPreloadStats().getTotalHits(),
                overview.getPreloadStats().getHitRate() * 100));
        
        report.append("\n【热点查询统计】\n");
        report.append(String.format("热点查询数: %d\n",
                overview.getHotQueryStats().getTotalHotQueries()));
        
        return report.toString();
    }
    
    /**
     * 解析内存大小
     */
    private long parseMemory(String memoryStr) {
        try {
            return Long.parseLong(memoryStr);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    
    /**
     * 解析长整型
     */
    private long parseLong(String str) {
        try {
            return Long.parseLong(str);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    
    /**
     * 缓存指标
     */
    @Data
    @lombok.Builder
    public static class CacheMetrics {
        private String cacheName;
        private String displayName;
        private long hitCount;
        private long missCount;
        private double hitRate;
        private long requestCount;
        private long evictionCount;
        private double averageLoadPenalty;
        private LocalDateTime timestamp;
    }
    
    /**
     * 缓存总览
     */
    @Data
    @lombok.Builder
    public static class CacheOverview {
        private double overallHitRate;
        private long totalCacheRequests;
        private long totalCacheHits;
        private long totalCacheMisses;
        private List<CacheMetrics> cacheMetrics;
        private CachePreheatService.PreheatStats preheatStats;
        private PreloadService.PreloadStats preloadStats;
        private HotQueryDetector.HotQueryStats hotQueryStats;
        private LocalDateTime timestamp;
    }
}
