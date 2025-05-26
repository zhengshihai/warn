package com.tianhai.warn.service;

import com.tianhai.warn.dto.AuditActionDTO;
import com.tianhai.warn.model.Explanation;
import com.tianhai.warn.query.ExplanationQuery;
import com.tianhai.warn.query.LateReturnQuery;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 晚归情况说明服务接口
 */
public interface ExplanationService {

    /**
     * 根据ID查询晚归说明
     */
    Explanation selectById(Long id);


    /**
     * 根据学号查询晚归说明
     */
    List<Explanation> selectByStudentNo(String studentNo);

    /**
     * 查询所有晚归说明
     */
    List<Explanation> selectAll();

    Explanation selectByExplanationId(String explanationId);

    /**
     * 根据条件查询晚归说明
     */
    List<Explanation> selectByCondition(ExplanationQuery explanationQuery);

    /**
     * 更新晚归说明
     */
    Integer update(ExplanationQuery explanationQuery);

    /**
     * 插入晚归说明
     */
    Integer insert(Explanation explanation);

    /**
     * 根据ID删除晚归说明
     */
    Integer deleteById(Long id);

    /**
     * 根据审核状态查询晚归说明
     */
    List<Explanation> selectByAuditStatus(Integer auditStatus);



    // 提交晚归记录说明
    Integer submitExplanation(LateReturnQuery lateReturnQuery, String studentNo, String reason, String description,
            MultipartFile file);

    Integer auditExplanation(AuditActionDTO auditActionDTO);
}