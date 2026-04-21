package com.tianhai.warn.service;

import com.tianhai.warn.dto.StudentLateQueryDTO;
import com.tianhai.warn.dto.StudentLateResultDTO;
import com.tianhai.warn.model.LateReturn;
import com.tianhai.warn.query.LateReturnQuery;
import com.tianhai.warn.utils.PageResult;
import reactor.util.annotation.Nullable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Date;
import java.util.Map;
import java.util.Set;

/**
 * 晚归记录服务接口
 */
public interface LateReturnService {

        /**
         * 根据ID查询晚归记录
         * 
         * @param id 晚归记录ID
         * @return 晚归记录
         */
        // LateReturn selectById(Long id);

        /**
         * 根据晚归记录ID查询晚归记录
         * 
         * @param lateReturnId 晚归记录ID
         * @return 晚归记录
         */
        LateReturn getByLateReturnId(String lateReturnId);

        /**
         * 查询所有晚归记录
         * 
         * @return 晚归记录列表
         */
        List<LateReturn> selectAll();

        /**
         * 根据条件查询晚归记录
         *
         * @param lateReturn 查询条件
         * @return 晚归记录列表
         */
        List<LateReturn> selectByCondition(LateReturnQuery lateReturn);

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
         * 根据时间范围和学生学号查询晚归记录
         * 
         * @param startTime 开始时间
         * @param endTime   结束时间
         * @param studentNo 学生学号
         * @return 晚归记录列表
         */
        List<LateReturn> selectByTimeRange(Date startTime, Date endTime, @Nullable String studentNo);

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
        // int deleteById(Long id);

        /**
         * 删除晚归记录
         * 
         * @param lateReturnId 晚归记录ID
         * @return 影响行数
         */
        int deleteByLateReturnId(String lateReturnId);

        /**
         * 更新晚归记录处理状态
         * 
         * @param id            晚归记录ID
         * @param processStatus 处理状态
         * @param processResult 处理结果
         * @param processRemark 处理备注
         * @return 影响行数
         */
        // int updateProcessStatus(Long id, String processStatus, String processResult,
        // String processRemark);

        /**
         * 更新晚归记录处理状态
         * 
         * @param lateReturnId  晚归记录ID
         * @param processStatus 处理状态
         * @param processResult 处理结果
         * @param processRemark 处理备注
         * @return 影响行数
         */
        int updateProcessStatus(String lateReturnId, String processStatus, String processResult, String processRemark);

        /**
         * 分页查询晚归记录
         * 
         * @param query
         * @return
         */
        PageResult<LateReturn> selectByPageQuery(LateReturnQuery query);

        /**
         * 统计指定时间范围内的（没正当理由）的晚归次数
         *
         * @param query 查询条件，包含时间范围
         * @return 晚归人数
         */
        Integer countPeriodLateReturns(LateReturnQuery query);

        /**
         * 统计指定时间段内晚归的学生人数（去重）
         *
         * @param query 查询条件
         * @return 晚归学生人数
         */
        Integer countPeriodLateReturnStudents(LateReturnQuery query);

        // /**
        // * 统计待处理晚归数量
        // */
        // Integer countPendingLateReturns(LateReturnQuery query);
        //
        // /**
        // * 统计待审核晚归数量 （待审核是指，宿管已经处理，但无法给出最终审核结果，转发给辅导员 班主任等人）
        // */
        // Integer countProcessingLateReturns(LateReturnQuery query);
        //
        // /**
        // * 统计已处理晚归数量
        // */
        // Integer countFinishedLateReturns(LateReturnQuery query);

        /**
         * 统计所有晚归记录的处理状态
         *
         * @param lateReturnQuery 查询条件
         * @return 晚归记录处理状态统计结果
         */
        Map<String, Object> countAllProcessStatus(LateReturnQuery lateReturnQuery);

        /**
         * 获取晚归统计数据
         * 
         * @param startDateStr   开始时间字符串
         * @param endTimeStr     结束时间字符串
         * @param userRoleStr    用户角色
         * @param currentUserObj 当前用户对象
         * @return 统计数据
         */
        Map<String, Object> getStatistics(String startDateStr, String endTimeStr,
                        String userRoleStr, Object currentUserObj);

        /**
         * 在对应周期内统计学生晚归情况
         *
         * @param studentLateQueryDTO 统计条件
         * @return 统计结果
         */
        List<StudentLateResultDTO> selectByStuLateQueryInPeriod(StudentLateQueryDTO studentLateQueryDTO);

        /**
         * 统计某个学生某段时间晚归的次数
         * 
         * @param studentNo 学号
         * @param startTime 起始时间
         * @param endTime   截止时间
         * @return 晚归次数
         */
        Integer countLateReturnsInPeriod(String studentNo,
                        Date startTime,
                        Date endTime);

        /**
         * 统计指定时间段内的违规晚归记录
         * 
         * @param query 查询条件
         * @return 晚归记录集合
         */
        List<LateReturn> listPeriodLateReturns(LateReturnQuery query);

        /**
         * 批量更新晚归记录
         * 
         * @param lateReturnList 晚归记录列表
         * @return 影响行数
         */
        int updateBatch(List<LateReturn> lateReturnList);

        /**
         * 批量插入晚归记录
         * 
         * @param lateReturnList 晚归记录
         * @return 插入行数
         */
        int insertBatch(List<LateReturn> lateReturnList);

        /**
         * 按月统计学生晚归次数（按学号分组）
         * 
         * @param startTime 开始时间
         * @param endTime   结束时间
         * @return Map，key为学号，value为晚归次数
         */
        Map<String, Integer> countLateReturnsByStudentNoInPeriod(Date startTime, Date endTime);
}