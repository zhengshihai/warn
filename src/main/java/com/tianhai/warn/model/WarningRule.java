package com.tianhai.warn.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * 预警规则实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WarningRule implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer id; // 主键ID
    private String ruleName; // 规则名称
    private Integer timeRangeDays; // 统计时间范围（天）
    private Integer maxLateTimes; // 最大晚归次数
    private String notifyTarget; // 通知对象（STUDENT/COUNSELOR/PARENT）
    private String notifyMethod; // 通知方式（SMS/EMAIL/WECHAT）
    private String status; // 规则状态（ENABLED/DISABLED）
    private String description; // 规则描述
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime; // 创建时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime; // 更新时间


    @Override
    public String toString() {
        return "WarningRule{" +
                "id=" + id +
                ", ruleName='" + ruleName + '\'' +
                ", timeRangeDays=" + timeRangeDays +
                ", maxLateTimes=" + maxLateTimes +
                ", notifyTarget='" + notifyTarget + '\'' +
                ", notifyMethod='" + notifyMethod + '\'' +
                ", status='" + status + '\'' +
                ", description='" + description + '\'' +
                ", createTime=" + createTime +
                ", updateTime=" + updateTime +
                '}';
    }
}