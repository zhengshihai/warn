package com.tianhai.warn.dto;

import lombok.Data;
import java.util.List;

/**
 * 学生Excel信息校验结果
 */
@Data
public class StudentInfoValidateResult {
    /**
     * 学生Excel数据
     */
    private StudentExcelDTO studentExcelDTO;

    /**
     * 是否校验通过
     */
    private boolean valid;

    /**
     * 错误信息列表
     */
    private List<String> errors;
}