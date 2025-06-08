package com.tianhai.warn.service.impl;

import com.tianhai.warn.model.AlarmHandlerConfig;
import com.tianhai.warn.service.AlarmHandlerConfigService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AlarmHandlerConfigServiceImpl implements AlarmHandlerConfigService {
    @Override
    public List<AlarmHandlerConfig> selectActiveHandlers() {
        return List.of();
    }
}
