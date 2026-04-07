package com.example.milvus.health;

import io.milvus.client.MilvusServiceClient;
import io.milvus.grpc.CheckHealthResponse;
import io.milvus.param.R;
import io.milvus.param.collection.HasCollectionParam;
import io.milvus.param.collection.ShowCollectionsParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.List;

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
                    
                    // 获取所有collection信息
                    try {
                        R<io.milvus.grpc.ShowCollectionsResponse> collectionsResponse = 
                            milvusClient.showCollections(ShowCollectionsParam.newBuilder().build());
                        
                        if (collectionsResponse.getStatus() == R.Status.Success.getCode()) {
                            List<String> collectionNames = collectionsResponse.getData().getCollectionNamesList();
                            builder.withDetail("collections", collectionNames)
                                   .withDetail("collection_count", collectionNames.size());
                        }
                    } catch (Exception e) {
                        log.warn("Failed to get collection list", e);
                        builder.withDetail("collections", "unable to retrieve");
                    }
                    
                    // 检查默认collection
                    try {
                        R<Boolean> hasCollection = milvusClient.hasCollection(
                            HasCollectionParam.newBuilder()
                                .withCollectionName("knowledge_vectors")
                                .build()
                        );
                        builder.withDetail("default_collection_exists", hasCollection.getData());
                    } catch (Exception e) {
                        log.debug("Default collection check skipped", e);
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
