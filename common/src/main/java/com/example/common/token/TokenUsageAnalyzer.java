package com.example.common.token;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Token使用分析器
 * 
 * <p>分析Token使用情况，包括按用户统计、按时间段分析、成本估算和异常检测。</p>
 * <p>功能特点：</p>
 * <ul>
 *   <li>按用户统计Token消耗</li>
 *   <li>按时间段分析使用趋势</li>
 *   <li>Token成本估算</li>
 *   <li>异常使用检测</li>
 *   <li>使用报告生成</li>
 * </ul>
 */
@Slf4j
@Component
public class TokenUsageAnalyzer {
    
    // Token使用记录：userId -> 使用记录列表
    private final Map<String, List<TokenUsageEntry>> usageRecords = new ConcurrentHashMap<>();
    
    // 全局统计
    private final Map<String, DailyStats> dailyStatsMap = new ConcurrentHashMap<>();
    
    // 价格配置（美元/千Token）
    private static final Map<String, TokenPrice> PRICING = Map.of(
            "gpt-3.5-turbo", new TokenPrice(0.0015, 0.002),
            "gpt-3.5-turbo-16k", new TokenPrice(0.003, 0.004),
            "gpt-4", new TokenPrice(0.03, 0.06),
            "gpt-4-32k", new TokenPrice(0.06, 0.12),
            "gpt-4-turbo", new TokenPrice(0.01, 0.03)
    );
    
    // 异常检测阈值
    private static final int MAX_HOURLY_TOKENS = 50000; // 每小时最大Token数
    private static final int MAX_SESSION_TOKENS = 30000; // 每会话最大Token数
    private static final double ANOMALY_THRESHOLD = 3.0; // 异常倍数
    
    /**
     * 记录Token使用
     * 
     * @param userId 用户ID
     * @param sessionId 会话ID
     * @param modelName 模型名称
     * @param inputTokens 输入Token数
     * @param outputTokens 输出Token数
     */
    public void recordUsage(String userId, String sessionId, String modelName, 
                           int inputTokens, int outputTokens) {
        TokenUsageEntry entry = new TokenUsageEntry(
                userId, sessionId, modelName, inputTokens, outputTokens, LocalDateTime.now()
        );
        
        usageRecords.computeIfAbsent(userId, k -> new ArrayList<>()).add(entry);
        
        // 更新每日统计
        updateDailyStats(entry);
        
        // 检测异常
        detectAnomaly(userId, entry);
        
        log.debug("记录Token使用: userId={}, sessionId={}, input={}, output={}", 
                userId, sessionId, inputTokens, outputTokens);
    }
    
    /**
     * 按用户统计Token消耗
     * 
     * @param userId 用户ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 用户Token统计
     */
    public UserTokenStatistics getUserStatistics(String userId, LocalDateTime startTime, 
                                                  LocalDateTime endTime) {
        List<TokenUsageEntry> records = usageRecords.getOrDefault(userId, Collections.emptyList());
        
        // 过滤时间范围
        List<TokenUsageEntry> filtered = records.stream()
                .filter(entry -> !entry.getTimestamp().isBefore(startTime) 
                        && !entry.getTimestamp().isAfter(endTime))
                .collect(Collectors.toList());
        
        // 统计
        int totalInputTokens = filtered.stream().mapToInt(TokenUsageEntry::getInputTokens).sum();
        int totalOutputTokens = filtered.stream().mapToInt(TokenUsageEntry::getOutputTokens).sum();
        int totalTokens = totalInputTokens + totalOutputTokens;
        int sessionCount = (int) filtered.stream().map(TokenUsageEntry::getSessionId).distinct().count();
        
        // 计算成本
        double totalCost = filtered.stream()
                .mapToDouble(entry -> calculateCost(entry.getModelName(), 
                        entry.getInputTokens(), entry.getOutputTokens()))
                .sum();
        
        return new UserTokenStatistics(
                userId, totalTokens, totalInputTokens, totalOutputTokens, 
                sessionCount, totalCost, startTime, endTime
        );
    }
    
