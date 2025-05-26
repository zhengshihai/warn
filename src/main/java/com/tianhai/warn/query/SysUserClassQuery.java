package com.tianhai.warn.query;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class SysUserClassQuery extends BaseQuery{

    private Integer id;
    private String sysUserNo;
    private String className; // 学生班级名
    private String jobRole; // 系统用户角色

    private String createTimeStart; // 创建时间开始
    private String createTimeEnd; // 创建时间结束

    private List<String> classNames; // 班级列表
    private List<String> jobRoles; // 角色列表
    private List<String> sysUserNos; // 用户编号列表

}
