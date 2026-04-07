package com.example.rag.manager;

import com.example.rag.model.RagRequest;
import com.example.rag.model.RetrievalResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 上下文管理器
 * 
 * <p>负责管理RAG检索的上下文窗口，处理文档分块策略，
 * 确保生成的上下文不超过模型限制。</p>
 */
@Slf4j
@Component
public class ContextManager {
    
    @Value("${rag.context.max-length:4000}")
    private int defaultMaxContextLength;
    
    @Value("${rag.context.overlap-size:100}")
    private int chunkOverlapSize;
    
    @Value("${rag.context.semantic-chunk-size:500}")
    private int semanticChunkSize;
    
    @Value("${rag.context.fixed-chunk-size:1000}")
    private int fixedChunkSize;
    
    /**
     * 融合检索结果，生成最终上下文
     * 
     * @param request RAG请求
     * @param results 检索结果列表
     * @return 融合后的上下文文本
     */
    public String fuseContext(RagRequest request, List<RetrievalResult> results) {
        if (results == null || results.isEmpty()) {
            return "";
        }
        
        int maxLength = request.getMaxContextLength() > 0 ? 
                request.getMaxContextLength() : defaultMaxContextLength;
        
        // 按重排序分数排序
        List<RetrievalResult> sortedResults = results.stream()
                .sorted((r1, r2) -> Double.compare(r2.getRerankScore(), r1.getRerankScore()))
                .collect(Collectors.toList());
        
        StringBuilder contextBuilder = new StringBuilder();
        Set<String> includedDocumentIds = new HashSet<>();
        int currentLength = 0;
        
        // 按相关性顺序添加文档内容，直到达到最大长度
        for (RetrievalResult result : sortedResults) {
            if (currentLength >= maxLength) {
                break;
            }
            
            // 去重：避免同一文档被多次添加
            if (includedDocumentIds.contains(result.getDocumentId())) {
                continue;
            }
            
            String content = result.getContent();
            int contentLength = content.length();
            
            // 如果添加此文档会超过最大长度，则进行截断
            if (currentLength + contentLength > maxLength) {
                int remainingLength = maxLength - currentLength;
                if (remainingLength > 100) { // 至少保留100个字符才有意义
                    content = content.substring(0, remainingLength) + "...";
                    contentLength = remainingLength + 3; // 加上省略号长度
                } else {
                    continue; // 剩余空间太小，跳过此文档
                }
            }
            
            // 添加文档分隔符
            if (currentLength > 0) {
                contextBuilder.append("\n\n---\n\n");
                currentLength += 6; // 分隔符长度估计
            }
            
            // 添加文档内容
            contextBuilder.append(content);
            includedDocumentIds.add(result.getDocumentId());
            currentLength += contentLength;
            
            // 添加元数据信息（可选）
            if (result.getMetadata() != null && !result.getMetadata().isEmpty()) {
                String source = result.getMetadata().getOrDefault("source", "未知来源").toString();
                contextBuilder.append("\n[来源: ").append(source).append("]");
                currentLength += source.length() + 7;
            }
        }
        
        String fusedContext = contextBuilder.toString();
        log.debug("上下文融合完成，总长度: {}字符，包含文档数: {}", 
                fusedContext.length(), includedDocumentIds.size());
        
        return fusedContext;
    }
    
    /**
     * 应用分块策略
     * 
     * @param document 原始文档
     * @param strategy 分块策略 (semantic/fixed)
     * @return 分块后的文档列表
     */
    public List<String> chunkDocument(String document, String strategy) {
        List<String> chunks = new ArrayList<>();
        
        if (strategy == null || strategy.equalsIgnoreCase("semantic")) {
            chunks = semanticChunking(document);
        } else if (strategy.equalsIgnoreCase("fixed")) {
            chunks = fixedLengthChunking(document);
        } else {
            log.warn("未知的分块策略: {}，使用默认语义分块", strategy);
            chunks = semanticChunking(document);
        }
        
        log.debug("文档分块完成，策略: {}, 原始长度: {}, 分块数: {}", 
                strategy, document.length(), chunks.size());
        
        return chunks;
    }
    
