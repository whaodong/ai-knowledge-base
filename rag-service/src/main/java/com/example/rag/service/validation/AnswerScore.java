package com.example.rag.service.validation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 答案评分模型
 * 
 * <p>多维度评分体系，用于全面评估答案质量。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnswerScore {
    
    /**
     * 相关性评分（0-1）
     * 答案与问题的相关程度
     */
    private double relevance;
    
    /**
     * 一致性评分（0-1）
     * 答案与检索文档的事实一致性
     */
    private double consistency;
    
    /**
     * 引用评分（0-1）
     * 引用的准确性和可信度
     */
    private double citationScore;
    
    /**
     * 完整性评分（0-1）
     * 答案的完整程度
     */
    private double completeness;
    
    /**
     * 综合置信度（0-1）
     * 加权平均后的总体评分
     */
    private double confidence;
    
    /**
     * 幻觉检测评分（0-1）
     * 检测到幻觉的程度（分数越低表示幻觉越严重）
     */
    private double hallucinationScore;
    
    /**
     * 各维度评分详情
     */
    private Map<String, Double> scoreDetails;
    
    /**
     * 评分等级
     */
    private ScoreLevel level;
    
    /**
     * 评分等级枚举
     */
    public enum ScoreLevel {
        EXCELLENT("优秀", 0.9),
        GOOD("良好", 0.8),
        ACCEPTABLE("合格", 0.7),
        POOR("较差", 0.6),
        UNACCEPTABLE("不合格", 0.0);
        
        private final String label;
        private final double threshold;
        
        ScoreLevel(String label, double threshold) {
            this.label = label;
            this.threshold = threshold;
        }
        
        public String getLabel() {
            return label;
        }
        
        public double getThreshold() {
            return threshold;
        }
        
        public static ScoreLevel fromScore(double score) {
            for (ScoreLevel level : values()) {
                if (score >= level.threshold) {
                    return level;
                }
            }
            return UNACCEPTABLE;
        }
    }
    
    /**
     * 计算综合置信度
     * 
     * @return 综合置信度
     */
    public double calculateConfidence() {
        // 默认权重配置
        double relevanceWeight = 0.25;
        double consistencyWeight = 0.30;
        double citationWeight = 0.15;
        double completenessWeight = 0.15;
        double hallucinationWeight = 0.15;
        
        this.confidence = relevance * relevanceWeight +
                consistency * consistencyWeight +
                citationScore * citationWeight +
                completeness * completenessWeight +
                hallucinationScore * hallucinationWeight;
        
        this.level = ScoreLevel.fromScore(this.confidence);
        
        return this.confidence;
    }
    
    /**
     * 判断是否通过最低置信度阈值
     * 
     * @param threshold 阈值
     * @return 是否通过
     */
    public boolean isPassed(double threshold) {
        return confidence >= threshold;
    }
    
    /**
     * 判断是否为高质量答案
     * 
     * @return 是否高质量
     */
    public boolean isHighQuality() {
        return level == ScoreLevel.EXCELLENT || level == ScoreLevel.GOOD;
    }
    
    /**
     * 获取需要改进的维度
     * 
     * @return 需要改进的维度列表
     */
    public Map<String, Double> getWeakDimensions() {
        Map<String, Double> weakDimensions = new java.util.HashMap<>();
        double threshold = 0.7;
        
        if (relevance < threshold) {
            weakDimensions.put("相关性", relevance);
        }
        if (consistency < threshold) {
            weakDimensions.put("一致性", consistency);
        }
        if (citationScore < threshold) {
            weakDimensions.put("引用质量", citationScore);
        }
        if (completeness < threshold) {
            weakDimensions.put("完整性", completeness);
        }
        if (hallucinationScore < threshold) {
            weakDimensions.put("可信度", hallucinationScore);
        }
        
        return weakDimensions;
    }
}
