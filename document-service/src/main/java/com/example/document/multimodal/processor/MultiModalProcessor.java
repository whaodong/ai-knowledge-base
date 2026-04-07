package com.example.document.multimodal.processor;

import com.example.document.multimodal.dto.MultiModalProcessResult;
import com.example.document.multimodal.enums.DocumentType;

import java.nio.file.Path;

/**
 * 多模态文档处理器接口
 */
public interface MultiModalProcessor {
    
    /**
     * 处理文档
     */
    MultiModalProcessResult process(Path filePath, DocumentType documentType);
    
    /**
     * 是否支持该文档类型
     */
    boolean supports(DocumentType documentType);
    
    /**
     * 获取处理器名称
     */
    String getProcessorName();
}
