package com.example.document.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 文档管理服务控制器
 * 
 * <p>提供文档管理相关的REST接口。</p>
 * 
 * @author AI Knowledge Base Team
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/document")
public class DocumentController {

    @Value("${spring.application.name}")
    private String appName;
    
    @Value("${server.port}")
    private String port;
    
    /**
     * 健康检查接口
     * 
     * @return 服务状态信息
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "UP");
        result.put("service", appName);
        result.put("port", port);
        result.put("timestamp", System.currentTimeMillis());
        return result;
    }
    
    /**
     * 服务信息接口
     * 
     * @return 服务信息
     */
    @GetMapping("/info")
    public Map<String, Object> getServiceInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("serviceName", appName);
        info.put("instanceId", appName + ":" + port);
        info.put("status", "ACTIVE");
        info.put("description", "文档管理服务，提供文档上传、解析、存储能力");
        info.put("timestamp", System.currentTimeMillis());
        return info;
    }
}