package com.tianhai.warn.service.impl;

import com.tianhai.warn.enums.ResultCode;
import com.tianhai.warn.exception.SystemException;
import com.tianhai.warn.mapper.AlarmVideoSliceMapper;
import com.tianhai.warn.model.AlarmVideoSlice;
import com.tianhai.warn.service.AlarmVideoSliceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AlarmVideoSliceServiceImpl implements AlarmVideoSliceService {
    private static final Logger logger = LoggerFactory.getLogger(AlarmVideoSliceServiceImpl.class);

    @Autowired
    private AlarmVideoSliceMapper alarmVideoSliceMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addAlarmVideoSliceBatch(List<AlarmVideoSlice> alarmVideoSliceList) {
        // 每批插入的数据量
        int batchSize = 500;

        // 计算总批次数
        int total = alarmVideoSliceList.size();
        int batchCount = (total + batchSize - 1) / batchSize;

        // 分批插入
        try {
            for (int i = 0; i < batchCount; i++) {
                int fromIndex = i * batchSize;
                int toIndex = Math.min((i + 1) * batchSize, total);

                List<AlarmVideoSlice> batchList = alarmVideoSliceList.subList(fromIndex, toIndex);

                alarmVideoSliceMapper.insertBatch(batchList);
            }
        } catch (Exception e) {
            logger.error("批量插入报警视频切片信息失败", e);
            throw new SystemException(ResultCode.VIDEO_INFO_SAVED_FAILED);
        }
    }

    @Override
    public int addAlarmVideoSlice(AlarmVideoSlice alarmVideoSlice) {
        return alarmVideoSliceMapper.insert(alarmVideoSlice);
    }

    @Override
    public int deleteAlarmVideoSliceById(Long id) {
        return alarmVideoSliceMapper.deleteById(id);
    }

    @Override
    public int updateAlarmVideoSliceById(AlarmVideoSlice alarmVideoSlice) {
        return alarmVideoSliceMapper.updateById(alarmVideoSlice);
    }

    @Override
    public AlarmVideoSlice getAlarmVideoSliceById(Long id) {
        return alarmVideoSliceMapper.selectById(id);
    }

    @Override
    public List<AlarmVideoSlice> getAlarmVideoSlicesByVideoId(String videoId) {
        return alarmVideoSliceMapper.selectByVideoId(videoId);
    }

    @Override
    public List<AlarmVideoSlice> getAllAlarmVideoSlices() {
        return alarmVideoSliceMapper.selectAll();
    }
}
