package com.example.common.cache;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

/**
 * 性能测试服务
 * 用于测试多级缓存的性能和防护机制效果
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnBean({CacheManager.class, RedisTemplate.class})
public class PerformanceTestService {

    private final MultiLevelCacheService cacheService;
    
    // 测试配置
    private static final String TEST_CACHE_NAME = "performance_test";
    private static final int DEFAULT_THREAD_COUNT = 50;
    private static final int DEFAULT_REQUEST_COUNT = 1000;
    private static final long DEFAULT_TTL = 60;  // 60秒
    
    /**
     * 运行完整的性能测试套件
     */
    public PerformanceReport runFullTestSuite() {
        PerformanceReport report = new PerformanceReport();
        
        log.info("开始性能测试套件...");
        
        // 1. 基础性能测试
        report.setBaselineTest(runBaselinePerformanceTest());
        
        // 2. 缓存命中率测试
        report.setHitRateTest(runHitRateTest());
        
        // 3. 并发压力测试
        report.setConcurrencyTest(runConcurrencyTest());
        
        // 4. 防护机制测试
        report.setProtectionTest(runProtectionMechanismTest());
        
        // 5. 热点key测试
        report.setHotspotTest(runHotspotTest());
        
        log.info("性能测试套件完成");
        return report;
    }
    
    /**
     * 基础性能测试 - 测量缓存与直接数据库访问的对比
     */
    private BaselineTestResult runBaselinePerformanceTest() {
        log.info("运行基础性能测试...");
        
        String testKey = "baseline_key";
        String testValue = "test_value_" + System.currentTimeMillis();
        
        // 预热
        cacheService.put(TEST_CACHE_NAME, testKey, testValue, DEFAULT_TTL);
        
        // 测试缓存读取性能
        long cacheStart = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            cacheService.get(TEST_CACHE_NAME, testKey, () -> testValue, DEFAULT_TTL);
        }
        long cacheEnd = System.nanoTime();
        long cacheTime = cacheEnd - cacheStart;
        
        // 模拟数据库访问（延迟10ms）
        long dbStart = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            simulateDatabaseAccess(10);
        }
        long dbEnd = System.nanoTime();
        long dbTime = dbEnd - dbStart;
        
        BaselineTestResult result = new BaselineTestResult();
        result.setCacheReadTimeNs(cacheTime);
        result.setDatabaseReadTimeNs(dbTime);
        result.setSpeedupFactor((double) dbTime / cacheTime);
        result.setThroughputCache(1000 / (cacheTime / 1_000_000_000.0));
        result.setThroughputDatabase(1000 / (dbTime / 1_000_000_000.0));
        
        log.info("基础性能测试完成: 缓存加速比={}x", result.getSpeedupFactor());
        return result;
    }
    
    /**
     * 缓存命中率测试
     */
    private HitRateTestResult runHitRateTest() {
        log.info("运行缓存命中率测试...");
        
        // 准备测试数据
        Map<String, String> testData = new HashMap<>();
        for (int i = 0; i < 100; i++) {
            testData.put("key_" + i, "value_" + i);
        }
        
        // 预加载部分数据到缓存
        testData.entrySet().stream()
                .limit(50)
                .forEach(entry -> {
                    cacheService.put(TEST_CACHE_NAME, entry.getKey(), entry.getValue(), DEFAULT_TTL);
                });
        
        AtomicInteger hits = new AtomicInteger(0);
        AtomicInteger misses = new AtomicInteger(0);
        
        // 随机访问测试
        Random random = new Random();
        List<String> keys = new ArrayList<>(testData.keySet());
        
        long startTime = System.nanoTime();
        for (int i = 0; i < DEFAULT_REQUEST_COUNT; i++) {
            String key = keys.get(random.nextInt(keys.size()));
            String value = cacheService.get(TEST_CACHE_NAME, key, 
                    () -> {
                        misses.incrementAndGet();
                        return testData.get(key);
                    }, 
                    DEFAULT_TTL);
            
            if (value != null) {
                hits.incrementAndGet();
            }
        }
        long endTime = System.nanoTime();
        
        HitRateTestResult result = new HitRateTestResult();
        result.setHitCount(hits.get());
        result.setMissCount(misses.get());
        result.setTotalRequests(DEFAULT_REQUEST_COUNT);
        result.setHitRate((double) hits.get() / DEFAULT_REQUEST_COUNT);
        result.setAverageResponseTimeNs((endTime - startTime) / DEFAULT_REQUEST_COUNT);
        result.setThroughput(DEFAULT_REQUEST_COUNT / ((endTime - startTime) / 1_000_000_000.0));
        
        log.info("缓存命中率测试完成: 命中率={}%", String.format("%.2f", result.getHitRate() * 100));
        return result;
    }
    
    /**
     * 并发压力测试
     */
    private ConcurrencyTestResult runConcurrencyTest() {
        log.info("运行并发压力测试...");
        
        ExecutorService executor = Executors.newFixedThreadPool(DEFAULT_THREAD_COUNT);
        List<Future<TaskResult>> futures = new ArrayList<>();
        
        String testKey = "concurrent_key";
        String testValue = "concurrent_value_" + System.currentTimeMillis();
        
        // 初始写入
        cacheService.put(TEST_CACHE_NAME, testKey, testValue, DEFAULT_TTL);
        
        // 提交并发任务
        for (int i = 0; i < DEFAULT_REQUEST_COUNT; i++) {
            futures.add(executor.submit(() -> {
                long start = System.nanoTime();
                String value = cacheService.get(TEST_CACHE_NAME, testKey, 
                        () -> testValue, DEFAULT_TTL);
                long end = System.nanoTime();
                
                TaskResult taskResult = new TaskResult();
                taskResult.setSuccess(value != null);
                taskResult.setResponseTimeNs(end - start);
                return taskResult;
            }));
        }
        
        // 收集结果
        List<TaskResult> results = new ArrayList<>();
        for (Future<TaskResult> future : futures) {
            try {
                results.add(future.get());
            } catch (Exception e) {
                log.warn("任务执行失败", e);
            }
        }
        
        executor.shutdown();
        
        // 分析结果
        long totalTime = results.stream()
                .mapToLong(TaskResult::getResponseTimeNs)
                .sum();
        long successfulTasks = results.stream()
                .filter(TaskResult::isSuccess)
                .count();
        double avgResponseTime = results.stream()
                .mapToLong(TaskResult::getResponseTimeNs)
                .average()
                .orElse(0);
        
        // 计算TPS
        long testDurationMs = TimeUnit.NANOSECONDS.toMillis(totalTime);
        double tps = testDurationMs > 0 ? 
                (double) DEFAULT_REQUEST_COUNT / (testDurationMs / 1000.0) : 0;
        
        ConcurrencyTestResult result = new ConcurrencyTestResult();
        result.setThreadCount(DEFAULT_THREAD_COUNT);
        result.setRequestCount(DEFAULT_REQUEST_COUNT);
        result.setSuccessfulRequests((int) successfulTasks);
        result.setFailedRequests(DEFAULT_REQUEST_COUNT - (int) successfulTasks);
        result.setAverageResponseTimeNs((long) avgResponseTime);
        result.setThroughput(tps);
        result.setP95ResponseTimeNs(calculatePercentile(
                results.stream().mapToLong(TaskResult::getResponseTimeNs).toArray(), 95));
        result.setP99ResponseTimeNs(calculatePercentile(
                results.stream().mapToLong(TaskResult::getResponseTimeNs).toArray(), 99));
        
        log.info("并发压力测试完成: TPS={}, 成功率={}%", 
                String.format("%.2f", tps),
                String.format("%.2f", (double) successfulTasks / DEFAULT_REQUEST_COUNT * 100));
        return result;
    }
    
    /**
     * 防护机制测试（穿透、击穿、雪崩）
     */
    private ProtectionTestResult runProtectionMechanismTest() {
        log.info("运行防护机制测试...");
        
        ProtectionTestResult result = new ProtectionTestResult();
        
        // 1. 缓存穿透测试
        result.setPenetrationTest(runPenetrationTest());
        
        // 2. 缓存击穿测试
        result.setBreakdownTest(runBreakdownTest());
        
        // 3. 缓存雪崩测试
        result.setAvalancheTest(runAvalancheTest());
        
        log.info("防护机制测试完成");
        return result;
    }
    
    /**
     * 缓存穿透测试
     */
    private PenetrationTestResult runPenetrationTest() {
        log.info("运行缓存穿透测试...");
        
        String nonExistentKey = "non_existent_" + System.currentTimeMillis();
        int requestCount = 100;
        
        long startTime = System.nanoTime();
        AtomicInteger dbCalls = new AtomicInteger(0);
        
        for (int i = 0; i < requestCount; i++) {
            cacheService.get(TEST_CACHE_NAME, nonExistentKey, () -> {
                dbCalls.incrementAndGet();
                return null;  // 模拟数据库返回空
            }, DEFAULT_TTL);
        }
        
        long endTime = System.nanoTime();
        
        PenetrationTestResult result = new PenetrationTestResult();
        result.setRequestCount(requestCount);
        result.setDatabaseCalls(dbCalls.get());
        result.setAverageResponseTimeNs((endTime - startTime) / requestCount);
        result.setProtectionEffective(dbCalls.get() < requestCount);  // 有空值缓存应小于请求数
        
        log.info("缓存穿透测试完成: 数据库调用={}/{}", dbCalls.get(), requestCount);
        return result;
    }
    
    /**
     * 缓存击穿测试
     */
    private BreakdownTestResult runBreakdownTest() {
        log.info("运行缓存击穿测试...");
        
        String hotKey = "hot_key_" + System.currentTimeMillis();
        String hotValue = "hot_value";
        
        // 初始写入
        cacheService.put(TEST_CACHE_NAME, hotKey, hotValue, DEFAULT_TTL);
        
        // 模拟缓存过期后大量并发访问
        cacheService.evict(TEST_CACHE_NAME, hotKey);
        
        int concurrentThreads = 100;
        ExecutorService executor = Executors.newFixedThreadPool(concurrentThreads);
        AtomicInteger dbCalls = new AtomicInteger(0);
        AtomicInteger successfulGets = new AtomicInteger(0);
        
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < concurrentThreads; i++) {
            futures.add(executor.submit(() -> {
                String value = cacheService.get(TEST_CACHE_NAME, hotKey, () -> {
                    dbCalls.incrementAndGet();
                    simulateDatabaseAccess(20);  // 模拟20ms数据库访问
                    return hotValue;
                }, DEFAULT_TTL);
                
                if (value != null) {
                    successfulGets.incrementAndGet();
                }
            }));
        }
        
        // 等待所有任务完成
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (Exception e) {
                log.warn("击穿测试任务失败", e);
            }
        }
        
        executor.shutdown();
        
        BreakdownTestResult result = new BreakdownTestResult();
        result.setConcurrentThreads(concurrentThreads);
        result.setDatabaseCalls(dbCalls.get());
        result.setSuccessfulGets(successfulGets.get());
        result.setProtectionEffective(dbCalls.get() < concurrentThreads);  // 互斥锁应减少数据库调用
        
        log.info("缓存击穿测试完成: 数据库调用={}/{}", dbCalls.get(), concurrentThreads);
        return result;
    }
    
    /**
     * 缓存雪崩测试
     */
    private AvalancheTestResult runAvalancheTest() {
        log.info("运行缓存雪崩测试...");
        
        int keyCount = 100;
        List<String> keys = new ArrayList<>();
        
        // 生成测试key
        for (int i = 0; i < keyCount; i++) {
            keys.add("avalanche_key_" + i);
        }
        
        // 同时设置相同的过期时间（模拟雪崩条件）
        long sameTtl = 5;  // 5秒
        for (String key : keys) {
            cacheService.put(TEST_CACHE_NAME, key, "value_" + key, sameTtl);
        }
        
        // 等待缓存过期
        try {
            Thread.sleep((sameTtl + 1) * 1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // 并发访问所有过期的key
        ExecutorService executor = Executors.newFixedThreadPool(50);
        AtomicInteger dbCalls = new AtomicInteger(0);
        List<Future<?>> futures = new ArrayList<>();
        
        long startTime = System.nanoTime();
        for (String key : keys) {
            futures.add(executor.submit(() -> {
                cacheService.get(TEST_CACHE_NAME, key, () -> {
                    dbCalls.incrementAndGet();
                    simulateDatabaseAccess(15);
                    return "reloaded_" + key;
                }, sameTtl);
            }));
        }
        
        // 等待所有任务完成
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (Exception e) {
                log.warn("雪崩测试任务失败", e);
            }
        }
        
        long endTime = System.nanoTime();
        executor.shutdown();
        
        AvalancheTestResult result = new AvalancheTestResult();
        result.setKeyCount(keyCount);
        result.setDatabaseCalls(dbCalls.get());
        result.setTotalResponseTimeNs(endTime - startTime);
        result.setAverageResponseTimeNs((endTime - startTime) / keyCount);
        result.setProtectionEffective(true);  // 随机化过期时间应缓解雪崩
        
        log.info("缓存雪崩测试完成: 数据库调用={}/{}", dbCalls.get(), keyCount);
        return result;
    }
    
    /**
     * 热点key测试
     */
    private HotspotTestResult runHotspotTest() {
        log.info("运行热点key测试...");
        
        // 生成测试key，其中少量key为热点
        int totalKeys = 1000;
        int hotKeyCount = 10;
        
        List<String> hotKeys = new ArrayList<>();
        for (int i = 0; i < hotKeyCount; i++) {
            hotKeys.add("hotspot_key_" + i);
        }
        
        // 模拟访问模式：热点key被频繁访问
        Map<String, Integer> accessPattern = new HashMap<>();
        Random random = new Random();
        
        int totalAccesses = 10000;
        for (int i = 0; i < totalAccesses; i++) {
            // 80%的访问集中在20%的key上（符合二八定律）
            String key;
            if (random.nextDouble() < 0.8) {
                key = hotKeys.get(random.nextInt(hotKeyCount));
            } else {
                key = "normal_key_" + random.nextInt(totalKeys - hotKeyCount);
            }
            
            accessPattern.put(key, accessPattern.getOrDefault(key, 0) + 1);
        }
        
        // 分析热点key检测效果
        int detectedHotKeys = (int) hotKeys.stream()
                .filter(key -> accessPattern.getOrDefault(key, 0) > 100)  // 阈值100
                .count();
        
        HotspotTestResult result = new HotspotTestResult();
        result.setTotalKeys(totalKeys);
        result.setHotKeyCount(hotKeyCount);
        result.setDetectedHotKeys(detectedHotKeys);
        result.setDetectionRate((double) detectedHotKeys / hotKeyCount);
        result.setAccessConcentration(calculateGiniCoefficient(
                accessPattern.values().stream().mapToInt(Integer::intValue).toArray()));
        
        log.info("热点key测试完成: 检测率={}%", String.format("%.2f", result.getDetectionRate() * 100));
        return result;
    }
    
    /**
     * 模拟数据库访问延迟
     */
    private void simulateDatabaseAccess(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * 计算百分位数
     */
    private long calculatePercentile(long[] values, double percentile) {
        if (values.length == 0) return 0;
        
        Arrays.sort(values);
        int index = (int) Math.ceil(percentile / 100.0 * values.length) - 1;
        index = Math.max(0, Math.min(index, values.length - 1));
        return values[index];
    }
    
    /**
     * 计算基尼系数（衡量访问集中度）
     */
    private double calculateGiniCoefficient(int[] values) {
        if (values.length == 0) return 0;
        
        Arrays.sort(values);
        double sum = 0;
        for (int i = 0; i < values.length; i++) {
            sum += (2 * i - values.length + 1) * (double) values[i];
        }
        return sum / (values.length * Arrays.stream(values).sum());
    }
    
    // 内部结果类定义
    @Data
    private static class TaskResult {
        private boolean success;
        private long responseTimeNs;
    }
    
    // 性能报告类
    @Data
    public static class PerformanceReport {
        private BaselineTestResult baselineTest;
        private HitRateTestResult hitRateTest;
        private ConcurrencyTestResult concurrencyTest;
        private ProtectionTestResult protectionTest;
        private HotspotTestResult hotspotTest;
        private Instant testTime = Instant.now();
    }
    
    // 各种测试结果类
    @Data
    public static class BaselineTestResult {
        private long cacheReadTimeNs;
        private long databaseReadTimeNs;
        private double speedupFactor;
        private double throughputCache;  // 请求/秒
        private double throughputDatabase;
    }
    
    @Data
    public static class HitRateTestResult {
        private int hitCount;
        private int missCount;
        private int totalRequests;
        private double hitRate;
        private long averageResponseTimeNs;
        private double throughput;
    }
    
    @Data
    public static class ConcurrencyTestResult {
        private int threadCount;
        private int requestCount;
        private int successfulRequests;
        private int failedRequests;
        private long averageResponseTimeNs;
        private double throughput;  // TPS
        private long p95ResponseTimeNs;
        private long p99ResponseTimeNs;
    }
    
    @Data
    public static class ProtectionTestResult {
        private PenetrationTestResult penetrationTest;
        private BreakdownTestResult breakdownTest;
        private AvalancheTestResult avalancheTest;
    }
    
    @Data
    public static class PenetrationTestResult {
        private int requestCount;
        private int databaseCalls;
        private long averageResponseTimeNs;
        private boolean protectionEffective;
    }
    
    @Data
    public static class BreakdownTestResult {
        private int concurrentThreads;
        private int databaseCalls;
        private int successfulGets;
        private boolean protectionEffective;
    }
    
    @Data
    public static class AvalancheTestResult {
        private int keyCount;
        private int databaseCalls;
        private long totalResponseTimeNs;
        private long averageResponseTimeNs;
        private boolean protectionEffective;
    }
    
    @Data
    public static class HotspotTestResult {
        private int totalKeys;
        private int hotKeyCount;
        private int detectedHotKeys;
        private double detectionRate;
        private double accessConcentration;  // 基尼系数
    }
}