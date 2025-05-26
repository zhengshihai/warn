package com.tianhai.warn.model;


import com.tianhai.warn.enums.CalculationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalculationResult {

    private CalculationStatus status; // 计算状态

    private Integer count; // 计算结果

    private String taskId; // 任务ID

    private Date startTime; // 开始时间

    private Date estimatedEndTime; // 预计完成时间


}
