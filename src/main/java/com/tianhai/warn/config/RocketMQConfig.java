package com.tianhai.warn.config;

import com.tianhai.warn.mq.RocketMQListenerMarker;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.consumer.listener.MessageListenerOrderly;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.consumer.ConsumeFromWhere;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Configuration
public class RocketMQConfig {

    /**
     * A[应用启动] --> B[扫描RocketMQListenerMarker实现]
     * B --> C[创建RocketMQConsumerConfig]
     * C --> D[创建DefaultMQPushConsumer]
     * D --> E[启动消费者]
     * E --> F[开始监听消息]
     */

    @Value("${rocketmq.name-server}")
    private String nameServer;

    // 报警业务配置
    @Value("${rocketmq.alarm.producer.group}")
    private String locationProducerGroup;

    @Value("${rocketmq.alarm.consumer.group}")
    private String locationConsumerGroup;

    // 视频业务配置
    @Value("${rocketmq.video.producer.group}")
    private String videoProducerGroup;

    @Value("${rocketmq.video.consumer.group}")
    private String videoConsumerGroup;

    // 补偿业务配置
    @Value("${rocketmq.compensate.producer.group}")
    private String compensateProducerGroup;

    @Value("${rocketmq.compensate.consumer.group}")
    private String compensateConsumerGroup;

    // 通用配置
    @Value("${rocketmq.producer.send-message-timeout}")
    private int sendMessageTimeout;

    @Value("${rocketmq.producer.retry-times-when-send-failed}")
    private int retryTimesWhenSendFailed;

    @Value("${rocketmq.producer.retry-times-when-send-async-failed}")
    private int retryTimesWhenSendAsyncFailed;

    @Value("${rocketmq.producer.max-message-size}")
    private int maxMessageSize;

    @Autowired
    private List<RocketMQListenerMarker> listenerBeans;


    private static final Logger logger = LoggerFactory.getLogger(RocketMQConfig.class);

    /**
     * 位置业务生产者
     */
    @Bean("locationProducer")
//    public DefaultMQProducer alarmProducer() {
    public DefaultMQProducer locationProducer() {
        DefaultMQProducer producer = new DefaultMQProducer();
        producer.setNamesrvAddr(nameServer);
        producer.setProducerGroup(locationProducerGroup);
        producer.setSendMsgTimeout(sendMessageTimeout);
        producer.setRetryTimesWhenSendFailed(retryTimesWhenSendFailed);
        producer.setRetryTimesWhenSendAsyncFailed(retryTimesWhenSendAsyncFailed);
        producer.setMaxMessageSize(maxMessageSize);

        try {
            logger.info("正在启动报警业务 RocketMQ Producer...");
            producer.start();

            logger.info("报警业务 RocketMQ Producer 启动成功，nameServer: {}, producerGroup: {}",
                    nameServer, locationProducerGroup);
        } catch (Exception e) {
            logger.error("报警业务 RocketMQ Producer 启动失败: {}, nameServer: {}, producerGroup: {}",
                    e.getMessage(), nameServer, locationProducerGroup, e);
            throw new RuntimeException("报警业务 RocketMQ Producer 启动失败", e);
        }

        return producer;
    }

    /**
     * 位置业务消费者
     */
    @Bean("locationConsumer")
//    public DefaultMQPushConsumer alarmConsumer() {
    public DefaultMQPushConsumer locationConsumer() {
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer();
        consumer.setNamesrvAddr(nameServer);
        consumer.setConsumerGroup(locationConsumerGroup);
        return consumer;
    }

    /**
     * 视频业务生产者
     */
    @Bean("videoProducer")
    public DefaultMQProducer videoProducer() {
        DefaultMQProducer producer = new DefaultMQProducer();
        producer.setNamesrvAddr(nameServer);
        producer.setProducerGroup(videoProducerGroup);
        producer.setSendMsgTimeout(sendMessageTimeout);
        producer.setRetryTimesWhenSendFailed(retryTimesWhenSendFailed);
        producer.setRetryTimesWhenSendAsyncFailed(retryTimesWhenSendAsyncFailed);
        producer.setMaxMessageSize(maxMessageSize);

        try {
            logger.info("正在启动视频业务 RocketMQ Producer...");
            producer.start();
            logger.info("视频业务 RocketMQ Producer 启动成功，nameServer: {}, producerGroup: {}",
                    nameServer, videoProducerGroup);
        } catch (Exception e) {
            logger.error("视频业务 RocketMQ Producer 启动失败: {}, nameServer: {}, producerGroup: {}",
                    e.getMessage(), nameServer, videoProducerGroup, e);
            throw new RuntimeException("视频业务 RocketMQ Producer 启动失败", e);
        }

        return producer;
    }

    /**
     * 视频业务消费者
     */
    @Bean("videoConsumer")
    public DefaultMQPushConsumer videoConsumer() {
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer();
        consumer.setNamesrvAddr(nameServer);
        consumer.setConsumerGroup(videoConsumerGroup);
        return consumer;
    }

    @Bean
    public List<RocketMQConsumerConfig> consumerConfigs() {
        List<RocketMQConsumerConfig> configs = new ArrayList<>();

        for (RocketMQListenerMarker listener : listenerBeans) {
            RocketMQConsumerConfig config;
            if (listener.isConsumeOrderly()) {
                config = new RocketMQConsumerConfig(
                        listener.getTopic(),
                        listener.getTags(),
                        (MessageListenerOrderly) listener,
                        listener.extraParams());
            } else {
                config = new RocketMQConsumerConfig(
                        listener.getTopic(),
                        listener.getTags(),
                        (MessageListenerConcurrently) listener,
                        listener.extraParams());
            }
            configs.add(config);
        }

        return configs;
    }

