package com.example.rag.service.validation;

import com.example.rag.model.RetrievalResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 幻觉检测器
 * 
 * <p>检测AI生成的答案中可能存在的幻觉内容。</p>
 * <p>功能特点：</p>
 * <ul>
 *   <li>检测无依据的陈述</li>
 *   <li>识别过度推断</li>
 *   <li>发现时间/数字不一致</li>
 *   <li>标记可疑内容</li>
 * </ul>
 */
@Slf4j
@Component
public class HallucinationDetector {
    
    // 过度肯定的词汇（可能导致幻觉）
    private static final Set<String> OVERCONFIDENT_WORDS = new HashSet<>(Arrays.asList(
            "肯定", "一定", "必然", "绝对", "毫无疑问", "百分之百",
            "always", "never", "definitely", "absolutely", "certainly", "undoubtedly"
    ));
    
    // 模糊词汇（可能表示不确定性或幻觉）
    private static final Set<String> VAGUE_WORDS = new HashSet<>(Arrays.asList(
            "可能", "也许", "大概", "或许", "似乎", "看起来", "好像",
            "maybe", "might", "possibly", "perhaps", "seems", "appears", "probably"
    ));
    
    // 推断性词汇
    private static final Set<String> INFERENCE_WORDS = new HashSet<>(Arrays.asList(
            "因此", "所以", "由此可见", "可以推断", "这意味着",
            "therefore", "thus", "hence", "consequently", "implies", "suggests"
    ));
    
    // 常见幻觉模式
    private static final List<HallucinationPattern> HALLUCINATION_PATTERNS = Arrays.asList(
            // 虚构的统计数据
            new HallucinationPattern(
                    "虚构统计",
                    Pattern.compile("(\\d+(?:\\.\\d+)?%)|(\\d+(?:万|亿))"),
                    HallucinationSeverity.HIGH
            ),
            // 虚构的具体日期
            new HallucinationPattern(
                    "虚构日期",
                    Pattern.compile("\\d{4}年\\d{1,2}月\\d{1,2}日"),
                    HallucinationSeverity.MEDIUM
            ),
            // 虚构的人名
            new HallucinationPattern(
                    "虚构人名",
                    Pattern.compile("[\\u4e00-\\u9fa5]{2,}(教授|博士|先生|女士)"),
                    HallucinationSeverity.HIGH
            ),
            // 虚构的组织
            new HallucinationPattern(
                    "虚构组织",
                    Pattern.compile("[\\u4e00-\\u9fa5]{2,}(公司|机构|研究院|中心)"),
                    HallucinationSeverity.MEDIUM
            )
    );
    
    /**
     * 检测答案中的幻觉
     * 
     * @param answer 生成的答案
     * @param retrievalResults 检索结果列表
     * @return 幻觉检测结果
     */
    public HallucinationDetectionResult detect(String answer, List<RetrievalResult> retrievalResults) {
        if (answer == null || answer.isEmpty()) {
            return HallucinationDetectionResult.empty("答案为空");
        }
        
        long startTime = System.currentTimeMillis();
        log.info("开始幻觉检测，答案长度: {}", answer.length());
        
        List<HallucinationIndicator> indicators = new ArrayList<>();
        
        // 1. 检测无依据的陈述
        indicators.addAll(detectUnsupportedStatements(answer, retrievalResults));
        
        // 2. 检测过度推断
        indicators.addAll(detectOverInference(answer, retrievalResults));
        
        // 3. 检测数字和时间不一致
        indicators.addAll(detectInconsistencies(answer, retrievalResults));
        
        // 4. 检测常见幻觉模式
        indicators.addAll(detectHallucinationPatterns(answer));
        
        // 5. 检测虚构实体
        indicators.addAll(detectFabricatedEntities(answer, retrievalResults));
        
        // 6. 计算幻觉评分
        double hallucinationScore = calculateHallucinationScore(indicators);
        
        // 7. 生成标记后的答案
        String markedAnswer = markHallucinations(answer, indicators);
        
        long duration = System.currentTimeMillis() - startTime;
        
        log.info("幻觉检测完成: 检测到 {} 个幻觉指标, 幻觉评分={}, 耗时={}ms",
                indicators.size(), hallucinationScore, duration);
        
        return new HallucinationDetectionResult(
                indicators,
                hallucinationScore,
                markedAnswer,
                duration
        );
    }
    
