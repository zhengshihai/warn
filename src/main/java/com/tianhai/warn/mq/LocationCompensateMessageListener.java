package com.tianhai.warn.mq;

import com.alibaba.fastjson.JSON;
import com.tianhai.warn.constants.AlarmConstants;
import com.tianhai.warn.dto.LocationUpdateDTO;
import com.tianhai.warn.service.LocationTrackService;
import io.lettuce.core.dynamic.annotation.CommandNaming;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;

// 位置补偿消息监听器
@Component
public class LocationCompensateMessageListener implements MessageListenerConcurrently, RocketMQListenerMarker {

    private static final Logger logger = LoggerFactory.getLogger(LocationCompensateMessageListener.class);

    @Autowired
    private LocationTrackService locationTrackService;

    @Override
    public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> messageExtList,
                                                    ConsumeConcurrentlyContext context) {
        for (Message message : messageExtList) {
            String body = new String(message.getBody(), StandardCharsets.UTF_8);
            try {
                LocationUpdateDTO locationUpdateDTO = JSON.parseObject(body, LocationUpdateDTO.class);
                // 再次尝试将定位信息写入Mysql
                locationTrackService.saveLocationTrack(locationUpdateDTO);
                logger.info("补偿消息成功写入MySQL: {}", locationUpdateDTO);

            } catch (Exception e) {
                logger.error("补偿消息写入MySQL失败，将重试：{}", body, e);
                return ConsumeConcurrentlyStatus.RECONSUME_LATER;
            }
        }

        return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
    }

    @Override
    public String getTopic() {
        return AlarmConstants.COMPENSATE_TOPIC;
    }

    @Override
    public String getTags() {
        return "*";
    }

    @Override
    public boolean isConsumeOrderly() {
        return false;
    }
}
