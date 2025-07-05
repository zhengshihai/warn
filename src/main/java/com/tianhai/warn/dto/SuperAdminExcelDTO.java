package com.tianhai.warn.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 超级管理员信息Excel导入DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuperAdminExcelDTO {
    @ExcelProperty("密码")
    private String password;

    @ExcelProperty("姓名")
    private String name;

    @ExcelProperty("邮箱")
    private String email;

}