    /**
     * 检测无依据的陈述
     */
    private List<HallucinationIndicator> detectUnsupportedStatements(
            String answer, List<RetrievalResult> results) {
        
        List<HallucinationIndicator> indicators = new ArrayList<>();
        String[] sentences = answer.split("[。.!?！？\\n]+");
        
        int position = 0;
        for (String sentence : sentences) {
            if (sentence.trim().isEmpty()) {
                position += sentence.length() + 1;
                continue;
            }
            
            // 检查是否包含具体信息
            if (containsSpecificInfo(sentence)) {
                // 检查是否有来源支持
                boolean hasSupport = checkSourceSupport(sentence, results);
                
                if (!hasSupport) {
                    indicators.add(new HallucinationIndicator(
                            HallucinationType.UNSUPPORTED_CLAIM,
                            sentence.trim(),
                            position,
                            position + sentence.length(),
                            HallucinationSeverity.MEDIUM,
                            "未找到来源支持的陈述",
                            generateEvidence(sentence, results)
                    ));
                }
            }
            
            position += sentence.length() + 1;
        }
        
        return indicators;
    }
    
    /**
     * 检测过度推断
     */
    private List<HallucinationIndicator> detectOverInference(
            String answer, List<RetrievalResult> results) {
        
        List<HallucinationIndicator> indicators = new ArrayList<>();
        String[] sentences = answer.split("[。.!?！？\\n]+");
        
        int position = 0;
        for (String sentence : sentences) {
            if (sentence.trim().isEmpty()) {
                position += sentence.length() + 1;
                continue;
            }
            
            // 检查过度肯定
            for (String word : OVERCONFIDENT_WORDS) {
                if (sentence.toLowerCase().contains(word.toLowerCase())) {
                    indicators.add(new HallucinationIndicator(
                            HallucinationType.OVERCONFIDENCE,
                            sentence.trim(),
                            position,
                            position + sentence.length(),
                            HallucinationSeverity.MEDIUM,
                            String.format("过度肯定的表述: '%s'", word),
                            "建议使用更谨慎的表述，如'通常''往往'等"
                    ));
                    break;
                }
            }
            
            // 检查过度推断
            for (String word : INFERENCE_WORDS) {
                int wordPos = sentence.toLowerCase().indexOf(word.toLowerCase());
                if (wordPos >= 0) {
                    // 检查推断是否有足够证据
                    String beforeInference = sentence.substring(0, wordPos).trim();
                    String afterInference = sentence.substring(wordPos + word.length()).trim();
                    
                    if (!hasSufficientEvidence(beforeInference, afterInference, results)) {
                        indicators.add(new HallucinationIndicator(
                                HallucinationType.OVER_INFERENCE,
                                sentence.trim(),
                                position,
                                position + sentence.length(),
                                HallucinationSeverity.HIGH,
                                String.format("过度推断: '%s'", word),
                                "推断依据不充分"
                        ));
                    }
                    break;
                }
            }
            
            position += sentence.length() + 1;
        }
        
        return indicators;
    }
    
    /**
     * 检测数字和时间不一致
     */
    private List<HallucinationIndicator> detectInconsistencies(
            String answer, List<RetrievalResult> results) {
        
        List<HallucinationIndicator> indicators = new ArrayList<>();
        
        // 提取答案中的数字
        Map<String, List<Integer>> answerNumbers = extractNumbersWithPositions(answer);
        
        // 提取检索结果中的数字
        Set<String> sourceNumbers = new HashSet<>();
        for (RetrievalResult result : results) {
            sourceNumbers.addAll(extractNumbers(result.getContent()));
        }
        
        // 检查数字一致性
        for (Map.Entry<String, List<Integer>> entry : answerNumbers.entrySet()) {
            String number = entry.getKey();
            
            if (!sourceNumbers.contains(number) && isSignificantNumber(number)) {
                for (Integer pos : entry.getValue()) {
                    indicators.add(new HallucinationIndicator(
                            HallucinationType.NUMBER_MISMATCH,
                            number,
                            pos,
                            pos + number.length(),
                            HallucinationSeverity.HIGH,
                            String.format("数字 '%s' 未在来源中找到", number),
                            "建议核实该数据的准确性"
                    ));
                }
            }
        }
        
        // 检查时间一致性
        List<TimeInconsistency> timeInconsistencies = detectTimeInconsistencies(answer, results);
        for (TimeInconsistency inconsistency : timeInconsistencies) {
            indicators.add(new HallucinationIndicator(
                    HallucinationType.TIME_INCONSISTENCY,
                    inconsistency.getTime(),
                    inconsistency.getPosition(),
                    inconsistency.getPosition() + inconsistency.getTime().length(),
                    HallucinationSeverity.MEDIUM,
                    "时间信息可能不一致",
                    "建议核实时间准确性"
            ));
        }
        
        return indicators;
    }
    
    /**
     * 检测常见幻觉模式
     */
    private List<HallucinationIndicator> detectHallucinationPatterns(String answer) {
        List<HallucinationIndicator> indicators = new ArrayList<>();
        
        for (HallucinationPattern pattern : HALLUCINATION_PATTERNS) {
            Matcher matcher = pattern.getPattern().matcher(answer);
            
            while (matcher.find()) {
                String matched = matcher.group();
                
                indicators.add(new HallucinationIndicator(
                        HallucinationType.FABRICATION,
                        matched,
                        matcher.start(),
                        matcher.end(),
                        pattern.getSeverity(),
                        String.format("疑似%s: '%s'", pattern.getDescription(), matched),
                        "建议验证该信息的真实性"
                ));
            }
        }
        
        return indicators;
    }
    
