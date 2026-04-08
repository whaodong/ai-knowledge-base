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
| Milvus SDK | 2.3.4 | 向量数据库Java客户端 |
| Milvus | 2.3+ | 向量数据库 |
| LangChain4j | 0.30.0 | LLM 应用框架 |
| Resilience4j | 2.2.0 | 熔断限流 |
| SpringDoc | 2.5.0 | OpenAPI文档 |
| jtokkit | 1.0.0 | Token计数 |
| 通义千问 | qwen-plus | 阿里云大模型（OpenAI兼容模式） |

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
                    │     :8086       │
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
| common | - | 公共模块，缓存配置、工具类、安全配置 |
| api-gateway | 8080 | API 网关，路由转发、认证过滤 |
| config-server | 8888 | 配置中心，统一配置管理 |
| eureka-server | 8761 | 服务注册中心 |
| document-service | 8081 | 文档解析与入库服务 |
| embedding-service | 8082 | 向量嵌入生成服务 |
| rag-service | 8083 | RAG 检索增强服务 |
| milvus-service | 8086 | Milvus 向量数据库服务 |

## 快速开始

### 环境要求

- JDK 21+
- Maven 3.9+
- Docker & Docker Compose
- Milvus 2.3+
- Redis (可选，用于缓存)

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

## 配置说明

### 阿里云通义千问配置

本项目使用阿里云通义千问（DashScope）作为 LLM 服务，通过 OpenAI 兼容模式接入：

```yaml
spring:
  ai:
    openai:
      api-key: ${DASHSCOPE_API_KEY:your-api-key}
      base-url: https://dashscope.aliyuncs.com/compatible-mode/v1
      chat:
        options:
          model: qwen-plus
      embedding:
        enabled: true
        base-url: https://dashscope.aliyuncs.com/compatible-mode/v1
        api-key: ${DASHSCOPE_API_KEY:your-api-key}
        options:
          model: text-embedding-v3
```

**支持的模型：**
- Chat: `qwen-plus`, `qwen-turbo`, `qwen-max`
- Embedding: `text-embedding-v3`

