package com.tianhai.warn.mapper;

import com.tianhai.warn.dto.StudentLateResultDTO;
import com.tianhai.warn.model.LateReturn;
import com.tianhai.warn.query.LateReturnQuery;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import java.util.Date;
import java.util.Map;
import java.util.Set;

/**
 * 晚归记录Mapper接口
 */
public interface LateReturnMapper {

    /**
     * 根据ID查询晚归记录
     * 
     * @param id 晚归记录ID
     * @return 晚归记录
     */
    LateReturn selectById(Long id);

    /**
     * 根据晚归记录ID查询晚归记录
     * 
     * @param lateReturnId 晚归记录ID
     * @return 晚归记录
     */
    LateReturn selectByLateReturnId(String lateReturnId);

    /**
     * 查询所有晚归记录
     * 
     * @return 晚归记录列表
     */
    List<LateReturn> selectAll();

    /**
     * 根据条件查询晚归记录
     * 
     * @param query 查询条件
     * @return 晚归记录列表
     */
    // List<LateReturn> selectByCondition(LateReturn lateReturn);
    List<LateReturn> selectByCondition(LateReturnQuery query);

    /**
     * 根据学号查询晚归记录
     * 
     * @param studentNo 学号
     * @return 晚归记录列表
     */
    List<LateReturn> selectByStudentNo(String studentNo);

    /**
     * 根据处理状态查询晚归记录
     * 
     * @param processStatus 处理状态
     * @return 晚归记录列表
     */
    List<LateReturn> selectByProcessStatus(String processStatus);

    /**
     * 根据处理结果查询晚归记录
     * 
     * @param processResult 处理结果
     * @return 晚归记录列表
     */
    List<LateReturn> selectByProcessResult(String processResult);

    /**
     * 根据时间范围查询晚归记录
     * 
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 晚归记录列表
     */
    List<LateReturn> selectByTimeRange(@Param("startTime") Date startTime, @Param("endTime") Date endTime);

    /**
     * 插入晚归记录
     * 
     * @param lateReturn 晚归记录
     * @return 影响行数
     */
    int insert(LateReturn lateReturn);

    /**
     * 更新晚归记录
     * 
     * @param lateReturn 晚归记录
     * @return 影响行数
     */
    int update(LateReturn lateReturn);

    /**
     * 删除晚归记录
     * 
     * @param id 晚归记录ID
     * @return 影响行数
     */
    int deleteById(Long id);

    /**
     * 更新晚归记录处理状态
     * 
     * @param id            晚归记录ID
     * @param processStatus 处理状态
     * @param processResult 处理结果
     * @param processRemark 处理备注
     * @return 影响行数
     */
    int updateProcessStatus(@Param("id") Long id,
            @Param("processStatus") String processStatus,
            @Param("processResult") String processResult,
            @Param("processRemark") String processRemark);

    /**
     * 统计所有晚归记录的处理状态
     * 
     * @param lateReturnQuery     查询参数
     * @return                    处理状态
     */
    Map<String, Object> countAllProcessStatus(LateReturnQuery lateReturnQuery);

    /**
     * 周期性统计学生晚归次数
     * @param startDate     开始时间
     * @param endDate       结束时间
     * @param college       学院
     * @param className     班级
     * @return              统计结果
     */

    List<StudentLateResultDTO> selectByStuLateQueryInPeriod(@Param("startDate")Date startDate,
                                                            @Param("endDate") Date endDate,
                                                            @Param("college") String college,
                                                            @Param("className") String className);

    /**
     * 统计某个学生某个时间段内晚归的次数
     * @param studentNo         学号
     * @param startLateTime     起始时间
     * @param endLateTime       结束时间
     * @return                  晚归次数
     */
    Integer countStudentLateReturnsInPeriod(@Param("studentNo") String studentNo,
                                            @Param("startLateTime") Date startLateTime,
                                            @Param("endLateTime") Date endLateTime);

    /**
     * 计算特定时间范围内不合规的晚归次数（晚归记录数）
     * @param query   查询条件
     * @return        晚归次数
     */
    Integer countPeriodLateReturns (LateReturnQuery query);

    /**
     * 统计指定时间段内晚归的学生人数（去重）
     *
     * @param query 查询条件
     * @return 晚归学生人数
     */
    Integer countPeriodLateReturnStudents(LateReturnQuery query);

    /**
     *  统计指定时间段内的违规晚归记录
     * @param query 查询条件
     * @return      晚归记录集合
     */
    List<LateReturn> listPeriodLateReturns (LateReturnQuery query);
}