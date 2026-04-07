package com.example.eurekaserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

/**
 * Eureka 服务注册中心
 * 
 * <p>Spring Cloud Netflix Eureka Server 实现，提供服务注册与发现功能。
 * 默认端口：8761，可通过 http://localhost:8761 访问控制台。</p>
 * 
 * <p>企业级配置说明：</p>
 * <ul>
 *   <li>关闭自注册：eureka.client.register-with-eureka=false</li>
 *   <li>关闭获取注册表：eureka.client.fetch-registry=false</li>
 *   <li>开启健康检查：eureka.client.healthcheck.enabled=true</li>
 *   <li>配置服务续约时间：eureka.instance.lease-renewal-interval-in-seconds=30</li>
 *   <li>配置服务过期时间：eureka.instance.lease-expiration-duration-in-seconds=90</li>
 * </ul>
 * 
 * @author AI Knowledge Base Team
 * @since 1.0.0
 */
@SpringBootApplication
@EnableEurekaServer
public class EurekaServerApplication {

    /**
     * 主启动方法
     * 
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(EurekaServerApplication.class, args);
    }
}