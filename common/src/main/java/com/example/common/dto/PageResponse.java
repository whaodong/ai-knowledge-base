package com.example.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 分页响应结果
 * 
 * @param <T> 数据类型
 */
@Data
@Schema(description = "分页响应结果")
public class PageResponse<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "数据列表")
    private List<T> records;

    @Schema(description = "总记录数", example = "100")
    private Long total;

    @Schema(description = "当前页码", example = "1")
    private Integer pageNum;

    @Schema(description = "每页大小", example = "10")
    private Integer pageSize;

    @Schema(description = "总页数", example = "10")
    private Integer totalPages;

    @Schema(description = "是否有下一页")
    private Boolean hasNext;

    @Schema(description = "是否有上一页")
    private Boolean hasPrevious;

    private PageResponse() {
    }

    /**
     * 从Spring Data的Page对象构建
     */
    public static <T> PageResponse<T> of(org.springframework.data.domain.Page<T> page) {
        PageResponse<T> response = new PageResponse<>();
        response.setRecords(page.getContent());
        response.setTotal(page.getTotalElements());
        response.setPageNum(page.getNumber() + 1);
        response.setPageSize(page.getSize());
        response.setTotalPages(page.getTotalPages());
        response.setHasNext(page.hasNext());
        response.setHasPrevious(page.hasPrevious());
        return response;
    }

    /**
     * 手动构建
     */
    public static <T> PageResponse<T> of(List<T> records, Long total, Integer pageNum, Integer pageSize) {
        PageResponse<T> response = new PageResponse<>();
        response.setRecords(records);
        response.setTotal(total);
        response.setPageNum(pageNum);
        response.setPageSize(pageSize);
        response.setTotalPages((int) Math.ceil((double) total / pageSize));
        response.setHasNext(pageNum < response.getTotalPages());
        response.setHasPrevious(pageNum > 1);
        return response;
    }

    /**
     * 空分页结果
     */
    public static <T> PageResponse<T> empty() {
        PageResponse<T> response = new PageResponse<>();
        response.setRecords(Collections.emptyList());
        response.setTotal(0L);
        response.setPageNum(1);
        response.setPageSize(10);
        response.setTotalPages(0);
        response.setHasNext(false);
        response.setHasPrevious(false);
        return response;
    }
}
