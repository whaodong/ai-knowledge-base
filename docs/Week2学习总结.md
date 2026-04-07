# Week 2 学习总结与复盘

## 学习周期
- **时间**: Day 8-14
- **主题**: RAG架构设计与Spring Cloud集成

## 完成情况

### Day 8-9: 文档处理流水线 ✅
- [x] 集成Apache Tika解析PDF/Word/Excel文档
- [x] 实现RecursiveCharacterTextSplitter文本分割
- [x] 设计文档元数据提取机制

### Day 10-11: Milvus向量数据库集成 ✅
- [x] Docker部署Milvus单机版
- [x] 配置Spring AI MilvusVectorStore
- [x] 实现向量数据的批量插入与检索

### Day 12-13: RAG检索服务实现 ✅
- [x] 构建多路检索器（向量检索 + BM25关键词检索）
- [x] 实现CrossEncoder重排序
- [x] 集成Redis缓存层

### Day 14: 项目编译与推送 ✅
- [x] 升级到 JDK 21 + Spring Boot 3.3.0
- [x] 适配 Spring AI 1.0.0-M3 API
- [x] 全部8个微服务模块编译成功
- [x] 推送代码到 GitHub

## 技术栈升级

| 组件 | 原版本 | 新版本 |
|------|--------|--------|
| JDK | 11 | 21 |
| Spring Boot | 2.7.18 | 3.3.0 |
| Spring Cloud | 2021.0.8 | 2023.0.1 |
| Spring AI | - | 1.0.0-M3 |
| Maven | - | 3.9.14 |

## 项目结构

```
ai-knowledge-base/
├── common/              # 公共模块（缓存配置）
├── api-gateway/         # API网关（路由、认证）
├── config-server/       # 配置中心
├── eureka-server/       # 服务注册中心
├── document-service/    # 文档解析服务
├── embedding-service/   # 向量嵌入服务
├── rag-service/         # RAG检索增强服务
└── milvus-service/      # Milvus向量数据库服务
```

## 关键技术突破

### 1. Spring AI 1.0.0-M3 API适配
- Document不可变对象模式
- SearchRequest静态工厂方法
- EmbeddingModel返回float[]

### 2. Spring Boot 3.x迁移
- javax → jakarta命名空间
- Spring Cloud Sleuth → Micrometer Tracing

### 3. Milvus SDK 2.x集成
- 使用gRPC类型替代已废弃的response包

## 遗留问题

1. **向量检索性能优化**: 需要调整HNSW索引参数
2. **缓存命中率**: 需要优化缓存Key设计
3. **异步流式响应**: 尚未实现SSE推送

## 下周计划 (Week 3)

### Day 15-17: 知识库核心功能
- 设计RESTful API
- 实现用户权限控制
- 构建管理后台

### Day 18-19: 工程化问题解决
- 高并发向量检索优化
- Token计数与上下文管理
- AI幻觉缓解策略

### Day 20-21: 系统集成与部署
- Docker容器化部署
- CI/CD流水线配置
- 健康检查与告警

## 学习资源

- Spring AI官方文档: https://docs.spring.io/spring-ai/
- Milvus文档: https://milvus.io/docs
- LangChain4j: https://langchain4j.cn/

---

**复盘日期**: 2026-04-07  
**下次复盘**: 2026-04-14 (Week 3结束)
