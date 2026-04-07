package com.example.rag.service.validation;

import com.example.rag.model.RetrievalResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 答案修正服务
 * 
 * <p>对低置信度答案进行修正和优化。</p>
 * <p>功能特点：</p>
 * <ul>
 *   <li>低置信度答案标记</li>
 *   <li>自动添加免责声明</li>
 *   <li>建议用户核实</li>
 *   <li>提供原始来源链接</li>
 * </ul>
 */
@Slf4j
@Service
public class AnswerCorrector {
    
    @Value("${rag.correction.confidence-threshold:0.7}")
    private double confidenceThreshold;
    
    @Value("${rag.correction.enable-disclaimer:true}")
    private boolean enableDisclaimer;
    
    @Value("${rag.correction.enable-suggestions:true}")
    private boolean enableSuggestions;
    
    @Value("${rag.correction.enable-sources:true}")
    private boolean enableSources;
    
    /**
     * 修正答案
     * 
     * @param answer 原始答案
     * @param score 答案评分
     * @param retrievalResults 检索结果
     * @return 修正后的答案
     */
    public CorrectedAnswer correct(String answer, AnswerScore score, List<RetrievalResult> retrievalResults) {
        if (answer == null || answer.isEmpty()) {
            return CorrectedAnswer.empty("答案为空");
        }
        
        long startTime = System.currentTimeMillis();
        
        StringBuilder correctedAnswer = new StringBuilder(answer);
        List<Correction> corrections = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        // 1. 检查是否需要修正
        boolean needsCorrection = score.getConfidence() < confidenceThreshold;
        
        if (needsCorrection) {
            log.info("答案置信度 {} 低于阈值 {}，开始修正", score.getConfidence(), confidenceThreshold);
            
            // 2. 添加置信度警告
            if (enableDisclaimer) {
                String disclaimer = generateDisclaimer(score);
                correctedAnswer.insert(0, disclaimer + "\n\n");
                corrections.add(new Correction(
                        CorrectionType.DISCLAIMER,
                        "添加置信度警告",
                        disclaimer
                ));
            }
            
            // 3. 标记弱项
            Map<String, Double> weakDimensions = score.getWeakDimensions();
            if (!weakDimensions.isEmpty()) {
                String warning = generateWeaknessWarning(weakDimensions);
                warnings.add(warning);
                corrections.add(new Correction(
                        CorrectionType.WARNING,
                        "标记质量弱项",
                        warning
                ));
            }
        }
        
        // 4. 添加建议
        if (enableSuggestions && needsCorrection) {
            List<String> suggestions = generateSuggestions(score, retrievalResults);
            if (!suggestions.isEmpty()) {
                String suggestionText = formatSuggestions(suggestions);
                correctedAnswer.append("\n\n").append(suggestionText);
                corrections.add(new Correction(
                        CorrectionType.SUGGESTION,
                        "添加核实建议",
                        suggestionText
                ));
            }
        }
        
        // 5. 添加来源链接
        if (enableSources && !retrievalResults.isEmpty()) {
            String sourcesSection = formatSources(retrievalResults);
            correctedAnswer.append("\n\n").append(sourcesSection);
            corrections.add(new Correction(
                        CorrectionType.SOURCE,
                        "添加来源引用",
                        sourcesSection
                ));
        }
        
        long duration = System.currentTimeMillis() - startTime;
        
        log.info("答案修正完成: 修正项={}, 耗时={}ms", corrections.size(), duration);
        
        return new CorrectedAnswer(
                correctedAnswer.toString(),
                corrections,
                warnings,
                needsCorrection,
                duration
        );
    }
    
    /**
     * 生成免责声明
     */
    private String generateDisclaimer(AnswerScore score) {
        StringBuilder disclaimer = new StringBuilder();
        
        disclaimer.append("⚠️ **注意**\n");
        disclaimer.append(String.format("本答案的置信度为 **%.1f%%**，", score.getConfidence() * 100));
        disclaimer.append("可能存在不准确或不确定的内容。\n\n");
        
        // 添加具体问题说明
        if (score.getConsistency() < 0.7) {
            disclaimer.append("• 部分内容可能与参考资料不一致\n");
        }
        if (score.getCitationScore() < 0.7) {
            disclaimer.append("• 引用来源验证不充分\n");
        }
        if (score.getHallucinationScore() < 0.7) {
            disclaimer.append("• 可能包含未经验证的信息\n");
        }
        
        disclaimer.append("\n建议您核实关键信息后再做决策。");
        
        return disclaimer.toString();
    }
    
    /**
     * 生成弱项警告
     */
    private String generateWeaknessWarning(Map<String, Double> weakDimensions) {
        StringBuilder warning = new StringBuilder();
        warning.append("以下维度的评分较低，需要注意：\n");
        
        weakDimensions.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .forEach(entry -> {
                    warning.append(String.format("• %s: %.0f%%\n", 
                            entry.getKey(), entry.getValue() * 100));
                });
        
        return warning.toString();
    }
    
