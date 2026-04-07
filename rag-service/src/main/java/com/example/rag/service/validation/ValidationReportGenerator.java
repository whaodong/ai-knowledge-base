package com.example.rag.service.validation;

import com.example.rag.model.RetrievalResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 验证报告生成器
 * 
 * <p>生成详细的答案验证报告，包括各项指标和建议。</p>
 */
@Slf4j
@Component
public class ValidationReportGenerator {
    
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    /**
     * 生成完整的验证报告
     * 
     * @param answer 答案
     * @param query 问题
     * @param score 评分
     * @param consistencyResult 一致性检查结果
     * @param citationResult 引用验证结果
     * @param hallucinationResult 幻觉检测结果
     * @param retrievalResults 检索结果
     * @return 验证报告
     */
    public ValidationReport generateReport(
            String answer,
            String query,
            AnswerScore score,
            FactConsistencyChecker.ConsistencyCheckResult consistencyResult,
            CitationValidator.CitationValidationResult citationResult,
            HallucinationDetector.HallucinationDetectionResult hallucinationResult,
            List<RetrievalResult> retrievalResults) {
        
        long startTime = System.currentTimeMillis();
        
        // 生成报告ID
        String reportId = UUID.randomUUID().toString();
        
        // 构建报告
        ValidationReport report = ValidationReport.builder()
                .reportId(reportId)
                .timestamp(new Date())
                .query(query)
                .answer(answer)
                .overallScore(score)
                .build();
        
        // 1. 添加评分详情
        report.setScoreBreakdown(generateScoreBreakdown(score));
        
        // 2. 添加一致性分析
        report.setConsistencyAnalysis(generateConsistencyAnalysis(consistencyResult));
        
        // 3. 添加引用分析
        report.setCitationAnalysis(generateCitationAnalysis(citationResult));
        
        // 4. 添加幻觉分析
        report.setHallucinationAnalysis(generateHallucinationAnalysis(hallucinationResult));
        
        // 5. 添加来源信息
        report.setSourceInfo(generateSourceInfo(retrievalResults));
        
        // 6. 生成建议
        report.setRecommendations(generateRecommendations(
                score, consistencyResult, citationResult, hallucinationResult));
        
        // 7. 生成总结
        report.setSummary(generateSummary(report));
        
        long duration = System.currentTimeMillis() - startTime;
        report.setReportGenerationTimeMs(duration);
        
        log.info("验证报告生成完成: reportId={}, 耗时={}ms", reportId, duration);
        
        return report;
    }
    
    /**
     * 生成评分详情
     */
    private ScoreBreakdown generateScoreBreakdown(AnswerScore score) {
        return ScoreBreakdown.builder()
                .relevance(score.getRelevance())
                .consistency(score.getConsistency())
                .citationScore(score.getCitationScore())
                .completeness(score.getCompleteness())
                .hallucinationScore(score.getHallucinationScore())
                .overallConfidence(score.getConfidence())
                .level(score.getLevel().getLabel())
                .weakDimensions(score.getWeakDimensions())
                .build();
    }
    
    /**
     * 生成一致性分析
     */
    private ConsistencyAnalysis generateConsistencyAnalysis(
            FactConsistencyChecker.ConsistencyCheckResult result) {
        
        return ConsistencyAnalysis.builder()
                .totalClaims(result.getClaims().size())
                .supportedClaims(result.getSupportedClaimsCount())
                .unsupportedClaims(result.getUnsupportedClaimsCount())
                .consistencyScore(result.getConsistencyScore())
                .hasContradictions(result.hasContradictions())
                .contradictionCount(result.getContradictions().size())
                .details(generateConsistencyDetails(result))
                .build();
    }
    
    /**
     * 生成一致性详情
     */
    private List<ClaimDetail> generateConsistencyDetails(
            FactConsistencyChecker.ConsistencyCheckResult result) {
        
        return result.getVerifications().stream()
                .limit(10) // 限制数量
                .map(v -> ClaimDetail.builder()
                        .claim(v.getClaim().getText())
                        .supported(v.isSupported())
                        .matchScore(v.getMaxMatchScore())
                        .type(v.getClaim().getType().name())
                        .build())
                .collect(Collectors.toList());
    }
    
