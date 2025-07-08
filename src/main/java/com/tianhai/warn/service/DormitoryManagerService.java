package com.tianhai.warn.service;

import com.tianhai.warn.model.DormitoryManager;
import com.tianhai.warn.query.DormitoryManagerQuery;
import com.tianhai.warn.utils.PageResult;

import java.util.List;

/**
 * 宿管信息服务接口
 */
public interface DormitoryManagerService {

    /**
     * 根据ID查询宿管信息
     * 
     * @param id 宿管ID
     * @return 宿管信息
     */
    DormitoryManager selectById(Integer id);

    /**
     * 根据工号查询宿管信息
     * 
     * @param managerId 工号
     * @return 宿管信息
     */
    DormitoryManager selectByManagerId(String managerId);

    /**
     * 根据用户名查询宿管信息
     * 
     * @param username 用户名
     * @return 宿管信息
     */
    DormitoryManager selectByUsername(String username);

    /**
     * 查询所有宿管信息
     * 
     * @return 宿管信息列表
     */
    List<DormitoryManager> selectAll();

    /**
     * 根据条件查询宿管信息
     * 
     * @param manager 查询条件
     * @return 宿管信息列表
     */
    List<DormitoryManager> selectByCondition(DormitoryManager manager);

    /**
     * 插入宿管信息
     * 
     * @param manager 宿管信息
     * @return 影响行数
     */
    int insert(DormitoryManager manager);

    /**
     * 删除宿管信息
     * 
     * @param id 宿管ID
     * @return 影响行数
     */
    int deleteById(Integer id);

    /**
     * 根据宿舍楼查询宿管列表
     * 
     * @param building 宿舍楼
     * @return 宿管信息列表
     */
    List<DormitoryManager> selectByBuilding(String building);

    /**
     * 根据邮箱获取宿管信息
     */
    DormitoryManager getByEmail(String email);

    /**
     * 更新宿管个人信息
     *
     * @param manager     宿管信息
     * @param currentEmail 当前邮箱
     */
    void updatePersonalInfo(DormitoryManager manager, String currentEmail);

    DormitoryManager getDormanByEmail(String email);

    /**
     * 获取管理的宿舍
     * @param managerId   宿管工号
     * @return            宿舍列表
     */
    List<String> getManagedDormitories(String managerId);

    /**
     * 更新最后登录的时间
     * @param id        主键id
     */
    void updateLastLoginTime(Integer id);

    /**
     * 批量插入宿管信息
     *
     * @param dormitoryManagerList    宿管信息
     * @return                        插入行数
     */
    int insertBatch(List<DormitoryManager> dormitoryManagerList);

    /**
     * 分页查询宿管信息
     *
     * @param query    宿管查询条件
     * @return         分页结果
     */
    PageResult<DormitoryManager> selectByPageQuery(DormitoryManagerQuery query);

    /**
     * 超级管理员更新宿管个人信息
     * @param newDorManInfo    新的宿管信息
     * @return                 更新行数
     */
    int updatePersonalInfoBySuperAdmin(DormitoryManager newDorManInfo);

    /**
     * 超级管理员更新宿管状态
     * @param dormitoryManager    宿管信息
     * @return                    更新行数
     */
    int updateStatus(DormitoryManager dormitoryManager);
}