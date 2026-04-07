package com.example.milvus.controller;

import com.example.milvus.index.IndexMonitorService;
import com.example.milvus.index.IndexStrategySelector;
import com.example.milvus.index.VectorIndexConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 索引管理控制器
 * 提供索引优化、监控、管理的REST API
 * 
 * @author AI Knowledge Base
 * @version 1.0
 */
@Slf4j
@RestController
@RequestMapping("/api/index")
@RequiredArgsConstructor
public class IndexManagementController {

    private final IndexStrategySelector indexStrategySelector;
    private final IndexMonitorService indexMonitorService;
    private final VectorIndexConfig vectorIndexConfig;

    /**
     * 获取索引推荐配置
     * 
     * @param collectionName 集合名称
     * @param dimension 向量维度
     * @return 索引推荐结果
     */
    @GetMapping("/recommendation")
    public ResponseEntity<Map<String, Object>> getIndexRecommendation(
            @RequestParam String collectionName,
            @RequestParam(defaultValue = "1536") int dimension) {
        
        log.info("获取索引推荐: collection={}, dimension={}", collectionName, dimension);
        
        try {
            IndexStrategySelector.IndexSelectionResult result = 
                    indexStrategySelector.getIndexRecommendation(collectionName, dimension);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", Map.of(
                    "indexType", result.getIndexType().getCode(),
                    "indexParams", result.getIndexParams(),
                    "searchParams", result.getSearchParams(),
                    "reason", result.getReason()
            ));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取索引推荐失败", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "获取索引推荐失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 应用索引推荐
     * 
     * @param collectionName 集合名称
     * @param fieldName 向量字段名
     * @param dimension 向量维度
     * @return 应用结果
     */
    @PostMapping("/apply")
    public ResponseEntity<Map<String, Object>> applyIndexRecommendation(
            @RequestParam String collectionName,
            @RequestParam(defaultValue = "embedding") String fieldName,
            @RequestParam(defaultValue = "1536") int dimension) {
        
        log.info("应用索引推荐: collection={}, field={}, dimension={}", 
                collectionName, fieldName, dimension);
        
        try {
            IndexStrategySelector.IndexSelectionResult result = 
                    indexStrategySelector.selectOptimalIndex(collectionName, dimension);
            
            boolean success = indexStrategySelector.applyIndexSelection(
                    collectionName, 
                    fieldName, 
                    "vector_index", 
                    result
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", success);
            if (success) {
                response.put("message", "索引应用成功");
                response.put("data", Map.of(
                        "indexType", result.getIndexType().getCode(),
                        "indexParams", result.getIndexParams()
                ));
            } else {
                response.put("message", "索引应用失败");
            }
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("应用索引推荐失败", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "应用索引推荐失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 获取索引监控数据
     * 
     * @param collectionName 集合名称
     * @param indexName 索引名称
     * @return 监控数据
     */
    @GetMapping("/monitor")
    public ResponseEntity<Map<String, Object>> getIndexMonitorData(
            @RequestParam String collectionName,
            @RequestParam(defaultValue = "vector_index") String indexName) {
        
        log.info("获取索引监控数据: collection={}, index={}", collectionName, indexName);
        
        try {
            IndexMonitorService.IndexMonitorData data = 
                    indexMonitorService.getIndexMonitorData(collectionName, indexName);
            
            if (data == null) {
                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "message", "未找到索引监控数据"
                ));
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", Map.of(
                    "collectionName", data.getCollectionName(),
                    "indexName", data.getIndexName(),
                    "indexType", data.getIndexType(),
                    "totalRows", data.getTotalRows(),
                    "indexedRows", data.getIndexedRows(),
                    "buildProgress", data.getBuildProgress(),
                    "healthStatus", data.getHealthStatus().name(),
                    "healthMessage", data.getHealthMessage(),
                    "lastUpdateTime", data.getLastUpdateTime()
            ));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("获取索引监控数据失败", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "获取索引监控数据失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 获取所有索引监控数据
     * 
     * @return 所有监控数据
     */
    @GetMapping("/monitor/all")
    public ResponseEntity<Map<String, Object>> getAllMonitorData() {
        log.info("获取所有索引监控数据");
        
        try {
            Map<String, IndexMonitorService.IndexMonitorData> allData = 
                    indexMonitorService.getAllMonitorData();
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", allData
            ));
        } catch (Exception e) {
            log.error("获取所有索引监控数据失败", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "获取所有索引监控数据失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 获取索引优化建议
     * 
     * @param collectionName 集合名称
     * @param dimension 向量维度
     * @return 优化建议列表
     */
    @GetMapping("/optimization-suggestions")
    public ResponseEntity<Map<String, Object>> getOptimizationSuggestions(
            @RequestParam String collectionName,
            @RequestParam(defaultValue = "1536") int dimension) {
        
        log.info("获取索引优化建议: collection={}, dimension={}", collectionName, dimension);
        
        try {
            List<IndexMonitorService.OptimizationSuggestion> suggestions = 
                    indexMonitorService.getOptimizationSuggestions(collectionName, dimension);
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", suggestions,
                    "count", suggestions.size()
            ));
        } catch (Exception e) {
            log.error("获取索引优化建议失败", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "获取索引优化建议失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 获取性能历史
     * 
     * @param collectionName 集合名称
     * @return 性能历史记录
     */
    @GetMapping("/performance-history")
    public ResponseEntity<Map<String, Object>> getPerformanceHistory(
            @RequestParam String collectionName) {
        
        log.info("获取性能历史: collection={}", collectionName);
        
        try {
            List<IndexMonitorService.PerformanceSnapshot> history = 
                    indexMonitorService.getPerformanceHistory(collectionName);
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", history,
                    "count", history.size()
            ));
        } catch (Exception e) {
            log.error("获取性能历史失败", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "获取性能历史失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 记录性能快照
     * 
     * @param collectionName 集合名称
     * @param queryLatencyMs 查询延迟(毫秒)
     * @param throughput 吞吐量
     * @return 操作结果
     */
    @PostMapping("/performance-snapshot")
    public ResponseEntity<Map<String, Object>> recordPerformanceSnapshot(
            @RequestParam String collectionName,
            @RequestParam long queryLatencyMs,
            @RequestParam double throughput) {
        
        log.info("记录性能快照: collection={}, latency={}ms, throughput={}", 
                collectionName, queryLatencyMs, throughput);
        
        try {
            indexMonitorService.recordPerformanceSnapshot(collectionName, queryLatencyMs, throughput);
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "性能快照记录成功"
            ));
        } catch (Exception e) {
            log.error("记录性能快照失败", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "记录性能快照失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 导出监控报告
     * 
     * @return 监控报告文本
     */
    @GetMapping("/report")
    public ResponseEntity<Map<String, Object>> exportMonitorReport() {
        log.info("导出索引监控报告");
        
        try {
            String report = indexMonitorService.exportMonitorReport();
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", report
            ));
        } catch (Exception e) {
            log.error("导出监控报告失败", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "导出监控报告失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 获取索引配置信息
     * 
     * @return 当前索引配置
     */
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> getIndexConfig() {
        log.info("获取索引配置信息");
        
        try {
            Map<String, Object> config = new HashMap<>();
            config.put("ivfFlat", vectorIndexConfig.getIvfFlat());
            config.put("hnsw", vectorIndexConfig.getHnsw());
            config.put("ivfPq", vectorIndexConfig.getIvfPq());
            config.put("autoSelect", vectorIndexConfig.getAutoSelect());
            config.put("rebuildThreshold", vectorIndexConfig.getRebuildThreshold());
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", config
            ));
        } catch (Exception e) {
            log.error("获取索引配置失败", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "获取索引配置失败: " + e.getMessage()
            ));
        }
    }

    /**
     * 获取推荐的索引参数
     * 
     * @param indexType 索引类型
     * @param vectorCount 向量数量
     * @param dimension 向量维度
     * @return 推荐参数
     */
    @GetMapping("/recommended-params")
    public ResponseEntity<Map<String, Object>> getRecommendedParams(
            @RequestParam String indexType,
            @RequestParam long vectorCount,
            @RequestParam(defaultValue = "1536") int dimension) {
        
        log.info("获取推荐参数: type={}, count={}, dimension={}", indexType, vectorCount, dimension);
        
        try {
            Map<String, Object> params = new HashMap<>();
            
            switch (indexType.toUpperCase()) {
                case "IVF_FLAT":
                    params = vectorIndexConfig.getIvfFlatParams(vectorCount);
                    break;
                case "HNSW":
                    params = vectorIndexConfig.getHnswParams(0.95);
                    break;
                case "IVF_PQ":
                    params = vectorIndexConfig.getIvfPqParams(vectorCount, dimension);
                    break;
                case "FLAT":
                    // FLAT不需要额外参数
                    break;
                default:
                    return ResponseEntity.badRequest().body(Map.of(
                            "success", false,
                            "message", "不支持的索引类型: " + indexType
                    ));
            }
            
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", params
            ));
        } catch (Exception e) {
            log.error("获取推荐参数失败", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "获取推荐参数失败: " + e.getMessage()
            ));
        }
    }
}
