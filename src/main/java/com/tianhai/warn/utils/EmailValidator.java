package com.tianhai.warn.utils;

import com.tianhai.warn.model.Student;
import com.tianhai.warn.model.SysUser;
import com.tianhai.warn.model.DormitoryManager;
import com.tianhai.warn.query.SysUserQuery;
import com.tianhai.warn.service.StudentService;
import com.tianhai.warn.service.SysUserService;
import com.tianhai.warn.service.DormitoryManagerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class EmailValidator {

    @Autowired
    private StudentService studentService;

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private DormitoryManagerService dormitoryManagerService;

    /**
     * 检查邮箱是否已被使用
     * 
     * @param email        要检查的邮箱
     * @param currentEmail 当前用户的邮箱（如果是修改邮箱，需要排除当前用户的邮箱）
     * @return 如果邮箱可用返回true，否则返回false
     */
    public boolean isEmailAvailable(String email, String currentEmail) {
        // 如果新邮箱和当前邮箱相同，直接返回true
        if (email.equals(currentEmail)) {
            return true;
        }

        // 检查学生表
        Student student = new Student();
        student.setEmail(email);
        if (!studentService.selectByCondition(student).isEmpty()) {
            return false;
        }

        // 检查系统用户表
        SysUserQuery sysUserQuery = new SysUserQuery();
        sysUserQuery.setEmail(email);
        if (!sysUserService.selectByCondition(sysUserQuery).isEmpty()) {
            return false;
        }

        // 检查宿管表
        DormitoryManager manager = new DormitoryManager();
        manager.setEmail(email);
        if (!dormitoryManagerService.selectByCondition(manager).isEmpty()) {
            return false;
        }

        return true;
    }
}