    /**
     * 生成引用分析
     */
    private CitationAnalysis generateCitationAnalysis(
            CitationValidator.CitationValidationResult result) {
        
        return CitationAnalysis.builder()
                .totalCitations(result.getCitations().size())
                .validCitations(result.getValidCitationsCount())
                .invalidCitations(result.getInvalidCitationsCount())
                .citationScore(result.getCitationScore())
                .unreferencedClaimsCount(result.getUnreferencedClaims().size())
                .details(generateCitationDetails(result))
                .build();
    }
    
    /**
     * 生成引用详情
     */
    private List<CitationDetail> generateCitationDetails(
            CitationValidator.CitationValidationResult result) {
        
        return result.getVerifications().stream()
                .limit(10)
                .map(v -> CitationDetail.builder()
                        .citationText(v.getCitation().getText())
                        .valid(v.isValid())
                        .credibilityScore(v.getCredibilityScore())
                        .sourceName(v.getCitation().getType().name())
                        .build())
                .collect(Collectors.toList());
    }
    
    /**
     * 生成幻觉分析
     */
    private HallucinationAnalysis generateHallucinationAnalysis(
            HallucinationDetector.HallucinationDetectionResult result) {
        
        return HallucinationAnalysis.builder()
                .totalIndicators(result.getIndicators().size())
                .highSeverityCount(result.getHighSeverityCount())
                .hallucinationScore(result.getHallucinationScore())
                .hasHallucinations(result.hasHallucinations())
                .indicatorsByType(result.getIndicatorsByType())
                .details(generateHallucinationDetails(result))
                .build();
    }
    
    /**
     * 生成幻觉详情
     */
    private List<HallucinationDetail> generateHallucinationDetails(
            HallucinationDetector.HallucinationDetectionResult result) {
        
        return result.getIndicators().stream()
                .limit(10)
                .map(i -> HallucinationDetail.builder()
                        .type(i.getType().getLabel())
                        .text(i.getText())
                        .severity(i.getSeverity().name())
                        .description(i.getDescription())
                        .build())
                .collect(Collectors.toList());
    }
    
    /**
     * 生成来源信息
     */
    private SourceInfo generateSourceInfo(List<RetrievalResult> results) {
        Set<String> sources = results.stream()
                .map(r -> r.getMetadata().getOrDefault("source", "未知").toString())
                .collect(Collectors.toSet());
        
        return SourceInfo.builder()
                .totalDocuments(results.size())
                .uniqueSources(sources.size())
                .sourceList(new ArrayList<>(sources))
                .averageRelevanceScore(results.stream()
                        .mapToDouble(RetrievalResult::getRawScore)
                        .average()
                        .orElse(0.0))
                .build();
    }
    
    /**
     * 生成建议
     */
    private List<Recommendation> generateRecommendations(
            AnswerScore score,
            FactConsistencyChecker.ConsistencyCheckResult consistencyResult,
            CitationValidator.CitationValidationResult citationResult,
            HallucinationDetector.HallucinationDetectionResult hallucinationResult) {
        
        List<Recommendation> recommendations = new ArrayList<>();
        
        // 基于评分的建议
        if (score.getConfidence() < 0.7) {
            recommendations.add(new Recommendation(
                    "置信度",
                    "高",
                    String.format("答案置信度为%.1f%%，建议谨慎使用", score.getConfidence() * 100),
                    "查看原始来源或咨询专家"
            ));
        }
        
        // 基于一致性的建议
        if (consistencyResult.hasContradictions()) {
            recommendations.add(new Recommendation(
                    "一致性",
                    "高",
                    String.format("发现%d个矛盾点，部分内容可能与来源不一致", 
                            consistencyResult.getContradictions().size()),
                    "核实矛盾内容，参考多个来源"
            ));
        }
        
        // 基于引用的建议
        if (citationResult.getInvalidCitationsCount() > 0) {
            recommendations.add(new Recommendation(
                    "引用",
                    "中",
                    String.format("有%d个引用未找到对应来源", citationResult.getInvalidCitationsCount()),
                    "检查引用的准确性"
            ));
        }
        
        // 基于幻觉的建议
        if (hallucinationResult.hasHallucinations()) {
            recommendations.add(new Recommendation(
                    "可信度",
                    "高",
                    String.format("检测到%d个潜在幻觉指标", hallucinationResult.getIndicators().size()),
                    "核实关键数据和信息"
            ));
        }
        
        // 基于各维度的建议
        Map<String, Double> weakDimensions = score.getWeakDimensions();
        for (Map.Entry<String, Double> entry : weakDimensions.entrySet()) {
            recommendations.add(new Recommendation(
                    entry.getKey(),
                    "中",
                    String.format("%s评分仅为%.0f%%，有待改进", entry.getKey(), entry.getValue() * 100),
                    getImprovementSuggestion(entry.getKey())
            ));
        }
        
        return recommendations;
    }
    
