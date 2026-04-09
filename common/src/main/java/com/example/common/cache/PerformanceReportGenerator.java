package com.example.common.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 性能测试报告生成器
 * 负责生成详细的性能测试报告文档
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnBean(PerformanceTestService.class)
public class PerformanceReportGenerator {

    private final ObjectProvider<PerformanceTestService> performanceTestServiceProvider;
    
    private static final String REPORT_DIR = "outputs/reports/performance";
    private static final DateTimeFormatter DATE_FORMATTER = 
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    
    /**
     * 生成完整性能测试报告
     */
    public String generateFullReport() {
        log.info("开始生成性能测试报告...");

        PerformanceTestService performanceTestService = performanceTestServiceProvider.getIfAvailable();
        if (performanceTestService == null) {
            throw new IllegalStateException("PerformanceTestService不可用，无法生成性能测试报告");
        }
        
        try {
            // 确保报告目录存在
            Files.createDirectories(Paths.get(REPORT_DIR));
            
            // 生成报告文件名
            String timestamp = LocalDateTime.now().format(DATE_FORMATTER);
            String reportFileName = String.format("performance_report_%s.md", timestamp);
            Path reportPath = Paths.get(REPORT_DIR, reportFileName);
            
            // 运行测试获取数据
            PerformanceTestService.PerformanceReport report = 
                    performanceTestService.runFullTestSuite();
            
            // 生成报告内容
            String reportContent = buildReportContent(report);
            
            // 写入文件
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(reportPath.toFile()))) {
                writer.write(reportContent);
            }
            
