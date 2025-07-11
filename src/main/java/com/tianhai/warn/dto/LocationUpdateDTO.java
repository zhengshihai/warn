package com.tianhai.warn.dto;

import com.tianhai.warn.annotation.AtLeastOneFieldNotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@AtLeastOneFieldNotNull
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

    /**
     * 前端上报间隔（秒）
     */
    private Integer changeUnit;

    // 报警业务id
    private String alarmNo;
}
