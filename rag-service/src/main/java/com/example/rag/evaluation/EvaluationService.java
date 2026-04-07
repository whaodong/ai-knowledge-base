package com.example.rag.evaluation;

import com.example.rag.model.RagResponse;
import com.example.rag.model.RetrievalResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * RAG检索质量评估服务
 * 
 * <p>计算检索结果的评估指标，包括MRR、NDCG@k、Precision@k、Recall@k等。</p>
 */
@Slf4j
@Service
public class EvaluationService {
    
    /**
     * 评估单个检索结果
     * 
     * @param response RAG响应
     * @param relevantDocumentIds 相关文档ID列表（ground truth）
     * @return 评估指标
     */
    public RagResponse.EvaluationMetrics evaluate(
            RagResponse response, 
            List<String> relevantDocumentIds) {
        
        if (response == null || !response.isSuccess() || 
                response.getRetrievedDocuments() == null) {
            return RagResponse.EvaluationMetrics.builder()
                    .mrr(0.0)
                    .ndcgAt3(0.0)
                    .ndcgAt5(0.0)
                    .precisionAt3(0.0)
                    .precisionAt5(0.0)
                    .recallAt5(0.0)
                    .averageRelevanceScore(0.0)
                    .build();
        }
        
        List<RetrievalResult> retrievedDocs = response.getRetrievedDocuments();
        
        // 构建相关性判断：如果文档ID在相关列表中，则为相关（相关性分数1.0）
        List<Double> relevanceGrades = retrievedDocs.stream()
                .map(doc -> relevantDocumentIds.contains(doc.getDocumentId()) ? 1.0 : 0.0)
                .collect(Collectors.toList());
        
        // 计算原始分数作为相关性估计（用于averageRelevanceScore）
        List<Double> rawScores = retrievedDocs.stream()
                .map(RetrievalResult::getRawScore)
                .collect(Collectors.toList());
        
        // 计算各项指标
        double mrr = calculateMRR(relevanceGrades);
        double ndcgAt3 = calculateNDCG(relevanceGrades, 3);
        double ndcgAt5 = calculateNDCG(relevanceGrades, 5);
        double precisionAt3 = calculatePrecision(relevanceGrades, 3);
        double precisionAt5 = calculatePrecision(relevanceGrades, 5);
        double recallAt5 = calculateRecall(relevanceGrades, relevantDocumentIds.size(), 5);
        double averageRelevanceScore = calculateAverageRelevanceScore(rawScores);
        
        RagResponse.EvaluationMetrics metrics = RagResponse.EvaluationMetrics.builder()
                .mrr(mrr)
                .ndcgAt3(ndcgAt3)
                .ndcgAt5(ndcgAt5)
                .precisionAt3(precisionAt3)
                .precisionAt5(precisionAt5)
                .recallAt5(recallAt5)
                .averageRelevanceScore(averageRelevanceScore)
                .build();
        
        log.debug("检索评估完成，MRR: {:.3f}, NDCG@5: {:.3f}, Precision@5: {:.3f}", 
                mrr, ndcgAt5, precisionAt5);
        
        return metrics;
    }
    
    /**
     * 批量评估检索结果
     */
    public Map<String, RagResponse.EvaluationMetrics> batchEvaluate(
            Map<String, RagResponse> responses,
            Map<String, List<String>> groundTruth) {
        
        Map<String, RagResponse.EvaluationMetrics> evaluationResults = new HashMap<>();
        
        for (Map.Entry<String, RagResponse> entry : responses.entrySet()) {
            String queryId = entry.getKey();
            RagResponse response = entry.getValue();
            List<String> relevantDocs = groundTruth.getOrDefault(queryId, Collections.emptyList());
            
            RagResponse.EvaluationMetrics metrics = evaluate(response, relevantDocs);
            evaluationResults.put(queryId, metrics);
        }
        
        return evaluationResults;
    }
    
