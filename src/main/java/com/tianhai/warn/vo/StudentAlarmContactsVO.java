package com.tianhai.warn.vo;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentAlarmContactsVO {

    private String studentName;

    private String studentNo;

    private String alarmNo;

    private String fatherPhone;

    private String motherPhone;

}
