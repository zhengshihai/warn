package com.tianhai.warn.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 学生晚归统计
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentLateStats implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Integer id; // 主键ID

    private String statsId; // 使用StatsIdGenerator生成

    private String studentNo; // 学生学号

    private Integer lateReturnCount; // 统计周期内的晚归次数

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date statsPeriodStartDate; // 统计周期的开始日期

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date statsPeriodEndDate; // 统计周期的结束日期

    private String statsPeriodType; //  统计周期类型，例如 "FIXED_MONTHLY", "LAST_30_DAYS", 
    // 这个字段可以帮助区分不同类型的统计记录

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastUpdatedTime; // 这条统计记录的最后更新时间，方便跟踪数据新鲜度

    private String schoolYear; // 学年，例如 "2023-2024"

    private String semester; // 学期，例如 "SPRING"表示第一学期", FALL表示"第二学期"

    private Integer activeStatus;   // 标记这条统计是否为当前“活跃”或“最新”的统计 0表示不活跃 1表示活跃


}
