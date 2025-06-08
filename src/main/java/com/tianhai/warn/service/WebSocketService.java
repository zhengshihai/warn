package com.tianhai.warn.service;

import com.tianhai.warn.dto.LocationUpdateDTO;
import com.tianhai.warn.mq.AlarmContext;

public interface WebSocketService {
    void establishConnection(AlarmContext context);

    void handleLocationUpdate(LocationUpdateDTO locationUpdateDTO);
}
