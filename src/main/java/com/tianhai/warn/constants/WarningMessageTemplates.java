package com.tianhai.warn.constants;

/**
 * 晚归预警消息模板常量
 */
public final class WarningMessageTemplates {
    // 单个规则的消息模板
    public static final String STUDENT_SINGLE_TEMPLATE =
            "请注意，你在最近%d天内的不正当晚归次数超过规定次数，请遵守学校制度";
    public static final String COUNSELOR_SINGLE_TEMPLATE =
            "请注意，学号为【%s】的学生在最近%d天内不正当晚归次数超过规定次数，请及时关注和处理";
    public static final String PARENT_SINGLE_TEMPLATE =
            "尊敬的%s家长您好，您的孩子%s在最近%d天内不正当晚归次数超过规定次数，请及时关注！";
    public static final String CLASS_TEACHER_SINGLE_TEMPLATE =
            "请注意，学号为【%s】的学生在最近%d天内不正当晚归次数超过规定次数，请及时关注和处理";

    // 多个规则消息模板
    public static final String STUDENT_MULTIPLE_TEMPLATE =
            "请注意，你在最近%s天内的不正当晚归次数均超过规定次数，请严格遵守学校制度";
    public static final String COUNSELOR_MULTIPLE_TEMPLATE =
            "请注意，学号为【%s】的学生在最近%s天内的不正当晚归次数均超过规定次数，请立即处理";
    public static final String PARENT_MULTIPLE_TEMPLATE =
            "尊敬的%s家长您好，您的孩子%s在最近%s天内的不正当晚归次数均超过规定次数，情况严重，请立即关注！";
    public static final String CLASS_TEACHER_MULTIPLE_TEMPLATE =
            "请注意，学号为【%s】的学生在最近%s天内的不正当晚归次数均超过规定次数，请立即处理";

}