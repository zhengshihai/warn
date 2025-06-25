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
public class StudentLateStatsQuery extends BaseQuery{
    private Integer id; // 主键
    private String statsId; // 统计编号
    private String studentNo; // 支持按学号查
    private String schoolYear; // 支持按学年查
    private String semester; // 支持按学期查
    private String statsPeriodType; // 支持按统计周期类型查
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date statsPeriodStartDate; // 支持按周期起止查
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date statsPeriodEndDate;
    private Integer activeStatus; // 是否只查活跃统计 0表示不活跃 1表示活跃

    private List<String> studentNos; // 根据学号批量查询
    private List<String> statsIds; // 根据统计编号批量查询


}