    /**
     * 按时间段分析使用趋势
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 使用趋势分析
     */
    public UsageTrendAnalysis analyzeTrend(LocalDateTime startTime, LocalDateTime endTime) {
        List<TokenUsageEntry> allRecords = usageRecords.values().stream()
                .flatMap(List::stream)
                .filter(entry -> !entry.getTimestamp().isBefore(startTime) 
                        && !entry.getTimestamp().isAfter(endTime))
                .collect(Collectors.toList());
        
        // 按日期分组
        Map<LocalDate, List<TokenUsageEntry>> byDate = allRecords.stream()
                .collect(Collectors.groupingBy(entry -> entry.getTimestamp().toLocalDate()));
        
        // 生成每日统计
        List<DailyUsage> dailyUsageList = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        
        LocalDate current = startTime.toLocalDate();
        while (!current.isAfter(endTime.toLocalDate())) {
            List<TokenUsageEntry> dayRecords = byDate.getOrDefault(current, Collections.emptyList());
            
            int dailyTokens = dayRecords.stream()
                    .mapToInt(entry -> entry.getInputTokens() + entry.getOutputTokens())
                    .sum();
            
            double dailyCost = dayRecords.stream()
                    .mapToDouble(entry -> calculateCost(entry.getModelName(), 
                            entry.getInputTokens(), entry.getOutputTokens()))
                    .sum();
            
            dailyUsageList.add(new DailyUsage(current.format(formatter), dailyTokens, dailyCost));
            current = current.plusDays(1);
        }
        
        // 计算趋势
        int totalTokens = allRecords.stream()
                .mapToInt(entry -> entry.getInputTokens() + entry.getOutputTokens())
                .sum();
        
        double totalCost = allRecords.stream()
                .mapToDouble(entry -> calculateCost(entry.getModelName(), 
                        entry.getInputTokens(), entry.getOutputTokens()))
                .sum();
        
        int uniqueUsers = (int) allRecords.stream()
                .map(TokenUsageEntry::getUserId)
                .distinct()
                .count();
        
        int uniqueSessions = (int) allRecords.stream()
                .map(TokenUsageEntry::getSessionId)
                .distinct()
                .count();
        
        // 计算增长率
        double growthRate = calculateGrowthRate(dailyUsageList);
        
        return new UsageTrendAnalysis(
                startTime, endTime, totalTokens, totalCost, 
                uniqueUsers, uniqueSessions, dailyUsageList, growthRate
        );
    }
    
    /**
     * Token成本估算
     * 
     * @param modelName 模型名称
     * @param inputTokens 输入Token数
     * @param outputTokens 输出Token数
     * @return 成本（美元）
     */
    public double calculateCost(String modelName, int inputTokens, int outputTokens) {
        TokenPrice price = PRICING.getOrDefault(modelName, new TokenPrice(0.002, 0.002));
        
        double inputCost = (inputTokens / 1000.0) * price.getInputPrice();
        double outputCost = (outputTokens / 1000.0) * price.getOutputPrice();
        
        return inputCost + outputCost;
    }
    
