package com.tianhai.warn.constants;

public class Constants {
    public static final String SESSION_ATTRIBUTE_USER = "user";
    public static final String SESSION_ATTRIBUTE_ROLE = "role"; // 用户角色 学生 班级管理员 宿管 超级管理员
    public static final String SESSION_ATTRIBUTE_JOB_ROLE = "job_role"; // 班级管理员的职位角色 辅导员 班主任 院系领导等
    public static final String SESSION_ATTRIBUTE_LATE_RETURN = "lateReturn";
    public static final String SESSION_ATTRIBUTE_AUDIT_PERSON = "auditPerson";

    public static final String STUDENT = "student";
    public static final String DORMITORY_MANAGER = "dormitorymanager";
    public static final String SYSTEM_USER = "systemuser";
    public static final String SUPER_ADMIN = "superadmin";

    public static final String JOB_ROLE_COUNSELOR = "COUNSELOR"; // 辅导员
    public static final String JOB_ROLE_CLASS_TEACHER = "CLASS_TEACHER"; // 班主任
    public static final String JOB_ROLE_DEAN = "DEAN"; // 院系领导
    public static final String JOB_ROLE_OTHER = "OTHER"; // 其他角色

    public static final String ENABLE_STR = "ENABLE";
    public static final Integer ENABLE_INT = 1;
    public static final String ON_DUTY = "ON_DUTY"; // 在职

    public static final String ALL = "all";

    public static final Integer DEFAULT_PAGE_SIZE = 10;
    public static final Integer DEFAULT_PAGE_NUM = 1;

    public static final String READ = "READ";
    public static final String UNREAD = "UNREAD";

    public static final String SYSTEM_NOTIFICATION = "系统通知"; // 系统通知
    public static final String LATE_RETURN_NOTIFICATION = "晚归通知"; // 晚归通知
    public static final String WARNING_NOTIFICATION = "预警通知"; // 预警通知

    public static final Integer NOTIFICATION_PERIOD_MONTH = 1; // 通知有效期（单位：月）

    public static final Integer LATE_RETURN_STATISTICS_PERIOD_MONTH = 2; // 晚归统计周期（单位：月）

    public static final String LATE_RETURN_PROCESS_STATUS_PENDING = "PENDING"; // 待处理
    public static final String LATE_RETURN_PROCESS_STATUS_PROCESSING = "PROCESSING"; // 处理中
    public static final String LATE_RETURN_PROCESS_STATUS_FINISHED = "FINISHED"; // 已完成处理

    public static final String AUDIT_ACTION_FORWARD = "PROCESSING"; // 转发
    public static final String AUDIT_ACTION_REJECT = "REJECTED"; // 驳回
    public static final String AUDIT_ACTION_APPROVED = "APPROVED"; // 通过

    public static final String NOTIFICATION_TYPE_AUDIT = "审核通知";
    public static final String NOTIFICATION_TYPE_LATE_RETURN = "晚归通知";
    public static final String NOTIFICATION_TYPE_WARNING = "预警通知";
    public static final String NOTIFICATION_TYPE_SYSTEM = "系统通知";

    public static final String LATE_RETURN_AUDIT_TITLE = "晚归审核通知"; // 晚归审核通知标题
    public static final Integer LATE_RETURN_AUDIT_DAYS = 4; // 晚归说明审核通知最长期限（天）

    public static final Integer EMAIL_MAX_RETRIES = 3; // 发送邮件失败的最大重试次数
    public static final String EMAIL_DEV_RECEIVER = "zhengzsh001@2925.com"; // 开发环境下接收者的邮箱
    public static final String EMAIL_DEV_SENDER = "774538399@qq.com"; // 开发环境下发送者的邮箱

    public static final String FIXED_WEEKLY = "FIXED_WEEKLY";
    public static final String FIXED_MONTHLY = "FIXED_MONTHLY";

    public static final String DYNAMIC_LAST_30_DAYS = "LAST_30_DAYS";
    public static final String DYNAMIC_LAST_7_DAYS = "LAST_7_DAYS";

    public static final Integer STATS_ACTIVE = 1; // 表示统计记录活跃
    public static final Integer STATS_INACTIVE = 0; // 表示统计记录不活跃

    public static final String SCHOOL_YEAR = "school_year"; // 学年
    public static final String SEMESTER = "semester"; // 学期
    public static final String SPRING = "SPRING"; // 上班学期
    public static final String FALL = "FALL"; // 下半学期

    public static final String EMAIL_REGEX = "^[a-zA-Z0-9_.+-]+@[a-zA-Z0-9-]+\\.[a-zA-Z0-9-.]+$";// 正确邮箱格式正则表达式

    public static final String MAX_LATEST_WEEK_LATE = "MAX_LATEST_WEEK_LATE"; // 最近7天晚归最大次数
    public static final String MAX_LATEST_MONTH_LATE = "MAX_LATEST_MONTH_LATE"; // 最近30天晚归最大次数

    public static final String SMS = "SMS";
    public static final String EMAIL = "EMAIL";
    public static final String PHONE_CALL = "PHONE_CALL";
    public static final String SYSTEM = "SYSTEM";

    public static final String START_DATE = "startDate"; // 起始日期 年月日
    public static final String END_DATE = "endDate"; // 截至日期 年月日
    public static final String START_TIME = "startTime"; // 起始时间 年月日时分秒
    public static final String END_TIME = "endTime"; // 结束时间 年月日时分秒

    public static final String LATE_RETURN_TIME_WEEKEND = "LATE_RETURN_TIME_WEEKENDS"; // 周末的晚归的晚归判定的起始时间 时分秒 例如
                                                                                       // "23:50:55"
    public static final String LATE_RETURN_TIME_WEEKDAYS = "LATE_RETURN_TIME_WEEKDAYS"; // 周一到周五的晚归判定的起始时间 时分秒
                                                                                        // 例如"23:30:00"
    public static final String LATE_RETURN_TIME_DECLINE = "LATE_RETURN_TIME_DECLINE"; // 晚归判定的截止时间

    // Redis缓存相关常量
    public static final String CACHE_REPORT_PREFIX = "report:cache:";
    public static final String CACHE_REPORT_CARD = CACHE_REPORT_PREFIX + "card:";
    public static final String CACHE_REPORT_WEEK = CACHE_REPORT_PREFIX + "week:";
    public static final String CACHE_REPORT_COLLEGE = CACHE_REPORT_PREFIX + "college:";
    public static final String CACHE_REPORT_DORMITORY = CACHE_REPORT_PREFIX + "dormitory:";
    public static final long CACHE_REPORT_EXPIRE_TIME = 7200; // 2小时过期

}
