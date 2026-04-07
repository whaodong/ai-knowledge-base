package com.example.common.context;

import com.example.common.token.TokenCounter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 智能上下文管理器
 * 
 * <p>基于Token计数实现上下文窗口管理，支持多种截断策略。</p>
 * <p>功能特点：</p>
 * <ul>
 *   <li>精确的Token级别上下文管理</li>
 *   <li>保留系统提示词</li>
 *   <li>保留最近N轮对话</li>
 *   <li>按相似度选择保留的检索结果</li>
 *   <li>支持流式响应Token计数</li>
 * </ul>
 */
@Slf4j
@Component
public class SmartContextManager {
    
    private final TokenCounter tokenCounter;
    
    @Value("${rag.context.max-tokens:4096}")
    private int defaultMaxTokens;
    
    @Value("${rag.context.system-prompt-reserved-tokens:500}")
    private int systemPromptReservedTokens;
    
    @Value("${rag.context.response-reserved-tokens:1000}")
    private int responseReservedTokens;
    
    @Value("${rag.context.recent-turns:3}")
    private int recentConversationTurns;
    
    @Value("${rag.context.similarity-threshold:0.7}")
    private double similarityThreshold;
    
    @Autowired
    public SmartContextManager(TokenCounter tokenCounter) {
        this.tokenCounter = tokenCounter;
    }
    
    /**
     * 构建完整的上下文
     * 
     * @param systemPrompt 系统提示词
     * @param conversationHistory 对话历史
     * @param retrievalResults 检索结果
     * @param currentQuery 当前查询
     * @param modelName 模型名称
     * @return 构建后的上下文
     */
    public ContextBuildResult buildContext(
            String systemPrompt,
            List<ConversationTurn> conversationHistory,
            List<RetrievalContext> retrievalResults,
            String currentQuery,
            String modelName) {
        
        // 获取模型最大Token数
        int maxTokens = tokenCounter.getMaxContextTokens(modelName);
        int availableTokens = maxTokens - responseReservedTokens;
        
        // 计算各部分Token预算
        int systemTokens = calculateAndReserveSystemPrompt(systemPrompt, availableTokens);
        availableTokens -= systemTokens;
        
        // 预留当前查询的Token
        int queryTokens = tokenCounter.countTokens(currentQuery, modelName);
        availableTokens -= queryTokens;
        
        // 分配对话历史和检索结果的Token预算
        int conversationBudget = (int) (availableTokens * 0.4);
        int retrievalBudget = (int) (availableTokens * 0.6);
        
        // 构建上下文各部分
        ContextParts parts = new ContextParts();
        parts.systemPrompt = systemPrompt;
        parts.systemTokens = systemTokens;
        
        // 选择最近的对话历史
        parts.selectedConversation = selectRecentConversation(
                conversationHistory, conversationBudget, modelName);
        parts.conversationTokens = calculateConversationTokens(parts.selectedConversation, modelName);
        
        // 按相似度选择检索结果
        parts.selectedRetrieval = selectRetrievalBySimilarity(
                retrievalResults, retrievalBudget, modelName);
        parts.retrievalTokens = calculateRetrievalTokens(parts.selectedRetrieval, modelName);
        
        parts.currentQuery = currentQuery;
        parts.queryTokens = queryTokens;
        
        // 构建最终文本
        String contextText = buildContextText(parts);
        int totalTokens = parts.getTotalTokens();
        
        // 创建统计信息
        ContextBuildResult result = new ContextBuildResult();
        result.setContextText(contextText);
        result.setTotalTokens(totalTokens);
        result.setMaxTokens(maxTokens);
        result.setSystemTokens(systemTokens);
        result.setConversationTokens(parts.conversationTokens);
        result.setRetrievalTokens(parts.retrievalTokens);
        result.setQueryTokens(queryTokens);
        result.setRemainingTokens(maxTokens - totalTokens);
        result.setParts(parts);
        
        log.debug("上下文构建完成: 总Token={}, 系统提示={}, 对话={}, 检索={}, 查询={}",
                totalTokens, systemTokens, parts.conversationTokens, 
                parts.retrievalTokens, queryTokens);
        
        return result;
    }
    
