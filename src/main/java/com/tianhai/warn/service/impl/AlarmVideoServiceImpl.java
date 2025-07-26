package com.tianhai.warn.service.impl;

import com.tianhai.warn.mapper.AlarmVideoMapper;
import com.tianhai.warn.model.AlarmVideo;
import com.tianhai.warn.service.AlarmService;
import com.tianhai.warn.service.AlarmVideoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AlarmVideoServiceImpl implements AlarmVideoService {

    private static final Logger logger = LoggerFactory.getLogger(AlarmVideoServiceImpl.class);

    @Autowired
    private AlarmVideoMapper alarmVideoMapper;

    @Override
    public int addAlarmVideo(AlarmVideo alarmVideo) {
        if (alarmVideo == null) {
            logger.error("alarmVideo不能为空");
        }

        try {
            return alarmVideoMapper.insert(alarmVideo);
        } catch (Exception e) {
            logger.error("添加报警视频失败，异常信息：{}", e.getMessage());
            return 0; // 返回0表示添加失败
        }

    }

    @Override
    public int deleteAlarmVideoById(Long id) {
        return alarmVideoMapper.deleteById(id);
    }

    @Override
    public int updateAlarmVideoById(AlarmVideo alarmVideo) {
        return alarmVideoMapper.updateById(alarmVideo);
    }

    @Override
    public AlarmVideo getAlarmVideoById(Long id) {
        return alarmVideoMapper.selectById(id);
    }

    @Override
    public List<AlarmVideo> getAllAlarmVideos() {
        return alarmVideoMapper.selectAll();
    }
}
