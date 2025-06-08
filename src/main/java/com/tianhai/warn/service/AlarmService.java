package com.tianhai.warn.service;

import com.tianhai.warn.dto.LocationUpdateDTO;
import com.tianhai.warn.dto.OneClickAlarmDTO;

public interface AlarmService {

    void processOneClickAlarm(OneClickAlarmDTO oneClickAlarmDTO);
}
