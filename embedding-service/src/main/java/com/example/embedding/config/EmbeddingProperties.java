package com.example.embedding.config;

import com.example.embedding.model.EmbeddingModelType;
import com.example.embedding.model.EmbeddingStrategy;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 编码器配置属性
 * 
 * @author AI Knowledge Base Team
 * @since 1.0.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "embedding")
public class EmbeddingProperties {
    
    /**
     * 当前使用的编码器
     */
    private EmbeddingModelType currentModel = EmbeddingModelType.OPENAI_TEXT_EMBEDDING_3_SMALL;
    
    /**
     * 编码器选择策略
     */
    private EmbeddingStrategy strategy = EmbeddingStrategy.FIXED;
    
    /**
     * 备用编码器列表（用于降级）
     */
    private List<EmbeddingModelType> fallbackModels = new ArrayList<>();
    
    /**
     * 批量编码配置
     */
    private BatchConfig batch = new BatchConfig();
    
    /**
     * 缓存配置
     */
    private CacheConfig cache = new CacheConfig();
    
    /**
     * OpenAI 配置
     */
    private OpenAIConfig openai = new OpenAIConfig();
    
    /**
     * Ollama 本地模型配置
     */
    private OllamaConfig ollama = new OllamaConfig();
    
    /**
     * 阿里云配置
     */
    private AlibabaConfig alibaba = new AlibabaConfig();
    
    /**
     * 百度配置
     */
    private BaiduConfig baidu = new BaiduConfig();
    
    /**
     * 启用的编码器列表
     */
    private List<EmbeddingModelType> enabledModels = new ArrayList<>();
    
    @Data
    public static class BatchConfig {
        /**
         * 是否启用批量编码
         */
        private Boolean enabled = true;
        
        /**
         * 默认批大小
         */
        private Integer defaultBatchSize = 20;
        
        /**
         * 最大批大小
         */
        private Integer maxBatchSize = 100;
        
        /**
         * 并行线程数
         */
        private Integer parallelThreads = 4;
        
        /**
         * 重试次数
         */
        private Integer retryTimes = 3;
        
        /**
         * 重试间隔（毫秒）
         */
        private Long retryInterval = 1000L;
        
        /**
         * 超时时间（毫秒）
         */
        private Long timeout = 30000L;
    }
    
    @Data
    public static class CacheConfig {
        /**
         * 是否启用缓存
         */
        private Boolean enabled = true;
        
        /**
         * 缓存过期时间（秒）
         */
        private Long expireSeconds = 86400L; // 24小时
        
        /**
         * 缓存键前缀
         */
        private String keyPrefix = "embedding:cache:";
        
        /**
         * 最大缓存数量
         */
        private Long maxSize = 100000L;
        
        /**
         * 是否统计命中率
         */
        private Boolean trackHitRate = true;
    }
    
    @Data
    public static class OpenAIConfig {
        private String apiKey;
        private String baseUrl = "https://api.openai.com/v1";
        private Integer timeout = 30000;
    }
    
    @Data
    public static class OllamaConfig {
        private Boolean enabled = false;
        private String baseUrl = "http://localhost:11434";
        private Integer timeout = 60000;
    }
    
    @Data
    public static class AlibabaConfig {
        private Boolean enabled = false;
        private String apiKey;
        private String region = "cn-shanghai";
    }
    
    @Data
    public static class BaiduConfig {
        private Boolean enabled = false;
        private String apiKey;
        private String secretKey;
    }
}
