package com.example.document.multimodal.dto;

import com.example.document.multimodal.enums.DocumentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 多模态处理结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MultiModalProcessResult {
    
    private ProcessStatus status;
    private DocumentType documentType;
    private DocumentType.ModalityType modalityType;
    private String textContent;
    private String ocrText;
    private String imageDescription;
    private String transcriptText;
    private List<AudioSegment> audioSegments;
    private List<VideoFrame> keyFrames;
    private String subtitleText;
    private Map<String, Object> metadata;
    private Long processingTime;
    private String errorMessage;
    private LocalDateTime processTime;
    
    public enum ProcessStatus {
        SUCCESS,
        PARTIAL_SUCCESS,
        FAILED
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AudioSegment {
        private Integer segmentIndex;
        private Double startTime;
        private Double endTime;
        private String text;
        private Double confidence;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VideoFrame {
        private Integer frameIndex;
        private Double timestamp;
        private String framePath;
        private String description;
        private List<Float> embedding;
    }
}
