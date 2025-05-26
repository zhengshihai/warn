package com.tianhai.warn.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.Builder;

import java.io.Serializable;
import java.util.Date;

/**
 * 系统日志实体类
 */
@Data
@Builder
public class SystemLog implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer id; // 主键ID
    private String logId; // 系统日志唯一ID
    private String userNo; // 用户编号（学号 工号 等）
    private String username; // 用户名
    private String userRole; // 用户角色
    private String operation; // 操作内容
    private String method; // 请求方法
    private String params; // 请求参数
    private String ip; // IP地址
    private String status; // 状态：成功/失败
    private String errorMsg; // 错误信息
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime; // 创建时间

    @Override
    public String toString() {
        return "SystemLog{" +
                "id=" + id +
                ", logId='" + logId + '\'' +
                ", userNo='" + userNo + '\'' +
                ", username='" + username + '\'' +
                ", userRole='" + userRole + '\'' +
                ", operation='" + operation + '\'' +
                ", method='" + method + '\'' +
                ", params='" + params + '\'' +
                ", ip='" + ip + '\'' +
                ", status='" + status + '\'' +
                ", errorMsg='" + errorMsg + '\'' +
                ", createTime=" + createTime +
                '}';
    }
}