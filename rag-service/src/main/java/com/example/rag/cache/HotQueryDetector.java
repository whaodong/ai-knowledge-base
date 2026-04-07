package com.example.rag.cache;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 热点查询检测器
 * 
 * <p>基于Redis实现热点查询检测，支持：</p>
 * <ul>
 *   <li>实时统计查询频率</li>
 *   <li>动态识别Top N热点查询</li>
 *   <li>查询时间窗口分析</li>
 *   <li>相关查询关联分析</li>
 * </ul>
 */
@Slf4j
@Component
public class HotQueryDetector {

    private final RedisTemplate<String, Object> redisTemplate;
    
    // Redis Key前缀
    private static final String QUERY_COUNT_KEY = "hot:query:count:";
    private static final String QUERY_TIME_KEY = "hot:query:time:";
    private static final String HOT_QUERIES_KEY = "hot:queries:top";
    private static final String QUERY_RELATION_KEY = "hot:query:relation:";
    
    // 热点阈值配置
    private static final int HOT_THRESHOLD = 10;           // 热点查询阈值（访问次数）
    private static final int TIME_WINDOW_HOURS = 1;        // 时间窗口（小时）
    private static final int TOP_N_QUERIES = 100;          // Top N热点查询数量
    private static final double RELATION_THRESHOLD = 0.3;  // 相关查询概率阈值
    
    public HotQueryDetector(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }
    
    /**
     * 记录查询访问
     * 
     * @param query 查询文本
     * @param userId 用户ID（可选）
     */
    public void recordQuery(String query, String userId) {
        if (query == null || query.trim().isEmpty()) {
            return;
        }
        
        String normalizedQuery = normalizeQuery(query);
        String countKey = QUERY_COUNT_KEY + getTimeWindow();
        String timeKey = QUERY_TIME_KEY + normalizedQuery;
        
        try {
            // 1. 增加查询计数
            redisTemplate.opsForZSet().incrementScore(countKey, normalizedQuery, 1);
            
            // 2. 记录查询时间
            redisTemplate.opsForValue().set(timeKey, System.currentTimeMillis(), 
                    Duration.ofHours(TIME_WINDOW_HOURS));
            
            // 3. 如果是热点查询，加入热点集合
            Double score = redisTemplate.opsForZSet().score(countKey, normalizedQuery);
            if (score != null && score >= HOT_THRESHOLD) {
                redisTemplate.opsForZSet().add(HOT_QUERIES_KEY, normalizedQuery, score);
                
                // 设置热点集合过期时间
                redisTemplate.expire(HOT_QUERIES_KEY, Duration.ofHours(TIME_WINDOW_HOURS * 2));
                
                log.debug("热点查询识别: query={}, count={}", normalizedQuery, score);
            }
            
            log.debug("记录查询访问: query={}, userId={}", normalizedQuery, userId);
            
        } catch (Exception e) {
            log.error("记录查询访问失败: query={}", normalizedQuery, e);
        }
    }
    
    /**
     * 记录查询关联
     * 用于分析查询A后大概率查询B的模式
     * 
     * @param previousQuery 前一个查询
     * @param currentQuery 当前查询
     */
    public void recordQueryRelation(String previousQuery, String currentQuery) {
        if (previousQuery == null || currentQuery == null || 
            previousQuery.equals(currentQuery)) {
            return;
        }
        
        String normalizedPrev = normalizeQuery(previousQuery);
        String normalizedCurr = normalizeQuery(currentQuery);
        String relationKey = QUERY_RELATION_KEY + normalizedPrev;
        
        try {
            // 记录从前一个查询到当前查询的跳转次数
            redisTemplate.opsForHash().increment(relationKey, normalizedCurr, 1);
            
            // 设置关联关系过期时间
            redisTemplate.expire(relationKey, Duration.ofHours(24));
            
            log.debug("记录查询关联: {} -> {}", normalizedPrev, normalizedCurr);
            
        } catch (Exception e) {
            log.error("记录查询关联失败", e);
        }
    }
    
    /**
     * 获取Top N热点查询
     * 
     * @param n 查询数量
     * @return 热点查询列表（按频率降序）
     */
    public List<HotQuery> getTopHotQueries(int n) {
        try {
            Set<ZSetOperations.TypedTuple<Object>> tuples = 
                    redisTemplate.opsForZSet().reverseRangeWithScores(
                            HOT_QUERIES_KEY, 0, n - 1);
            
            if (tuples == null || tuples.isEmpty()) {
                return Collections.emptyList();
            }
            
            return tuples.stream()
                    .map(tuple -> new HotQuery(
                            (String) tuple.getValue(),
                            tuple.getScore().intValue(),
                            LocalDateTime.now()
                    ))
                    .collect(Collectors.toList());
            
        } catch (Exception e) {
            log.error("获取Top N热点查询失败", e);
            return Collections.emptyList();
        }
    }
    
    /**
     * 检查是否为热点查询
     * 
     * @param query 查询文本
     * @return 是否为热点查询
     */
    public boolean isHotQuery(String query) {
        if (query == null || query.trim().isEmpty()) {
            return false;
        }
        
        String normalizedQuery = normalizeQuery(query);
        Double score = redisTemplate.opsForZSet().score(HOT_QUERIES_KEY, normalizedQuery);
        
        return score != null && score >= HOT_THRESHOLD;
    }
    
