package com.tianhai.warn.service;

import com.tianhai.warn.vo.*;

import java.util.Date;
import java.util.List;
import java.util.Map;

public interface ReportService {
    ReportCardStatVO statsReportCardDataExcludeHighRisk(Date startDate, Date endDate, String college, String dormitoryBuilding);

    /**
     * 按照周一到周日统计晚归次数
     * @param startTime     起始统计时间
     * @param endTime       截至统计时间
     * @param college       学院
     * @param dormitoryBuilding  宿舍楼栋
     * @return              统计结果
     */
    List<WeekLateReturnStatVO> calWeekLateReturnStat(Date startTime,
                                                     Date endTime,
                                                     String college,
                                                     String dormitoryBuilding);

    /**
     * 按照宿舍门牌号与宿舍楼进行统计
     * @param startDate
     * @param endDate
     * @param college
     * @param dormitoryBuilding
     * @return
     */
    Map<String, List<DormitoryLateReturnStatVO>> calDormitoryLateReturnStat(Date startDate,
                                                                            Date endDate,
                                                                            String college,
                                                                            String dormitoryBuilding);

    /**
     * 按照学院进行统计
     * @param startTime
     * @param endTime
     * @param college
     * @param dormitoryBuilding
     * @return
     */
    List<CollegeLateReturnStatVO> calCollegeLateReturnStat(Date startTime,
                                                           Date endTime,
                                                           String college,
                                                           String dormitoryBuilding);

    /**
     * 按照时间点范围进行统计
     * @param startTime
     * @param endTime
     * @param college
     * @param dormitoryBuilding
     * @return
     */
    List<TimeRangeLateReturnStatVO> calTimeLateReturnStat(Date startTime,
                                                          Date endTime,
                                                          String college,
                                                          String dormitoryBuilding);
}