    /**
     * 智能截断上下文
     * 
     * @param contextText 上下文文本
     * @param maxTokens 最大Token数
     * @param modelName 模型名称
     * @return 截断后的上下文
     */
    public String smartTruncate(String contextText, int maxTokens, String modelName) {
        if (contextText == null || contextText.isEmpty()) {
            return contextText;
        }
        
        int currentTokens = tokenCounter.countTokens(contextText, modelName);
        if (currentTokens <= maxTokens) {
            return contextText;
        }
        
        // 解析上下文各部分
        String[] sections = parseContextSections(contextText);
        
        StringBuilder result = new StringBuilder();
        int usedTokens = 0;
        
        // 1. 优先保留系统提示词
        if (sections.length > 0 && sections[0].startsWith("[系统提示]")) {
            int systemTokens = tokenCounter.countTokens(sections[0], modelName);
            if (usedTokens + systemTokens <= maxTokens) {
                result.append(sections[0]).append("\n\n");
                usedTokens += systemTokens;
            }
        }
        
        // 2. 保留当前查询
        if (sections.length > 0) {
            String querySection = sections[sections.length - 1];
            int queryTokens = tokenCounter.countTokens(querySection, modelName);
            if (usedTokens + queryTokens <= maxTokens) {
                result.append(querySection).append("\n\n");
                usedTokens += queryTokens;
            }
        }
        
        // 3. 尝试保留检索结果（按相似度排序）
        if (sections.length > 2) {
            int remainingTokens = maxTokens - usedTokens;
            List<IndexedSection> retrievalSections = new ArrayList<>();
            
            for (int i = 1; i < sections.length - 1; i++) {
                if (sections[i].contains("[检索结果]")) {
                    double similarity = extractSimilarity(sections[i]);
                    retrievalSections.add(new IndexedSection(i, sections[i], similarity));
                }
            }
            
            // 按相似度降序排序
            retrievalSections.sort((a, b) -> Double.compare(b.similarity, a.similarity));
            
            // 添加检索结果直到达到Token限制
            for (IndexedSection section : retrievalSections) {
                int sectionTokens = tokenCounter.countTokens(section.text, modelName);
                if (usedTokens + sectionTokens <= maxTokens) {
                    result.insert(result.length() - querySectionLength(result), 
                            section.text + "\n\n");
                    usedTokens += sectionTokens;
                } else {
                    // 尝试截断后添加
                    String truncated = tokenCounter.truncateText(
                            section.text, remainingTokens, modelName, 
                            TokenCounter.TruncationStrategy.SENTENCE);
                    if (!truncated.isEmpty()) {
                        result.insert(result.length() - querySectionLength(result),
                                truncated + "\n\n");
                        usedTokens += tokenCounter.countTokens(truncated, modelName);
                    }
                    break;
                }
            }
        }
        
        return result.toString().trim();
    }
    
    /**
     * 流式响应Token计数器
     */
    public StreamingTokenCounter createStreamingCounter(String modelName) {
        return new StreamingTokenCounter(tokenCounter, modelName);
    }
    
    // ============= 私有辅助方法 =============
    
    private int calculateAndReserveSystemPrompt(String systemPrompt, int availableTokens) {
        if (systemPrompt == null || systemPrompt.isEmpty()) {
            return 0;
        }
        
        int systemTokens = tokenCounter.countTokens(systemPrompt);
        
        // 如果系统提示词超过预留Token，进行截断
        if (systemTokens > systemPromptReservedTokens) {
            log.warn("系统提示词超过预留Token数: {} > {}, 将进行截断", 
                    systemTokens, systemPromptReservedTokens);
            return systemPromptReservedTokens;
        }
        
        return systemTokens;
    }
    
