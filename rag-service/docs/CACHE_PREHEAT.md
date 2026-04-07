# 缓存预热功能实现文档

## 概述

本次实现为RAG服务添加了完整的检索结果预热缓存机制，包括热点查询检测、智能预加载、多级缓存架构和全面的监控体系。

## 新增功能

### 1. 热点查询检测 (HotQueryDetector)

**功能特性：**
- 基于Redis的实时查询频率统计
- 动态识别Top N热点查询
- 查询时间窗口分析（默认1小时）
- 相关查询关联分析（查询A → 查询B的概率）

**核心方法：**
```java
// 记录查询访问
void recordQuery(String query, String userId)

// 检查是否为热点查询
boolean isHotQuery(String query)

// 获取Top N热点查询
List<HotQuery> getTopHotQueries(int n)

// 获取相关查询预测
List<RelatedQuery> getRelatedQueries(String query)
```

**配置参数：**
- `cache.hot-query.threshold`: 热点查询阈值（默认10次）
- `cache.hot-query.time-window-hours`: 时间窗口（默认1小时）
- `cache.hot-query.top-n`: Top N数量（默认100）
- `cache.hot-query.relation-threshold`: 相关查询概率阈值（默认0.3）

### 2. 缓存预热服务 (CachePreheatService)

**功能特性：**
- 定时预热Top N热点查询（默认每10分钟）
- TTL动态调整（热点查询缓存时间更长）
- 异步预热，不影响正常查询
- 预热任务执行监控

**核心方法：**
```java
// 手动预热单个查询
String manualPreheat(String query)

// 批量预热查询
List<String> batchPreheat(List<String> queries)

// 获取预热任务状态
PreheatTask getTaskStatus(String taskId)

// 获取预热统计
PreheatStats getStats()
```

**配置参数：**
- `cache.preheat.top-n`: 预热Top N数量（默认20）
- `cache.preheat.base-ttl`: 基础TTL（默认300秒）
- `cache.preheat.max-ttl`: 最大TTL（默认3600秒）
- `cache.preheat.fixed-rate`: 预热频率（默认600000毫秒）

**动态TTL计算：**
```
TTL = 基础TTL + (查询频率 / 频率权重) * 60秒
例如：查询频率为50次，基础TTL为300秒
TTL = 300 + (50 / 10) * 60 = 600秒（10分钟）
```

### 3. 智能预加载服务 (PreloadService)

**功能特性：**
- 用户查询历史分析（最多保存20条）
- 相关查询预测（查询A后大概率查询B）
- 后台异步预加载队列
- 预加载效果追踪

**核心方法：**
```java
// 分析查询并触发预加载
void analyzeAndPreload(String userId, String currentQuery)

// 获取用户查询历史
List<String> getUserQueryHistory(String userId)

// 获取预加载统计
PreloadStats getStats()
```

**配置参数：**
- `cache.preload.max-queue-size`: 最大预加载队列长度（默认50）
- `cache.preload.threshold`: 预加载概率阈值（默认0.3）
- `cache.preload.ttl`: 预加载缓存TTL（默认300秒）
- `cache.preload.delay-ms`: 预加载延迟（默认1000毫秒）

**预加载策略：**
1. 记录用户查询历史
2. 分析查询关联关系（A → B）
3. 当概率 ≥ 阈值时，触发预加载
4. 异步执行RAG检索并缓存结果

### 4. 多级缓存架构

**架构设计：**
```
┌─────────────────────────────────────┐
│  L1: Caffeine本地缓存（毫秒级）      │
│  - 容量: 1000                        │
│  - TTL: 写入300秒 / 访问180秒        │
│  - 统计: 开启                        │
└─────────────────────────────────────┘
            ↓
┌─────────────────────────────────────┐
│  L2: Redis分布式缓存                 │
│  - 默认TTL: 600秒                    │
│  - 键前缀: rag:cache:                │
│  - 序列化: JSON                      │
└─────────────────────────────────────┘
            ↓
┌─────────────────────────────────────┐
│  L3: Milvus向量库                    │
│  - 向量检索                          │
│  - 混合检索                          │
└─────────────────────────────────────┘
```

