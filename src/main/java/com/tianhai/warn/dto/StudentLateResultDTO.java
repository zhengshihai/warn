package com.tianhai.warn.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class StudentLateResultDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String studentNo;

    private Integer lateReturnCount;


}
