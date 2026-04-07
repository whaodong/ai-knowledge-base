package com.example.document.service;

import com.example.common.dto.PageResponse;
import com.example.document.dto.*;
import com.example.document.entity.Document;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文档服务接口
 */
public interface DocumentService {

    /**
     * 上传文档
     */
    Document uploadDocument(MultipartFile file, DocumentUploadRequest request);

    /**
     * 批量上传文档
     */
    DocumentBatchUploadResponse batchUploadDocuments(MultipartFile[] files, DocumentBatchUploadRequest request);

    /**
     * 获取文档详情
     */
    Document getDocumentById(Long id);

    /**
     * 分页查询文档
     */
    PageResponse<Document> queryDocuments(DocumentQueryRequest request);

    /**
     * 删除文档
     */
    void deleteDocument(Long id);

    /**
     * 批量删除文档
     */
    void batchDeleteDocuments(Long[] ids);
}
