package com.tianhai.warn.mapper;

import com.tianhai.warn.model.Explanation;
import com.tianhai.warn.query.ExplanationQuery;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ExplanationMapper {
    // 根据ID查询
    Explanation selectById(Long id);

    /**
     * 根据晚归记录ID查询晚归说明
     * 
     * @param lateReturnId 晚归记录ID
     * @return 晚归说明列表
     */
    List<Explanation> selectByLateReturnId(String lateReturnId);

    // 根据学号查询
    List<Explanation> selectByStudentNo(String studentNo);

    // 查询所有
    List<Explanation> selectAll();

    // 根据条件查询
    List<Explanation> selectByCondition(ExplanationQuery explanationQuery);

    // 插入
    int insert(Explanation explanation);

    // 更新
    int update(ExplanationQuery explanationQuery);

    // 删除
    int deleteById(Long id);

    // 根据审核状态查询
    List<Explanation> selectByAuditStatus(Integer auditStatus);

    int updateAuditStatus(@Param("id") Long id,
            @Param("auditStatus") Integer auditStatus,
            @Param("auditPerson") String auditPerson,
            @Param("auditRemark") String auditRemark);

    /**
     * 根据explanationId查询晚归说明
     * 
     * @param explanationId 晚归情况说明唯一ID
     * @return 晚归说明
     */
    Explanation selectByExplanationId(String explanationId);
}