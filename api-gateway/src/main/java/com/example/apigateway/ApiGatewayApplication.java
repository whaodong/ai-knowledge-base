package com.example.apigateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * API网关服务 - 基于Spring Cloud Gateway
 * 
 * 提供统一的企业级入口，包含路由转发、过滤器链、安全认证等功能
 * 支持动态路由配置、负载均衡、熔断限流等高级特性
 */
@SpringBootApplication
@EnableDiscoveryClient
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}