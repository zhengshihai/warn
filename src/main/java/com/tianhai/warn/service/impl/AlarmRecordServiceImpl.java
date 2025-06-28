package com.tianhai.warn.service.impl;

import com.tianhai.warn.constants.AlarmConstants;
import com.tianhai.warn.dto.OneClickAlarmDTO;
import com.tianhai.warn.enums.AlarmStatus;
import com.tianhai.warn.enums.AlarmType;
import com.tianhai.warn.enums.ResultCode;
import com.tianhai.warn.exception.BusinessException;
import com.tianhai.warn.mapper.AlarmRecordMapper;
import com.tianhai.warn.model.AlarmConfig;
import com.tianhai.warn.model.AlarmRecord;
import com.tianhai.warn.query.AlarmRecordQuery;
import com.tianhai.warn.service.AlarmRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class AlarmRecordServiceImpl implements AlarmRecordService {
    @Autowired
    private AlarmRecordMapper alarmRecordMapper;

    @Override
    public AlarmRecord selectByAlarmNo(String alarmNo) {
        return alarmRecordMapper.selectByAlarmNo(alarmNo);
    }

    @Override
    public AlarmRecord selectById(Long id) {
        return alarmRecordMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insert(AlarmRecord record) {
        return alarmRecordMapper.insert(record);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int update(AlarmRecord record) {
        return alarmRecordMapper.update(record);
    }

    @Override
    public int updateBatch(List<AlarmRecord> alarmRecordList) {
        return alarmRecordMapper.updateBatch(alarmRecordList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteById(Long id) {
        return alarmRecordMapper.deleteById(id);
    }

    @Override
    public List<AlarmRecord> selectList(AlarmRecordQuery query) {
        return alarmRecordMapper.selectList(query);
    }

    @Override
    public int selectCount(AlarmRecordQuery query) {
        return alarmRecordMapper.selectCount(query);
    }

    @Override
    public List<AlarmRecord> selectPage(AlarmRecordQuery query) {
        return alarmRecordMapper.selectPage(query);
    }

    @Override
    public AlarmConfig selectByKey(String key) {
        return alarmRecordMapper.selectByKey(key);
    }
}
