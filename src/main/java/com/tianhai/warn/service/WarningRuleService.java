package com.tianhai.warn.service;

import com.tianhai.warn.model.CalculationResult;
import com.tianhai.warn.model.WarningRule;

import java.util.Date;
import java.util.List;

/**
 * 预警规则服务接口
 */
public interface WarningRuleService {

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
    int updateStatus(Integer id, String status);

    /**
     * 统计高危预警学号名单（重点关注对象名单）
     * @param startTime     起始时间
     * @param endTime       截至时间
     * @return              响应结果
     */
    CalculationResult calHighRiskStudents(Date startTime, Date endTime);

    /**
     * 获取任务状态
     * @param taskId        任务ID
     * @return              任务状态
     */
    CalculationResult getTaskStatus(String taskId);
}