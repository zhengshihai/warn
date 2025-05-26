package com.tianhai.warn.mapper;

import com.tianhai.warn.model.StudentLateStats;
import com.tianhai.warn.query.StudentLateStatsQuery;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface StudentLateStatsMapper {
    /**
     * 根据主键ID查询学生晚归统计记录
     *
     * @param id 主键ID
     * @return 学生晚归统计记录
     */
    StudentLateStats selectById(Integer id);

    /**
     * 根据业务ID (statsId) 查询学生晚归统计记录
     *
     * @param statsId 业务ID
     * @return 学生晚归统计记录
     */
    StudentLateStats selectByStatsId(String statsId);

    /**
     * 根据条件查询学生晚归统计记录列表
     *
     * @param query 查询条件
     * @return 学生晚归统计记录列表
     */
    List<StudentLateStats> selectByCondition(StudentLateStatsQuery query);

    /**
     * 插入一条学生晚归统计记录
     *
     * @param stats 待插入的统计记录
     * @return 影响行数
     */
    int insert(StudentLateStats stats);

    /**
     * 批量插入学生晚归统计记录
     *
     * @param statsList 待插入的统计记录列表
     * @return 影响行数
     */
    int batchInsert(@Param("statsList") List<StudentLateStats> statsList);

    /**
     * 更新一条学生晚归统计记录 (根据主键ID)
     * 通常用于更新 lateReturnCount, lastUpdatedTime, isActive 等字段
     *
     * @param stats 包含更新信息的统计记录
     * @return 影响行数
     */
    int updateById(StudentLateStats stats);


    /**
     * 批量更新 isActive 状态 (例如，将旧的统计标记为不活跃)
     *
     * @param studentNos 学生学号列表
     * @param schoolYear 学年
     * @param semester 学期
     * @param statsPeriodType 统计周期类型
     * @param isActive 更新后的状态 (0 或 1)
     * @return 影响行数
     */
    int batchUpdateIsActive(@Param("studentNos") List<String> studentNos,
                            @Param("schoolYear") String schoolYear,
                            @Param("semester") String semester,
                            @Param("statsPeriodType") String statsPeriodType,
                            @Param("isActive") Integer isActive);


    /**
     * (可选) 根据主键ID删除一条统计记录
     *
     * @param id 主键ID
     * @return 影响行数
     */
    int deleteById(Integer id);

    /**
     * (可选) 根据业务ID (statsId) 删除一条统计记录
     *
     * @param statsId 业务ID
     * @return 影响行数
     */
    int deleteByStatsId(String statsId);

    /**
     * (可选) 根据特定条件批量删除统计记录
     * 例如：删除某个学期之前的所有非活跃记录
     * @param query 删除条件
     * @return 影响行数
     */
    int deleteByQuery(StudentLateStatsQuery query);

}