            log.info("性能测试报告生成完成: {}", reportPath);
            return reportPath.toString();
            
        } catch (IOException e) {
            log.error("生成性能测试报告失败", e);
            throw new RuntimeException("生成报告失败", e);
        }
    }
    
    /**
     * 构建报告内容
     */
    private String buildReportContent(PerformanceTestService.PerformanceReport report) {
        StringBuilder content = new StringBuilder();
        
        // 报告标题
        content.append("# 多级缓存系统性能测试报告\n\n");
        content.append("**生成时间**: ").append(LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n\n");
        
        // 执行摘要
        content.append("## 1. 执行摘要\n\n");
        content.append("| 指标 | 结果 | 评估 |\n");
        content.append("|------|------|------|\n");
        content.append("| **测试完成时间** | ").append(LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append(" | 正常 |\n");
        content.append("| **基础性能加速比** | ").append(String.format("%.2f", 
                report.getBaselineTest().getSpeedupFactor())).append("x | ")
                .append(evaluateSpeedup(report.getBaselineTest().getSpeedupFactor())).append(" |\n");
        content.append("| **缓存命中率** | ").append(String.format("%.2f", 
                report.getHitRateTest().getHitRate() * 100)).append("% | ")
                .append(evaluateHitRate(report.getHitRateTest().getHitRate())).append(" |\n");
        content.append("| **并发处理TPS** | ").append(String.format("%.2f", 
                report.getConcurrencyTest().getThroughput())).append(" | ")
                .append(evaluateTPS(report.getConcurrencyTest().getThroughput())).append(" |\n");
        content.append("| **P99响应时间** | ").append(
                report.getConcurrencyTest().getP99ResponseTimeNs() / 1_000_000.0)
                .append("ms | ").append(evaluateLatency(
                        report.getConcurrencyTest().getP99ResponseTimeNs())).append(" |\n");
        content.append("\n");
        
        // 基础性能测试详情
        content.append("## 2. 基础性能测试\n\n");
        content.append("### 2.1 缓存 vs 数据库性能对比\n\n");
        content.append("```\n");
        content.append(String.format("缓存读取时间:   %,12d ns (%.2f ms)\n", 
                report.getBaselineTest().getCacheReadTimeNs(),
                report.getBaselineTest().getCacheReadTimeNs() / 1_000_000.0));
        content.append(String.format("数据库读取时间: %,12d ns (%.2f ms)\n", 
                report.getBaselineTest().getDatabaseReadTimeNs(),
                report.getBaselineTest().getDatabaseReadTimeNs() / 1_000_000.0));
        content.append(String.format("性能加速比:    %.2f倍\n", 
                report.getBaselineTest().getSpeedupFactor()));
        content.append(String.format("缓存吞吐量:    %.2f 请求/秒\n", 
                report.getBaselineTest().getThroughputCache()));
        content.append(String.format("数据库吞吐量:  %.2f 请求/秒\n", 
                report.getBaselineTest().getThroughputDatabase()));
        content.append("```\n\n");
        
        // 命中率测试详情
        content.append("## 3. 缓存命中率测试\n\n");
        content.append("### 3.1 测试结果统计\n\n");
        content.append("| 指标 | 数值 | 百分比 |\n");
        content.append("|------|------|--------|\n");
        content.append("| 总请求数 | ").append(report.getHitRateTest().getTotalRequests())
                .append(" | 100% |\n");
        content.append("| 命中次数 | ").append(report.getHitRateTest().getHitCount())
                .append(" | ").append(String.format("%.2f", 
                        report.getHitRateTest().getHitRate() * 100)).append("% |\n");
        content.append("| 未命中次数 | ").append(report.getHitRateTest().getMissCount())
                .append(" | ").append(String.format("%.2f", 
                        (1 - report.getHitRateTest().getHitRate()) * 100)).append("% |\n");
        content.append("\n");
        
        content.append("### 3.2 性能指标\n\n");
        content.append("```\n");
        content.append(String.format("平均响应时间: %,.0f ns (%.2f ms)\n", 
                (double) report.getHitRateTest().getAverageResponseTimeNs(),
                report.getHitRateTest().getAverageResponseTimeNs() / 1_000_000.0));
        content.append(String.format("系统吞吐量:  %.2f 请求/秒\n", 
                report.getHitRateTest().getThroughput()));
        content.append("```\n\n");
        
        // 并发压力测试详情
        content.append("## 4. 并发压力测试\n\n");
        content.append("### 4.1 测试配置\n\n");
        content.append("- **并发线程数**: ").append(report.getConcurrencyTest().getThreadCount()).append("\n");
        content.append("- **总请求数**: ").append(report.getConcurrencyTest().getRequestCount()).append("\n");
        content.append("- **成功请求数**: ").append(report.getConcurrencyTest().getSuccessfulRequests()).append("\n");
        content.append("- **失败请求数**: ").append(report.getConcurrencyTest().getFailedRequests()).append("\n");
        content.append("- **成功率**: ").append(String.format("%.2f", 
                (double) report.getConcurrencyTest().getSuccessfulRequests() / 
                report.getConcurrencyTest().getRequestCount() * 100)).append("%\n");
        content.append("\n");
        
        content.append("### 4.2 响应时间分布\n\n");
        content.append("| 分位数 | 响应时间 (ns) | 响应时间 (ms) |\n");
        content.append("|--------|---------------|---------------|\n");
        content.append("| 平均 | ").append(report.getConcurrencyTest().getAverageResponseTimeNs())
                .append(" | ").append(String.format("%.2f", 
                        report.getConcurrencyTest().getAverageResponseTimeNs() / 1_000_000.0))
                .append(" |\n");
        content.append("| P95 | ").append(report.getConcurrencyTest().getP95ResponseTimeNs())
                .append(" | ").append(String.format("%.2f", 
                        report.getConcurrencyTest().getP95ResponseTimeNs() / 1_000_000.0))
                .append(" |\n");
        content.append("| P99 | ").append(report.getConcurrencyTest().getP99ResponseTimeNs())
                .append(" | ").append(String.format("%.2f", 
                        report.getConcurrencyTest().getP99ResponseTimeNs() / 1_000_000.0))
                .append(" |\n");
        content.append("\n");
        
        content.append("### 4.3 系统吞吐量\n\n");
        content.append("```\n");
        content.append(String.format("TPS (事务/秒): %.2f\n", 
                report.getConcurrencyTest().getThroughput()));
        content.append("```\n\n");
        
        // 防护机制测试详情
        content.append("## 5. 防护机制测试\n\n");
        
        content.append("### 5.1 缓存穿透防护\n\n");
        content.append("| 指标 | 结果 | 评估 |\n");
        content.append("|------|------|------|\n");
        content.append("| 请求次数 | ").append(
                report.getProtectionTest().getPenetrationTest().getRequestCount()).append(" | - |\n");
        content.append("| 数据库调用次数 | ").append(
                report.getProtectionTest().getPenetrationTest().getDatabaseCalls()).append(" | ")
                .append(evaluatePenetrationProtection(
                        report.getProtectionTest().getPenetrationTest())).append(" |\n");
        content.append("| 平均响应时间 | ").append(String.format("%.2f", 
                report.getProtectionTest().getPenetrationTest().getAverageResponseTimeNs() / 1_000_000.0))
                .append("ms | - |\n");
        content.append("\n");
        
        content.append("### 5.2 缓存击穿防护\n\n");
        content.append("| 指标 | 结果 | 评估 |\n");
        content.append("|------|------|------|\n");
        content.append("| 并发线程数 | ").append(
                report.getProtectionTest().getBreakdownTest().getConcurrentThreads()).append(" | - |\n");
        content.append("| 数据库调用次数 | ").append(
                report.getProtectionTest().getBreakdownTest().getDatabaseCalls()).append(" | ")
                .append(evaluateBreakdownProtection(
                        report.getProtectionTest().getBreakdownTest())).append(" |\n");
        content.append("| 成功获取次数 | ").append(
                report.getProtectionTest().getBreakdownTest().getSuccessfulGets()).append(" | - |\n");
        content.append("\n");
        
        content.append("### 5.3 缓存雪崩防护\n\n");
        content.append("| 指标 | 结果 | 评估 |\n");
        content.append("|------|------|------|\n");
        content.append("| 测试key数量 | ").append(
                report.getProtectionTest().getAvalancheTest().getKeyCount()).append(" | - |\n");
        content.append("| 数据库调用次数 | ").append(
                report.getProtectionTest().getAvalancheTest().getDatabaseCalls()).append(" | ")
                .append(evaluateAvalancheProtection(
                        report.getProtectionTest().getAvalancheTest())).append(" |\n");
        content.append("| 总响应时间 | ").append(String.format("%.2f", 
                report.getProtectionTest().getAvalancheTest().getTotalResponseTimeNs() / 1_000_000_000.0))
                .append("s | - |\n");
        content.append("\n");
        
        // 热点key测试详情
        content.append("## 6. 热点key检测测试\n\n");
        content.append("| 指标 | 结果 | 评估 |\n");
        content.append("|------|------|------|\n");
        content.append("| 总key数量 | ").append(report.getHotspotTest().getTotalKeys())
                .append(" | - |\n");
        content.append("| 热点key数量 | ").append(report.getHotspotTest().getHotKeyCount())
                .append(" | - |\n");
        content.append("| 检测到的热点key | ").append(report.getHotspotTest().getDetectedHotKeys())
                .append(" | ").append(evaluateHotspotDetection(
                        report.getHotspotTest())).append(" |\n");
        content.append("| 检测率 | ").append(String.format("%.2f", 
                report.getHotspotTest().getDetectionRate() * 100)).append("% | - |\n");
        content.append("| 访问集中度 (基尼系数) | ").append(String.format("%.3f", 
                report.getHotspotTest().getAccessConcentration())).append(" | ")
                .append(evaluateAccessConcentration(
                        report.getHotspotTest().getAccessConcentration())).append(" |\n");
        content.append("\n");
        
        // 结论和建议
        content.append("## 7. 结论和建议\n\n");
        
        content.append("### 7.1 总体评估\n\n");
        content.append(buildOverallAssessment(report)).append("\n\n");
        
        content.append("### 7.2 优化建议\n\n");
        content.append(buildOptimizationSuggestions(report)).append("\n\n");
        
        content.append("### 7.3 风险提示\n\n");
        content.append(buildRiskWarnings(report)).append("\n\n");
        
        // 附录
        content.append("## 附录：测试环境配置\n\n");
        content.append("| 组件 | 版本/配置 |\n");
        content.append("|------|-----------|\n");
        content.append("| Spring Boot | 3.3.0 |\n");
        content.append("| Java | 21 |\n");
        content.append("| Caffeine | 3.1.8 |\n");
        content.append("| Redis Stack | 7.2.0 |\n");
        content.append("| Spring Data Redis | 3.3.0 |\n");
        content.append("| 测试机器配置 | 4核8GB |\n");
        content.append("| 操作系统 | Linux (容器环境) |\n");
        
        return content.toString();
    }
    
    /**
     * 评估加速比
     */
    private String evaluateSpeedup(double speedup) {
        if (speedup >= 100) return "优秀";
        if (speedup >= 50) return "良好";
        if (speedup >= 20) return "一般";
        return "需要优化";
    }
    
    /**
     * 评估命中率
     */
    private String evaluateHitRate(double hitRate) {
        if (hitRate >= 0.9) return "优秀";
        if (hitRate >= 0.8) return "良好";
        if (hitRate >= 0.7) return "一般";
        return "需要优化";
    }
    
    /**
     * 评估TPS
     */
    private String evaluateTPS(double tps) {
        if (tps >= 1000) return "优秀";
        if (tps >= 500) return "良好";
        if (tps >= 200) return "一般";
        return "需要优化";
    }
    
    /**
     * 评估延迟
     */
    private String evaluateLatency(long p99Ns) {
        double p99Ms = p99Ns / 1_000_000.0;
        if (p99Ms <= 10) return "优秀";
        if (p99Ms <= 50) return "良好";
        if (p99Ms <= 100) return "一般";
        return "需要优化";
    }
    
    /**
     * 评估穿透防护效果
     */
    private String evaluatePenetrationProtection(
            PerformanceTestService.PenetrationTestResult result) {
        double ratio = (double) result.getDatabaseCalls() / result.getRequestCount();
        if (ratio <= 0.1) return "优秀";
        if (ratio <= 0.3) return "良好";
        if (ratio <= 0.5) return "一般";
        return "防护不足";
    }
    
    /**
     * 评估击穿防护效果
     */
    private String evaluateBreakdownProtection(
            PerformanceTestService.BreakdownTestResult result) {
        double ratio = (double) result.getDatabaseCalls() / result.getConcurrentThreads();
        if (ratio <= 0.1) return "优秀";
        if (ratio <= 0.3) return "良好";
        if (ratio <= 0.5) return "一般";
        return "防护不足";
    }
    
    /**
     * 评估雪崩防护效果
     */
    private String evaluateAvalancheProtection(
            PerformanceTestService.AvalancheTestResult result) {
        double ratio = (double) result.getDatabaseCalls() / result.getKeyCount();
        if (ratio <= 0.1) return "优秀";
        if (ratio <= 0.3) return "良好";
        if (ratio <= 0.5) return "一般";
        return "防护不足";
    }
    
    /**
     * 评估热点key检测效果
     */
    private String evaluateHotspotDetection(
            PerformanceTestService.HotspotTestResult result) {
        if (result.getDetectionRate() >= 0.9) return "优秀";
        if (result.getDetectionRate() >= 0.7) return "良好";
        if (result.getDetectionRate() >= 0.5) return "一般";
        return "检测不足";
    }
    
    /**
     * 评估访问集中度
     */
    private String evaluateAccessConcentration(double giniCoefficient) {
        if (giniCoefficient >= 0.7) return "高度集中（需要优化）";
        if (giniCoefficient >= 0.5) return "中度集中";
        if (giniCoefficient >= 0.3) return "轻度集中";
        return "分布均匀";
    }
    
    /**
     * 构建总体评估
     */
    private String buildOverallAssessment(PerformanceTestService.PerformanceReport report) {
        StringBuilder assessment = new StringBuilder();
        
        assessment.append("### 7.1.1 优势\n\n");
        assessment.append("1. **高性能缓存访问**: 缓存读取相比数据库访问加速")
                .append(String.format("%.2f", report.getBaselineTest().getSpeedupFactor()))
                .append("倍，显著提升系统响应速度。\n");
        
        if (report.getHitRateTest().getHitRate() >= 0.9) {
            assessment.append("2. **优秀命中率**: 缓存命中率达到")
                    .append(String.format("%.2f", report.getHitRateTest().getHitRate() * 100))
                    .append("%，远超90%的目标要求。\n");
        }
        
        if (report.getConcurrencyTest().getThroughput() >= 1000) {
            assessment.append("3. **高并发处理能力**: 系统TPS达到")
                    .append(String.format("%.2f", report.getConcurrencyTest().getThroughput()))
                    .append("，满足生产级性能要求。\n");
        }
        
        assessment.append("\n### 7.1.2 改进空间\n\n");
        
        if (report.getHitRateTest().getHitRate() < 0.9) {
            assessment.append("1. **缓存命中率优化**: 当前命中率")
                    .append(String.format("%.2f", report.getHitRateTest().getHitRate() * 100))
                    .append("%未达到90%目标，建议优化缓存策略和容量规划。\n");
        }
        
        if (report.getConcurrencyTest().getP99ResponseTimeNs() / 1_000_000.0 > 100) {
            assessment.append("2. **尾部延迟优化**: P99响应时间")
                    .append(String.format("%.2f", 
                            report.getConcurrencyTest().getP99ResponseTimeNs() / 1_000_000.0))
                    .append("ms超过100ms阈值，建议优化网络和资源分配。\n");
        }
        
        assessment.append("\n### 7.1.3 总体评级\n\n");
        
        int score = 0;
        if (report.getBaselineTest().getSpeedupFactor() >= 50) score += 2;
        if (report.getHitRateTest().getHitRate() >= 0.9) score += 2;
        if (report.getConcurrencyTest().getThroughput() >= 1000) score += 2;
        if (report.getConcurrencyTest().getP99ResponseTimeNs() / 1_000_000.0 <= 100) score += 2;
        
        String rating;
        if (score >= 7) rating = "**A级（优秀）** - 完全满足生产要求";
        else if (score >= 5) rating = "**B级（良好）** - 基本满足生产要求";
        else if (score >= 3) rating = "**C级（一般）** - 需要部分优化";
        else rating = "**D级（需改进）** - 需要全面优化";
        
        assessment.append(rating).append("\n\n");
        
        return assessment.toString();
    }
    
    /**
     * 构建优化建议
     */
    private String buildOptimizationSuggestions(PerformanceTestService.PerformanceReport report) {
        StringBuilder suggestions = new StringBuilder();
        
        suggestions.append("### 7.2.1 缓存策略优化\n\n");
        
        // 命中率建议
        if (report.getHitRateTest().getHitRate() < 0.9) {
            suggestions.append("1. **增加缓存容量**: 当前命中率")
                    .append(String.format("%.2f", report.getHitRateTest().getHitRate() * 100))
                    .append("%，建议增加本地缓存容量至2000+条目，Redis内存至8GB+。\n");
            
            suggestions.append("2. **优化过期策略**: 对热点数据采用较短TTL（5-10分钟），"
                    + "静态数据采用较长TTL（24小时）。\n");
        }
        
        // 并发建议
        if (report.getConcurrencyTest().getFailedRequests() > 0) {
            suggestions.append("3. **优化线程池配置**: 当前失败请求")
                    .append(report.getConcurrencyTest().getFailedRequests())
                    .append("个，建议增加线程池核心线程数和队列容量。\n");
        }
        
        suggestions.append("\n### 7.2.2 架构优化\n\n");
        
        suggestions.append("1. **多级缓存精细化**: 根据数据类型设计不同缓存级别：\n");
        suggestions.append("   - L1（Caffeine）：会话、配置等高频小数据\n");
        suggestions.append("   - L2（Redis）：文档、向量等中频大数据\n");
        suggestions.append("   - L3（数据库）：低频、大容量数据\n");
        
        suggestions.append("2. **热点数据预加载**: 基于历史访问模式，预测并预加载热点数据。\n");
        
        suggestions.append("\n### 7.2.3 监控和告警优化\n\n");
        
        suggestions.append("1. **关键指标监控**: 建立实时监控看板，重点关注：\n");
        suggestions.append("   - 缓存命中率（目标 > 90%)\n");
        suggestions.append("   - P99响应时间（目标 < 100ms）\n");
        suggestions.append("   - 系统吞吐量（目标 > 1000 TPS）\n");
        
        suggestions.append("2. **自动扩缩容**: 基于监控指标实现资源的自动扩缩容。\n");
        
        return suggestions.toString();
    }
    
    /**
     * 构建风险提示
     */
    private String buildRiskWarnings(PerformanceTestService.PerformanceReport report) {
        StringBuilder warnings = new StringBuilder();
        
        warnings.append("### 7.3.1 已知风险\n\n");
        
        warnings.append("1. **缓存一致性问题**: 多级缓存架构下，数据一致性维护复杂，"
                + "建议实现缓存同步机制或设置合理的过期时间。\n");
        
        warnings.append("2. **雪崩风险**: 尽管已实现随机抖动，但在极端情况下仍可能发生雪崩，"
                + "建议实现分层过期和熔断降级。\n");
        
        warnings.append("3. **内存泄漏风险**: Caffeine本地缓存如配置不当可能导致内存泄漏，"
                + "建议定期监控内存使用并设置合理的淘汰策略。\n");
        
        warnings.append("\n### 7.3.2 应急预案\n\n");
        
        warnings.append("1. **缓存降级**: 在缓存系统故障时，自动切换到直接数据库访问，"
                + "保障核心功能可用性。\n");
        
        warnings.append("2. **快速恢复**: 建立缓存数据备份和快速恢复机制，"
                + "确保在故障后能快速重建缓存。\n");
        
        warnings.append("3. **容量预警**: 设置容量使用阈值，提前触发扩容操作，"
                + "避免因容量不足导致的性能下降。\n");
        
        return warnings.toString();
    }
}
