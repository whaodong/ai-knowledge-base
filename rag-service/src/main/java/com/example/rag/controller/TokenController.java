package com.example.rag.controller;

import com.example.common.dto.Result;
import com.example.common.token.*;
import com.example.rag.token.RagTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Token管理控制器
 * 
 * <p>提供Token使用统计、分析、预测等API接口。</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/tokens")
@Tag(name = "Token管理", description = "Token使用统计与分析接口")
public class TokenController {
    
    @Autowired
    private RagTokenService ragTokenService;
    
    @Autowired
    private TokenCounter tokenCounter;
    
    /**
     * 计算文本Token数
     */
    @PostMapping("/count")
    @Operation(summary = "计算Token数", description = "计算给定文本的Token数量")
    public ResponseEntity<Result<Map<String, Object>>> countTokens(
            @Parameter(description = "文本内容") @RequestBody String text,
            @Parameter(description = "模型名称") @RequestParam(defaultValue = "gpt-3.5-turbo") String model) {
        
        int tokens = tokenCounter.countTokens(text, model);
        int maxTokens = tokenCounter.getMaxContextTokens(model);
        
        Map<String, Object> result = new HashMap<>();
        result.put("text", text.length() > 50 ? text.substring(0, 50) + "..." : text);
        result.put("textLength", text.length());
        result.put("tokenCount", tokens);
        result.put("modelName", model);
        result.put("maxContextTokens", maxTokens);
        result.put("usagePercentage", String.format("%.2f%%", (double) tokens / maxTokens * 100));
        
        return ResponseEntity.ok(Result.success(result));
    }
    
    /**
     * 获取会话Token统计
     */
    @GetMapping("/session/{sessionId}")
    @Operation(summary = "会话Token统计", description = "获取指定会话的Token使用统计")
    public ResponseEntity<Result<Map<String, Object>>> getSessionStats(
            @Parameter(description = "会话ID") @PathVariable String sessionId) {
        
        RagTokenService.TokenUsageSummary summary = ragTokenService.completeSession(sessionId, null);
        
        if (summary == null) {
            return ResponseEntity.ok(Result.fail(404, "会话不存在或已结束"));
        }
        
        return ResponseEntity.ok(Result.success(summary.toMap()));
    }
    
    /**
     * 获取实时Token使用信息
     */
    @GetMapping("/session/{sessionId}/realtime")
    @Operation(summary = "实时Token信息", description = "获取会话的实时Token使用情况")
    public ResponseEntity<Result<StreamingTokenCounter.TokenUsageInfo>> getRealTimeTokenInfo(
            @Parameter(description = "会话ID") @PathVariable String sessionId) {
        
        StreamingTokenCounter.TokenUsageInfo info = ragTokenService.getRealTimeTokenInfo(sessionId);
        
        if (info == null) {
            return ResponseEntity.ok(Result.fail(404, "会话不存在或未激活"));
        }
        
        return ResponseEntity.ok(Result.success(info));
    }
    
    /**
     * 预测Token使用
     */
    @PostMapping("/predict")
    @Operation(summary = "预测Token使用", description = "预测问题的Token使用情况")
    public ResponseEntity<Result<TokenPredictor.ContextWindowPrediction>> predictTokens(
            @Parameter(description = "问题内容") @RequestParam String question,
            @Parameter(description = "上下文内容") @RequestParam(required = false) String context,
            @Parameter(description = "模型名称") @RequestParam(defaultValue = "gpt-3.5-turbo") String model) {
        
        TokenPredictor.ContextWindowPrediction prediction = 
                ragTokenService.predictContextWindow("predict-session", question, context);
        
        return ResponseEntity.ok(Result.success(prediction));
    }
    
