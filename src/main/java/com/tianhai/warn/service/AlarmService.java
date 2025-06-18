package com.tianhai.warn.service;

import com.tianhai.warn.dto.CancelAlarmDTO;
import com.tianhai.warn.dto.LocationDTO;
import com.tianhai.warn.dto.LocationUpdateDTO;
import com.tianhai.warn.dto.OneClickAlarmDTO;

import java.util.Map;

public interface AlarmService {

    void processOneClickAlarm(OneClickAlarmDTO oneClickAlarmDTO);

    void cancelOneClickAlarm(CancelAlarmDTO cancelAlarmDTO);

    /**
     * 获取地图信息
     * @param locationDTO 位置信息
     * @param amapKey 高德地图API Key
     * @return 地图信息
     */
//    Map<String, Object> getMapInfo(LocationDTO locationDTO, String amapKey);
}
