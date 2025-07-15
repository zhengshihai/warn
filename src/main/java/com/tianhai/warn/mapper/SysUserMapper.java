package com.tianhai.warn.mapper;

import com.tianhai.warn.model.SysUser;
import com.tianhai.warn.query.SysUserQuery;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Map;

/**
 * 班级管理员信息Mapper接口
 */
public interface SysUserMapper {

    /**
     * 根据ID查询班级管理员信息
     * 
     * @param id 班级管理员ID
     * @return 系统用户信息
     */
    SysUser selectById(Integer id);

    /**
     * 根据邮箱查询系统班级管理员
     * 
     * @param email 邮箱
     * @return 班级管理员信息
     */
    SysUser selectByEmail(String email);

    /**
     * 查询所有班级管理员信息
     * 
     * @return 班级管理员信息列表
     */
    List<SysUser> selectAll();

    /**
     * 根据条件查询班级管理员信息
     * 
     * @param query 查询条件
     * @return 班级管理员信息列表
     */
    List<SysUser> selectByCondition(SysUserQuery query);

    /**
     * 插入班级管理员
     * 
     * @param sysUser 班级管理员信息
     * @return 影响行数
     */
    int insert(SysUser sysUser);

    /**
     * 更新班级管理员信息
     * 
     * @param sysUser 班级管理员信息
     * @return 影响行数
     */
    int update(SysUser sysUser);

    /**
     * 删除班级管理员信息
     * 
     * @param id 班级管理员ID
     * @return 影响行数
     */
    int deleteById(Integer id);

    /**
     * 更新最后登录时间
     * 
     * @param id 班级管理员ID
     * @return 影响行数
     */
    int updateLastLoginTime(Integer id);

    /**
     * 根据角色查询班级管理员列表
     * 
     * @param role 角色
     * @return 班级管理员信息列表
     */
    List<SysUser> selectByRole(String role);

    /**
     * 根据用户编号列表查询班级管理员信息
     * 
     * @param sysUserNos 用户编号列表
     * @return 班级管理员信息列表
     */
    List<SysUser> selectBySysUserNos(@Param("sysUserNos") List<String> sysUserNos);

    /**
     * 批量插入班级管理员数据
     * 
     * @param sysUserList 班级管理员信息
     * @return 插入行数
     */
    int insertBatch(@Param("sysUserList") List<SysUser> sysUserList);

    /**
     * 获取班级管理员的工号和职位角色
     * @return    属性Map
     */
    List<Map<String, Object>> selectAllSysUserNoAndJobRole();
}