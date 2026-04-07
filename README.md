# AI Knowledge Base

[![CI/CD Pipeline](https://github.com/whaodong/ai-knowledge-base/workflows/CI%2FCD%20Pipeline/badge.svg)](https://github.com/whaodong/ai-knowledge-base/actions)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java Version](https://img.shields.io/badge/Java-21-orange)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.0-brightgreen)](https://spring.io/projects/spring-boot)

基于 Spring Boot 3.3 + Spring AI 1.0.0-M3 + Milvus 的智能知识库系统，支持 RAG（检索增强生成）架构。

## 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| JDK | 21 | OpenJDK 21.0.10 |
| Spring Boot | 3.3.0 | 基础框架 |
| Spring Cloud | 2023.0.1 | 微服务架构 |
| Spring AI | 1.0.0-M3 | AI 集成框架 |
| Milvus | 2.3+ | 向量数据库 |
| LangChain4j | 0.30.0 | LLM 应用框架 |

## 系统架构

```
                    ┌─────────────────┐
                    │   API Gateway   │
                    │     :8080       │
                    └────────┬────────┘
                             │
        ┌────────────────────┼────────────────────┐
        │                    │                    │
        ▼                    ▼                    ▼
┌───────────────┐   ┌───────────────┐   ┌───────────────┐
│   Document    │   │   Embedding   │   │     RAG       │
│   Service     │   │   Service     │   │   Service     │
│    :8081      │   │    :8082      │   │    :8083      │
└───────────────┘   └───────────────┘   └───────────────┘
        │                    │                    │
        └────────────────────┼────────────────────┘
                             │
                    ┌────────┴────────┐
                    │  Milvus Service │
                    │     :8084       │
                    └────────┬────────┘
                             │
                    ┌────────┴────────┐
                    │     Milvus      │
                    │     :19530      │
                    └─────────────────┘
```

## 模块说明

| 模块 | 端口 | 功能 |
|------|------|------|
| common | - | 公共模块，缓存配置、工具类 |
| api-gateway | 8080 | API 网关，路由转发、认证过滤 |
| config-server | 8888 | 配置中心，统一配置管理 |
| eureka-server | 8761 | 服务注册中心 |
| document-service | 8081 | 文档解析与入库服务 |
| embedding-service | 8082 | 向量嵌入生成服务 |
| rag-service | 8083 | RAG 检索增强服务 |
| milvus-service | 8084 | Milvus 向量数据库服务 |

## 快速开始

### 环境要求

- JDK 21+
- Maven 3.9+
- Docker & Docker Compose
- Milvus 2.3+

### 启动 Milvus

```bash
# 使用 Docker Compose 启动 Milvus
wget https://github.com/milvus-io/milvus/releases/download/v2.3.0/milvus-standalone-docker-compose.yml -O docker-compose.yml
docker compose up -d
```

### 编译项目

```bash
mvn clean install -DskipTests -Denforcer.skip=true
```

### 启动服务

```bash
# 1. 启动 Eureka 服务注册中心
java -jar eureka-server/target/eureka-server-1.0.0-SNAPSHOT.jar

# 2. 启动配置中心
java -jar config-server/target/config-server-1.0.0-SNAPSHOT.jar

# 3. 启动其他服务
java -jar document-service/target/document-service-1.0.0-SNAPSHOT.jar
java -jar embedding-service/target/embedding-service-1.0.0-SNAPSHOT.jar
java -jar milvus-service/target/milvus-service-1.0.0-SNAPSHOT.jar
java -jar rag-service/target/rag-service-1.0.0-SNAPSHOT.jar

# 4. 启动 API 网关
java -jar api-gateway/target/api-gateway-1.0.0-SNAPSHOT.jar
```

## API 文档

### 访问在线文档

启动服务后访问：
- API Gateway: http://localhost:8080
- Eureka Dashboard: http://localhost:8761
- Swagger UI (各服务): http://localhost:{port}/swagger-ui.html

### RESTful API 设计

本系统实现了完整的RESTful API，遵循以下设计原则：

#### 统一响应格式

所有API返回统一的 `Result<T>` 格式：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": { ... },
  "timestamp": "2024-04-07T17:00:00",
  "traceId": "req-12345"
}
```

#### 主要API端点

**文档管理API** (Document Service - 8081)
- `POST /api/v1/documents` - 上传文档
- `POST /api/v1/documents/batch` - 批量上传
- `GET /api/v1/documents` - 文档列表（支持分页、筛选）
- `GET /api/v1/documents/{id}` - 文档详情
- `DELETE /api/v1/documents/{id}` - 删除文档

**向量化API** (Embedding Service - 8082)
- `POST /api/v1/embeddings` - 文本向量化
- `POST /api/v1/embeddings/batch` - 批量向量化
- `GET /api/v1/embeddings/status/{taskId}` - 查询任务状态

**RAG查询API** (RAG Service - 8083)
- `POST /api/v1/rag/query` - RAG查询（混合检索+重排序）
- `POST /api/v1/rag/chat` - 流式对话（SSE）
- `GET /api/v1/rag/history/{sessionId}` - 会话历史

#### 详细API文档

完整的API设计文档请参考：[docs/api-design.md](docs/api-design.md)

包含：
- 统一响应格式说明
- 分页参数规范
- 异常处理机制
- 错误码定义
- 各服务API详细说明
- 请求/响应示例

## 核心功能

### RAG 检索增强生成

1. **文档入库**: 支持 PDF、Word 等文档解析，自动分块
2. **向量嵌入**: 使用 Embedding Model 生成文档向量
3. **相似度搜索**: Milvus 向量检索
4. **重排序**: CrossEncoder 精排
5. **上下文增强**: 多路检索融合

### 分布式特性

- **服务发现**: Eureka
- **配置中心**: Spring Cloud Config
- **API 网关**: Spring Cloud Gateway
- **熔断限流**: Resilience4j
- **分布式追踪**: Micrometer Tracing + Zipkin

### 分布式追踪系统

本系统集成了完整的分布式追踪能力，支持Zipkin和Jaeger。

#### 主要功能

1. **自动追踪**
   - Service层方法自动追踪
   - Repository层数据库访问追踪
   - 支持自定义Span

2. **AI服务链路追踪**
   - 文档处理链路：上传→解析→分割→向量化→入库
   - RAG查询链路：问题→检索→重排序→生成→返回
   - Token使用追踪
   - 外部API调用追踪

3. **Trace分析**
   - 慢请求分析（P99延迟）
   - 错误链路追踪
   - 服务依赖图生成
   - 性能瓶颈定位

#### 快速开始

```bash
# 启动Zipkin
cd monitoring/tracing
docker-compose up -d zipkin

# 访问Zipkin UI
http://localhost:9411

# 调用API生成追踪数据
curl -X POST http://localhost:8080/api/v1/rag/retrieve \
  -H "Content-Type: application/json" \
  -d '{"query": "测试问题", "topK": 5}'

# 查看追踪统计
curl http://localhost:8080/api/v1/tracing/stats
curl http://localhost:8080/api/v1/tracing/bottlenecks
```

详细文档：
- [分布式追踪完整指南](docs/tracing-guide.md)
- [快速开始指南](docs/tracing-quickstart.md)

## 配置说明

### Milvus 配置

```yaml
spring:
  ai:
    vectorstore:
      milvus:
        client:
          host: localhost
          port: 19530
        database-name: default
        collection-name: knowledge_base
        embedding-dimension: 1536
        index-type: HNSW
        metric-type: COSINE
```

### OpenAI 配置

```yaml
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      base-url: https://api.openai.com
```

## 项目结构

```
ai-knowledge-base/
├── common/                    # 公共模块
│   └── src/main/java/
│       └── com/example/common/
│           └── cache/         # 多级缓存
├── api-gateway/               # API 网关
│   └── src/main/java/
│       └── com/example/apigateway/
│           └── filter/        # 过滤器
├── config-server/             # 配置中心
│   └── src/main/resources/
│       └── config-repo/       # 配置文件仓库
├── document-service/          # 文档服务
├── embedding-service/         # 嵌入服务
├── rag-service/               # RAG 服务
│   └── src/main/java/
│       └── com/example/rag/
│           ├── controller/    # REST 控制器
│           ├── service/       # 业务服务
│           ├── retriever/     # 检索器
│           ├── reranker/      # 重排序器
│           └── model/         # 数据模型
├── milvus-service/            # Milvus 服务
│   └── src/main/java/
│       └── com/example/milvus/
│           ├── config/        # 配置类
│           ├── controller/    # REST 控制器
│           └── service/       # 向量操作服务
└── eureka-server/             # 服务注册中心
```

## 开发指南

### 代码规范

- 遵循阿里巴巴 Java 开发手册
- 使用 Lombok 简化代码
- 单元测试覆盖率 > 60%

### 分支管理

- `main`: 主分支，稳定版本
- `develop`: 开发分支
- `feature/*`: 功能分支

## License

MIT License

## 贡献

欢迎提交 Issue 和 Pull Request！

## 监控与告警系统

本系统集成了完善的监控和告警体系，基于Prometheus + Alertmanager + Grafana构建。

### 核心功能

1. **性能基线告警**
   - RAG查询P99延迟监控（基线3000ms）
   - 向量化延迟监控（基线500ms）
   - Milvus查询延迟监控（基线100ms）
   - 缓存命中率监控（基线70%）
   - Token使用率监控（基线80%）

2. **多级告警体系**
   - **P0（紧急）**: 服务宕机、数据丢失、错误率>10%
   - **P1（严重）**: 性能下降>50%、错误率>5%、内存>90%
   - **P2（警告）**: 性能下降>20%、缓存命中率<50%、内存>85%
   - **P3（提示）**: 资源使用预警、趋势分析

3. **通知渠道**
   - 邮件通知（所有级别）
   - Slack（P0-P2）
   - 企业微信/钉钉（P0-P1）
   - 短信/电话（P0）

### 快速开始

```bash
# 启动监控系统
cd monitoring
docker-compose up -d

# 访问监控界面
# Prometheus: http://localhost:9090
# Alertmanager: http://localhost:9093
# Grafana: http://localhost:3000 (admin/admin)

# 运行告警测试
./test-alerts.sh

# 验证性能基线
./test-alerts.sh
# 选择选项10: 验证性能基线
```

### 告警测试

```bash
# 测试P0级别告警（服务宕机）
curl -X POST http://localhost:9093/api/v1/alerts \
  -H "Content-Type: application/json" \
  -d '[
    {
      "labels": {
        "alertname": "ServiceDown",
        "severity": "critical",
        "priority": "P0",
        "instance": "test-service:8080"
      },
      "annotations": {
        "summary": "测试服务宕机",
        "description": "测试实例已宕机"
      },
      "startsAt": "'$(date -u +"%Y-%m-%dT%H:%M:%SZ")'"
    }
  ]'

# 查询活跃告警
curl http://localhost:9093/api/v1/alerts
```

### 性能基线验证

```bash
# RAG查询P99延迟
curl -s 'http://localhost:9090/api/v1/query?query=histogram_quantile(0.99,rate(rag_query_latency_seconds_bucket[5m]))'

# 缓存命中率
curl -s 'http://localhost:9090/api/v1/query?query=rate(rag_cache_hits_total[5m])/(rate(rag_cache_hits_total[5m])+rate(rag_cache_misses_total[5m]))'

# Milvus查询延迟
curl -s 'http://localhost:9090/api/v1/query?query=histogram_quantile(0.99,rate(milvus_query_latency_seconds_bucket[5m]))'
```

### 监控配置文件

- `monitoring/performance-baselines.yml` - 性能基线配置
- `monitoring/alert.rules.yml` - 告警规则（包含P0-P3多级告警）
- `monitoring/alertmanager.yml` - Alertmanager完整配置
- `monitoring/prometheus.yml` - Prometheus配置
- `monitoring/grafana/alert-dashboard.json` - Grafana告警仪表盘
- `monitoring/test-alerts.sh` - 告警测试脚本

详细文档请参考：[告警测试文档](monitoring/docs/alert-testing-guide.md)

## 持续集成

本项目使用 GitHub Actions 实现持续集成和持续部署。

### CI/CD 流程

1. **代码检查**: 代码风格检查、单元测试
2. **构建**: Maven 构建和打包
3. **测试**: 自动化测试
4. **部署**: Docker 镜像构建和推送

查看构建状态：
- [GitHub Actions](https://github.com/whaodong/ai-knowledge-base/actions)
