# 增量式Token计数功能实现文档

## 概述

本次实现为RAG系统增加了完整的增量式Token计数功能，包括流式Token统计、会话Token管理、上下文优化、使用分析和Token预测等核心功能。

## 架构设计

### 1. 核心组件

#### 1.1 StreamingTokenCounter - 流式Token计数器
**位置**: `common/src/main/java/com/example/common/token/StreamingTokenCounter.java`

**功能特性**:
- ✅ 实时Token累加统计
- ✅ SSE流式输出支持
- ✅ Token使用量预警（80%预警，95%临界）
- ✅ 性能监控和监听器机制

**核心方法**:
```java
// 开始流式Token计数
StreamingTokenStats startStreaming(String sessionId, String modelName);

// 增量统计Token
int incrementTokens(String sessionId, String content);

// 获取实时Token使用信息
TokenUsageInfo getUsageInfo(String sessionId);

// 完成流式计数
StreamingTokenStats finishStreaming(String sessionId);
```

#### 1.2 SessionTokenManager - 会话Token管理器
**位置**: `common/src/main/java/com/example/common/token/SessionTokenManager.java`

**功能特性**:
- ✅ 会话Token累计统计（提问+回答）
- ✅ Token配额管理（用户级、会话级）
- ✅ 超限预警和截断
- ✅ 会话Token历史记录
- ✅ 多用户配额隔离

**核心方法**:
```java
// 开始新会话
SessionTokenStats startSession(String sessionId, String userId, String modelName);

// 记录提问Token
int recordQuestionTokens(String sessionId, String question);

// 记录回答Token
int recordAnswerTokens(String sessionId, String answer);

// 获取用户Token配额
UserTokenQuota getUserQuota(String userId);

// 设置用户Token配额
void setUserQuota(String userId, int dailyLimit);
```

#### 1.3 IncrementalContextOptimizer - 增量式上下文优化器
**位置**: `common/src/main/java/com/example/common/token/IncrementalContextOptimizer.java`

**功能特性**:
- ✅ 新增内容增量Token计算
- ✅ 差量Token计算优化
- ✅ 智能上下文保留策略（基于重要性）
- ✅ 历史Token缓存复用
- ✅ 上下文滑动窗口管理

**核心方法**:
```java
// 初始化上下文缓存
void initContextCache(String sessionId, String modelName);

// 增量计算Token
int calculateIncrementalTokens(String sessionId, String newContent, boolean isQuestion);

// 智能优化上下文
OptimizedContext optimizeContext(String sessionId, int maxTokens);

// 预测新增内容后的Token数
int predictTotalTokens(String sessionId, String newContent);
```

#### 1.4 TokenUsageAnalyzer - Token使用分析器
**位置**: `common/src/main/java/com/example/common/token/TokenUsageAnalyzer.java`

**功能特性**:
- ✅ 按用户统计Token消耗
- ✅ 按时间段分析使用趋势
- ✅ Token成本估算（支持多种模型定价）
- ✅ 异常使用检测
- ✅ 使用报告生成

**核心方法**:
```java
// 记录Token使用
void recordUsage(String userId, String sessionId, String modelName, 
                 int inputTokens, int outputTokens);

// 按用户统计
UserTokenStatistics getUserStatistics(String userId, LocalDateTime startTime, LocalDateTime endTime);

// 分析趋势
UsageTrendAnalysis analyzeTrend(LocalDateTime startTime, LocalDateTime endTime);

// 计算成本
double calculateCost(String modelName, int inputTokens, int outputTokens);

// 检测异常
List<AnomalyRecord> detectAnomalies(String userId);
```

#### 1.5 TokenPredictor - Token预测器
**位置**: `common/src/main/java/com/example/common/token/TokenPredictor.java`

**功能特性**:
- ✅ 预估回答Token数
- ✅ 动态调整检索结果数量
- ✅ 预测是否超出上下文窗口
- ✅ 基于历史数据的预测模型
- ✅ 智能提示词优化建议

**核心方法**:
```java
// 预估回答Token
TokenPrediction predictAnswerTokens(String question, String context, String modelName);

// 动态调整检索数量
int adjustRetrievalTopK(String question, int requestedTopK, String modelName, int maxContextTokens);

// 预测上下文窗口使用情况
ContextWindowPrediction predictContextWindowExceeded(String question, String context, String modelName);

// 获取优化建议
List<TokenOptimizationSuggestion> getOptimizationSuggestions(String question, String context, String modelName);
```

### 2. 集成层

#### 2.1 RagTokenService
**位置**: `rag-service/src/main/java/com/example/rag/token/RagTokenService.java`

**功能**:
- 统一封装Token管理功能
- 提供简化的API接口
- 管理会话生命周期

#### 2.2 TokenController
**位置**: `rag-service/src/main/java/com/example/rag/controller/TokenController.java`

**API接口**:

| 接口 | 方法 | 功能 |
|------|------|------|
| `/api/v1/tokens/count` | POST | 计算文本Token数 |
| `/api/v1/tokens/session/{sessionId}` | GET | 获取会话Token统计 |
| `/api/v1/tokens/session/{sessionId}/realtime` | GET | 获取实时Token信息 |
| `/api/v1/tokens/predict` | POST | 预测Token使用 |
| `/api/v1/tokens/user/{userId}/stats` | GET | 获取用户Token统计 |
| `/api/v1/tokens/trend` | GET | 获取使用趋势分析 |
| `/api/v1/tokens/report` | GET | 获取Token使用报告 |
| `/api/v1/tokens/anomalies/{userId}` | GET | 检测异常Token使用 |
| `/api/v1/tokens/optimize/suggestions` | POST | 获取Token优化建议 |
| `/api/v1/tokens/quota/{userId}` | POST | 设置用户Token配额 |
| `/api/v1/tokens/truncate` | POST | 智能截断文本 |
| `/api/v1/tokens/models` | GET | 获取支持的模型信息 |
| `/api/v1/tokens/adjust-topk` | POST | 动态调整检索数量 |

