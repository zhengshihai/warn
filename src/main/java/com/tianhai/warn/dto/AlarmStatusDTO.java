package com.tianhai.warn.dto;

import com.tianhai.warn.enums.AlarmStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlarmStatusDTO {
    // 报警状态
    private AlarmStatus alarmStatus;

    /**
     * 报警号
     */
    private String alarmNo;

    /**
     * 最后更新时间
     */
    private Date lastUpdateTime;


}