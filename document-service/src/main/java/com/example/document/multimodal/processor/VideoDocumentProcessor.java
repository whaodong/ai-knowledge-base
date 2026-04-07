package com.example.document.multimodal.processor;

import com.example.document.multimodal.dto.MultiModalProcessResult;
import com.example.document.multimodal.dto.VideoMetadata;
import com.example.document.multimodal.enums.DocumentType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 视频文档处理器
 * 支持：关键帧提取、视频转文字、字幕提取
 */
@Slf4j
@Component
public class VideoDocumentProcessor implements MultiModalProcessor {
    
    @Value("${multimodal.video.keyframe.enabled:true}")
    private boolean keyframeEnabled;
    
    @Value("${multimodal.video.keyframe.interval:5.0}")
    private double keyframeInterval;
    
    @Value("${multimodal.video.keyframe.output-dir:/tmp/keyframes}")
    private String keyframeOutputDir;
    
    @Value("${multimodal.video.asr.enabled:true}")
    private boolean asrEnabled;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Override
    public MultiModalProcessResult process(Path filePath, DocumentType documentType) {
        log.info("开始处理视频文档: {}, 类型: {}", filePath, documentType);
        
        long startTime = System.currentTimeMillis();
        MultiModalProcessResult.MultiModalProcessResultBuilder resultBuilder = 
            MultiModalProcessResult.builder()
                .documentType(documentType)
                .modalityType(DocumentType.ModalityType.VIDEO)
                .processTime(LocalDateTime.now());
        
        try {
            File videoFile = filePath.toFile();
            
            // 1. 提取视频元数据
            VideoMetadata metadata = extractMetadata(videoFile);
            Map<String, Object> metadataMap = convertMetadataToMap(metadata);
            resultBuilder.metadata(metadataMap);
            
            // 2. 提取关键帧
            if (keyframeEnabled) {
                List<MultiModalProcessResult.VideoFrame> keyFrames = extractKeyFrames(videoFile);
                resultBuilder.keyFrames(keyFrames);
            }
            
            // 3. 视频转文字
            String transcriptText = null;
            if (asrEnabled) {
                transcriptText = extractAudioTranscript(videoFile);
                resultBuilder.transcriptText(transcriptText);
            }
            
            // 4. 构建文本内容
            StringBuilder textContent = new StringBuilder();
            if (transcriptText != null && !transcriptText.trim().isEmpty()) {
                textContent.append("语音转录: ").append(transcriptText);
            }
            resultBuilder.textContent(textContent.toString());
            
            // 5. 设置处理状态
            if (transcriptText != null && !transcriptText.trim().isEmpty()) {
                resultBuilder.status(MultiModalProcessResult.ProcessStatus.SUCCESS);
            } else {
                resultBuilder.status(MultiModalProcessResult.ProcessStatus.PARTIAL_SUCCESS);
            }
            
        } catch (Exception e) {
            log.error("视频处理失败: {}", filePath, e);
            resultBuilder.status(MultiModalProcessResult.ProcessStatus.FAILED)
                .errorMessage(e.getMessage());
        }
        
        resultBuilder.processingTime(System.currentTimeMillis() - startTime);
        return resultBuilder.build();
    }
    
    @Override
    public boolean supports(DocumentType documentType) {
        return documentType.isVideo();
    }
    
    @Override
    public String getProcessorName() {
        return "VideoDocumentProcessor";
    }
    
    private VideoMetadata extractMetadata(File videoFile) {
        // TODO: 实际实现应使用FFmpeg提取元数据
        return VideoMetadata.builder()
            .format(videoFile.getName().substring(videoFile.getName().lastIndexOf(".") + 1))
            .build();
    }
    
    private List<MultiModalProcessResult.VideoFrame> extractKeyFrames(File videoFile) {
        log.info("提取关键帧: {}, 间隔: {}秒", videoFile.getName(), keyframeInterval);
        // TODO: 实际实现应使用FFmpeg提取关键帧
        return List.of();
    }
    
    private String extractAudioTranscript(File videoFile) {
        log.info("提取视频音频并转录: {}", videoFile.getName());
        // TODO: 实际实现应提取音频后调用ASR
        return "";
    }
    
    private Map<String, Object> convertMetadataToMap(VideoMetadata metadata) {
        try {
            return objectMapper.convertValue(metadata, Map.class);
        } catch (Exception e) {
            return new HashMap<>();
        }
    }
}
