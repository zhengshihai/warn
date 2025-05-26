package com.tianhai.warn.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 学生实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Student implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Integer id; // 主键ID
    private String studentNo; // 学号
    private String name; // 姓名
    private String college; // 学院
    private String className; // 班级
    private String dormitory; // 宿舍号
    private String phone; // 联系电话
    private String email; // 电子邮箱
    private String password; // 登录密码
    private String fatherName; // 父亲姓名
    private String fatherPhone; // 父亲电话
    private String motherName; // 母亲姓名
    private String motherPhone; // 母亲电话
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime; // 创建时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime; // 更新时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastLoginTime; // 最后登录时间

    @Override
    public String toString() {
        return "Student{" +
                "id=" + id +
                ", studentNo='" + studentNo + '\'' +
                ", name='" + name + '\'' +
                ", college='" + college + '\'' +
                ", className='" + className + '\'' +
                ", dormitory='" + dormitory + '\'' +
                ", phone='" + phone + '\'' +
                ", email='" + email + '\'' +
                ", fatherName='" + fatherName + '\'' +
                ", fatherPhone='" + fatherPhone + '\'' +
                ", motherName='" + motherName + '\'' +
                ", motherPhone='" + motherPhone + '\'' +
                ", createTime=" + createTime +
                ", updateTime=" + updateTime +
                '}';
    }
}