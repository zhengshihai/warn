package com.tianhai.warn.mapper;

import com.tianhai.warn.model.AlarmHandlerConfig;

import java.util.List;

public interface AlarmHandlerConfigMapper {
    // todo
    List<AlarmHandlerConfig> selectActiveHandlers();
}
