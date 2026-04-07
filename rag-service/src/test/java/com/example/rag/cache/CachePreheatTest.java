package com.example.rag.cache;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 缓存预热功能测试
 */
@Slf4j
@SpringBootTest
class CachePreheatTest {

    @Autowired
    private HotQueryDetector hotQueryDetector;

    @Autowired
    private CachePreheatService cachePreheatService;

    @Autowired
    private PreloadService preloadService;

    @Autowired
    private CacheMonitorService cacheMonitorService;

    @Test
    void testRecordQuery() {
        // 记录查询
        String query = "机器学习算法";
        hotQueryDetector.recordQuery(query, "user1");

        // 检查查询频率
        int frequency = hotQueryDetector.getQueryFrequency(query);
        assertTrue(frequency > 0, "查询频率应该大于0");

        log.info("查询频率: {}", frequency);
    }

    @Test
    void testHotQueryDetection() {
        // 模拟热点查询（多次访问）
        String query = "深度学习框架";
        for (int i = 0; i < 15; i++) {
            hotQueryDetector.recordQuery(query, "user" + i);
        }

        // 检查是否识别为热点
        boolean isHot = hotQueryDetector.isHotQuery(query);
        log.info("是否为热点查询: {}", isHot);

        // 获取热点查询列表
        List<HotQueryDetector.HotQuery> hotQueries = hotQueryDetector.getTopHotQueries(10);
        log.info("Top热点查询数量: {}", hotQueries.size());
    }

    @Test
    void testQueryRelation() {
        // 记录查询关联
        String query1 = "自然语言处理";
        String query2 = "BERT模型";

        hotQueryDetector.recordQueryRelation(query1, query2);

        // 获取相关查询
        List<HotQueryDetector.RelatedQuery> relatedQueries = 
                hotQueryDetector.getRelatedQueries(query1);

        log.info("相关查询数量: {}", relatedQueries.size());
        relatedQueries.forEach(rq -> 
                log.info("相关查询: {}, 概率: {}", rq.getQuery(), rq.getProbability()));
    }

    @Test
    void testPreheatStats() {
        // 获取预热统计
        CachePreheatService.PreheatStats stats = cachePreheatService.getStats();

        log.info("预热统计: 总任务={}, 成功={}, 失败={}", 
                stats.getTotalTasks(), stats.getSuccessCount(), stats.getFailureCount());

        assertNotNull(stats);
    }

    @Test
    void testPreloadAnalysis() {
        // 分析查询并触发预加载
        String userId = "testUser";
        String query = "Spring Boot教程";

        preloadService.analyzeAndPreload(userId, query);

        // 获取预加载统计
        PreloadService.PreloadStats stats = preloadService.getStats();

        log.info("预加载统计: 总预加载数={}, 命中数={}, 命中率={}", 
                stats.getTotalPreloaded(), stats.getTotalHits(), stats.getHitRate());

        assertNotNull(stats);
    }

    @Test
    void testCacheOverview() {
        // 获取缓存总览
        CacheMonitorService.CacheOverview overview = cacheMonitorService.getCacheOverview();

        log.info("缓存总览: 总请求数={}, 总命中数={}, 命中率={}", 
                overview.getTotalCacheRequests(), 
                overview.getTotalCacheHits(), 
                overview.getOverallHitRate());

        assertNotNull(overview);
        assertNotNull(overview.getCacheMetrics());
    }

    @Test
    void testCacheMetrics() {
        // 获取所有缓存指标
        List<CacheMonitorService.CacheMetrics> metrics = cacheMonitorService.getAllCacheMetrics();

        log.info("缓存区域数量: {}", metrics.size());
        metrics.forEach(m -> 
                log.info("缓存区域: {}, 命中率: {}", m.getDisplayName(), m.getHitRate()));

        assertNotNull(metrics);
    }

    @Test
    void testExportReport() {
        // 导出监控报告
        String report = cacheMonitorService.exportReport();

        log.info("缓存监控报告:\n{}", report);

        assertNotNull(report);
        assertTrue(report.contains("缓存监控报告"));
    }
}
