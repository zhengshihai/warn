package com.tianhai.warn.mapper;

import com.tianhai.warn.model.Application;
import com.tianhai.warn.query.ApplicationQuery;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.Date;
import java.util.List;

@Mapper
public interface ApplicationMapper {
    /**
     * 插入晚归申请
     */
    int insert(Application application);

    /**
     * 根据ID查询晚归申请
     */
    Application selectById(Long id);

    /**
     * 根据学号查询晚归申请
     */
    List<Application> selectByStudentNo(String studentNo);

    /**
     * 查询所有晚归申请
     */
    List<Application> selectAll();

    /**
     * 根据条件查询晚归申请
     */
    // List<Application> selectByCondition(Application application);
    List<Application> selectByCondition(ApplicationQuery query);

    /**
     * 更新晚归申请
     */
    int update(Application application);

    /**
     * 根据ID删除晚归申请
     */
    int deleteById(Long id);

    /**
     * 根据审核状态查询晚归申请
     */
    List<Application> selectByAuditStatus(Integer auditStatus);

    /**
     * 根据预期回校时间范围查询晚归申请
     */
    List<Application> selectByExpectedReturnTimeRange(@Param("startTime") Date startTime,
            @Param("endTime") Date endTime);

    /**
     * 根据applicationId查询晚归申请
     */
    Application selectByApplicationId(String applicationId);
}