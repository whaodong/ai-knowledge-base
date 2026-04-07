# 分布式追踪快速开始指南

## 1. 环境准备

### 启动Zipkin

```bash
cd monitoring/tracing
docker-compose up -d zipkin
```

验证Zipkin运行：
```bash
curl http://localhost:9411/health
```

## 2. 配置应用

### 2.1 添加配置

在`application.yml`中添加：

```yaml
spring:
  profiles:
    include: tracing
```

或直接添加追踪配置：

```yaml
tracing:
  enabled: true
  sampling:
    probability: 1.0  # 开发环境使用100%采样
  zipkin:
    endpoint: http://localhost:9411/api/v2/spans

management:
  tracing:
    enabled: true
```

### 2.2 启动应用

```bash
mvn spring-boot:run
```

## 3. 生成追踪数据

### 3.1 调用API

```bash
# RAG查询
curl -X POST http://localhost:8080/api/v1/rag/retrieve \
  -H "Content-Type: application/json" \
  -d '{
    "query": "什么是人工智能?",
    "topK": 5
  }'

# 获取追踪ID
curl http://localhost:8080/api/v1/tracing/current-trace
```

### 3.2 查看追踪

访问Zipkin UI：http://localhost:9411

## 4. 查看分析结果

```bash
# 获取统计信息
curl http://localhost:8080/api/v1/tracing/stats

# 获取P99延迟
curl http://localhost:8080/api/v1/tracing/stats/p99

# 查看慢请求
curl http://localhost:8080/api/v1/tracing/slow-traces?limit=10

# 分析性能瓶颈
curl http://localhost:8080/api/v1/tracing/bottlenecks
```

## 5. 在代码中使用

### 5.1 自动追踪

Service层方法会自动追踪：

```java
@Service
public class MyService {
    public void myMethod() {
        // 自动创建Span: MyService.myMethod
    }
}
```

### 5.2 手动追踪

```java
@Autowired
private TracingContext tracingContext;

public void customOperation() {
    tracingContext.withSpan("custom.operation", () -> {
        // 业务逻辑
        tracingContext.addTag("key", "value");
    });
}
```

### 5.3 RAG服务追踪

```java
@Autowired
private RagTracingService ragTracingService;

public RagResponse query(String question) {
    RagRequest request = new RagRequest();
    request.setQuery(question);
    return ragTracingService.retrieveWithTracing(request);
}
```

## 6. 常见问题

### Q: Trace ID在哪里？
A: 
- 查看响应头：`X-Trace-Id`
- 查看日志：日志中包含`[traceId,spanId]`
- 调用API：`/api/v1/tracing/current-trace`

### Q: 为什么看不到追踪数据？
A: 
1. 检查Zipkin是否运行
2. 检查采样率配置
3. 查看日志是否有错误

### Q: 如何在生产环境使用？
A: 
```yaml
tracing:
  sampling:
    probability: 0.1  # 10%采样
    sample-errors: true  # 错误强制采样
    slow-request-threshold: 1000  # 慢请求强制采样
```

## 7. 下一步

- 查看[完整文档](./tracing-guide.md)
- 了解[最佳实践](./tracing-guide.md#最佳实践)
- 配置[告警集成](./tracing-guide.md#监控集成)
