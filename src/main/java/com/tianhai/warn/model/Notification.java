package com.tianhai.warn.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 通知实体类
 */
@Data
public class Notification implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer id; // 主键ID
    private String noticeId; // 通知唯一ID
    private String title; // 通知标题
    private String content; // 通知内容
    private String noticeType; // 通知类型：系统通知/晚归通知/预警通知
    private String targetType; // 目标类型：全部/学生/宿管/管理员
    private String targetId; // 目标ID（特定用户）
    private String status; // 状态：已读/未读
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime; // 创建时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime; // 更新时间

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
                ", status='" + status + '\'' +
                ", createTime=" + createTime +
                ", updateTime=" + updateTime +
                '}';
    }
}