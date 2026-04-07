# AI Knowledge Base

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

启动服务后访问：
- API Gateway: http://localhost:8080
- Eureka Dashboard: http://localhost:8761
- Swagger UI (各服务): http://localhost:{port}/swagger-ui.html

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
