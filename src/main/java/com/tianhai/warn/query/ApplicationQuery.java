package com.tianhai.warn.query;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.tianhai.warn.annotation.AtLeastOneFieldNotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.Date;
import java.util.List;

@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@AtLeastOneFieldNotNull
public class ApplicationQuery extends BaseQuery {
    private Long id;
    private String applicationId; // 申请ID
    private String studentNo;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date expectedReturnTime;
    private String reason;
    private String attachmentUrl;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date applyTime;
    private Integer auditStatus; // 0待审核,1通过,2驳回
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date auditTime;
    private String auditPerson; // 审核人工号
    private String auditRemark;

    // 批量查询条件
    private List<String> studentNos; // 学号列表
    private List<Integer> auditStatuses; // 审核状态列表
    private List<String> auditPersons; // 审核人工号列表

    // 时间范围查询
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date expectedReturnTimeStart; // 预期返校时间范围-开始
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date expectedReturnTimeEnd; // 预期返校时间范围-结束
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date applyTimeStart; // 申请时间范围-开始
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date applyTimeEnd; // 申请时间范围-结束
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date auditTimeStart; // 审核时间范围-开始
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date auditTimeEnd; // 审核时间范围-结束

    // 模糊查询
    private String reasonLike; // 模糊查询原因
}
