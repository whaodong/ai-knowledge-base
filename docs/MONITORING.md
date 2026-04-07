# 监控配置说明 (Monitoring Guide)

## 目录
- [监控架构](#监控架构)
- [Prometheus配置](#prometheus配置)
- [Grafana配置](#grafana配置)
- [自定义指标](#自定义指标)
- [健康检查](#健康检查)
- [告警配置](#告警配置)

## 监控架构

本项目采用 **Prometheus + Grafana** 的监控方案：

```
┌─────────────────────────────────────────────────────────────┐
│                      监控架构图                              │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────┐ │
│  │ Spring Boot  │───▶│  Prometheus  │───▶│   Grafana    │ │
│  │  Actuator    │    │   (存储+查询) │    │  (可视化)     │ │
│  └──────────────┘    └──────────────┘    └──────────────┘ │
│         │                   │                              │
│         │                   ▼                              │
│         │          ┌──────────────┐                        │
│         │          │Alertmanager  │                        │
│         │          │  (告警通知)   │                        │
│         │          └──────────────┘                        │
│         │                                                  │
│         ▼                                                  │
│  ┌──────────────┐                                         │
│  │  Micrometer  │                                         │
│  │  (指标收集)   │                                         │
│  └──────────────┘                                         │
└─────────────────────────────────────────────────────────────┘
```

### 组件说明

| 组件 | 版本 | 用途 |
|------|------|------|
| Spring Boot Actuator | 3.3.0 | 应用健康检查、指标暴露 |
| Micrometer | 1.12+ | 指标收集和导出 |
| Prometheus | 2.45.0 | 时序数据库、指标查询 |
| Grafana | 10.0.0 | 数据可视化、Dashboard |

## Prometheus配置

### 启动Prometheus

```bash
cd monitoring
docker-compose up -d prometheus
```

### 配置文件说明

Prometheus配置文件位于 `monitoring/prometheus.yml`：

```yaml
global:
  scrape_interval: 15s        # 抓取间隔
  evaluation_interval: 15s    # 规则评估间隔
  external_labels:
    monitor: 'ai-knowledge-base'

scrape_configs:
  # Spring Boot服务监控
  - job_name: 'rag-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['rag-service:8083']
```

### 服务发现配置

对于动态环境，可以使用服务发现：

```yaml
scrape_configs:
  # Eureka服务发现
  - job_name: 'spring-boot-services'
    metrics_path: '/actuator/prometheus'
    eureka_sd_configs:
      - server: 'http://eureka-server:8761/eureka'
```

### 访问Prometheus

- Web UI: http://localhost:9090
- 查询示例:
  - `rag_query_count_total` - RAG查询总数
  - `rag_query_latency_seconds` - RAG查询延迟
  - `jvm_memory_used_bytes` - JVM内存使用

## Grafana配置

### 启动Grafana

```bash
cd monitoring
docker-compose up -d grafana
```

### 访问Grafana

- URL: http://localhost:3000
- 默认用户名: `admin`
- 默认密码: `admin`

### 添加数据源

1. 登录Grafana
2. 进入 Configuration > Data Sources
3. 添加 Prometheus 数据源
4. 配置URL: `http://prometheus:9090`
5. 点击 "Save & Test"

### 导入Dashboard

已预配置的Dashboard位于 `monitoring/grafana/dashboard.json`

#### 手动导入步骤：
1. 进入 Dashboards > Import
2. 上传 `dashboard.json` 文件
3. 选择 Prometheus 数据源
4. 点击 Import

### Dashboard功能

预配置的Dashboard包含以下面板：

1. **RAG查询延迟** - P95、P99延迟指标
2. **查询速率** - 每分钟查询次数
3. **Milvus延迟** - 向量搜索延迟
4. **活跃查询数** - 当前处理的查询数
5. **Token使用量** - 累计Token消耗
6. **缓存命中率** - Redis缓存效率
7. **系统资源** - CPU、内存使用情况
8. **服务健康状态** - 所有服务状态

## 自定义指标

### RAG服务指标

#### 1. RAG查询计数
```java
// 指标名称: rag_query_count
// 类型: Counter
// 说明: 累计RAG查询次数
rag_query_count_total{service="rag-service"} 1234
```

#### 2. RAG查询延迟
```java
// 指标名称: rag_query_latency
// 类型: Timer (Histogram)
// 说明: RAG查询响应时间
rag_query_latency_seconds{quantile="0.95"} 0.125
rag_query_latency_seconds{quantile="0.99"} 0.250
```

#### 3. Token使用量
```java
// 指标名称: embedding_tokens
// 类型: Counter
// 说明: Embedding生成的Token总数
embedding_tokens_total{service="rag-service"} 50000
```

#### 4. Milvus延迟
```java
// 指标名称: milvus_latency
// 类型: Timer (Histogram)
// 说明: Milvus向量查询延迟
milvus_latency_seconds{quantile="0.95"} 0.050
```

### 使用方式

#### 1. 注解方式
```java
@Timed(value = "rag.query.time", description = "RAG query time")
public RagResponse query(RagRequest request) {
    // 业务逻辑
}
```

#### 2. 编程方式
```java
@Autowired
private RagMetricsConfig metricsConfig;

public void processQuery() {
    metricsConfig.getRagQueryCount().increment();
    
    Timer.Sample sample = Timer.start();
    // 执行查询
    sample.stop(metricsConfig.getRagQueryLatency());
}
```

#### 3. AOP方式
```java
@Around("execution(* com.example.rag.service.*.*(..))")
public Object monitor(ProceedingJoinPoint pjp) {
    Timer.Sample sample = Timer.start();
    try {
        return pjp.proceed();
    } finally {
        sample.stop(meterRegistry.timer("method.duration"));
    }
}
```

## 健康检查

### Actuator端点

所有服务都配置了Spring Boot Actuator，提供以下端点：

| 端点 | 用途 | 访问方式 |
|------|------|----------|
| `/actuator/health` | 健康检查 | `GET` |
| `/actuator/info` | 应用信息 | `GET` |
| `/actuator/metrics` | 指标列表 | `GET` |
| `/actuator/prometheus` | Prometheus格式指标 | `GET` |
| `/actuator/circuitbreakers` | 熔断器状态 | `GET` |

### 自定义健康检查

#### 1. Milvus健康检查
```java
@Component
public class MilvusHealthIndicator implements HealthIndicator {
    @Override
    public Health health() {
        // 检查Milvus连接和状态
        return Health.up()
            .withDetail("status", "healthy")
            .build();
    }
}
```

#### 2. Redis健康检查
```java
@Component
public class RedisHealthIndicator implements HealthIndicator {
    @Override
    public Health health() {
        // 检查Redis连接和读写
        return Health.up()
            .withDetail("version", "7.0")
            .withDetail("connected_clients", "10")
            .build();
    }
}
```

### Kubernetes探针

配置了Kubernetes就绪和存活探针：

```yaml
# 就绪探针
readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8083
  initialDelaySeconds: 60
  periodSeconds: 10

# 存活探针
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8083
  initialDelaySeconds: 60
  periodSeconds: 10
```

### 探针配置

```yaml
management:
  endpoint:
    health:
      probes:
        enabled: true
      group:
        liveness:
          include: livenessState
        readiness:
          include: readinessState,milvus,redis
```

## 告警配置

### Prometheus告警规则

创建 `monitoring/alert.rules.yml`：

```yaml
groups:
  - name: ai-knowledge-base-alerts
    rules:
      # 服务宕机告警
      - alert: ServiceDown
        expr: up == 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "Service {{ $labels.job }} is down"
          description: "{{ $labels.instance }} has been down for more than 1 minute."

      # 高延迟告警
      - alert: HighLatency
        expr: histogram_quantile(0.95, rate(rag_query_latency_seconds_bucket[5m])) > 1
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High RAG query latency"
          description: "95th percentile latency is above 1s for 5 minutes."

      # 内存使用告警
      - alert: HighMemoryUsage
        expr: jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} > 0.85
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "High JVM heap usage"
          description: "Heap usage is above 85% for 5 minutes."

      # 错误率告警
      - alert: HighErrorRate
        expr: rate(http_server_requests_seconds_count{status=~"5.."}[5m]) > 0.05
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "High error rate"
          description: "Error rate is above 5% for 5 minutes."

      # Milvus不可用告警
      - alert: MilvusUnavailable
        expr: milvus_health_status == 0
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "Milvus is unavailable"
          description: "Milvus has been unavailable for more than 2 minutes."
```

### Alertmanager配置

创建 `monitoring/alertmanager.yml`：

```yaml
global:
  resolve_timeout: 5m
  smtp_smarthost: 'smtp.example.com:587'
  smtp_from: 'alertmanager@example.com'
  smtp_auth_username: 'alertmanager@example.com'
  smtp_auth_password: 'password'

route:
  group_by: ['alertname', 'severity']
  group_wait: 10s
  group_interval: 10s
  repeat_interval: 12h
  receiver: 'team-email'
  
  routes:
    - match:
        severity: critical
      receiver: 'team-email'
    - match:
        severity: warning
      receiver: 'team-email'

receivers:
  - name: 'team-email'
    email_configs:
      - to: 'team@example.com'
        send_resolved: true

  - name: 'team-slack'
    slack_configs:
      - api_url: 'https://hooks.slack.com/services/YOUR/SLACK/WEBHOOK'
        channel: '#alerts'
        send_resolved: true
```

### 启动Alertmanager

```bash
docker-compose up -d alertmanager
```

### 访问Alertmanager

- Web UI: http://localhost:9093

## 监控最佳实践

### 1. 指标命名规范
- 使用小写和下划线：`rag_query_count`
- 包含单位：`rag_query_latency_seconds`
- 添加标签：`service`, `instance`, `method`

### 2. 标签使用
```java
Counter.builder("requests_total")
    .tag("service", "rag-service")
    .tag("method", "query")
    .tag("status", "success")
    .register(meterRegistry);
```

### 3. 直方图配置
```java
Timer.builder("rag.query.latency")
    .publishPercentiles(0.5, 0.95, 0.99)
    .publishPercentileHistogram()
    .minimumExpectedValue(Duration.ofMillis(1))
    .maximumExpectedValue(Duration.ofSeconds(30))
    .register(meterRegistry);
```

### 4. 安全配置

限制Actuator端点访问：

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: when-authorized
```

### 5. 性能优化

- 使用 `rate()` 函数计算速率
- 避免高基数标签
- 合理设置抓取间隔
- 配置数据保留策略

## 常用查询

### 1. 查询速率
```promql
# 每秒请求数
rate(http_server_requests_seconds_count[5m])

# 每分钟RAG查询数
increase(rag_query_count_total[1m])
```

### 2. 延迟分析
```promql
# P95延迟
histogram_quantile(0.95, rate(rag_query_latency_seconds_bucket[5m]))

# 平均延迟
rate(rag_query_latency_seconds_sum[5m]) / rate(rag_query_latency_seconds_count[5m])
```

### 3. 错误率
```promql
# 错误请求占比
sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) 
  / sum(rate(http_server_requests_seconds_count[5m]))
```

### 4. 资源使用
```promql
# JVM堆内存使用率
jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"}

# CPU使用率
system_cpu_usage
```

## 监控面板截图

访问 http://localhost:3000 查看实时监控面板。

## 故障排查

### Prometheus无法抓取指标
1. 检查服务是否启动
2. 验证Actuator端点可访问
3. 检查网络连接
4. 查看Prometheus日志

### Grafana无数据
1. 验证Prometheus数据源配置
2. 检查时间范围
3. 确认查询语法正确
4. 查看浏览器控制台错误

### 指标丢失
1. 检查服务注册状态
2. 验证指标是否暴露
3. 确认Prometheus配置正确
4. 查看服务日志

## 相关文档

- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
- [Prometheus Documentation](https://prometheus.io/docs/)
- [Grafana Documentation](https://grafana.com/docs/)
- [Micrometer Documentation](https://micrometer.io/docs)
