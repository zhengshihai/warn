package com.tianhai.warn.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 视频元数据凤凰类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoMetaData {
    private Long durationSec;    // 视频时长(秒)
    private Long fileSizeBytes;  // 文件大小(字节)
    private String resolution;   // 分辨率(如"1920x1080")
    private Long bitrate;// 码率(bps)
    private String videoId; // 视频唯一的业务标识id

    @Override
    public String toString() {
        return "VideoMetaData{" +
                "durationSec=" + durationSec +
                ", fileSizeBytes=" + fileSizeBytes +
                ", resolution='" + resolution +
                ", bitrate=" + bitrate +
                ", videoId='" + videoId +
                '}';
    }
}
