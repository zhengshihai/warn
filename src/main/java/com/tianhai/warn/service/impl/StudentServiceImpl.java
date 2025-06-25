package com.tianhai.warn.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.tianhai.warn.enums.ResultCode;
import com.tianhai.warn.exception.BusinessException;
import com.tianhai.warn.exception.SystemException;
import com.tianhai.warn.mapper.StudentMapper;
import com.tianhai.warn.model.Student;
import com.tianhai.warn.query.StudentQuery;
import com.tianhai.warn.service.StudentService;
import com.tianhai.warn.utils.EmailValidator;
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

import java.util.Date;
import java.util.List;
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
    @Transactional
    public int insert(Student student) {
        student.setCreateTime(new Date());
        student.setUpdateTime(new Date());
        return studentMapper.insert(student);
    }

    @Override
    @Transactional
    public int update(Student student) {
        student.setUpdateTime(new Date());
        return studentMapper.update(student);
    }

    @Override
    @Transactional
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
    public void updateLastLoginTime(Integer id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ResultCode.VALIDATE_FAILED);
        }

        studentMapper.updateLastLoginTime(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<?> updatePersonalInfo(Student student, String currentEmail) {
        // 检查数据是否被修改
        Student currentStudent = getStudentByEmail(currentEmail);
        if (currentStudent == null) {
//            return Result.error("用户信息不存在");
            throw new BusinessException(ResultCode.USER_NOT_EXISTS);
        }

        // 检查邮箱是否可用
        if (!emailValidator.isEmailAvailable(student.getEmail(), currentEmail)) {
//            return Result.error("该邮箱已被其他用户使用");
            throw new BusinessException(ResultCode.EMAIL_USED);
        }

        //本项目设定不同用户是使用唯一的邮箱，考虑到可能的并发修改问题，使用redisson加锁
        String lockKey = "lock:email:" + student.getEmail();
        RLock lock = null;
        boolean isLocked = false;
        int waitTime = 5;
        int lockTime = 3;
        int updateResult;

        try {
            //只有邮箱发生变化时，才需要加锁
            if (!currentEmail.equals(student.getEmail())) {
                lock = redissonClient.getLock(lockKey);
                isLocked = lock.tryLock(waitTime, lockTime, TimeUnit.SECONDS);
                if (!isLocked) {
                    throw new BusinessException(ResultCode.EMAIL_LOCKED_FAIL);
                }
            }

            // 如果密码不为空，则更新密码
            if (student.getPassword() != null && !student.getPassword().trim().isEmpty()) {
                // 对密码进行加密
                student.setPassword(DigestUtils.md5DigestAsHex(student.getPassword().getBytes()));
            } else {
                // 如果密码为空，保持原密码不变
                student.setPassword(currentStudent.getPassword());
            }

            // 设置更新时间
            student.setUpdateTime(new Date());

            // 更新学生信息
            updateResult = studentMapper.update(student);
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
            // 如果邮箱发生变化，需要更新其他相关表的数据
            if (!currentEmail.equals(student.getEmail())) {
                // TODO: 更新其他相关表的数据
            }
            return Result.success(ResultCode.SUCCESS);
        } else {
            throw new BusinessException(ResultCode.ERROR);
        }

    }

    @Override //todo
    public List<Student> searchByStudentQuery(StudentQuery query) {
        return studentMapper.searchByStudentQuery(query);
    }

    @Override
    public PageResult<Student> selectByPageQuery(StudentQuery query) {
        List<Student> studentList;
        PageResult<Student> result;
        try (Page<Student> page = PageHelper.startPage(query.getPageNum(), query.getPageSize())) {
            studentList = studentMapper.selectAll();
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
}