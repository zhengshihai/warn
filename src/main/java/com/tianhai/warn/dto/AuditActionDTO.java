package com.tianhai.warn.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class AuditActionDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;


    private String jobRole; // 用户角色字符串

    @NotBlank(message = "审核人不能为空")
    private String auditPerson; // 审核人的工号
    private String auditRemark;
    @NotNull(message = "审核状态不能为空")
    private Integer auditStatus; // 0待审核,1通过,2驳回
    @NotBlank(message = "晚归记录id不能为空")
    private String lateReturnId; // 晚归记录id

    private String explanationId; // 晚归说明id

    @NotBlank(message = "处理状态不能为空")
    private String processStatus;
    private String processResult;
    @NotBlank(message = "审核备注不能为空")
    private String processRemark;

    private String studentNo;
}
