package com.tianhai.warn.mapper;

import com.tianhai.warn.model.SysUserClass;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * 用户-班级关联Mapper接口
 */
@Mapper
public interface SysUserClassMapper {
    /**
     * 根据班级管理员工号查询班级列表
     * 
     * @param sysUserNo 用户编号
     * @return 班级列表
     */
    List<String> selectClassesByUserNo(String sysUserNo);

    /**
     * 根据班级管理员工号删除关联数据
     * 
     * @param sysUserNo 用户编号
     * @return 影响行数
     */
    int deleteBySysUserNo(String sysUserNo);

    /**
     * 批量插入班级管理员-班级关联
     * 
     * @param list 班级管理员-班级关联列表
     * @return 影响行数
     */
    int batchInsert(List<SysUserClass> list);

    /**
     * 检查班级管理员是否有权限管理指定班级
     * 
     * @param sysUserNo 班级管理员工号
     * @param className 班级
     * @return 记录数
     */
    int countBySysUserNoAndClass(@Param("sysUserNo") String sysUserNo, @Param("className") String className);

    /**
     * 级联删除班级
     * 
     * @param className 班级
     */
    void deleteByClassName(String className);

    /**
     * 根据班级名称获取SysUserClass列表
     * 
     * @param className 班级名称
     * @return SysUserClass列表
     */
    List<SysUserClass> selectByClassName(String className);


    /**
     * 根据班级管理员工号更新表
     * @param sysUserClassList    更新信息
     * @return                    更新行数
     */
    int updateBatchBySysUserNo(@Param("sysUserClassList") List<SysUserClass> sysUserClassList);

    /**
     * 更新班级管理员的工号
     * @param oldSysUserNo     班级管理员旧工号
     * @param newSysUserNo     班级管理员新工号
     * @param updateTime       更新时间
     * @return                 更新行数
     */
    int updateSysUserNo(@Param("oldSysUserNo") String oldSysUserNo,
                        @Param("newSysUserNo") String newSysUserNo,
                        @Param("updateTime") Date updateTime);

    /**
     * 根据班级管理员工号统计
     * @param sysUserNo     班级管理员工号
     * @return              班级管理员-班级结果数
     */
    int countBySysUserNo(String sysUserNo);
}