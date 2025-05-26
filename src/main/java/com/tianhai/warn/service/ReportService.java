package com.tianhai.warn.service;

import com.tianhai.warn.vo.ReportVO;

import java.util.Date;

public interface ReportService {
    ReportVO statsReportCardData(Date startDate, Date endDate, String college, String dormitoryBuilding);
}
