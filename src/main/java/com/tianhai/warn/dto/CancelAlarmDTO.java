package com.tianhai.warn.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CancelAlarmDTO {
    private String studentNo;

    private String name; // 学生姓名

    private String alarmNo; // 报警编号
}
