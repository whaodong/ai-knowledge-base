package com.example.rag.service.validation;

import com.example.rag.model.RetrievalResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 答案验证服务
 * 
 * <p>集成所有验证组件，提供统一的答案验证接口。</p>
 * <p>功能特点：</p>
 * <ul>
 *   <li>多维度评分</li>
 *   <li>事实一致性验证</li>
 *   <li>引用溯源验证</li>
 *   <li>幻觉检测</li>
 *   <li>答案修正</li>
 *   <li>验证报告生成</li>
 * </ul>
 */
@Slf4j
@Service
public class AnswerValidationService {
    
    @Autowired
    private FactConsistencyChecker factConsistencyChecker;
    
    @Autowired
    private CitationValidator citationValidator;
    
    @Autowired
    private AnswerScorer answerScorer;
    
    @Autowired
    private AnswerCorrector answerCorrector;
    
    @Autowired
    private HallucinationDetector hallucinationDetector;
    
    @Autowired
    private ValidationReportGenerator reportGenerator;
    
    @Value("${rag.validation.enabled:true}")
    private boolean validationEnabled;
    
    @Value("${rag.validation.auto-correct:true}")
    private boolean autoCorrectEnabled;
    
    @Value("${rag.validation.confidence-threshold:0.7}")
    private double confidenceThreshold;
    
    /**
     * 验证答案（完整验证）
     * 
     * @param answer 生成的答案
     * @param query 用户问题
     * @param retrievalResults 检索结果列表
     * @return 验证结果
     */
    public ValidationResult validate(String answer, String query, List<RetrievalResult> retrievalResults) {
        if (!validationEnabled) {
            log.debug("验证功能已禁用，跳过验证");
            return ValidationResult.disabled(answer);
        }
        
        long startTime = System.currentTimeMillis();
        log.info("开始答案验证，答案长度: {}, 检索结果数: {}", 
                answer != null ? answer.length() : 0, 
                retrievalResults != null ? retrievalResults.size() : 0);
        
        try {
            // 1. 事实一致性检查
            long consistencyStart = System.currentTimeMillis();
            FactConsistencyChecker.ConsistencyCheckResult consistencyResult = 
                    factConsistencyChecker.check(answer, retrievalResults);
            long consistencyTime = System.currentTimeMillis() - consistencyStart;
            log.debug("一致性检查完成，耗时: {}ms", consistencyTime);
            
            // 2. 引用验证
            long citationStart = System.currentTimeMillis();
            CitationValidator.CitationValidationResult citationResult = 
                    citationValidator.validate(answer, retrievalResults);
            long citationTime = System.currentTimeMillis() - citationStart;
            log.debug("引用验证完成，耗时: {}ms", citationTime);
            
            // 3. 幻觉检测
            long hallucinationStart = System.currentTimeMillis();
            HallucinationDetector.HallucinationDetectionResult hallucinationResult = 
                    hallucinationDetector.detect(answer, retrievalResults);
            long hallucinationTime = System.currentTimeMillis() - hallucinationStart;
            log.debug("幻觉检测完成，耗时: {}ms", hallucinationTime);
            
            // 4. 多维度评分
            long scoringStart = System.currentTimeMillis();
            AnswerScore score = answerScorer.score(answer, query, retrievalResults);
            long scoringTime = System.currentTimeMillis() - scoringStart;
            log.debug("评分完成，耗时: {}ms", scoringTime);
            
            // 5. 答案修正（如果需要）
            String finalAnswer = answer;
            AnswerCorrector.CorrectedAnswer correctedAnswer = null;
            boolean wasCorrected = false;
            
            if (autoCorrectEnabled && score.getConfidence() < confidenceThreshold) {
                long correctionStart = System.currentTimeMillis();
                correctedAnswer = answerCorrector.correct(answer, score, retrievalResults);
                finalAnswer = correctedAnswer.getCorrectedText();
                wasCorrected = true;
                long correctionTime = System.currentTimeMillis() - correctionStart;
                log.info("答案已修正，耗时: {}ms", correctionTime);
            }
            
            // 6. 生成验证报告
            long reportStart = System.currentTimeMillis();
            ValidationReportGenerator.ValidationReport report = reportGenerator.generateReport(
                    answer, query, score, consistencyResult, citationResult, 
                    hallucinationResult, retrievalResults);
            long reportTime = System.currentTimeMillis() - reportStart;
            log.debug("报告生成完成，耗时: {}ms", reportTime);
            
            long totalTime = System.currentTimeMillis() - startTime;
            
            log.info("答案验证完成: 置信度={}, 耗时={}ms", 
                    score.getConfidence(), totalTime);
            
            // 7. 构建返回结果
            return ValidationResult.builder()
                    .originalAnswer(answer)
                    .finalAnswer(finalAnswer)
                    .wasCorrected(wasCorrected)
                    .score(score)
                    .consistencyResult(consistencyResult)
                    .citationResult(citationResult)
                    .hallucinationResult(hallucinationResult)
                    .correctedAnswer(correctedAnswer)
                    .report(report)
                    .totalValidationTimeMs(totalTime)
                    .consistencyCheckTimeMs(consistencyTime)
                    .citationValidationTimeMs(citationTime)
                    .hallucinationDetectionTimeMs(hallucinationTime)
                    .scoringTimeMs(scoringTime)
                    .passed(score.getConfidence() >= confidenceThreshold)
                    .build();
            
        } catch (Exception e) {
            log.error("答案验证失败", e);
            return ValidationResult.error(answer, "验证失败: " + e.getMessage());
        }
    }
    
