package com.tianhai.warn.service;

import com.tianhai.warn.model.StudentLateStats;
import com.tianhai.warn.query.StudentLateStatsQuery;
import com.tianhai.warn.utils.PageResult;

import java.util.List;
import java.util.Map;

public interface StudentLateStatsService {
    /**
     * 根据ID获取学生晚归统计详情
     *
     * @param id 主键ID
     * @return 统计详情
     */
    StudentLateStats getById(Integer id);

    /**
     * 根据业务ID获取学生晚归统计详情
     *
     * @param statsId 业务ID
     * @return 统计详情
     */
    StudentLateStats getByStatsId(String statsId);

    /**
     * 根据条件查询学生晚归统计列表 (不分页)
     *
     * @param query 查询条件
     * @return 列表
     */
    List<StudentLateStats> selectList(StudentLateStatsQuery query);

    /**
     *  根据条件分页查询学生晚归统计列表
     *
     * @param studentLateStatsQuery 查询条件 (包含分页参数)
     * @return  分页结果
     */
    PageResult<StudentLateStats> selectByPageQuery(StudentLateStatsQuery studentLateStatsQuery);


    /**
     * 创建一条学生晚归统计记录 (通常由定时任务调用)
     *
     * @param stats 统计记录
     * @return 创建成功后的记录 (可能包含生成的ID)
     */
    StudentLateStats createStats(StudentLateStats stats);

    /**
     * 批量创建学生晚归统计记录 (通常由定时任务调用)
     *
     * @param statsList 统计记录列表
     * @return 成功插入的数量
     */
    int batchCreateStats(List<StudentLateStats> statsList);

    /**
     * 更新学生晚归统计记录
     *
     * @param stats 包含更新信息的统计记录
     * @return 更新后的记录
     */
    StudentLateStats updateStats(StudentLateStats stats);

    /**
     * 定时任务：生成并存储学生晚归统计数据的方法
     * (例如：统计上个月或最近30天的晚归数据)
     */
    void generateAndStorePeriodicLateReturnStats();

    /**
     * 根据ID删除统计记录
     *
     * @param id 主键ID
     * @return 是否成功
     */
    boolean deleteById(Integer id);

    /**
     * 根据业务ID删除统计记录
     *
     * @param statsId 统计记录ID
     * @return 是否成功
     */
    boolean deleteByStatsId(String statsId);

    /**
     * 根据条件删除统计记录
     *
     * @param studentLateStatsQuery 统计条件
     * @return                      删除的记录数
     */
    Integer deleteConditional(StudentLateStatsQuery studentLateStatsQuery);

    /**
     * 统计指定时间范围内的晚归次数
     * @param studentNo             学生学号
     * @param timeRangeDaysList     时间范围
     * @return                      统计结果
     */
    Map<Integer, Integer> getLateReturnCountsInDaysRange(String studentNo, List<Integer> timeRangeDaysList);

    /**
     * 批量更新学生晚归统计记录信息
     * @param statsList             学生晚归统计记录列表
     * @return                      更新行数
     */
    int updateBatch(List<StudentLateStats> statsList);
}
