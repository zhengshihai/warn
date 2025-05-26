package com.tianhai.warn.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

/**
 * 晚归情况说明实体类
 */
@Data
public class Explanation implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String lateReturnId;
    private String studentNo;
    private String description;
    private String attachmentUrl;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date submitTime;
    private Integer auditStatus; // 0待审核,1通过,2驳回
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date auditTime;
    private String auditPerson;
    private String auditRemark;
    private String explanationId; // 晚归情况说明唯一ID

    @Override
    public String toString() {
        return "LateReturnExplanation{" +
                "id=" + id +
                ", explanationId='" + explanationId + '\'' +
                ", lateReturnId=" + lateReturnId +
                ", studentNo='" + studentNo + '\'' +
                ", description='" + description + '\'' +
                ", attachmentUrl='" + attachmentUrl + '\'' +
                ", submitTime=" + submitTime +
                ", auditStatus=" + auditStatus +
                ", auditTime=" + auditTime +
                ", auditPerson='" + auditPerson + '\'' +
                ", auditRemark='" + auditRemark + '\'' +
                '}';
    }
}