    /**
     * 检测虚构实体
     */
    private List<HallucinationIndicator> detectFabricatedEntities(
            String answer, List<RetrievalResult> results) {
        
        List<HallucinationIndicator> indicators = new ArrayList<>();
        
        // 提取实体名称
        Set<String> entities = extractEntities(answer);
        
        // 检查实体是否在来源中出现
        for (String entity : entities) {
            if (!isEntityInSources(entity, results)) {
                int pos = answer.indexOf(entity);
                if (pos >= 0) {
                    indicators.add(new HallucinationIndicator(
                            HallucinationType.FABRICATED_ENTITY,
                            entity,
                            pos,
                            pos + entity.length(),
                            HallucinationSeverity.HIGH,
                            String.format("实体 '%s' 未在来源中找到", entity),
                            "可能为虚构实体"
                    ));
                }
            }
        }
        
        return indicators;
    }
    
    /**
     * 计算幻觉评分
     */
    private double calculateHallucinationScore(List<HallucinationIndicator> indicators) {
        if (indicators.isEmpty()) {
            return 1.0; // 无幻觉指标，满分
        }
        
        // 根据严重程度计算扣分
        double deduction = 0;
        
        for (HallucinationIndicator indicator : indicators) {
            switch (indicator.getSeverity()) {
                case HIGH:
                    deduction += 0.3;
                    break;
                case MEDIUM:
                    deduction += 0.2;
                    break;
                case LOW:
                    deduction += 0.1;
                    break;
            }
        }
        
        // 限制最大扣分
        deduction = Math.min(deduction, 0.9);
        
        return Math.max(0.1, 1.0 - deduction);
    }
    
    /**
     * 标记幻觉内容
     */
    private String markHallucinations(String answer, List<HallucinationIndicator> indicators) {
        if (indicators.isEmpty()) {
            return answer;
        }
        
        // 按位置排序（从后往前标记，避免位置偏移）
        List<HallucinationIndicator> sorted = new ArrayList<>(indicators);
        sorted.sort((a, b) -> Integer.compare(b.getEndPosition(), a.getEndPosition()));
        
        StringBuilder marked = new StringBuilder(answer);
        
        for (HallucinationIndicator indicator : sorted) {
            String marker = String.format("⚠️[%s]", indicator.getType().getLabel());
            marked.insert(indicator.getEndPosition(), marker);
        }
        
        return marked.toString();
    }
    
    // ============= 辅助方法 =============
    
    private boolean containsSpecificInfo(String text) {
        return text.matches(".*\\d+.*") ||  // 包含数字
               text.matches(".*[A-Z]{2,}.*") ||  // 包含专业术语
               text.matches(".*\\d{4}.*") ||  // 包含年份
               text.matches(".*(公司|机构|组织|系统).*");  // 包含实体
    }
    
    private boolean checkSourceSupport(String sentence, List<RetrievalResult> results) {
        String lowerSentence = sentence.toLowerCase();
        
        for (RetrievalResult result : results) {
            String content = result.getContent().toLowerCase();
            
            // 简单的关键词匹配
            String[] keywords = lowerSentence.split("\\s+");
            int matchCount = 0;
            
            for (String keyword : keywords) {
                if (keyword.length() > 2 && content.contains(keyword)) {
                    matchCount++;
                }
            }
            
            if (matchCount > keywords.length * 0.3) {
                return true;
            }
        }
        
        return false;
    }
    
    private String generateEvidence(String sentence, List<RetrievalResult> results) {
        StringBuilder evidence = new StringBuilder();
        
        for (RetrievalResult result : results) {
            String content = result.getContent();
            // 查找相关片段
            if (content.length() > 100) {
                evidence.append(content.substring(0, 100)).append("...\n");
            } else {
                evidence.append(content).append("\n");
            }
        }
        
        return evidence.toString();
    }
    
    private boolean hasSufficientEvidence(String premise, String conclusion, List<RetrievalResult> results) {
        // 简化实现：检查前提和结论是否都在来源中出现
        boolean premiseInSource = checkSourceSupport(premise, results);
        boolean conclusionInSource = checkSourceSupport(conclusion, results);
        
        return premiseInSource && conclusionInSource;
    }
    
