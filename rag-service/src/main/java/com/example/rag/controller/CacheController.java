package com.example.rag.controller;

import com.example.rag.cache.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 缓存管理控制器
 * 
 * <p>提供缓存预热、监控和管理的REST API</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/cache")
@RequiredArgsConstructor
public class CacheController {

    private final CachePreheatService cachePreheatService;
    private final PreloadService preloadService;
    private final CacheMonitorService cacheMonitorService;
    private final HotQueryDetector hotQueryDetector;
    
    /**
     * 手动预热指定查询
     * 
     * @param query 查询文本
     * @return 预热任务ID
     */
    @PostMapping("/preheat")
    public ResponseEntity<Map<String, Object>> preheatQuery(@RequestParam String query) {
        log.info("手动预热查询: {}", query);
        
        try {
            String taskId = cachePreheatService.manualPreheat(query);
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "taskId", taskId,
                    "message", "预热任务已提交"
            ));
        } catch (Exception e) {
            log.error("手动预热失败: query={}", query, e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }
    
    /**
     * 批量预热查询
     * 
     * @param queries 查询列表
     * @return 预热任务ID列表
     */
    @PostMapping("/preheat/batch")
    public ResponseEntity<Map<String, Object>> batchPreheat(@RequestBody List<String> queries) {
        log.info("批量预热查询: count={}", queries.size());
        
        try {
            List<String> taskIds = cachePreheatService.batchPreheat(queries);
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "taskIds", taskIds,
                    "message", "批量预热任务已提交"
            ));
        } catch (Exception e) {
            log.error("批量预热失败", e);
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }
    
    /**
     * 获取预热任务状态
     * 
     * @param taskId 任务ID
     * @return 预热任务信息
     */
    @GetMapping("/preheat/task/{taskId}")
    public ResponseEntity<Map<String, Object>> getPreheatTaskStatus(@PathVariable String taskId) {
        CachePreheatService.PreheatTask task = cachePreheatService.getTaskStatus(taskId);
        
        if (task == null) {
            return ResponseEntity.notFound().build();
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("taskId", task.getTaskId());
        result.put("query", task.getQuery());
        result.put("status", task.getStatus());
        result.put("startTime", task.getStartTime());
        result.put("endTime", task.getEndTime());
        result.put("error", task.getError());
        
        return ResponseEntity.ok(result);
    }
    
    /**
     * 获取所有运行中的预热任务
     * 
     * @return 预热任务列表
     */
    @GetMapping("/preheat/tasks")
    public ResponseEntity<List<CachePreheatService.PreheatTask>> getAllPreheatTasks() {
        return ResponseEntity.ok(cachePreheatService.getAllRunningTasks());
    }
    
    /**
     * 获取预热统计信息
     * 
     * @return 预热统计
     */
    @GetMapping("/preheat/stats")
    public ResponseEntity<CachePreheatService.PreheatStats> getPreheatStats() {
        return ResponseEntity.ok(cachePreheatService.getStats());
    }
    
    /**
     * 获取缓存监控总览
     * 
     * @return 缓存总览报告
     */
    @GetMapping("/monitor/overview")
    public ResponseEntity<CacheMonitorService.CacheOverview> getCacheOverview() {
        return ResponseEntity.ok(cacheMonitorService.getCacheOverview());
    }
    
    /**
     * 获取特定缓存的指标
     * 
     * @param cacheName 缓存名称
     * @return 缓存指标
     */
    @GetMapping("/monitor/metrics/{cacheName}")
    public ResponseEntity<CacheMonitorService.CacheMetrics> getCacheMetrics(
            @PathVariable String cacheName) {
        CacheMonitorService.CacheMetrics metrics = cacheMonitorService.getCacheMetrics(cacheName);
        
        if (metrics == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(metrics);
    }
    
    /**
     * 获取所有缓存指标
     * 
     * @return 缓存指标列表
     */
    @GetMapping("/monitor/metrics")
    public ResponseEntity<List<CacheMonitorService.CacheMetrics>> getAllCacheMetrics() {
        return ResponseEntity.ok(cacheMonitorService.getAllCacheMetrics());
    }
    
    /**
     * 导出缓存监控报告
     * 
     * @return 文本格式的监控报告
     */
    @GetMapping("/monitor/report")
    public ResponseEntity<String> exportMonitorReport() {
        return ResponseEntity.ok(cacheMonitorService.exportReport());
    }
    
    /**
     * 获取Top N热点查询
     * 
     * @param n 查询数量（默认20）
     * @return 热点查询列表
     */
    @GetMapping("/hot-queries")
    public ResponseEntity<List<HotQueryDetector.HotQuery>> getTopHotQueries(
            @RequestParam(defaultValue = "20") int n) {
        return ResponseEntity.ok(hotQueryDetector.getTopHotQueries(n));
    }
    
    /**
     * 检查是否为热点查询
     * 
     * @param query 查询文本
     * @return 是否为热点查询
     */
    @GetMapping("/hot-queries/check")
    public ResponseEntity<Map<String, Object>> checkHotQuery(@RequestParam String query) {
        boolean isHot = hotQueryDetector.isHotQuery(query);
        int frequency = hotQueryDetector.getQueryFrequency(query);
        
        return ResponseEntity.ok(Map.of(
                "query", query,
                "isHot", isHot,
                "frequency", frequency
        ));
    }
    
    /**
     * 获取相关查询预测
     * 
     * @param query 查询文本
     * @return 相关查询列表
     */
    @GetMapping("/related-queries")
    public ResponseEntity<List<HotQueryDetector.RelatedQuery>> getRelatedQueries(
            @RequestParam String query) {
        return ResponseEntity.ok(hotQueryDetector.getRelatedQueries(query));
    }
    
    /**
     * 获取热点查询统计信息
     * 
     * @return 热点查询统计
     */
    @GetMapping("/hot-queries/stats")
    public ResponseEntity<HotQueryDetector.HotQueryStats> getHotQueryStats() {
        return ResponseEntity.ok(hotQueryDetector.getStats());
    }
    
    /**
     * 获取预加载统计信息
     * 
     * @return 预加载统计
     */
    @GetMapping("/preload/stats")
    public ResponseEntity<PreloadService.PreloadStats> getPreloadStats() {
        return ResponseEntity.ok(preloadService.getStats());
    }
    
    /**
     * 获取预加载结果列表
     * 
     * @return 预加载结果列表
     */
    @GetMapping("/preload/results")
    public ResponseEntity<List<PreloadService.PreloadResult>> getPreloadResults() {
        return ResponseEntity.ok(preloadService.getPreloadResults());
    }
    
    /**
     * 获取用户查询历史
     * 
     * @param userId 用户ID
     * @return 查询历史列表
     */
    @GetMapping("/user/history")
    public ResponseEntity<List<String>> getUserQueryHistory(@RequestParam String userId) {
        return ResponseEntity.ok(preloadService.getUserQueryHistory(userId));
    }
    
    /**
     * 清除用户查询历史
     * 
     * @param userId 用户ID
     * @return 操作结果
     */
    @DeleteMapping("/user/history")
    public ResponseEntity<Map<String, Object>> clearUserHistory(@RequestParam String userId) {
        preloadService.clearUserHistory(userId);
        
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "用户查询历史已清除"
        ));
    }
    
    /**
     * 重置所有统计
     * 
     * @return 操作结果
     */
    @PostMapping("/stats/reset")
    public ResponseEntity<Map<String, Object>> resetAllStats() {
        cachePreheatService.resetStats();
        preloadService.resetStats();
        cacheMonitorService.resetStats();
        
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "所有缓存统计已重置"
        ));
    }
}
