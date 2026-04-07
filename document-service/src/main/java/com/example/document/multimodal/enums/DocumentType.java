package com.example.document.multimodal.enums;

import lombok.Getter;

/**
 * 文档类型枚举
 * 支持文本、图片、音频、视频等多种模态
 */
@Getter
public enum DocumentType {
    
    // 文本文档
    TEXT("text", "纯文本", "txt"),
    PDF("pdf", "PDF文档", "pdf"),
    WORD("word", "Word文档", "doc", "docx"),
    EXCEL("excel", "Excel表格", "xls", "xlsx"),
    MARKDOWN("markdown", "Markdown文档", "md"),
    HTML("html", "HTML文档", "html", "htm"),
    
    // 图像文档
    IMAGE_JPG("image_jpg", "JPEG图片", "jpg", "jpeg"),
    IMAGE_PNG("image_png", "PNG图片", "png"),
    IMAGE_GIF("image_gif", "GIF图片", "gif"),
    IMAGE_BMP("image_bmp", "BMP图片", "bmp"),
    IMAGE_WEBP("image_webp", "WebP图片", "webp"),
    
    // 音频文档
    AUDIO_MP3("audio_mp3", "MP3音频", "mp3"),
    AUDIO_WAV("audio_wav", "WAV音频", "wav"),
    AUDIO_AAC("audio_aac", "AAC音频", "aac"),
    AUDIO_FLAC("audio_flac", "FLAC音频", "flac"),
    AUDIO_OGG("audio_ogg", "OGG音频", "ogg"),
    
    // 视频文档
    VIDEO_MP4("video_mp4", "MP4视频", "mp4"),
    VIDEO_AVI("video_avi", "AVI视频", "avi"),
    VIDEO_MOV("video_mov", "MOV视频", "mov"),
    VIDEO_MKV("video_mkv", "MKV视频", "mkv"),
    VIDEO_WEBM("video_webm", "WebM视频", "webm");
    
    private final String code;
    private final String description;
    private final String[] extensions;
    
    DocumentType(String code, String description, String... extensions) {
        this.code = code;
        this.description = description;
        this.extensions = extensions;
    }
    
    /**
     * 根据文件扩展名获取文档类型
     */
    public static DocumentType fromExtension(String extension) {
        if (extension == null) {
            return TEXT;
        }
        
        String ext = extension.toLowerCase().replace(".", "");
        
        for (DocumentType type : values()) {
            for (String typeExt : type.getExtensions()) {
                if (typeExt.equalsIgnoreCase(ext)) {
                    return type;
                }
            }
        }
        
        return TEXT;
    }
    
    /**
     * 判断是否为图像类型
     */
    public boolean isImage() {
        return this.name().startsWith("IMAGE_");
    }
    
    /**
     * 判断是否为音频类型
     */
    public boolean isAudio() {
        return this.name().startsWith("AUDIO_");
    }
    
    /**
     * 判断是否为视频类型
     */
    public boolean isVideo() {
        return this.name().startsWith("VIDEO_");
    }
    
    /**
     * 判断是否为多模态类型
     */
    public boolean isMultiModal() {
        return isImage() || isAudio() || isVideo();
    }
    
    /**
     * 获取主要模态类型
     */
    public ModalityType getModalityType() {
        if (isImage()) {
            return ModalityType.IMAGE;
        } else if (isAudio()) {
            return ModalityType.AUDIO;
        } else if (isVideo()) {
            return ModalityType.VIDEO;
        } else {
            return ModalityType.TEXT;
        }
    }
    
    /**
     * 模态类型枚举
     */
    public enum ModalityType {
        TEXT,
        IMAGE,
        AUDIO,
        VIDEO
    }
}
