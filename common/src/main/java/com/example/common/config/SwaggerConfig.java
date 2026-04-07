package com.example.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

/**
 * Swagger配置
 */
@Configuration
public class SwaggerConfig {

    @Value("${spring.application.name:AI Knowledge Base}")
    private String applicationName;

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .servers(Arrays.asList(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("开发环境"),
                        new Server()
                                .url("https://api.knowledge-base.example.com")
                                .description("生产环境")
                ));
    }

    private Info apiInfo() {
        return new Info()
                .title(applicationName + " API")
                .description("企业级AI知识库系统RESTful API文档")
                .version("v1.0.0")
                .contact(new Contact()
                        .name("AI Knowledge Base Team")
                        .email("support@knowledge-base.example.com")
                        .url("https://knowledge-base.example.com"))
                .license(new License()
                        .name("Apache 2.0")
                        .url("https://www.apache.org/licenses/LICENSE-2.0"));
    }
}
