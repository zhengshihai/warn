package com.tianhai.warn.dto;

import lombok.Data;

import java.util.List;

/**
 * 超级管理员Excel信息校验结果
 */
@Data
public class SuperAdminInfoValidateResult {
    /**
     * 超级管理员Excel数据
     */
    private SuperAdminExcelDTO superAdminExcelDTO;


    /**
     * 是否校验通过
     */
    private boolean valid;

    /**
     * 错误信息列表
     */
    private List<String> errors;
}
