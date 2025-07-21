package com.tianhai.warn.mapper;

import com.tianhai.warn.model.NotificationReceiver;
import com.tianhai.warn.query.NotificationReceiverQuery;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface NotificationReceiverMapper {
    /**
     * 条件查询通知接收信息
     * 
     * @param query 查询条件
     * @return 查询结果
     */
    List<NotificationReceiver> selectByCondition(NotificationReceiverQuery query);

    /**
     * 插入一条通知接收信息
     * 
     * @param notRec 通知接收信息
     * @return 插入行数
     */
    int insert(NotificationReceiver notRec);

    /**
     * 批量插入通知接收信息
     * 
     * @param notificationReceiverList 通知接收信息列表
     * @return 插入行数
     */
    int insertBatch(@Param("notificationReceiverList") List<NotificationReceiver> notificationReceiverList);

    /**
     * 批量更新通知接收信息
     * 
     * @param notificationReceiverList 通知接收信息列表
     * @return 更新行数
     */
    int updateBatch(@Param("notificationReceiverList") List<NotificationReceiver> notificationReceiverList);

    /**
     * 批量删除通知接收消息
     * @param notRecQuery       删除条件
     * @return                  删除行数
     */
    int deleteBatch(NotificationReceiverQuery notRecQuery);
}
