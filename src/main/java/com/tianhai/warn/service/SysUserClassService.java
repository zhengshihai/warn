package com.tianhai.warn.service;

import com.tianhai.warn.model.SysUser;
import com.tianhai.warn.model.SysUserClass;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
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


    /**
     * 根据班级管理员班级实体的sys_user_no批量更新
     * @param sysUserClassList   班级管理员信息列表
     * @return              更新行数
     */
    int updateBatchBySysUserNo(@Param("sysUserClassList") List<SysUserClass> sysUserClassList);

    /**
     * 更新班级管理员的工号
     * @param oldSysUserNo     旧工号
     * @param newSysUserNo     新工号
     * @param updateTime       更新时间
     * @return                 更新行数
     */
    int updateSysUserNo(String oldSysUserNo, String newSysUserNo, Date updateTime);

    /**
     * 根据班级管理员工号统计总数
     * @param sysUserNo     班级管理员工号
     * @return              总数
     */
    int countBySysUserNo(String sysUserNo);

    /**
     * 根据班级管理员工号删除信息
     * @param sysUserNo    班级管理员
     * @return             删除行数
     */
    int deleteBySysUserNo(String sysUserNo);
}