    /**
     * 异常使用检测
     * 
     * @param userId 用户ID
     * @return 异常记录列表
     */
    public List<AnomalyRecord> detectAnomalies(String userId) {
        List<TokenUsageEntry> records = usageRecords.getOrDefault(userId, Collections.emptyList());
        List<AnomalyRecord> anomalies = new ArrayList<>();
        
        if (records.isEmpty()) {
            return anomalies;
        }
        
        // 检测小时级异常
        Map<Integer, List<TokenUsageEntry>> hourlyUsage = records.stream()
                .collect(Collectors.groupingBy(e -> e.getTimestamp().getHour()));
        
        double avgHourly = records.size() > 24 ? 
                records.stream().mapToInt(e -> e.getInputTokens() + e.getOutputTokens()).average().orElse(0) : 0;
        
        for (Map.Entry<Integer, List<TokenUsageEntry>> entry : hourlyUsage.entrySet()) {
            int hourlyTokens = entry.getValue().stream()
                    .mapToInt(e -> e.getInputTokens() + e.getOutputTokens())
                    .sum();
            
            if (hourlyTokens > MAX_HOURLY_TOKENS) {
                anomalies.add(new AnomalyRecord(
                        userId, "HOURLY_EXCEED", hourlyTokens, 
                        MAX_HOURLY_TOKENS, entry.getKey(), LocalDateTime.now()
                ));
            }
            
            if (avgHourly > 0 && hourlyTokens > avgHourly * ANOMALY_THRESHOLD) {
                anomalies.add(new AnomalyRecord(
                        userId, "HOURLY_ANOMALY", hourlyTokens, 
                        (int) (avgHourly * ANOMALY_THRESHOLD), entry.getKey(), LocalDateTime.now()
                ));
            }
        }
        
        // 检测会话级异常
        Map<String, List<TokenUsageEntry>> sessionUsage = records.stream()
                .collect(Collectors.groupingBy(TokenUsageEntry::getSessionId));
        
        for (Map.Entry<String, List<TokenUsageEntry>> entry : sessionUsage.entrySet()) {
            int sessionTokens = entry.getValue().stream()
                    .mapToInt(e -> e.getInputTokens() + e.getOutputTokens())
                    .sum();
            
            if (sessionTokens > MAX_SESSION_TOKENS) {
                anomalies.add(new AnomalyRecord(
                        userId, "SESSION_EXCEED", sessionTokens, 
                        MAX_SESSION_TOKENS, entry.getKey(), LocalDateTime.now()
                ));
            }
        }
        
        return anomalies;
    }
    
    /**
     * 生成使用报告
     * 
     * @param userId 用户ID（可选，null表示全部用户）
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 使用报告
     */
    public UsageReport generateReport(String userId, LocalDateTime startTime, LocalDateTime endTime) {
        List<UserTokenStatistics> userStats = new ArrayList<>();
        
        if (userId != null) {
            userStats.add(getUserStatistics(userId, startTime, endTime));
        } else {
            // 所有用户
            usageRecords.keySet().forEach(uid -> 
                    userStats.add(getUserStatistics(uid, startTime, endTime)));
        }
        
        UsageTrendAnalysis trend = analyzeTrend(startTime, endTime);
        
        // 模型使用分布
        Map<String, Integer> modelDistribution = new HashMap<>();
        usageRecords.values().stream()
                .flatMap(List::stream)
                .filter(entry -> !entry.getTimestamp().isBefore(startTime) 
                        && !entry.getTimestamp().isAfter(endTime))
                .forEach(entry -> modelDistribution.merge(entry.getModelName(), 
                        entry.getInputTokens() + entry.getOutputTokens(), Integer::sum));
        
        return new UsageReport(
                startTime, endTime, userStats, trend, modelDistribution
        );
    }
    
    /**
     * 获取热门用户（Token使用最多）
     * 
     * @param limit 数量限制
     * @return 热门用户列表
     */
    public List<UserTokenStatistics> getTopUsers(int limit) {
        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusDays(7); // 最近7天
        
        return usageRecords.keySet().stream()
                .map(userId -> getUserStatistics(userId, startTime, endTime))
                .sorted((a, b) -> Integer.compare(b.getTotalTokens(), a.getTotalTokens()))
                .limit(limit)
                .collect(Collectors.toList());
    }
    
    // ============= 私有方法 =============
    
    /**
     * 更新每日统计
     */
    private void updateDailyStats(TokenUsageEntry entry) {
        String dateKey = entry.getTimestamp().toLocalDate().toString();
        
        dailyStatsMap.compute(dateKey, (key, stats) -> {
            if (stats == null) {
                stats = new DailyStats(dateKey);
            }
            stats.addTokens(entry.getInputTokens(), entry.getOutputTokens());
            return stats;
        });
    }
    
