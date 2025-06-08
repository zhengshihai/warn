package com.tianhai.warn.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

// 报警处理记录实体类
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlarmProcessRecord {
    /**
     * 主键ID
     */
    private Long id;

    /**
     * 报警记录ID
     */
    private String alarmNo;

    /**
     * 处理方类型：1-学校安保，2-警方，3-医疗, 4-超级管理员
     */
    private Integer handlerType;

    /**
     * 处理方ID
     */
    private String handlerId;

    /**
     * 处理人姓名
     */
    private String handlerName;

    /**
     * 处理状态：0-待处理，1-处理中，2-已处理，3-已关闭
     */
    private Integer processStatus;

    /**
     * 处理结果
     */
    private String processResult;

    /**
     * 处理时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date processTime;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createdAt;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updatedAt;
}
