package com.example.rag.health;

import io.milvus.client.MilvusServiceClient;
import io.milvus.grpc.CheckHealthResponse;
import io.milvus.param.R;
import io.milvus.param.collection.HasCollectionParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Milvus健康检查指示器
 * 检查Milvus向量数据库的连接状态和健康状况
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MilvusHealthIndicator implements HealthIndicator {

    private final MilvusServiceClient milvusClient;

    @Override
    public Health health() {
        try {
            // 检查Milvus服务健康状态
            R<CheckHealthResponse> response = milvusClient.checkHealth();
            
            if (response.getStatus() == R.Status.Success.getCode()) {
                CheckHealthResponse healthResponse = response.getData();
                
                if (healthResponse.getIsHealthy()) {
                    Health.Builder builder = Health.up()
                        .withDetail("status", "healthy")
                        .withDetail("reasons", healthResponse.getReasonsList());
                    
                    // 检查是否有可用的collection
                    try {
                        R<Boolean> hasCollection = milvusClient.hasCollection(
                            HasCollectionParam.newBuilder()
                                .withCollectionName("knowledge_vectors")
                                .build()
                        );
                        builder.withDetail("collection_exists", hasCollection.getData());
                    } catch (Exception e) {
                        log.warn("Failed to check collection existence", e);
                        builder.withDetail("collection_exists", "unknown");
                    }
                    
                    return builder.build();
                } else {
                    return Health.down()
                        .withDetail("status", "unhealthy")
                        .withDetail("reasons", healthResponse.getReasonsList())
                        .build();
                }
            } else {
                return Health.down()
                    .withDetail("error", "Failed to check Milvus health")
                    .withDetail("status_code", response.getStatus())
                    .build();
            }
        } catch (Exception e) {
            log.error("Milvus health check failed", e);
            return Health.down()
                .withDetail("error", e.getMessage())
                .withException(e)
                .build();
        }
    }
}
