package com.tianhai.warn.mapper;

import com.tianhai.warn.model.DormitoryManager;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 宿管信息Mapper接口
 */
public interface DormitoryManagerMapper {

    /**
     * 根据ID查询宿管信息
     * 
     * @param id 宿管ID
     * @return 宿管信息
     */
    DormitoryManager selectById(Integer id);

    /**
     * 根据邮箱查询宿管信息
     * 
     * @param email 邮箱
     * @return 宿管信息
     */
    DormitoryManager selectByEmail(String email);

    /**
     * 根据工号查询宿管信息
     * 
     * @param managerId 工号
     * @return 宿管信息
     */
    DormitoryManager selectByManagerId(String managerId);

    /**
     * 根据姓名查询宿管信息
     * 
     * @param name 姓名
     * @return 宿管信息
     */
    DormitoryManager selectByName(String name);

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
     * 更新宿管信息
     * 
     * @param manager 宿管信息
     * @return 影响行数
     */
    int update(DormitoryManager manager);

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
     * 更新最后登录时间
     * 
     * @param id
     */
    void updateLastLoginTime(Integer id);

    /**
     * 批量插入宿管信息
     * 
     * @param dormitoryManagerList 宿管信息列表
     * @return 插入成功的记录数
     */
    int insertBatch(@Param("dormitoryManagerList") List<DormitoryManager> dormitoryManagerList);
}