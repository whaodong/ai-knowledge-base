# 自动化性能回归测试体系

## 📋 概述

本系统实现了完整的自动化性能回归测试体系，使用Gatling进行性能测试，GitHub Actions进行CI/CD集成。

## 🏗️ 体系架构

```
┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│  Gatling     │───▶│  Baseline    │───▶│   GitHub     │
│  测试脚本    │    │  基准数据    │    │   Actions    │
└──────────────┘    └──────────────┘    └──────────────┘
```

## 📊 性能基准

| 服务 | 并发 | P99 | 成功率 |
|------|------|-----|--------|
| RAG查询 | 100 | < 3s | > 95% |
| 文档上传 | 50 | < 5s | > 90% |
| 向量化 | 20 | < 2s | > 95% |
| Milvus检索 | 200 | < 100ms | > 99% |

## 🚀 快速开始

```bash
# 运行性能测试
cd gatling
mvn gatling:test

# 性能对比
python3 scripts/performance-compare.py \
  --baseline performance-baseline/rag-query-baseline.json \
  --current results.json \
  --output-dir reports/
```

## 📁 目录结构

- `gatling/`: Gatling性能测试脚本
- `performance-baseline/`: 性能基准数据
- `scripts/`: 工具脚本
- `.github/workflows/`: GitHub Actions配置
