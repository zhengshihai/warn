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
     * 搜索通知信息列表
     * @param query    查询条件
     * @return         通知列表
     */
    List<Notification> selectByQuery(NotificationQuery query);

    /**
     * 插入通知信息
     * @param notification 通知
     * @return             插入行数
     */
    int insert(Notification notification);

    /**
     * 批量插入通知信息
     * @param notificationList    通知信息
     * @return                           插入行数
     */
    int insertBatch(@Param("notificationList") List<Notification> notificationList);

    /**
     * 批量删除通知
     * @param notQuery          删除条件
     * @return                  删除行数
     */
    int deleteBatch(NotificationQuery notQuery);

    /**
     * 获取所有通知的主要信息 （title, type, content)
     * @return              通知列表
     */
    List<Notification> selectAllNotificationMainInfo();

    /**
     * 按照一定顺序批量获取通知
     * @param offset        偏移量
     * @param limit         限制数量
     * @return              通知列表
     */
    List<Notification> selectBatchWithOffset(@Param("offset") int offset,@Param("limit") int limit);

    /**
     * 统计通知总数量
     * @return      通知总数量
     */
    int countAll();
}