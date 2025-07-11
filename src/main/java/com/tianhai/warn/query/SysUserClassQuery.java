package com.tianhai.warn.query;

import com.tianhai.warn.annotation.AtLeastOneFieldNotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.Date;
import java.util.List;

@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@AtLeastOneFieldNotNull
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
