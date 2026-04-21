package com.tianhai.warn.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class ApplicationAuditDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private String applicationId; // 申请ID
    private Integer auditStatus; // 审核状态（1通过，2拒绝）
    private String auditPerson; // 审核人
    private String auditRemark; // 审核备注
}
