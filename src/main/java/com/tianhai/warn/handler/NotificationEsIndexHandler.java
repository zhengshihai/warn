package com.tianhai.warn.handler;

import com.tianhai.warn.model.Notification;
import com.tianhai.warn.service.NotificationService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
// 简化业务 放弃使用ES
/**
 * 通知ES索引定时处理器
 * 定时检查未索引的通知并建立ES文档
 */
@Component
public class NotificationEsIndexHandler {
    private static final Logger logger = LoggerFactory.getLogger(NotificationEsIndexHandler.class);

    @Autowired
    private NotificationService notificationService;

//    @Value("${notification.es.index.batch.size:100}")
//    private int batchSize;
//
//    @Value("${notification.es.index.max.retry:3}")
//    private int maxRetry;
//
//    @Value("${notification.es.index.timeout:30}")
//    private int timeoutSeconds;


    // 为方便毕设，暂时停用xxl-job
//    private final Integer batchSize = 1000; // 每批次处理的通知数量
//
//    private final Integer maxRetry = 3; // 最大重试次数
//
//    private final Integer timeoutSeconds = 30; // 每批次处理的超时时间（秒）
//
//    /**
//     * 定时处理未索引的通知
//     */
//    @XxlJob("notificationEsIndexHandler")
//    public void handleNotificationEsIndex() {
//        try {
//            XxlJobHelper.log("开始执行通知ES索引定时任务");
//
//            // 统计未索引的通知数量
//            int totalUnindexed = notificationService.countUnindexedNotifications();
//            XxlJobHelper.log("未索引的通知总数: {}", totalUnindexed);
//
//            if (totalUnindexed == 0) {
//                XxlJobHelper.log("没有未索引的通知，任务结束");
//                return;
//            }
//
//            // 分批次处理
//            int processedCount = 0;
//            int batchCount = 0;
//            int maxBatches = (int) Math.ceil((double) totalUnindexed / batchSize);
//
//            while (processedCount < totalUnindexed) {
//                batchCount++;
//                XxlJobHelper.log("开始处理第 {}/{} 批次", batchCount, maxBatches);
//
//                try {
//                    // 查询当前批次未索引的通知
//                    List<Notification> batchNotifications = notificationService.selectBatchUnindexed(batchSize);
//
//                    if (batchNotifications.isEmpty()) {
//                        XxlJobHelper.log("批次 {} 没有数据，跳过", batchCount);
//                        break;
//                    }
//
//                    // 异步索引当前批次
//                    final Integer curBatchCount = batchCount;
//                    CompletableFuture<Void> indexFuture = CompletableFuture.runAsync(() -> {
//                        try {
//                            notificationService.indexNotificationList(batchNotifications);
//                            XxlJobHelper.log("批次 {} 索引完成，处理了 {} 条通知",
//                                    curBatchCount, batchNotifications.size());
//                        } catch (Exception e) {
//                            logger.error("批次 {} 索引失败", curBatchCount, e);
//                            XxlJobHelper.log("批次 {} 索引失败: {}", curBatchCount, e.getMessage());
//                        }
//                    });
//
//                    // 等待当前批次完成，设置超时
//                    try {
//                        indexFuture.get(timeoutSeconds, TimeUnit.SECONDS);
//                        processedCount += batchNotifications.size();
//                        XxlJobHelper.log("批次 {} 处理完成，已处理: {}/{}",
//                                batchCount, processedCount, totalUnindexed);
//                    } catch (Exception e) {
//                        logger.error("批次 {} 处理超时或失败", batchCount, e);
//                        XxlJobHelper.log("批次 {} 处理超时或失败: {}", batchCount, e.getMessage());
//                    }
//
//                    // 添加小延迟，避免对ES造成过大压力
//                    Thread.sleep(1000);
//
//                } catch (Exception e) {
//                    logger.error("处理批次 {} 时发生错误", batchCount, e);
//                    XxlJobHelper.log("处理批次 {} 时发生错误: {}", batchCount, e.getMessage());
//                }
//            }
//
//            XxlJobHelper.log("通知ES索引定时任务执行完成，共处理 {} 条通知", processedCount);
//
//        } catch (Exception e) {
//            logger.error("通知ES索引定时任务执行失败", e);
//            XxlJobHelper.log("通知ES索引定时任务执行失败: {}", e.getMessage());
//            throw e;
//        }
//    }
//
//    /**
//     * 手动触发ES索引重建
//     * @param indexName  ES索引名称
//     * @param className  ES索引对应的类名（如 Notification）
//     */
//    @XxlJob("notificationEsRebuildHandler")
//    public void handleNotificationEsRebuild(String indexName, String className) {
//        try {
//            XxlJobHelper.log("开始执行通知ES索引重建任务");
//
//            // 重建通知的ES索引
//            notificationService.rebuildIndex(indexName, className);
//
//            XxlJobHelper.log("通知ES索引重建任务执行完成");
//
//        } catch (Exception e) {
//            logger.error("通知ES索引重建任务执行失败", e);
//            XxlJobHelper.log("通知ES索引重建任务执行失败: {}", e.getMessage());
//            throw e;
//        }
//    }
//
//    /**
//     * 检查ES索引状态
//     */
//    @XxlJob("notificationEsStatusCheckHandler")
//    public void handleNotificationEsStatusCheck(String indexName) {
//        try {
//            XxlJobHelper.log("开始检查通知ES索引状态");
//
//            // 检查索引是否存在
//            boolean indexExists = notificationService.validateIndexExists(indexName);
//            XxlJobHelper.log("ES索引存在状态: {}", indexExists);
//
//            // 统计未索引的通知数量
//            int unindexedCount = notificationService.countUnindexedNotifications();
//            XxlJobHelper.log("未索引的通知数量: {}", unindexedCount);
//
//            // 统计已索引的通知数量
//            int indexedCount = notificationService.selectIndexedNotifications().size();
//            XxlJobHelper.log("已索引的通知数量: {}", indexedCount);
//
//            // 统计总通知数量
//            int totalCount = notificationService.countAll();
//            XxlJobHelper.log("总通知数量: {}", totalCount);
//
//            // 计算索引覆盖率
//            double coverage = totalCount > 0 ? (double) indexedCount / totalCount * 100 : 0;
//            XxlJobHelper.log("ES索引覆盖率: {:.2f}%", coverage);
//
//            XxlJobHelper.log("通知ES索引状态检查完成");
//
//        } catch (Exception e) {
//            logger.error("通知ES索引状态检查失败", e);
//            XxlJobHelper.log("通知ES索引状态检查失败: {}", e.getMessage());
//            throw e;
//        }
//    }
}
