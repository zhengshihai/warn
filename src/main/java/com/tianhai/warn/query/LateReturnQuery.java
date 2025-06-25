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
public class LateReturnQuery extends BaseQuery {
    private Long id; // 晚归主键id
    private String lateReturnId; // 晚归记录id
    private String studentNo;
    private String processStatus;
    private String processResult;
    private String processRemark;

    private String college;
    private String dormitory;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startLateTime; // 晚归时间范围-起
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endLateTime;

    private String studentName; // 学生姓名

    private String dormitoryLike; // 宿舍模糊查询
    private String processRemarkLike; // 审核备注模糊查询

    // 批量查询字段
    private List<String> studentNos; // 批量学号查询
    private List<String> colleges; // 批量学院查询
    private List<String> dormitories; // 批量宿舍查询
    private List<String> classes; // 批量班级查询
    private List<String> processResults; // 批量处理结果查询

}