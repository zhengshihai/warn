package com.tianhai.warn.constants;

public class AlarmConstants {
    // Redis key前缀
    public static final String REDIS_KEYS_ALARM_GEO = "alarm:geo:";
    public static final String REDIS_KEY_ALARM_STATUS = "alarm:status:";
    public static final String REDIS_KEY_ALARM_RATE_LIMIT = "alarm:rate:limit:";
    public static final String REDIS_KEY_ALARM_TRACK_TIME = "alarm:track:time:";

    // Redis过期时间
    public static final long REDIS_EXPIRE_ALARM_STATUS = 24 * 60 * 60; // 24小时
    public static final long REDIS_EXPIRE_ALARM_GEO = 60 * 60; // 1小时
    public static final long REDIS_EXPIRE_ALARM_PROCESS = 24 * 60 * 60; // 24小时
    public static final long REDIS_EXPIRE_ALARM_RATE_LIMIT = 60 * 30; // 半小时

    public static final String COMPENSATE_TOPIC = "compensate-topic"; // MQ补偿主题
    public static final String ROCKETMQ_TOPIC_LOCATION = "location-topic";
    public static final String ROCKETMQ_TAG_LOCATION_UPDATE = "location-update";
    public static final String ROCKETMQ_TOPIC_VIDEO_PROCESS = "video-topic";
    public static final String ROCKETMQ_TAG_VIDEO_PROCESS = "video-process";

    public static final Integer ONE_CLICK_ALARM_TYPE = 1;
    public static final Integer INTERFACE_ALARM_TYPE = 2;

    public static final String ALARM_LBS_MAP = "ALARM_LBS_MAP"; // 高德地图API标识符
    public static final Integer ALARM_CONFIG_ACTIVE = 1; // 报警配置启用状态
    public static final Integer ALARM_CONFIG_INACTIVE = 0; // 报警配置禁用状态
    public static final Integer ALARM_ONE_CLICK_RATE_LIMIT = 30;// 一键报警在半小时内次数最多不超过30次


    /**
     * WebSocket会话超时时间（单位：秒）
     */
    public static final class WebSocketTimeout {
        /**
         * 一键报警定位会话超时时间 - 30分钟
         * 考虑到报警处理可能需要较长时间，给予足够的会话保持时间
         */
        public static final int ALARM_LOCATION = 1800;

        /**
         * 普通位置追踪会话超时时间 - 20分钟
         * 用于日常位置追踪，超时时间可以相对短一些
         */
        public static final int NORMAL_TRACKING = 1200;

        /**
         * 临时会话超时时间 - 5分钟
         * 用于临时性的位置查询等操作
         */
        public static final int TEMPORARY = 300;

        /**
         * 心跳检测间隔时间 - 30秒
         * 定期发送心跳包以保持连接活跃
         */
        public static final int HEARTBEAT_INTERVAL = 30;
    }

    // 触发一键报警短信的内容模板
    public static final String ALARM_TRIGGER_SMS_CLASS_MANAGER_TEMPLATE =
            "您有学生发出了一键报警求助！学号：%s，报警等级：%s，时间：%s，请立即处理！";
    public static final String ALARM_TRIGGER_SMS_PARENT_TEMPLATE =
            "您的孩子 %s 发出了一键报警求助！报警等级：%s，时间：%s，请立即联系孩子和学校";

    // 取消一键报警短信的内容模板
    public static final String ALARM_CANCEL_SMS_CLASS_MANAGER_TEMPLATE =
            "您的学生取消了一键报警求助！学号：%s, 姓名：%s，时间：%s，为确保安全，建议您及时联系该学生！";
    public static final String ALARM_CANCEL_SMS_PARENT_TEMPLATE =
            "您的孩子 %s 在 %s 取消了一键报警求助！为确保安全，建议您及时联系您的孩子";

}