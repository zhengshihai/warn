package com.tianhai.warn.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.github.pagehelper.Constant;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.tianhai.warn.constants.Constants;
import com.tianhai.warn.enums.AlarmStatus;
import com.tianhai.warn.enums.ResultCode;
import com.tianhai.warn.enums.TargetScope;
import com.tianhai.warn.exception.BusinessException;
import com.tianhai.warn.exception.SystemException;
import com.tianhai.warn.mapper.StudentLateStatsMapper;
import com.tianhai.warn.mapper.StudentMapper;
import com.tianhai.warn.model.*;
import com.tianhai.warn.query.*;
import com.tianhai.warn.scheduler.UpdateScheduler;
import com.tianhai.warn.service.*;
import com.tianhai.warn.utils.EmailValidator;
import com.tianhai.warn.utils.NoticeIdGenerator;
import com.tianhai.warn.utils.PageResult;
import com.tianhai.warn.utils.Result;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 学生信息服务实现类
 */
@Service
public class StudentServiceImpl implements StudentService {

    private static final Logger logger = LoggerFactory.getLogger(StudentServiceImpl.class);

    @Autowired
    private StudentMapper studentMapper;

    @Autowired
    private EmailValidator emailValidator;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private AlarmRecordService alarmRecordService;

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private ExplanationService explanationService;

    @Autowired
    private LateReturnService lateReturnService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private StudentLateStatsService studentLateStatsService;

    @Autowired
    private UpdateScheduler updateScheduler;

    @Override
    public Student selectById(Integer id) {
        return studentMapper.selectById(id);
    }

    @Override
    public Student selectByStudentNo(String studentNo) {
        return studentMapper.selectByStudentNo(studentNo);
    }

    @Override
    public List<Student> selectAll() {
        return studentMapper.selectAll();
    }

