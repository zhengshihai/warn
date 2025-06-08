package com.tianhai.warn.query;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.util.Date;
import java.util.List;

@Getter
@Setter
public class AlarmRecordQuery extends BaseQuery{
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String alarmNo;
    private String studentNo;
    private Integer alarmType; // 报警类型：1-一键报警，2-接口报警
    private Integer alarmLevel;
    private Integer alarmStatus;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date alarmTime;
    private Double latitude;
    private Double longitude;
    private String locationAddress;
    private String description;
    private List<String> mediaUrls;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createdAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updatedAt;


    private String alarmNoLike; // 报警记录ID（模糊查询）
    private String studentNoLike; // 学生ID（模糊查询）

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date alarmTimeStart; //报警时间范围-起始时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date alarmTimeEnd; // 报警时间范围-起始时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createdAtStart; //创建时间范围-起始时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createdAtEnd; // 创建时间范围-结束时间

}
