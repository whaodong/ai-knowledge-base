# AGENTS.md - Java项目配置规范

## 1. 项目概览
- **项目类型**: 基于 Spring Boot 3.3 + Spring AI 1.0.0-M3 + Milvus 的智能知识库系统，支持 RAG（检索增强生成）架构。
- **Java版本**: 21 (LTS)
- **构建工具**: Maven
- **包管理**: Maven Central
- **项目结构**:
    - `src/main/java`: 核心业务代码
    - `src/test/java`: 单元测试与集成测试
    - `src/main/resources`: 配置文件与静态资源

## 2. 技术架构
- 后端基础：Java 21、Spring Boot 3.3.0
- 微服务治理：Spring Cloud、Spring Cloud Alibaba Sentinel
- AI/RAG 相关：Spring AI 1.0.0-M3、LangChain4j 0.30.0、Milvus SDK、OpenAI 集成。
- 数据与缓存：Milvus（向量库）+ etcd + MinIO，Redis（Jedis），本地/测试有 H2，运行时支持 PostgreSQL
- 安全与韧性：Spring Security + JWT (jjwt)，Resilience4j（熔断/限流）。
- 文档处理：Apache Tika、Apache PDFBox（文档解析）。
- 前端：React 18 + TypeScript 5 + Vite 5，UI 使用 Ant Design 5，状态管理 Zustand，数据请求 Axios + TanStack React Query，图表 ECharts。参考 UI/package.json
- 观测与监控：Spring Boot Actuator、Micrometer、Prometheus、Grafana、Alertmanager，链路追踪用 Micrometer Tracing + Brave + Zipkin（含 Jaeger 备选）。参考 monitoring/docker-compose.yml 与 monitoring/tracing/docker-compose.yml
- 测试与压测：Spring Boot Test、Testcontainers（PostgreSQL/Milvus）、Gatling + Scala


## 3. 编码规范
- **代码风格**
    - 遵循阿里巴巴Java开发手册
- **命名使用驼峰式**:
    - 类名: `PascalCase` (如`UserService`)
    - 方法名: `camelCase` (如`getUserById`)
    - 变量名: `camelCase` (如`userRepository`)
    - 常量: `UPPER_SNAKE_CASE` (如`MAX_RETRY_COUNT`)
- **注释要求**:
    - 所有public方法必须有Javadoc
    - 复杂逻辑添加行内注释
    - 避免无意义注释 (如`// get user`)

## 4. 安全与质量审查
- **安全检查**:
    - 所有API必须有身份验证
    - 敏感数据加密存储
    - 验证输入防止SQL注入/XSS
- **代码审查清单**:
    - 检查空指针异常处理
    - 验证资源是否正确关闭
    - 确认日志级别适当
    - 检查异常处理是否合理
## 5.架构规范
- **必须**：所有安全相关逻辑放在security包下
- **禁止**：在Controller中直接处理密码
- **必须**：使用PasswordEncoder处理密码