    /**
     * 获取改进建议
     */
    private String getImprovementSuggestion(String dimension) {
        switch (dimension) {
            case "相关性":
                return "确保答案直接回应问题，避免无关信息";
            case "一致性":
                return "对比多个来源，确保事实准确";
            case "引用质量":
                return "添加可靠的引用来源，提高可信度";
            case "完整性":
                return "补充缺失的信息，提供更全面的回答";
            case "可信度":
                return "核实数据和事实，避免过度推断";
            default:
                return "改进答案质量";
        }
    }
    
    /**
     * 生成总结
     */
    private String generateSummary(ValidationReport report) {
        StringBuilder summary = new StringBuilder();
        
        summary.append(String.format("本答案的综合置信度为 %.1f%%（%s）。",
                report.getOverallScore().getConfidence() * 100,
                report.getOverallScore().getLevel().getLabel()));
        
        if (report.getOverallScore().isHighQuality()) {
            summary.append("答案质量良好，可以放心使用。");
        } else {
            summary.append("答案存在一些问题，建议核实后使用。");
        }
        
        // 添加主要问题
        if (report.getConsistencyAnalysis().isHasContradictions()) {
            summary.append("部分内容与来源不一致。");
        }
        
        if (report.getHallucinationAnalysis().isHasHallucinations()) {
            summary.append("检测到潜在的不准确信息。");
        }
        
        if (report.getCitationAnalysis().getUnreferencedClaimsCount() > 0) {
            summary.append("部分重要信息缺少引用。");
        }
        
        return summary.toString();
    }
    
    /**
     * 生成Markdown格式报告
     */
    public String generateMarkdownReport(ValidationReport report) {
        StringBuilder md = new StringBuilder();
        
        md.append("# 答案验证报告\n\n");
        md.append(String.format("**报告ID**: %s  \n", report.getReportId()));
        md.append(String.format("**生成时间**: %s  \n", DATE_FORMAT.format(report.getTimestamp())));
        md.append(String.format("**查询**: %s  \n\n", report.getQuery()));
        
        // 总体评分
        md.append("## 📊 总体评分\n\n");
        md.append(String.format("**综合置信度**: %.1f%% (%s)  \n\n",
                report.getOverallScore().getConfidence() * 100,
                report.getOverallScore().getLevel().getLabel()));
        
        // 评分详情表格
        md.append("| 维度 | 评分 | 状态 |\n");
        md.append("|------|------|------|\n");
        
        ScoreBreakdown breakdown = report.getScoreBreakdown();
        md.append(String.format("| 相关性 | %.1f%% | %s |\n",
                breakdown.getRelevance() * 100,
                getStatusEmoji(breakdown.getRelevance())));
        md.append(String.format("| 一致性 | %.1f%% | %s |\n",
                breakdown.getConsistency() * 100,
                getStatusEmoji(breakdown.getConsistency())));
        md.append(String.format("| 引用质量 | %.1f%% | %s |\n",
                breakdown.getCitationScore() * 100,
                getStatusEmoji(breakdown.getCitationScore())));
        md.append(String.format("| 完整性 | %.1f%% | %s |\n",
                breakdown.getCompleteness() * 100,
                getStatusEmoji(breakdown.getCompleteness())));
        md.append(String.format("| 可信度 | %.1f%% | %s |\n\n",
                breakdown.getHallucinationScore() * 100,
                getStatusEmoji(breakdown.getHallucinationScore())));
        
        // 建议
        if (!report.getRecommendations().isEmpty()) {
            md.append("## 💡 改进建议\n\n");
            for (Recommendation rec : report.getRecommendations()) {
                md.append(String.format("- **[%s]** %s\n  - 建议：%s\n\n",
                        rec.getPriority(), rec.getIssue(), rec.getSuggestion()));
            }
        }
        
        // 总结
        md.append("## 📝 总结\n\n");
        md.append(report.getSummary());
        
        return md.toString();
    }
    
