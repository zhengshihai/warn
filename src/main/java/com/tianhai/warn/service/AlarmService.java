package com.tianhai.warn.service;

import com.tianhai.warn.dto.CancelAlarmDTO;
import com.tianhai.warn.dto.LocationDTO;
import com.tianhai.warn.dto.LocationUpdateDTO;
import com.tianhai.warn.dto.OneClickAlarmDTO;
import com.tianhai.warn.model.AlarmRecord;
import com.tianhai.warn.vo.StudentAlarmContactsVO;

import java.util.List;
import java.util.Map;

public interface AlarmService {

    void processOneClickAlarm(OneClickAlarmDTO oneClickAlarmDTO);

    void cancelOneClickAlarm(CancelAlarmDTO cancelAlarmDTO);

    // 获取触发报警的学生的基本信息，报警信息，家长联系方式
    List<StudentAlarmContactsVO> searchStudentAlarmContactInfo(String helperNo, String role);

}
