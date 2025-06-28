package com.tianhai.warn.scheduler;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentNoUpdateTask {
    private String oldStudentNo;
    private String newStudentNo;
    private Date createTime;
    private String status; // PENDING, PROCESSING, SUCCESS, FAILED
    private String errorMessage;
}