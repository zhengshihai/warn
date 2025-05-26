package com.tianhai.warn.service.impl;

import com.sun.net.httpserver.HttpsConfigurator;
import com.tianhai.warn.constants.Constants;
import com.tianhai.warn.enums.ResultCode;
import com.tianhai.warn.enums.UserRole;
import com.tianhai.warn.exception.BusinessException;
import com.tianhai.warn.exception.SystemException;
import com.tianhai.warn.model.DormitoryManager;
import com.tianhai.warn.model.Student;
import com.tianhai.warn.model.SuperAdmin;
import com.tianhai.warn.model.SysUser;
import com.tianhai.warn.service.*;
import com.tianhai.warn.utils.Result;
import com.tianhai.warn.utils.SessionUtils;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 认证服务实现类
 */
@Service
public class AuthServiceImpl implements AuthService {
    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);

    @Autowired
    private StudentService studentService;

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private DormitoryManagerService dormitoryManagerService;

    @Autowired
    private SuperAdminService superAdminService;

    /**
     * 项目设定在学生 班级管理员 宿管三个角色将 每个用户的邮箱具有唯一性
       但超级管理员的邮箱可出现在特定的班级管理员中
     * @param name     姓名
     * @param email    邮箱
     * @param password 密码
     * @param role     角色
     * @return         用户信息
     */
    @Override
    public Map<String, Object> login(String name, String email, String password, String role) {
        // 1. 验证角色是否有效
        if (!UserRole.isValidRole(role)) {
            throw new BusinessException(ResultCode.USER_ROLE_DISABLE);
        }

        // 2. 根据角色验证用户
        String encryptedPassword = DigestUtils.md5DigestAsHex(password.getBytes());
        Map<String, Object> infoMap = new HashMap<>();

            switch (Objects.requireNonNull(UserRole.getByCode(role))) {
                // 学生
                case STUDENT:
                    Student student = studentService.getStudentByEmail(email);
                    if (student == null || !student.getPassword().equals(encryptedPassword)) {
                        throw new BusinessException(ResultCode.USER_NAME_PWD_FALSE);
                    }
                    student.setPassword(null);
                    infoMap.put(Constants.SESSION_ATTRIBUTE_USER, student);
                    infoMap.put(Constants.SESSION_ATTRIBUTE_ROLE, UserRole.STUDENT.getCode());

                    studentService.updateLastLoginTime(student.getId());
                    break;

                // 班级管理员 具体的职位角色，在注册时已经完成设置
                case SYSTEM_USER:
                    SysUser sysUser = sysUserService.getSysUserByEmail(email);
                    if (sysUser == null || !sysUser.getPassword().equals(encryptedPassword)) {
                        throw new BusinessException(ResultCode.USER_NAME_PWD_FALSE);
                    }
                    sysUser.setPassword(null);
                    infoMap.put(Constants.SESSION_ATTRIBUTE_USER, sysUser);
                    infoMap.put(Constants.SESSION_ATTRIBUTE_ROLE, UserRole.SYSTEM_USER.getCode());
                    infoMap.put(Constants.SESSION_ATTRIBUTE_JOB_ROLE, sysUser.getJobRole().toLowerCase());

                    sysUserService.updateLastLoginTime(sysUser.getId());
                    break;

                // 宿管
                case DORMITORY_MANAGER:
                    DormitoryManager manager = dormitoryManagerService.getByEmail(email);
                    if (manager == null || !manager.getPassword().equals(encryptedPassword)) {
                        throw new BusinessException(ResultCode.USER_NAME_PWD_FALSE);
                    }
                    manager.setPassword(null);
                    infoMap.put(Constants.SESSION_ATTRIBUTE_USER, manager);
                    infoMap.put(Constants.SESSION_ATTRIBUTE_ROLE, UserRole.DORMITORY_MANAGER.getCode());

                    dormitoryManagerService.updateLastLoginTime(manager.getId());
                    break;

                // 超级管理员
                case SUPER_ADMIN:
                    SuperAdmin superAdmin = superAdminService.getByEmail(email);
                    if (superAdmin == null || !superAdmin.getPassword().equals(encryptedPassword)) {
                        throw new BusinessException(ResultCode.USER_NAME_PWD_FALSE);
                    }
                    superAdmin.setPassword(null);
                    infoMap.put(Constants.SESSION_ATTRIBUTE_USER, superAdmin);
                    infoMap.put(Constants.SESSION_ATTRIBUTE_ROLE, UserRole.SUPER_ADMIN);

                    superAdminService.updateLastLoginTime(superAdmin.getId());
                    break;

                default:
                    throw new BusinessException(ResultCode.USER_ROLE_DISABLE);

            }

            return infoMap;

    }


}
