package com.tianhai.warn.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.tianhai.warn.annotation.EsField;
import com.tianhai.warn.enums.EsFieldType;
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
//    @EsField(type = EsFieldType.INTEGER)
    private Integer id;

    /**
     * 通知ID，业务唯一标识
     */
//    @EsField(type = EsFieldType.KEYWORD)
    private String noticeId;

    /**
     * 通知标题
     */
//    @EsField(type = EsFieldType.TEXT, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String title;

    /**
     * 通知内容
     */
//    @EsField(type = EsFieldType.TEXT, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String content;

    /**
     * 通知类型（如系统通知、晚归通知、预警通知等）
     */
//    @EsField(type = EsFieldType.KEYWORD)
    private String noticeType;

    /**
     * 通知目标用户的角色类型
     * （如 STUDENT, DORMITORY_MANAGER, COUNSELOR, CLASS_TEACHER, DEAN, SUPER_ADMIN）
     */
//    @EsField(type = EsFieldType.KEYWORD)
    private String targetType;

    /**
     * 通知目标用户的业务ID（如学号、工号等）
     */
//    @EsField(type = EsFieldType.KEYWORD)
    private String targetId;

    /**
     * 通知对象范围（ALL_USERS: 全体用户；SPECIAL_ROLE: 特定角色；SPECIAL_USER: 特定用户）
     */
//    @EsField(type = EsFieldType.KEYWORD)
    private String targetScope;

    /**
     * 创建时间
     */
//    @EsField(type = EsFieldType.DATE, format = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /**
     * 更新时间
     */
//    @EsField(type = EsFieldType.DATE, format = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    // 简化业务 放弃使用ES
//    /**
//     * 是否已被ES索引 0-未索引，1-已索引
//     */
//    private Integer esIndexed;

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
//                ", esIndexed=" + esIndexed +
                '}';
    }

}