    private List<ConversationTurn> selectRecentConversation(
            List<ConversationTurn> history, int budget, String modelName) {
        
        if (history == null || history.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<ConversationTurn> selected = new ArrayList<>();
        int usedTokens = 0;
        
        // 从最近的对话开始选择
        for (int i = history.size() - 1; i >= 0 && selected.size() < recentConversationTurns; i--) {
            ConversationTurn turn = history.get(i);
            int turnTokens = calculateConversationTurnTokens(turn, modelName);
            
            if (usedTokens + turnTokens <= budget) {
                selected.add(0, turn); // 添加到开头，保持时间顺序
                usedTokens += turnTokens;
            } else {
                // 尝试只保留用户问题
                int questionTokens = tokenCounter.countTokens(turn.getUserMessage(), modelName);
                if (usedTokens + questionTokens <= budget) {
                    ConversationTurn partialTurn = new ConversationTurn();
                    partialTurn.setUserMessage(turn.getUserMessage());
                    partialTurn.setAssistantMessage("");
                    selected.add(0, partialTurn);
                    usedTokens += questionTokens;
                }
                break;
            }
        }
        
        return selected;
    }
    
    private List<RetrievalContext> selectRetrievalBySimilarity(
            List<RetrievalContext> results, int budget, String modelName) {
        
        if (results == null || results.isEmpty()) {
            return Collections.emptyList();
        }
        
        // 过滤低相似度的结果
        List<RetrievalContext> filtered = results.stream()
                .filter(r -> r.getSimilarity() >= similarityThreshold)
                .sorted((r1, r2) -> Double.compare(r2.getSimilarity(), r1.getSimilarity()))
                .collect(Collectors.toList());
        
        List<RetrievalContext> selected = new ArrayList<>();
        int usedTokens = 0;
        
        for (RetrievalContext result : filtered) {
            int resultTokens = tokenCounter.countTokens(result.getContent(), modelName);
            
            if (usedTokens + resultTokens <= budget) {
                selected.add(result);
                usedTokens += resultTokens;
            } else {
                // 尝试截断后添加
                int remainingTokens = budget - usedTokens;
                if (remainingTokens > 100) {
                    String truncated = tokenCounter.truncateText(
                            result.getContent(), remainingTokens, modelName,
                            TokenCounter.TruncationStrategy.SENTENCE);
                    
                    RetrievalContext truncatedResult = new RetrievalContext();
                    truncatedResult.setContent(truncated);
                    truncatedResult.setSimilarity(result.getSimilarity());
                    truncatedResult.setSource(result.getSource());
                    truncatedResult.setTruncated(true);
                    selected.add(truncatedResult);
                }
                break;
            }
        }
        
        return selected;
    }
    
    private int calculateConversationTokens(List<ConversationTurn> turns, String modelName) {
        int total = 0;
        for (ConversationTurn turn : turns) {
            total += calculateConversationTurnTokens(turn, modelName);
        }
        return total;
    }
    
    private int calculateConversationTurnTokens(ConversationTurn turn, String modelName) {
        int tokens = 0;
        tokens += tokenCounter.countTokens(turn.getUserMessage(), modelName);
        tokens += tokenCounter.countTokens(turn.getAssistantMessage(), modelName);
        // 每轮对话的格式开销
        tokens += 10;
        return tokens;
    }
    
    private int calculateRetrievalTokens(List<RetrievalContext> results, String modelName) {
        int total = 0;
        for (RetrievalContext result : results) {
            total += tokenCounter.countTokens(result.getContent(), modelName);
        }
        return total;
    }
    
    private String buildContextText(ContextParts parts) {
        StringBuilder builder = new StringBuilder();
        
        // 系统提示词
        if (parts.systemPrompt != null && !parts.systemPrompt.isEmpty()) {
            builder.append("[系统提示]\n").append(parts.systemPrompt).append("\n\n");
        }
        
        // 检索结果
        if (parts.selectedRetrieval != null && !parts.selectedRetrieval.isEmpty()) {
            builder.append("[检索结果]\n");
            for (int i = 0; i < parts.selectedRetrieval.size(); i++) {
                RetrievalContext result = parts.selectedRetrieval.get(i);
                builder.append(String.format("[%d] (相似度: %.2f) %s\n来源: %s\n",
                        i + 1, result.getSimilarity(), result.getContent(), 
                        result.getSource()));
                if (result.isTruncated()) {
                    builder.append("(内容已截断)\n");
                }
                builder.append("\n");
            }
        }
        
        // 对话历史
        if (parts.selectedConversation != null && !parts.selectedConversation.isEmpty()) {
            builder.append("[对话历史]\n");
            for (ConversationTurn turn : parts.selectedConversation) {
                builder.append("用户: ").append(turn.getUserMessage()).append("\n");
                if (turn.getAssistantMessage() != null && !turn.getAssistantMessage().isEmpty()) {
                    builder.append("助手: ").append(turn.getAssistantMessage()).append("\n");
                }
            }
            builder.append("\n");
        }
        
        // 当前查询
        builder.append("[当前查询]\n").append(parts.currentQuery);
        
        return builder.toString();
    }
    
    private String[] parseContextSections(String contextText) {
        return contextText.split("\n\n(?=\\[)");
    }
    
    private double extractSimilarity(String section) {
        try {
            int start = section.indexOf("(相似度: ");
            if (start == -1) return 0.0;
            start += 7;
            int end = section.indexOf(")", start);
            return Double.parseDouble(section.substring(start, end));
        } catch (Exception e) {
            return 0.0;
        }
    }
    
    private int querySectionLength(StringBuilder builder) {
        String text = builder.toString();
        int idx = text.lastIndexOf("[当前查询]");
        if (idx == -1) return 0;
        return text.length() - idx;
    }
    
    // ============= 内部类 =============
    
    private static class ContextParts {
        String systemPrompt;
        int systemTokens;
        List<ConversationTurn> selectedConversation;
        int conversationTokens;
        List<RetrievalContext> selectedRetrieval;
        int retrievalTokens;
        String currentQuery;
        int queryTokens;
        
        int getTotalTokens() {
            return systemTokens + conversationTokens + retrievalTokens + queryTokens;
        }
    }
    
    private static class IndexedSection {
        int index;
        String text;
        double similarity;
        
        IndexedSection(int index, String text, double similarity) {
            this.index = index;
            this.text = text;
            this.similarity = similarity;
        }
    }
    
    /**
     * 对话轮次
     */
    public static class ConversationTurn {
        private String userMessage;
        private String assistantMessage;
        
        public String getUserMessage() { return userMessage; }
        public void setUserMessage(String userMessage) { this.userMessage = userMessage; }
        public String getAssistantMessage() { return assistantMessage; }
        public void setAssistantMessage(String assistantMessage) { this.assistantMessage = assistantMessage; }
    }
    
    /**
     * 检索上下文
     */
    public static class RetrievalContext {
        private String content;
        private double similarity;
        private String source;
        private boolean truncated;
        
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public double getSimilarity() { return similarity; }
        public void setSimilarity(double similarity) { this.similarity = similarity; }
        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }
        public boolean isTruncated() { return truncated; }
        public void setTruncated(boolean truncated) { this.truncated = truncated; }
    }
    