    /**
     * 检测单条记录异常
     */
    private void detectAnomaly(String userId, TokenUsageEntry entry) {
        int totalTokens = entry.getInputTokens() + entry.getOutputTokens();
        
        if (totalTokens > MAX_SESSION_TOKENS) {
            log.warn("检测到异常Token使用: userId={}, sessionId={}, tokens={}", 
                    userId, entry.getSessionId(), totalTokens);
        }
    }
    
    /**
     * 计算增长率
     */
    private double calculateGrowthRate(List<DailyUsage> dailyUsageList) {
        if (dailyUsageList.size() < 2) {
            return 0;
        }
        
        int firstHalf = dailyUsageList.subList(0, dailyUsageList.size() / 2).stream()
                .mapToInt(DailyUsage::getTokens)
                .sum();
        
        int secondHalf = dailyUsageList.subList(dailyUsageList.size() / 2, dailyUsageList.size()).stream()
                .mapToInt(DailyUsage::getTokens)
                .sum();
        
        if (firstHalf == 0) {
            return secondHalf > 0 ? 100.0 : 0;
        }
        
        return ((double) (secondHalf - firstHalf) / firstHalf) * 100;
    }
    
    // ============= 内部类 =============
    
    /**
     * Token使用记录
     */
    public static class TokenUsageEntry {
        private final String userId;
        private final String sessionId;
        private final String modelName;
        private final int inputTokens;
        private final int outputTokens;
        private final LocalDateTime timestamp;
        
        public TokenUsageEntry(String userId, String sessionId, String modelName, 
                               int inputTokens, int outputTokens, LocalDateTime timestamp) {
            this.userId = userId;
            this.sessionId = sessionId;
            this.modelName = modelName;
            this.inputTokens = inputTokens;
            this.outputTokens = outputTokens;
            this.timestamp = timestamp;
        }
        
        // Getters
        public String getUserId() { return userId; }
        public String getSessionId() { return sessionId; }
        public String getModelName() { return modelName; }
        public int getInputTokens() { return inputTokens; }
        public int getOutputTokens() { return outputTokens; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }
    
    /**
     * Token价格
     */
    public static class TokenPrice {
        private final double inputPrice; // 输入价格（美元/千Token）
        private final double outputPrice; // 输出价格（美元/千Token）
        
        public TokenPrice(double inputPrice, double outputPrice) {
            this.inputPrice = inputPrice;
            this.outputPrice = outputPrice;
        }
        
        public double getInputPrice() { return inputPrice; }
        public double getOutputPrice() { return outputPrice; }
    }
    
    /**
     * 用户Token统计
     */
    public static class UserTokenStatistics {
        private final String userId;
        private final int totalTokens;
        private final int totalInputTokens;
        private final int totalOutputTokens;
        private final int sessionCount;
        private final double totalCost;
        private final LocalDateTime startTime;
        private final LocalDateTime endTime;
        
        public UserTokenStatistics(String userId, int totalTokens, int totalInputTokens, 
                                   int totalOutputTokens, int sessionCount, double totalCost,
                                   LocalDateTime startTime, LocalDateTime endTime) {
            this.userId = userId;
            this.totalTokens = totalTokens;
            this.totalInputTokens = totalInputTokens;
            this.totalOutputTokens = totalOutputTokens;
            this.sessionCount = sessionCount;
            this.totalCost = totalCost;
            this.startTime = startTime;
            this.endTime = endTime;
        }
        
        // Getters
        public String getUserId() { return userId; }
        public int getTotalTokens() { return totalTokens; }
        public int getTotalInputTokens() { return totalInputTokens; }
        public int getTotalOutputTokens() { return totalOutputTokens; }
        public int getSessionCount() { return sessionCount; }
        public double getTotalCost() { return totalCost; }
        public LocalDateTime getStartTime() { return startTime; }
        public LocalDateTime getEndTime() { return endTime; }
    }
    
    /**
     * 使用趋势分析
     */
    public static class UsageTrendAnalysis {
        private final LocalDateTime startTime;
        private final LocalDateTime endTime;
        private final int totalTokens;
        private final double totalCost;
        private final int uniqueUsers;
        private final int uniqueSessions;
        private final List<DailyUsage> dailyUsage;
        private final double growthRate;
        
