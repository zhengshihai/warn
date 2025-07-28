package com.tianhai.warn.component;

import com.tianhai.warn.constants.Constants;
import com.tianhai.warn.dto.RegisterDTO;
import com.tianhai.warn.enums.UserRole;
import com.tianhai.warn.model.DormitoryManager;
import com.tianhai.warn.model.Student;
import com.tianhai.warn.model.SuperAdmin;
import com.tianhai.warn.model.SysUser;
import org.springframework.stereotype.Component;

@Component
public class UserFactory {

    private static final String STUDENT = Constants.STUDENT;
    private static final String SYSTEM_USER = Constants.SYSTEM_USER;
    private static final String DORMITORY_MANAGER = Constants.DORMITORY_MANAGER;
    private static final String SUPER_ADMIN = Constants.SUPER_ADMIN;

    public Object createUser(RegisterDTO registerDTO) {
        switch (registerDTO.getRole().toLowerCase()) {
            case STUDENT:
                Student student = new Student();
                student.setName(registerDTO.getName());
                student.setEmail(registerDTO.getEmail());
                return student;

            case SYSTEM_USER:
                SysUser sysUser = new SysUser();
                sysUser.setName(registerDTO.getName());
                sysUser.setEmail(registerDTO.getEmail());
                return sysUser;

            case DORMITORY_MANAGER:
                DormitoryManager manager = new DormitoryManager();
                manager.setName(registerDTO.getName());
                manager.setEmail(registerDTO.getEmail());
                return manager;

            case SUPER_ADMIN:
                SuperAdmin superAdmin = new SuperAdmin();
                superAdmin.setName(registerDTO.getName());
                superAdmin.setEmail(registerDTO.getEmail());
                return superAdmin;

            default:
                throw new IllegalArgumentException("不支持的用户角色：" + registerDTO.getRole());
        }
    }

    public String getRoleCode(String role) {
        return switch (role.toLowerCase()) {
            case STUDENT -> UserRole.STUDENT.getCode();

            case SYSTEM_USER -> UserRole.SYSTEM_USER.getCode();

            case DORMITORY_MANAGER -> UserRole.DORMITORY_MANAGER.getCode();

            case SUPER_ADMIN -> UserRole.SUPER_ADMIN.getCode();

            default -> throw new IllegalArgumentException("暂不支持的用户角色：" + role);
        };
    }

    public String getPagePath(String role) {
        switch (role.toLowerCase()) {
            case STUDENT:
                return "student";
            case SYSTEM_USER:
                return "staff-dashboard";
            case DORMITORY_MANAGER:
                return "staff-dashboard";
            case SUPER_ADMIN:
                return "super-admin";
            default:
                throw new IllegalArgumentException("不支持的用户角色：" + role);
        }
    }
}