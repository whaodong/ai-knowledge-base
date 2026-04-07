package com.example.document.multimodal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 音频元数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AudioMetadata {
    private Double duration;
    private Integer sampleRate;
    private Integer channels;
    private Integer bitDepth;
    private String codec;
    private Integer bitRate;
    private String format;
    private String language;
    private Boolean hasSilence;
    private Integer silenceSegments;
}
