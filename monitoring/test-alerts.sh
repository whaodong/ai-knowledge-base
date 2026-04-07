#!/bin/bash
# AI知识库系统告警测试脚本
# 用于验证告警规则和通知渠道

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 配置
PROMETHEUS_URL="http://localhost:9090"
ALERTMANAGER_URL="http://localhost:9093"
WEBHOOK_URL="http://localhost:5001/webhook"

# 日志函数
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 检查服务状态
check_services() {
    log_info "检查服务状态..."
    
    # 检查Prometheus
    if curl -s "$PROMETHEUS_URL/-/healthy" > /dev/null; then
        log_success "Prometheus运行正常"
    else
        log_error "Prometheus未运行或无法访问"
        return 1
    fi
    
    # 检查Alertmanager
    if curl -s "$ALERTMANAGER_URL/-/healthy" > /dev/null; then
        log_success "Alertmanager运行正常"
    else
        log_error "Alertmanager未运行或无法访问"
        return 1
    fi
    
    log_success "所有服务检查通过"
}

# 测试P0级别告警
test_p0_alerts() {
    log_info "测试P0级别告警..."
    
    # 模拟服务宕机
    log_info "模拟服务宕机告警..."
    curl -X POST "$ALERTMANAGER_URL/api/v1/alerts" -H "Content-Type: application/json" -d '[
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
    
    log_success "P0告警已发送"
    read -p "按回车继续..."
}

# 测试P1级别告警
test_p1_alerts() {
    log_info "测试P1级别告警..."
    
    # 模拟高内存使用
    log_info "模拟高内存使用告警..."
    curl -X POST "$ALERTMANAGER_URL/api/v1/alerts" -H "Content-Type: application/json" -d '[
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
    
    log_success "P1告警已发送"
    read -p "按回车继续..."
}

# 测试P2级别告警
test_p2_alerts() {
    log_info "测试P2级别告警..."
    
    # 模拟缓存命中率低
    log_info "模拟缓存命中率低告警..."
    curl -X POST "$ALERTMANAGER_URL/api/v1/alerts" -H "Content-Type: application/json" -d '[
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
                "action": "检查缓存策略、缓存容量、热点数据",
                "baseline": "performance-baselines.yml: rag_service.cache.hit_rate=0.7"
            },
            "startsAt": "'$(date -u +"%Y-%m-%dT%H:%M:%SZ")'"
        }
    ]'
    
    log_success "P2告警已发送"
    read -p "按回车继续..."
}

# 测试P3级别告警
test_p3_alerts() {
    log_info "测试P3级别告警..."
    
    # 模拟资源使用预警
    log_info "模拟资源使用预警告警..."
    curl -X POST "$ALERTMANAGER_URL/api/v1/alerts" -H "Content-Type: application/json" -d '[
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
    
    log_success "P3告警已发送"
    read -p "按回车继续..."
}

# 测试告警恢复
test_alert_resolution() {
    log_info "测试告警恢复..."
    
    curl -X POST "$ALERTMANAGER_URL/api/v1/alerts" -H "Content-Type: application/json" -d '[
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
    
    log_success "告警恢复通知已发送"
}

# 查询告警状态
query_alerts() {
    log_info "查询当前告警状态..."
    
    # Prometheus告警规则
    log_info "Prometheus告警规则:"
    curl -s "$PROMETHEUS_URL/api/v1/rules" | jq '.data.groups[].rules[] | select(.state=="firing") | {alertname: .name, state: .state, severity: .labels.severity}'
    
    # Alertmanager告警
    log_info "Alertmanager活动告警:"
    curl -s "$ALERTMANAGER_URL/api/v1/alerts" | jq '.data[] | {alertname: .labels.alertname, status: .status.state}'
    
    # Alertmanager静默
    log_info "Alertmanager静默规则:"
    curl -s "$ALERTMANAGER_URL/api/v1/silences" | jq '.data[] | {id: .id, comment: .comment, status: .status.state}'
}

# 创建静默规则
create_silence() {
    log_info "创建静默规则..."
    
    read -p "输入告警名称 (例如: HighMemoryUsage): " alertname
    read -p "输入实例 (例如: rag-service:8083): " instance
    read -p "输入持续时间(小时，默认1): " duration
    duration=${duration:-1}
    read -p "输入原因: " comment
    
    end_time=$(date -u -d "+${duration} hours" +"%Y-%m-%dT%H:%M:%SZ")
    
    curl -X POST "$ALERTMANAGER_URL/api/v1/silences" -H "Content-Type: application/json" -d '{
        "matchers": [
            {
                "name": "alertname",
                "value": "'$alertname'",
                "isRegex": false
            },
            {
                "name": "instance",
                "value": "'$instance'",
                "isRegex": false
            }
        ],
        "startsAt": "'$(date -u +"%Y-%m-%dT%H:%M:%SZ")'",
        "endsAt": "'$end_time'",
        "createdBy": "admin@ai-knowledge-base.com",
        "comment": "'$comment'"
    }'
    
    log_success "静默规则已创建"
}

