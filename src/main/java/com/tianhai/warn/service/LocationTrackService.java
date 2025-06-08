package com.tianhai.warn.service;

import com.tianhai.warn.dto.LocationUpdateDTO;
import com.tianhai.warn.model.LocationTrack;
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

}
