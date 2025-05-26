package com.tianhai.warn.service;

import com.tianhai.warn.model.SystemRule;
import java.util.List;

/**
 * 系统规则服务接口
 */
public interface SystemRuleService {

    /**
     * 根据ID查询系统规则
     * 
     * @param id 系统规则ID
     * @return 系统规则
     */
    SystemRule selectById(Integer id);

    /**
     * 根据规则键查询系统规则
     * 
     * @param ruleKey 规则键
     * @return 系统规则
     */
    SystemRule selectByRuleKey(String ruleKey);

    /**
     * 查询所有系统规则
     * 
     * @return 系统规则列表
     */
    List<SystemRule> selectAll();

    /**
     * 根据条件查询系统规则
     * 
     * @param rule 查询条件
     * @return 系统规则列表
     */
    List<SystemRule> selectByCondition(SystemRule rule);

    /**
     * 插入系统规则
     * 
     * @param rule 系统规则
     * @return 影响行数
     */
    int insert(SystemRule rule);

    /**
     * 更新系统规则
     * 
     * @param rule 系统规则
     * @return 影响行数
     */
    int update(SystemRule rule);

    /**
     * 删除系统规则
     * 
     * @param id 系统规则ID
     * @return 影响行数
     */
    int deleteById(Integer id);

    /**
     * 根据规则键更新规则值
     * 
     * @param ruleKey   规则键
     * @param ruleValue 规则值
     * @return 影响行数
     */
    int updateRuleValue(String ruleKey, String ruleValue);
}