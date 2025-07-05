package com.tianhai.warn.dto;

import lombok.Data;

import java.util.List;

/**
 * 宿管Excel信息校验结果
 */
@Data
public class DorManInfoValidateResult {
    /**
     * 超级管理员Excel数据
     */
    private DormitoryManagerExcelDTO dormitoryManagerExcelDTO;


    /**
     * 是否校验通过
     */
    private boolean valid;

    /**
     * 错误信息列表
     */
    private List<String> errors;
}
