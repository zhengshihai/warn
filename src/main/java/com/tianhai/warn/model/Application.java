package com.tianhai.warn.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * 晚归报备申请实体类
 */
@Data
public class Application implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String studentNo;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date expectedReturnTime;
    private String reason;
    private String destination;
    private String description;
    private String attachmentUrl;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date applyTime;
    private Integer auditStatus; // 0待审核,1通过,2驳回
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date auditTime;
    private String auditPerson;
    private String auditRemark;
    private String applicationId; // 晚归报备申请唯一ID

    @Override
    public String toString() {
        return "LateReturnApplication{" +
                "id=" + id +
                ", applicationId='" + applicationId + '\'' +
                ", studentNo='" + studentNo + '\'' +
                ", expectedReturnTime=" + expectedReturnTime +
                ", reason='" + reason + '\'' +
                ", destination='" + destination + '\'' +
                ", description='" + description + '\'' +
                ", attachmentUrl='" + attachmentUrl + '\'' +
                ", applyTime=" + applyTime +
                ", auditStatus=" + auditStatus +
                ", auditTime=" + auditTime +
                ", auditPerson='" + auditPerson + '\'' +
                ", auditRemark='" + auditRemark + '\'' +
                '}';
    }
}