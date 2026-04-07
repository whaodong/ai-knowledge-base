# 部署指南 (Deployment Guide)

## 目录
- [环境要求](#环境要求)
- [快速开始](#快速开始)
- [详细部署步骤](#详细部署步骤)
- [配置说明](#配置说明)
- [故障排查](#故障排查)

## 环境要求

### 基础环境
- **操作系统**: Linux (推荐 Ubuntu 20.04+), macOS, Windows 10/11
- **Docker**: 20.10+
- **Docker Compose**: 2.0+
- **JDK**: 21 (Temurin/Eclipse Adoptium)
- **Maven**: 3.8+

### 硬件要求
- **CPU**: 4核心以上
- **内存**: 16GB以上 (推荐 32GB)
- **磁盘**: 50GB可用空间

### 外部依赖
- Milvus 2.2+
- Redis Stack 7.0+
- PostgreSQL 14+ (可选，用于持久化存储)

## 快速开始

### 1. 克隆项目
```bash
git clone https://github.com/whaodong/ai-knowledge-base.git
cd ai-knowledge-base
```

### 2. 构建项目
```bash
# 编译项目
mvn clean compile -DskipTests -Denforcer.skip=true

# 打包项目
mvn clean package -DskipTests -Denforcer.skip=true
```

### 3. 启动基础设施
```bash
# 启动 Milvus、Redis 等基础服务
docker-compose up -d milvus redis

# 等待服务启动 (约30秒)
sleep 30
```

### 4. 启动应用服务
```bash
# 启动所有服务
docker-compose up -d
```

### 5. 验证部署
```bash
# 检查服务状态
docker-compose ps

# 检查健康状态
curl http://localhost:8080/actuator/health
```

## 详细部署步骤

### 第一步：准备环境

#### 安装 Docker
```bash
# Ubuntu/Debian
sudo apt-get update
sudo apt-get install docker.io docker-compose-plugin

# macOS (使用 Docker Desktop)
brew install --cask docker

# 验证安装
docker --version
docker compose version
```

#### 安装 JDK 21
```bash
# Ubuntu/Debian
sudo apt-get install temurin-21-jdk

# macOS
brew install temurin

# 验证安装
java -version
```

#### 安装 Maven
```bash
# Ubuntu/Debian
sudo apt-get install maven

# macOS
brew install maven

# 验证安装
mvn -version
```

### 第二步：配置环境变量

创建 `.env` 文件：
```bash
# 创建环境变量文件
cat > .env << EOF
# OpenAI API Key
OPENAI_API_KEY=your-api-key-here

# Milvus配置
MILVUS_HOST=milvus
MILVUS_PORT=19530

# Redis配置
REDIS_HOST=redis
REDIS_PORT=6379

# Eureka配置
EUREKA_SERVER_URL=http://eureka-server:8761/eureka/

# Config Server配置
CONFIG_SERVER_URL=http://config-server:8888
EOF
```

### 第三步：构建 Docker 镜像

```bash
# 构建所有服务的镜像
docker-compose build

# 或单独构建某个服务
docker build -t ai-knowledge-base-rag-service:latest ./rag-service
```

### 第四步：启动服务

#### 启动顺序（重要）
1. **基础设施服务**
   ```bash
   docker-compose up -d milvus redis
   sleep 30  # 等待服务就绪
   ```

2. **注册中心**
   ```bash
   docker-compose up -d eureka-server
   sleep 20  # 等待Eureka启动
   ```

3. **配置中心**
   ```bash
   docker-compose up -d config-server
   sleep 20  # 等待Config Server启动
   ```

4. **业务服务**
   ```bash
   docker-compose up -d document-service embedding-service milvus-service rag-service
   ```

5. **API网关**
   ```bash
   docker-compose up -d api-gateway
   ```

#### 一键启动
```bash
# 启动所有服务
docker-compose up -d

# 查看日志
docker-compose logs -f
```

### 第五步：验证部署

#### 检查服务状态
```bash
# 查看所有容器状态
docker-compose ps

# 检查服务健康状态
curl http://localhost:8761/actuator/health   # Eureka
curl http://localhost:8888/actuator/health   # Config Server
curl http://localhost:8080/actuator/health   # API Gateway
curl http://localhost:8083/actuator/health   # RAG Service
```

#### 检查服务注册
访问 Eureka Dashboard: http://localhost:8761

#### 测试API
```bash
# 测试RAG查询
curl -X POST http://localhost:8080/api/rag/query \
  -H "Content-Type: application/json" \
  -d '{
    "query": "什么是机器学习？",
    "topK": 5
  }'
```

## 配置说明

### 端口映射

| 服务 | 内部端口 | 外部端口 |
|------|---------|---------|
| API Gateway | 8080 | 8080 |
| Eureka Server | 8761 | 8761 |
| Config Server | 8888 | 8888 |
| Document Service | 8081 | 8081 |
| Embedding Service | 8082 | 8082 |
| RAG Service | 8083 | 8083 |
| Milvus Service | 8086 | 8086 |
| Milvus | 19530 | 19530 |
| Redis | 6379 | 6379 |
| Prometheus | 9090 | 9090 |
| Grafana | 3000 | 3000 |

### 环境变量配置

#### 必需环境变量
- `OPENAI_API_KEY`: OpenAI API密钥
- `MILVUS_HOST`: Milvus服务地址
- `REDIS_HOST`: Redis服务地址

#### 可选环境变量
- `SPRING_PROFILES_ACTIVE`: Spring配置profile (默认: docker)
- `JAVA_OPTS`: JVM参数配置
- `LOG_LEVEL`: 日志级别

### 资源配置

每个服务的资源配置可以在 `docker-compose.yml` 中调整：

```yaml
services:
  rag-service:
    deploy:
      resources:
        limits:
          cpus: '2'
          memory: 2G
        reservations:
          cpus: '1'
          memory: 1G
```

## Kubernetes部署 (可选)

### 创建命名空间
```bash
kubectl create namespace ai-knowledge-base
```

### 部署服务
```bash
# 使用Helm部署（推荐）
helm install ai-knowledge-base ./k8s/helm-chart -n ai-knowledge-base

# 或使用kubectl
kubectl apply -f k8s/deployments/ -n ai-knowledge-base
```

### 验证K8s部署
```bash
kubectl get pods -n ai-knowledge-base
kubectl get services -n ai-knowledge-base
```

## 故障排查

### 常见问题

#### 1. 服务无法启动
```bash
# 查看日志
docker-compose logs rag-service

# 检查端口占用
netstat -tlnp | grep 8083

# 检查资源使用
docker stats
```

#### 2. 服务间通信失败
```bash
# 检查网络
docker network ls
docker network inspect ai-knowledge-base_default

# 验证服务发现
curl http://localhost:8761/eureka/apps
```

#### 3. Milvus连接失败
```bash
# 检查Milvus状态
docker-compose logs milvus

# 测试Milvus连接
curl http://localhost:19530/v1/health
```

#### 4. 内存不足
```bash
# 增加Docker内存限制
# 在docker-compose.yml中调整memory配置

# 或清理未使用的容器
docker system prune -a
```

### 日志查看

```bash
# 查看所有服务日志
docker-compose logs -f

# 查看特定服务日志
docker-compose logs -f rag-service

# 导出日志
docker-compose logs > logs.txt
```

### 重启服务

```bash
# 重启单个服务
docker-compose restart rag-service

# 重启所有服务
docker-compose restart

# 完全重新部署
docker-compose down
docker-compose up -d
```

## 生产环境建议

### 安全配置
1. 启用HTTPS/TLS
2. 配置认证授权
3. 限制Actuator端点访问
4. 使用Secrets管理敏感信息

### 高可用配置
1. 部署多个实例
2. 配置负载均衡
3. 使用外部注册中心（如Consul）
4. 配置数据库集群

### 性能优化
1. 调整JVM参数
2. 优化数据库连接池
3. 配置缓存策略
4. 启用GZIP压缩

### 备份策略
1. 定期备份配置
2. 备份向量数据
3. 备份Redis数据
4. 自动化备份脚本

## 监控和运维

请参考 [MONITORING.md](./MONITORING.md) 了解详细的监控配置。

## CI/CD

项目已配置GitHub Actions CI/CD流水线，详见 `.github/workflows/ci.yml`。

### 手动触发部署
```bash
# 推送到main分支触发生产部署
git push origin main

# 推送到develop分支触发测试环境部署
git push origin develop
```

## 更新维护

### 更新服务
```bash
# 拉取最新代码
git pull

# 重新构建
mvn clean package -DskipTests

# 重新部署
docker-compose up -d --build
```

### 版本回滚
```bash
# 回滚到指定版本
git checkout <commit-hash>

# 重新部署
docker-compose up -d --build
```

## 联系支持

如遇到部署问题，请：
1. 查看日志文件
2. 检查配置是否正确
3. 参考故障排查章节
4. 提交Issue到GitHub仓库
