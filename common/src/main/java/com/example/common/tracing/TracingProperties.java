package com.example.common.tracing;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 追踪配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "tracing")
public class TracingProperties {

    /**
     * 是否启用追踪
     */
    private boolean enabled = true;

    /**
     * 采样配置
     */
    private SamplingConfig sampling = new SamplingConfig();

    /**
     * Zipkin配置
     */
    private ZipkinConfig zipkin = new ZipkinConfig();

    /**
     * Jaeger配置
     */
    private JaegerConfig jaeger = new JaegerConfig();

    /**
     * 自定义标签
     */
    private Map<String, String> customTags = new HashMap<>();

    /**
     * 需要追踪的包路径
     */
    private List<String> basePackages = new ArrayList<>();

    @Data
    public static class SamplingConfig {
        /**
         * 采样概率 (0.0-1.0)
         */
        private double probability = 0.1;

        /**
         * 是否对错误请求强制采样
         */
        private boolean sampleErrors = true;

        /**
         * 慢请求阈值（毫秒），超过此阈值的请求强制采样
         */
        private long slowRequestThreshold = 1000;
    }

    @Data
    public static class ZipkinConfig {
        /**
         * Zipkin服务地址
         */
        private String endpoint = "http://localhost:9411/api/v2/spans";

        /**
         * 连接超时时间（毫秒）
         */
        private int connectTimeout = 5000;

        /**
         * 读取超时时间（毫秒）
         */
        private int readTimeout = 10000;
    }

    @Data
    public static class JaegerConfig {
        /**
         * Jaeger服务地址
         */
        private String endpoint = "http://localhost:14268/api/traces";

        /**
         * 是否使用UDP传输
         */
        private boolean useUdp = false;

        /**
         * UDP主机
         */
        private String udpHost = "localhost";

        /**
         * UDP端口
         */
        private int udpPort = 6831;
    }
}