    /**
     * 获取状态表情
     */
    private String getStatusEmoji(double score) {
        if (score >= 0.8) return "✅ 良好";
        if (score >= 0.6) return "⚠️ 一般";
        return "❌ 较差";
    }
    
    // ============= 内部类 =============
    
    /**
     * 验证报告
     */
    @lombok.Data
    @lombok.Builder
    public static class ValidationReport {
        private String reportId;
        private Date timestamp;
        private String query;
        private String answer;
        private AnswerScore overallScore;
        private ScoreBreakdown scoreBreakdown;
        private ConsistencyAnalysis consistencyAnalysis;
        private CitationAnalysis citationAnalysis;
        private HallucinationAnalysis hallucinationAnalysis;
        private SourceInfo sourceInfo;
        private List<Recommendation> recommendations;
        private String summary;
        private long reportGenerationTimeMs;
    }
    
    /**
     * 评分详情
     */
    @lombok.Data
    @lombok.Builder
    public static class ScoreBreakdown {
        private double relevance;
        private double consistency;
        private double citationScore;
        private double completeness;
        private double hallucinationScore;
        private double overallConfidence;
        private String level;
        private Map<String, Double> weakDimensions;
    }
    
    /**
     * 一致性分析
     */
    @lombok.Data
    @lombok.Builder
    public static class ConsistencyAnalysis {
        private int totalClaims;
        private int supportedClaims;
        private int unsupportedClaims;
        private double consistencyScore;
        private boolean hasContradictions;
        private int contradictionCount;
        private List<ClaimDetail> details;
    }
    
    /**
     * 声明详情
     */
    @lombok.Data
    @lombok.Builder
    public static class ClaimDetail {
        private String claim;
        private boolean supported;
        private double matchScore;
        private String type;
    }
    
    /**
     * 引用分析
     */
    @lombok.Data
    @lombok.Builder
    public static class CitationAnalysis {
        private int totalCitations;
        private int validCitations;
        private int invalidCitations;
        private double citationScore;
        private int unreferencedClaimsCount;
        private List<CitationDetail> details;
    }
    
    /**
     * 引用详情
     */
    @lombok.Data
    @lombok.Builder
    public static class CitationDetail {
        private String citationText;
        private boolean valid;
        private double credibilityScore;
        private String sourceName;
    }
    
    /**
     * 幻觉分析
     */
    @lombok.Data
    @lombok.Builder
    public static class HallucinationAnalysis {
        private int totalIndicators;
        private int highSeverityCount;
        private double hallucinationScore;
        private boolean hasHallucinations;
        private Map<HallucinationDetector.HallucinationType, Long> indicatorsByType;
        private List<HallucinationDetail> details;
    }
    
    /**
     * 幻觉详情
     */
    @lombok.Data
    @lombok.Builder
    public static class HallucinationDetail {
        private String type;
        private String text;
        private String severity;
        private String description;
    }
    
    /**
     * 来源信息
     */
    @lombok.Data
    @lombok.Builder
    public static class SourceInfo {
        private int totalDocuments;
        private int uniqueSources;
        private List<String> sourceList;
        private double averageRelevanceScore;
    }
    
    /**
     * 建议
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class Recommendation {
        private String dimension;
        private String priority;
        private String issue;
        private String suggestion;
    }
}
