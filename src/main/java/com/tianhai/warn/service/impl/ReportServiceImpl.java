package com.tianhai.warn.service.impl;

import com.tianhai.warn.constants.Constants;
import com.tianhai.warn.model.LateReturn;
import com.tianhai.warn.query.LateReturnQuery;
import com.tianhai.warn.service.LateReturnService;
import com.tianhai.warn.service.ReportService;
import com.tianhai.warn.utils.DateUtils;
import com.tianhai.warn.vo.ReportVO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class ReportServiceImpl implements ReportService {
    @Autowired
    private LateReturnService lateReturnService;

    @Override
    public ReportVO statsReportCardData(Date startDate,
                                        Date endDate,
                                        String college,
                                        String dormitoryBuilding) {
        Map<String, Date> timeRangeMap = DateUtils.resolveDateRange(startDate, endDate);
        Date startTime = timeRangeMap.get(Constants.START_TIME);
        Date endTime = timeRangeMap.get(Constants.END_TIME);

        // 没有正当理由的总晚归次数
        LateReturnQuery query = new LateReturnQuery();
        query.setStartLateTime(startTime);
        query.setEndLateTime(endTime);

        // 处理宿舍楼的范围匹配
        // 1. 改为null 让sql语句匹配所有宿舍楼
        if (dormitoryBuilding.equalsIgnoreCase(Constants.ALL)) {
            dormitoryBuilding = null;
        }
        // 2. 用宿舍楼的首字母进行模糊查询
        if (dormitoryBuilding != null && dormitoryBuilding.matches("^[A-Za-z]栋$")) {
            char firstChar = dormitoryBuilding.charAt(0);
            dormitoryBuilding = String.valueOf(Character.toUpperCase(firstChar));
        }
        // 3.设定条件
        query.setDormitoryLike(dormitoryBuilding);

        // 处理学院的范围匹配
        // 1. 改为null 让sql语句匹配所有学院
        if (college.equalsIgnoreCase(Constants.ALL)) {
            college = null;
        }
        // 2. 设定条件
        query.setCollege(college);

        // 获取所有晚归记录（包括违规于不违规）
        List<LateReturn> lateReturns = lateReturnService.selectByCondition(query);

        // 筛选违规的晚归记录
        List<LateReturn> unjustifiedLateReturns = lateReturns.stream()
                .filter(lr -> Constants.LATE_RETURN_PROCESS_STATUS_FINISHED.equals(lr.getProcessStatus())
                        && Constants.AUDIT_ACTION_REJECT.equals(lr.getProcessResult())
                        || StringUtils.isBlank(lr.getProcessResult()))
                .toList();

        // 获取卡片数据
        int totalLateReturns, lateStudentCount;
        totalLateReturns = unjustifiedLateReturns.isEmpty() ? 0 : unjustifiedLateReturns.size();

        lateStudentCount = unjustifiedLateReturns.isEmpty()
                ? 0
                : (int) unjustifiedLateReturns.stream()
                .map(LateReturn::getStudentNo)
                .filter(Objects::nonNull)
                .distinct()
                .count();

        int finishedCount = (int) unjustifiedLateReturns.stream()
                .filter(lr -> Constants.LATE_RETURN_PROCESS_STATUS_FINISHED.equals(lr.getProcessStatus()))
                .count();
        int unjustifiedCount = unjustifiedLateReturns.size();
        String completionRateStr;
        if (unjustifiedCount == 0) {
            completionRateStr = "100.00%"; // 没有数据默认设置为100.00%
        } else {
            BigDecimal completionRate = BigDecimal.valueOf(finishedCount * 100.0)
                    .divide(BigDecimal.valueOf(unjustifiedCount), 2, RoundingMode.HALF_UP);
            completionRateStr = completionRate.toPlainString() + "%";
        }

        // todo 根据不同条件 缓存结果到redis 时效性为当天

        return ReportVO.builder()
                .totalLateReturns(totalLateReturns)
                .lateStudentCount(lateStudentCount)
                .completionRate(completionRateStr)
                .build();
    }
}
