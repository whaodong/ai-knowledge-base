# AI知识库系统告警测试文档

## 目录
- [系统概述](#系统概述)
- [告警体系说明](#告警体系说明)
- [测试环境准备](#测试环境准备)
- [告警测试方法](#告警测试方法)
- [性能基线验证](#性能基线验证)
- [通知渠道测试](#通知渠道测试)
- [故障排查指南](#故障排查指南)
- [运维手册](#运维手册)

## 系统概述

### 架构组件
- **Prometheus**: 监控数据采集和告警规则评估
- **Alertmanager**: 告警聚合、分组、路由和通知
- **Grafana**: 可视化仪表盘和告警展示
- **Webhook服务**: 接收告警通知的自定义服务

### 监控服务
| 服务 | 端口 | 监控端点 |
|------|------|----------|
| Prometheus | 9090 | /metrics, /alerts, /rules |
| Alertmanager | 9093 | /metrics, /alerts, /silences |
| Grafana | 3000 | /api/alerts |
| RAG Service | 8083 | /actuator/prometheus |
| Milvus Service | 8086 | /actuator/prometheus |

## 告警体系说明

### 告警级别定义

#### P0 - 紧急 (Critical)
**定义**: 服务完全不可用或数据丢失风险

**影响范围**: 
- 服务宕机
- 数据库连接失败
- 向量数据库不可用
- 系统错误率>10%

**响应要求**: 
- 5分钟内响应
- 24小时on-call
- 全渠道通知（邮件、短信、电话、Slack、企业微信）

**示例告警**:
```yaml
- alert: ServiceDown
  expr: up == 0
  for: 1m
  labels:
    severity: critical
    priority: P0
```

#### P1 - 严重 (Critical)
**定义**: 服务性能严重下降或部分功能不可用

**影响范围**:
- 性能下降>50%
- 错误率>5%
- 内存使用>90%
- 关键依赖不可用

**响应要求**:
- 15分钟内响应
- 工作时间优先处理
- 多渠道通知（邮件、Slack）

**示例告警**:
```yaml
- alert: HighErrorRate
  expr: rate(http_server_requests_seconds_count{status=~"5.."}[5m]) > 0.05
  for: 5m
  labels:
    severity: critical
    priority: P1
```

#### P2 - 警告 (Warning)
**定义**: 性能低于预期，需要关注

**影响范围**:
- 性能下降20-50%
- 缓存命中率<50%
- 内存使用>85%
- CPU使用>85%

**响应要求**:
- 工作时间内处理
- 邮件和Slack通知

**示例告警**:
```yaml
- alert: LowCacheHitRate
  expr: rate(rag_cache_hits_total[5m]) / (rate(rag_cache_hits_total[5m]) + rate(rag_cache_misses_total[5m])) < 0.5
  for: 10m
  labels:
    severity: warning
    priority: P2
```

#### P3 - 提示 (Info)
**定义**: 资源使用预警，提前规划

**影响范围**:
- 资源使用>75%
- 连接数预警
- 磁盘空间预警

**响应要求**:
- 计划内处理
- 邮件通知

**示例告警**:
```yaml
- alert: ResourceUsageWarning
  expr: jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} > 0.75
  for: 20m
  labels:
    severity: info
    priority: P3
```

### 性能基线

| 指标 | 基线值 | P2告警阈值 | P1告警阈值 |
|------|--------|-----------|-----------|
| RAG查询P99延迟 | 3000ms | 1500ms (P95) | 3000ms (P99) |
| 向量化延迟 | 500ms | 300ms | 500ms |
| Milvus查询延迟 | 100ms | 200ms | 300ms |
| 缓存命中率 | 70% | <50% | <30% |
| Token使用率 | 80% | >80% | >90% |
| JVM堆内存 | 75% | >85% | >90% |
| CPU使用率 | 70% | >85% | >90% |

## 测试环境准备

### 前置条件
```bash
# 1. 检查服务运行状态
docker-compose ps

# 2. 验证网络连通性
curl http://localhost:9090/-/healthy
curl http://localhost:9093/-/healthy

# 3. 安装依赖工具
# Ubuntu/Debian
sudo apt-get install jq bc

# CentOS/RHEL
sudo yum install jq bc

# macOS
brew install jq bc
```

### 配置检查
```bash
# 检查Prometheus配置
cd monitoring
promtool check config prometheus.yml

# 检查告警规则
promtool check rules alert.rules.yml

# 检查Alertmanager配置
amtool check-config alertmanager.yml
```

### 测试脚本准备
```bash
# 赋予执行权限
chmod +x test-alerts.sh

# 配置环境变量（可选）
export PROMETHEUS_URL="http://localhost:9090"
export ALERTMANAGER_URL="http://localhost:9093"
```

## 告警测试方法

### 方法一：使用测试脚本（推荐）

```bash
# 运行交互式测试工具
./test-alerts.sh

# 主菜单选项：
# 1. 检查服务状态
# 2. 测试P0级别告警
# 3. 测试P1级别告警
# 4. 测试P2级别告警
# 5. 测试P3级别告警
# 6. 测试告警恢复
# 7. 查询告警状态
# 8. 创建静默规则
# 9. 测试通知渠道
# 10. 验证性能基线
# 11. 运行所有测试
```

### 方法二：手动API测试

#### 1. 测试P0告警（服务宕机）
```bash
curl -X POST http://localhost:9093/api/v1/alerts \
  -H "Content-Type: application/json" \
  -d '[
    {
      "labels": {
        "alertname": "ServiceDown",
        "severity": "critical",
        "priority": "P0",
        "team": "sre",
        "instance": "test-service:8080",
        "job": "test-service"
      },
      "annotations": {
        "summary": "测试服务宕机",
        "description": "测试实例 test-service:8080 已宕机",
        "impact": "服务不可用，影响所有用户",
        "action": "立即检查服务状态、日志，必要时重启服务"
      },
      "startsAt": "'$(date -u +"%Y-%m-%dT%H:%M:%SZ")'"
    }
  ]'
```

**预期结果**:
- Alertmanager收到告警
- 邮件、Slack、企业微信等通知渠道收到消息
- 告警出现在Grafana仪表盘

#### 2. 测试P1告警（高内存使用）
```bash
curl -X POST http://localhost:9093/api/v1/alerts \
  -H "Content-Type: application/json" \
  -d '[
    {
      "labels": {
        "alertname": "HighMemoryUsage",
        "severity": "critical",
        "priority": "P1",
        "team": "dev",
        "instance": "rag-service:8083"
      },
      "annotations": {
        "summary": "JVM堆内存使用超过90%",
        "description": "堆内存使用率达到92%，可能发生OOM",
        "impact": "服务可能崩溃或性能下降",
        "action": "检查内存泄漏、考虑扩容、调整JVM参数"
      },
      "startsAt": "'$(date -u +"%Y-%m-%dT%H:%M:%SZ")'"
    }
  ]'
```

#### 3. 测试P2告警（缓存命中率低）
```bash
curl -X POST http://localhost:9093/api/v1/alerts \
  -H "Content-Type: application/json" \
  -d '[
    {
      "labels": {
        "alertname": "LowCacheHitRate",
        "severity": "warning",
        "priority": "P2",
        "team": "ml",
        "instance": "rag-service:8083"
      },
      "annotations": {
        "summary": "缓存命中率低于50%",
        "description": "当前缓存命中率为45%，低于基线70%",
        "impact": "查询性能下降，后端压力增加",
        "action": "检查缓存策略、缓存容量、热点数据"
      },
      "startsAt": "'$(date -u +"%Y-%m-%dT%H:%M:%SZ")'"
    }
  ]'
```

#### 4. 测试P3告警（资源使用预警）
```bash
curl -X POST http://localhost:9093/api/v1/alerts \
  -H "Content-Type: application/json" \
  -d '[
    {
      "labels": {
        "alertname": "ResourceUsageWarning",
        "severity": "info",
        "priority": "P3",
        "team": "dev",
        "instance": "document-service:8081"
      },
      "annotations": {
        "summary": "JVM堆内存使用超过75%",
        "description": "堆内存使用率达到78%，请关注",
        "impact": "无明显影响",
        "action": "长期监控，规划扩容"
      },
      "startsAt": "'$(date -u +"%Y-%m-%dT%H:%M:%SZ")'"
    }
  ]'
```

#### 5. 测试告警恢复
```bash
curl -X POST http://localhost:9093/api/v1/alerts \
  -H "Content-Type: application/json" \
  -d '[
    {
      "labels": {
        "alertname": "ServiceDown",
        "severity": "critical",
        "priority": "P0",
        "team": "sre",
        "instance": "test-service:8080",
        "job": "test-service"
      },
      "annotations": {
        "summary": "测试服务宕机",
        "description": "测试实例 test-service:8080 已宕机",
        "impact": "服务不可用，影响所有用户",
        "action": "立即检查服务状态、日志，必要时重启服务"
      },
      "startsAt": "'$(date -u +"%Y-%m-%dT%H:%M:%SZ")'",
      "endsAt": "'$(date -u +"%Y-%m-%dT%H:%M:%SZ")'"
    }
  ]'
```

### 方法三：通过Prometheus模拟指标

#### 1. 使用Pushgateway推送测试指标
```bash
# 启用Pushgateway（如果未启用）
docker run -d -p 9091:9091 prom/pushgateway

# 推送高内存指标
cat <<EOF | curl --data-binary @- http://localhost:9091/metrics/job/test-memory
# TYPE jvm_memory_used_bytes gauge
jvm_memory_used_bytes{area="heap",instance="rag-service:8083"} 9.2E9
# TYPE jvm_memory_max_bytes gauge
jvm_memory_max_bytes{area="heap",instance="rag-service:8083"} 1.0E10
EOF

# 等待5分钟后检查告警
curl http://localhost:9090/api/v1/alerts
```

#### 2. 使用Python脚本模拟指标
```python
#!/usr/bin/env python3
import requests
import time

PROMETHEUS_URL = "http://localhost:9090"
PUSHGATEWAY_URL = "http://localhost:9091"

def push_metric(name, value, labels):
    """推送指标到Pushgateway"""
    metric_str = f"# TYPE {name} gauge\n{name}"
    for key, val in labels.items():
        metric_str += f'{{{key}="{val}"}}'
    metric_str += f" {value}\n"
    
    response = requests.post(
        f"{PUSHGATEWAY_URL}/metrics/job/test_alert",
        data=metric_str
    )
    return response.status_code == 202

def test_high_latency():
    """测试高延迟告警"""
    print("模拟高查询延迟...")
    push_metric("rag_query_latency_seconds", 5.0, {"instance": "rag-service:8083"})
    time.sleep(300)  # 等待5分钟
    check_alert("HighRAGQueryLatencyP99")

def check_alert(alertname):
    """检查告警是否触发"""
    response = requests.get(f"{PROMETHEUS_URL}/api/v1/alerts")
    alerts = response.json()["data"]["alerts"]
    for alert in alerts:
        if alert["labels"]["alertname"] == alertname:
            print(f"告警 {alertname} 已触发")
            return True
    print(f"告警 {alertname} 未触发")
    return False

if __name__ == "__main__":
    test_high_latency()
```

## 性能基线验证

### 自动验证脚本
```bash
# 使用测试脚本验证性能基线
./test-alerts.sh
# 选择选项10: 验证性能基线
```

### 手动验证步骤

#### 1. RAG查询P99延迟
```bash
# 查询当前P99延迟
curl -s 'http://localhost:9090/api/v1/query?query=histogram_quantile(0.99,rate(rag_query_latency_seconds_bucket[5m]))' | jq -r '.data.result[0].value[1]'

# 预期结果: < 3.0 (秒)
# 如果 > 3.0，则触发P1告警
```

#### 2. 缓存命中率
```bash
# 查询缓存命中率
curl -s 'http://localhost:9090/api/v1/query?query=rate(rag_cache_hits_total[5m])/(rate(rag_cache_hits_total[5m])+rate(rag_cache_misses_total[5m]))' | jq -r '.data.result[0].value[1]'

# 预期结果: > 0.7
# 如果 < 0.5，则触发P2告警
```

#### 3. Milvus查询延迟
```bash
# 查询Milvus P99延迟
curl -s 'http://localhost:9090/api/v1/query?query=histogram_quantile(0.99,rate(milvus_query_latency_seconds_bucket[5m]))' | jq -r '.data.result[0].value[1]'

# 预期结果: < 0.3 (秒)
# 如果 > 0.3，则触发P2告警
```

#### 4. JVM内存使用率
```bash
# 查询JVM堆内存使用率
curl -s 'http://localhost:9090/api/v1/query?query=jvm_memory_used_bytes{area="heap"}/jvm_memory_max_bytes{area="heap"}' | jq '.data.result[] | {instance: .metric.instance, value: .value[1]}'

# 预期结果: < 0.85
# 如果 > 0.90，则触发P1告警
# 如果 > 0.85，则触发P2告警
```

### 性能基线仪表盘查询

#### Grafana面板查询语句
```promql
# RAG查询延迟分布
histogram_quantile(0.50, rate(rag_query_latency_seconds_bucket[5m]))  # P50
histogram_quantile(0.95, rate(rag_query_latency_seconds_bucket[5m]))  # P95
histogram_quantile(0.99, rate(rag_query_latency_seconds_bucket[5m]))  # P99

# 缓存命中率趋势
rate(rag_cache_hits_total[5m]) / (rate(rag_cache_hits_total[5m]) + rate(rag_cache_misses_total[5m]))

# 向量化延迟
histogram_quantile(0.95, rate(embedding_latency_seconds_bucket[5m]))

# Milvus查询性能
histogram_quantile(0.99, rate(milvus_query_latency_seconds_bucket[5m]))

# Token使用率
sum(rate(llm_token_usage_total[5m])) / sum(rate(llm_token_limit_total[5m]))
```

## 通知渠道测试

### 1. 邮件通知测试
```bash
# 发送测试邮件
curl -X POST http://localhost:9093/api/v1/alerts \
  -H "Content-Type: application/json" \
  -d '[{
    "labels": {
      "alertname": "TestEmailNotification",
      "severity": "info"
    },
    "annotations": {
      "summary": "邮件通知测试",
      "description": "这是一封测试邮件"
    },
    "startsAt": "'$(date -u +"%Y-%m-%dT%H:%M:%SZ")'"
  }]'

# 检查邮件收件箱
```

### 2. Slack通知测试
```bash
# 直接测试Slack Webhook
curl -X POST https://hooks.slack.com/services/YOUR/WEBHOOK/URL \
  -H "Content-Type: application/json" \
  -d '{
    "text": "测试消息：Slack通知渠道测试成功",
    "username": "Alertmanager",
    "icon_emoji": ":warning:"
  }'

# 检查Slack频道消息
```

### 3. 企业微信通知测试
```bash
# 获取access_token
curl -s "https://qyapi.weixin.qq.com/cgi-bin/gettoken?corpid=YOUR_CORP_ID&corpsecret=YOUR_SECRET" | jq -r '.access_token'

# 发送测试消息
ACCESS_TOKEN="YOUR_ACCESS_TOKEN"
curl -X POST "https://qyapi.weixin.qq.com/cgi-bin/message/send?access_token=$ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "touser": "@all",
    "msgtype": "text",
    "agentid": 1000001,
    "text": {
      "content": "测试消息：企业微信通知渠道测试成功"
    }
  }'
```

### 4. 钉钉通知测试
```bash
# 测试钉钉机器人Webhook
curl -X POST https://oapi.dingtalk.com/robot/send?access_token=YOUR_TOKEN \
  -H "Content-Type: application/json" \
  -d '{
    "msgtype": "text",
    "text": {
      "content": "测试消息：钉钉通知渠道测试成功"
    }
  }'
```

### 5. Webhook通知测试
```bash
# 启动简单的webhook接收服务
python3 -m http.server 5001 &
# 或使用nc
nc -l 5001

# 发送测试告警
curl -X POST http://localhost:5001/webhook \
  -H "Content-Type: application/json" \
  -d '{"test": true, "message": "Webhook测试"}'

# 查看接收的数据
```

## 故障排查指南

### 问题1：告警未触发

**可能原因**:
1. Prometheus未采集到指标
2. 告警规则配置错误
3. 持续时间未达到阈值

**排查步骤**:
```bash
# 1. 检查指标是否存在
curl 'http://localhost:9090/api/v1/query?query=<metric_name>'

# 2. 检查告警规则状态
curl 'http://localhost:9090/api/v1/rules' | jq '.data.groups[].rules[] | select(.name=="<alert_name>")'

# 3. 检查告警评估
curl 'http://localhost:9090/api/v1/alerts'

# 4. 查看Prometheus日志
docker logs prometheus
```

### 问题2：告警已触发但未收到通知

**可能原因**:
1. Alertmanager未运行
2. 路由配置错误
3. 接收器配置错误
4. 通知渠道配置错误

**排查步骤**:
```bash
# 1. 检查Alertmanager状态
curl 'http://localhost:9093/api/v1/status'

# 2. 检查Alertmanager收到的告警
curl 'http://localhost:9093/api/v1/alerts'

# 3. 检查路由配置
amtool config routes show --config.file=alertmanager.yml

# 4. 检查通知发送日志
docker logs alertmanager

# 5. 测试通知渠道
amtool alert add alertname="Test" severity="warning" --alertmanager.url=http://localhost:9093
```

### 问题3：告警重复发送

**可能原因**:
1. repeat_interval配置过短
2. 告警未正确恢复

**排查步骤**:
```bash
# 1. 检查告警状态
curl 'http://localhost:9093/api/v1/alerts' | jq '.data[] | select(.labels.alertname=="<alert_name>")'

# 2. 检查repeat_interval配置
grep "repeat_interval" alertmanager.yml

# 3. 手动解决告警
curl -X POST 'http://localhost:9093/api/v1/alerts' -H "Content-Type: application/json" -d '[
  {
    "labels": {"alertname": "<alert_name>", ...},
    "endsAt": "'$(date -u +"%Y-%m-%dT%H:%M:%SZ")'"
  }
]'
```

### 问题4：抑制规则未生效

**可能原因**:
1. inhibit_rules配置错误
2. 标签匹配不正确

**排查步骤**:
```bash
# 1. 检查抑制规则配置
grep -A 10 "inhibit_rules" alertmanager.yml

# 2. 查看被抑制的告警
curl 'http://localhost:9093/api/v1/alerts' | jq '.data[] | select(.status.inhibitedBy | length > 0)'

# 3. 查看抑制源告警
curl 'http://localhost:9093/api/v1/alerts' | jq '.data[] | select(.status.inhibitedBy | length > 0) | .status.inhibitedBy'
```

### 问题5：性能基线指标不准确

**可能原因**:
1. 数据采集间隔不合适
2. 指标计算公式错误
3. 数据样本不足

**排查步骤**:
```bash
# 1. 检查数据采集配置
grep "scrape_interval" prometheus.yml

# 2. 验证指标计算
curl 'http://localhost:9090/api/v1/query?query=<expression>'

# 3. 查看原始数据
curl 'http://localhost:9090/api/v1/query?query=<metric_name>[5m]'

# 4. 增加数据采样窗口
curl 'http://localhost:9090/api/v1/query?query=<metric_name>[30m]'
```

## 运维手册

### 日常巡检清单

#### 每日检查
- [ ] 检查Prometheus服务状态
- [ ] 检查Alertmanager服务状态
- [ ] 查看活跃告警数量
- [ ] 确认P0/P1告警已处理

#### 每周检查
- [ ] 检查告警规则命中率
- [ ] 分析告警趋势
- [ ] 评估性能基线
- [ ] 清理过期静默规则

#### 每月检查
- [ ] 回顾告警处理效率
- [ ] 优化告警规则
- [ ] 更新性能基线
- [ | 培训新成员

### 告警处理流程

#### P0告警处理
```
1. 收到P0告警通知（电话+短信+邮件）
   ↓
2. 5分钟内响应（确认告警真实性）
   ↓
3. 评估影响范围
   ↓
4. 启动应急预案
   - 服务宕机：重启服务
   - 数据库故障：切换备库
   - 数据丢失：启动数据恢复流程
   ↓
5. 通知相关团队
   ↓
6. 每15分钟更新处理进度
   ↓
7. 解决后发送恢复通知
   ↓
8. 编写故障报告（24小时内）
```

#### P1告警处理
```
1. 收到P1告警通知（邮件+Slack）
   ↓
2. 15分钟内响应
   ↓
3. 分析根本原因
   ↓
4. 实施修复方案
   ↓
5. 验证修复效果
   ↓
6. 更新文档
```

#### P2/P3告警处理
```
1. 收到告警通知
   ↓
2. 工作时间内处理
   ↓
3. 记录处理过程
   ↓
4. 持续监控趋势
```

### 静默管理

#### 创建静默（计划维护）
```bash
# 使用amtool创建静默
amtool silence add \
  alertname=HighMemoryUsage \
  instance=rag-service:8083 \
  --duration=2h \
  --comment="计划内维护，增加内存" \
  --author="admin@example.com"

# 或使用API
curl -X POST http://localhost:9093/api/v1/silences \
  -H "Content-Type: application/json" \
  -d '{
    "matchers": [
      {"name": "alertname", "value": "HighMemoryUsage", "isRegex": false},
      {"name": "instance", "value": "rag-service:8083", "isRegex": false}
    ],
    "startsAt": "'$(date -u +"%Y-%m-%dT%H:%M:%SZ")'",
    "endsAt": "'$(date -u -d "+2 hours" +"%Y-%m-%dT%H:%M:%SZ")'",
    "createdBy": "admin@example.com",
    "comment": "计划内维护，增加内存"
  }'
```

#### 查看静默
```bash
# 列出所有静默
amtool silence query

# 查看特定静默详情
amtool silence query <silence_id>
```

#### 删除静默
```bash
# 使用amtool删除
amtool silence expire <silence_id>

# 或使用API
curl -X DELETE http://localhost:9093/api/v1/silence/<silence_id>
```

### 性能调优

#### Prometheus调优
```yaml
# prometheus.yml
global:
  scrape_interval: 15s        # 减少采集频率
  evaluation_interval: 30s    # 减少规则评估频率
  
# 存储配置
storage:
  tsdb:
    retention.time: 15d       # 数据保留时间
    retention.size: 50GB      # 数据保留大小
```

#### Alertmanager调优
```yaml
# alertmanager.yml
route:
  group_wait: 30s             # 增加分组等待时间
  group_interval: 5m          # 增加分组间隔
  repeat_interval: 4h         # 减少重复通知频率
```

#### 告警规则优化
```yaml
# 避免过于敏感的告警
- alert: HighCPUUsage
  expr: system_cpu_usage > 0.85
  for: 15m                    # 增加持续时间，减少误报
```

### 备份与恢复

#### 备份配置
```bash
#!/bin/bash
# 备份监控配置

BACKUP_DIR="/backup/monitoring/$(date +%Y%m%d)"
mkdir -p $BACKUP_DIR

# 备份Prometheus配置
cp prometheus.yml $BACKUP_DIR/
cp alert.rules.yml $BACKUP_DIR/

# 备份Alertmanager配置
cp alertmanager.yml $BACKUP_DIR/

# 备份静默规则
curl -s http://localhost:9093/api/v1/silences > $BACKUP_DIR/silences.json

# 备份Grafana仪表盘
curl -s http://admin:admin@localhost:3000/api/search?query=% > $BACKUP_DIR/dashboards.json

echo "备份完成: $BACKUP_DIR"
```

#### 恢复配置
```bash
#!/bin/bash
# 恢复监控配置

BACKUP_DIR=$1

# 恢复Prometheus配置
cp $BACKUP_DIR/prometheus.yml .
cp $BACKUP_DIR/alert.rules.yml .

# 恢复Alertmanager配置
cp $BACKUP_DIR/alertmanager.yml .

# 恢复静默规则
cat $BACKUP_DIR/silences.json | jq -c '.data[]' | while read silence; do
  curl -X POST http://localhost:9093/api/v1/silences \
    -H "Content-Type: application/json" \
    -d "$silence"
done

# 重启服务
docker-compose restart prometheus alertmanager

echo "恢复完成"
```

### 监控系统自监控

#### Prometheus自监控
```bash
# 检查Prometheus健康状态
curl http://localhost:9090/-/healthy

# 检查数据采集状态
curl 'http://localhost:9090/api/v1/targets' | jq '.data.activeTargets[] | {job: .labels.job, health: .health}'

# 检查规则评估状态
curl 'http://localhost:9090/api/v1/rules' | jq '.data.groups[].rules[] | {alert: .name, state: .state}'

# 检查存储状态
curl 'http://localhost:9090/api/v1/status/tsdb' | jq '.data'
```

#### Alertmanager自监控
```bash
# 检查Alertmanager健康状态
curl http://localhost:9093/-/healthy

# 检查集群状态（如果使用集群模式）
curl 'http://localhost:9093/api/v1/status' | jq '.data.cluster'

# 检查通知队列
curl 'http://localhost:9093/api/v1/status' | jq '.data'
```

### 日志管理

#### 查看服务日志
```bash
# Prometheus日志
docker logs prometheus -f --tail 100

# Alertmanager日志
docker logs alertmanager -f --tail 100

# 查看特定时间段的日志
docker logs prometheus --since 2h | grep ERROR
```

#### 日志分析
```bash
# 统计错误日志
docker logs prometheus 2>&1 | grep -i error | wc -l

# 查看最近的告警发送日志
docker logs alertmanager 2>&1 | grep "Notify" | tail -20

# 分析告警处理延迟
docker logs alertmanager 2>&1 | grep "time_elapsed"
```

## 附录

### 常用命令速查

```bash
# Prometheus
curl http://localhost:9090/api/v1/query?query=<QUERY>
curl http://localhost:9090/api/v1/alerts
curl http://localhost:9090/api/v1/rules
promtool check config prometheus.yml
promtool check rules alert.rules.yml

# Alertmanager
curl http://localhost:9093/api/v1/alerts
curl http://localhost:9093/api/v1/silences
amtool alert query
amtool silence query
amtool check-config alertmanager.yml

# Grafana
curl http://admin:admin@localhost:3000/api/alerts
curl http://admin:admin@localhost:3000/api/search?query=%
```

### 性能基线快速查询

```bash
# RAG查询性能
curl -s 'http://localhost:9090/api/v1/query?query=histogram_quantile(0.99,rate(rag_query_latency_seconds_bucket[5m]))' | jq -r '.data.result[0].value[1]'

# 缓存命中率
curl -s 'http://localhost:9090/api/v1/query?query=rate(rag_cache_hits_total[5m])/(rate(rag_cache_hits_total[5m])+rate(rag_cache_misses_total[5m]))' | jq -r '.data.result[0].value[1]'

# Milvus性能
curl -s 'http://localhost:9090/api/v1/query?query=histogram_quantile(0.99,rate(milvus_query_latency_seconds_bucket[5m]))' | jq -r '.data.result[0].value[1]'

# 内存使用率
curl -s 'http://localhost:9090/api/v1/query?query=jvm_memory_used_bytes{area="heap"}/jvm_memory_max_bytes{area="heap"}' | jq '.data.result[]'
```

### 联系方式

| 角色 | 姓名 | 邮箱 | 电话 | 企业微信 |
|------|------|------|------|----------|
| SRE负责人 | - | sre-lead@example.com | - | - |
| DBA负责人 | - | dba-lead@example.com | - | - |
| ML团队负责人 | - | ml-lead@example.com | - | - |
| On-call值班 | - | oncall@example.com | - | - |

---

**文档版本**: v1.0  
**最后更新**: 2024-01-01  
**维护团队**: SRE团队
