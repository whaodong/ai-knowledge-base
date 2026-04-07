# 分布式追踪系统

## 概述

本项目集成了Micrometer Tracing，支持Zipkin和Jaeger作为追踪后端，提供完整的分布式追踪能力。

## 架构

```
┌─────────────────┐
│  应用服务       │
│  (Spring Boot)  │
└────────┬────────┘
         │
         │ Micrometer Tracing
         ▼
┌─────────────────┐
│  Tracer         │
│  (Brave Bridge) │
└────────┬────────┘
         │
         ├─────► Zipkin
         │
         └─────► Jaeger
```

## 功能特性

### 1. 自动追踪

- **Service层**：自动追踪所有Service层方法
- **Repository层**：自动追踪数据库访问
- **自定义注解**：支持@WithSpan注解

### 2. AI服务追踪

#### 文档处理链路
```java
文档上传 → 解析 → 分割 → 向量化 → 入库
```

#### RAG查询链路
```java
问题 → 检索(向量+关键词) → 重排序 → 阈值过滤 → 上下文融合 → 返回
```

### 3. Trace分析

- 慢请求分析（P99延迟）
- 错误链路追踪
- 服务依赖图生成
- 性能瓶颈定位

## 配置

### 1. 依赖配置

已在父POM和common模块中添加：

```xml
<!-- Micrometer Tracing -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing</artifactId>
</dependency>

<!-- Zipkin Bridge -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-brave</artifactId>
</dependency>
```

### 2. 应用配置

在`application.yml`中添加：

```yaml
spring:
  profiles:
    active: tracing

management:
  tracing:
    enabled: true
    sampling:
      probability: 0.1  # 采样率10%
  
  zipkin:
    tracing:
      endpoint: http://localhost:9411/api/v2/spans
```

### 3. 环境配置

#### 开发环境（100%采样）
```yaml
tracing:
  sampling:
    probability: 1.0
```

#### 生产环境（10%采样）
```yaml
tracing:
  sampling:
    probability: 0.1
    sample-errors: true  # 错误请求强制采样
    slow-request-threshold: 1000  # 慢请求强制采样
```

## 使用示例

### 1. 自动追踪

Service层方法会自动添加追踪：

```java
@Service
public class RagRetrievalService {
    public RagResponse retrieve(RagRequest request) {
        // 自动创建Span: RagRetrievalService.retrieve
        // ...
    }
}
```

### 2. 手动追踪

使用`TracingContext`工具类：

```java
@Autowired
private TracingContext tracingContext;

public void myMethod() {
    tracingContext.withSpan("custom.operation", () -> {
        // 业务逻辑
        tracingContext.addTag("custom.tag", "value");
    });
}
```

### 3. RAG服务追踪

```java
@Autowired
private RagTracingService ragTracingService;

public RagResponse query(String question) {
    RagRequest request = new RagRequest();
    request.setQuery(question);
    request.setTopK(10);
    
    return ragTracingService.retrieveWithTracing(request);
}
```

### 4. 文档处理追踪

```java
@Autowired
private DocumentTracingService documentTracingService;

// 上传文档
Document doc = documentTracingService.uploadDocumentWithTracing(file, request);

// 处理文档
documentTracingService.processDocumentWithTracing(doc.getId(), "parse");
documentTracingService.processDocumentWithTracing(doc.getId(), "split");
documentTracingService.processDocumentWithTracing(doc.getId(), "embed");
documentTracingService.processDocumentWithTracing(doc.getId(), "store");
```

## API接口

### 追踪分析接口

| 接口 | 说明 |
|------|------|
| GET /api/v1/tracing/stats | 获取追踪统计信息 |
| GET /api/v1/tracing/stats/p99 | 获取P99延迟统计 |
| GET /api/v1/tracing/slow-traces | 获取慢请求列表 |
| GET /api/v1/tracing/error-traces | 获取错误追踪列表 |
| GET /api/v1/tracing/dependencies | 获取服务依赖关系 |
| GET /api/v1/tracing/bottlenecks | 分析性能瓶颈 |
| GET /api/v1/tracing/current-trace | 获取当前追踪信息 |

