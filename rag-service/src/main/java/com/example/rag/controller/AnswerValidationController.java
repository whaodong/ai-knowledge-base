package com.example.rag.controller;

import com.example.rag.model.RetrievalResult;
import com.example.rag.service.validation.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 答案验证控制器
 * 
 * <p>提供答案验证相关的REST API接口。</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/validation")
public class AnswerValidationController {
    
    @Autowired
    private AnswerValidationService validationService;
    
    @Autowired
    private AnswerScorer answerScorer;
    
    @Autowired
    private FactConsistencyChecker consistencyChecker;
    
    @Autowired
    private CitationValidator citationValidator;
    
    @Autowired
    private HallucinationDetector hallucinationDetector;
    
    @Autowired
    private ValidationReportGenerator reportGenerator;
    
    /**
     * 完整验证接口
     * 
     * @param request 验证请求
     * @return 验证结果
     */
    @PostMapping("/validate")
    public ResponseEntity<ValidationResponse> validate(@RequestBody ValidationRequest request) {
        log.info("收到验证请求: query={}", request.getQuery());
        
        try {
            // 转换检索结果
            List<RetrievalResult> retrievalResults = convertToRetrievalResults(
                    request.getRetrievalResults());
            
            // 执行验证
            AnswerValidationService.ValidationResult result = validationService.validate(
                    request.getAnswer(),
                    request.getQuery(),
                    retrievalResults
            );
            
            // 构建响应
            ValidationResponse response = buildValidationResponse(result);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("验证失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 快速评分接口
     * 
     * @param request 评分请求
     * @return 评分结果
     */
    @PostMapping("/score")
    public ResponseEntity<ScoreResponse> score(@RequestBody ValidationRequest request) {
        log.info("收到评分请求");
        
        try {
            List<RetrievalResult> retrievalResults = convertToRetrievalResults(
                    request.getRetrievalResults());
            
            AnswerScore score = validationService.quickScore(
                    request.getAnswer(),
                    request.getQuery(),
                    retrievalResults
            );
            
            ScoreResponse response = ScoreResponse.builder()
                    .score(score)
                    .build();
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("评分失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 一致性检查接口
     * 
     * @param request 检查请求
     * @return 检查结果
     */
    @PostMapping("/consistency")
    public ResponseEntity<ConsistencyResponse> checkConsistency(@RequestBody ValidationRequest request) {
        log.info("收到一致性检查请求");
        
        try {
            List<RetrievalResult> retrievalResults = convertToRetrievalResults(
                    request.getRetrievalResults());
            
            FactConsistencyChecker.ConsistencyCheckResult result = 
                    validationService.validateConsistency(request.getAnswer(), retrievalResults);
            
            ConsistencyResponse response = ConsistencyResponse.builder()
                    .consistencyScore(result.getConsistencyScore())
                    .totalClaims(result.getClaims().size())
                    .supportedClaims(result.getSupportedClaimsCount())
                    .unsupportedClaims(result.getUnsupportedClaimsCount())
                    .hasContradictions(result.hasContradictions())
                    .contradictionCount(result.getContradictions().size())
                    .build();
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("一致性检查失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 引用验证接口
     * 
     * @param request 验证请求
     * @return 验证结果
     */
    @PostMapping("/citations")
    public ResponseEntity<CitationResponse> validateCitations(@RequestBody ValidationRequest request) {
        log.info("收到引用验证请求");
        
        try {
            List<RetrievalResult> retrievalResults = convertToRetrievalResults(
                    request.getRetrievalResults());
            
            CitationValidator.CitationValidationResult result = 
                    validationService.validateCitations(request.getAnswer(), retrievalResults);
            
            CitationResponse response = CitationResponse.builder()
                    .citationScore(result.getCitationScore())
                    .totalCitations(result.getCitations().size())
                    .validCitations(result.getValidCitationsCount())
                    .invalidCitations(result.getInvalidCitationsCount())
                    .unreferencedClaimsCount(result.getUnreferencedClaims().size())
                    .build();
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("引用验证失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 幻觉检测接口
     * 
     * @param request 检测请求
     * @return 检测结果
     */
    @PostMapping("/hallucinations")
    public ResponseEntity<HallucinationResponse> detectHallucinations(@RequestBody ValidationRequest request) {
        log.info("收到幻觉检测请求");
        
        try {
            List<RetrievalResult> retrievalResults = convertToRetrievalResults(
                    request.getRetrievalResults());
            
            HallucinationDetector.HallucinationDetectionResult result = 
                    validationService.detectHallucinations(request.getAnswer(), retrievalResults);
            
            HallucinationResponse response = HallucinationResponse.builder()
                    .hallucinationScore(result.getHallucinationScore())
                    .totalIndicators(result.getIndicators().size())
                    .highSeverityCount(result.getHighSeverityCount())
                    .hasHallucinations(result.hasHallucinations())
                    .indicatorsByType(result.getIndicatorsByType())
                    .build();
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("幻觉检测失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 生成验证报告接口
     * 
     * @param request 报告请求
     * @return 报告内容
     */
    @PostMapping("/report")
    public ResponseEntity<ReportResponse> generateReport(@RequestBody ValidationRequest request) {
        log.info("收到报告生成请求");
        
        try {
            List<RetrievalResult> retrievalResults = convertToRetrievalResults(
                    request.getRetrievalResults());
            
            // 完整验证
            AnswerValidationService.ValidationResult validationResult = 
                    validationService.validate(request.getAnswer(), request.getQuery(), retrievalResults);
            
            // 生成Markdown报告
            String markdownReport = validationResult.getMarkdownReport();
            
            ReportResponse response = ReportResponse.builder()
                    .reportId(validationResult.getReport().getReportId())
                    .markdownReport(markdownReport)
                    .validationResult(validationResult)
                    .build();
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("报告生成失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * 答案修正接口
     * 
     * @param request 修正请求
     * @return 修正后的答案
     */
    @PostMapping("/correct")
    public ResponseEntity<CorrectionResponse> correctAnswer(@RequestBody ValidationRequest request) {
        log.info("收到答案修正请求");
        
        try {
            List<RetrievalResult> retrievalResults = convertToRetrievalResults(
                    request.getRetrievalResults());
            
            // 先评分
            AnswerScore score = validationService.quickScore(
                    request.getAnswer(),
                    request.getQuery(),
                    retrievalResults
            );
            
            // 修正答案
            AnswerCorrector.CorrectedAnswer corrected = validationService.correctAnswer(
                    request.getAnswer(),
                    score,
                    retrievalResults
            );
            
            CorrectionResponse response = CorrectionResponse.builder()
                    .originalAnswer(request.getAnswer())
                    .correctedAnswer(corrected.getCorrectedText())
                    .wasCorrected(corrected.isWasCorrected())
                    .correctionsCount(corrected.getCorrectionsCount())
                    .confidence(score.getConfidence())
                    .build();
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("答案修正失败", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    // ============= 辅助方法 =============
    
    /**
     * 转换检索结果
     */
    private List<RetrievalResult> convertToRetrievalResults(List<RetrievalResultDto> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            return Collections.emptyList();
        }
        
        return dtos.stream()
                .map(dto -> RetrievalResult.builder()
                        .documentId(dto.getDocumentId())
                        .content(dto.getContent())
                        .metadata(dto.getMetadata())
                        .rawScore(dto.getRawScore())
                        .rerankScore(dto.getRerankScore())
                        .passedThreshold(dto.isPassedThreshold())
                        .retrieverType(dto.getRetrieverType())
                        .chunkIndex(dto.getChunkIndex())
                        .totalChunks(dto.getTotalChunks())
                        .build())
                .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * 构建验证响应
     */
    private ValidationResponse buildValidationResponse(
            AnswerValidationService.ValidationResult result) {
        
        return ValidationResponse.builder()
                .originalAnswer(result.getOriginalAnswer())
                .finalAnswer(result.getFinalAnswer())
                .wasCorrected(result.isWasCorrected())
                .passed(result.isPassed())
                .score(result.getScore())
                .consistencyScore(result.getConsistencyResult().getConsistencyScore())
                .citationScore(result.getCitationResult().getCitationScore())
                .hallucinationScore(result.getHallucinationResult().getHallucinationScore())
                .totalValidationTimeMs(result.getTotalValidationTimeMs())
                .report(result.getReport())
                .build();
    }
    
    // ============= DTO类 =============
    
    /**
     * 验证请求
     */
    @lombok.Data
    public static class ValidationRequest {
        private String answer;
        private String query;
        private List<RetrievalResultDto> retrievalResults;
    }
    
    /**
     * 检索结果DTO
     */
    @lombok.Data
    public static class RetrievalResultDto {
        private String documentId;
        private String content;
        private Map<String, Object> metadata;
        private double rawScore;
        private double rerankScore;
        private boolean passedThreshold;
        private String retrieverType;
        private int chunkIndex;
        private int totalChunks;
    }
    
    /**
     * 验证响应
     */
    @lombok.Data
    @lombok.Builder
    public static class ValidationResponse {
        private String originalAnswer;
        private String finalAnswer;
        private boolean wasCorrected;
        private boolean passed;
        private AnswerScore score;
        private double consistencyScore;
        private double citationScore;
        private double hallucinationScore;
        private long totalValidationTimeMs;
        private ValidationReportGenerator.ValidationReport report;
    }
    
    /**
     * 评分响应
     */
    @lombok.Data
    @lombok.Builder
    public static class ScoreResponse {
        private AnswerScore score;
    }
    
    /**
     * 一致性响应
     */
    @lombok.Data
    @lombok.Builder
    public static class ConsistencyResponse {
        private double consistencyScore;
        private int totalClaims;
        private int supportedClaims;
        private int unsupportedClaims;
        private boolean hasContradictions;
        private int contradictionCount;
    }
    
    /**
     * 引用响应
     */
    @lombok.Data
    @lombok.Builder
    public static class CitationResponse {
        private double citationScore;
        private int totalCitations;
        private int validCitations;
        private int invalidCitations;
        private int unreferencedClaimsCount;
    }
    
    /**
     * 幻觉响应
     */
    @lombok.Data
    @lombok.Builder
    public static class HallucinationResponse {
        private double hallucinationScore;
        private int totalIndicators;
        private int highSeverityCount;
        private boolean hasHallucinations;
        private Map<HallucinationDetector.HallucinationType, Long> indicatorsByType;
    }
    
    /**
     * 报告响应
     */
    @lombok.Data
    @lombok.Builder
    public static class ReportResponse {
        private String reportId;
        private String markdownReport;
        private AnswerValidationService.ValidationResult validationResult;
    }
    
    /**
     * 修正响应
     */
    @lombok.Data
    @lombok.Builder
    public static class CorrectionResponse {
        private String originalAnswer;
        private String correctedAnswer;
        private boolean wasCorrected;
        private int correctionsCount;
        private double confidence;
    }
}
