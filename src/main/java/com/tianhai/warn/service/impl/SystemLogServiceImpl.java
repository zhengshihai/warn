package com.tianhai.warn.service.impl;

import com.tianhai.warn.mapper.SystemLogMapper;
import com.tianhai.warn.model.SystemLog;
import com.tianhai.warn.service.SystemLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * 系统日志服务实现类
 */
@Service
public class SystemLogServiceImpl implements SystemLogService {

    @Autowired
    private SystemLogMapper systemLogMapper;

    @Override
    public SystemLog selectById(Integer id) {
        return systemLogMapper.selectById(id);
    }

    @Override
    public List<SystemLog> selectAll() {
        return systemLogMapper.selectAll();
    }

    @Override
    public List<SystemLog> selectByCondition(SystemLog log) {
        return systemLogMapper.selectByCondition(log);
    }

    @Override
    public List<SystemLog> selectByUserNo(String userNo) {
        return systemLogMapper.selectByUserNo(userNo);
    }

    @Override
    public List<SystemLog> selectByUsername(String username) {
        return systemLogMapper.selectByUsername(username);
    }

    @Override
    public List<SystemLog> selectByUserRole(String userRole) {
        return systemLogMapper.selectByUserRole(userRole);
    }

    @Override
    public List<SystemLog> selectByOperation(String operation) {
        return systemLogMapper.selectByOperation(operation);
    }

    @Override
    public List<SystemLog> selectByStatus(String status) {
        return systemLogMapper.selectByStatus(status);
    }

    @Override
    public List<SystemLog> selectByTimeRange(Date startTime, Date endTime) {
        return systemLogMapper.selectByTimeRange(startTime, endTime);
    }

    @Override
    public int insert(SystemLog log) {
        return systemLogMapper.insert(log);
    }

    @Override
    public int update(SystemLog log) {
        return systemLogMapper.update(log);
    }

    @Override
    public int deleteById(Integer id) {
        return systemLogMapper.deleteById(id);
    }

    @Override
    public int batchDelete(List<Integer> ids) {
        return systemLogMapper.batchDelete(ids);
    }

    @Override
    public int deleteBeforeTime(Date time) {
        return systemLogMapper.deleteBeforeTime(time);
    }

    @Override
    public SystemLog selectByLogId(String logId) {
        return systemLogMapper.selectByLogId(logId);
    }
}