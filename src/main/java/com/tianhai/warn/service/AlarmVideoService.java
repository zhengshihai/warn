package com.tianhai.warn.service;

import com.tianhai.warn.model.AlarmVideo;

import java.util.List;

public interface AlarmVideoService {
    int addAlarmVideo(AlarmVideo alarmVideo);

    int deleteAlarmVideoById(Long id);

    int updateAlarmVideoById(AlarmVideo alarmVideo);

    AlarmVideo getAlarmVideoById(Long id);

    List<AlarmVideo> getAllAlarmVideos();
}
