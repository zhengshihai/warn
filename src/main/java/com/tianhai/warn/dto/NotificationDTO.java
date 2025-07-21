package com.tianhai.warn.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDTO {
    private String noticeId;

    private String receiverId; // 接收人ID（业务唯一标识，如学号、工号等）

    private String targetType; // 接收人角色（具体到职位角色）

    private String noticeType; // 通知类型 （系统通知/晚归通知/预警通知等）

    private String readStatus;

    private List<String> noticeIdList;

    private List<String> receiverIdList; // 接收人业务ID列表

    private String title; // 通知标题

    private String content; // 通知内容

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date createTimeStart; // 通知开始时间

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date createTimeEnd; // 通知结束时间
}
