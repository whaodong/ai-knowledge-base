package com.example.document.service;

import com.example.common.tracing.RagTracingEnhancer;
import com.example.common.dto.PageResponse;
import com.example.document.dto.*;
import com.example.document.entity.Document;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * 文档服务追踪装饰器
 * 
 * <p>为文档服务添加分布式追踪支持，追踪文档处理链路：</p>
 * <ul>
 *   <li>文档上传</li>
 *   <li>文档解析</li>
 *   <li>文档分割</li>
 *   <li>向量化</li>
 *   <li>入库</li>
 * </ul>
 */
@Slf4j
@Service
public class DocumentTracingService {

    private final DocumentService documentService;
    private final Tracer tracer;
    private final RagTracingEnhancer ragTracingEnhancer;

    @Autowired
    public DocumentTracingService(
            DocumentService documentService,
            Tracer tracer,
            RagTracingEnhancer ragTracingEnhancer) {
        this.documentService = documentService;
        this.tracer = tracer;
        this.ragTracingEnhancer = ragTracingEnhancer;
    }

    /**
     * 带追踪的文档上传
     */
    public Document uploadDocumentWithTracing(MultipartFile file, DocumentUploadRequest request) {
        String documentId = UUID.randomUUID().toString();
        Span span = ragTracingEnhancer.createDocumentProcessingSpan(documentId, file.getOriginalFilename());
        
        Instant startTime = Instant.now();
        
        try (Tracer.SpanInScope ws = tracer.withSpan(span)) {
            span.tag("process.stage", "upload");
            span.tag("file.size", String.valueOf(file.getSize()));
            span.tag("file.type", getFileExtension(file.getOriginalFilename()));
            
            Document document = documentService.uploadDocument(file, request);
            
            span.tag("document.id", document.getId().toString());
            span.tag("process.status", "uploaded");
            
            log.info("文档上传完成: id={}, name={}, traceId={}", 
                    document.getId(), file.getOriginalFilename(), span.context().traceId());
            
            return document;
            
        } catch (Exception ex) {
            span.error(ex);
            span.tag("process.status", "failed");
            throw ex;
        } finally {
            span.end();
            
            Duration duration = Duration.between(startTime, Instant.now());
            log.debug("文档上传耗时: {}ms", duration.toMillis());
        }
    }

    /**
     * 带追踪的文档处理流程
     */
    public void processDocumentWithTracing(Long documentId, String stage) {
        Span span = tracer.spanBuilder()
                .name("document.process." + stage)
                .kind(Span.Kind.SERVER)
                .start();
        
        Instant startTime = Instant.now();
        
        try (Tracer.SpanInScope ws = tracer.withSpan(span)) {
            span.tag("document.id", String.valueOf(documentId));
            span.tag("process.stage", stage);
            
            // 根据阶段执行不同处理
            switch (stage) {
                case "parse":
                    processParsing(documentId, span);
                    break;
                case "split":
                    processSplitting(documentId, span);
                    break;
                case "embed":
                    processEmbedding(documentId, span);
                    break;
                case "store":
                    processStoring(documentId, span);
                    break;
                default:
                    log.warn("未知的文档处理阶段: {}", stage);
            }
            
            span.tag("process.status", "completed");
            
        } catch (Exception ex) {
            span.error(ex);
            span.tag("process.status", "failed");
            throw ex;
        } finally {
            span.end();
            
            Duration duration = Duration.between(startTime, Instant.now());
            log.info("文档处理阶段完成: stage={}, documentId={}, duration={}ms", 
                    stage, documentId, duration.toMillis());
        }
    }

    private void processParsing(Long documentId, Span span) {
        span.event("开始文档解析");
        // 实际的解析逻辑会在DocumentService中实现
        span.tag("parser.type", "auto-detect");
    }

    private void processSplitting(Long documentId, Span span) {
        span.event("开始文档分割");
        span.tag("splitter.type", "recursive");
    }

    private void processEmbedding(Long documentId, Span span) {
        span.event("开始向量化");
        span.tag("embedding.model", "text-embedding-3-small");
    }

    private void processStoring(Long documentId, Span span) {
        span.event("开始入库");
        span.tag("storage.type", "milvus");
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "unknown";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }
}
