package com.example.document.multimodal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 图像元数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageMetadata {
    private Integer width;
    private Integer height;
    private String colorSpace;
    private Integer bitDepth;
    private String format;
    private Integer dpi;
    private LocalDateTime dateTimeOriginal;
    private String make;
    private String model;
    private Double gpsLatitude;
    private Double gpsLongitude;
    private String exposureTime;
    private Double fNumber;
    private Integer iso;
    private Double focalLength;
    private Map<String, Object> additionalExif;
}