#### 2.3 TokenManagementScheduler
**位置**: `rag-service/src/main/java/com/example/rag/token/TokenManagementScheduler.java`

**定时任务**:
- 每日凌晨重置用户Token配额
- 每小时清理过期会话
- 每5分钟检查Token使用预警

## 使用示例

### 1. 流式对话集成Token管理

```java
// 在RagController的chat方法中
// 1. 初始化Token管理
ragTokenService.initSession(sessionId, userId, modelName);

// 2. 记录提问Token
ragTokenService.processQueryTokens(sessionId, question, context);

// 3. 注册流式Token监听器
ragTokenService.registerStreamingListener(sessionId, event -> {
    // 实时发送Token使用信息到客户端
    emitter.send(event);
});

// 4. 流式输出时增量统计
ragTokenService.processAnswerChunk(sessionId, answerChunk);

// 5. 完成会话
TokenUsageSummary summary = ragTokenService.completeSession(sessionId, userId);
```

### 2. 动态调整检索数量

```java
// 根据Token限制自动调整TopK
int adjustedTopK = ragTokenService.adjustRetrievalTopK(sessionId, question, requestedTopK);

// 使用调整后的TopK进行检索
RagRequest request = RagRequest.builder()
    .query(question)
    .topK(adjustedTopK)
    .build();
```

### 3. Token使用预测

```java
// 预测Token使用情况
TokenPredictor.ContextWindowPrediction prediction = 
    ragTokenService.predictContextWindow(sessionId, question, context);

if (prediction.isWillExceed()) {
    // 获取优化建议
    List<TokenOptimizationSuggestion> suggestions = 
        ragTokenService.getTokenOptimizationSuggestions(question, context, modelName);
    
    // 执行优化策略
    // ...
}
```

### 4. 上下文优化

```java
// 当上下文接近Token限制时，智能优化
if (contextOptimizer.needsOptimization(sessionId, 0.9)) {
    OptimizedContext optimized = ragTokenService.optimizeContext(sessionId, maxTokens);
    String optimizedContext = optimized.getContext();
}
```

## SSE事件格式

流式对话过程中，会发送以下SSE事件：

### 1. token-update - Token使用更新
```json
{
  "incrementalTokens": 15,
  "totalTokens": 1250,
  "timestamp": "2024-04-07T18:30:45"
}
```

### 2. warning - 预警事件
```json
{
  "message": "Token使用量接近上下文窗口限制",
  "predictedTokens": 4500,
  "maxTokens": 4096
}
```

### 3. done - 完成事件（包含Token统计）
```json
{
  "response": {
    "sessionId": "session-123",
    "reply": "完整回答内容",
    "finished": true
  },
  "tokenSummary": {
    "sessionId": "session-123",
    "userId": "user-001",
    "totalTokens": 1520,
    "questionTokens": 320,
    "answerTokens": 1200,
    "usagePercentage": "37.11%"
  }
}
```

## 性能优化

### 1. Token缓存
- 使用ConcurrentHashMap缓存已计算的Token数
- 最大缓存大小：10,000条记录

### 2. 批量计算
- 支持批量Token计算，减少重复计算

### 3. 增量统计
- 流式输出时采用增量统计，避免重复计算整个文本

### 4. 异步处理
- Token使用分析异步记录，不影响主流程性能

## 监控指标

### 1. 实时指标
- 当前会话Token使用量
- 用户配额使用率
- 上下文窗口使用率

### 2. 历史指标
- 每日Token使用量
- 每周使用趋势
- 用户使用排名

### 3. 异常检测
- 小时级异常使用
- 会话级异常使用
- 配额超限预警

## 配置参数

### 默认配置
```java
// Token配额
DEFAULT_DAILY_QUOTA = 100000;      // 每日10万Token
DEFAULT_SESSION_QUOTA = 8000;       // 每会话8000Token

// 预警阈值
WARNING_THRESHOLD = 0.8;            // 80%预警
CRITICAL_THRESHOLD = 0.95;          // 95%临界

// 上下文优化
RETENTION_RATIO = 0.3;              // 保留30%历史上下文
MIN_RETAINED_TOKENS = 500;          // 最少保留500 Token

// 异常检测
MAX_HOURLY_TOKENS = 50000;          // 每小时最大Token数
MAX_SESSION_TOKENS = 30000;         // 每会话最大Token数
```

## 扩展性

### 1. 添加新模型支持
在TokenUsageAnalyzer中添加模型定价：
```java
private static final Map<String, TokenPrice> PRICING = Map.of(
    "new-model", new TokenPrice(0.01, 0.02)
);
```

### 2. 自定义预测策略
继承TokenPredictor并实现自定义预测逻辑

### 3. 扩展监听器
实现Consumer<TokenUsageEvent>接口，注册自定义监听器

## 测试建议

### 单元测试
1. Token计数准确性测试
2. 流式Token累加测试
3. 配额管理测试
4. 上下文优化测试

### 集成测试
1. 流式对话完整流程测试
2. Token预测准确性测试
3. 异常使用检测测试

### 性能测试
1. 并发会话Token统计测试
2. 大文本Token计算性能测试
3. Token缓存命中率测试

## 后续优化方向

1. **持久化存储**: 将Token使用记录存储到数据库
2. **分布式支持**: 支持多实例环境下的Token管理
3. **机器学习**: 基于历史数据训练预测模型
4. **告警通知**: 集成消息通知系统，发送预警通知
5. **可视化面板**: 开发Token使用可视化监控面板
