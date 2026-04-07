package com.example.rag.controller;

import com.example.common.cache.PerformanceTestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 缓存性能测试控制器
 * 提供HTTP接口触发和获取缓存性能测试结果
 */
@Slf4j
@RestController
@RequestMapping("/api/cache-test")
@RequiredArgsConstructor
@Tag(name = "缓存性能测试", description = "多级缓存性能测试接口")
public class CacheTestController {

    private final PerformanceTestService performanceTestService;
    
    /**
     * 运行完整的性能测试套件
     */
    @PostMapping("/full-suite")
    @Operation(summary = "运行完整性能测试套件", description = "执行所有性能测试并生成综合报告")
    public ResponseEntity<?> runFullPerformanceTestSuite() {
        log.info("收到完整性能测试套件请求");
        
        try {
            PerformanceTestService.PerformanceReport report = 
                    performanceTestService.runFullTestSuite();
            
            return ResponseEntity.ok(buildResponse("测试完成", report));
        } catch (Exception e) {
            log.error("性能测试执行失败", e);
            return ResponseEntity.internalServerError()
                    .body(buildErrorResponse("测试执行失败: " + e.getMessage()));
        }
    }
    
    /**
     * 运行基础性能测试
     */
    @PostMapping("/baseline")
    @Operation(summary = "运行基础性能测试", description = "对比缓存与数据库访问性能")
    public ResponseEntity<?> runBaselineTest() {
        log.info("收到基础性能测试请求");
        
        try {
            PerformanceTestService.BaselineTestResult result = 
                    performanceTestService.runFullTestSuite().getBaselineTest();
            
            Map<String, Object> data = new HashMap<>();
            data.put("测试类型", "基础性能对比");
            data.put("缓存读取时间(ns)", result.getCacheReadTimeNs());
            data.put("数据库读取时间(ns)", result.getDatabaseReadTimeNs());
            data.put("加速比", String.format("%.2f", result.getSpeedupFactor()) + "x");
            data.put("缓存吞吐量(请求/秒)", String.format("%.2f", result.getThroughputCache()));
            data.put("数据库吞吐量(请求/秒)", String.format("%.2f", result.getThroughputDatabase()));
            
            return ResponseEntity.ok(buildResponse("基础测试完成", data));
        } catch (Exception e) {
            log.error("基础性能测试执行失败", e);
            return ResponseEntity.internalServerError()
                    .body(buildErrorResponse("测试执行失败: " + e.getMessage()));
        }
    }
    
    /**
     * 运行并发压力测试
     */
    @PostMapping("/concurrency")
    @Operation(summary = "运行并发压力测试", description = "测试多线程并发访问下的缓存性能")
    public ResponseEntity<?> runConcurrencyTest() {
        log.info("收到并发压力测试请求");
        
        try {
            PerformanceTestService.ConcurrencyTestResult result = 
                    performanceTestService.runFullTestSuite().getConcurrencyTest();
            
            Map<String, Object> data = new HashMap<>();
            data.put("测试类型", "并发压力测试");
            data.put("线程数", result.getThreadCount());
            data.put("总请求数", result.getRequestCount());
            data.put("成功请求数", result.getSuccessfulRequests());
            data.put("失败请求数", result.getFailedRequests());
            data.put("平均响应时间(ns)", result.getAverageResponseTimeNs());
            data.put("TPS(事务/秒)", String.format("%.2f", result.getThroughput()));
            data.put("P95响应时间(ns)", result.getP95ResponseTimeNs());
            data.put("P99响应时间(ns)", result.getP99ResponseTimeNs());
            
            return ResponseEntity.ok(buildResponse("并发测试完成", data));
        } catch (Exception e) {
            log.error("并发压力测试执行失败", e);
            return ResponseEntity.internalServerError()
                    .body(buildErrorResponse("测试执行失败: " + e.getMessage()));
        }
    }
    
    /**
     * 运行缓存防护机制测试
     */
    @PostMapping("/protection")
    @Operation(summary = "运行防护机制测试", description = "测试缓存穿透、击穿、雪崩防护效果")
    public ResponseEntity<?> runProtectionTest() {
        log.info("收到防护机制测试请求");
        
        try {
            PerformanceTestService.ProtectionTestResult result = 
                    performanceTestService.runFullTestSuite().getProtectionTest();
            
            Map<String, Object> data = new HashMap<>();
            
            // 穿透测试结果
            Map<String, Object> penetration = new HashMap<>();
            penetration.put("请求次数", result.getPenetrationTest().getRequestCount());
            penetration.put("数据库调用次数", result.getPenetrationTest().getDatabaseCalls());
            penetration.put("防护生效", result.getPenetrationTest().isProtectionEffective());
            data.put("缓存穿透测试", penetration);
            
            // 击穿测试结果
            Map<String, Object> breakdown = new HashMap<>();
            breakdown.put("并发线程数", result.getBreakdownTest().getConcurrentThreads());
            breakdown.put("数据库调用次数", result.getBreakdownTest().getDatabaseCalls());
            breakdown.put("防护生效", result.getBreakdownTest().isProtectionEffective());
            data.put("缓存击穿测试", breakdown);
            
            // 雪崩测试结果
            Map<String, Object> avalanche = new HashMap<>();
            avalanche.put("测试key数量", result.getAvalancheTest().getKeyCount());
            avalanche.put("数据库调用次数", result.getAvalancheTest().getDatabaseCalls());
            avalanche.put("防护生效", result.getAvalancheTest().isProtectionEffective());
            data.put("缓存雪崩测试", avalanche);
            
            return ResponseEntity.ok(buildResponse("防护机制测试完成", data));
        } catch (Exception e) {
            log.error("防护机制测试执行失败", e);
            return ResponseEntity.internalServerError()
                    .body(buildErrorResponse("测试执行失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取测试配置信息
     */
    @GetMapping("/config")
    @Operation(summary = "获取测试配置", description = "查看当前性能测试的配置参数")
    public ResponseEntity<?> getTestConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("缓存架构", "多级缓存（L1: Caffeine, L2: Redis）");
        config.put("缓存策略", "Cache-Aside模式");
        config.put("防护机制", "防穿透、防击穿、防雪崩");
        config.put("默认线程数", 50);
        config.put("默认请求数", 1000);
        config.put("默认TTL", "60秒");
        config.put("热点key阈值", "100次/分钟");
        config.put("随机抖动范围", "±10%");
        
        return ResponseEntity.ok(buildResponse("配置信息", config));
    }
    
    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    @Operation(summary = "缓存健康检查", description = "检查缓存系统健康状态")
    public ResponseEntity<?> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("状态", "正常");
        health.put("服务", "多级缓存性能测试");
        health.put("时间戳", System.currentTimeMillis());
        health.put("环境", System.getProperty("spring.profiles.active", "default"));
        
        return ResponseEntity.ok(buildResponse("健康状态", health));
    }
    
    /**
     * 构建成功响应
     */
    private Map<String, Object> buildResponse(String message, Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", message);
        response.put("timestamp", System.currentTimeMillis());
        response.put("data", data);
        return response;
    }
    
    /**
     * 构建错误响应
     */
    private Map<String, Object> buildErrorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        response.put("timestamp", System.currentTimeMillis());
        response.put("data", null);
        return response;
    }
}