    /**
     * 计算平均评估指标
     */
    public RagResponse.EvaluationMetrics calculateAverageMetrics(
            Collection<RagResponse.EvaluationMetrics> metricsList) {
        
        if (metricsList == null || metricsList.isEmpty()) {
            return RagResponse.EvaluationMetrics.builder()
                    .mrr(0.0)
                    .ndcgAt3(0.0)
                    .ndcgAt5(0.0)
                    .precisionAt3(0.0)
                    .precisionAt5(0.0)
                    .recallAt5(0.0)
                    .averageRelevanceScore(0.0)
                    .build();
        }
        
        double mrrSum = 0.0;
        double ndcgAt3Sum = 0.0;
        double ndcgAt5Sum = 0.0;
        double precisionAt3Sum = 0.0;
        double precisionAt5Sum = 0.0;
        double recallAt5Sum = 0.0;
        double relevanceScoreSum = 0.0;
        
        int count = 0;
        for (RagResponse.EvaluationMetrics metrics : metricsList) {
            mrrSum += metrics.getMrr();
            ndcgAt3Sum += metrics.getNdcgAt3();
            ndcgAt5Sum += metrics.getNdcgAt5();
            precisionAt3Sum += metrics.getPrecisionAt3();
            precisionAt5Sum += metrics.getPrecisionAt5();
            recallAt5Sum += metrics.getRecallAt5();
            relevanceScoreSum += metrics.getAverageRelevanceScore();
            count++;
        }
        
        return RagResponse.EvaluationMetrics.builder()
                .mrr(mrrSum / count)
                .ndcgAt3(ndcgAt3Sum / count)
                .ndcgAt5(ndcgAt5Sum / count)
                .precisionAt3(precisionAt3Sum / count)
                .precisionAt5(precisionAt5Sum / count)
                .recallAt5(recallAt5Sum / count)
                .averageRelevanceScore(relevanceScoreSum / count)
                .build();
    }
    
    /**
     * 计算MRR（平均倒数排名）
     * 
     * @param relevanceGrades 相关性分数列表（1.0表示相关，0.0表示不相关）
     * @return MRR值
     */
    private double calculateMRR(List<Double> relevanceGrades) {
        if (relevanceGrades == null || relevanceGrades.isEmpty()) {
            return 0.0;
        }
        
        // 找到第一个相关文档的位置（从1开始计数）
        for (int i = 0; i < relevanceGrades.size(); i++) {
            if (relevanceGrades.get(i) > 0.5) { // 相关
                return 1.0 / (i + 1); // 倒数排名
            }
        }
        
        return 0.0; // 没有相关文档
    }
    
    /**
     * 计算NDCG@k（归一化折损累积增益）
     * 
     * @param relevanceGrades 相关性分数列表
     * @param k 计算前k个结果
     * @return NDCG@k值
     */
    private double calculateNDCG(List<Double> relevanceGrades, int k) {
        if (relevanceGrades == null || relevanceGrades.isEmpty() || k <= 0) {
            return 0.0;
        }
        
        int actualK = Math.min(k, relevanceGrades.size());
        
        // 计算DCG（折损累积增益）
        double dcg = 0.0;
        for (int i = 0; i < actualK; i++) {
            double relevance = relevanceGrades.get(i);
            // 折损因子：1 / log2(i + 2)
            dcg += relevance / Math.log(i + 2) / Math.log(2);
        }
        
        // 计算IDCG（理想DCG）
        List<Double> idealRelevance = new ArrayList<>(relevanceGrades);
        idealRelevance.sort(Comparator.reverseOrder());
        
        double idcg = 0.0;
        for (int i = 0; i < actualK; i++) {
            double relevance = idealRelevance.get(i);
            idcg += relevance / Math.log(i + 2) / Math.log(2);
        }
        
        // NDCG = DCG / IDCG
        if (idcg == 0.0) {
            return 0.0;
        }
        
        return dcg / idcg;
    }
    
    /**
     * 计算Precision@k（精确率）
     */
    private double calculatePrecision(List<Double> relevanceGrades, int k) {
        if (relevanceGrades == null || relevanceGrades.isEmpty() || k <= 0) {
            return 0.0;
        }
        
        int actualK = Math.min(k, relevanceGrades.size());
        
        long relevantCount = 0;
        for (int i = 0; i < actualK; i++) {
            if (relevanceGrades.get(i) > 0.5) {
                relevantCount++;
            }
        }
        
        return (double) relevantCount / actualK;
    }
    
    /**
     * 计算Recall@k（召回率）
     */
    private double calculateRecall(List<Double> relevanceGrades, int totalRelevantDocs, int k) {
        if (relevanceGrades == null || relevanceGrades.isEmpty() || k <= 0 || totalRelevantDocs <= 0) {
            return 0.0;
        }
        
        int actualK = Math.min(k, relevanceGrades.size());
        
        long retrievedRelevantCount = 0;
        for (int i = 0; i < actualK; i++) {
            if (relevanceGrades.get(i) > 0.5) {
                retrievedRelevantCount++;
            }
        }
        
        return (double) retrievedRelevantCount / totalRelevantDocs;
    }
    
