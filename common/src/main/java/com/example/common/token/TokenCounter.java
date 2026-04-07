package com.example.common.token;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.EncodingType;
import com.knuddels.jtokkit.api.ModelType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Token计数器
 * 
 * <p>基于jtokkit库实现准确的Token计数，支持多种模型。</p>
 * <p>功能特点：</p>
 * <ul>
 *   <li>支持GPT-3.5、GPT-4等主流模型的Token计数</li>
 *   <li>实时流式Token计数</li>
 *   <li>上下文窗口管理</li>
 *   <li>智能截断策略</li>
 * </ul>
 */
@Slf4j
@Component
public class TokenCounter {
    
    private final EncodingRegistry registry = Encodings.newDefaultEncodingRegistry();
    private final Map<String, Encoding> encodingCache = new ConcurrentHashMap<>();
    
    // 默认模型配置
    private static final String DEFAULT_MODEL = "gpt-3.5-turbo";
    private static final int DEFAULT_MAX_TOKENS = 4096;
    
    @PostConstruct
    public void init() {
        log.info("TokenCounter初始化完成，支持模型: GPT-3.5-Turbo, GPT-4, GPT-4-Turbo等");
    }
    
    /**
     * 计算文本的Token数量
     * 
     * @param text 待计算的文本
     * @return Token数量
     */
    public int countTokens(String text) {
        return countTokens(text, DEFAULT_MODEL);
    }
    