    /**
     * 快速验证（仅评分）
     * 
     * @param answer 生成的答案
     * @param query 用户问题
     * @param retrievalResults 检索结果列表
     * @return 评分结果
     */
    public AnswerScore quickScore(String answer, String query, List<RetrievalResult> retrievalResults) {
        if (!validationEnabled) {
            return createDefaultScore();
        }
        
        return answerScorer.score(answer, query, retrievalResults);
    }
    
    /**
     * 仅验证一致性
     */
    public FactConsistencyChecker.ConsistencyCheckResult validateConsistency(
            String answer, List<RetrievalResult> retrievalResults) {
        return factConsistencyChecker.check(answer, retrievalResults);
    }
    
    /**
     * 仅验证引用
     */
    public CitationValidator.CitationValidationResult validateCitations(
            String answer, List<RetrievalResult> retrievalResults) {
        return citationValidator.validate(answer, retrievalResults);
    }
    
    /**
     * 仅检测幻觉
     */
    public HallucinationDetector.HallucinationDetectionResult detectHallucinations(
            String answer, List<RetrievalResult> retrievalResults) {
        return hallucinationDetector.detect(answer, retrievalResults);
    }
    
    /**
     * 修正答案
     */
    public AnswerCorrector.CorrectedAnswer correctAnswer(
            String answer, AnswerScore score, List<RetrievalResult> retrievalResults) {
        return answerCorrector.correct(answer, score, retrievalResults);
    }
    
    /**
     * 生成验证报告
     */
    public ValidationReportGenerator.ValidationReport generateReport(
            String answer,
            String query,
            AnswerScore score,
            FactConsistencyChecker.ConsistencyCheckResult consistencyResult,
            CitationValidator.CitationValidationResult citationResult,
            HallucinationDetector.HallucinationDetectionResult hallucinationResult,
            List<RetrievalResult> retrievalResults) {
        
        return reportGenerator.generateReport(
                answer, query, score, consistencyResult, 
                citationResult, hallucinationResult, retrievalResults);
    }
    
    /**
     * 批量验证
     */
    public List<ValidationResult> batchValidate(
            List<String> answers,
            List<String> queries,
            List<List<RetrievalResult>> retrievalResults) {
        
        if (answers.size() != queries.size() || answers.size() != retrievalResults.size()) {
            throw new IllegalArgumentException("输入列表大小不一致");
        }
        
        return java.util.stream.IntStream.range(0, answers.size())
                .mapToObj(i -> validate(answers.get(i), queries.get(i), retrievalResults.get(i)))
                .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * 创建默认评分
     */
    private AnswerScore createDefaultScore() {
        return AnswerScore.builder()
                .relevance(0.5)
                .consistency(0.5)
                .citationScore(0.5)
                .completeness(0.5)
                .hallucinationScore(0.5)
                .confidence(0.5)
                .level(AnswerScore.ScoreLevel.ACCEPTABLE)
                .build();
    }
    
    // ============= 内部类 =============
    
    /**
     * 验证结果
     */
    @lombok.Data
    @lombok.Builder
    public static class ValidationResult {
        private String originalAnswer;
        private String finalAnswer;
        private boolean wasCorrected;
        private AnswerScore score;
        private FactConsistencyChecker.ConsistencyCheckResult consistencyResult;
        private CitationValidator.CitationValidationResult citationResult;
        private HallucinationDetector.HallucinationDetectionResult hallucinationResult;
        private AnswerCorrector.CorrectedAnswer correctedAnswer;
        private ValidationReportGenerator.ValidationReport report;
        private long totalValidationTimeMs;
        private long consistencyCheckTimeMs;
        private long citationValidationTimeMs;
        private long hallucinationDetectionTimeMs;
        private long scoringTimeMs;
        private boolean passed;
        
        public static ValidationResult disabled(String answer) {
            return ValidationResult.builder()
                    .originalAnswer(answer)
                    .finalAnswer(answer)
                    .wasCorrected(false)
                    .passed(true)
                    .build();
        }
        
        public static ValidationResult error(String answer, String error) {
            return ValidationResult.builder()
                    .originalAnswer(answer)
                    .finalAnswer(answer)
                    .wasCorrected(false)
                    .passed(false)
                    .build();
        }
        
        public boolean needsAttention() {
            return !passed || wasCorrected;
        }
        
        public String getMarkdownReport() {
            if (report == null) {
                return "无验证报告";
            }
            return new ValidationReportGenerator().generateMarkdownReport(report);
        }
    }
}