# 测试通知渠道
test_notification_channels() {
    log_info "测试通知渠道..."
    
    echo "1. 测试邮件通知"
    echo "2. 测试Slack通知"
    echo "3. 测试企业微信通知"
    echo "4. 测试钉钉通知"
    echo "5. 测试Webhook"
    read -p "选择要测试的渠道: " choice
    
    case $choice in
        1)
            log_info "发送测试邮件..."
            curl -X POST "$ALERTMANAGER_URL/api/v1/alerts" -H "Content-Type: application/json" -d '[
                {
                    "labels": {
                        "alertname": "TestEmailNotification",
                        "severity": "info",
                        "priority": "P3",
                        "team": "sre"
                    },
                    "annotations": {
                        "summary": "邮件通知测试",
                        "description": "这是一封测试邮件，验证邮件通知渠道是否正常工作"
                    },
                    "startsAt": "'$(date -u +"%Y-%m-%dT%H:%M:%SZ")'"
                }
            ]'
            ;;
        2)
            log_info "发送测试Slack消息..."
            # 直接测试Slack webhook
            curl -X POST "https://hooks.slack.com/services/YOUR/SLACK/WEBHOOK" \
                -H "Content-Type: application/json" \
                -d '{"text": "测试消息：Slack通知渠道测试成功"}'
            ;;
        3)
            log_info "发送测试企业微信消息..."
            curl -X POST "https://qyapi.weixin.qq.com/cgi-bin/message/send?access_token=YOUR_TOKEN" \
                -H "Content-Type: application/json" \
                -d '{"touser": "@all", "msgtype": "text", "text": {"content": "测试消息：企业微信通知渠道测试成功"}}'
            ;;
        4)
            log_info "发送测试钉钉消息..."
            curl -X POST "https://oapi.dingtalk.com/robot/send?access_token=YOUR_TOKEN" \
                -H "Content-Type: application/json" \
                -d '{"msgtype": "text", "text": {"content": "测试消息：钉钉通知渠道测试成功"}}'
            ;;
        5)
            log_info "发送测试Webhook..."
            curl -X POST "$WEBHOOK_URL" -H "Content-Type: application/json" -d '{
                "test": true,
                "message": "Webhook通知渠道测试"
            }'
            ;;
        *)
            log_warning "无效选择"
            ;;
    esac
    
    log_success "测试完成"
}

# 性能基线验证
verify_baselines() {
    log_info "验证性能基线..."
    
    # RAG查询P99延迟
    log_info "检查RAG查询P99延迟..."
    rag_p99=$(curl -s "$PROMETHEUS_URL/api/v1/query?query=histogram_quantile(0.99,rate(rag_query_latency_seconds_bucket[5m]))" | jq -r '.data.result[0].value[1]')
    if [ ! -z "$rag_p99" ]; then
        if (( $(echo "$rag_p99 > 3" | bc -l) )); then
            log_warning "RAG查询P99延迟 ($rag_p99 秒) 超过基线 (3秒)"
        else
            log_success "RAG查询P99延迟 ($rag_p99 秒) 在基线范围内"
        fi
    fi
    
    # 缓存命中率
    log_info "检查缓存命中率..."
    cache_hit=$(curl -s "$PROMETHEUS_URL/api/v1/query?query=rate(rag_cache_hits_total[5m])/(rate(rag_cache_hits_total[5m])+rate(rag_cache_misses_total[5m]))" | jq -r '.data.result[0].value[1]')
    if [ ! -z "$cache_hit" ]; then
        if (( $(echo "$cache_hit < 0.7" | bc -l) )); then
            log_warning "缓存命中率 ($cache_hit) 低于基线 (0.7)"
        else
            log_success "缓存命中率 ($cache_hit) 达到基线要求"
        fi
    fi
    
    # Milvus查询延迟
    log_info "检查Milvus查询延迟..."
    milvus_latency=$(curl -s "$PROMETHEUS_URL/api/v1/query?query=histogram_quantile(0.99,rate(milvus_query_latency_seconds_bucket[5m]))" | jq -r '.data.result[0].value[1]')
    if [ ! -z "$milvus_latency" ]; then
        if (( $(echo "$milvus_latency > 0.3" | bc -l) )); then
            log_warning "Milvus查询P99延迟 ($milvus_latency 秒) 超过基线 (0.3秒)"
        else
            log_success "Milvus查询P99延迟 ($milvus_latency 秒) 在基线范围内"
        fi
    fi
}

# 主菜单
main_menu() {
    while true; do
        echo ""
        echo "==================================="
        echo "  AI知识库告警系统测试"
        echo "==================================="
        echo "1. 检查服务状态"
        echo "2. 测试P0级别告警"
        echo "3. 测试P1级别告警"
        echo "4. 测试P2级别告警"
        echo "5. 测试P3级别告警"
        echo "6. 测试告警恢复"
        echo "7. 查询告警状态"
        echo "8. 创建静默规则"
        echo "9. 测试通知渠道"
        echo "10. 验证性能基线"
        echo "11. 运行所有测试"
        echo "0. 退出"
        echo "==================================="
        read -p "请选择操作: " choice
        
        case $choice in
            1) check_services ;;
            2) test_p0_alerts ;;
            3) test_p1_alerts ;;
            4) test_p2_alerts ;;
            5) test_p3_alerts ;;
            6) test_alert_resolution ;;
            7) query_alerts ;;
            8) create_silence ;;
            9) test_notification_channels ;;
            10) verify_baselines ;;
            11)
                check_services
                test_p0_alerts
                test_p1_alerts
                test_p2_alerts
                test_p3_alerts
                test_alert_resolution
                verify_baselines
                ;;
            0)
                log_info "退出程序"
                exit 0
                ;;
            *)
                log_warning "无效选择，请重新输入"
                ;;
        esac
    done
}

# 主函数
main() {
    echo -e "${GREEN}"
    echo "====================================="
    echo "  AI知识库系统告警测试工具"
    echo "====================================="
    echo -e "${NC}"
    
    # 检查依赖
    if ! command -v jq &> /dev/null; then
        log_warning "jq未安装，部分功能可能无法正常使用"
    fi
    
    if ! command -v bc &> /dev/null; then
        log_warning "bc未安装，性能基线验证功能可能无法正常使用"
    fi
    
    # 运行主菜单
    main_menu
}

# 运行主函数
main "$@"
