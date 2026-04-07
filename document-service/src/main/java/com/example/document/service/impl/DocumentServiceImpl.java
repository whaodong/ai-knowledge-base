package com.example.document.service.impl;

import com.example.common.dto.PageResponse;
import com.example.common.enums.ErrorCode;
import com.example.common.exception.BusinessException;
import com.example.document.dto.*;
import com.example.document.entity.Document;
import com.example.document.repository.DocumentRepository;
import com.example.document.service.DocumentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 文档服务实现
 */
@Slf4j
@Service
@Transactional
public class DocumentServiceImpl implements DocumentService {

    @Autowired
    private DocumentRepository documentRepository;

    @Value("${document.upload.path:/tmp/uploads}")
    private String uploadPath;

    @Value("${document.upload.max-size:10485760}")
    private Long maxFileSize;

    private static final List<String> ALLOWED_FILE_TYPES = Arrays.asList(
            "pdf", "doc", "docx", "txt", "md", "html", "xls", "xlsx", "ppt", "pptx"
    );

    @Override
    public Document uploadDocument(MultipartFile file, DocumentUploadRequest request) {
        log.info("开始上传文档: {}", file.getOriginalFilename());

        // 验证文件
        validateFile(file);

        // 计算校验和
        String checksum = calculateChecksum(file);

        // 检查文件是否已存在
        Optional<Document> existingDoc = documentRepository.findByFileNameAndChecksum(
                file.getOriginalFilename(), checksum);
        if (existingDoc.isPresent()) {
            throw new BusinessException(ErrorCode.DOCUMENT_ALREADY_EXISTS, "文档已存在");
        }

        // 保存文件
        String filePath = saveFile(file);

        // 创建文档实体
        Document document = new Document();
        document.setFileName(UUID.randomUUID().toString());
        document.setOriginalFileName(file.getOriginalFilename());
        document.setFilePath(filePath);
        document.setFileType(getFileExtension(file.getOriginalFilename()));
        document.setFileSize(file.getSize());
        document.setChecksum(checksum);
        document.setStatus(Document.DocumentStatus.UPLOADED);

        if (request.getMetadata() != null) {
            document.setMetadata(request.getMetadata().toString());
        }

        document = documentRepository.save(document);
        log.info("文档上传成功，ID: {}, 文件名: {}", document.getId(), document.getOriginalFileName());

        return document;
    }

    @Override
    public DocumentBatchUploadResponse batchUploadDocuments(MultipartFile[] files, DocumentBatchUploadRequest request) {
        log.info("开始批量上传文档，数量: {}", files.length);

        DocumentBatchUploadResponse response = new DocumentBatchUploadResponse();
        response.setTotal(files.length);
        List<Long> successIds = new ArrayList<>();
        List<String> failedFiles = new ArrayList<>();
        List<String> errorMessages = new ArrayList<>();

        for (MultipartFile file : files) {
            try {
                DocumentUploadRequest uploadRequest = new DocumentUploadRequest();
                uploadRequest.setFileName(file.getOriginalFilename());
                uploadRequest.setFileType(getFileExtension(file.getOriginalFilename()));

                Document document = uploadDocument(file, uploadRequest);
                successIds.add(document.getId());
                response.setSuccessCount(response.getSuccessCount() + 1);
            } catch (Exception e) {
                failedFiles.add(file.getOriginalFilename());
                errorMessages.add(e.getMessage());
                response.setFailedCount(response.getFailedCount() + 1);
                log.error("文档上传失败: {}", file.getOriginalFilename(), e);
            }
        }

        response.setSuccessIds(successIds);
        response.setFailedFiles(failedFiles);
        response.setErrorMessages(errorMessages);

        log.info("批量上传完成，成功: {}, 失败: {}", response.getSuccessCount(), response.getFailedCount());
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public Document getDocumentById(Long id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.DOCUMENT_NOT_FOUND, "文档不存在: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<Document> queryDocuments(DocumentQueryRequest request) {
        log.info("查询文档列表，请求: {}", request);

        Specification<Document> spec = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

            if (request.getFileName() != null && !request.getFileName().isEmpty()) {
                predicates.add(cb.like(root.get("fileName"), "%" + request.getFileName() + "%"));
            }

            if (request.getFileType() != null && !request.getFileType().isEmpty()) {
                predicates.add(cb.equal(root.get("fileType"), request.getFileType()));
            }

            if (request.getStatus() != null && !request.getStatus().isEmpty()) {
                predicates.add(cb.equal(root.get("status"), Document.DocumentStatus.valueOf(request.getStatus())));
            }

            if (request.getStartTime() != null && !request.getStartTime().isEmpty()) {
                LocalDateTime start = LocalDateTime.parse(request.getStartTime(), DateTimeFormatter.ISO_DATE_TIME);
                predicates.add(cb.greaterThanOrEqualTo(root.get("createTime"), start));
            }

            if (request.getEndTime() != null && !request.getEndTime().isEmpty()) {
                LocalDateTime end = LocalDateTime.parse(request.getEndTime(), DateTimeFormatter.ISO_DATE_TIME);
                predicates.add(cb.lessThanOrEqualTo(root.get("createTime"), end));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };

        Page<Document> page = documentRepository.findAll(spec, request.toPageable());
        return PageResponse.of(page);
    }

    @Override
    public void deleteDocument(Long id) {
        log.info("删除文档: {}", id);

        Document document = getDocumentById(id);

        // 删除文件
        try {
            Path filePath = Paths.get(document.getFilePath());
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.warn("删除文件失败: {}", document.getFilePath(), e);
        }

        // 更新状态
        document.setStatus(Document.DocumentStatus.DELETED);
        documentRepository.save(document);

        log.info("文档删除成功: {}", id);
    }

    @Override
    public void batchDeleteDocuments(Long[] ids) {
        log.info("批量删除文档，数量: {}", ids.length);
        for (Long id : ids) {
            try {
                deleteDocument(id);
            } catch (Exception e) {
                log.error("删除文档失败: {}", id, e);
            }
        }
    }

    /**
     * 验证文件
     */
    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "文件不能为空");
        }

        if (file.getSize() > maxFileSize) {
            throw new BusinessException(ErrorCode.FILE_SIZE_EXCEEDED, 
                    "文件大小超出限制，最大允许: " + maxFileSize + " 字节");
        }

        String fileType = getFileExtension(file.getOriginalFilename());
        if (!ALLOWED_FILE_TYPES.contains(fileType.toLowerCase())) {
            throw new BusinessException(ErrorCode.INVALID_FILE_FORMAT, 
                    "不支持的文件类型: " + fileType);
        }
    }

    /**
     * 保存文件
     */
    private String saveFile(MultipartFile file) {
        try {
            Path uploadDir = Paths.get(uploadPath);
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            String fileName = UUID.randomUUID().toString() + "." + getFileExtension(file.getOriginalFilename());
            Path filePath = uploadDir.resolve(fileName);
            file.transferTo(filePath.toFile());

            return filePath.toString();
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.DOCUMENT_UPLOAD_FAILED, "文件保存失败", e);
        }
    }

    /**
     * 计算文件校验和
     */
    private String calculateChecksum(MultipartFile file) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] bytes = file.getBytes();
            byte[] digest = md.digest(bytes);
            return Base64.getEncoder().encodeToString(digest);
        } catch (Exception e) {
            log.error("计算文件校验和失败", e);
            return UUID.randomUUID().toString();
        }
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.lastIndexOf(".") == -1) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }
}