    /**
     * 计算文本的Token数量（指定模型）
     * 
     * @param text 待计算的文本
     * @param modelName 模型名称
     * @return Token数量
     */
    public int countTokens(String text, String modelName) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        
        try {
            Encoding encoding = getEncoding(modelName);
            return encoding.countTokens(text);
        } catch (Exception e) {
            log.warn("Token计数失败，使用估算方法: {}", e.getMessage());
            // 降级方案：按字符数估算（中文约1.5字符/token，英文约4字符/token）
            return (int) Math.ceil(text.length() / 2.5);
        }
    }
    
    /**
     * 计算消息列表的总Token数量
     * 
     * @param messages 消息列表（role + content）
     * @return 总Token数量
     */
    public int countMessagesToken(Iterable<Map<String, String>> messages) {
        return countMessagesToken(messages, DEFAULT_MODEL);
    }
    
    /**
     * 计算消息列表的总Token数量（指定模型）
     * 
     * @param messages 消息列表
     * @param modelName 模型名称
     * @return 总Token数量
     */
    public int countMessagesToken(Iterable<Map<String, String>> messages, String modelName) {
        int totalTokens = 0;
        
        // 每条消息的固定开销
        int tokensPerMessage = 4; // role, content, 2个分隔符
        int tokensPerName = 1;    // name字段（如果存在）
        
        Encoding encoding = getEncoding(modelName);
        
        for (Map<String, String> message : messages) {
            totalTokens += tokensPerMessage;
            
            for (Map.Entry<String, String> entry : message.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                
                if (value != null) {
                    totalTokens += encoding.countTokens(value);
                }
                
                if ("name".equals(key)) {
                    totalTokens += tokensPerName;
                }
            }
        }
        
        // 加上消息序列的分隔符
        totalTokens += 2;
        
        return totalTokens;
    }
    
    /**
     * 智能截断文本到指定Token限制
     * 
     * @param text 原始文本
     * @param maxTokens 最大Token数量
     * @return 截断后的文本
     */
    public String truncateText(String text, int maxTokens) {
        return truncateText(text, maxTokens, DEFAULT_MODEL, TruncationStrategy.END);
    }
    
    /**
     * 智能截断文本（指定截断策略）
     * 
     * @param text 原始文本
     * @param maxTokens 最大Token数量
     * @param modelName 模型名称
     * @param strategy 截断策略
     * @return 截断后的文本
     */
    public String truncateText(String text, int maxTokens, String modelName, TruncationStrategy strategy) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        int currentTokens = countTokens(text, modelName);
        if (currentTokens <= maxTokens) {
            return text;
        }
        
        Encoding encoding = getEncoding(modelName);
        
        switch (strategy) {
            case START:
                // 保留开头，截断末尾
                return truncateFromEnd(text, maxTokens, encoding);
            case END:
                // 截断开头，保留末尾
                return truncateFromStart(text, maxTokens, encoding);
            case MIDDLE:
                // 保留开头和结尾，截断中间
                return truncateMiddle(text, maxTokens, encoding);
            case SENTENCE:
                // 按句子边界截断
                return truncateBySentence(text, maxTokens, encoding);
            default:
                return truncateFromEnd(text, maxTokens, encoding);
        }
    }
    
    /**
     * 获取模型的最大上下文窗口
     * 
     * @param modelName 模型名称
     * @return 最大Token数量
     */
    public int getMaxContextTokens(String modelName) {
        try {
            ModelType modelType = ModelType.fromName(modelName)
                    .orElse(ModelType.GPT_3_5_TURBO);
            return modelType.getMaxContextLength();
        } catch (Exception e) {
            log.warn("无法获取模型 {} 的上下文窗口大小，使用默认值: {}", modelName, DEFAULT_MAX_TOKENS);
            return DEFAULT_MAX_TOKENS;
        }
    }
    
    /**
     * 检查是否超出Token限制
     * 
     * @param text 文本
     * @param maxTokens 最大Token数量
     * @return true-超出限制，false-未超出
     */
    public boolean exceedsTokenLimit(String text, int maxTokens) {
        return countTokens(text) > maxTokens;
    }
    
    /**
     * 获取剩余可用Token数量
     * 
     * @param usedTokens 已使用的Token数量
     * @param modelName 模型名称
     * @return 剩余Token数量
     */
    public int getRemainingTokens(int usedTokens, String modelName) {
        int maxTokens = getMaxContextTokens(modelName);
        return Math.max(0, maxTokens - usedTokens);
    }
    
    /**
     * 批量计算Token数量（优化性能）
     * 
     * @param texts 文本列表
     * @return Token数量列表
     */
    public int[] batchCountTokens(Iterable<String> texts) {
        return batchCountTokens(texts, DEFAULT_MODEL);
    }
    
    /**
     * 批量计算Token数量（指定模型）
     */
    public int[] batchCountTokens(Iterable<String> texts, String modelName) {
        Encoding encoding = getEncoding(modelName);
        
        int count = 0;
        for (String ignored : texts) {
            count++;
        }
        
        int[] tokenCounts = new int[count];
        int index = 0;
        
        for (String text : texts) {
            tokenCounts[index++] = text != null ? encoding.countTokens(text) : 0;
        }
        
        return tokenCounts;
    }
    
    // ============= 私有辅助方法 =============
    
    /**
     * 获取编码器（带缓存）
     */
    private Encoding getEncoding(String modelName) {
        return encodingCache.computeIfAbsent(modelName, name -> {
            try {
                ModelType modelType = ModelType.fromName(name)
                        .orElse(ModelType.GPT_3_5_TURBO);
                return registry.getEncodingForModel(modelType);
            } catch (Exception e) {
                log.warn("无法获取模型 {} 的编码器，使用默认编码器", name);
                return registry.getEncoding(EncodingType.CL100K_BASE);
            }
        });
    }
    
    /**
     * 从末尾截断
     */
    private String truncateFromEnd(String text, int maxTokens, Encoding encoding) {
        // 二分查找合适的截断位置
        int left = 0;
        int right = text.length();
        int bestIndex = 0;
        
        while (left <= right) {
            int mid = left + (right - left) / 2;
            String substring = text.substring(0, mid);
            int tokens = encoding.countTokens(substring);
            
            if (tokens <= maxTokens) {
                bestIndex = mid;
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }
        
        String result = text.substring(0, bestIndex);
        
        // 尝试在句子边界截断
        int lastSentenceEnd = findLastSentenceEnd(result);
        if (lastSentenceEnd > bestIndex * 0.8) {
            result = result.substring(0, lastSentenceEnd);
        }
        
        return result + "...";
    }
    
    /**
     * 从开头截断
     */
    private String truncateFromStart(String text, int maxTokens, Encoding encoding) {
        // 二分查找合适的起始位置
        int left = 0;
        int right = text.length();
        int bestIndex = text.length();
        
        while (left <= right) {
            int mid = left + (right - left) / 2;
            String substring = text.substring(mid);
            int tokens = encoding.countTokens(substring);
            
            if (tokens <= maxTokens) {
                bestIndex = mid;
                right = mid - 1;
            } else {
                left = mid + 1;
            }
        }
        
        String result = text.substring(bestIndex);
        
        // 尝试在句子边界截断
        int firstSentenceStart = findFirstSentenceStart(result);
        if (firstSentenceStart < result.length() * 0.2) {
            result = result.substring(firstSentenceStart);
        }
        
        return "..." + result;
    }
    
    /**
     * 截断中间部分
     */
    private String truncateMiddle(String text, int maxTokens, Encoding encoding) {
        int halfTokens = maxTokens / 2;
        
        // 保留开头部分
        String start = truncateFromEnd(text.substring(0, text.length() / 2), halfTokens, encoding);
        // 保留结尾部分
        String end = truncateFromStart(text.substring(text.length() / 2), halfTokens, encoding);
        
        return start + "\n\n[...内容已省略...]\n\n" + end;
    }
    
    /**
     * 按句子边界截断
     */
    private String truncateBySentence(String text, int maxTokens, Encoding encoding) {
        String[] sentences = text.split("(?<=[。.!?！？])\\s*");
        
        StringBuilder result = new StringBuilder();
        int currentTokens = 0;
        
        for (String sentence : sentences) {
            int sentenceTokens = encoding.countTokens(sentence);
            
            if (currentTokens + sentenceTokens <= maxTokens) {
                result.append(sentence);
                currentTokens += sentenceTokens;
            } else {
                break;
            }
        }
        
        if (result.length() < text.length()) {
            result.append("...");
        }
        
        return result.toString();
    }
    
    /**
     * 查找最后一个句子边界
     */
    private int findLastSentenceEnd(String text) {
        for (int i = text.length() - 1; i >= 0; i--) {
            char c = text.charAt(i);
            if (c == '。' || c == '.' || c == '!' || c == '?' || c == '！' || c == '？') {
                return i + 1;
            }
        }
        return text.length();
    }
    
    /**
     * 查找第一个句子起始位置
     */
    private int findFirstSentenceStart(String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c != ' ' && c != '\n' && c != '\t') {
                return i;
            }
        }
        return 0;
    }
    
    /**
     * 截断策略枚举
     */
    public enum TruncationStrategy {
        /** 保留开头 */
        START,
        /** 保留结尾 */
        END,
        /** 保留开头和结尾 */
        MIDDLE,
        /** 按句子边界截断 */
        SENTENCE
    }
    
    /**
     * Token统计信息
     */
    public static class TokenStats {
        private final int totalTokens;
        private final int maxTokens;
        private final int remainingTokens;
        private final double usagePercentage;
        
        public TokenStats(int totalTokens, int maxTokens) {
            this.totalTokens = totalTokens;
            this.maxTokens = maxTokens;
            this.remainingTokens = Math.max(0, maxTokens - totalTokens);
            this.usagePercentage = maxTokens > 0 ? (double) totalTokens / maxTokens * 100 : 0;
        }
        
        public int getTotalTokens() { return totalTokens; }
        public int getMaxTokens() { return maxTokens; }
        public int getRemainingTokens() { return remainingTokens; }
        public double getUsagePercentage() { return usagePercentage; }
        
        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("totalTokens", totalTokens);
            map.put("maxTokens", maxTokens);
            map.put("remainingTokens", remainingTokens);
            map.put("usagePercentage", String.format("%.2f%%", usagePercentage));
            return map;
        }
    }
}
