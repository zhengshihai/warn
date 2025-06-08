package com.tianhai.warn.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.tianhai.warn.constants.AlarmConstants;
import com.tianhai.warn.dto.LocationUpdateDTO;
import com.tianhai.warn.dto.OneClickAlarmDTO;
import com.tianhai.warn.enums.AlarmLevel;
import com.tianhai.warn.enums.AlarmStatus;
import com.tianhai.warn.enums.ResultCode;
import com.tianhai.warn.exception.BusinessException;
import com.tianhai.warn.model.AlarmConfig;
import com.tianhai.warn.model.AlarmRecord;
import com.tianhai.warn.mq.AlarmContext;
import com.tianhai.warn.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
public class AlarmServiceImpl implements AlarmService {

    private static final Logger logger = LoggerFactory.getLogger(AlarmServiceImpl.class);

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private AlarmHandlerConfigService alarmHandlerConfigService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private SmsService smsService;

    @Autowired
    private AlarmProcessRecordService alarmProcessRecordService;

    @Autowired
    private LocationTrackService locationTrackService;

    @Autowired
    private WebSocketService webSocketService;

    @Autowired
    private AlarmRecordService alarmRecordService;

    @Override
    public void processOneClickAlarm(OneClickAlarmDTO oneClickAlarmDTO) {
        // 1 检查报警频率
        checkAlarmPermission(oneClickAlarmDTO.getStudentNo());

        // 2 检查报警状态
        checkAndUpdateAlarmStatus(oneClickAlarmDTO);

        // 3 构建报警上下文
        AlarmContext alarmContext = buiLdAlarmContext(oneClickAlarmDTO);

        // 4. 发送一键报警短信
        // notificationService.sendOneClickAlarmNotification(
        // oneClickAlarmDTO.getStudentNo(), oneClickAlarmDTO.getAlarmLevel());
        smsService.sendOneClickAlarmSms(
                oneClickAlarmDTO.getStudentNo(),
                oneClickAlarmDTO.getAlarmLevel(),
                new Date());

        // 5. 然后建立WebSocket连接
        webSocketService.establishConnection(alarmContext);
    }

    // 构建一键报警上下文
    private AlarmContext buiLdAlarmContext(OneClickAlarmDTO oneClickAlarmDTO) {
        LocationUpdateDTO locationUpdateDTO = new LocationUpdateDTO();
        BeanUtil.copyProperties(oneClickAlarmDTO, locationUpdateDTO);

        AlarmContext alarmContext = new AlarmContext();
        alarmContext.setLocationUpdateDTO(locationUpdateDTO);
        alarmContext.setExtraInfo(new HashMap<>());

        return alarmContext;
    }

    // 检查一键报警频率
    private void checkAlarmPermission(String studentNo) {
        // 检查报警频率限制
        String rateLimitKey = AlarmConstants.REDIS_KEY_ALARM_RATE_LIMIT + studentNo;
        Long count = redisTemplate.opsForValue().increment(rateLimitKey);
        if (count == 1) {
            redisTemplate.expire(rateLimitKey,
                    AlarmConstants.REDIS_EXPIRE_ALARM_RATE_LIMIT, TimeUnit.SECONDS);
        }

        // 获取配置的报警频率限制 todo
        // AlarmConfig config = alarmRecordService.selectByKey("ALARM_RATE_LIMIT");
        // int maxCount = Integer.parseInt(config.getConfigValue());
        //
        // if (count > maxCount) {
        // throw new BusinessException(ResultCode.ALARM_RATE_TOO_HIGH);
        // }
    }

    // 检查并更新报警状态
    private void checkAndUpdateAlarmStatus(OneClickAlarmDTO oneClickAlarmDTO) {
        String alarmNo = oneClickAlarmDTO.getAlarmNo();
        String alarmStatusKey = AlarmConstants.REDIS_KEY_ALARM_STATUS + alarmNo;
        AlarmStatus alarmStatus = (AlarmStatus) redisTemplate.opsForValue().get(alarmStatusKey);

        // 优先使用缓存判断
        if (alarmStatus != null) {
            if (alarmStatus == AlarmStatus.PROCESSED || alarmStatus == AlarmStatus.CLOSED) {
                logger.error("该报警状态已关闭或已经处理，alarmNo: {}", alarmNo);
                throw new BusinessException(ResultCode.ALARM_ENDED);
            }
            return;
        }

        // 缓存无值则查询数据库
        AlarmRecord alarmRecord = alarmRecordService.selectByAlarmNo(alarmNo);

        // 缓存和数据库都为空则同步设置缓存，异步保存数据库
        if (alarmRecord == null) {
            // 同步设置缓存
            redisTemplate.opsForValue().set(
                    AlarmConstants.REDIS_KEY_ALARM_STATUS + alarmNo,
                    AlarmStatus.PENDING,
                    AlarmConstants.REDIS_EXPIRE_ALARM_STATUS,
                    TimeUnit.SECONDS);

            // 异步保存到数据库，不等待结果
            CompletableFuture.runAsync(() -> {
                try {
                    AlarmRecord newAlarmRecord = new AlarmRecord();
                    BeanUtil.copyProperties(oneClickAlarmDTO, newAlarmRecord);

                    Integer alarmLevel = oneClickAlarmDTO.getAlarmLevel().getCode();
                    newAlarmRecord.setAlarmLevel(alarmLevel);

                    newAlarmRecord.setAlarmType(AlarmConstants.ONE_CLICK_ALARM_TYPE);
                    newAlarmRecord.setAlarmStatus(AlarmStatus.PENDING.getCode());
                    newAlarmRecord.setAlarmTime(new Date());
                    newAlarmRecord.setCreatedAt(new Date());
                    newAlarmRecord.setUpdatedAt(new Date());

                    int result = alarmRecordService.insert(newAlarmRecord);
                    if (result <= 0) {
                        logger.error("保存报警记录失败: {}", newAlarmRecord);
                    } else {
                        logger.info("报警记录保存成功: {}", newAlarmRecord);
                    }
                } catch (Exception e) {
                    logger.error("保存报警记录时发生异常", e);
                }
            });
        }
    }

}
