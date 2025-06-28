package com.tianhai.warn.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.tianhai.warn.enums.CollegeEnum;
import com.tianhai.warn.enums.ResultCode;
import com.tianhai.warn.exception.BusinessException;
import com.tianhai.warn.mapper.ApplicationMapper;
import com.tianhai.warn.model.Application;
import com.tianhai.warn.model.Student;
import com.tianhai.warn.query.ApplicationQuery;
import com.tianhai.warn.service.ApplicationService;
import com.tianhai.warn.service.FileStorageService;
import com.tianhai.warn.service.StudentService;
import com.tianhai.warn.utils.ApplicationIdGenerator;
import com.tianhai.warn.utils.PageResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Service
public class ApplicationServiceImpl implements ApplicationService {
    private static final Logger logger = LoggerFactory.getLogger(ApplicationServiceImpl.class);

    @Autowired
    private ApplicationMapper applicationMapper;

    @Autowired
    private StudentService studentService;

    @Autowired
    private FileStorageService fileStorageService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer submitApplication(String studentNo,
            Date expectedReturnTime,
            String reason,
            String destination,
            MultipartFile file) {
        // 获取学生信息
        Student student = studentService.selectByStudentNo(studentNo);
        if (student == null) {
            logger.error("未找到学生信息");
            throw new BusinessException(ResultCode.USER_NOT_EXISTS);
        }

        // 创建申请记录
        Application application = new Application();
        application.setStudentNo(studentNo);
        application.setExpectedReturnTime(expectedReturnTime);
        logger.info("保存申请时间: {}", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(expectedReturnTime));
        application.setReason(reason);
        application.setDestination(destination);
        application.setApplicationId(ApplicationIdGenerator.generate());

        // 处理文件上传
        if (file != null && !file.isEmpty()) {
            try {
                // 获取学院代码
                String collegeCode = CollegeEnum.getCodeByName(student.getCollege());
                if (collegeCode == null) {
                    collegeCode = "OTHER"; // 如果找不到对应的学院代码，使用默认值
                }

                // 构建文件存储路径：applications/学院代码/班级/
                String directory = String.format("applications/%s/%s",
                        // student.getCollege(), student.getClassName());
                        collegeCode, student.getClassName());

                // 添加时间戳到文件名，避免并发冲突
                String timestamp = String.valueOf(System.currentTimeMillis());
                String fileUrl = fileStorageService.storeFile(
                        file, directory, studentNo, timestamp, expectedReturnTime);
                application.setAttachmentUrl(fileUrl);
            } catch (IOException e) {
                logger.error("晚归申请文件上传失败", e);
                throw new BusinessException(ResultCode.FILE_UPLOAD_FAILED);
            }
        }

        // 保存申请记录
        return applicationMapper.insert(application);
    }

    @Override
    public Application selectByApplicationId(String applicationId) {
        return applicationMapper.selectByApplicationId(applicationId);
    }

    @Override
    @Transactional
    public void insert(Application application) {
        applicationMapper.insert(application);
    }

    @Override
    public Application selectById(Long id) {
        return applicationMapper.selectById(id);
    }

    @Override
    public List<Application> selectByStudentNo(String studentNo) {
        return applicationMapper.selectByStudentNo(studentNo);
    }

    @Override
    public List<Application> selectAll() {
        return applicationMapper.selectAll();
    }

    @Override
    public List<Application> selectByCondition(ApplicationQuery query) {
        return applicationMapper.selectByCondition(query);
    }

    @Override
    @Transactional
    public void update(Application application) {
        applicationMapper.update(application);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        applicationMapper.deleteById(id);
    }

    @Override
    public List<Application> selectByAuditStatus(Integer auditStatus) {
        return applicationMapper.selectByAuditStatus(auditStatus);
    }

    @Override
    public List<Application> selectByExpectedReturnTimeRange(Date startTime, Date endTime) {
        return applicationMapper.selectByExpectedReturnTimeRange(startTime, endTime);
    }

    @Override
    @Transactional
    public void auditApplication(Long id, Integer auditStatus, String auditPerson, String auditRemark) {
        Application application = new Application();
        application.setId(id);
        application.setAuditStatus(auditStatus);
        application.setAuditPerson(auditPerson);
        application.setAuditRemark(auditRemark);
        applicationMapper.update(application);
    }

    @Override
    public PageResult<Application> selectByPageQuery(ApplicationQuery query) {
        PageHelper.startPage(query.getPageNum(), query.getPageSize());

        List<Application> list = applicationMapper.selectByCondition(query);

        PageInfo<Application> pageInfo = new PageInfo<>(list);

        PageResult<Application> result = new PageResult<>();
        result.setData(pageInfo.getList());
        result.setTotal((int) pageInfo.getTotal());
        result.setPageSize(pageInfo.getPageSize());

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateBatch(List<Application> applicationList) {
        return applicationMapper.updateBatch(applicationList);
    }

    /**
     * 安全地获取单条记录
     * 
     * @param list 查询结果列表
     * @return 如果列表不为空返回第一条记录，否则返回null
     */
    private Application getFirstSafely(List<Application> list) {
        return list != null && !list.isEmpty() ? list.get(0) : null;
    }

    @Override
    public Application getById(Long id) {
        ApplicationQuery query = new ApplicationQuery();
        query.setId(id);
        return getFirstSafely(applicationMapper.selectByCondition(query));
    }

    @Override
    public Application getByApplicationId(String applicationId) {
        ApplicationQuery query = new ApplicationQuery();
        query.setApplicationId(applicationId);
        return getFirstSafely(applicationMapper.selectByCondition(query));
    }

    @Override
    public List<Application> getByStudentNo(String studentNo) {
        ApplicationQuery query = new ApplicationQuery();
        query.setStudentNo(studentNo);
        return applicationMapper.selectByCondition(query);
    }

    @Override
    public List<Application> getByAuditStatus(Integer auditStatus) {
        ApplicationQuery query = new ApplicationQuery();
        query.setAuditStatus(auditStatus);
        return applicationMapper.selectByCondition(query);
    }

}