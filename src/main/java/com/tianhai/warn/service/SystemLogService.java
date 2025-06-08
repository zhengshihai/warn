package com.tianhai.warn.service;

import com.tianhai.warn.model.SystemLog;
import java.util.List;
import java.util.Date;

/**
 * 系统日志服务接口
 */
public interface SystemLogService {

    /**
     * 根据ID查询系统日志
     *
     * @param id 系统日志ID
     * @return 系统日志
     */
    SystemLog selectById(Integer id);

    /**
     * 查询所有系统日志
     *
     * @return 系统日志列表
     */
    List<SystemLog> selectAll();

    /**
     * 根据条件查询系统日志
     *
     * @param log 查询条件
     * @return 系统日志列表
     */
    List<SystemLog> selectByCondition(SystemLog log);

    /**
     * 根据用户编号查询系统日志
     *
     * @param userNo 用户编号
     * @return 系统日志列表
     */
    List<SystemLog> selectByUserNo(String userNo);

    /**
     * 根据用户名查询系统日志
     *
     * @param username 用户名
     * @return 系统日志列表
     */
    List<SystemLog> selectByUsername(String username);

    /**
     * 根据用户角色查询系统日志
     *
     * @param userRole 用户角色
     * @return 系统日志列表
     */
    List<SystemLog> selectByUserRole(String userRole);

    /**
     * 根据操作内容查询系统日志
     *
     * @param operation 操作内容
     * @return 系统日志列表
     */
    List<SystemLog> selectByOperation(String operation);

    /**
     * 根据状态查询系统日志
     *
     * @param status 状态
     * @return 系统日志列表
     */
    List<SystemLog> selectByStatus(String status);

    /**
     * 根据时间范围查询系统日志
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 系统日志列表
     */
    List<SystemLog> selectByTimeRange(Date startTime, Date endTime);

    /**
     * 插入系统日志
     *
     * @param log 系统日志
     * @return 影响行数
     */
    int insert(SystemLog log);

    /**
     * 更新系统日志
     *
     * @param log 系统日志
     * @return 影响行数
     */
    int update(SystemLog log);

    /**
     * 删除系统日志
     *
     * @param id 系统日志ID
     * @return 影响行数
     */
    int deleteById(Integer id);

    /**
     * 批量删除系统日志
     *
     * @param ids 系统日志ID列表
     * @return 影响行数
     */
    int batchDelete(List<Integer> ids);

    /**
     * 清空指定时间之前的系统日志
     *
     * @param time 时间点
     * @return 影响行数
     */
    int deleteBeforeTime(Date time);

    /**
     * 根据logId查询系统日志
     *
     * @param logId 系统日志唯一ID
     * @return 系统日志
     */
    SystemLog selectByLogId(String logId);
}