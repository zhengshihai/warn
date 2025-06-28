package com.tianhai.warn.service;

import com.tianhai.warn.enums.AlarmLevel;
import com.tianhai.warn.model.Notification;
import com.tianhai.warn.mq.AlarmContext;
import com.tianhai.warn.query.NotificationQuery;
import com.tianhai.warn.utils.PageResult;

import java.util.List;
import java.util.Map;

/**
 * 通知信息服务接口
 */
public interface NotificationService {

    /**
     * 根据ID查询通知信息
     * 
     * @param id 通知ID
     * @return 通知信息
     */
    Notification selectById(Integer id);

    /**
     * 查询所有通知信息
     * 
     * @return 通知信息列表
     */
    List<Notification> selectAll();

    /**
     * 根据条件查询通知信息
     * 
     * @param notificationQuery 查询条件
     * @return 通知信息列表
     */
    List<Notification> selectByCondition(NotificationQuery notificationQuery);

    /**
     * 根据目标类型和目标ID查询通知信息
     * 
     * @param targetType 目标类型
     * @param targetId   目标ID
     * @return 通知信息列表
     */
    List<Notification> selectByTarget(String targetType, String targetId);

    /**
     * 根据通知类型查询通知信息
     * 
     * @param type 通知类型
     * @return 通知信息列表
     */
    List<Notification> selectByType(String type);

    /**
     * 根据状态查询通知信息
     * 
     * @param status 状态
     * @return 通知信息列表
     */
    List<Notification> selectByStatus(String status);

    /**
     * 插入通知信息
     * 
     * @param notification 通知信息
     * @return 影响行数
     */
    int insert(Notification notification);

    /**
     * 更新通知信息
     * 
     * @param notification 通知信息
     * @return 影响行数
     */
    int update(Notification notification);

    /**
     * 删除通知信息
     * 
     * @param id 通知ID
     * @return 影响行数
     */
    int deleteById(Integer id);

    /**
     * 更新通知状态
     * 
     * @param notificationId 通知记录Id
     * @param status         状态
     * @return 影响行数
     */
    int updateStatus(String notificationId, String status);

    /**
     * 批量更新通知状态
     * 
     * @param ids    通知ID列表
     * @param status 状态
     * @return 影响行数
     */
    int batchUpdateStatus(List<String> ids, String status);

    /**
     * 根据noticeId查询通知信息
     * 
     * @param noticeId 通知唯一ID
     * @return 通知信息
     */
    Notification selectByNoticeId(String noticeId);

    /**
     * 根据用户角色和当前用户获取通知
     * 
     * @param userRoleStr
     * @param user
     * @param query
     * @return
     */
    PageResult<Notification> selectByPageQuery(String userRoleStr, Object user, NotificationQuery query);

    /**
     * 获取通知统计信息
     *
     * @param currentUser 当前用户
     * @return 通知统计信息
     */
    Map<String, Object> getNotificationStats(Object currentUser);

    /**
     * 检查当前用户是否有权限查看通知
     * 
     * @param userRoleStr
     * @param currentUser
     * @param noticeId
     * @return
     */
    boolean hasPermissionToRead(String userRoleStr, Object currentUser, String noticeId);

    /**
     * 批量检查当前用户是否有权限查看通知
     * 
     * @param userRoleStr
     * @param currentUser
     * @param noticeIds
     * @return
     */
    boolean hasPermissionToReadBatch(String userRoleStr, Object currentUser, List<String> noticeIds);

    /**
     * 发送报警通知 todo
     * 
     * @return
     */
    Integer sendOneClickAlarmNotification(String studentNo, AlarmLevel alarmLevel);

    /**
     * 批量更新通知信息
     * 
     * @param notificationList 通知信息列表
     * @return 影响行数
     */
    int updateBatch(List<Notification> notificationList);
}