    /**
     * 获取用户Token统计
     */
    @GetMapping("/user/{userId}/stats")
    @Operation(summary = "用户Token统计", description = "获取用户的Token使用统计")
    public ResponseEntity<Result<TokenUsageAnalyzer.UserTokenStatistics>> getUserTokenStats(
            @Parameter(description = "用户ID") @PathVariable String userId,
            @Parameter(description = "开始时间") @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @Parameter(description = "结束时间") @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        
        if (startTime == null) {
            startTime = LocalDateTime.now().minusDays(7);
        }
        if (endTime == null) {
            endTime = LocalDateTime.now();
        }
        
        TokenUsageAnalyzer.UserTokenStatistics stats = 
                ragTokenService.getUserTokenStats(userId, startTime, endTime);
        
        return ResponseEntity.ok(Result.success(stats));
    }
    
    /**
     * 获取使用趋势分析
     */
    @GetMapping("/trend")
    @Operation(summary = "使用趋势分析", description = "分析Token使用趋势")
    public ResponseEntity<Result<TokenUsageAnalyzer.UsageTrendAnalysis>> getUsageTrend(
            @Parameter(description = "开始时间") @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @Parameter(description = "结束时间") @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        
        if (startTime == null) {
            startTime = LocalDateTime.now().minusDays(7);
        }
        if (endTime == null) {
            endTime = LocalDateTime.now();
        }
        
        TokenUsageAnalyzer.UsageTrendAnalysis trend = 
                ragTokenService.getUsageTrend(startTime, endTime);
        
        return ResponseEntity.ok(Result.success(trend));
    }
    
    /**
     * 获取Token使用报告
     */
    @GetMapping("/report")
    @Operation(summary = "Token使用报告", description = "生成Token使用报告")
    public ResponseEntity<Result<TokenUsageAnalyzer.UsageReport>> getUsageReport(
            @Parameter(description = "用户ID（可选）") @RequestParam(required = false) String userId,
            @Parameter(description = "开始时间") @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @Parameter(description = "结束时间") @RequestParam(required = false) 
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        
        if (startTime == null) {
            startTime = LocalDateTime.now().minusDays(7);
        }
        if (endTime == null) {
            endTime = LocalDateTime.now();
        }
        
        TokenUsageAnalyzer.UsageReport report = 
                ragTokenService.getUsageReport(userId, startTime, endTime);
        
        return ResponseEntity.ok(Result.success(report));
    }
    
    /**
     * 检测异常Token使用
     */
    @GetMapping("/anomalies/{userId}")
    @Operation(summary = "异常Token检测", description = "检测用户的异常Token使用情况")
    public ResponseEntity<Result<List<TokenUsageAnalyzer.AnomalyRecord>>> detectAnomalies(
            @Parameter(description = "用户ID") @PathVariable String userId) {
        
        List<TokenUsageAnalyzer.AnomalyRecord> anomalies = ragTokenService.detectAnomalies(userId);
        
        return ResponseEntity.ok(Result.success(anomalies));
    }
    
    /**
     * 获取Token优化建议
     */
    @PostMapping("/optimize/suggestions")
    @Operation(summary = "Token优化建议", description = "获取Token使用优化建议")
    public ResponseEntity<Result<List<TokenPredictor.TokenOptimizationSuggestion>>> getOptimizationSuggestions(
            @Parameter(description = "问题内容") @RequestParam String question,
            @Parameter(description = "上下文内容") @RequestParam(required = false) String context,
            @Parameter(description = "模型名称") @RequestParam(defaultValue = "gpt-3.5-turbo") String model) {
        
        List<TokenPredictor.TokenOptimizationSuggestion> suggestions = 
                ragTokenService.getTokenOptimizationSuggestions(question, context, model);
        
        return ResponseEntity.ok(Result.success(suggestions));
    }
    
    /**
     * 设置用户Token配额
     */
    @PostMapping("/quota/{userId}")
    @Operation(summary = "设置Token配额", description = "设置用户的Token配额")
    public ResponseEntity<Result<String>> setUserQuota(
            @Parameter(description = "用户ID") @PathVariable String userId,
            @Parameter(description = "每日限制") @RequestParam int dailyLimit) {
        
        ragTokenService.setUserTokenQuota(userId, dailyLimit);
        
        return ResponseEntity.ok(Result.success("Token配额设置成功"));
    }
    
