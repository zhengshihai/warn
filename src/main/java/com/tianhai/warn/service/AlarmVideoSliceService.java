package com.tianhai.warn.service;

import com.tianhai.warn.model.AlarmVideoSlice;

import java.util.List;

public interface AlarmVideoSliceService {
    /**
     * 批量插入视频切片信息
     * @param alarmVideoSliceList       切片视频列表 todo
     */
    void addAlarmVideoSliceBatch(List<AlarmVideoSlice> alarmVideoSliceList);

    int addAlarmVideoSlice(AlarmVideoSlice alarmVideoSlice);

    int deleteAlarmVideoSliceById(Long id);

    int updateAlarmVideoSliceById(AlarmVideoSlice alarmVideoSlice);

    AlarmVideoSlice getAlarmVideoSliceById(Long id);

    List<AlarmVideoSlice> getAlarmVideoSlicesByVideoId(String videoId);

    List<AlarmVideoSlice> getAllAlarmVideoSlices();


}
