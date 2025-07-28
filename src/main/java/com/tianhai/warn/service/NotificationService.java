package com.tianhai.warn.service;

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

    /**
     * 统计通知总数量
     * @return          通知总数量
     */
    int countAll();

// ------------------------------以下为Elasticsearch接口---------------------------------------------------

    /**
     * 索引通知文档
     * @param notification      通知信息
     */
    void indexNotification(Notification notification);

    /**
     * 索引通知列表
     * @param notificationList  通知信息列表
     */
    void indexNotificationList(List<Notification> notificationList);

    /**
     * 根据文档ID删除ES索引
     * @param indexId    诸如notificationId的文档id
     */
    void deleteEsIndexByNotId(String indexId);

    /**
     * 使用Elasticsearch查询通知列表
     * @param notificationQuery     查询条件
     * @return                      分页结果
     */
    PageResult<NotificationVO> searchNotificationListPageByEs(NotificationQuery notificationQuery);

    /**
     * ES全文检索通知（包括检索其他角色）
     * @param keyword       检索关键词
     * @return              通知信息列表
     */
    List<Notification> fullTextSearchWithoutLimited(String keyword);

    /**
     * ES全文检索通知（仅限当前职位角色和业务标识ID）
     * @param keyword           检索关键词
     * @param searcherRole      检索者职位角色（具体到用户角色）
     * @param searcherNo        检索者业务标识ID（如学号、工号等）
     * @return                  通知信息列表
     */
    List<Notification> fullTextSearchWithRoleLimited(String keyword, String searcherRole, String searcherNo);

    /**
     * 重建通知ES索引
     * @param indexName  ES索引名称
     */
    void rebuildIndex(String indexName);

    /**
     * 创建通知索引
     */
    void createIndex();

    /**
     * 检查通知索引是否存在
     * @param indexName  ES索引名称
     * @return      存在为true
     */
    boolean validateIndexExists(String indexName);

    /**
     * 删除指定名称的ES索引
     * @param indexName     ES索引名称
     */
    boolean deleteEsIndexByIndexName(String indexName);
}