**获取 API Key：**
1. 访问 [阿里云 DashScope](https://dashscope.console.aliyun.com/)
2. 开通服务并创建 API Key
3. 设置环境变量：`export DASHSCOPE_API_KEY=your-api-key`

### Milvus 配置

```yaml
milvus:
  host: ${MILVUS_HOST:localhost}
  port: ${MILVUS_PORT:19530}
  database: ${MILVUS_DATABASE:default}
  collection:
    name: ${MILVUS_COLLECTION_NAME:knowledge_base}
    vector-dimension: ${MILVUS_VECTOR_DIMENSION:1024}
    index-type: ${MILVUS_INDEX_TYPE:HNSW}
    metric-type: ${MILVUS_METRIC_TYPE:COSINE}
```

### Redis 配置（可选）

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      # password: 123456  # 如需密码认证
```

### Bean 覆盖配置

由于存在自动配置和手动配置的 Bean 冲突，需启用覆盖：

```yaml
spring:
  main:
    allow-bean-definition-overriding: true
```

## API 文档

### 访问在线文档

启动服务后访问：
- API Gateway: http://localhost:8080
- Eureka Dashboard: http://localhost:8761
- Swagger UI (各服务): http://localhost:{port}/swagger-ui.html

### 主要API端点

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
- `POST /api/v1/rag/chat` - 流式对话（SSE，集成Token管理）
- `GET /api/v1/rag/history/{sessionId}` - 会话历史

**Token管理API** (RAG Service - 8083)
- `POST /api/v1/tokens/count` - 计算文本Token数
- `GET /api/v1/tokens/session/{sessionId}` - 获取会话Token统计
- `POST /api/v1/tokens/predict` - 预测Token使用

**Milvus向量操作API** (Milvus Service - 8086)
- `POST /api/v1/milvus/collections` - 创建集合
- `POST /api/v1/milvus/vectors` - 插入向量
- `POST /api/v1/milvus/search` - 向量搜索
- `GET /api/v1/milvus/stats` - 集合统计

## 核心功能

### RAG 检索增强生成

1. **文档入库**: 支持 PDF、Word 等文档解析，自动分块
2. **向量嵌入**: 使用通义千问 Embedding 模型生成文档向量
3. **相似度搜索**: Milvus 向量检索
4. **重排序**: CrossEncoder 精排
5. **上下文增强**: 多路检索融合

### 增量式Token计数

基于 jtokkit 库实现准确的 Token 计数：

- **实时统计**: 监听流式响应事件，增量累加Token数
- **SSE支持**: 实时显示Token使用量
- **预警机制**: 80%预警、95%临界预警
- **配额管理**: 用户级、会话级Token配额
- **成本估算**: 支持多种模型定价

### 多级缓存系统

支持本地缓存（Caffeine）和分布式缓存（Redis）的多级缓存架构：

- **防穿透**: 空值缓存
- **防击穿**: 互斥锁
- **防雪崩**: 过期时间随机化
- **热点检测**: 基于Redis的热点查询识别
- **智能预热**: 定时预热Top N热点查询

**条件装配说明：**

所有缓存相关服务使用 `@ConditionalOnBean({CacheManager.class, RedisTemplate.class})` 进行条件装配，当 Redis 不可用时，整个缓存服务链会自动跳过加载，不影响主业务运行。

受影响的服务包括：
- `MultiLevelCacheService` - 多级缓存服务
- `RagCacheService` - RAG缓存服务
- `CacheMonitorService` - 缓存监控服务
- `CachePreheatService` - 缓存预热服务
- `HotQueryDetector` - 热点查询检测器
- `PerformanceTestService` - 性能测试服务
- `CacheController` - 缓存管理控制器

### 分布式特性

- **服务发现**: Eureka
- **配置中心**: Spring Cloud Config
- **API 网关**: Spring Cloud Gateway
- **熔断限流**: Resilience4j
- **分布式追踪**: Micrometer Tracing + Zipkin

## Milvus SDK 2.3.4 API 变化

本项目使用 Milvus Java SDK 2.3.4，相比旧版本有以下重要变化：

### 包路径变化

| 类 | 旧路径 | 新路径 |
|---|--------|--------|
| IndexType | `io.milvus.common.clientenum` | `io.milvus.param` |
| MetricType | `io.milvus.common.clientenum` | `io.milvus.param` |
| RpcStatus | `io.milvus.grpc` | `io.milvus.param` |
| DataType | `io.milvus.param` | `io.milvus.grpc` |

### 方法变化

| 旧方法 | 新方法 |
|--------|--------|
| `createIndex()` 返回 `R<Boolean>` | `createIndex()` 返回 `R<RpcStatus>` |
| `IndexDescription.getIndexTypeName()` | `IndexDescription.getIndexName()` |

### 示例代码

```java
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import io.milvus.param.RpcStatus;
import io.milvus.grpc.DataType;

// 创建索引
R<RpcStatus> response = milvusClient.createIndex(createIndexParam);
if (response.getStatus() == R.Status.Success.getCode()) {
    log.info("索引创建成功");
}
```

## 项目结构

```
ai-knowledge-base/
├── common/                    # 公共模块
│   └── src/main/java/
│       └── com/example/common/
│           ├── cache/         # 多级缓存
│           ├── security/      # 安全配置
│           ├── token/         # Token计数
│           └── tracing/       # 分布式追踪
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
│           ├── cache/         # 缓存服务
│           ├── retriever/     # 检索器
│           ├── reranker/      # 重排序器
│           └── model/         # 数据模型
├── milvus-service/            # Milvus 服务
│   └── src/main/java/
│       └── com/example/milvus/
│           ├── config/        # 配置类
│           ├── controller/    # REST 控制器
│           └── index/         # 索引策略
└── eureka-server/             # 服务注册中心
```

## 监控与告警系统

本系统集成了完善的监控和告警体系，基于 Prometheus + Alertmanager + Grafana 构建。

### 核心功能

1. **性能基线告警**
   - RAG查询P99延迟监控（基线3000ms）
   - 向量化延迟监控（基线500ms）
   - Milvus查询延迟监控（基线100ms）
   - 缓存命中率监控（基线70%）

2. **多级告警体系**
   - **P0（紧急）**: 服务宕机、数据丢失、错误率>10%
   - **P1（严重）**: 性能下降>50%、错误率>5%
   - **P2（警告）**: 性能下降>20%、缓存命中率<50%
   - **P3（提示）**: 资源使用预警

### 快速开始

```bash
# 启动监控系统
cd monitoring
docker-compose up -d

# 访问监控界面
# Prometheus: http://localhost:9090
# Alertmanager: http://localhost:9093
# Grafana: http://localhost:3000 (admin/admin)
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

### 本地开发

```bash
# 克隆项目
git clone https://github.com/whaodong/ai-knowledge-base.git
cd ai-knowledge-base

# 编译（跳过依赖检查和测试）
mvn clean install -DskipTests -Denforcer.skip=true

# 启动单个服务（开发模式）
cd rag-service
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8083"
```

## 常见问题

### Q: 启动时报 "OpenAI API key must be set"？

A: 需要配置通义千问 API Key：

```bash
export DASHSCOPE_API_KEY=your-api-key
```

或在配置文件中设置：

```yaml
spring:
  ai:
    openai:
      api-key: your-api-key
      base-url: https://dashscope.aliyuncs.com/compatible-mode/v1
```

### Q: 启动时报 "Parameter 0 of constructor required a bean of type 'MultiLevelCacheService'"？

A: 这是正常的条件装配行为。当 Redis 未启动时，缓存相关服务会被跳过，不影响核心业务功能。

如需使用缓存功能，请确保 Redis 已启动：

```bash
docker run -d --name redis -p 6379:6379 redis:latest
```

### Q: 编译时报依赖收敛错误？

A: 使用以下命令跳过依赖检查：

```bash
mvn clean install -Denforcer.skip=true
```

### Q: Milvus 连接失败？

A: 确保 Milvus 已启动：

```bash
# 检查 Milvus 状态
docker ps | grep milvus

# 启动 Milvus
cd monitoring
docker-compose up -d milvus-standalone
```

## License

MIT License

## 贡献

欢迎提交 Issue 和 Pull Request！

## 联系方式

- GitHub: [https://github.com/whaodong/ai-knowledge-base](https://github.com/whaodong/ai-knowledge-base)
