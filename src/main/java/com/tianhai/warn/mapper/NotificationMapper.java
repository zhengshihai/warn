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
     * 简化分页：定向给某 targetId 的通知，或全体用户（ALL_USERS / 历史 allusers）通知
     */
    List<Notification> selectSimplePageForTarget(@Param("targetId") String targetId);

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

    /**
     * 更新ES索引状态
     * @param noticeId 通知ID
     * @param esIndexed 索引状态
     */
    int updateEsIndexed(@Param("noticeId") String noticeId, @Param("esIndexed") Integer esIndexed);

    /**
     * 批量更新ES索引状态
     * @param noticeIdList 通知ID列表
     * @param esIndexed 索引状态
     */
    int updateBatchEsIndexed(@Param("noticeIdList") List<String> noticeIdList, @Param("esIndexed") Integer esIndexed);

    /**
     * 查询未索引的通知
     * @return 未索引的通知列表
     */
    List<Notification> selectUnindexedNotifications();

    /**
     * 分批次查询未索引的通知
     * @param batchSize 批次大小
     * @return 未索引的通知列表
     */
    List<Notification> selectBatchUnindexed(@Param("batchSize") int batchSize);

    /**
     * 统计未索引的通知数量
     * @return 未索引的通知数量
     */
    int countUnindexedNotifications();

    /**
     * 查询已索引的通知
     * @return 已索引的通知列表
     */
    List<Notification> selectIndexedNotifications();
}