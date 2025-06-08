package com.tianhai.warn.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationUpdateDTO {
    /**
     * 纬度
     */
    private Double latitude;

    /**
     * 经度
     */
    private Double longitude;

    /**
     * 精确度(米)
     */
    private Double locationAccuracy;

    /**
     * 速度(米/秒)
     */
    private Double speed;

    /**
     * 方向(度)
     */
    private Double direction;

    /**
     * 时间戳
     */
    private Date locationTime;

    // 报警业务id
    private String alarmNo;
}