    private Map<String, List<Integer>> extractNumbersWithPositions(String text) {
        Map<String, List<Integer>> numbers = new HashMap<>();
        Pattern pattern = Pattern.compile("\\b\\d+(?:\\.\\d+)?(?:%|万|亿)?\\b");
        Matcher matcher = pattern.matcher(text);
        
        while (matcher.find()) {
            String number = matcher.group();
            numbers.computeIfAbsent(number, k -> new ArrayList<>()).add(matcher.start());
        }
        
        return numbers;
    }
    
    private Set<String> extractNumbers(String text) {
        Set<String> numbers = new HashSet<>();
        Pattern pattern = Pattern.compile("\\b\\d+(?:\\.\\d+)?(?:%|万|亿)?\\b");
        Matcher matcher = pattern.matcher(text);
        
        while (matcher.find()) {
            numbers.add(matcher.group());
        }
        
        return numbers;
    }
    
    private boolean isSignificantNumber(String number) {
        try {
            double value = Double.parseDouble(number.replaceAll("[%万亿]", ""));
            return value > 0;  // 排除0
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    private List<TimeInconsistency> detectTimeInconsistencies(String answer, List<RetrievalResult> results) {
        List<TimeInconsistency> inconsistencies = new ArrayList<>();
        
        // 提取年份
        Pattern yearPattern = Pattern.compile("\\b(20\\d{2}|19\\d{2})\\b");
        Matcher matcher = yearPattern.matcher(answer);
        
        while (matcher.find()) {
            String year = matcher.group();
            // 检查是否为未来年份
            try {
                int yearInt = Integer.parseInt(year);
                int currentYear = Calendar.getInstance().get(Calendar.YEAR);
                
                if (yearInt > currentYear) {
                    inconsistencies.add(new TimeInconsistency(year, matcher.start()));
                }
            } catch (NumberFormatException e) {
                // 忽略
            }
        }
        
        return inconsistencies;
    }
    
    private Set<String> extractEntities(String text) {
        Set<String> entities = new HashSet<>();
        
        // 提取公司名
        Pattern companyPattern = Pattern.compile("[\\u4e00-\\u9fa5]{2,}(公司|企业|集团)");
        Matcher matcher = companyPattern.matcher(text);
        while (matcher.find()) {
            entities.add(matcher.group());
        }
        
        // 提取机构名
        Pattern orgPattern = Pattern.compile("[\\u4e00-\\u9fa5]{2,}(机构|研究院|中心|研究所)");
        matcher = orgPattern.matcher(text);
        while (matcher.find()) {
            entities.add(matcher.group());
        }
        
        return entities;
    }
    
    private boolean isEntityInSources(String entity, List<RetrievalResult> results) {
        for (RetrievalResult result : results) {
            if (result.getContent().contains(entity)) {
                return true;
            }
        }
        return false;
    }
    
    // ============= 内部类 =============
    
    /**
     * 幻觉类型
     */
    public enum HallucinationType {
        UNSUPPORTED_CLAIM("无依据陈述"),
        OVERCONFIDENCE("过度肯定"),
        OVER_INFERENCE("过度推断"),
        NUMBER_MISMATCH("数字不一致"),
        TIME_INCONSISTENCY("时间不一致"),
        FABRICATION("虚构内容"),
        FABRICATED_ENTITY("虚构实体");
        
        private final String label;
        
        HallucinationType(String label) {
            this.label = label;
        }
        
        public String getLabel() {
            return label;
        }
    }
    
    /**
     * 幻觉严重程度
     */
    public enum HallucinationSeverity {
        HIGH,    // 高严重性
        MEDIUM,  // 中等严重性
        LOW      // 低严重性
    }
    
    /**
     * 幻觉指标
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class HallucinationIndicator {
        private HallucinationType type;
        private String text;
        private int startPosition;
        private int endPosition;
        private HallucinationSeverity severity;
        private String description;
        private String evidence;
    }
    
    /**
     * 幻觉模式
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    private static class HallucinationPattern {
        private String description;
        private Pattern pattern;
        private HallucinationSeverity severity;
    }
    
    /**
     * 时间不一致
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    private static class TimeInconsistency {
        private String time;
        private int position;
    }
    
    /**
     * 幻觉检测结果
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class HallucinationDetectionResult {
        private List<HallucinationIndicator> indicators;
        private double hallucinationScore;
        private String markedAnswer;
        private long durationMs;
        
        public static HallucinationDetectionResult empty(String reason) {
            return new HallucinationDetectionResult(
                    Collections.emptyList(),
                    1.0,
                    "",
                    0
            );
        }
        
        public boolean hasHallucinations() {
            return !indicators.isEmpty();
        }
        
        public int getHighSeverityCount() {
            return (int) indicators.stream()
                    .filter(i -> i.getSeverity() == HallucinationSeverity.HIGH)
                    .count();
        }
        
        public Map<HallucinationType, Long> getIndicatorsByType() {
            return indicators.stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                            HallucinationIndicator::getType,
                            java.util.stream.Collectors.counting()
                    ));
        }
    }
}
