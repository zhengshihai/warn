package com.tianhai.warn.query;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.Date;
import java.util.List;

@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class NotificationReceiverQuery extends BaseQuery{
    /**
     * 主键ID，自增
     */
    private Long id;

    /**
     * 精确匹配：通知ID
     */
    private String noticeId;

    /**
     * 批量匹配：通知ID集合
     */
    private List<String> noticeIdList;

    /**
     * 精确匹配：接收者ID（学号/工号等）
     */
    private String receiverId;

    /**
     * 精确匹配：接收者角色（STUDENT、TEACHER等）
     */
    private String receiverRole;

    /**
     * 精确匹配：阅读状态（UNREAD / READ）
     */
    private String readStatus;

    /**
     * 时间范围 - 阅读时间开始
     */
    private Date readTimeStart;

    /**
     * 时间范围 - 阅读时间结束
     */
    private Date readTimeEnd;

    /**
     * 创建时间范围 - 开始
     */
    private Date createTimeStart;

    /**
     * 创建时间范围 - 结束
     */
    private Date createTimeEnd;
}
