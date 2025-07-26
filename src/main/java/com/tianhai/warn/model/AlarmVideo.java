package com.tianhai.warn.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 视频基本信息表，存储原视频元数据
 *
 * 这里的原视频是指将webm格式的视频转为mp4格式后的视频，相对于它的切片来说是原视频
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlarmVideo implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id; // 视频表主键，自增ID
    private String videoId; // 视频唯一标识ID，格式：VI + 年月日 + 6位随机数
    private String alarmNo; // 关联的报警记录ID
    private String studentNo; // 学生学号
    private String title; // 视频名称
    private String filePath; // 视频文件的存储路径或URL
    private Long durationSec; // 视频总时长（秒）
    private Long fileSizeBytes; // 视频文件大小，单位字节
    private String format; // 视频格式，例如mp4、webm等
    private String resolution; // 视频分辨率，如1920x1080
    private Long bitrate; // 视频码率，单位bps
    private String uploadUserId; // 上传用户ID，关联用户表（如有）
    private Integer status; // 切片完成状态，0-未切片，1-切片中，2-已切片
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createdAt; // 记录创建时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updatedAt; // 记录最后更新时间

    @Override
    public String toString() {
        return "AlarmVideo{" +
                "id=" + id +
                ", videoId='" + videoId + '\'' +
                ", alarmNo='" + alarmNo + '\'' +
                ", studentNo='" + studentNo + '\'' +
                ", title='" + title + '\'' +
                ", filePath='" + filePath + '\'' +
                ", durationSec=" + durationSec +
                ", fileSizeBytes=" + fileSizeBytes +
                ", format='" + format + '\'' +
                ", resolution='" + resolution + '\'' +
                ", bitrate=" + bitrate +
                ", uploadUserId=" + uploadUserId +
                ", status=" + status +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
