package com.tianhai.warn.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.alibaba.excel.annotation.ExcelProperty;



@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentExcelDTO {
    @ExcelProperty("学号")
    private String studentNo;
    @ExcelProperty("密码")
    private String password;
    @ExcelProperty("姓名")
    private String name;
    @ExcelProperty("学院")
    private String college;
    @ExcelProperty("班级")
    private String className;
    @ExcelProperty("宿舍")
    private String dormitory;
    @ExcelProperty("电话")
    private String phone;
    @ExcelProperty("邮箱")
    private String email;
    @ExcelProperty("父亲姓名")
    private String fatherName;
    @ExcelProperty("父亲电话")
    private String fatherPhone;
    @ExcelProperty("母亲姓名")
    private String motherName;
    @ExcelProperty("母亲电话")
    private String motherPhone;
}
