package com.tianhai.warn.handler;

import com.alibaba.fastjson.JSON;
import com.tianhai.warn.constants.AlarmConstants;
import com.tianhai.warn.dto.LocationUpdateDTO;
import com.tianhai.warn.exception.BusinessException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

//todo 前端在建立 WebSocket 连接时，通过 URL 参数指定业务类型：  
//   ws://localhost:8080/warn/ws/location?alarmNo=AL0001&businessType=ALARM_LOCATION
@Component
public class LocationWebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(LocationWebSocketHandler.class);
    private static final Map<String, WebSocketSession> SESSIONS = new ConcurrentHashMap<>();
    private static final Map<String, Long> LAST_MESSAGE_TIME = new ConcurrentHashMap<>();
    private static final Map<String, Integer> SESSION_TIMEOUTS = new ConcurrentHashMap<>();
    private static final int MAX_MESSAGE_SIZE = 1024 * 1024; // 1MB

    @Autowired
    @Qualifier("alarmProducer")
    private DefaultMQProducer defaultMQProducer;

    public LocationWebSocketHandler() {
        // 启动心跳检查
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(this::checkHeartbeats,
                AlarmConstants.WebSocketTimeout.HEARTBEAT_INTERVAL,
                AlarmConstants.WebSocketTimeout.HEARTBEAT_INTERVAL,
                TimeUnit.SECONDS);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        logger.info("尝试建立WebSocket连接，URI: {}", session.getUri());

        String alarmNo = getAlarmNoFromSession(session);
        if (alarmNo == null) {
            logger.error("WebSocket连接失败：缺少alarmNo参数，URI: {}", session.getUri());
            session.close(CloseStatus.BAD_DATA.withReason("Missing alarmNo parameter"));
            return;
        }

        // 设置消息大小限制
        session.setBinaryMessageSizeLimit(MAX_MESSAGE_SIZE);
        session.setTextMessageSizeLimit(MAX_MESSAGE_SIZE);

        // 根据业务类型设置会话超时时间
        int timeout = determineSessionTimeout(session);
        SESSION_TIMEOUTS.put(alarmNo, timeout);

        SESSIONS.put(alarmNo, session);
        LAST_MESSAGE_TIME.put(alarmNo, System.currentTimeMillis());
        logger.info("WebSocket连接已建立，alarmNo: {}, 超时时间: {}秒, 远程地址: {}, URI: {}",
                alarmNo, timeout, session.getRemoteAddress(), session.getUri());

        // 发送心跳消息
        sendHeartbeat(session);
    }

    private int determineSessionTimeout(WebSocketSession session) {
        // 从session属性或请求参数中获取业务类型
        String businessType = getBusinessTypeFromSession(session);

        // 根据业务类型返回对应的超时时间
        switch (businessType) {
            case "ALARM_LOCATION":
                return AlarmConstants.WebSocketTimeout.ALARM_LOCATION;
            case "NORMAL_TRACKING":
                return AlarmConstants.WebSocketTimeout.NORMAL_TRACKING;
            case "TEMPORARY":
                return AlarmConstants.WebSocketTimeout.TEMPORARY;
            default:
                // 默认使用报警定位的超时时间
                return AlarmConstants.WebSocketTimeout.ALARM_LOCATION;
        }
    }

    private String getBusinessTypeFromSession(WebSocketSession session) {
        try {
            String query = session.getUri().getQuery();
            if (query != null && query.contains("businessType=")) {
                return query.split("businessType=")[1].split("&")[0];
            }
        } catch (Exception e) {
            logger.error("获取业务类型失败", e);
        }
        return "ALARM_LOCATION"; // 默认返回报警定位类型
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String alarmNo = getAlarmNoFromSession(session);
        if (alarmNo == null) {
            session.close(CloseStatus.BAD_DATA.withReason("Missing alarmNo parameter"));
            return;
        }

        try {
            // 更新最后消息时间
            LAST_MESSAGE_TIME.put(alarmNo, System.currentTimeMillis());

            // 解析位置更新消息
            LocationUpdateDTO locationUpdateDTO = JSON.parseObject(message.getPayload(), LocationUpdateDTO.class);

            // 验证消息格式
            if (!isValidLocationMessage(locationUpdateDTO)) {
                logger.warn("无效的位置消息格式: {}", message.getPayload());
                session.sendMessage(new TextMessage("Invalid message format"));
                return;
            }

            // 发送到RocketMQ
            sendToRocketMQ(locationUpdateDTO);

            // 发送确认消息
            session.sendMessage(new TextMessage("Message received"));
        } catch (Exception e) {
            logger.error("处理位置消息失败: {}", message.getPayload(), e);
            session.sendMessage(new TextMessage("Error processing message"));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String alarmNo = getAlarmNoFromSession(session);
        if (alarmNo != null) {
            SESSIONS.remove(alarmNo);
            LAST_MESSAGE_TIME.remove(alarmNo);
            SESSION_TIMEOUTS.remove(alarmNo);
            logger.info("WebSocket连接已关闭，alarmNo: {}, status: {}", alarmNo, status);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        logger.error("WebSocket传输错误: {}", exception.getMessage());
        String alarmNo = getAlarmNoFromSession(session);
        if (alarmNo != null) {
            SESSIONS.remove(alarmNo);
            LAST_MESSAGE_TIME.remove(alarmNo);
            SESSION_TIMEOUTS.remove(alarmNo);
        }
        session.close(CloseStatus.SERVER_ERROR);
    }

    private String getAlarmNoFromSession(WebSocketSession session) {
        try {
            String query = session.getUri().getQuery();
            if (query == null || !query.contains("alarmNo=")) {
                return null;
            }
            return query.split("alarmNo=")[1].split("&")[0];
        } catch (Exception e) {
            logger.error("获取alarmNo失败", e);
            return null;
        }
    }

    public boolean isSessionExists(String alarmNo) {
        WebSocketSession session = SESSIONS.get(alarmNo);
        return session != null && session.isOpen();
    }

    public void sendMessage(String alarmNo, String message) {
        WebSocketSession session = SESSIONS.get(alarmNo);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(message));
                // 更新最后消息时间
                LAST_MESSAGE_TIME.put(alarmNo, System.currentTimeMillis());
            } catch (IOException e) {
                logger.error("发送消息失败，alarmNo: {}", alarmNo, e);
                SESSIONS.remove(alarmNo);
                LAST_MESSAGE_TIME.remove(alarmNo);
            }
        } else {
            logger.warn("WebSocket会话不存在或已关闭，alarmNo: {}", alarmNo);
        }
    }

    private void sendToRocketMQ(LocationUpdateDTO locationUpdateDTO) {
        try {
            Message message = new Message(
                    AlarmConstants.ROCKETMQ_TOPIC_LOCATION,
                    AlarmConstants.ROCKETMQ_TAG_LOCATION_UPDATE,
                    locationUpdateDTO.getAlarmNo(),
                    JSON.toJSONString(locationUpdateDTO).getBytes());
            SendResult sendResult = defaultMQProducer.send(message);
            logger.info("发送消息到RocketMQ成功: {}, SendResult: {}", locationUpdateDTO, sendResult);
        } catch (Exception e) {
            logger.error("发送消息到RocketMQ失败: {}, 错误信息: {}", locationUpdateDTO, e.getMessage(), e);
            throw new BusinessException("发送消息失败");
        }
    }

    private boolean isValidLocationMessage(LocationUpdateDTO dto) {
        if (dto == null) {
            logger.warn("位置信息为空");
            return false;
        }
        if (dto.getAlarmNo() == null) {
            logger.warn("alarmNo 为空");
            return false;
        }
        if (dto.getLatitude() == null || dto.getLongitude() == null) {
            logger.warn("经纬度为空: lat={}, lon={}", dto.getLatitude(), dto.getLongitude());
            return false;
        }
        if (dto.getLocationTime() == null) {
            logger.warn("locationTime 为空");
            return false;
        }
        if (dto.getChangeUnit() != null && dto.getChangeUnit() <= 0) {
            logger.warn("changeUnit 非法: {}", dto.getChangeUnit());
            return false;
        }
        return true;
    }

    private void sendHeartbeat(WebSocketSession session) {
        try {
            session.sendMessage(new TextMessage("heartbeat"));
            String alarmNo = getAlarmNoFromSession(session);
            if (alarmNo != null) {
                LAST_MESSAGE_TIME.put(alarmNo, System.currentTimeMillis());
            }
        } catch (IOException e) {
            logger.error("发送心跳消息失败", e);
        }
    }

    private void checkHeartbeats() {
        long now = System.currentTimeMillis();
        SESSIONS.forEach((alarmNo, session) -> {
            Long lastMessageTime = LAST_MESSAGE_TIME.get(alarmNo);
            Integer timeout = SESSION_TIMEOUTS.get(alarmNo);

            if (lastMessageTime != null && timeout != null &&
                    now - lastMessageTime > timeout * 1000) {
                try {
                    session.close(CloseStatus.SESSION_NOT_RELIABLE);
                    SESSIONS.remove(alarmNo);
                    LAST_MESSAGE_TIME.remove(alarmNo);
                    SESSION_TIMEOUTS.remove(alarmNo);
                    logger.info("会话超时关闭，alarmNo: {}, 超时时间: {}秒", alarmNo, timeout);
                } catch (IOException e) {
                    logger.error("关闭超时会话失败，alarmNo: {}", alarmNo, e);
                }
            }
        });
    }

    public void closeConnection(String alarmNo) {
        WebSocketSession session = SESSIONS.get(alarmNo);
        if (session != null && session.isOpen()) {
            try {
                session.close(CloseStatus.NORMAL);
                SESSIONS.remove(alarmNo);
                LAST_MESSAGE_TIME.remove(alarmNo);
                SESSION_TIMEOUTS.remove(alarmNo);
                logger.info("WebSocket连接已关闭，alarmNo:{}", alarmNo);
            } catch (IOException e) {
                logger.error("关闭WebSocket连接失败，alarmNo:{}");
            }
        }
    }
}

/*
POSTMAN测试用例：
{
  "alarmNo": "AL0001",
 "latitude": 30.79,
  "longitude": 104.5744,
  "locationTime": "2025-06-08 09:38",
  "speed": 0,
  "direction": 0,
  "locationAccuracy": 10
}
 */