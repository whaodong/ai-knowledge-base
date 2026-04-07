package com.example.document.multimodal.service;

import com.example.document.multimodal.dto.MultiModalProcessResult;
import com.example.document.multimodal.enums.DocumentType;
import com.example.document.multimodal.processor.MultiModalProcessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.List;

/**
 * 多模态文档处理器服务
 */
@Slf4j
@Service
public class MultiModalDocumentProcessorService {
    
    @Autowired
    private List<MultiModalProcessor> processors;
    
    public MultiModalProcessResult processDocument(Path filePath, DocumentType documentType) {
        log.info("开始处理多模态文档: {}, 类型: {}", filePath, documentType);
        
        MultiModalProcessor processor = findProcessor(documentType);
        
        if (processor == null) {
            log.warn("未找到支持文档类型 {} 的处理器", documentType);
            return MultiModalProcessResult.builder()
                .documentType(documentType)
                .modalityType(documentType.getModalityType())
                .status(MultiModalProcessResult.ProcessStatus.FAILED)
                .errorMessage("未找到支持的处理器")
                .build();
        }
        
        log.info("使用处理器: {}", processor.getProcessorName());
        return processor.process(filePath, documentType);
    }
    
    private MultiModalProcessor findProcessor(DocumentType documentType) {
        return processors.stream()
            .filter(p -> p.supports(documentType))
            .findFirst()
            .orElse(null);
    }
    
    public boolean isSupported(DocumentType documentType) {
        return processors.stream().anyMatch(p -> p.supports(documentType));
    }
    
    public List<DocumentType> getSupportedTypes() {
        return processors.stream()
            .flatMap(p -> java.util.Arrays.stream(DocumentType.values()).filter(p::supports))
            .distinct()
            .toList();
    }
}
