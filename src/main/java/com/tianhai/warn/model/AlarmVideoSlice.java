package com.tianhai.warn.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlarmVideoSlice implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id; // 切片表主键，自增ID
    private String videoId; // 关联视频表videos的ID，外键
    private String sliceId; // AlarmNo + 五位数字，例如00001,00002等，用于标识切片顺序
    private Long startTimeSec; // 切片起始时间（秒）
    private Long durationSec; // 切片持续时长（秒）
    private String sliceFilePath; // 切片文件存储路径或URL
    private Long fileSizeBytes; // 切片文件大小，单位字节
    private Date createdAt; // 记录创建时间
    private Date updatedAt; // 记录最后更新时间

    public String toString() {
        return "AlarmVideoSlice{" +
                "id=" + id +
                ", videoId=" + videoId +
                ", sliceId=" + sliceId +
                ", startTimeSec=" + startTimeSec +
                ", durationSec=" + durationSec +
                ", sliceFilePath='" + sliceFilePath + '\'' +
                ", fileSizeBytes=" + fileSizeBytes +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }

}
