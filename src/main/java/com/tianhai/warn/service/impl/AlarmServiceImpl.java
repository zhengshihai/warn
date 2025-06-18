package com.tianhai.warn.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.tianhai.warn.constants.AlarmConstants;
import com.tianhai.warn.dto.CancelAlarmDTO;
import com.tianhai.warn.dto.LocationUpdateDTO;
import com.tianhai.warn.dto.OneClickAlarmDTO;
import com.tianhai.warn.enums.AlarmStatus;
import com.tianhai.warn.enums.ResultCode;
import com.tianhai.warn.exception.BusinessException;
import com.tianhai.warn.exception.SystemException;
import com.tianhai.warn.mapper.StudentMapper;
import com.tianhai.warn.model.AlarmRecord;
import com.tianhai.warn.mq.AlarmContext;
import com.tianhai.warn.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

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
    private StudentMapper studentMapper;

//    @Value("${amap.api.url}")
//    private String amapApiUrl; //高德地图API基础URL
//    @Autowired
//    private RestTemplate restTemplate;

    //处理一键报警
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
        smsService.sendTriggerOneClickAlarmSms(
                oneClickAlarmDTO.getStudentNo(),
                oneClickAlarmDTO.getAlarmLevel(),
                new Date());

        // 5. 然后建立WebSocket连接
        webSocketService.establishConnection(alarmContext);
    }

    // 取消一键报警
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOneClickAlarm(CancelAlarmDTO cancelAlarmDTO) {
        String alarmNo = cancelAlarmDTO.getAlarmNo();
        String studentNo = cancelAlarmDTO.getStudentNo();
        String alarmStatusKey = AlarmConstants.REDIS_KEY_ALARM_STATUS + alarmNo;

        //  验证报警记录
        AlarmRecord alarmRecord = alarmRecordService.selectByAlarmNo(alarmNo);
        if (alarmRecord == null) {
            logger.error("该报警记录不存在， cancelAlarmDTO: {}", cancelAlarmDTO);
            throw new BusinessException(ResultCode.PARAMETER_ERROR);
        }

        //  验证权限
        if (!studentNo.equals(alarmRecord.getStudentNo())) {
            logger.error("无权取消他人的的报警");
            throw new BusinessException(ResultCode.PARAMETER_ERROR);
        }

        //  验证状态
        boolean isClosedOrProcessed = alarmRecord.getAlarmStatus() == AlarmStatus.CLOSED.getCode()
                || alarmRecord.getAlarmStatus() == AlarmStatus.PROCESSED.getCode();
        if (isClosedOrProcessed) {
            logger.info("mysql中这条报警记录已关闭或已处理");
        }


        //  删除redis中一键报警状态 //todo 这里应该是更改状态还是删除呢
        AlarmStatus cachedAlarmStatus = (AlarmStatus) redisTemplate.opsForValue().get(alarmStatusKey);
        if (cachedAlarmStatus == AlarmStatus.CLOSED || cachedAlarmStatus == AlarmStatus.PROCESSED) {
            logger.debug("该一键报警已被关闭或已经处理完成");
        } else {
            logger.debug("成功取消缓存中的一键报警，alarmNo: {}", alarmNo);
            redisTemplate.delete(alarmStatusKey);
        }

        //  更新mysql中的一键报警状态
        alarmRecord.setAlarmStatus(AlarmStatus.CLOSED.getCode());
        alarmRecord.setUpdatedAt(new Date());
        int updateResult = alarmRecordService.update(alarmRecord);
        if (updateResult <= 0) {
            logger.error("更新mysql数据库的一键报警状态发生异常，alarmRecord:{}", alarmRecord);
            throw new SystemException(ResultCode.ERROR);
        }

        //  发送取消一键报警状态
        try {
            smsService.sendCancelOneClickAlarmSms(cancelAlarmDTO.getStudentNo(),
                    cancelAlarmDTO.getName(), new Date());
        } catch (Exception e) {
            logger.error("发送取消一键报警短信失败：cancelAlarmDTO:{}", cancelAlarmDTO, e);
        }

        //  清理WebSocket
        webSocketService.closeConnection(alarmNo);

        // todo 将redis中对应的位置信息持久化到mysql

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

    // 检查一键报警频率 //todo 报警处理后需要删除redis报警频率信息
    private void checkAlarmPermission(String studentNo) {
        // 检查报警频率限制
        String rateLimitKey = AlarmConstants.REDIS_KEY_ALARM_RATE_LIMIT + studentNo;
        Long count = redisTemplate.opsForValue().increment(rateLimitKey);
        if (count != null && count == 1) {
            redisTemplate.expire(rateLimitKey,
                    AlarmConstants.REDIS_EXPIRE_ALARM_RATE_LIMIT, TimeUnit.SECONDS);
            return;
        }

        // 判断在规定时间内报警次数是否超过阈值 //todo 优化：这里可以从mysql获取配置并同步到redis
        if (count != null && count >= AlarmConstants.ALARM_ONE_CLICK_RATE_LIMIT) {
            logger.error("一键报警频率过于频繁");
            throw new BusinessException(ResultCode.ALARM_ONE_CLICK_RATE_TOO_HIGH);
        }

    }

    // 检查并更新报警状态
    private void checkAndUpdateAlarmStatus(OneClickAlarmDTO oneClickAlarmDTO) {
        String alarmNo = oneClickAlarmDTO.getAlarmNo();
        String alarmStatusKey = AlarmConstants.REDIS_KEY_ALARM_STATUS + alarmNo;
        AlarmStatus alarmStatus = (AlarmStatus) redisTemplate.opsForValue().get(alarmStatusKey);

        // 优先使用缓存判断
        if (alarmStatus != null) {
            if (alarmStatus == AlarmStatus.PROCESSED || alarmStatus == AlarmStatus.CLOSED) {
                logger.error("该报警已关闭或已经处理，alarmNo: {}", alarmNo);
                throw new BusinessException(ResultCode.ALARM_ENDED);
            }

            if (alarmStatus == AlarmStatus.PROCESSING) {
                logger.info("该报警正在处理中，alarmNo: {}", alarmNo);
                throw new BusinessException(ResultCode.ALARM_PROCESSING);
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
                    AlarmStatus.PROCESSING,
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
                    newAlarmRecord.setAlarmStatus(AlarmStatus.PROCESSING.getCode());
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

//    //获得前端地图渲染所需的信息
//    @Override
//    public Map<String, Object> getMapInfo(LocationDTO locationDTO, String amapKey) {
//        try {
//            // 构建请求URL
//            String url = String.format("%s/regeocode?key=%s&location=%f,%f&extensions=all",
//                    amapApiUrl,
//                    amapKey,
//                    locationDTO.getLatitude(),
//                    locationDTO.getLongitude());
//
//            // 远程调用（非响应式）高德地图API
//            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
//
//            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
//                Map<String, Object> result = response.getBody();
//
//                // 检查API调用是否成功
//                if (!"1".equals(result.get("status"))) {
//                    logger.error("高德地图API调用失败: {}", result.get("info"));
//                    throw new BusinessException(ResultCode.ALARM_AMAP_API_ERROR);
//                }
//
//                // 提取需要的信息
//                Map<String, Object> regeoCode = (Map<String, Object>) result.get("regeocode");
//
//                // 构建返回结果
//                Map<String, Object> mapInfo = new HashMap<>();
//                // 基础位置信息
//                mapInfo.put("longitude", locationDTO.getLongitude());
//                mapInfo.put("latitude", locationDTO.getLatitude());
//                mapInfo.put("address", regeoCode.get("formatted_address"));
//
//                // 地图渲染所需信息
//                mapInfo.put("zoom", 16); // 默认缩放级别
//                mapInfo.put("mapStyle", "normal"); // 默认地图样式
//                mapInfo.put("markerIcon", "https://webapi.amap.com/theme/v1.3/markers/n/mark_b.png"); // 默认标记图标
//
//                // 周边POI信息
//                List<Map<String, Object>> pois = (List<Map<String, Object>>) regeoCode.get("pois");
//                if (pois != null && !pois.isEmpty()) {
//                    mapInfo.put("nearbyPois", pois.stream()
//                            .map(poi -> {
//                                Map<String, Object> poiInfo = new HashMap<>();
//                                poiInfo.put("name", poi.get("name"));
//                                poiInfo.put("type", poi.get("type"));
//                                poiInfo.put("distance", poi.get("distance"));
//                                poiInfo.put("location", poi.get("location"));  // 添加POI位置信息
//                                return poiInfo;
//                            }).collect(Collectors.toList()));
//                }
//
//                return mapInfo;
//            } else {
//                logger.error("高德地图API调用失败，响应状态码: {}", response.getStatusCode());
//                throw new BusinessException(ResultCode.ALARM_AMAP_API_ERROR);
//            }
//        } catch (Exception e) {
//            logger.error("获取地图信息失败", e);
//            throw new SystemException(ResultCode.ERROR);
//        }
//    }
}
