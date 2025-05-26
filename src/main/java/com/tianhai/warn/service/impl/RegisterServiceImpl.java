package com.tianhai.warn.service.impl;

import com.tianhai.warn.constants.Constants;
import com.tianhai.warn.dto.RegisterDTO;
import com.tianhai.warn.enums.BusinessType;
import com.tianhai.warn.enums.ResultCode;
import com.tianhai.warn.enums.UserRole;
import com.tianhai.warn.exception.BusinessException;
import com.tianhai.warn.exception.SystemException;
import com.tianhai.warn.model.DormitoryManager;
import com.tianhai.warn.model.Student;
import com.tianhai.warn.model.SuperAdmin;
import com.tianhai.warn.model.SysUser;
import com.tianhai.warn.query.SysUserQuery;
import com.tianhai.warn.service.*;
import com.tianhai.warn.utils.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RegisterServiceImpl implements RegisterService {
    private static final Logger logger = LoggerFactory.getLogger(RegisterServiceImpl.class);

    @Autowired
    private VerificationService verificationService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private StudentService studentService;

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private DormitoryManagerService dormitoryManagerService;

    @Autowired
    private SuperAdminService superAdminService;

    private static final String DEFAULT_PLACEHOLDER = "待完善";
    private static final String STUDENT = Constants.STUDENT;
    private static final String SYS_USER = Constants.SYSTEM_USER;
    private static final String DORMITORY_MANAGER = Constants.DORMITORY_MANAGER;
    private static final String SUPER_ADMIN = Constants.SUPER_ADMIN;

    @Override
    @Transactional
//    public Result<?> register(RegisterDTO registerDTO) {
    public Map<String, Object> register(RegisterDTO registerDTO) {
        Map<String, Object> infoMap = new HashMap<>();
        try {
            Date now = new Date();
            String role = registerDTO.getRole().toLowerCase();
            String password = DigestUtils.md5DigestAsHex(registerDTO.getPassword().getBytes());

            switch (role) {
                case STUDENT:
                    Student student = Student.builder()
                            .name(registerDTO.getName())
                            .email(registerDTO.getEmail())
                            .password(password)
                            .studentNo(registerDTO.getEmail())  // 默认用 email 作为学号
                            .college(DEFAULT_PLACEHOLDER)
                            .className(DEFAULT_PLACEHOLDER)
                            .dormitory(DEFAULT_PLACEHOLDER)
                            .phone(DEFAULT_PLACEHOLDER)
                            .fatherName(DEFAULT_PLACEHOLDER)
                            .fatherPhone(DEFAULT_PLACEHOLDER)
                            .motherName(DEFAULT_PLACEHOLDER)
                            .motherPhone(DEFAULT_PLACEHOLDER)
                            .createTime(now)
                            .updateTime(now)
                            .build();

                    infoMap.put("userRole", UserRole.STUDENT.getCode());

                    studentService.insert(student);

                    logger.info("学生账号注册成功：{}", student.getEmail());
                    break;

                case SYS_USER:
                    // todo 这里增加职位角色的设置 (扩展方式一：读取Excel表格  扩展方式二：读取配置文件）
                    String jobRole = Constants.JOB_ROLE_CLASS_TEACHER.toLowerCase();
                    if (registerDTO.getEmail().equals("zsh774538399@gmail.com")) {
                        jobRole = Constants.JOB_ROLE_DEAN;
                    }
                    SysUser sysUser = SysUser.builder()
                            .password(password)
                            .name(registerDTO.getName())
                            .sysUserNo(registerDTO.getEmail())  // 使用 email 作为用户编号
                            .email(registerDTO.getEmail())
                            .phone(DEFAULT_PLACEHOLDER)
                            .jobRole(jobRole)  // 默认角色为班主任
                            .status(Constants.ENABLE_STR)      // 默认状态为启用
                            .createTime(now)
                            .updateTime(now)
                            .build();

                    infoMap.put("userRole", UserRole.SYSTEM_USER.getCode());
                    infoMap.put("jobRole", jobRole);

                    sysUserService.insertSysUser(sysUser);

                    logger.info("系统用户账号注册成功：{}", sysUser.getEmail());
                    break;

                case DORMITORY_MANAGER:
                    DormitoryManager manager = DormitoryManager.builder()
                            .email(registerDTO.getEmail()) // 使用邮箱作为用户名
                            .name(registerDTO.getName())
                            .password(password)
                            .managerId(DEFAULT_PLACEHOLDER)
                            .building(DEFAULT_PLACEHOLDER)
                            .phone(DEFAULT_PLACEHOLDER)
                            .status(Constants.ON_DUTY) // 默认在职
                            .createTime(now)
                            .updateTime(now)
                            .build();

                    infoMap.put("userRole", UserRole.DORMITORY_MANAGER.getCode());

                    dormitoryManagerService.insert(manager);

                    logger.info("宿管账号注册成功：{}", manager.getEmail());
                    break;

                case SUPER_ADMIN:
                    SuperAdmin superAdmin = SuperAdmin.builder()
                            .email(registerDTO.getName())
                            .password(password)
                            .enabled(Constants.ENABLE_INT)
                            .createTime(now)
                            .updateTime(now)
                            .version(0)
                            .build();

                    superAdminService.insert(superAdmin);

                    logger.info("超级管理员账号注册成功：{}", superAdmin.getEmail());
                    break;

                default:
                    logger.error("暂不支持的用户角色：{}" ,role);
                    throw new BusinessException(ResultCode.USER_ROLE_DISABLE);
            }

            // "warn:email:token:" 作为前缀 token作为值

//            return Result.success("注册成功！" + "，请尽快登录并完善信息。");
            return infoMap;

        } catch (Exception e) {
            logger.error("注册失败", e);
            throw new SystemException(ResultCode.ERROR);
        }
    }

    @Override
    public Result<?> sendEmailCaptcha(String email) {
        try {
            // 1. 检查邮箱是否已被注册
            // 检查学生表
            Student student = new Student();
            student.setEmail(email);
            List<Student> students = studentService.selectByCondition(student);
            if (students != null && !students.isEmpty()) {
                return Result.error("该邮箱已被学生账号注册");
            }

            // 检查系统用户表
            SysUserQuery sysUserQuery = new SysUserQuery();
            sysUserQuery.setEmail(email);
            List<SysUser> sysUsers = sysUserService.selectByCondition(sysUserQuery);
            if (sysUsers != null && !sysUsers.isEmpty()) {
                return Result.error("该邮箱已被系统用户账号注册");
            }

            // 检查宿舍管理员表
            DormitoryManager dormitoryManager = new DormitoryManager();
            dormitoryManager.setEmail(email);
            List<DormitoryManager> managers = dormitoryManagerService.selectByCondition(dormitoryManager);
            if (managers != null && !managers.isEmpty()) {
                return Result.error("该邮箱已被宿舍管理员账号注册");
            }

            // 2. 生成邮箱验证码
            Result<String> emailResult = verificationService.generateEmailCaptcha(email, BusinessType.REGISTER);
            if (!emailResult.isSuccess()) {
                return emailResult;
            }

            // 3. 发送邮件
            emailService.sendCaptcha(email, emailResult.getData(), false);

            logger.info("邮箱验证码发送成功，邮箱：{}，验证码：{}", email, emailResult.getData());
            return Result.success();

        } catch (Exception e) {
            logger.error("发送邮箱验证码失败", e);
            throw new SystemException(ResultCode.EMAIL_SEND_FAIL);
        }
    }
}