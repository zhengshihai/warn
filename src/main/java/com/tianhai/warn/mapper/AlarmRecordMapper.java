package com.tianhai.warn.mapper;

import com.tianhai.warn.model.AlarmConfig;
import com.tianhai.warn.model.AlarmRecord;
import com.tianhai.warn.query.AlarmRecordQuery;

import java.util.List;

public interface AlarmRecordMapper {
    // 根据报警编号查询
    AlarmRecord selectByAlarmNo(String alarmNo);

    // 根据ID查询
    AlarmRecord selectById(Long id);

    // 新增报警记录
    int insert(AlarmRecord record);

    // 更新报警记录
    int update(AlarmRecord record);

    // 删除报警记录
    int deleteById(Long id);

    // 条件查询列表
    List<AlarmRecord> selectList(AlarmRecordQuery query);

    // 条件查询总数
    int selectCount(AlarmRecordQuery query);

    // 分页查询
    List<AlarmRecord> selectPage(AlarmRecordQuery query);

    // 配置相关
    AlarmConfig selectByKey(String key);

    // 查找待处理或正在处理的报警记录
    List<AlarmRecord> selectNotEndedAlarms();
}
