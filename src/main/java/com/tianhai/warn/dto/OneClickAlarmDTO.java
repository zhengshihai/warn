package com.tianhai.warn.dto;

import com.tianhai.warn.enums.AlarmLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OneClickAlarmDTO {
    /**
     * 学号
     */
    private String studentNo;

    /**
     * 报警等级
     */
    private AlarmLevel alarmLevel;

    /**
     * 位置纬度
     */
    private Double latitude;

    /**
     * 位置经度
     */
    private Double longitude;

    /**
     * 位置精度
     */
    private Double locationAccuracy;

    /**
     * 移动速度
     */
    private Double speed;

    /**
     * 移动方向
     */
    private Double direction;

    /**
     * 报警描述
     */
    private String description;

    /**
     * 媒体文件URL列表
     */
    private List<String> mediaUrls;

    private String alarmNo;
}
