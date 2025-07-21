package com.tianhai.warn.service;

import com.tianhai.warn.controller.NotificationController;
import com.tianhai.warn.dto.NotificationDTO;
import com.tianhai.warn.enums.NotificationSendMode;
import com.tianhai.warn.model.Notification;
import com.tianhai.warn.query.NotificationQuery;
import com.tianhai.warn.utils.PageResult;
import com.tianhai.warn.vo.NotificationVO;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 站内通知信息服务接口
 */
public interface NotificationService {
    /**
     * 分页搜索通知
     * @param query    查询条件
     * @return         分页结果
     */
    PageResult<NotificationVO> searchSpecialUserPageList(NotificationQuery query);

    /**
     * 条件搜索通知信息
     *
     * @param notificationQuery 查询条件
     * @return                  通知信息列表
     */
    List<Notification> selectByCondition(NotificationQuery notificationQuery);


    /**
     * 插入通知信息
     *
     * @param notification 通知信息
     * @return 影响行数
     */
    int insert(Notification notification);

    /**
     * 标记通知为已读
     * @param notificationDTO    通知数据传输对象
     * @return                   标记行数
     */
    int markRead(NotificationDTO notificationDTO);

    /**
     * 发送通知
     * @param notificationDTO    通知数据传输对象
     * @param sendMode           发送模式  （表示通过List<String> receiverIdList发送，还是通过String targetType发送）
     * @return                   无效的接收者ID列表
     */
    Map<String, Set<String>> sendNotification(NotificationDTO notificationDTO, NotificationSendMode sendMode);

    /**
     * 批量删除通知
     * @param notificationDTO    通知数据传输对象
     * @return                   删除行数
     */
    int deleteBatch(NotificationDTO notificationDTO);
}