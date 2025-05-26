package com.tianhai.warn.mapper;

import com.tianhai.warn.model.WarningRule;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 预警规则Mapper接口
 */
public interface WarningRuleMapper {

    /**
     * 根据ID查询预警规则
     * 
     * @param id 预警规则ID
     * @return 预警规则
     */
    WarningRule selectById(Integer id);

    /**
     * 查询所有预警规则
     * 
     * @return 预警规则列表
     */
    List<WarningRule> selectAll();

    /**
     * 根据条件查询预警规则
     * 
     * @param rule 查询条件
     * @return 预警规则列表
     */
    List<WarningRule> selectByCondition(WarningRule rule);

    /**
     * 根据状态查询预警规则
     * 
     * @param status 状态
     * @return 预警规则列表
     */
    List<WarningRule> selectByStatus(String status);

    /**
     * 插入预警规则
     * 
     * @param rule 预警规则
     * @return 影响行数
     */
    int insert(WarningRule rule);

    /**
     * 更新预警规则
     * 
     * @param rule 预警规则
     * @return 影响行数
     */
    int update(WarningRule rule);

    /**
     * 删除预警规则
     * 
     * @param id 预警规则ID
     * @return 影响行数
     */
    int deleteById(Integer id);

    /**
     * 更新预警规则状态
     * 
     * @param id     预警规则ID
     * @param status 状态
     * @return 影响行数
     */
    int updateStatus(@Param("id") Integer id, @Param("status") String status);
}