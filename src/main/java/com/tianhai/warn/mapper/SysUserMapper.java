package com.tianhai.warn.mapper;

import com.tianhai.warn.model.SysUser;
import com.tianhai.warn.query.SysUserQuery;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 系统用户信息Mapper接口
 */
public interface SysUserMapper {

    /**
     * 根据ID查询系统用户信息
     * 
     * @param id 系统用户ID
     * @return 系统用户信息
     */
    SysUser selectById(Integer id);

    /**
     * 根据邮箱查询系统用户信息
     * 
     * @param email 邮箱
     * @return 系统用户信息
     */
    SysUser selectByEmail(String email);

    /**
     * 查询所有系统用户信息
     * 
     * @return 系统用户信息列表
     */
    List<SysUser> selectAll();

    /**
     * 根据条件查询系统用户信息
     * 
     * @param query 查询条件
     * @return 系统用户信息列表
     */
    List<SysUser> selectByCondition(SysUserQuery query);

    /**
     * 插入系统用户信息
     * 
     * @param sysUser 系统用户信息
     * @return 影响行数
     */
    int insert(SysUser sysUser);

    /**
     * 更新系统用户信息
     * 
     * @param sysUser 系统用户信息
     * @return 影响行数
     */
    int update(SysUser sysUser);

    /**
     * 删除系统用户信息
     * 
     * @param id 系统用户ID
     * @return 影响行数
     */
    int deleteById(Integer id);

    /**
     * 更新最后登录时间
     * 
     * @param id 系统用户ID
     * @return 影响行数
     */
    int updateLastLoginTime(Integer id);

    /**
     * 根据角色查询系统用户列表
     * 
     * @param role 角色
     * @return 系统用户信息列表
     */
    List<SysUser> selectByRole(String role);

    /**
     * 根据用户编号列表查询系统用户信息
     * 
     * @param sysUserNos 用户编号列表
     * @return 系统用户信息列表
     */
    List<SysUser> selectBySysUserNos(@Param("sysUserNos") List<String> sysUserNos);
}