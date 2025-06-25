package com.tianhai.warn.service;

import com.tianhai.warn.model.SysUserClass;

import java.util.List;

/**
 * 用户-班级关联Service接口
 */
public interface SysUserClassService {
    /**
     * 获取用户负责的班级列表
     * @param sysUserNo 用户编号
     * @return 班级列表
     */
    List<String> getUserClasses(String sysUserNo);

    /**
     * 更新用户负责的班级
     * @param sysUserNo 用户编号
     * @param classList 班级列表
     */
    void updateUserClasses(String sysUserNo, List<String> classList);


    /**
     * 级联删除班级
     * @param className  班级
     */
    void deleteByClassName(String className);

    /**
     * 检查用户是否有权限管理指定班级
     * @param sysUserNo 用户编号
     * @param className 班级
     * @return 是否有权限
     */
    boolean hasClassPermission(String sysUserNo, String className);


    /**
     * 根据班级名称获取班级管理员列表
     * @param className     班级名称
     * @return              班级管理员列表
     */
    List<SysUserClass> getSysUserClassListByClassName(String className);


}