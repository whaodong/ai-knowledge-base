package com.example.embedding.registry;

import com.example.embedding.config.EmbeddingProperties;
import com.example.embedding.model.EmbeddingModelType;
import com.example.embedding.model.EmbeddingResult;
import com.example.embedding.model.EmbeddingStrategy;
import com.example.embedding.provider.EmbeddingProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 编码器注册中心
 * 
 * <p>管理所有编码器提供者，提供编码器选择和切换功能</p>
 * 
 * @author AI Knowledge Base Team
 * @since 1.0.0
 */
@Slf4j
@Component
public class EmbeddingModelRegistry {
    
    @Autowired
    private EmbeddingProperties properties;
    
    @Autowired
    private List<EmbeddingProvider> providers;
    
    /**
     * 模型 -> 提供者映射
     */
    private final Map<EmbeddingModelType, EmbeddingProvider> modelProviderMap = new ConcurrentHashMap<>();
    
    /**
     * 提供者名称 -> 提供者映射
     */
    private final Map<String, EmbeddingProvider> providerMap = new ConcurrentHashMap<>();
    
    /**
     * 健康状态缓存
     */
    private final Map<EmbeddingModelType, Boolean> healthStatus = new ConcurrentHashMap<>();
    
    @PostConstruct
    public void init() {
        log.info("Initializing EmbeddingModelRegistry...");
        
        // 注册所有提供者
        for (EmbeddingProvider provider : providers) {
            registerProvider(provider);
        }
        
        log.info("Registered {} providers, {} models", 
            providerMap.size(), modelProviderMap.size());
    }
    
    /**
     * 注册提供者
     */
    public void registerProvider(EmbeddingProvider provider) {
        providerMap.put(provider.getName(), provider);
        
        for (EmbeddingModelType modelType : provider.getSupportedModels()) {
            if (provider.isEnabled()) {
                modelProviderMap.put(modelType, provider);
                log.info("Registered model: {} with provider: {}", 
                    modelType.getModelName(), provider.getName());
            }
        }
    }
    
    /**
     * 获取指定模型的提供者
     */
    public EmbeddingProvider getProvider(EmbeddingModelType modelType) {
        EmbeddingProvider provider = modelProviderMap.get(modelType);
        
        if (provider == null) {
            throw new IllegalStateException("No provider found for model: " + modelType.getModelName());
        }
        
        return provider;
    }
    
    /**
     * 根据策略选择最佳模型
     */
    public EmbeddingModelType selectBestModel(EmbeddingStrategy strategy) {
        List<EmbeddingModelType> availableModels = getAvailableModels();
        
        if (availableModels.isEmpty()) {
            throw new IllegalStateException("No available embedding models");
        }
        
        return switch (strategy) {
            case COST_FIRST -> selectByCost(availableModels);
            case QUALITY_FIRST -> selectByQuality(availableModels);
            case SPEED_FIRST -> selectBySpeed(availableModels);
            case BALANCED -> selectBalanced(availableModels);
            case FIXED -> properties.getCurrentModel();
        };
    }
    
    /**
     * 成本优先选择
     */
    private EmbeddingModelType selectByCost(List<EmbeddingModelType> models) {
        return models.stream()
            .min(Comparator.comparingInt(m -> m.getCost().getScore()))
            .orElse(models.get(0));
    }
    
    /**
     * 质量优先选择
     */
    private EmbeddingModelType selectByQuality(List<EmbeddingModelType> models) {
        return models.stream()
            .max(Comparator.comparingInt(m -> m.getQuality().getScore()))
            .orElse(models.get(0));
    }
    
    /**
     * 速度优先选择
     */
    private EmbeddingModelType selectBySpeed(List<EmbeddingModelType> models) {
        return models.stream()
            .max(Comparator.comparingInt(m -> m.getSpeed().getScore()))
            .orElse(models.get(0));
    }
    
    /**
     * 平衡选择
     */
    private EmbeddingModelType selectBalanced(List<EmbeddingModelType> models) {
        // 综合评分：质量 * 0.5 + 速度 * 0.3 - 成本 * 0.2
        return models.stream()
            .max(Comparator.comparingDouble(m -> 
                m.getQuality().getScore() * 0.5 
                + m.getSpeed().getScore() * 0.3 
                - m.getCost().getScore() * 0.2
            ))
            .orElse(models.get(0));
    }
    
    /**
     * 执行编码（支持自动降级）
     */
    public EmbeddingResult embedWithFallback(String text, EmbeddingModelType preferredModel) {
        // 尝试首选模型
        EmbeddingProvider provider = getProvider(preferredModel);
        EmbeddingResult result = provider.embed(text, preferredModel);
        
        if (result.getSuccess()) {
            return result;
        }
        
        // 首选模型失败，尝试备用模型
        log.warn("Primary model {} failed, trying fallback models", preferredModel.getModelName());
        
        List<EmbeddingModelType> fallbackModels = properties.getFallbackModels();
        if (fallbackModels != null && !fallbackModels.isEmpty()) {
            for (EmbeddingModelType fallbackModel : fallbackModels) {
                try {
                    provider = getProvider(fallbackModel);
                    result = provider.embed(text, fallbackModel);
                    
                    if (result.getSuccess()) {
                        log.info("Successfully used fallback model: {}", fallbackModel.getModelName());
                        return result;
                    }
                } catch (Exception e) {
                    log.error("Fallback model {} failed: {}", fallbackModel.getModelName(), e.getMessage());
                }
            }
        }
        
        // 所有模型都失败
        return EmbeddingResult.failure("All embedding models failed");
    }
    
    /**
     * 获取所有可用模型
     */
    public List<EmbeddingModelType> getAvailableModels() {
        return new ArrayList<>(modelProviderMap.keySet());
    }
    
    /**
     * 检查模型是否可用
     */
    public boolean isModelAvailable(EmbeddingModelType modelType) {
        return modelProviderMap.containsKey(modelType);
    }
    
    /**
     * 更新健康状态
     */
    public void updateHealthStatus(EmbeddingModelType modelType, boolean healthy) {
        healthStatus.put(modelType, healthy);
    }
    
    /**
     * 获取健康状态
     */
    public boolean isHealthy(EmbeddingModelType modelType) {
        return healthStatus.getOrDefault(modelType, true);
    }
    
    /**
     * 获取所有提供者状态
     */
    public Map<String, Boolean> getProviderStatus() {
        Map<String, Boolean> status = new HashMap<>();
        
        for (Map.Entry<String, EmbeddingProvider> entry : providerMap.entrySet()) {
            status.put(entry.getKey(), entry.getValue().healthCheck());
        }
        
        return status;
    }
}
