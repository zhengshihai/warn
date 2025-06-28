package com.tianhai.warn.mapper;

import com.tianhai.warn.model.Notification;
import com.tianhai.warn.query.NotificationQuery;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 通知信息Mapper接口
 */
public interface NotificationMapper {

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
    List<Notification> selectByTarget(@Param("targetType") String targetType, @Param("targetId") String targetId);

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
     * @param noticeId 通知记录id
     * @param status   状态
     * @return 影响行数
     */
    int updateStatus(@Param("noticeId") String noticeId, @Param("status") String status);

    /**
     * 批量更新通知状态
     * 
     * @param ids    通知ID列表
     * @param status 状态
     * @return 影响行数
     */
    int batchUpdateStatus(@Param("ids") List<String> ids, @Param("status") String status);

    /**
     * 根据noticeId查询通知信息
     * 
     * @param noticeId 通知唯一ID
     * @return 通知信息
     */
    Notification selectByNoticeId(String noticeId);

    /**
     * 批量更新通知信息
     * 
     * @param notificationList 通知信息列表
     * @return 影响行数
     */
    int updateBatch(@Param("notificationList") List<Notification> notificationList);
}