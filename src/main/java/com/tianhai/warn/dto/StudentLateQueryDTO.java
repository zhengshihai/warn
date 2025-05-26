package com.tianhai.warn.dto;

import lombok.*;

import java.util.Date;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentLateQueryDTO {
    private Date startDate;

    private Date endDate;

    private String college; // 学院

    private String className; // 班级

}
