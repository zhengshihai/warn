package com.tianhai.warn.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.tianhai.warn.constants.Constants;
import com.tianhai.warn.dto.AuditActionDTO;
import com.tianhai.warn.enums.CollegeEnum;
import com.tianhai.warn.enums.ResultCode;
import com.tianhai.warn.events.AuditEvent;
import com.tianhai.warn.exception.BusinessException;
import com.tianhai.warn.exception.SystemException;
import com.tianhai.warn.mapper.ExplanationMapper;
import com.tianhai.warn.model.Explanation;
import com.tianhai.warn.model.LateReturn;
import com.tianhai.warn.model.Student;
import com.tianhai.warn.query.ExplanationQuery;
import com.tianhai.warn.query.LateReturnQuery;
import com.tianhai.warn.service.ExplanationService;
import com.tianhai.warn.service.FileService;
import com.tianhai.warn.service.LateReturnService;
import com.tianhai.warn.service.StudentService;
import com.tianhai.warn.utils.ExplanationIdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Date;
import java.util.List;

@Service
public class ExplanationServiceImpl implements ExplanationService {

    private static final Logger logger = LoggerFactory.getLogger(ExplanationServiceImpl.class);

    @Autowired
    private ExplanationMapper explanationMapper;

    @Autowired
    private StudentService studentService;

    @Autowired
    private LateReturnService lateReturnService;

    @Autowired
    private FileService fileService;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    public ExplanationServiceImpl() {
        logger.info("ExplanationServiceImpl initialized");
    }

    @Override
    public Explanation selectById(Long id) {
        return explanationMapper.selectById(id);
    }

    @Override
    public List<Explanation> selectByStudentNo(String studentNo) {
        return explanationMapper.selectByStudentNo(studentNo);
    }

    @Override
    public List<Explanation> selectAll() {
        return explanationMapper.selectAll();
    }

    @Override
    public Explanation selectByExplanationId(String explanationId) {
        return explanationMapper.selectByExplanationId(explanationId);
    }

    @Override
    public List<Explanation> selectByCondition(ExplanationQuery explanationQuery) {
        return explanationMapper.selectByCondition(explanationQuery);
    }

    @Override
    @Transactional
    public Integer insert(Explanation explanation) {
        explanation.setSubmitTime(new Date());
        explanation.setAuditStatus(0); // 默认待审核
        return explanationMapper.insert(explanation);
    }

    @Override
    @Transactional
    public Integer update(ExplanationQuery explanationQuery) {
        return explanationMapper.update(explanationQuery);
    }

    @Override
    @Transactional
    public Integer deleteById(Long id) {
        return explanationMapper.deleteById(id);
    }

    @Override
    public List<Explanation> selectByAuditStatus(Integer auditStatus) {
        return explanationMapper.selectByAuditStatus(auditStatus);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer submitExplanation(LateReturnQuery lateReturnQuery,
            String studentNo,
            String reason,
            String description,
            MultipartFile file) {
        // 获取学生信息
        Student student = studentService.selectByStudentNo(studentNo);
        if (student == null) {
            logger.error("未找到学生信息");
            throw new BusinessException(ResultCode.USER_NOT_EXISTS);
        }

        // 获取晚归信息
        LateReturn lateReturn = lateReturnService.selectByCondition(lateReturnQuery).get(0);
        if (lateReturn == null || lateReturn.getProcessStatus().equals("FINISHED")) {
            logger.info("该晚归说明已处理完成");
            throw new BusinessException(ResultCode.EXPLANATION_REPEATED);
        }

        // 创建说明记录
        Explanation explanation = new Explanation();
        explanation.setStudentNo(studentNo);
        explanation.setLateReturnId(lateReturn.getLateReturnId());
        explanation.setDescription(description);
        explanation.setSubmitTime(new Date());
        explanation.setAuditStatus(0); // 待审核
        explanation.setExplanationId(ExplanationIdGenerator.generate());

        // 处理文件上传
        if (file != null && !file.isEmpty()) {
            try {
                // 获取学院代码
                String collegeCode = CollegeEnum.getCodeByName(student.getCollege());
                if (collegeCode == null) {
                    collegeCode = "OTHER"; // 如果找不到对应的学院代码，使用默认值
                }

                // 构建文件存储路径：explanations/学院代码/班级/
                String directory = String.format("explanations/%s/%s",
                        // student.getCollege(), student.getClassName());
                        collegeCode, student.getClassName());
                // 添加时间戳到文件名，避免并发冲突
                String timeStamp = String.valueOf(System.currentTimeMillis());
                String fileUrl = fileService.storeFile(
                        file, directory, studentNo, timeStamp, lateReturn.getLateTime());

                explanation.setAttachmentUrl(fileUrl);
            } catch (IOException e) {
                logger.error("晚归说明文件上传失败");
                throw new BusinessException(ResultCode.FILE_UPLOAD_FAILED);
            }
        }

        return explanationMapper.insert(explanation);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer auditExplanation(AuditActionDTO auditActionDTO) {
        logger.info("Starting auditExplanation with DTO: {}", auditActionDTO);
        // 同步处理数据库更新
        ExplanationQuery explanation = new ExplanationQuery();

        BeanUtil.copyProperties(auditActionDTO, explanation);
        explanation.setAuditTime(new Date());
        Integer explanationUpdate = explanationMapper.update(explanation);

        Integer lateReturnUpdate = lateReturnService.updateProcessStatus(
                auditActionDTO.getLateReturnId(),
                auditActionDTO.getProcessStatus(),
                auditActionDTO.getProcessResult(),
                auditActionDTO.getProcessRemark());

        if (explanationUpdate + lateReturnUpdate < 2) {
            throw new SystemException(ResultCode.ERROR);
        }

        // 异步处理
        // 发布事件： 如果当前审核人未能确定晚归说明，则触发事件：转发给下一审核人
        boolean needToForward = Constants.AUDIT_ACTION_FORWARD.equalsIgnoreCase(
                auditActionDTO.getProcessStatus());
        if (needToForward) {
            applicationEventPublisher.publishEvent(new AuditEvent(this, auditActionDTO));
        }

        return explanationUpdate + lateReturnUpdate;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateBatch(List<Explanation> explanationList) {
        return explanationMapper.updateBatch(explanationList);
    }
}