package com.tianhai.warn.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 晚归卡片数据VO
 * 包括总晚归次数 晚归学生数 处理完成率
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportCardStatVO {
    private Integer totalLateReturns; // 没有正当理由的总晚归次数

    private Integer lateStudentCount; // 晚归学生数

//    private Integer highRiskAlertCount; // 高危预警数 违反WaringRule数两条及以上

    private String completionRate; // 处理完成率
}
