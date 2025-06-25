package com.tianhai.warn.query;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.Date;
import java.util.List;

@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class ExplanationQuery extends BaseQuery{
    private Long id;
    private String lateReturnId; // 晚归记录id
    private String explanationId; // 晚归说明id
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

    private String descriptionLike; // 模糊查询说明

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date submitTimeStart; // 提交时间范围-起
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date submitTimeEnd; // 提交时间范围-止

    private List<String> lateReturnIds; // 晚归记录id列表
    private List<String> explanationIds; // 晚归说明id列表
    private List<String> studentNos; // 学号列表

    private List<Integer> auditStatuses; // 审核状态列表
    private List<String> auditPersons; // 审核人列表
    

}
