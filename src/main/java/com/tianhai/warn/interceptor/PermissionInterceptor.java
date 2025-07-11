package com.tianhai.warn.interceptor;

import java.util.List;

import com.tianhai.warn.constants.Constants;
import com.tianhai.warn.utils.RoleObjectCaster;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import com.tianhai.warn.annotation.RequirePermission;
import com.tianhai.warn.enums.ResultCode;
import com.tianhai.warn.exception.BusinessException;
import com.tianhai.warn.model.DormitoryManager;
import com.tianhai.warn.model.Student;
import com.tianhai.warn.model.SysUser;
import com.tianhai.warn.service.DormitoryManagerService;
import com.tianhai.warn.service.StudentService;
import com.tianhai.warn.service.SysUserClassService;
import com.tianhai.warn.utils.DataScope;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Component
public class PermissionInterceptor implements HandlerInterceptor{

    private static final String STUDENT = "student";
    private static final String DORMITORY_MANAGER = "dormitorymanager";
    private static final String SYSTEM_USER = "systemuser";


    @Autowired
    private DormitoryManagerService dormitoryManagerService;

    @Autowired
    private SysUserClassService sysUserClassService;

    @Autowired 
    private StudentService studentService;


    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;

        RequirePermission requirePermission = handlerMethod.getMethodAnnotation(RequirePermission.class);
        if (requirePermission == null) {
            return true;
        } 

        // 登录校验
        HttpSession session = request.getSession();
        // 此处获得的用户角色名分别是四大类角色，Student, SystemUser, DormitoryManager, SuperAdmin
        //如果是SystemUser 则还有子角色，例如辅导员，班主任，院级领导等
        String userRoleName = (String) session.getAttribute(Constants.SESSION_ATTRIBUTE_ROLE);
        Object currentUserObject = session.getAttribute(Constants.SESSION_ATTRIBUTE_USER);

        if (currentUserObject == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        if (StringUtils.isBlank(userRoleName)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
       
        // 角色校验
        String[] allowedRoles = requirePermission.roles();
        if (allowedRoles.length > 0) {
            boolean hasRole = false;
            for (String role : allowedRoles) {
                if (role.equalsIgnoreCase(userRoleName)) {
                    hasRole = true;
                    break;
                }
            }            
            if (!hasRole) {
                throw new BusinessException(ResultCode.FORBIDDEN);
            }
        }

        // 数据范围校验
        if (requirePermission.checkDataScope()) {
            DataScope dataScope = null;

            switch (userRoleName.toLowerCase())  {
                case STUDENT:
                    Student validateStudent = (Student) currentUserObject;
//                    Student validateStudent = RoleObjectCaster.cast(userRoleName.toLowerCase(), currentUserObject);
                    Student student = studentService.selectByCondition(validateStudent).get(0);
                    if (student == null) {
                        throw new BusinessException(ResultCode.FORBIDDEN);
                    }
                    dataScope = DataScope.forStudent(student.getStudentNo());
                    break;
                
                case DORMITORY_MANAGER:
                    DormitoryManager validateManager = (DormitoryManager) currentUserObject;
//                    DormitoryManager validateManager = RoleObjectCaster.cast(userRoleName.toLowerCase(), currentUserObject );
                    DormitoryManager dormitoryManager = dormitoryManagerService.selectByCondition(validateManager).get(0);
                    if (dormitoryManager == null) {
                        throw new BusinessException(ResultCode.FORBIDDEN);
                    }

                    List<String> managedDormitories = dormitoryManagerService.getManagedDormitories(dormitoryManager.getManagerId());
                    dataScope = DataScope.forDormitoryManager(managedDormitories);
                    break;
                case SYSTEM_USER:
                    switch (((SysUser)currentUserObject).getJobRole().toUpperCase()) {
                        case "COUNSELOR":
                        case "TEARCHER":
                            //辅员/班主任  todo这里如果是院级领导 需要重新优化 todo 这些热点数据需要放在redis中
                            List<String> managedClasses = sysUserClassService.getUserClasses(
                                    ((SysUser)currentUserObject).getSysUserNo());
                            dataScope = DataScope.forOthers(managedClasses);
                    }

                default:
                    break;
            }

            if (dataScope != null) {
                request.setAttribute("dataScope", dataScope);
            }
        }

        return true;
    }
}
