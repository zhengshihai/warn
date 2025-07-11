package com.tianhai.warn.service;

import com.tianhai.warn.model.NotificationReceiver;
import com.tianhai.warn.query.NotificationReceiverQuery;

import java.util.List;

public interface NotificationReceiverService {
    /**
     * 条件查询通知接收信息
     * @param query  查询条件
     * @return        通知接收信息列表
     */
    List<NotificationReceiver> selectByCondition(NotificationReceiverQuery query);

    /**
     * 插入通知接收信息
     * @param notRec   通知接收信息
     * @return         插入行数
     */
    int insert(NotificationReceiver notRec);

    /**
     * 批量插入通知接收信息
     * @param notificationReceiverList    通知接收信息列表
     * @return                            插入行数
     */
    int insertBatch(List<NotificationReceiver> notificationReceiverList);

    /**
     * 批量更新通知接收信息
     * @param existingNotRecList        通知接收信息列表
     * @return                          更新行数
     */
    int updateBatch(List<NotificationReceiver> existingNotRecList);
}
