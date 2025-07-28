package com.tianhai.warn.utils;

import com.tianhai.warn.enums.JobRole;
import com.tianhai.warn.enums.ResultCode;
import com.tianhai.warn.enums.UserRole;
import com.tianhai.warn.exception.BusinessException;
import org.apache.commons.lang3.StringUtils;

import java.util.Set;

public class RoleUtils {
    /**
     * 校验用户具体角色是否合规
     * 具体到职位角色，这里不包括SystemUser这个角色，因为班级管理员角色已经被细分
     * @param role          用户角色
     * @return              true 合规，false 不合规
     */
    public static boolean validateConcreteRole(String role) {
        Set<String> allowedRoles = Set.of(
                UserRole.STUDENT.getCode().toLowerCase(),
                UserRole.DORMITORY_MANAGER.getCode().toLowerCase(),
                UserRole.SUPER_ADMIN.getCode().toLowerCase(),
                JobRole.DEAN.getCode().toLowerCase(),
                JobRole.CLASS_TEACHER.getCode().toLowerCase(),
                JobRole.COUNSELOR.getCode().toLowerCase()
        );

        String searcherRole = role.toLowerCase();

        return StringUtils.isNotBlank(searcherRole) && allowedRoles.contains(searcherRole);
    }

    /**
     * 校验用户基本角色是否合规
     * 此处字校验基本角色，包括学生，宿管，超级管理员，班级管理员
     * @param role      用户角色
     * @return          true 合规，false 不合规
     */
    public static boolean validateBasicRole(String role) {
        Set<String> allowedRoles = Set.of(
                UserRole.STUDENT.getCode().toLowerCase(),
                UserRole.DORMITORY_MANAGER.getCode().toLowerCase(),
                UserRole.SUPER_ADMIN.getCode().toLowerCase(),
                UserRole.SYSTEM_USER.getCode().toLowerCase() // 班级管理员角色
        );

        String searcherRole = role.toLowerCase();
        return StringUtils.isNotBlank(searcherRole) && allowedRoles.contains(searcherRole);
    }
}
