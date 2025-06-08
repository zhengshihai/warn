package com.tianhai.warn.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

// 报警记录实体类
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlarmRecord {
    /**
     * 主键ID
     */
    private Long id;

    /**
     * 报警记录ID
     */
    private String alarmNo;

    /**
     * 学生ID
     */
    private String studentNo;

    /**
     * 报警类型：1-一键报警，2-接口报警
     */
    private Integer alarmType;

    /**
     * 报警级别：1-普通，2-紧急
     */
    private Integer alarmLevel;

    /**
     * 处理状态：0-未处理，1-处理中，2-已处理，3-已关闭
     */
    private Integer alarmStatus;

    /**
     * 报警时间
     */
    private Date alarmTime;

    /**
     * 纬度
     */
    private Double latitude;

    /**
     * 经度
     */
    private Double longitude;

    /**
     * 位置描述
     */
    private String locationAddress;

    /**
     * 报警描述
     */
    private String description;

    /**
     * 媒体文件URL列表
     */
    private List<String> mediaUrls;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createdAt;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updatedAt;
}
