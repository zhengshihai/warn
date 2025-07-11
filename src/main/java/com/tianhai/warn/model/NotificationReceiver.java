package com.tianhai.warn.model;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 通知接收记录实体类
 */
@Data
public class NotificationReceiver implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID，自增
     */
    private Long id;

    /**
     * 通知ID（关联通知表）
     */
    private String noticeId;

    /**
     * 接收者的业务标识ID（如学号、工号，超级管理员为主键ID）
     */
    private String receiverId;

    /**
     * 接收者的角色
     * （如 STUDENT, DORMITORY_MANAGER, COUNSELOR, CLASS_TEACHER, DEAN, SUPER_ADMIN）
     */
    private String receiverRole;

    /**
     * 阅读状态（UNREAD / READ）
     */
    private String readStatus;

    /**
     * 阅读时间
     */
    private Date readTime;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;
}
