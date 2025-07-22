package com.tianhai.warn.enums;

public enum ResultCode implements IResultCode {
    SUCCESS(200, "操作成功"),
    ERROR(500, "操作失败，请稍后再试"),
    VALIDATE_FAILED(404, "参数校验失败"),
    UNAUTHORIZED(401, "暂未登录或session已过期"),
    FORBIDDEN(403, "没有相关权限"),
    PARAMETER_ERROR(999, "参数错误或者重复或者不完整"),

    LOGIN_FAILED(100, "登录失败，请检查邮箱或密码是否正确"),

    // 业务异常码
    USER_UPDATE_FAILED(1000, "更新用户信息失败"),
    USER_NOT_EXISTS(1001, "用户不存在"),
    USER_EXISTS(1002, "用户已存在"),
    USER_ROLE_DISABLE(1003, "无效的用户角色"),
    USER_NAME_PWD_FALSE(1004, "邮箱或密码错误"),
    USER_LOCKED(1005, "用户状态暂不可用，请联系管理员"),

    CAPTCHA_VALIDATE_ERROR(2000, "验证码输入错误或已过期"),
    CAPTCHA_GENERATE_ERROR(2001, "生成验证码失败"),
    CAPTCHA_EMAIL_ERROR(2003, "邮箱验证码错误"),

    EMAIL_USED(2004, "该邮箱已被使用"),
    EMAIL_UPDATING(2005, "邮箱正在修改中，请稍后再试"),
    EMAIL_LOCKED_FAIL(2006, "邮箱锁定失败"), // 获取邮箱锁失败
    EMAIL_SEND_FAIL(2007, "邮件发送失败"), // 邮件发送失败

    FILE_SIZE_ERROR_5MB(3000, "文件大小不能超过5MB"),
    FILE_SIZE_ERROR_10MB(3001, "文件大小不能超过10MB"),
    FILE_TYPE_ERROR(3002, "不支持该格式的文件"),
    FILE_UPLOAD_FAILED(3003, "文件上传失败"),
    FILE_NOT_EXISTS(3004, "文件不存在"),
    FILE_DOWNLOAD_ERROR(3005, "文件下载失败"),
    FILE_PREVIEW_ERROR(3006, "文件预览失败"),
    FILE_PARSE_FAIL(3007, "文件解析失败"),

    APPLICATION_DUPLICATE(4000, "您已经提交过该日期的晚归申请"),
    APPLICATION_SAVE_FAILED(4001, "保存晚归申请记录失败"),

    EXPLANATION_DUPLICATE(5000, "您已提交过该日期的晚归说明"),
    EXPLANATION_SAVE_FAILED(5001, "保存晚归说明记录失败"),
    EXPLANATION_REPEATED(5002, "该晚归记录说明已处理"),

    NOTIFICATION_SEND_FAILED(6000, "通知发送失败"),
    NOTIFICATION_SAVE_FAILED(6001, "通知保存失败"),
    NOTIFICATION_UPDATE_FAILED(6002, "通知更新失败"),

    SUPER_ADMIN_NOT_FOUND(7000, "该超级管理员不存在"),
    SUPER_ADMIN_DISABLE(7001, "该管理员已被禁用"),
    SUPER_ADMIN_DELETE_FAILED(7002, "删除管理员失败"),

    WARN_RULE_NOT_SET(8001, "找不到该预警规则"),

    ALARM_RATE_TOO_HIGH(9000, "报警频率过高，请稍后再试"),
    ALARM_ENDED(9001, "该报警已结束，无法进行操作"),
    ALARM_PROCESSING(9002, "该报警正在处理中，请不要重复报警"),
    ALARM_ONE_CLICK_RATE_TOO_HIGH(9003, "一键报警报警过于频繁"),
    ALARM_NOT_FOUND(9004, "找不到该报警相关的信息"),

    LOCATION_TRACK_NOT_FOUND(10000, "找不到该位置轨迹信息"),
    LOCATION_TRACK_SAVE_FAILED(10001, "保存位置轨迹失败"),

    ALARM_CONFIG_KEY_EXISTS(11000, "配置键已存在"),
    ALARM_CONFIG_NOT_FOUNT(11001, "该报警配置不存在"),
    ALARM_CONFIG_VERSION_MISMATCH(10010, "配置版本不匹配，请刷新后重试"),
    ALARM_AMAP_API_ERROR(10011, "调用高低地图API失败"),
    ALARM_CONFIG_STATUS_INVALID(10012, "该报警配置状态无效" ),

    SYS_USER_NO_LOCKED_FAIL(12000, "获取SysUserNo锁失败"),
    SYS_USER_DISABLE(12001, "该班级管理员已被禁用"),


    VIDEO_DATA_SAVE_FAILED(13000, "保存音视频数据失败" ),
    VIDEO_CHUNK_MERGE_FAILED(13001, "合并音视频失败" );

    private final Integer code;
    private final String message;

    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public Integer getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
