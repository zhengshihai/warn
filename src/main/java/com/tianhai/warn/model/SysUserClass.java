package com.tianhai.warn.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

@Data
public class SysUserClass implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer id;
    private String sysUserNo;
    private String className; // 学生班级名
    private String jobRole; // 系统用户角色
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    @Override
    public String toString() {
        return "SysUserClass{" +
                "id=" + id +
                ", sysUserNo='" + sysUserNo + '\'' +
                ", className='" + className + '\'' +
                ", jobRole='" + jobRole + '\'' +
                ", createTime=" + createTime +
                ", updateTime=" + updateTime +
                '}';
    }
}
