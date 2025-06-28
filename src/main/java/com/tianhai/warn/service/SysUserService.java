package com.tianhai.warn.service;

import com.tianhai.warn.model.SysUser;
import com.tianhai.warn.query.SysUserQuery;
import com.tianhai.warn.utils.PageResult;
import com.tianhai.warn.utils.Result;

import java.util.List;

/**
 * 系统用户服务接口
 */
public interface SysUserService {

    /**
     * 根据ID获取系统用户信息
     */
    SysUser getSysUserById(Integer id);

    /**
     * 根据邮箱获取系统用户信息
     */
    SysUser getSysUserByEmail(String email);

    /**
     * 获取所有系统用户列表
     */
    List<SysUser> getAllSysUsers();

    /**
     * 条件查询系统用户
     */
    List<SysUser> selectByCondition(SysUserQuery query);

    /**
     * 新增系统用户
     */
    SysUser insertSysUser(SysUser user);

    /**
     * 更新系统用户信息
     */
    SysUser updateSysUser(SysUser user);

    /**
     * 删除系统用户
     */
    void deleteSysUser(Integer id);

    /**
     * 根据角色获取系统用户列表
     * 
     * @param jobRole 角色
     * @return 系统用户列表
     */
    List<SysUser> getSysUsersByRole(String jobRole);

    /**
     * 更新系统用户最后登录时间
     */
    void updateLastLoginTime(Integer id);

    /**
     * 更新系统用户个人信息
     * 
     * @param sysUser      系统用户信息
     * @param currentEmail 当前邮箱
     */
    void updatePersonalInfo(SysUser sysUser, String currentEmail);

    /**
     * 根据用户编号列表查询系统用户信息
     * 
     * @param sysUserNos 用户编号列表
     * @return 系统用户信息列表
     */
    List<SysUser> selectBySysUserNos(List<String> sysUserNos);

    /**
     * 根据主键ID查询系统用户信息
     *
     * @param id  主键id
     * @return   班级管理员
     */
    SysUser selectById(Integer id);

    /**
     * 分页查询班级管理员信息
     * @param query     查询条件
     * @return          分页查询结果
     */
    PageResult<SysUser> selectByPageQuery(SysUserQuery query);

    /**
     * 超级管理员更新班级管理员信息
     * @param newSysUserInfo    班级管理员信息
     * @return                  更新行数
     */
    void updatePersonalInfoBySuperAdmin(SysUser newSysUserInfo);
}