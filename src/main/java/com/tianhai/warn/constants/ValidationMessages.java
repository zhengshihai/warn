package com.tianhai.warn.constants;

/**
 * 学生信息校验错误消息常量
 */
public class ValidationMessages {
    // 必填校验
    public static final String STUDENT_NO_EMPTY = "学号不能为空";
    public static final String NAME_EMPTY = "姓名不能为空";
    public static final String EMAIL_EMPTY = "邮箱不能为空";
    public static final String CLASS_NAME_EMPTY = "班级不能为空";
    public static final String DORMITORY_EMPTY = "宿舍号不能为空";
    public static final String PASSWORD_EMPTY = "登录密码不能为空";
    public static final String MANAGER_ID_EMPTY = "宿管工号不能为空";
    public static final String PHONE_EMPTY = "手机号不能为空";
    public static final String BUILDING_EMPTY = "宿舍楼栋不能为空";
    public static final String SYS_USER_NO_EMPTY = "班级管理员工号不能为空";
    public static final String JOR_ROLE_EMPTY = "职位角色不能为空";

    // 长度校验
    public static final String STUDENT_NO_TOO_LONG = "学号长度不能超过20";
    public static final String NAME_TOO_LONG = "姓名长度不能超过50";
    public static final String EMAIL_TOO_LONG = "邮箱长度不能超过50";
    public static final String CLASS_NAME_TOO_LONG = "班级长度不能超过50";
    public static final String DORMITORY_TOO_LONG = "宿舍号长度不能超过50";
    public static final String PASSWORD_TOO_LONG = "密码长度不能超过100";
    public static final String MANAGER_ID_TOO_LONG = "宿管工号太长";
    public static final String BUILDING_TOO_LONG = "宿管负责的楼栋编号太长";
    public static final String PHONE_TOO_LONG = "手机号码过长";
    public static final String SYS_USER_NO_TOO_LONG = "班级管理员工号长度不能超过40";
    public static final String JOB_ROLE_TOO_LONG = "职位角色长度不能超过20";

    // 格式校验
    public static final String EMAIL_FORMAT_INVALID = "邮箱格式不正确";
    public static final String PHONE_FORMAT_INVALID = "联系电话格式不正确";

    // 唯一性校验
    public static final String STUDENT_NO_EXISTS = "学号已存在";
    public static final String STUDENT_NO_DUPLICATE = "导入数据中学号存在重复";
    public static final String EMAIL_EXISTS = "邮箱已存在";
    public static final String EMAIL_DUPLICATE = "导入数据中邮箱存在重复";
    public static final String MANAGER_ID_EXISTS = "宿管工号已存在";
    public static final String MANAGER_ID_DUPLICATE = "导入数据中宿管工号存在重复";
    public static final String SYS_USER_NO_EXISTS = "班级管理员工号已存在";
    public static final String SYS_USER_NO_DUPLICATE = "导入数据中班级管理员工号存在重复";

}