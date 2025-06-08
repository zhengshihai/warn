package com.tianhai.warn.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

// 位置轨迹实体类
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationTrack {
    /**
     * 主键ID
     */
    private Long id;

    /**
     * 报警记录ID
     */
    private String alarmNo;

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
     * 创建时间
     */
    private Date createdAt;
}
