package com.tianhai.warn.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 通知实体类
 */
@Data
public class Notification implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

   
    /**
     * 主键ID，自增
     */
    private Integer id;

    /**
     * 通知ID，业务唯一标识
     */
    private String noticeId;

    /**
     * 通知标题
     */
    private String title;

    /**
     * 通知内容
     */
    private String content;

    /**
     * 通知类型（如系统通知、晚归通知、预警通知等）
     */
    private String noticeType;

    /**
     * 通知目标用户的角色类型
     * （如 STUDENT, DORMITORY_MANAGER, COUNSELOR, CLASS_TEACHER, DEAN, SUPER_ADMIN）
     */
    private String targetType;

    /**
     * 通知目标用户的业务ID（如学号、工号等）
     */
    private String targetId;

    /**
     * 通知对象范围（ALL_USERS: 全体用户；SPECIAL_ROLE: 特定角色；SPECIAL_USER: 特定用户）
     */
    private String targetScope;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    @Override
    public String toString() {
        return "Notification{" +
                "id=" + id +
                ", noticeId='" + noticeId + '\'' +
                ", title='" + title + '\'' +
                ", content='" + content + '\'' +
                ", noticeType='" + noticeType + '\'' +
                ", targetType='" + targetType + '\'' +
                ", targetId='" + targetId + '\'' +
                ", targetScope='" + targetScope + '\'' +
                ", createTime=" + createTime +
                ", updateTime=" + updateTime +
                '}';
    }

}