    /**
     * 上下文构建结果
     */
    public static class ContextBuildResult {
        private String contextText;
        private int totalTokens;
        private int maxTokens;
        private int systemTokens;
        private int conversationTokens;
        private int retrievalTokens;
        private int queryTokens;
        private int remainingTokens;
        private ContextParts parts;
        
        // Getters
        public String getContextText() { return contextText; }
        public int getTotalTokens() { return totalTokens; }
        public int getMaxTokens() { return maxTokens; }
        public int getSystemTokens() { return systemTokens; }
        public int getConversationTokens() { return conversationTokens; }
        public int getRetrievalTokens() { return retrievalTokens; }
        public int getQueryTokens() { return queryTokens; }
        public int getRemainingTokens() { return remainingTokens; }
        public ContextParts getParts() { return parts; }
        
        // Setters
        public void setContextText(String contextText) { this.contextText = contextText; }
        public void setTotalTokens(int totalTokens) { this.totalTokens = totalTokens; }
        public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }
        public void setSystemTokens(int systemTokens) { this.systemTokens = systemTokens; }
        public void setConversationTokens(int conversationTokens) { this.conversationTokens = conversationTokens; }
        public void setRetrievalTokens(int retrievalTokens) { this.retrievalTokens = retrievalTokens; }
        public void setQueryTokens(int queryTokens) { this.queryTokens = queryTokens; }
        public void setRemainingTokens(int remainingTokens) { this.remainingTokens = remainingTokens; }
        public void setParts(ContextParts parts) { this.parts = parts; }
    }
    
    /**
     * 流式Token计数器
     */
    public static class StreamingTokenCounter {
        private final TokenCounter tokenCounter;
        private final String modelName;
        private final StringBuilder buffer;
        private int totalTokens;
        
        public StreamingTokenCounter(TokenCounter tokenCounter, String modelName) {
            this.tokenCounter = tokenCounter;
            this.modelName = modelName;
            this.buffer = new StringBuilder();
            this.totalTokens = 0;
        }
        
        /**
         * 添加Token块
         */
        public void addChunk(String chunk) {
            buffer.append(chunk);
            totalTokens = tokenCounter.countTokens(buffer.toString(), modelName);
        }
        
        /**
         * 获取当前Token数
         */
        public int getCurrentTokens() {
            return totalTokens;
        }
        
        /**
         * 获取完整文本
         */
        public String getCompleteText() {
            return buffer.toString();
        }
        
        /**
         * 重置计数器
         */
        public void reset() {
            buffer.setLength(0);
            totalTokens = 0;
        }
    }
}