    /**
     * 生成建议
     */
    private List<String> generateSuggestions(AnswerScore score, List<RetrievalResult> results) {
        List<String> suggestions = new ArrayList<>();
        
        // 基于评分维度生成建议
        if (score.getConsistency() < 0.7) {
            suggestions.add("建议对比多个来源验证事实准确性");
        }
        
        if (score.getCitationScore() < 0.7) {
            suggestions.add("建议查看原始文献或官方资料");
        }
        
        if (score.getCompleteness() < 0.7) {
            suggestions.add("建议进一步了解相关背景信息");
        }
        
        if (score.getHallucinationScore() < 0.7) {
            suggestions.add("建议谨慎对待未经证实的数据和陈述");
        }
        
        // 基于检索结果生成建议
        if (!results.isEmpty()) {
            suggestions.add("可以查看下方的参考来源获取更多详细信息");
        }
        
        // 添加通用建议
        if (score.getConfidence() < 0.5) {
            suggestions.add("建议咨询专业人士或查阅权威资料");
        }
        
        return suggestions;
    }
    
    /**
     * 格式化建议
     */
    private String formatSuggestions(List<String> suggestions) {
        StringBuilder text = new StringBuilder();
        text.append("💡 **建议**\n");
        
        for (int i = 0; i < suggestions.size(); i++) {
            text.append(String.format("%d. %s\n", i + 1, suggestions.get(i)));
        }
        
        return text.toString();
    }
    
    /**
     * 格式化来源
     */
    private String formatSources(List<RetrievalResult> results) {
        StringBuilder text = new StringBuilder();
        text.append("📚 **参考来源**\n\n");
        
        // 去重
        Set<String> seenSources = new HashSet<>();
        int index = 1;
        
        for (RetrievalResult result : results) {
            String source = result.getMetadata().getOrDefault("source", "未知来源").toString();
            
            if (!seenSources.contains(source)) {
                seenSources.add(source);
                
                text.append(String.format("[%d] ", index));
                text.append(source);
                
                // 添加URL（如果有）
                if (result.getMetadata().containsKey("url")) {
                    text.append(" - ");
                    text.append(result.getMetadata().get("url"));
                }
                
                // 添加时间（如果有）
                if (result.getMetadata().containsKey("timestamp")) {
                    text.append(" (");
                    text.append(result.getMetadata().get("timestamp"));
                    text.append(")");
                }
                
                text.append("\n");
                index++;
            }
        }
        
        return text.toString();
    }
    
    /**
     * 高质量答案增强
     * 
     * @param answer 原始答案
     * @param retrievalResults 检索结果
     * @return 增强后的答案
     */
    public String enhance(String answer, List<RetrievalResult> retrievalResults) {
        if (answer == null || answer.isEmpty()) {
            return answer;
        }
        
        StringBuilder enhanced = new StringBuilder(answer);
        
        // 添加质量标记
        enhanced.insert(0, "✅ **高质量答案**\n\n");
        
        // 添加来源
        if (!retrievalResults.isEmpty()) {
            enhanced.append("\n\n").append(formatSources(retrievalResults));
        }
        
        return enhanced.toString();
    }
    
    /**
     * 批量修正
     */
    public List<CorrectedAnswer> batchCorrect(
            List<String> answers, 
            List<AnswerScore> scores, 
            List<List<RetrievalResult>> retrievalResults) {
        
        if (answers.size() != scores.size() || answers.size() != retrievalResults.size()) {
            throw new IllegalArgumentException("输入列表大小不一致");
        }
        
        List<CorrectedAnswer> corrected = new ArrayList<>();
        
        for (int i = 0; i < answers.size(); i++) {
            corrected.add(correct(answers.get(i), scores.get(i), retrievalResults.get(i)));
        }
        
        return corrected;
    }
    
    // ============= 内部类 =============
    
    /**
     * 修正类型
     */
    public enum CorrectionType {
        DISCLAIMER,    // 免责声明
        WARNING,       // 警告标记
        SUGGESTION,    // 建议
        SOURCE,        // 来源引用
        FORMATTING     // 格式化
    }
    
    /**
     * 修正项
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class Correction {
        private CorrectionType type;
        private String description;
        private String content;
    }
    
    /**
     * 修正后的答案
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class CorrectedAnswer {
        private String correctedText;
        private List<Correction> corrections;
        private List<String> warnings;
        private boolean wasCorrected;
        private long durationMs;
        
        public static CorrectedAnswer empty(String reason) {
            return new CorrectedAnswer(
                    "",
                    Collections.emptyList(),
                    Collections.singletonList(reason),
                    false,
                    0
            );
        }
        
        public boolean hasWarnings() {
            return !warnings.isEmpty();
        }
        
        public int getCorrectionsCount() {
            return corrections.size();
        }
    }
}