**缓存区域：**
- `embedding`: 向量嵌入缓存
- `similarity`: 相似度搜索缓存
- `llm_response`: LLM响应缓存
- `hotspot`: 热点查询缓存
- `preheat`: 预热缓存
- `preload`: 预加载缓存

**防护机制：**
- **防穿透**: 缓存空值，TTL缩短至60秒
- **防击穿**: 互斥锁，超时100ms
- **防雪崩**: 过期时间随机化（±10%）

### 5. 缓存监控服务 (CacheMonitorService)

**功能特性：**
- 缓存命中率统计
- 预热任务执行状态
- 缓存大小监控
- 性能指标收集
- 告警机制

**核心方法：**
```java
// 获取缓存总览
CacheOverview getCacheOverview()

// 获取特定缓存指标
CacheMetrics getCacheMetrics(String cacheName)

// 导出监控报告
String exportReport()
```

**配置参数：**
- `cache.monitor.collect-rate`: 指标收集频率（默认300000毫秒）
- `cache.monitor.low-hit-rate-threshold`: 低命中率阈值（默认0.5）
- `cache.monitor.high-memory-threshold`: 高内存使用阈值（默认10000MB）
- `cache.monitor.alert-cooldown-minutes`: 告警冷却时间（默认10分钟）

**监控指标：**
- 命中次数 / 未命中次数 / 命中率
- 请求数 / 淘汰数
- 平均加载延迟
- 预热成功/失败次数
- 预加载命中数/命中率

## REST API

### 预热管理

```bash
# 手动预热查询
POST /api/cache/preheat?query=机器学习算法

# 批量预热
POST /api/cache/preheat/batch
Content-Type: application/json
["查询1", "查询2", "查询3"]

# 查看预热任务状态
GET /api/cache/preheat/task/{taskId}

# 查看所有运行中的预热任务
GET /api/cache/preheat/tasks

# 查看预热统计
GET /api/cache/preheat/stats
```

### 监控管理

```bash
# 获取缓存总览
GET /api/cache/monitor/overview

# 获取特定缓存指标
GET /api/cache/monitor/metrics/{cacheName}

# 获取所有缓存指标
GET /api/cache/monitor/metrics

# 导出监控报告
GET /api/cache/monitor/report
```

### 热点查询管理

```bash
# 获取Top N热点查询
GET /api/cache/hot-queries?n=20

# 检查是否为热点查询
GET /api/cache/hot-queries/check?query=深度学习

# 获取相关查询预测
GET /api/cache/related-queries?query=自然语言处理

# 获取热点查询统计
GET /api/cache/hot-queries/stats
```

### 预加载管理

```bash
# 获取预加载统计
GET /api/cache/preload/stats

# 获取预加载结果列表
GET /api/cache/preload/results

# 获取用户查询历史
GET /api/cache/user/history?userId=user123

# 清除用户查询历史
DELETE /api/cache/user/history?userId=user123
```

### 统计管理

```bash
# 重置所有统计
POST /api/cache/stats/reset
```

## 使用示例

### 1. 使用带缓存的RAG检索

```java
@Autowired
private CachedRagRetrievalService cachedRagRetrievalService;

public RagResponse search(String query, String userId) {
    RagRequest request = RagRequest.builder()
            .query(query)
            .topK(10)
            .hybridSearch(true)
            .rerankEnabled(true)
            .metadata(Map.of("userId", userId))
            .build();
    
    return cachedRagRetrievalService.retrieve(request);
}
```

### 2. 手动预热查询

```bash
curl -X POST "http://localhost:8083/api/cache/preheat?query=Spring%20Boot教程"
```

### 3. 查看监控报告

```bash
curl "http://localhost:8083/api/cache/monitor/report"
```

