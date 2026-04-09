# AGENTS.md
## 后端约束
## 项目概述

本仓库是一个基于 Java 21、Spring Boot 3.3、Spring Cloud、Spring AI、Milvus 的智能知识库系统，采用微服务架构，核心目标是实现：

- 文档解析与入库
- 向量化与向量存储
- 基于 Milvus 的相似度检索
- 基于 RAG 的知识问答与流式对话
- Token 统计、缓存、多级监控与告警

项目核心技术栈：

- Java 21
- Spring Boot 3.3.0
- Spring Cloud 2023.0.1
- Spring AI 1.0.0-M3
- Milvus 2.3+
- LangChain4j
- Resilience4j
- Redis（可选）
- Micrometer + Zipkin
- Prometheus + Grafana + Alertmanager

---

## 微服务模块职责

### common

公共模块，包含：

- 多级缓存
- Token 统计
- 安全配置
- 分布式追踪
- 工具类
- 通用配置

只有当多个服务都需要复用时，才允许把逻辑抽到 common 中。不要为了“看起来优雅”而过度抽象。

---

### api-gateway

负责：

- API 路由转发
- 统一鉴权
- 请求过滤
- 服务聚合入口

不要在 gateway 中实现业务逻辑。

---

### config-server

负责：

- Spring Cloud Config 配置中心
- 配置仓库管理
- 各服务配置下发

不要把业务逻辑写入 config-server。

---

### eureka-server

负责：

- 服务注册
- 服务发现

除注册与发现逻辑外，不要增加其他职责。

---

### document-service

负责：

- 文档上传
- 文档解析
- 文档切分
- 文档元数据处理
- 文档入库前准备

与文档格式相关的能力应优先放在 document-service 中，例如：

- PDF
- Word
- Markdown
- HTML
- Excel
- OCR 扩展

不要把文档解析逻辑写到 rag-service 或 embedding-service 中。

---

### embedding-service

负责：

- 文本向量化
- 批量向量化
- Embedding 模型调用
- 向量化任务状态管理

与 embedding、向量生成、批处理、重试、限流、异步任务有关的逻辑，都应优先放在 embedding-service 中。

---

### rag-service

负责：

- RAG 查询
- 多轮对话
- 会话历史
- Token 统计
- 上下文拼接
- 检索器
- 重排序器
- SSE 流式输出
- 缓存查询结果

所有与问答链路、聊天链路、上下文增强、提示词组装、会话状态相关的逻辑，都优先放在 rag-service 中。

常见目录职责：

- controller：REST 接口
- service：业务逻辑
- retriever：检索逻辑
- reranker：重排序逻辑
- cache：缓存逻辑
- model：数据结构
- config：配置类

除非明确需要，不要把 rag-service 的逻辑拆到其他服务中。

---

### milvus-service

负责：

- 向量集合管理
- 向量插入
- 向量搜索
- 索引创建
- 向量统计
- Milvus 配置与封装

与 Milvus SDK、集合、索引、向量存储相关的逻辑，都应放在 milvus-service 中。

不要把 Milvus 的底层调用散落在其他服务中。

---

## 默认工作方式

处理任务时，默认遵循以下流程：

1. 先阅读相关模块和调用链
2. 找到真实入口类和配置文件
3. 明确修改应该落在哪个服务
4. 给出最小改动方案
5. 再开始编码
6. 最后输出修改说明、验证步骤、风险点

对于复杂需求，优先先分析，再编码。

---

## 开发约束

### 最小改动原则

默认只做最小、可回滚、高置信度修改。

禁止：

- 无意义重命名
- 大范围包迁移
- 大面积格式化
- 随意调整已有类层次
- 引入新框架
- 引入新的复杂抽象层
- 为了“优雅”而过度设计

优先：

- 复用已有 Service
- 复用已有 DTO / VO / Model
- 复用已有 Config
- 复用已有工具类
- 保持原有命名风格
- 保持原有包结构

---

### 服务边界约束

新增功能时，优先保证服务边界清晰：

- 文档解析 → document-service
- 向量生成 → embedding-service
- 检索、重排序、聊天 → rag-service
- 向量存储与搜索 → milvus-service
- 通用工具 → common

不要因为实现方便而跨服务乱写逻辑。

---

### 配置约束

所有新增配置必须：

- 支持 application.yml
- 支持环境变量覆盖
- 命名风格与现有配置保持一致
- 尽量兼容 config-server

不要把配置硬编码到 Java 代码中。

---

## DashScope / OpenAI 兼容模式约束

本项目使用阿里云 DashScope 的 OpenAI 兼容模式。

默认配置类似：

```yaml
spring:
  ai:
    openai:
      api-key: ${DASHSCOPE_API_KEY}
      base-url: https://dashscope.aliyuncs.com/compatible-mode/v1
```

## 前端生成约束
# AGENTS.md

## 项目目标

本仓库的目标是：基于现有 Java 后端项目，生成并持续完善一套配套的前端项目。

前端项目必须做到：

- 与后端真实业务模块一一对应
- 与后端接口结构兼容
- 与后端鉴权方式兼容
- 与后端统一返回结构、分页结构、错误处理方式兼容
- 保持可维护、可扩展、可联调

默认工作模式不是“凭空生成一个前端”，而是：

1. 先阅读后端代码
2. 理解模块边界与接口结构
3. 产出前端架构方案
4. 再分阶段生成前端代码
5. 最后进行构建、联调、修复与完善

---

## 总体原则

所有工作必须遵循以下原则：

- 先分析，后编码
- 优先基于真实后端代码，而不是猜测
- 优先做最小可运行方案
- 优先保持与后端一致
- 优先复用已有前端代码和组件
- 避免一次性生成过大范围的代码
- 每轮只完成一个明确阶段或模块
- 改完后必须说明如何验证

不要一开始就生成“完整生产级前端”。
要先从可运行骨架和核心模块开始，再逐步补全。

---

## 后端优先原则

当前任务的核心依据是 Java 后端项目。

在开始任何前端生成或修改之前，必须优先识别并理解以下内容：

- controller
- service
- dto
- vo
- entity
- 统一响应体
- 分页结构
- 错误码结构
- 鉴权逻辑
- 文件上传逻辑
- 流式接口（如 SSE）
- 配置文件
- Swagger / OpenAPI（如果存在）

如果后端代码和 README、文档描述不一致，以真实代码为准。

---

## 前端生成目标

根据后端项目，优先生成以下内容：

- 前端项目目录结构
- 路由结构
- 页面骨架
- API 请求层
- TypeScript 类型定义
- 登录与鉴权适配
- 列表页
- 详情页
- 创建页
- 编辑页
- 搜索、筛选、分页
- loading / empty / error 三态
- 基础布局与导航

如果后端存在特殊能力，也要考虑前端配套支持，例如：

- 文件上传
- 流式聊天
- Token 用量显示
- 实时状态轮询
- 向量检索结果展示
- 管理后台统计页

---

## 前端技术栈默认约束

除非明确要求，否则默认使用以下技术栈生成前端：

- React
- TypeScript
- Vite
- React Router
- 统一 request 封装
- 轻量状态管理（优先 Zustand，或保持仓库现有方案）
- 与当前项目风格一致的 UI 方案

如果仓库中已经存在前端项目或前端技术栈，则优先复用现有方案，不要擅自切换。

不要无故引入：

- 新的主框架
- 新的状态管理方案
- 新的样式体系
- 多套 UI 库并存

---

## 前端目录结构建议

默认生成或维护如下结构：

```text
src/
  api/
  assets/
  components/
  hooks/
  layouts/
  pages/
  router/
  store/
  styles/
  types/
  utils/
```