    /**
     * 获取查询频率
     * 
     * @param query 查询文本
     * @return 查询频率
     */
    public int getQueryFrequency(String query) {
        if (query == null || query.trim().isEmpty()) {
            return 0;
        }
        
        String normalizedQuery = normalizeQuery(query);
        String countKey = QUERY_COUNT_KEY + getTimeWindow();
        Double score = redisTemplate.opsForZSet().score(countKey, normalizedQuery);
        
        return score != null ? score.intValue() : 0;
    }
    
    /**
     * 获取相关查询预测
     * 基于历史数据分析，返回查询A后大概率查询B的列表
     * 
     * @param query 当前查询
     * @return 相关查询列表（按概率降序）
     */
    public List<RelatedQuery> getRelatedQueries(String query) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }
        
        String normalizedQuery = normalizeQuery(query);
        String relationKey = QUERY_RELATION_KEY + normalizedQuery;
        
        try {
            // 获取该查询的所有关联查询
            Map<Object, Object> relations = redisTemplate.opsForHash().entries(relationKey);
            
            if (relations.isEmpty()) {
                return Collections.emptyList();
            }
            
            // 计算总跳转次数
            int totalJumps = relations.values().stream()
                    .mapToInt(v -> Integer.parseInt(v.toString()))
                    .sum();
            
            // 计算每个关联查询的概率并排序
            return relations.entrySet().stream()
                    .map(entry -> {
                        String relatedQuery = (String) entry.getKey();
                        int count = Integer.parseInt(entry.getValue().toString());
                        double probability = (double) count / totalJumps;
                        
                        return new RelatedQuery(relatedQuery, probability, count);
                    })
                    .filter(rq -> rq.getProbability() >= RELATION_THRESHOLD)
                    .sorted(Comparator.comparingDouble(RelatedQuery::getProbability).reversed())
                    .limit(10)  // 最多返回10个相关查询
                    .collect(Collectors.toList());
            
        } catch (Exception e) {
            log.error("获取相关查询预测失败: query={}", normalizedQuery, e);
            return Collections.emptyList();
        }
    }
    
    /**
     * 获取热点查询统计信息
     */
    public HotQueryStats getStats() {
        try {
            Long totalHotQueries = redisTemplate.opsForZSet().size(HOT_QUERIES_KEY);
            Set<Object> topQueries = redisTemplate.opsForZSet().range(HOT_QUERIES_KEY, 0, 9);
            
            return HotQueryStats.builder()
                    .totalHotQueries(totalHotQueries != null ? totalHotQueries : 0)
                    .topQueries(topQueries != null ? 
                            topQueries.stream().map(Object::toString).collect(Collectors.toList()) : 
                            Collections.emptyList())
                    .timeWindowHours(TIME_WINDOW_HOURS)
                    .hotThreshold(HOT_THRESHOLD)
                    .build();
            
        } catch (Exception e) {
            log.error("获取热点查询统计信息失败", e);
            return HotQueryStats.builder().build();
        }
    }
    
    /**
     * 定时清理过期数据
     * 每小时执行一次
     */
    @Scheduled(fixedRate = 3600000)
    public void cleanupExpiredData() {
        try {
            log.info("开始清理过期的热点查询数据...");
            
            // 清理过期的计数器
            String oldTimeWindow = QUERY_COUNT_KEY + getTimeWindow(-1);
            redisTemplate.delete(oldTimeWindow);
            
            // 清理热点查询集合中分数低于阈值的项
            redisTemplate.opsForZSet().removeRangeByScore(HOT_QUERIES_KEY, 0, HOT_THRESHOLD - 1);
            
            log.info("过期热点查询数据清理完成");
            
        } catch (Exception e) {
            log.error("清理过期热点查询数据失败", e);
        }
    }
    
    /**
     * 规范化查询文本
     * 转小写、去空格、去除特殊字符
     */
    private String normalizeQuery(String query) {
        if (query == null) {
            return "";
        }
        return query.toLowerCase()
                .trim()
                .replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5]", "");
    }
    
    /**
     * 获取时间窗口标识
     * 用于按时间段统计查询频率
     */
    private String getTimeWindow() {
        return getTimeWindow(0);
    }
    
    private String getTimeWindow(int offsetHours) {
        LocalDateTime now = LocalDateTime.now().plusHours(offsetHours);
        return String.format("%d-%02d-%02d-%02d", 
                now.getYear(), now.getMonthValue(), now.getDayOfMonth(), now.getHour());
    }
    
    /**
     * 热点查询信息
     */
    @Data
    public static class HotQuery {
        private final String query;
        private final int frequency;
        private final LocalDateTime lastAccessTime;
        
        public HotQuery(String query, int frequency, LocalDateTime lastAccessTime) {
            this.query = query;
            this.frequency = frequency;
            this.lastAccessTime = lastAccessTime;
        }
    }
    
    /**
     * 相关查询信息
     */
    @Data
    public static class RelatedQuery {
        private final String query;
        private final double probability;  // 出现概率
        private final int count;           // 出现次数
        
        public RelatedQuery(String query, double probability, int count) {
            this.query = query;
            this.probability = probability;
            this.count = count;
        }
    }
    
    /**
     * 热点查询统计信息
     */
    @Data
    @lombok.Builder
    public static class HotQueryStats {
        private long totalHotQueries;
        private List<String> topQueries;
        private int timeWindowHours;
        private int hotThreshold;
    }
}