    /**
     * 计算平均相关性分数
     */
    private double calculateAverageRelevanceScore(List<Double> rawScores) {
        if (rawScores == null || rawScores.isEmpty()) {
            return 0.0;
        }
        
        double sum = 0.0;
        for (double score : rawScores) {
            sum += score;
        }
        
        return sum / rawScores.size();
    }
    
    /**
     * 生成评估报告
     */
    public String generateEvaluationReport(
            Map<String, RagResponse.EvaluationMetrics> evaluationResults) {
        
        if (evaluationResults == null || evaluationResults.isEmpty()) {
            return "暂无评估数据";
        }
        
        StringBuilder report = new StringBuilder();
        report.append("# RAG检索质量评估报告\n\n");
        report.append("## 评估概览\n");
        report.append(String.format("- 评估查询数量: %d\n", evaluationResults.size()));
        report.append(String.format("- 评估时间: %s\n\n", new Date()));
        
        // 计算平均指标
        RagResponse.EvaluationMetrics averageMetrics = 
                calculateAverageMetrics(evaluationResults.values());
        
        report.append("## 平均评估指标\n");
        report.append(String.format("- **MRR**: %.3f\n", averageMetrics.getMrr()));
        report.append(String.format("- **NDCG@3**: %.3f\n", averageMetrics.getNdcgAt3()));
        report.append(String.format("- **NDCG@5**: %.3f\n", averageMetrics.getNdcgAt5()));
        report.append(String.format("- **Precision@3**: %.3f\n", averageMetrics.getPrecisionAt3()));
        report.append(String.format("- **Precision@5**: %.3f\n", averageMetrics.getPrecisionAt5()));
        report.append(String.format("- **Recall@5**: %.3f\n", averageMetrics.getRecallAt5()));
        report.append(String.format("- **平均相关性分数**: %.3f\n\n", averageMetrics.getAverageRelevanceScore()));
        
        // 详细结果
        report.append("## 详细评估结果\n");
        report.append("| 查询ID | MRR | NDCG@3 | NDCG@5 | Precision@5 | Recall@5 |\n");
        report.append("|--------|-----|--------|--------|-------------|----------|\n");
        
        for (Map.Entry<String, RagResponse.EvaluationMetrics> entry : evaluationResults.entrySet()) {
            RagResponse.EvaluationMetrics metrics = entry.getValue();
            report.append(String.format("| %s | %.3f | %.3f | %.3f | %.3f | %.3f |\n",
                    entry.getKey(),
                    metrics.getMrr(),
                    metrics.getNdcgAt3(),
                    metrics.getNdcgAt5(),
                    metrics.getPrecisionAt5(),
                    metrics.getRecallAt5()));
        }
        
        // 评估结论
        report.append("\n## 评估结论\n");
        
        if (averageMetrics.getMrr() >= 0.7) {
            report.append("✅ **MRR指标达标**：平均倒数排名为" + 
                    String.format("%.3f", averageMetrics.getMrr()) + 
                    "，达到行业平均水平（≥0.7）。\n");
        } else {
            report.append("⚠️ **MRR指标待提升**：平均倒数排名为" + 
                    String.format("%.3f", averageMetrics.getMrr()) + 
                    "，未达到行业平均水平（≥0.7）。\n");
        }
        
        if (averageMetrics.getNdcgAt5() >= 0.6) {
            report.append("✅ **NDCG@5指标良好**：归一化折损累积增益为" + 
                    String.format("%.3f", averageMetrics.getNdcgAt5()) + 
                    "，表明检索结果排序质量较高。\n");
        } else {
            report.append("⚠️ **NDCG@5指标待优化**：归一化折损累积增益为" + 
                    String.format("%.3f", averageMetrics.getNdcgAt5()) + 
                    "，建议优化排序算法。\n");
        }
        
        report.append("\n## 改进建议\n");
        report.append("1. 如果MRR指标偏低，建议优化检索器的召回能力，确保相关文档能被检索到\n");
        report.append("2. 如果NDCG指标偏低，建议优化重排序算法，提升结果排序质量\n");
        report.append("3. 考虑调整混合检索的权重配置，平衡向量检索和关键词检索的效果\n");
        report.append("4. 增加训练数据，优化Embedding模型对领域知识的理解\n");
        
        return report.toString();
    }
}