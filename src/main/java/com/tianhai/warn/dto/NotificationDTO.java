package com.tianhai.warn.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDTO {
    private String noticeId;

    private String receiverId; // 接收人ID

    private String targetType; // 接收人角色（具体到职位角色）

    private String readStatus;

    private List<String> noticeIdList;
}
