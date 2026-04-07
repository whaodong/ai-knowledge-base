package com.example.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.io.Serializable;

/**
 * 分页请求参数
 */
@Data
@Schema(description = "分页请求参数")
public class PageRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "页码，从1开始", example = "1", defaultValue = "1")
    @Min(value = 1, message = "页码不能小于1")
    private Integer pageNum = 1;

    @Schema(description = "每页大小", example = "10", defaultValue = "10")
    @Min(value = 1, message = "每页大小不能小于1")
    @Max(value = 100, message = "每页大小不能超过100")
    private Integer pageSize = 10;

    @Schema(description = "排序字段", example = "createTime")
    private String sortBy;

    @Schema(description = "排序方向，ASC或DESC", example = "DESC", defaultValue = "DESC")
    private String sortOrder = "DESC";

    /**
     * 获取偏移量
     */
    public long getOffset() {
        return (long) (pageNum - 1) * pageSize;
    }

    /**
     * 获取Spring Data的Pageable对象
     */
    public org.springframework.data.domain.Pageable toPageable() {
        if (sortBy == null || sortBy.isEmpty()) {
            return org.springframework.data.domain.PageRequest.of(pageNum - 1, pageSize);
        }
        
        org.springframework.data.domain.Sort.Direction direction = 
            "ASC".equalsIgnoreCase(sortOrder) 
                ? org.springframework.data.domain.Sort.Direction.ASC 
                : org.springframework.data.domain.Sort.Direction.DESC;
        
        return org.springframework.data.domain.PageRequest.of(
            pageNum - 1, 
            pageSize, 
            org.springframework.data.domain.Sort.by(direction, sortBy)
        );
    }
}
