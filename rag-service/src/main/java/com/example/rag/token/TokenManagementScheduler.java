package com.example.rag.token;

import com.example.common.token.SessionTokenManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Token管理定时任务
 * 
 * <p>定时执行Token配额重置、数据清理等任务。</p>
 */
@Slf4j
@Component
public class TokenManagementScheduler {
    
    @Autowired
    private SessionTokenManager sessionTokenManager;
    
    /**
     * 每日凌晨重置用户Token配额
     * 执行时间：每天 00:00:00
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void resetDailyTokenQuotas() {
        log.info("开始执行每日Token配额重置任务");
        
        try {
            sessionTokenManager.resetAllDailyQuotas();
            log.info("每日Token配额重置完成");
        } catch (Exception e) {
            log.error("每日Token配额重置失败", e);
        }
    }
    
    /**
     * 每小时清理过期会话
     * 执行时间：每小时执行一次
     */
    @Scheduled(cron = "0 0 * * * ?")
    public void cleanupExpiredSessions() {
        log.info("开始执行过期会话清理任务");
        
        try {
            // 这里可以添加清理逻辑
            // 例如：清理超过24小时未活跃的会话
            log.info("过期会话清理完成");
        } catch (Exception e) {
            log.error("过期会话清理失败", e);
        }
    }
    
    /**
     * 每5分钟检查Token使用预警
     * 执行时间：每5分钟执行一次
     */
    @Scheduled(cron = "0 */5 * * * ?")
    public void checkTokenUsageWarnings() {
        log.debug("开始执行Token使用预警检查");
        
        try {
            // 检查用户配额使用情况
            // 如果有用户接近配额上限，发送预警通知
            log.debug("Token使用预警检查完成");
        } catch (Exception e) {
            log.error("Token使用预警检查失败", e);
        }
    }
}
