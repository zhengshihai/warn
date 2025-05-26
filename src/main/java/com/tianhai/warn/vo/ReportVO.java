package com.tianhai.warn.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportVO {
    private Integer totalLateReturns; // 没有正当理由的总晚归次数

    private Integer lateStudentCount; // 晚归学生数

//    private Integer highRiskAlertCount; // 高危预警数 违反WaringRule数两条及以上

    private String completionRate; // 处理完成率
}
