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

    private Integer id;
    private String logId;
    private String userNo;
    private String username;
    private String userRole;
    private String operation;
    private String method;
    private String params;
    private String ip;
    private String status;
    private String errorMsg;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}