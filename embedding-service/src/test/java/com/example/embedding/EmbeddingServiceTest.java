package com.example.embedding;

import com.example.embedding.config.EmbeddingProperties;
import com.example.embedding.model.EmbeddingModelType;
import com.example.embedding.model.EmbeddingResult;
import com.example.embedding.model.EmbeddingStrategy;
import com.example.embedding.provider.MockEmbeddingProvider;
import com.example.embedding.registry.EmbeddingModelRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 多编码器功能测试
 * 
 * @author AI Knowledge Base Team
 * @since 1.0.0
 */
@SpringBootTest
class EmbeddingServiceTest {

    @Autowired
    private EmbeddingModelRegistry registry;
    
    @Autowired
    private EmbeddingProperties properties;
    
    @Autowired
    private MockEmbeddingProvider mockProvider;

    @Test
    void testAvailableModels() {
        List<EmbeddingModelType> models = registry.getAvailableModels();
        assertNotNull(models);
        assertFalse(models.isEmpty());
        System.out.println("Available models: " + models.size());
    }

    @Test
    void testModelSelection() {
        // 测试不同策略的模型选择
        EmbeddingModelType costFirst = registry.selectBestModel(EmbeddingStrategy.COST_FIRST);
        assertNotNull(costFirst);
        System.out.println("Cost first model: " + costFirst.getModelName());
        
        EmbeddingModelType qualityFirst = registry.selectBestModel(EmbeddingStrategy.QUALITY_FIRST);
        assertNotNull(qualityFirst);
        System.out.println("Quality first model: " + qualityFirst.getModelName());
        
        EmbeddingModelType speedFirst = registry.selectBestModel(EmbeddingStrategy.SPEED_FIRST);
        assertNotNull(speedFirst);
        System.out.println("Speed first model: " + speedFirst.getModelName());
    }

    @Test
    void testMockProvider() {
        String testText = "这是一个测试文本";
        
        EmbeddingResult result = mockProvider.embed(testText, EmbeddingModelType.OPENAI_TEXT_EMBEDDING_3_SMALL);
        
        assertNotNull(result);
        assertTrue(result.getSuccess());
        assertNotNull(result.getEmbedding());
        assertEquals(1536, result.getEmbedding().size());
        System.out.println("Mock embedding dimension: " + result.getDimension());
    }

    @Test
    void testBatchEmbedding() {
        List<String> texts = Arrays.asList(
            "文本1",
            "文本2",
            "文本3"
        );
        
        List<EmbeddingResult> results = mockProvider.embedBatch(texts, EmbeddingModelType.BGE_LARGE_ZH);
        
        assertNotNull(results);
        assertEquals(3, results.size());
        results.forEach(r -> assertTrue(r.getSuccess()));
        System.out.println("Batch embedding completed: " + results.size() + " results");
    }

    @Test
    void testFallback() {
        String testText = "测试降级功能";
        
        EmbeddingResult result = registry.embedWithFallback(testText, EmbeddingModelType.OPENAI_TEXT_EMBEDDING_3_SMALL);
        
        assertNotNull(result);
        // Mock provider should always succeed
        assertTrue(result.getSuccess());
        System.out.println("Fallback test result: " + result.getSuccess());
    }

    @Test
    void testProviderStatus() {
        var status = registry.getProviderStatus();
        
        assertNotNull(status);
        assertFalse(status.isEmpty());
        status.forEach((provider, healthy) -> 
            System.out.println("Provider: " + provider + ", Healthy: " + healthy));
    }

    @Test
    void testConfiguration() {
        assertNotNull(properties);
        assertEquals(EmbeddingStrategy.FIXED, properties.getStrategy());
        assertNotNull(properties.getBatch());
        assertTrue(properties.getBatch().getEnabled());
        assertNotNull(properties.getCache());
        assertTrue(properties.getCache().getEnabled());
        System.out.println("Configuration loaded successfully");
    }
}
