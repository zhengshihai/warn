package com.tianhai.warn.service.impl;

import com.alibaba.fastjson.JSON;
import com.tianhai.warn.constants.AlarmConstants;
import com.tianhai.warn.dto.LocationUpdateDTO;
import com.tianhai.warn.enums.AlarmStatus;
import com.tianhai.warn.enums.ResultCode;
import com.tianhai.warn.exception.BusinessException;
import com.tianhai.warn.exception.SystemException;
import com.tianhai.warn.handler.LocationWebSocketHandler;
import com.tianhai.warn.model.AlarmRecord;
import com.tianhai.warn.mq.AlarmContext;
import com.tianhai.warn.mq.RocketMQMessageSender;
import com.tianhai.warn.service.AlarmRecordService;
import com.tianhai.warn.service.WebSocketService;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class WebSocketServiceImpl implements WebSocketService {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketServiceImpl.class);
    private static final int MAX_RETRY_TIMES = 3;
    private static final long RETRY_DELAY_MS = 1000;

    @Autowired
    private LocationWebSocketHandler locationWebSocketHandler;

    @Autowired
    @Qualifier("alarmProducer")
    private DefaultMQProducer defaultMQProducer;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private RocketMQMessageSender rocketMQMessageSender;

    @Autowired
    private AlarmRecordService alarmRecordService;

    @Override
    public void establishConnection(AlarmContext context) {
        try {
            // 验证报警状态
            if (isAlarmInActive(context.getLocationUpdateDTO().getAlarmNo())) {
                throw new BusinessException(ResultCode.ALARM_ENDED);
            }

            // 发送初始消息
            String initialMessage = JSON.toJSONString(context);

            // 检查WebSocket连接是否存在
            if (!locationWebSocketHandler.isSessionExists(context.getLocationUpdateDTO().getAlarmNo())) {
                logger.info("WebSocket连接尚未建立，等待客户端连接, alarmNo: {}",
                        context.getLocationUpdateDTO().getAlarmNo());
                return;
            }

            locationWebSocketHandler.sendMessage(
                    context.getLocationUpdateDTO().getAlarmNo(),
                    initialMessage);
        } catch (Exception e) {
            logger.error("建立WebSocket连接失败: {}", context, e);
            throw new SystemException(ResultCode.ERROR);
        }
    }

    @Override
    public void handleLocationUpdate(LocationUpdateDTO locationUpdateDTO) {
        // 判断报警是否处理完成
        if (isAlarmInActive(locationUpdateDTO.getAlarmNo())) {
            logger.error("该报警已处理，不再接收位置信息, alarmNo: {}",
                    locationUpdateDTO.getAlarmNo());
            throw new BusinessException(ResultCode.ALARM_ENDED);
        }

        // 构建RocketMQ消息
        Message message = new Message(
                AlarmConstants.ROCKETMQ_TOPIC_LOCATION,
                AlarmConstants.ROCKETMQ_TAG_LOCATION_UPDATE,
                locationUpdateDTO.getAlarmNo(),
                JSON.toJSONString(locationUpdateDTO).getBytes());

        // 发送消息到RocketMQ（带重试）
        sendMessageWithRetry(message, locationUpdateDTO.getAlarmNo());
    }

    private void sendMessageWithRetry(Message message, String alarmNo) {
        int retryCount = 0;
        while (retryCount < MAX_RETRY_TIMES) {
            try {
                // 此处使用mq原始方式发送
                defaultMQProducer.send(message);
                return;
            } catch (Exception e) {
                retryCount++;
                if (retryCount == MAX_RETRY_TIMES) {
                    logger.error("发送位置消息到RocketMQ失败，已达到最大重试次数, alarmNo: {}",
                            alarmNo, e);
                    throw new SystemException(ResultCode.ERROR);
                }
                logger.warn("发送位置消息到RocketMQ失败，准备重试, alarmNo: {}, 重试次数: {}",
                        alarmNo, retryCount);
                try {
                    TimeUnit.MILLISECONDS.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new SystemException(ResultCode.ERROR);
                }
            }
        }
    }

    private boolean isAlarmInActive(String alarmNo) {
        if (alarmNo == null) {
            logger.error("报警编号为空");
            return true;
        }

        // 检查Redis缓存中的报警状态
        String statusKey = AlarmConstants.REDIS_KEY_ALARM_STATUS + alarmNo;
        AlarmStatus status = (AlarmStatus) redisTemplate.opsForValue().get(statusKey);

        // 如果缓存中有状态，直接返回结果
        if (status != null) {
            logger.info("从缓存中获取到报警状态: {}, alarmNo: {}", status, alarmNo);
            return status == AlarmStatus.PROCESSED || status == AlarmStatus.CLOSED;
        }

        // 如果缓存中没有状态，说明报警还未被处理，返回false
        logger.info("缓存中未找到报警状态，认为报警未结束, alarmNo: {}", alarmNo);
        return false;
    }

    @Override
    public void closeConnection(String alarmNo) {
        locationWebSocketHandler.closeConnection(alarmNo);
    }
}
