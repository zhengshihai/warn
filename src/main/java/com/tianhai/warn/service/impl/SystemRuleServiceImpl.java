package com.tianhai.warn.service.impl;

import com.tianhai.warn.mapper.SystemRuleMapper;
import com.tianhai.warn.model.SystemRule;
import com.tianhai.warn.service.SystemRuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 系统规则服务实现类
 */
@Service
public class SystemRuleServiceImpl implements SystemRuleService {

    @Autowired
    private SystemRuleMapper systemRuleMapper;

    @Override
    public SystemRule selectById(Integer id) {
        return systemRuleMapper.selectById(id);
    }

    @Override
    public SystemRule selectByRuleKey(String ruleKey) {
        return systemRuleMapper.selectByRuleKey(ruleKey);
    }

    @Override
    public List<SystemRule> selectAll() {
        return systemRuleMapper.selectAll();
    }

    @Override
    public List<SystemRule> selectByCondition(SystemRule rule) {
        return systemRuleMapper.selectByCondition(rule);
    }

    @Override
    public int insert(SystemRule rule) {
        return systemRuleMapper.insert(rule);
    }

    @Override
    public int update(SystemRule rule) {
        return systemRuleMapper.update(rule);
    }

    @Override
    public int deleteById(Integer id) {
        return systemRuleMapper.deleteById(id);
    }

    @Override
    public int updateRuleValue(String ruleKey, String ruleValue) {
        return systemRuleMapper.updateRuleValue(ruleKey, ruleValue);
    }
}