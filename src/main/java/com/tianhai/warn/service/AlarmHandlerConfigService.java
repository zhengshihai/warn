package com.tianhai.warn.service;

import com.tianhai.warn.model.AlarmHandlerConfig;
import org.springframework.stereotype.Service;

import java.util.List;


public interface AlarmHandlerConfigService {
    List<AlarmHandlerConfig> selectActiveHandlers();
}
