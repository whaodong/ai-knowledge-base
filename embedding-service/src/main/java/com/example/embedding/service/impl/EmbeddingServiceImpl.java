package com.example.embedding.service.impl;

import com.example.embedding.dto.*;
import com.example.embedding.service.EmbeddingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 向量化服务实现
 */
@Slf4j
@Service
public class EmbeddingServiceImpl implements EmbeddingService {

    // 模拟任务存储（实际应使用Redis或数据库）
    private final Map<String, EmbeddingResponse> taskStore = new ConcurrentHashMap<>();

    @Override
    public EmbeddingResponse embedText(EmbeddingRequest request) {
        log.info("开始文本向量化: {}", request.getText().substring(0, Math.min(50, request.getText().length())));

        String taskId = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();

        try {
            // 模拟向量化过程（实际应调用OpenAI API或本地模型）
            List<Float> embedding = generateMockEmbedding(1536);

            long duration = System.currentTimeMillis() - startTime;

            EmbeddingResponse response = EmbeddingResponse.success(
                taskId,
                request.getText(),
                embedding,
                request.getModel(),
                duration
            );

            // 存储任务结果
            taskStore.put(taskId, response);

            log.info("文本向量化完成，任务ID: {}, 耗时: {}ms", taskId, duration);
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

        List<EmbeddingResponse> results = new ArrayList<>();

        for (EmbeddingRequest textRequest : request.getTexts()) {
            try {
                EmbeddingResponse result = embedText(textRequest);
                results.add(result);

                if (result.getStatus() == EmbeddingResponse.TaskStatus.COMPLETED) {
                    response.setSuccessCount(response.getSuccessCount() + 1);
                } else {
                    response.setFailedCount(response.getFailedCount() + 1);
                }
            } catch (Exception e) {
                log.error("批量向量化失败", e);
                EmbeddingResponse failedResult = EmbeddingResponse.failed(
                    UUID.randomUUID().toString(),
                    e.getMessage()
                );
                results.add(failedResult);
                response.setFailedCount(response.getFailedCount() + 1);
            }
        }

        response.setResults(results);

        log.info("批量文本向量化完成，成功: {}, 失败: {}", 
                response.getSuccessCount(), response.getFailedCount());
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

    /**
     * 生成模拟向量（实际应调用AI模型）
     */
    private List<Float> generateMockEmbedding(int dimension) {
        Random random = new Random();
        List<Float> embedding = new ArrayList<>(dimension);
        for (int i = 0; i < dimension; i++) {
            embedding.add(random.nextFloat() * 2 - 1); // -1到1之间的随机数
        }
        return embedding;
    }
}