        public UsageTrendAnalysis(LocalDateTime startTime, LocalDateTime endTime, 
                                  int totalTokens, double totalCost, int uniqueUsers, 
                                  int uniqueSessions, List<DailyUsage> dailyUsage, 
                                  double growthRate) {
            this.startTime = startTime;
            this.endTime = endTime;
            this.totalTokens = totalTokens;
            this.totalCost = totalCost;
            this.uniqueUsers = uniqueUsers;
            this.uniqueSessions = uniqueSessions;
            this.dailyUsage = dailyUsage;
            this.growthRate = growthRate;
        }
        
        // Getters
        public LocalDateTime getStartTime() { return startTime; }
        public LocalDateTime getEndTime() { return endTime; }
        public int getTotalTokens() { return totalTokens; }
        public double getTotalCost() { return totalCost; }
        public int getUniqueUsers() { return uniqueUsers; }
        public int getUniqueSessions() { return uniqueSessions; }
        public List<DailyUsage> getDailyUsage() { return dailyUsage; }
        public double getGrowthRate() { return growthRate; }
    }
    
    /**
     * 每日使用量
     */
    public static class DailyUsage {
        private final String date;
        private final int tokens;
        private final double cost;
        
        public DailyUsage(String date, int tokens, double cost) {
            this.date = date;
            this.tokens = tokens;
            this.cost = cost;
        }
        
        public String getDate() { return date; }
        public int getTokens() { return tokens; }
        public double getCost() { return cost; }
    }
    
    /**
     * 异常记录
     */
    public static class AnomalyRecord {
        private final String userId;
        private final String type; // HOURLY_EXCEED, HOURLY_ANOMALY, SESSION_EXCEED
        private final int actualTokens;
        private final int threshold;
        private final Object context; // 小时数或会话ID
        private final LocalDateTime timestamp;
        
        public AnomalyRecord(String userId, String type, int actualTokens, 
                            int threshold, Object context, LocalDateTime timestamp) {
            this.userId = userId;
            this.type = type;
            this.actualTokens = actualTokens;
            this.threshold = threshold;
            this.context = context;
            this.timestamp = timestamp;
        }
        
        // Getters
        public String getUserId() { return userId; }
        public String getType() { return type; }
        public int getActualTokens() { return actualTokens; }
        public int getThreshold() { return threshold; }
        public Object getContext() { return context; }
        public LocalDateTime getTimestamp() { return timestamp; }
    }
    
    /**
     * 使用报告
     */
    public static class UsageReport {
        private final LocalDateTime startTime;
        private final LocalDateTime endTime;
        private final List<UserTokenStatistics> userStatistics;
        private final UsageTrendAnalysis trendAnalysis;
        private final Map<String, Integer> modelDistribution;
        
        public UsageReport(LocalDateTime startTime, LocalDateTime endTime, 
                          List<UserTokenStatistics> userStatistics, 
                          UsageTrendAnalysis trendAnalysis, 
                          Map<String, Integer> modelDistribution) {
            this.startTime = startTime;
            this.endTime = endTime;
            this.userStatistics = userStatistics;
            this.trendAnalysis = trendAnalysis;
            this.modelDistribution = modelDistribution;
        }
        
        // Getters
        public LocalDateTime getStartTime() { return startTime; }
        public LocalDateTime getEndTime() { return endTime; }
        public List<UserTokenStatistics> getUserStatistics() { return userStatistics; }
        public UsageTrendAnalysis getTrendAnalysis() { return trendAnalysis; }
        public Map<String, Integer> getModelDistribution() { return modelDistribution; }
    }
    
    /**
     * 每日统计
     */
    private static class DailyStats {
        private final String date;
        private int totalInputTokens = 0;
        private int totalOutputTokens = 0;
        
        public DailyStats(String date) {
            this.date = date;
        }
        
        public void addTokens(int inputTokens, int outputTokens) {
            this.totalInputTokens += inputTokens;
            this.totalOutputTokens += outputTokens;
        }
    }
}