### 响应示例

```bash
# 获取P99延迟
curl http://localhost:8080/api/v1/tracing/stats/p99

{
  "code": 200,
  "message": "success",
  "data": {
    "RagRetrievalService.retrieve": 1234,
    "DocumentServiceImpl.uploadDocument": 567,
    "VectorRetriever.retrieve": 234
  }
}

# 分析性能瓶颈
curl http://localhost:8080/api/v1/tracing/bottlenecks

{
  "code": 200,
  "message": "success",
  "data": [
    {
      "spanName": "RagRetrievalService.retrieve",
      "avgDurationMs": 850,
      "p99DurationMs": 1234,
      "callCount": 1024,
      "errorRate": 0.02
    }
  ]
}
```

## 部署

### 1. 启动Zipkin

```bash
cd monitoring/tracing
docker-compose up -d zipkin
```

访问：http://localhost:9411

### 2. 启动Jaeger（备选）

```bash
cd monitoring/tracing
docker-compose up -d jaeger
```

访问：http://localhost:16686

### 3. 启动应用

```bash
mvn spring-boot:run -Dspring.profiles.active=tracing
```

## 日志集成

### 日志格式

日志中自动包含Trace ID和Span ID：

```
2024-04-07 12:34:56.789 [http-nio-8080-exec-1] [abc123def456,xyz789ghi012] INFO  c.e.rag.service.RagRetrievalService - 开始RAG检索
```

### 日志配置

```yaml
logging:
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] [%X{traceId:-},%X{spanId:-}] %-5level %logger{36} - %msg%n"
```

### Trace ID传递

- **请求头**：`X-Trace-Id`, `X-Span-Id`
- **响应头**：自动添加Trace ID到响应头
- **前端**：可通过响应头获取Trace ID用于问题排查

## 监控集成

### Prometheus指标关联

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus,traces
```

### 告警集成

在`alert.rules.yml`中添加追踪相关告警：

```yaml
- alert: HighErrorRate
  expr: rate(http_server_requests_seconds_count{status=~"5.."}[5m]) > 0.1
  for: 5m
  annotations:
    summary: "高错误率告警"
    description: "服务 {{ $labels.service }} 错误率超过10%，请检查追踪信息"
```

## 最佳实践

### 1. 采样策略

- **开发环境**：100%采样，便于调试
- **测试环境**：100%采样，便于问题定位
- **生产环境**：10%采样，降低性能开销
- **错误请求**：强制采样
- **慢请求**：强制采样

### 2. Span命名

- 使用清晰、有意义的名称
- 包含服务名和操作名
- 示例：`rag.query`, `document.upload`, `vector.retrieval`

### 3. 标签使用

```java
// 好的做法
span.tag("user.id", userId);
span.tag("query.length", String.valueOf(query.length()));
span.tag("result.count", String.valueOf(results.size()));

// 避免敏感信息
// span.tag("password", password); // ❌
```

### 4. 性能考虑

- 避免在循环中创建Span
- 异步操作需要手动传递Trace上下文
- 大量数据操作考虑采样

## 故障排查

### 1. Trace ID未显示

检查：
- 追踪是否启用：`tracing.enabled=true`
- 日志格式是否正确配置
- Tracer是否正确注入

### 2. 追踪数据未上报

检查：
- Zipkin/Jaeger服务是否运行
- 网络连接是否正常
- 采样率是否过低

### 3. 性能影响

如果发现性能问题：
- 降低采样率
- 过滤不必要的Span
- 检查Span标签是否过多

## 参考资料

- [Micrometer Tracing文档](https://micrometer.io/docs/tracing)
- [Zipkin文档](https://zipkin.io/)
- [Jaeger文档](https://www.jaegertracing.io/)
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html)
