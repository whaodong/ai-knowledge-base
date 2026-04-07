#!/bin/bash

# 分布式追踪系统测试脚本

echo "========================================"
echo "分布式追踪系统测试"
echo "========================================"

BASE_URL="http://localhost:8080"
ZIPKIN_URL="http://localhost:9411"

# 1. 检查Zipkin是否运行
echo -e "\n[1] 检查Zipkin服务..."
if curl -s "$ZIPKIN_URL/health" | grep -q "UP"; then
    echo "✓ Zipkin服务正常运行"
else
    echo "✗ Zipkin服务未启动，请先运行: cd monitoring/tracing && docker-compose up -d zipkin"
    exit 1
fi

# 2. 检查应用服务
echo -e "\n[2] 检查应用服务..."
if curl -s "$BASE_URL/actuator/health" | grep -q "UP"; then
    echo "✓ 应用服务正常运行"
else
    echo "✗ 应用服务未启动，请先启动应用"
    exit 1
fi

# 3. 测试追踪API
echo -e "\n[3] 测试追踪API..."

# 获取当前Trace ID
echo "  - 获取当前追踪信息..."
TRACE_INFO=$(curl -s "$BASE_URL/api/v1/tracing/current-trace")
echo "    响应: $TRACE_INFO"

# 获取统计信息
echo "  - 获取追踪统计..."
STATS=$(curl -s "$BASE_URL/api/v1/tracing/stats")
echo "    响应: ${STATS:0:100}..."

# 4. 生成追踪数据
echo -e "\n[4] 生成追踪数据..."

# 模拟RAG查询
echo "  - 执行RAG查询..."
QUERY_RESPONSE=$(curl -s -X POST "$BASE_URL/api/v1/rag/retrieve" \
    -H "Content-Type: application/json" \
    -d '{
        "query": "什么是分布式追踪?",
        "topK": 5,
        "hybridSearch": true
    }')

# 从响应中提取Trace ID
TRACE_ID=$(echo "$QUERY_RESPONSE" | grep -o '"traceId":"[^"]*' | cut -d'"' -f4)
echo "    生成Trace ID: $TRACE_ID"

# 5. 查看追踪分析
echo -e "\n[5] 查看追踪分析结果..."

# P99延迟
echo "  - P99延迟统计..."
curl -s "$BASE_URL/api/v1/tracing/stats/p99"

# 慢请求
echo -e "\n  - 慢请求列表..."
curl -s "$BASE_URL/api/v1/tracing/slow-traces?limit=5"

# 性能瓶颈
echo -e "\n  - 性能瓶颈分析..."
curl -s "$BASE_URL/api/v1/tracing/bottlenecks"

# 6. 在Zipkin中查看
echo -e "\n[6] 在Zipkin中查看追踪..."
if [ ! -z "$TRACE_ID" ]; then
    echo "  访问: $ZIPKIN_URL/zipkin/traces/$TRACE_ID"
fi

echo -e "\n========================================"
echo "测试完成！"
echo "========================================"
echo ""
echo "访问以下地址查看详细追踪信息:"
echo "  - Zipkin UI: $ZIPKIN_URL"
echo "  - 追踪统计: $BASE_URL/api/v1/tracing/stats"
echo "  - 性能分析: $BASE_URL/api/v1/tracing/bottlenecks"
echo ""
