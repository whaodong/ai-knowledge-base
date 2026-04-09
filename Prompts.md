# 项目专用 Codex 提示词清单

## 1. 仓库扫描与架构理解

### Prompt：全局扫描仓库

```text
先不要修改代码。

请阅读当前仓库，并输出：

1. 各微服务职责
2. 模块依赖关系
3. 文档入库 → 向量化 → Milvus 存储 → RAG 查询 → SSE 流式输出 的完整链路
4. 与缓存、Token 统计、Milvus、DashScope 配置相关的关键类和配置文件
5. 高风险修改点
6. 最小改动建议

要求：
- 优先基于真实代码，不要只依赖 README
- 输出具体类名、包名、配置路径
- 不要修改代码
```

### Prompt：识别某个需求应该落在哪个服务

```text
先不要写代码。

请分析以下需求应该落在哪个服务中实现：

需求：
[在这里填写需求]

要求：
- 明确应该修改哪个服务
- 明确需要修改哪些 package
- 明确哪些服务不应该动
- 给出最小改动方案
- 给出备选方案
```

## 2. RAG 查询链路

### Prompt：新增 RAG 查询能力

```text
请在 rag-service 中新增以下能力：

[填写功能]

要求：
- 优先复用现有 controller、service、retriever、reranker、model
- 保持现有 API 风格
- 不要新增无必要抽象层
- 不要破坏已有查询行为
- 如需新增配置，必须支持 application.yml 和环境变量
- 输出修改文件清单、验证步骤、风险点
```

### Prompt：增强 `/api/v1/rag/query`

```text
请增强现有 `/api/v1/rag/query` 接口，增加以下能力：

[填写功能，例如：metadata 过滤、最小相似度阈值、TopK 可配置]

要求：
- 保持旧接口兼容
- 新增参数必须是可选参数
- 默认行为保持不变
- 优先复用现有 retriever 和 reranker
- 给出性能影响说明
```

### Prompt：增强 `/api/v1/rag/chat`

```text
请增强现有 `/api/v1/rag/chat` 流式对话接口，增加以下能力：

[填写功能，例如：来源引用、上下文压缩、Token 限制、失败重试]

要求：
- 保持 SSE 流式输出兼容
- 不要破坏 Token 统计逻辑
- 不要破坏历史会话逻辑
- Redis 不可用时仍可正常工作
- 外部模型异常时必须记录日志并返回可诊断错误
- 至少补一个测试
```

## 3. 文档解析与向量化

### Prompt：新增文档格式支持

```text
请在 document-service 中新增对以下文档格式的支持：

[填写格式，例如：Markdown、HTML、Excel、OCR]

要求：
- 先定位现有文档解析入口
- 保持现有上传和分块流程兼容
- 不要影响 PDF 和 Word 已有逻辑
- 优先复用现有 parser 风格
- 增加必要测试
```

### Prompt：增强 embedding-service

```text
请增强 embedding-service，增加以下能力：

[填写功能，例如：批量限流、重试、异步任务状态、失败重跑]

要求：
- 保持现有 API 不变
- 不依赖真实外部模型进行测试
- 对外部 embedding 服务失败时不要吞异常
- 日志中保留 taskId、耗时、错误码
- 如需引入 Resilience4j，请遵守现有项目风格
```

## 4. Milvus 相关

### Prompt：检查 Milvus SDK 兼容性

```text
请检查 milvus-service 中所有 Milvus SDK 2.3.4 相关代码。

重点检查：
- io.milvus.param / io.milvus.grpc 的 import
- createIndex 返回值类型
- IndexDescription API
- MetricType / IndexType 用法

要求：
- 仅做兼容性修复
- 不修改业务语义
- 不做无关重构
- 输出兼容性修复清单
```

## 5. Redis 与缓存

### Prompt：排查 Redis 不可用时的启动失败

```text
请排查 Redis 未启动时缓存相关 Bean 报错问题。

要求：
- 检查 @ConditionalOnBean 相关逻辑
- Redis 不可用时主业务必须正常运行
- 不要粗暴删除条件装配
- 输出根因、修改文件、验证步骤
```

## 6. Token 统计与会话管理

### Prompt：增强 Token 统计

```text
请增强 Token 管理逻辑，增加以下能力：

[填写功能，例如：按用户统计、按模型统计、按天统计、成本报表]

要求：
- 保持现有 session 级统计逻辑
- 不破坏 SSE 增量统计
- 输出新增字段和兼容性说明
```

## 7. 启动失败 / Bug 修复

### Prompt：排查启动失败

```text
请排查以下启动失败问题：

[粘贴报错]

要求：
- 先分析根因
- 重点检查：
  - DashScope API Key
  - Redis 条件装配
  - Bean 覆盖
  - Milvus 配置
  - Eureka / Config Server 启动顺序
- 只做最小改动修复
- 输出修复步骤和验证方法
```

## 8. 测试

### Prompt：补单元测试

```text
请为以下类或功能补充单元测试：

[填写类名或功能]

要求：
- 不依赖真实 DashScope
- 不依赖真实 Redis
- 不依赖真实 Milvus
- 优先 mock 外部依赖
- 覆盖正常路径、异常路径、边界输入
- 输出测试运行命令
```

## 9. 文档与代码审查

### Prompt：做代码审查

```text
请对以下改动做代码审查。

重点关注：
- 是否破坏微服务边界
- 是否违反最小改动原则
- 是否有潜在空指针
- 是否有线程安全问题
- 是否有缓存不一致问题
- 是否会影响 Token 统计和 SSE 行为
- 是否需要补测试
```
