package com.tianhai.warn.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.alibaba.excel.annotation.ExcelProperty;

/**
 * 班级管理员信息Excel导入DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SysUserExcelDTO {
    @ExcelProperty("工号")
    private String sysUserNo;

    @ExcelProperty("密码")
    private String password;

    @ExcelProperty("姓名")
    private String name;

    @ExcelProperty("电话")
    private String phone;

    @ExcelProperty("邮箱")
    private String email;

    @ExcelProperty("职位角色")
    private String jobRole;

}