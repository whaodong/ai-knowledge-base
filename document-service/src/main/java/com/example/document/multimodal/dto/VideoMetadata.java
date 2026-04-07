package com.example.document.multimodal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 视频元数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoMetadata {
    private Double duration;
    private Integer width;
    private Integer height;
    private Double frameRate;
    private String videoCodec;
    private String audioCodec;
    private Integer videoBitRate;
    private Integer audioBitRate;
    private String format;
    private Long totalFrames;
    private Integer audioSampleRate;
    private Integer audioChannels;
    private Boolean hasSubtitles;
    private Integer keyFrameCount;
}
