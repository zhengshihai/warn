package com.tianhai.warn.query;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.Date;

@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class SystemLogQuery extends BaseQuery{
    // 精确匹配
    private String logId;
    private String userNo;
    private String status;
    private String userRole;

    // 模糊匹配
    private String usernameLike;
    private String operationLike;
    private String methodLike;
    private String ipLike;
    private String errorMsgLike;

    // 时间范围查询
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;
}
