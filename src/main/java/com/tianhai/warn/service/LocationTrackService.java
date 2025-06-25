package com.tianhai.warn.service;

import com.tianhai.warn.dto.LocationUpdateDTO;
import com.tianhai.warn.model.LocationTrack;
import com.tianhai.warn.vo.LatestLocationVO;
import org.springframework.data.geo.Point;

import java.util.Date;
import java.util.List;

public interface LocationTrackService {
    void handleLocationMessage(String locationMessage);

    void updateRealTimeLocation(LocationUpdateDTO locationDTO);

    void updateCacheLocationWithGeo(LocationUpdateDTO locationDTO);

    Integer saveLocationTrack(LocationUpdateDTO locationDTO);

    List<LocationTrack> getLocationHistory(String alarmNo, Date startTime, Date endTime);

    Point getCurrentLocation(String alarmNo);

    Integer insert(LocationTrack track);

    List<LocationTrack> selectByTimeRange(String alarmNo, Date startTime, Date endTime);

    LocationTrack selectById(Long id);

    /**
     * 设置指定报警编号的位置信息缓存在半小时后过期
     * 
     * @param alarmNo 报警编号
     */
    void expireLocationCacheByAlarmNo(String alarmNo);

    // 返回最近一定数量的位置记录
    List<LocationTrack> selectWithLimitByAlarmNo(String alarmNo, Integer amount);

    LatestLocationVO selectLastByAlarmNo(String alarmNo);

}
