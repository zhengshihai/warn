package com.tianhai.warn.service;

import com.tianhai.warn.model.Application;
import java.util.Date;
import java.util.List;

import com.tianhai.warn.query.ApplicationQuery;
import com.tianhai.warn.utils.PageResult;
import org.apache.ibatis.annotations.Param;
import org.springframework.web.multipart.MultipartFile;

public interface ApplicationService {
    /**
     * 添加晚归申请
     */
    void insert(Application application);

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
    List<Application> selectByCondition(ApplicationQuery query);

    /**
     * 更新晚归申请
     */
    void update(Application application);

    /**
     * 根据ID删除晚归申请
     */
    void deleteById(Long id);

    /**
     * 根据审核状态查询晚归申请
     */
    List<Application> selectByAuditStatus(Integer auditStatus);

    /**
     * 根据预期回校时间范围查询晚归申请
     */
    List<Application> selectByExpectedReturnTimeRange(Date startTime, Date endTime);

    /**
     * 审核晚归申请
     */
    void auditApplication(Long id, Integer auditStatus, String auditPerson, String auditRemark);

    /**
     * 提交晚归申请
     * 
     * @param studentNo          学号
     * @param expectedReturnTime 预计返回时间
     * @param reason             晚归原因
     * @param destination        目的地
     * @param file               附件文件
     * @return 申请结果 0：失败 1：成功
     */
    Integer submitApplication(String studentNo, Date expectedReturnTime, String reason,
            String destination, MultipartFile file);

    /**
     * 分页查询晚归记录
     * 
     * @param query
     * @return
     */
    PageResult<Application> selectByPageQuery(ApplicationQuery query);

    Application selectByApplicationId(String applicationId);

    /**
     * 根据ID获取申请记录
     * 
     * @param id 申请记录ID
     * @return 申请记录，如果不存在返回null
     */
    Application getById(Long id);

    /**
     * 根据申请ID获取申请记录
     * 
     * @param applicationId 申请ID
     * @return 申请记录，如果不存在返回null
     */
    Application getByApplicationId(String applicationId);

    /**
     * 根据学号获取申请记录列表
     * 
     * @param studentNo 学号
     * @return 申请记录列表
     */
    List<Application> getByStudentNo(String studentNo);

    /**
     * 根据审核状态获取申请记录列表
     * 
     * @param auditStatus 审核状态
     * @return 申请记录列表
     */
    List<Application> getByAuditStatus(Integer auditStatus);

    /**
     * 批量更新晚归申请
     * @param applicationList   新的晚归申请
     * @return                  更新行数
     */
    int updateBatch(@Param("applicationList") List<Application> applicationList);
}