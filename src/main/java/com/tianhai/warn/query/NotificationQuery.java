package com.tianhai.warn.query;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;

@Getter
@Setter
public class NotificationQuery extends BaseQuery{

    private String noticeId; // 通知唯一ID
    private String title; // 通知标题
    private String content; // 通知内容
    private String noticeType; // 通知类型：系统通知/晚归通知/预警通知
    private String targetType; // 目标类型：全部/学生/宿管/管理员
    private String targetId; // 目标ID（特定用户）
    private String status; // 状态：已读/未读

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startNoticeTime; // 通知创建时间范围-起
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endNoticeTime;

    private String titleLike;//通知标题模糊查询
    private String contentLike;//通知内容模糊查询

    //批量查询字段
    private List<String> noticeIds;
    private List<String> noticeTypes;
    private List<String> targetIds;


}
