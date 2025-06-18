package com.tianhai.warn.mapper;

import com.tianhai.warn.model.LocationTrack;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

public interface LocationTrackMapper {
        Integer insert(LocationTrack track);

        List<LocationTrack> selectByTimeRange(String alarmNo, Date startTime, Date endTime);

        LocationTrack selectById(Long id);

        List<LocationTrack> selectByAlarmNo(String alarmNo);

        List<LocationTrack> selectByAlarmNoAndTimeRange(@Param("alarmNo") String alarmNo,
                        @Param("startTime") Date startTime,
                        @Param("endTime") Date endTime);

        Integer update(LocationTrack track);

        Integer deleteById(Long id);

        Integer deleteByAlarmNo(String alarmNo);

        Integer deleteByTimeRange(@Param("alarmNo") String alarmNo,
                        @Param("startTime") Date startTime,
                        @Param("endTime") Date endTime);

        List<LocationTrack> selectLatestByAlarmNo(String alarmNo, Integer limit);
}
