package com.tianhai.warn.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.alibaba.excel.annotation.ExcelProperty;

/**
 * 宿管信息Excel导入DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DormitoryManagerExcelDTO {
    @ExcelProperty("工号")
    private String managerId;

    @ExcelProperty("密码")
    private String password;

    @ExcelProperty("姓名")
    private String name;

    @ExcelProperty("电话")
    private String phone;

    @ExcelProperty("邮箱")
    private String email;

    @ExcelProperty("负责楼栋")
    private String building;

}