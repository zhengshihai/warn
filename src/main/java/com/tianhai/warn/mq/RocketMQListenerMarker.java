package com.tianhai.warn.mq;

import java.util.Collections;
import java.util.Map;

/**
 * 一个标记接口，用于标识所有 RocketMQ 消息监听器
 */
public interface RocketMQListenerMarker {
    String getTopic();

    String getTags();

    boolean isConsumeOrderly();

    default Map<String, Object> extraParams() {
        return Collections.emptyMap();
    }

}