    /**
     * 智能截断文本
     */
    @PostMapping("/truncate")
    @Operation(summary = "智能截断文本", description = "将文本截断到指定Token限制内")
    public ResponseEntity<Result<Map<String, Object>>> truncateText(
            @Parameter(description = "文本内容") @RequestBody String text,
            @Parameter(description = "最大Token数") @RequestParam int maxTokens,
            @Parameter(description = "模型名称") @RequestParam(defaultValue = "gpt-3.5-turbo") String model,
            @Parameter(description = "截断策略") @RequestParam(defaultValue = "SENTENCE") String strategy) {
        
        TokenCounter.TruncationStrategy truncationStrategy = 
                TokenCounter.TruncationStrategy.valueOf(strategy.toUpperCase());
        
        String truncatedText = tokenCounter.truncateText(text, maxTokens, model, truncationStrategy);
        int originalTokens = tokenCounter.countTokens(text, model);
        int truncatedTokens = tokenCounter.countTokens(truncatedText, model);
        
        Map<String, Object> result = new HashMap<>();
        result.put("originalLength", text.length());
        result.put("truncatedLength", truncatedText.length());
        result.put("originalTokens", originalTokens);
        result.put("truncatedTokens", truncatedTokens);
        result.put("savedTokens", originalTokens - truncatedTokens);
        result.put("truncatedText", truncatedText);
        
        return ResponseEntity.ok(Result.success(result));
    }
    
    /**
     * 获取模型信息
     */
    @GetMapping("/models")
    @Operation(summary = "模型信息", description = "获取支持的模型及其上下文窗口信息")
    public ResponseEntity<Result<Map<String, Object>>> getModelsInfo() {
        Map<String, Object> models = new HashMap<>();
        
        models.put("gpt-3.5-turbo", Map.of(
                "maxContextTokens", 4096,
                "inputPrice", "$0.0015/1K tokens",
                "outputPrice", "$0.002/1K tokens"
        ));
        
        models.put("gpt-3.5-turbo-16k", Map.of(
                "maxContextTokens", 16384,
                "inputPrice", "$0.003/1K tokens",
                "outputPrice", "$0.004/1K tokens"
        ));
        
        models.put("gpt-4", Map.of(
                "maxContextTokens", 8192,
                "inputPrice", "$0.03/1K tokens",
                "outputPrice", "$0.06/1K tokens"
        ));
        
        models.put("gpt-4-turbo", Map.of(
                "maxContextTokens", 128000,
                "inputPrice", "$0.01/1K tokens",
                "outputPrice", "$0.03/1K tokens"
        ));
        
        return ResponseEntity.ok(Result.success(models));
    }
    
    /**
     * 动态调整检索数量
     */
    @PostMapping("/adjust-topk")
    @Operation(summary = "调整检索数量", description = "根据Token限制动态调整检索文档数量")
    public ResponseEntity<Result<Map<String, Object>>> adjustRetrievalTopK(
            @Parameter(description = "问题内容") @RequestParam String question,
            @Parameter(description = "请求的TopK") @RequestParam int requestedTopK,
            @Parameter(description = "模型名称") @RequestParam(defaultValue = "gpt-3.5-turbo") String model) {
        
        int maxContextTokens = tokenCounter.getMaxContextTokens(model);
        int adjustedTopK = ragTokenService.adjustRetrievalTopK(
                "adjust-session", question, requestedTopK);
        
        Map<String, Object> result = new HashMap<>();
        result.put("question", question);
        result.put("requestedTopK", requestedTopK);
        result.put("adjustedTopK", adjustedTopK);
        result.put("modelName", model);
        result.put("maxContextTokens", maxContextTokens);
        
        return ResponseEntity.ok(Result.success(result));
    }
}
