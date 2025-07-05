package com.tianhai.warn.dto;

import lombok.Data;

import java.util.List;

/**
 * 班级管理员Excel信息校验结果
 */
@Data
public class SysUserInfoValidateResult {
    /**
     * 班级管理员Excel数据
     */
    private SysUserExcelDTO sysUserExcelDTO;


    /**
     * 是否校验通过
     */
    private boolean valid;

    /**
     * 错误信息列表
     */
    private List<String> errors;
}
