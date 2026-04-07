package com.example.common.enums;

import lombok.Getter;

/**
 * 错误码枚举
 */
@Getter
public enum ErrorCode {

    // 通用错误 1xxx
    SUCCESS(200, "操作成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "禁止访问"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "方法不允许"),
    CONFLICT(409, "资源冲突"),
    INTERNAL_SERVER_ERROR(500, "服务器内部错误"),
    SERVICE_UNAVAILABLE(503, "服务不可用"),

    // 文档服务错误 2xxx
    DOCUMENT_NOT_FOUND(2001, "文档不存在"),
    DOCUMENT_UPLOAD_FAILED(2002, "文档上传失败"),
    DOCUMENT_PARSE_FAILED(2003, "文档解析失败"),
    DOCUMENT_DELETE_FAILED(2004, "文档删除失败"),
    DOCUMENT_ALREADY_EXISTS(2005, "文档已存在"),
    INVALID_FILE_FORMAT(2006, "不支持的文件格式"),
    FILE_SIZE_EXCEEDED(2007, "文件大小超出限制"),

    // 向量化服务错误 3xxx
    EMBEDDING_GENERATION_FAILED(3001, "向量生成失败"),
    EMBEDDING_TASK_NOT_FOUND(3002, "向量化任务不存在"),
    EMBEDDING_BATCH_FAILED(3003, "批量向量化失败"),
    EMBEDDING_MODEL_NOT_AVAILABLE(3004, "向量化模型不可用"),

    // RAG服务错误 4xxx
    RAG_QUERY_FAILED(4001, "RAG查询失败"),
    RAG_SESSION_NOT_FOUND(4002, "会话不存在"),
    RAG_RETRIEVAL_FAILED(4003, "检索失败"),
    RAG_RERANK_FAILED(4004, "重排序失败"),
    INVALID_QUERY(4005, "无效的查询参数"),

    // Milvus服务错误 5xxx
    MILVUS_CONNECTION_FAILED(5001, "Milvus连接失败"),
    MILVUS_COLLECTION_NOT_FOUND(5002, "集合不存在"),
    MILVUS_INSERT_FAILED(5003, "向量插入失败"),
    MILVUS_SEARCH_FAILED(5004, "向量搜索失败"),
    MILVUS_DELETE_FAILED(5005, "向量删除失败");

    private final Integer code;
    private final String message;

    ErrorCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