输出示例：
```
=== 缓存监控报告 ===
生成时间: 2026-04-08T15:30:00

【总体统计】
总请求数: 1250
总命中数: 980
总未命中数: 270
总体命中率: 78.40%

【各缓存区域指标】
- 向量嵌入缓存: 命中率=85.20%, 请求数=450, 淘汰数=23
- 相似度搜索缓存: 命中率=72.50%, 请求数=600, 淘退数=45
- 预热缓存: 命中率=91.30%, 请求数=150, 淘汰数=5

【预热统计】
成功: 45, 失败: 2

【预加载统计】
总预加载数: 38, 命中数: 25, 命中率: 65.79%

【热点查询统计】
热点查询数: 15
```

## 性能优化建议

### 1. 缓存配置优化

**高并发场景：**
- 增加Caffeine最大容量至5000+
- 缩短本地缓存TTL，避免数据不一致
- 提高预热频率至每5分钟

**低并发场景：**
- 减少Caffeine容量至500
- 延长Redis TTL至1200秒
- 降低热点阈值至5次

### 2. 预热策略优化

**热门领域：**
- 提高Top N至50
- 延长热点查询TTL至1小时
- 降低预加载阈值至0.2

**长尾查询：**
- 降低Top N至10
- 缩短TTL，节省内存
- 提高预加载阈值至0.5

### 3. 监控告警优化

**关键指标：**
- 缓存命中率 < 60% → 预热策略调整
- 预加载命中率 < 30% → 预测模型优化
- Redis内存使用 > 80% → 清理过期数据

## 测试验证

运行测试类：
```bash
mvn test -Dtest=CachePreheatTest
```

测试覆盖：
- 热点查询记录和检测
- 查询关联关系分析
- 预热统计信息获取
- 预加载分析触发
- 缓存监控报告导出

## 注意事项

1. **内存使用**：Caffeine缓存会占用JVM堆内存，注意调整-Xmx参数
2. **Redis连接**：确保Redis连接池配置合理，避免连接泄漏
3. **定时任务**：预热和监控任务会在后台运行，注意线程池配置
4. **数据一致性**：本地缓存与Redis缓存可能存在短暂不一致
5. **序列化**：缓存的对象需要支持JSON序列化

## 后续优化方向

1. **分布式锁**：使用Redis分布式锁替代本地锁，支持集群环境
2. **机器学习**：基于用户行为训练预测模型，提高预加载准确性
3. **自适应调优**：根据实时监控数据自动调整缓存参数
4. **缓存分层**：增加持久化缓存层，减少冷启动时间
5. **A/B测试**：对比不同缓存策略的性能差异

## 文件清单

新增文件：
```
rag-service/src/main/java/com/example/rag/cache/
├── HotQueryDetector.java         # 热点查询检测器
├── CachePreheatService.java      # 缓存预热服务
├── PreloadService.java           # 智能预加载服务
└── CacheMonitorService.java      # 缓存监控服务

rag-service/src/main/java/com/example/rag/controller/
└── CacheController.java          # 缓存管理控制器

rag-service/src/main/java/com/example/rag/config/
└── CacheConfig.java              # 缓存配置类

rag-service/src/main/java/com/example/rag/service/
└── CachedRagRetrievalService.java # 带缓存的RAG检索服务

rag-service/src/test/java/com/example/rag/cache/
└── CachePreheatTest.java         # 缓存预热测试类
```

修改文件：
```
rag-service/src/main/resources/application.yml  # 添加缓存配置
rag-service/src/main/java/com/example/rag/model/RagRequest.java   # 添加metadata字段
rag-service/src/main/java/com/example/rag/model/RagResponse.java  # 添加fromCache字段
```

## 技术栈

- Spring Boot 3.3.0
- Spring AI 1.0.0-M3
- Caffeine 3.x（本地缓存）
- Redis（分布式缓存）
- Milvus（向量数据库）
- Lombok（简化代码）

## 联系方式

如有问题或建议，请联系开发团队。
