package com.example.embedding.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.web.client.RestTemplate;

/**
 * 编码器配置类
 * 
 * @author AI Knowledge Base Team
 * @since 1.0.0
 */
@Configuration
@EnableAsync
public class EmbeddingConfig {
    
    /**
     * RestTemplate Bean
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
