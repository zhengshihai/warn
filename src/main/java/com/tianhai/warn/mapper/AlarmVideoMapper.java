package com.tianhai.warn.mapper;

import com.tianhai.warn.model.AlarmVideo;
import io.lettuce.core.dynamic.annotation.Param;

import java.util.List;

public interface AlarmVideoMapper {
    int insert(AlarmVideo alarmVideo);

    int deleteById(@Param("id") Long id);

    int updateById(AlarmVideo alarmVideo);

    AlarmVideo selectById(@Param("id") Long id);

    List<AlarmVideo> selectAll();
}