    @Override
    public List<Student> selectByCondition(Student student) {
        return studentMapper.selectByCondition(student);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insert(Student student) {
        student.setCreateTime(new Date());
        student.setUpdateTime(new Date());
        return studentMapper.insert(student);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int update(Student student) {
        student.setUpdateTime(new Date());
        return studentMapper.update(student);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteById(Integer id) {
        return studentMapper.deleteById(id);
    }

    @Override
    public List<Student> selectByDormitory(String dormitory) {
        return studentMapper.selectByDormitory(dormitory);
    }

    @Override
    public List<Student> selectByClassName(String className) {
        return studentMapper.selectByClassName(className);
    }

    @Override
    public Student getStudentByEmail(String email) {
        return studentMapper.selectByEmail(email);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateLastLoginTime(Integer id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ResultCode.VALIDATE_FAILED);
        }

        studentMapper.updateLastLoginTime(id);
    }

    /**
     * 学生修改个人信息（包括邮箱、密码等），保证邮箱在全系统唯一，并发安全。
     * <p>
     * 1. 校验当前学生信息是否存在。
     * 2. 校验新邮箱在所有用户表（学生、班级管理员、宿管）中唯一。
     * 3. 若邮箱发生变化，对新邮箱加分布式锁，防止并发下邮箱冲突。
     * 4. 若有新密码则加密存储，否则保持原密码。
     * 5. 更新学生信息。
     * 6. 若邮箱变更，可扩展同步更新其他相关表。
     *
     * @param newStudentInfo 前端传入的学生对象（包含要修改的信息）
     * @param sessionEmail   当前登录学生的原邮箱
     * @return 更新结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> updatePersonalInfoByStudent(Student newStudentInfo, String sessionEmail) {
        // 检查数据是否被修改
        Student currentStudent = getStudentByEmail(sessionEmail);
        if (currentStudent == null) {
            logger.error("用户信息不存在");
            throw new BusinessException(ResultCode.USER_NOT_EXISTS);
        }

        // 检查新邮箱是否已被其他用户（学生、班级管理员、宿管）占用
        // 如果新邮箱和当前邮箱相同，直接通过
        if (!emailValidator.isEmailAvailable(newStudentInfo.getEmail(), sessionEmail)) {
            throw new BusinessException(ResultCode.EMAIL_USED);
        }

        // 本项目设定不同用户是使用唯一的邮箱，考虑到可能的并发修改问题，使用redisson加锁
        String lockKey = "lock:email:" + newStudentInfo.getEmail();
        RLock lock = null;
        boolean isLocked = false;
        int waitTime = 5;
        int lockTime = 3;
        int updateResult;

        try {
            // 只有当邮箱发生变化时，才对新邮箱加分布式锁，防止并发下多个用户同时把邮箱改成同一个
            if (!sessionEmail.equals(newStudentInfo.getEmail())) {
                lock = redissonClient.getLock(lockKey);
                isLocked = lock.tryLock(waitTime, lockTime, TimeUnit.SECONDS);
                if (!isLocked) {
                    throw new BusinessException(ResultCode.EMAIL_LOCKED_FAIL);
                }
            }

            // 如果前端传了新密码，则加密后存储，否则保持原密码不变
            if (newStudentInfo.getPassword() != null && !newStudentInfo.getPassword().trim().isEmpty()) {
                newStudentInfo.setPassword(DigestUtils.md5DigestAsHex(newStudentInfo.getPassword().getBytes()));
            } else {
                newStudentInfo.setPassword(currentStudent.getPassword());
            }

            // 设置更新时间
            newStudentInfo.setUpdateTime(new Date());

            // 更新学生信息
            updateResult = studentMapper.update(newStudentInfo);
        } catch (InterruptedException e) {
            logger.error("获取邮箱锁失败", e);
            throw new SystemException(ResultCode.EMAIL_LOCKED_FAIL);
        } finally {
            if (isLocked) {
                try {
                    lock.unlock();
                } catch (IllegalMonitorStateException e) {
                    logger.warn("Redisson锁释放失败", e);
                }
            }
        }

        if (updateResult > 0) {
            // 如果邮箱发生变化，需要更新其他相关表的数据（如有需要可在此扩展）
            if (!sessionEmail.equals(newStudentInfo.getEmail())) {
                // TODO: 更新其他相关表的数据
            }
            return Result.success(ResultCode.SUCCESS);
        } else {
            throw new BusinessException(ResultCode.ERROR);
        }

    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updatePersonalInfoBySuperAdmin(Student newStudentInfo) {
        Integer id = newStudentInfo.getId();
        Student studentExisting = studentMapper.selectById(id);
        if (studentExisting == null) {
            logger.info("找不到 id 为 {} 的学生", id);
            throw new BusinessException(ResultCode.USER_NOT_EXISTS);
        }

        // 如果该学生处于报警状态时，则不允许管理员更新学生信息
        checkStudentIfAlarming(studentExisting);

        // 检查新邮箱是否已被其他用户（学生、班级管理员、宿管）占用
        if (!emailValidator.isEmailAvailable(newStudentInfo.getEmail(), studentExisting.getEmail())) {
            throw new BusinessException(ResultCode.EMAIL_USED);
        }

        Student studentToBeInserted = new Student();
        BeanUtil.copyProperties(newStudentInfo, studentToBeInserted);
        logger.info("studentToBeInserted: {}", JSONUtil.toJsonStr(studentToBeInserted));

        // 本项目设定不同用户是使用唯一的邮箱，考虑到可能的并发修改问题，使用redisson加锁
        String lockKey = "lock:email:" + newStudentInfo.getEmail();
        RLock lock = null;
        boolean isLocked = false;
        int waitTime = 5;
        int lockTime = 3;
        int updateResult;

        try {
            // 只有当邮箱发生变化时，才对新邮箱加分布式锁，防止并发下多个用户同时把邮箱改成同一个
            if (!studentExisting.getEmail().equals(newStudentInfo.getEmail())) {
                lock = redissonClient.getLock(lockKey);
                isLocked = lock.tryLock(waitTime, lockTime, TimeUnit.SECONDS);
                if (!isLocked) {
                    logger.error("获取RLock锁失败，lockKey: {}", lockKey);
                    throw new BusinessException(ResultCode.EMAIL_LOCKED_FAIL);
                }
            }

            // 如果前端传了新密码，则加密后存储，否则保持原密码不变
            boolean passwordNotEmpty = studentToBeInserted.getPassword() != null
                    && !studentToBeInserted.getPassword().trim().isEmpty();
            if (passwordNotEmpty) {
                studentToBeInserted.setPassword(DigestUtils.md5DigestAsHex(newStudentInfo.getPassword().getBytes()));
            } else {
                studentToBeInserted.setPassword(studentExisting.getPassword());
            }

              // 方案一
//            // 如果学号发生改变，则需要更新其他表中的信息
//            if (!studentExisting.getStudentNo().equals(newStudentInfo.getStudentNo())) {
//                updateStudentInfoInOtherTables(newStudentInfo, studentExisting);
//            }
//
//            // 设置更新时间
//            studentToBeInserted.setUpdateTime(new Date());
//
//            // 更新student表信息
//            updateResult = studentMapper.update(studentToBeInserted);

            // 方案二
            // 立即更新除学号外的其他信息
            Student studentToUpdate = new Student();
            BeanUtil.copyProperties(newStudentInfo, studentToUpdate);
            studentToUpdate.setStudentNo(studentExisting.getStudentNo()); // 保持原学号
            studentToUpdate.setUpdateTime(new Date());

            updateResult = studentMapper.update(studentToUpdate);

            // 如果学号发生变化，给该学生发送通知，并加入异步队列
            if (!studentExisting.getStudentNo().equals(newStudentInfo.getStudentNo())) {
                int notificationRow = generateAndInsertNotification(studentExisting.getStudentNo());
                if (notificationRow <= 0) {
                    logger.error("生成站内通知出现异常，studentExisting:{}", studentExisting);
                }

                logger.info("学号变更将在凌晨3点执行");
                updateScheduler.scheduleStudentNoUpdate(studentExisting.getStudentNo(),
                        newStudentInfo.getStudentNo());
            }

        } catch (InterruptedException e) {
            logger.error("获取邮箱锁失败", e);
            throw new SystemException(ResultCode.EMAIL_LOCKED_FAIL);
        } catch (Exception e) {
            logger.error("更新学生信息失败，可能是部分信息不符合要求， newStudentInfo:{}", newStudentInfo);
            throw new BusinessException(ResultCode.PARAMETER_ERROR);
        } finally {
            if (isLocked) {
                try {
                    lock.unlock();
                } catch (IllegalMonitorStateException e) {
                    logger.warn("Redisson锁释放失败", e);
                }
            }
        }

        return updateResult;

    }

    // 检查该学生是否处于报警状态中
    // 如果处于报警状态中，则不允许超级管理员更新学生信息
    private void checkStudentIfAlarming(Student student) {
        String studentNo = student.getStudentNo();
        AlarmRecordQuery alarmRecordQuery = new AlarmRecordQuery();
        alarmRecordQuery.setStudentNo(studentNo);
        List<AlarmRecord> alarmRecordList = alarmRecordService.selectList(alarmRecordQuery);

        boolean isAlarming = alarmRecordList.stream()
                .anyMatch(alarmRecord -> alarmRecord.getAlarmStatus() != null
                        && (alarmRecord.getAlarmStatus() == AlarmStatus.PENDING.getCode()
                                || alarmRecord.getAlarmStatus() == AlarmStatus.PROCESSING.getCode()));

        if (isAlarming) {
            logger.error("该学生正处于报警状态中，不允许修改学生资料");
            throw new SystemException(ResultCode.ERROR);
        }
    }

    // 插入一条该学生的站内通知
    private int generateAndInsertNotification(String oldStudentNo) {
        Notification notification = new Notification();

        LocalDateTime now = LocalDateTime.now();
        // 取今天凌晨3点
        LocalDateTime next3AM = now.withHour(3).withMinute(0).withNano(0);
        // 如果当前时间在今天凌晨3点之后，就取明天的凌晨3点日期
        if (!now.isBefore(next3AM)) {
            next3AM = next3AM.plusDays(1);
        }
        String dateStr = next3AM.toLocalDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        String content = String.format(
                "你好，由于超级管理员为你更新了学号，所以在%s凌晨 03:00 - 03：10 这个时间段内请不要使用本系统", dateStr);

        notification.setNoticeId(NoticeIdGenerator.generate());
        notification.setTitle("学号更新通知");
        notification.setNoticeType("系统通知");
        notification.setTargetType(Constants.STUDENT);
        notification.setTargetScope(TargetScope.SPECIAL_USER.getCode());
        notification.setTargetId(oldStudentNo);
        notification.setCreateTime(new Date());
        notification.setUpdateTime(new Date());
        notification.setContent(content);

        return notificationService.insert(notification);
    }


    @Override // todo
    public List<Student> searchByStudentQuery(StudentQuery query) {
        return studentMapper.searchByStudentQuery(query);
    }

    @Override
    public PageResult<Student> selectByPageQuery(StudentQuery query) {
        List<Student> studentList;
        PageResult<Student> result;
        try (Page<Student> page = PageHelper.startPage(query.getPageNum(), query.getPageSize())) {
            studentList = studentMapper.searchByStudentQuery(query);
            result = buildPageResult(studentList);
        }

        return result;
    }

    // 构建分页结果
    private PageResult<Student> buildPageResult(List<Student> studentList) {
        PageInfo<Student> pageInfo = new PageInfo<>(studentList);

        PageResult<Student> result = new PageResult<>();
        result.setData(pageInfo.getList());
        result.setTotal((int) pageInfo.getTotal());
        result.setPageNum(pageInfo.getPageNum());
        result.setPageSize(pageInfo.getPageSize());

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteByIds(List<Integer> distinctIds) {

        return studentMapper.deleteByIds(distinctIds);
    }

    @Override
    public Set<String> selectAllEmail() {
        Set<String> emailSet = studentMapper.selectAllEmail();
        if (emailSet.isEmpty()) {
            logger.warn("学生表中找不到邮箱信息");
        }

        return emailSet;
    }

    @Override
    public Set<String> selectAllStudentNo() {
        Set<String> studentNoSet = studentMapper.selectAllStudentNo();
        if (studentNoSet.isEmpty()) {
            logger.warn("学生表中找不到学号信息");
        }

        return studentNoSet;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertBatch(List<Student> studentList) {
        if (studentList == null || studentList.isEmpty()) {
            logger.error("批量插入的学生信息为空");
            return 0;
        }

        final int batchSize = 1000;
        int totalInsertCount = 0;

        try {
            for (int i = 0; i < studentList.size(); i += batchSize) {
                int end = Math.min(i + batchSize, studentList.size());
                List<Student> batch = studentList.subList(i, end);
                int insertedRows = studentMapper.insertBatch(batch);

                if (insertedRows != batch.size()) {
                    logger.error("批次插入未完成，应插入 {} 条，实际插入 {} 条", batch.size(), insertedRows);
                    throw new SystemException(ResultCode.ERROR);
                }

                totalInsertCount += insertedRows;
                logger.info("已成功插入第 {} 批， 插入 {} 条", (i / batchSize) + 1, insertedRows);
            }

            return totalInsertCount;
        } catch (Exception e) {
            logger.error("批量插入学生信息异常", e);
            throw new SystemException(ResultCode.ERROR);
        }
    }
}