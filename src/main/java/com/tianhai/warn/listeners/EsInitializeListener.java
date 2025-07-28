package com.tianhai.warn.listeners;

import com.tianhai.warn.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;
import reactor.util.annotation.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Elasticsearch初始化监听器
 * 在Spring容器启动完成后检查并初始化Elasticsearch索引
 */
@Component
public class EsInitializeListener implements ApplicationListener<ContextRefreshedEvent> {

    private static final Logger logger = LoggerFactory.getLogger(EsInitializeListener.class);

//    private static volatile boolean initialized = false;

    private static final String INDEX_NAME = "notification";

    @Autowired
    private NotificationService notificationService;

//    @Override
//    public void onApplicationEvent(ContextRefreshedEvent event) {
//        // 确保只执行一次
//        if (initialized) {
//            return;
//        }
//
//        synchronized (this) {
//            if (initialized) {
//                return;
//            }
//
//            try {
//                logger.info("开始检查Elasticsearch索引状态...");
//
//                // 检查索引是否存在
//                if (!notificationService.validateIndexExists(INDEX_NAME)) {
//                    logger.info("该Elasticsearch索引 {} 不存在，开始创建索引...", INDEX_NAME);
//                    notificationService.rebuildIndex(INDEX_NAME);
//                } else {
//                    logger.info("该Elasticsearch索引 {} 已存在，无需创建。", INDEX_NAME);
//                }
//
//                initialized = true;
//                logger.info("Elasticsearch索引初始化完成");
//            } catch (Exception e) {
//                logger.info("Elasticsearch索引初始化失败", e);
//                notificationService.deleteEsIndexByIndexName(INDEX_NAME);
//            }
//        }
//    }

    private static final AtomicBoolean initialized = new AtomicBoolean(false);

    @Override
    public void onApplicationEvent(@Nullable ContextRefreshedEvent event) {
        // 使用CAS操作 确保只执行一次全量同步ES索引
        if (initialized.compareAndSet(false, true)) {
            try {
                logger.info("开始检查Elasticsearch索引状态...");

                // 检查索引是否存在
                if (!notificationService.validateIndexExists(INDEX_NAME)) {
                    logger.info("该Elasticsearch索引 {} 不存在，开始创建索引...", INDEX_NAME);
                    notificationService.rebuildIndex(INDEX_NAME);
                } else {
                    logger.info("该Elasticsearch索引 {} 已存在，无需创建。", INDEX_NAME);
                }

                logger.info("Elasticsearch索引初始化完成");

            } catch (Exception e) {
                logger.info("Elasticsearch索引初始化失败", e);
                notificationService.deleteEsIndexByIndexName(INDEX_NAME);
            }
        }
    }
}