    /**
     * 补偿业务生产者
     */
    @Bean("compensateProducer")
    public DefaultMQProducer compensateProducer() {
        DefaultMQProducer producer = new DefaultMQProducer();
        producer.setNamesrvAddr(nameServer);
        producer.setProducerGroup(compensateProducerGroup);
        producer.setSendMsgTimeout(sendMessageTimeout);
        producer.setRetryTimesWhenSendFailed(retryTimesWhenSendFailed);
        producer.setRetryTimesWhenSendAsyncFailed(retryTimesWhenSendAsyncFailed);
        producer.setMaxMessageSize(maxMessageSize);

        try {
            logger.info("正在启动补偿业务 RocketMQ Producer...");
            producer.start();
            logger.info("补偿业务 RocketMQ Producer 启动成功，nameServer: {}, producerGroup: {}",
                    nameServer, "compensate-producer-group");
        } catch (Exception e) {
            logger.error("补偿业务 RocketMQ Producer 启动失败: {}, nameServer: {}, producerGroup: {}",
                    e.getMessage(), nameServer, "compensate-producer-group", e);
            throw new RuntimeException("补偿业务 RocketMQ Producer 启动失败", e);
        }

        return producer;
    }

    @Bean
    public List<DefaultMQPushConsumer> consumers(List<RocketMQConsumerConfig> configs) throws MQClientException {
        List<DefaultMQPushConsumer> consumers = new ArrayList<>();

        for (RocketMQConsumerConfig config : configs) {
            // 根据topic判断使用哪个消费者组
            String consumerGroup;
            if (config.getTopic().startsWith("location-")) {
                consumerGroup = locationConsumerGroup;
            } else if (config.getTopic().startsWith("video-")) {
                consumerGroup = videoConsumerGroup;
            } else {
                logger.warn("未知的topic类型: {}, 使用默认的报警消费者组", config.getTopic());
                consumerGroup = locationConsumerGroup;
            }

            DefaultMQPushConsumer consumer = new DefaultMQPushConsumer(consumerGroup);
            consumer.setNamesrvAddr(nameServer);
            consumer.subscribe(config.getTopic(), config.getTags());

            // 设置消费模式
            consumer.setConsumeFromWhere(ConsumeFromWhere.CONSUME_FROM_LAST_OFFSET);

            // 注册监听器
            if (config.isConsumeOrderly()) {
                consumer.registerMessageListener(config.getOrderlyListener());
            } else {
                consumer.registerMessageListener(config.getConcurrentListener());
            }

            // 应用额外参数
            applyExtraParams(consumer, config.getExtraParams());

            try {
                logger.info("正在启动 RocketMQ Consumer...");
                consumer.start();
                logger.info("RocketMQ Consumer 启动成功，nameServer: {}, consumerGroup: {}，topic:{}, tags:{}",
                        nameServer, consumerGroup, config.getTopic(), config.getTags());
            } catch (Exception e) {
                logger.error("RocketMQ Consumer 启动失败: {}, nameServer: {}, consumerGroup: {}, topic:{}, tags:{}",
                        e.getMessage(), nameServer, consumerGroup, config.getTopic(), config.getTags(), e);
                throw new RuntimeException("RocketMQ Consumer 启动失败", e);
            }

            consumers.add(consumer);
        }

        return consumers;
    }

    // 扩展其他参数
    private void applyExtraParams(DefaultMQPushConsumer consumer, Map<String, Object> extraParams) {
        if (extraParams == null || extraParams.isEmpty()) {
            logger.info("未配置任何额外参数，使用默认设置。");
            return;
        }

        boolean hasCustomParam = false;

        // 消费线程最小值
        Object minThreads = extraParams.get("consumeThreadMin");
        if (minThreads instanceof Number) {
            consumer.setConsumeThreadMin(((Number) minThreads).intValue());
            hasCustomParam = true;
        }

        // 消费线程最大值
        Object maxThreads = extraParams.get("consumeThreadMax");
        if (maxThreads instanceof Number) {
            consumer.setConsumeThreadMax(((Number) maxThreads).intValue());
            hasCustomParam = true;
        }

        // 单次拉取消息数量
        Object pullBatchSize = extraParams.get("pullBatchSize");
        if (pullBatchSize instanceof Number) {
            consumer.setPullBatchSize(((Number) pullBatchSize).intValue());
            hasCustomParam = true;
        }

        // 消息批量消费最大数
        Object consumeMessageBatchMaxSize = extraParams.get("consumeMessageBatchMaxSize");
        if (consumeMessageBatchMaxSize instanceof Number) {
            consumer.setConsumeMessageBatchMaxSize(((Number) consumeMessageBatchMaxSize).intValue());
            hasCustomParam = true;
        }

        // 拉取间隔（单位 ms）
        Object pullInterval = extraParams.get("pullInterval");
        if (pullInterval instanceof Number) {
            consumer.setPullInterval(((Number) pullInterval).longValue());
            hasCustomParam = true;
        }

        // 消息消费超时时间（单位分钟）
        Object consumeTimeout = extraParams.get("consumeTimeout");
        if (consumeTimeout instanceof Number) {
            consumer.setConsumeTimeout(((Number) consumeTimeout).longValue());
            hasCustomParam = true;
        }

        // 最大重新消费次数
        Object maxReconsumeTimes = extraParams.get("maxReconsumeTimes");
        if (maxReconsumeTimes instanceof Number) {
            consumer.setMaxReconsumeTimes(((Number) maxReconsumeTimes).intValue());
            hasCustomParam = true;
        }

        if (!hasCustomParam) {
            logger.info("未匹配任何已知可配置参数，extraParams: {}", extraParams);
        }
    }
}
