package com.tianhai.warn.query;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.tianhai.warn.annotation.AtLeastOneFieldNotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.Date;
import java.util.List;

@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@AtLeastOneFieldNotNull
public class NotificationQuery extends BaseQuery{
    /**
     * 精确匹配：通知ID
     */
    private String noticeId;

    /**
     * 阅读状态（UNREAD / READ / UNREAD）
     */
    private String readStatus;

    /**
     * 模糊查询：通知标题（支持 LIKE %title%）
     */
    private String titleLike;

    /**
     * 模糊查询：通知内容（支持 LIKE %content%）
     */
    private String contentLike;

    /**
     * 精确匹配：通知类型（如系统通知、晚归通知等）
     */
    private String noticeType;

    /**
     * 精确匹配：通知目标角色类型
     * （如 STUDENT, DORMITORY_MANAGER, COUNSELOR, CLASS_TEACHER, DEAN）
     *  当targetScope为ALL_USERS时，targetType为null
     */
    private String targetType;

    /**
     * 精确匹配：通知目标ID（业务ID）
     * 当targetScope为ALL_USERS或SPECIAL_ROLE时，targetId为null
     */
    private String targetId;

    /**
     * 精确匹配：通知范围（ALL_USERS / SPECIAL_ROLE / SPECIAL_USER）
     */
    private String targetScope;

    /**
     * 批量匹配：多个通知ID
     */
    private List<String> noticeIdList;

    /**
     * 批量匹配： 多个通知目标ID
     */
    private List<String> targetIdList;

    /**
     * 创建时间范围 - 开始
     */
    private Date createTimeStart;

    /**
     * 创建时间范围 - 结束
     */
    private Date createTimeEnd;

}
