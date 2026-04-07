package com.example.embedding.model;

import lombok.Getter;

/**
 * 编码器模型类型枚举
 * 
 * @author AI Knowledge Base Team
 * @since 1.0.0
 */
@Getter
public enum EmbeddingModelType {
    
    // OpenAI 编码器
    OPENAI_TEXT_EMBEDDING_3_SMALL("text-embedding-3-small", "OpenAI", 1536, Speed.FAST, Quality.MEDIUM, Cost.LOW),
    OPENAI_TEXT_EMBEDDING_3_LARGE("text-embedding-3-large", "OpenAI", 3072, Speed.MEDIUM, Quality.HIGH, Cost.MEDIUM),
    OPENAI_TEXT_EMBEDDING_ADA_002("text-embedding-ada-002", "OpenAI", 1536, Speed.MEDIUM, Quality.MEDIUM, Cost.MEDIUM),
    
    // 本地模型 (通过 Ollama)
    BGE_LARGE_ZH("bge-large-zh", "Ollama", 1024, Speed.FAST, Quality.HIGH, Cost.FREE),
    BGE_SMALL_ZH("bge-small-zh", "Ollama", 512, Speed.FASTEST, Quality.MEDIUM, Cost.FREE),
    M3E_BASE("m3e-base", "Ollama", 768, Speed.FASTEST, Quality.MEDIUM, Cost.FREE),
    M3E_LARGE("m3e-large", "Ollama", 1024, Speed.FAST, Quality.HIGH, Cost.FREE),
    
    // 阿里云编码器
    ALIBABA_TEXT_EMBEDDING_V3("text-embedding-v3", "Alibaba", 1024, Speed.FAST, Quality.HIGH, Cost.LOW),
    
    // 百度编码器
    BAIDU_EMBEDDING_V1("Embedding-V1", "Baidu", 1024, Speed.MEDIUM, Quality.HIGH, Cost.MEDIUM);
    
    private final String modelName;
    private final String provider;
    private final int dimension;
    private final Speed speed;
    private final Quality quality;
    private final Cost cost;
    
    EmbeddingModelType(String modelName, String provider, int dimension, Speed speed, Quality quality, Cost cost) {
        this.modelName = modelName;
        this.provider = provider;
        this.dimension = dimension;
        this.speed = speed;
        this.quality = quality;
        this.cost = cost;
    }
    
    /**
     * 速度等级
     */
    public enum Speed {
        FASTEST("最快", 5),
        FAST("快", 4),
        MEDIUM("中", 3),
        SLOW("慢", 2);
        
        private final String description;
        private final int score;
        
        Speed(String description, int score) {
            this.description = description;
            this.score = score;
        }
        
        public String getDescription() {
            return description;
        }
        
        public int getScore() {
            return score;
        }
    }
    
    /**
     * 质量等级
     */
    public enum Quality {
        HIGH("高", 5),
        MEDIUM("中", 3),
        LOW("低", 1);
        
        private final String description;
        private final int score;
        
        Quality(String description, int score) {
            this.description = description;
            this.score = score;
        }
        
        public String getDescription() {
            return description;
        }
        
        public int getScore() {
            return score;
        }
    }
    
    /**
     * 成本等级
     */
    public enum Cost {
        FREE("免费", 0),
        LOW("低", 1),
        MEDIUM("中", 2),
        HIGH("高", 3);
        
        private final String description;
        private final int score;
        
        Cost(String description, int score) {
            this.description = description;
            this.score = score;
        }
        
        public String getDescription() {
            return description;
        }
        
        public int getScore() {
            return score;
        }
    }
    
    /**
     * 根据模型名称获取枚举
     */
    public static EmbeddingModelType fromModelName(String modelName) {
        for (EmbeddingModelType type : values()) {
            if (type.getModelName().equalsIgnoreCase(modelName)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown embedding model: " + modelName);
    }
    
    /**
     * 是否为本地模型
     */
    public boolean isLocal() {
        return "Ollama".equals(provider);
    }
    
    /**
     * 是否需要API Key
     */
    public boolean requiresApiKey() {
        return !isLocal();
    }
}
