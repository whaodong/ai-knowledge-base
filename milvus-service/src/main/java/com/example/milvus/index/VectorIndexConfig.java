package com.example.milvus.index;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 向量索引配置类
 * 支持多种索引类型的参数配置和自动调优
 * 
 * @author AI Knowledge Base
 * @version 1.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "milvus.index")
public class VectorIndexConfig {

    /**
     * 索引类型配置
     */
    private IndexTypeProperties ivfFlat = new IndexTypeProperties();
    private IndexTypeProperties hnsw = new IndexTypeProperties();
    private IndexTypeProperties ivfPq = new IndexTypeProperties();

    /**
     * 自动选择策略配置
     */
    private AutoSelectProperties autoSelect = new AutoSelectProperties();

    /**
     * 索引重建阈值配置
     */
    private RebuildThresholdProperties rebuildThreshold = new RebuildThresholdProperties();

    /**
     * IVF_FLAT索引配置
     */
    @Data
    public static class IndexTypeProperties {
        /**
         * 是否启用
         */
        private boolean enabled = true;

        /**
         * nlist: 聚类中心数量（IVF系列索引）
         * 建议值：sqrt(n)，其中n为向量数量
         */
        private int nlist = 1024;

        /**
         * nprobe: 查询时搜索的聚类数
         * 值越大，精度越高，速度越慢
         * 建议值：nlist的1/16到1/4
         */
        private int nprobe = 64;

        /**
         * M: 每层最大连接数（HNSW索引）
         * 值越大，精度越高，内存占用越高
         * 建议值：16-48
         */
        private int m = 16;

        /**
         * efConstruction: 构建时的搜索范围（HNSW）
         * 值越大，精度越高，构建时间越长
         * 建议值：200-500
         */
        private int efConstruction = 200;

        /**
         * ef: 查询时的搜索范围（HNSW）
         * 值越大，精度越高，查询速度越慢
         * 建议值：64-200
         */
        private int ef = 64;

        /**
         * nbits: 量化位数（PQ索引）
         * 建议值：8
         */
        private int nbits = 8;

        /**
         * m PQ: 子向量数量（PQ索引）
         * 需能被向量维度整除
         */
        private int mPq = 8;

        /**
         * 自适应参数映射
         * 根据数据规模自动调整参数
         */
        private Map<Long, Map<String, Object>> adaptiveParams = new HashMap<>();
    }

    /**
     * 自动选择策略配置
     */
    @Data
    public static class AutoSelectProperties {
        /**
         * 是否启用自动选择
         */
        private boolean enabled = true;

        /**
         * 小数据集阈值（使用FLAT索引）
         */
        private long smallDatasetThreshold = 100_000L;

        /**
         * 中等数据集阈值（使用IVF_FLAT索引）
         */
        private long mediumDatasetThreshold = 1_000_000L;

        /**
         * 大数据集阈值（使用HNSW或IVF_PQ索引）
         */
        private long largeDatasetThreshold = 10_000_000L;

        /**
         * 内存限制（GB），超过后优先选择压缩索引
         */
        private double memoryLimitGB = 16.0;

        /**
         * 是否考虑内存因素
         */
        private boolean considerMemory = true;

        /**
         * 召回率要求（0-1）
         * 影响索引类型选择
         */
        private double recallRequirement = 0.95;
    }

    /**
     * 索引重建阈值配置
     */
    @Data
    public static class RebuildThresholdProperties {
        /**
         * 数据增长比例阈值
         * 当新增数据量超过此比例时建议重建
         */
        private double growthRatio = 0.3;

        /**
         * 最小重建间隔（小时）
         */
        private int minRebuildIntervalHours = 24;

        /**
         * 查询性能下降阈值
         * 当查询延迟增长超过此比例时建议重建
         */
        private double latencyGrowthRatio = 0.5;

        /**
         * 是否启用自动重建建议
         */
        private boolean autoSuggest = true;
    }

    /**
     * 索引类型枚举
     */
    public enum IndexType {
        FLAT("FLAT", "暴力搜索，精度最高，适合小数据集"),
        IVF_FLAT("IVF_FLAT", "倒排索引，平衡速度和精度，适合中等数据集"),
        HNSW("HNSW", "层次导航小世界图，高召回率，适合大数据集"),
        IVF_PQ("IVF_PQ", "倒排索引+乘积量化，高压缩率，适合超大数据集");

        private final String code;
        private final String description;

        IndexType(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public String getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 根据数据规模获取推荐的nlist值
     * 公式：sqrt(n) * 调整系数
     * 
     * @param vectorCount 向量数量
     * @return 推荐的nlist值
     */
    public int getRecommendedNlist(long vectorCount) {
        if (vectorCount < 10_000) {
            return 128;
        } else if (vectorCount < 100_000) {
            return 256;
        } else if (vectorCount < 1_000_000) {
            return 1024;
        } else if (vectorCount < 10_000_000) {
            return 4096;
        } else {
            return 8192;
        }
    }

    /**
     * 根据数据规模获取推荐的nprobe值
     * 通常为nlist的1/16到1/4
     * 
     * @param nlist nlist值
     * @return 推荐的nprobe值
     */
    public int getRecommendedNprobe(int nlist) {
        return Math.max(nlist / 16, 32);
    }

    /**
     * 根据召回率要求获取推荐的ef值
     * 
     * @param recallRequirement 召回率要求
     * @return 推荐的ef值
     */
    public int getRecommendedEf(double recallRequirement) {
        if (recallRequirement >= 0.99) {
            return 200;
        } else if (recallRequirement >= 0.95) {
            return 128;
        } else if (recallRequirement >= 0.90) {
            return 64;
        } else {
            return 32;
        }
    }

    /**
     * 获取IVF_FLAT索引参数
     */
    public Map<String, Object> getIvfFlatParams(long vectorCount) {
        Map<String, Object> params = new HashMap<>();
        int nlist = getRecommendedNlist(vectorCount);
        params.put("nlist", nlist);
        params.put("nprobe", getRecommendedNprobe(nlist));
        return params;
    }

    /**
     * 获取HNSW索引参数
     */
    public Map<String, Object> getHnswParams(double recallRequirement) {
        Map<String, Object> params = new HashMap<>();
        params.put("M", hnsw.getM());
        params.put("efConstruction", hnsw.getEfConstruction());
        params.put("ef", getRecommendedEf(recallRequirement));
        return params;
    }

    /**
     * 获取IVF_PQ索引参数
     */
    public Map<String, Object> getIvfPqParams(long vectorCount, int dimension) {
        Map<String, Object> params = new HashMap<>();
        int nlist = getRecommendedNlist(vectorCount);
        params.put("nlist", nlist);
        params.put("nprobe", getRecommendedNprobe(nlist));
        params.put("nbits", ivfPq.getNbits());
        // 确保m能被维度整除
        int m = calculateOptimalM(dimension);
        params.put("m", m);
        return params;
    }

    /**
     * 计算最优的PQ子向量数量
     */
    private int calculateOptimalM(int dimension) {
        int m = ivfPq.getMPq();
        // 确保维度能被m整除
        while (dimension % m != 0) {
            m--;
            if (m < 1) {
                m = 1;
                break;
            }
        }
        return m;
    }
}
