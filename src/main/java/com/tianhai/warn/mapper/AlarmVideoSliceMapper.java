package com.tianhai.warn.mapper;

import com.tianhai.warn.model.AlarmVideoSlice;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 视频切片接口
 */
public interface AlarmVideoSliceMapper {
    int insert(AlarmVideoSlice alarmVideoSlice);

    int insertBatch(List<AlarmVideoSlice> alarmVideoSliceList);

    int deleteById(@Param("id") Long id);

    int updateById(AlarmVideoSlice alarmVideoSlice);

    AlarmVideoSlice selectById(@Param("id") Long id);

    List<AlarmVideoSlice> selectByVideoId(@Param("videoId") String videoId);

    List<AlarmVideoSlice> selectAll();
}