    /**
     * 语义分块（简化实现）
     * 
     * <p>实际生产环境应使用NLP模型进行语义边界检测，
     * 这里使用段落和句子边界作为近似。</p>
     */
    private List<String> semanticChunking(String document) {
        List<String> chunks = new ArrayList<>();
        
        // 按段落分割
        String[] paragraphs = document.split("\n\n");
        
        StringBuilder currentChunk = new StringBuilder();
        int currentChunkSize = 0;
        
        for (String paragraph : paragraphs) {
            int paragraphLength = paragraph.length();
            
            // 如果段落本身超过语义分块大小，则按句子进一步分割
            if (paragraphLength > semanticChunkSize) {
                // 先提交当前块
                if (currentChunk.length() > 0) {
                    chunks.add(currentChunk.toString());
                    currentChunk = new StringBuilder();
                    currentChunkSize = 0;
                }
                
                // 按句子分割段落
                List<String> sentences = splitIntoSentences(paragraph);
                for (String sentence : sentences) {
                    int sentenceLength = sentence.length();
                    
                    if (currentChunkSize + sentenceLength > semanticChunkSize) {
                        if (currentChunk.length() > 0) {
                            chunks.add(currentChunk.toString());
                            currentChunk = new StringBuilder();
                            currentChunkSize = 0;
                        }
                    }
                    
                    if (currentChunk.length() > 0) {
                        currentChunk.append(" ");
                        currentChunkSize += 1;
                    }
                    
                    currentChunk.append(sentence);
                    currentChunkSize += sentenceLength;
                }
                
            } else {
                // 如果添加此段落会超过分块大小，则提交当前块
                if (currentChunkSize + paragraphLength > semanticChunkSize) {
                    if (currentChunk.length() > 0) {
                        chunks.add(currentChunk.toString());
                        currentChunk = new StringBuilder();
                        currentChunkSize = 0;
                    }
                }
                
                // 添加段落
                if (currentChunk.length() > 0) {
                    currentChunk.append("\n\n");
                    currentChunkSize += 2;
                }
                
                currentChunk.append(paragraph);
                currentChunkSize += paragraphLength;
            }
        }
        
        // 添加最后一个块
        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString());
        }
        
        return chunks;
    }
    
    /**
     * 固定长度分块
     */
    private List<String> fixedLengthChunking(String document) {
        List<String> chunks = new ArrayList<>();
        int documentLength = document.length();
        int startIndex = 0;
        
        while (startIndex < documentLength) {
            int endIndex = Math.min(startIndex + fixedChunkSize, documentLength);
            
            // 尝试在句子或单词边界处截断
            if (endIndex < documentLength) {
                // 查找最近的句子边界（句号、问号、感叹号）
                int sentenceBoundary = findSentenceBoundary(document, startIndex, endIndex);
                if (sentenceBoundary > startIndex + fixedChunkSize * 0.7) {
                    // 如果找到合适的边界，且边界在块的后70%位置，则使用该边界
                    endIndex = sentenceBoundary;
                } else {
                    // 否则查找单词边界
                    int wordBoundary = findWordBoundary(document, startIndex, endIndex);
                    if (wordBoundary > startIndex + fixedChunkSize * 0.8) {
                        endIndex = wordBoundary;
                    }
                }
            }
            
            String chunk = document.substring(startIndex, endIndex);
            chunks.add(chunk);
            
            // 下一个块的起始位置考虑重叠
            startIndex = endIndex - chunkOverlapSize;
            if (startIndex < 0) {
                startIndex = 0;
            }
            
            // 防止无限循环
            if (startIndex >= documentLength || startIndex == endIndex) {
                break;
            }
        }
        
        return chunks;
    }
    
    /**
     * 查找句子边界
     */
    private int findSentenceBoundary(String text, int start, int end) {
        // 查找句号、问号、感叹号，后面跟着空格或换行
        for (int i = end - 1; i > start; i--) {
            char c = text.charAt(i);
            if (c == '.' || c == '?' || c == '!') {
                if (i + 1 < text.length()) {
                    char nextChar = text.charAt(i + 1);
                    if (nextChar == ' ' || nextChar == '\n' || nextChar == '\r') {
                        return i + 1; // 返回边界后位置
                    }
                }
            }
        }
        return end;
    }
    
    /**
     * 查找单词边界
     */
    private int findWordBoundary(String text, int start, int end) {
        // 查找空格或标点符号
        for (int i = end - 1; i > start; i--) {
            char c = text.charAt(i);
            if (c == ' ' || c == ',' || c == ';' || c == ':' || c == '\n' || c == '\r') {
                return i + 1; // 返回边界后位置
            }
        }
        return end;
    }
    
    /**
     * 分割成句子（简化实现）
     */
    private List<String> splitIntoSentences(String text) {
        List<String> sentences = new ArrayList<>();
        
        // 使用正则表达式分割句子
        String[] parts = text.split("(?<=[.!?])\\s+");
        
        for (String part : parts) {
            if (!part.trim().isEmpty()) {
                sentences.add(part.trim());
            }
        }
        
        return sentences;
    }
    
    /**
     * 评估分块质量
     * 
     * @param chunks 分块列表
     * @return 质量指标
     */
    public Map<String, Object> evaluateChunkingQuality(List<String> chunks) {
        Map<String, Object> metrics = new HashMap<>();
        
        if (chunks == null || chunks.isEmpty()) {
            metrics.put("chunk_count", 0);
            metrics.put("average_length", 0);
            metrics.put("std_dev_length", 0);
            metrics.put("cohesion_score", 0.0);
            return metrics;
        }
        
        // 计算基本统计信息
        double totalLength = 0.0;
        List<Integer> lengths = new ArrayList<>();
        
        for (String chunk : chunks) {
            int length = chunk.length();
            totalLength += length;
            lengths.add(length);
        }
        
        double averageLength = totalLength / chunks.size();
        
        // 计算标准差
        double variance = 0.0;
        for (int length : lengths) {
            variance += Math.pow(length - averageLength, 2);
        }
        double stdDev = Math.sqrt(variance / chunks.size());
        
        // 计算连贯性分数（简化：基于块内句子完整性）
        double cohesionScore = calculateCohesionScore(chunks);
        
        metrics.put("chunk_count", chunks.size());
        metrics.put("average_length", averageLength);
        metrics.put("std_dev_length", stdDev);
        metrics.put("cohesion_score", cohesionScore);
        
        return metrics;
    }
    
    /**
     * 计算连贯性分数
     */
    private double calculateCohesionScore(List<String> chunks) {
        if (chunks.isEmpty()) {
            return 0.0;
        }
        
        int completeSentences = 0;
        int totalSentences = 0;
        
        for (String chunk : chunks) {
            // 简单检测：以标点符号结尾的句子视为完整
            String trimmed = chunk.trim();
            if (!trimmed.isEmpty()) {
                totalSentences++;
                char lastChar = trimmed.charAt(trimmed.length() - 1);
                if (lastChar == '.' || lastChar == '?' || lastChar == '!' || lastChar == '。' || lastChar == '？' || lastChar == '！') {
                    completeSentences++;
                }
            }
        }
        
        return totalSentences > 0 ? (double) completeSentences / totalSentences : 0.0;
    }
}