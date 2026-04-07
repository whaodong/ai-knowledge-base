package com.example.document.multimodal.processor;

import com.example.document.multimodal.dto.ImageMetadata;
import com.example.document.multimodal.dto.MultiModalProcessResult;
import com.example.document.multimodal.enums.DocumentType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 图像文档处理器
 * 支持：OCR识别、图片描述、元数据提取
 */
@Slf4j
@Component
public class ImageDocumentProcessor implements MultiModalProcessor {
    
    @Value("${multimodal.image.ocr.enabled:true}")
    private boolean ocrEnabled;
    
    @Value("${multimodal.image.ocr.provider:tesseract}")
    private String ocrProvider;
    
    @Value("${multimodal.image.description.enabled:true}")
    private boolean descriptionEnabled;
    
    @Value("${multimodal.image.description.provider:gpt4v}")
    private String descriptionProvider;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Override
    public MultiModalProcessResult process(Path filePath, DocumentType documentType) {
        log.info("开始处理图像文档: {}, 类型: {}", filePath, documentType);
        
        long startTime = System.currentTimeMillis();
        MultiModalProcessResult.MultiModalProcessResultBuilder resultBuilder = 
            MultiModalProcessResult.builder()
                .documentType(documentType)
                .modalityType(DocumentType.ModalityType.IMAGE)
                .processTime(LocalDateTime.now());
        
        try {
            File imageFile = filePath.toFile();
            
            // 1. 提取图像元数据
            ImageMetadata metadata = extractMetadata(imageFile);
            Map<String, Object> metadataMap = convertMetadataToMap(metadata);
            resultBuilder.metadata(metadataMap);
            
            // 2. OCR识别文本
            String ocrText = null;
            if (ocrEnabled) {
                ocrText = performOCR(imageFile);
                resultBuilder.ocrText(ocrText);
            }
            
            // 3. 生成图片描述
            String imageDescription = null;
            if (descriptionEnabled) {
                imageDescription = generateImageDescription(imageFile);
                resultBuilder.imageDescription(imageDescription);
            }
            
            // 4. 构建文本内容（用于向量化）
            StringBuilder textContent = new StringBuilder();
            if (imageDescription != null) {
                textContent.append("图片描述: ").append(imageDescription).append("\n");
            }
            if (ocrText != null && !ocrText.trim().isEmpty()) {
                textContent.append("图片文字: ").append(ocrText);
            }
            resultBuilder.textContent(textContent.toString());
            
            // 5. 设置处理状态
            if (imageDescription != null || (ocrText != null && !ocrText.trim().isEmpty())) {
                resultBuilder.status(MultiModalProcessResult.ProcessStatus.SUCCESS);
            } else {
                resultBuilder.status(MultiModalProcessResult.ProcessStatus.PARTIAL_SUCCESS);
            }
            
        } catch (Exception e) {
            log.error("图像处理失败: {}", filePath, e);
            resultBuilder.status(MultiModalProcessResult.ProcessStatus.FAILED)
                .errorMessage(e.getMessage());
        }
        
        resultBuilder.processingTime(System.currentTimeMillis() - startTime);
        return resultBuilder.build();
    }
    
    @Override
    public boolean supports(DocumentType documentType) {
        return documentType.isImage();
    }
    
    @Override
    public String getProcessorName() {
        return "ImageDocumentProcessor";
    }
    
    private ImageMetadata extractMetadata(File imageFile) {
        try {
            BufferedImage image = ImageIO.read(imageFile);
            return ImageMetadata.builder()
                .width(image.getWidth())
                .height(image.getHeight())
                .format(imageFile.getName().substring(imageFile.getName().lastIndexOf(".") + 1).toUpperCase())
                .build();
        } catch (Exception e) {
            log.error("提取图像元数据失败", e);
            return ImageMetadata.builder().build();
        }
    }
    
    private String performOCR(File imageFile) {
        log.info("执行OCR识别: {}, 提供商: {}", imageFile.getName(), ocrProvider);
        // TODO: 实际实现应调用OCR API (Tesseract/Baidu/Google)
        return "";
    }
    
    private String generateImageDescription(File imageFile) {
        log.info("生成图片描述: {}, 提供商: {}", imageFile.getName(), descriptionProvider);
        // TODO: 实际实现应调用多模态模型 (GPT-4V/Claude Vision)
        return "";
    }
    
    private Map<String, Object> convertMetadataToMap(ImageMetadata metadata) {
        try {
            return objectMapper.convertValue(metadata, Map.class);
        } catch (Exception e) {
            return new HashMap<>();
        }
    }
}
