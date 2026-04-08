package com.example.embedding.service.impl;

import com.example.embedding.dto.*;
import com.example.embedding.service.EmbeddingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 向量化服务实现
 * 
 * <p>使用 Spring AI EmbeddingModel 进行真实的文本向量化。</p>
 * <p>支持通义千问 text-embedding-v3 模型（OpenAI 兼容模式）。</p>
 */
@Slf4j
@Service
public class EmbeddingServiceImpl implements EmbeddingService {

    private final EmbeddingModel embeddingModel;
    
    // 任务存储（实际生产环境应使用Redis）
    private final Map<String, EmbeddingResponse> taskStore = new ConcurrentHashMap<>();

    @Autowired
    public EmbeddingServiceImpl(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    @Override
    public EmbeddingResponse embedText(EmbeddingRequest request) {
        String text = request.getText();
        log.info("开始文本向量化: {}", text.substring(0, Math.min(50, text.length())));

        String taskId = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();

        try {
            // 调用真实 Embedding 模型
            EmbeddingResponse aiResponse = embeddingModel.embedForResponse(List.of(text));
            
            if (aiResponse == null || aiResponse.getResults().isEmpty()) {
                throw new RuntimeException("Embedding 模型返回空结果");
            }
            
            // 提取向量
            float[] embeddingArray = aiResponse.getResults().get(0).getOutput();
            List<Float> embedding = new ArrayList<>(embeddingArray.length);
            for (float v : embeddingArray) {
                embedding.add(v);
            }

            long duration = System.currentTimeMillis() - startTime;
            int dimension = embedding.size();

            log.info("文本向量化完成，任务ID: {}, 维度: {}, 耗时: {}ms", taskId, dimension, duration);

            EmbeddingResponse response = EmbeddingResponse.success(
                taskId,
                text,
                embedding,
                request.getModel(),
                duration
            );

            taskStore.put(taskId, response);
            return response;

        } catch (Exception e) {
            log.error("文本向量化失败", e);
            EmbeddingResponse response = EmbeddingResponse.failed(taskId, e.getMessage());
            taskStore.put(taskId, response);
            return response;
        }
    }

    @Override
    public EmbeddingBatchResponse batchEmbedTexts(EmbeddingBatchRequest request) {
        log.info("开始批量文本向量化，数量: {}", request.getTexts().size());

        String batchTaskId = UUID.randomUUID().toString();
        EmbeddingBatchResponse response = new EmbeddingBatchResponse();
        response.setBatchTaskId(batchTaskId);
        response.setTotal(request.getTexts().size());

        try {
            // 提取所有文本
            List<String> texts = request.getTexts().stream()
                    .map(EmbeddingRequest::getText)
                    .collect(Collectors.toList());

            long startTime = System.currentTimeMillis();
            
            // 批量调用 Embedding 模型
            EmbeddingResponse aiResponse = embeddingModel.embedForResponse(texts);
            long duration = System.currentTimeMillis() - startTime;

            List<EmbeddingResponse> results = new ArrayList<>();
            
            if (aiResponse != null && !aiResponse.getResults().isEmpty()) {
                for (int i = 0; i < aiResponse.getResults().size(); i++) {
                    String text = texts.get(i);
                    float[] embeddingArray = aiResponse.getResults().get(i).getOutput();
                    
                    List<Float> embedding = new ArrayList<>(embeddingArray.length);
                    for (float v : embeddingArray) {
                        embedding.add(v);
                    }

                    EmbeddingResponse result = EmbeddingResponse.success(
                        UUID.randomUUID().toString(),
                        text,
                        embedding,
                        request.getModel(),
                        duration / texts.size()
                    );
                    results.add(result);
                    response.setSuccessCount(response.getSuccessCount() + 1);
                }
            }

            log.info("批量文本向量化完成，成功: {}, 耗时: {}ms", response.getSuccessCount(), duration);

        } catch (Exception e) {
            log.error("批量文本向量化失败", e);
            // 如果批量失败，降级为逐个处理
            for (EmbeddingRequest textRequest : request.getTexts()) {
                try {
                    EmbeddingResponse result = embedText(textRequest);
                    results.add(result);
                    if (result.getStatus() == EmbeddingResponse.TaskStatus.COMPLETED) {
                        response.setSuccessCount(response.getSuccessCount() + 1);
                    } else {
                        response.setFailedCount(response.getFailedCount() + 1);
                    }
                } catch (Exception ex) {
                    response.setFailedCount(response.getFailedCount() + 1);
                }
            }
        }

        response.setResults(results);
        return response;
    }

    @Override
    public EmbeddingResponse getTaskStatus(String taskId) {
        log.info("查询向量化任务状态: {}", taskId);

        EmbeddingResponse response = taskStore.get(taskId);
        if (response == null) {
            response = new EmbeddingResponse();
            response.setTaskId(taskId);
            response.setStatus(EmbeddingResponse.TaskStatus.PENDING);
        }

        return response;
    }
}
