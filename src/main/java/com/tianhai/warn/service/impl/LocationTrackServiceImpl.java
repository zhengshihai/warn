package com.tianhai.warn.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.alibaba.fastjson.JSON;
import com.tianhai.warn.constants.AlarmConstants;
import com.tianhai.warn.dto.LocationUpdateDTO;
import com.tianhai.warn.enums.AlarmStatus;
import com.tianhai.warn.enums.ResultCode;
import com.tianhai.warn.exception.BusinessException;
import com.tianhai.warn.exception.SystemException;
import com.tianhai.warn.mapper.LocationTrackMapper;
import com.tianhai.warn.model.LocationTrack;
import com.tianhai.warn.mq.RocketMQMessageSender;
import com.tianhai.warn.service.LocationTrackService;

import com.tianhai.warn.vo.LatestLocationVO;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 位置轨迹服务实现类
 * 主要功能：
 * 1. 接收并处理位置更新消息
 * 2. 验证位置数据的有效性
 * 3. 使用Redis GEO存储和查询位置信息
 * 4. 计算移动速度并过滤异常数据
 * 5. 维护报警状态
 */
@Service
public class LocationTrackServiceImpl implements LocationTrackService {

    private static final Logger logger = LoggerFactory.getLogger(LocationTrackServiceImpl.class);

    /**
     * 最大合理速度阈值（单位：米/秒）
     * 超过此速度的位置数据可能为异常数据
     */
    private static final double MAX_REASONABLE_SPEED = 300.0; // 约1080km/h

    /**
     * 最小合理速度阈值（单位：米/秒）
     * 低于此速度的位置数据可能为静止状态或历史数据
     */
    private static final double MIN_REASONABLE_SPEED = 0;

    private static final String COMPENSATE_TOPIC = "alarm-location-compensate";

    private static final String COMPENSATE_REDIS_LIST = "location:compensate:redis";

    private static final Integer MAX_RETRY_TIMES = 3;

    @Value("video.file.storage.path") // todo 区分开发环境和线上环境
    private String videoFilePath;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private LocationTrackMapper locationTrackMapper;

    @Autowired
    private RocketMQMessageSender rocketMQMessageSender;

    @Override
    public void handleLocationMessage(String locationMessage) {
        try {
            logger.info("开始消费消息: {}", locationMessage);
            LocationUpdateDTO locationUpdateDTO = JSON.parseObject(locationMessage, LocationUpdateDTO.class);
            logger.info("解析位置信息: {}", locationUpdateDTO);

            // 验证位置信息
            if (!isValidLocation(locationUpdateDTO)) {
                logger.error("位置信息无效，直接丢弃消息：{}", locationUpdateDTO);
                return;
            }

            // 更新Redis位置信息
            updateRealTimeLocation(locationUpdateDTO);
            logger.info("位置信息更新完成: {}", locationUpdateDTO);

        } catch (Exception e) {
            logger.error("处理位置消息失败：{}, 错误信息：{}",
                    locationMessage, e.getMessage(), e);
            throw new RuntimeException("处理位置消息失败", e);
        }
    }

    /**
     * 验证位置信息是否有效
     * 验证逻辑：
     * 1. 获取相邻时间点的位置信息
     * 2. 计算移动速度
     * 3. 根据速度判断位置是否有效
     * 
     * @param locationUpdateDTO 位置更新DTO
     * @return 位置是否有效
     */
    private boolean isValidLocation(LocationUpdateDTO locationUpdateDTO) {
        String alarmNo = locationUpdateDTO.getAlarmNo();
        String geoKey = AlarmConstants.REDIS_KEYS_ALARM_GEO + alarmNo;
        String trackTimeKey = AlarmConstants.REDIS_KEY_ALARM_TRACK_TIME + alarmNo;

        // 获取时间戳相邻的两个点
        Set<String> adjacentPoints = getAdjacentPoints(trackTimeKey, locationUpdateDTO.getLocationTime());
        if (adjacentPoints.size() < 2) {
            return true; // 如果相邻点不足两个，则认为是有效位置
        }

        // 计算速度
        double speed = calculateSpeed(geoKey, trackTimeKey, adjacentPoints);

        // 速度判断逻辑：
        // 1. 如果速度超过最大合理速度，可能是异常数据
        if (speed > MAX_REASONABLE_SPEED) {
            logger.warn("检测到超高速移动，可能为异常数据: speed={}m/s, location={}",
                    speed, locationUpdateDTO);
            return false;
        }

        // 2. 如果速度在合理范围内，保留位置信息
        if (speed >= MIN_REASONABLE_SPEED) {
            logger.info("检测到合理速度移动，保留位置信息: speed={}m/s, location={}",
                    speed, locationUpdateDTO);
            return true;
        }

        // 3. 如果速度小于最小合理速度，可能是历史数据或静止状态
        logger.warn("检测到低速移动，丢弃位置信息: speed={}m/s, location={}",
                speed, locationUpdateDTO);
        return false;
    }

    /**
     * 获取时间戳相邻的两个点（member)
     * 使用Redis ZSet的rangeByScore功能获取时间上相邻的点
     * 
     * @param trackTimeKey Redis键
     * @param timestamp    目标时间戳
     * @return 相邻的两个点的member集合
     */
    private Set<String> getAdjacentPoints(String trackTimeKey, Date timestamp) {
        // 将Date转为毫秒时间戳
        long timeStampMillis = timestamp.getTime();

        // 获取小于当前时间戳的最大点
        Set<Object> lowerPoints = redisTemplate.opsForZSet().reverseRangeByScore(
                trackTimeKey, 0, timeStampMillis, 0, 1);

        // 获取大于当前时间戳的最小点
        Set<Object> higherPoints = redisTemplate.opsForZSet().rangeByScore(
                trackTimeKey, timeStampMillis, Double.MAX_VALUE, 0, 1);

        Set<String> result = new HashSet<>();
        if (!lowerPoints.isEmpty()) {
            result.add(lowerPoints.iterator().next().toString());
        }
        if (!higherPoints.isEmpty()) {
            result.add(higherPoints.iterator().next().toString());
        }
        return result;
    }

    /**
     * 计算两点之间的距离
     * 使用Haversine公式计算球面距离
     * 
     * @param point1 第一个点
     * @param point2 第二个点
     * @return 两点之间的距离（米）
     */
    // todo 此处需改用： redisTemplate.opsForGeo().distance(key, point1, point2,
    // Metrics.METERS);
    private double calculateDistance(Point point1, Point point2) {
        // 使用Haversine公式计算球面距离
        double R = 6371000; // 地球半径（米）
        double lat1 = Math.toRadians(point1.getY());
        double lat2 = Math.toRadians(point2.getY());
        double lon1 = Math.toRadians(point1.getX());
        double lon2 = Math.toRadians(point2.getX());

        double dLat = lat2 - lat1;
        double dLon = lon2 - lon1;

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(lat1) * Math.cos(lat2) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }

    /**
     * 计算移动速度
     * 基于两点之间的距离和时间差计算平均速度
     * 
     * @param geoKey         Redis GEO键
     * @param trackTimeKey   Redis时间戳键
     * @param adjacentPoints 相邻的两个点
     * @return 移动速度（米/秒）
     */
    private double calculateSpeed(String geoKey, String trackTimeKey, Set<String> adjacentPoints) {
        if (adjacentPoints.size() < 2) {
            return 0.0;
        }

        List<String> pointList = new ArrayList<>(adjacentPoints);
        String pointA = pointList.get(0);
        String pointC = pointList.get(1);

        // 获取两个点的位置信息
        List<Point> points = redisTemplate.opsForGeo().position(geoKey, pointA, pointC);
        if (points.size() < 2) {
            return 0.0;
        }

        // 计算距离（米）
        double distance = calculateDistance(points.get(0), points.get(1));

        // 获取两个点对应的时间戳
        Double scoreA = redisTemplate.opsForZSet().score(trackTimeKey, pointA);
        Double scoreC = redisTemplate.opsForZSet().score(trackTimeKey, pointC);
        if (scoreA == null || scoreC == null) {
            return 0.0;
        }

        // 计算时间差（秒）
        double timeDiff = Math.abs(scoreC - scoreA) / 1000.0; // 转换为秒
        if (timeDiff == 0) {
            return 0.0;
        }

        // 计算速度（米/秒）
        return distance / timeDiff;
    }

    /**
     * 更新实时位置
     * 处理流程：
     * 1. 验证位置数据
     * 2. 检测位置异常
     * 3. 更新Redis缓存
     * 4. 更新报警状态
     * 5. 异步保存位置轨迹
     * 
     * @param locationUpdateDTO 位置更新DTO
     */
    @Override
    public void updateRealTimeLocation(LocationUpdateDTO locationUpdateDTO) {
        // 验证位置数据
        validateLocationData(locationUpdateDTO);

        // 检测位置异常
        checkLocationAbnormal(locationUpdateDTO);

        // 更新Redis位置信息缓存
        updateCacheLocationWithGeo(locationUpdateDTO);

        // 更新Redis报警状态缓存
        updateCacheAlarmStatus(locationUpdateDTO, AlarmStatus.PROCESSING);

        // 异步保存位置轨迹到mysql 1引入"写入状态确认"机制 2 消息队列异步吸入+事件触发后处理
        asyncSaveLocationWithCompensate(locationUpdateDTO);
    }

    /**
     * 使用Redis GEO更新位置
     * 使用事务确保位置信息和时间戳的原子性更新
     * 
     * @param locationUpdateDTO 位置更新DTO
     */
    public void updateCacheLocationWithGeo(LocationUpdateDTO locationUpdateDTO) {
        // 更新位置信息缓存
        String alarmNo = locationUpdateDTO.getAlarmNo();
        String pointId = UUID.randomUUID().toString();
        String geoKey = AlarmConstants.REDIS_KEYS_ALARM_GEO + alarmNo;
        String trackTimeKey = AlarmConstants.REDIS_KEY_ALARM_TRACK_TIME + alarmNo;

        logger.info("开始更新Redis缓存, alarmNo: {}, geoKey: {}, trackTimeKey: {}",
                alarmNo, geoKey, trackTimeKey);

        // 使用事务确保原子性
        redisTemplate.execute(new SessionCallback<>() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                operations.multi();

                // 1. 更新GEO数据
                operations.opsForGeo().add(
                        geoKey,
                        new RedisGeoCommands.GeoLocation<>(
                                pointId,
                                new Point(locationUpdateDTO.getLongitude(), locationUpdateDTO.getLatitude())));

                // 2. 更新ZSet数据
                operations.opsForZSet().add(
                        trackTimeKey,
                        pointId,
                        locationUpdateDTO.getLocationTime().getTime());

                List<Object> results = operations.exec();
                logger.info("Redis事务执行结果: {}", results);
                return results;
            }
        });

        // 验证数据是否写入成功
        // 使用 ZSet 的 size 来验证，因为 GEO 数据实际上是存储在 ZSet 中的
        Long zSetSize = redisTemplate.opsForZSet().size(trackTimeKey);
        // 验证新添加的点是否存在
        Boolean pointExists = redisTemplate.opsForZSet().score(trackTimeKey, pointId) != null;
        logger.info("Redis数据验证 - trackTimeKey: {}, size: {}, pointId: {}, exists: {}",
                trackTimeKey, zSetSize, pointId, pointExists);
    }

    /**
     * 更新报警状态缓存
     * 
     * @param locationUpdateDTO 位置更新DTO
     */
    private void updateCacheAlarmStatus(LocationUpdateDTO locationUpdateDTO, AlarmStatus alarmStatus) {
        String alarmNo = locationUpdateDTO.getAlarmNo();

        // 更新报警状态为处理中
        redisTemplate.opsForValue().set(
                AlarmConstants.REDIS_KEY_ALARM_STATUS + alarmNo,
                alarmStatus,
                AlarmConstants.REDIS_EXPIRE_ALARM_STATUS,
                TimeUnit.SECONDS);

        logger.info("更新报警状态缓存成功: alarmNo={}, status={}",
                alarmNo, AlarmStatus.PROCESSING);
    }

    /**
     * 检测位置异常
     * TODO: 需要根据具体业务场景实现异常检测逻辑
     * 
     * @param locationDTO 位置更新DTO
     */
    private void checkLocationAbnormal(LocationUpdateDTO locationDTO) {
        // TODO: 实现位置异常检测逻辑
    }

    /**
     * 保存位置轨迹到数据库
     * 注意：此方法应该在位置信息验证通过后调用
     * 
     * @param locationUpdateDTO 位置更新DTO
     * @return 插入记录数
     */
    @Override
    public Integer saveLocationTrack(LocationUpdateDTO locationUpdateDTO) {
        // 处理时区转换
        if (locationUpdateDTO.getLocationTime() != null) {
            // 检查时间格式是否包含 'Z'（UTC时间标识）
            String timeStr = locationUpdateDTO.getLocationTime().toString();
            if (timeStr.endsWith("Z")) {
                // 如果是UTC时间，转换为本地时间
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(locationUpdateDTO.getLocationTime());
                calendar.add(Calendar.HOUR_OF_DAY, 8); // 添加8小时
                locationUpdateDTO.setLocationTime(calendar.getTime());
            }
            // 如果是本地时间，不做转换
        }

        try {
            return saveOrUpdateTrackWithMerge(locationUpdateDTO);
        } catch (Exception e) {
            logger.error("保存位置轨迹到数据库失败: {}", locationUpdateDTO, e);
            throw new BusinessException("保存位置轨迹失败");
        }

        // saveOrUpdateTrackWithMerge(locationUpdateDTO);
        // throw new RuntimeException("手动抛出异常，验证定位信息能否进入mq补偿队列");
    }

    /**
     * 验证位置数据
     * 验证纬度、经度范围和精确度
     * 
     * @param locationUpdateDTO 位置更新DTO
     * @throws BusinessException 当位置数据无效时抛出异常
     */
    private void validateLocationData(LocationUpdateDTO locationUpdateDTO) {
        Double latitude = locationUpdateDTO.getLatitude();
        Double longitude = locationUpdateDTO.getLongitude();
        Double accuracy = locationUpdateDTO.getLocationAccuracy();

        // 验证纬度范围 [-90, 90]
        if (latitude == null || latitude < -90 || latitude > 90) {
            throw new BusinessException("纬度数据异常");
        }

        // 验证经度范围 [-180, 180]
        if (longitude == null || longitude < -180 || longitude > 180) {
            throw new BusinessException("经度数据异常");
        }

        // 验证精确度（允许最大精度 1000 米）
        if (accuracy != null && accuracy > 1000) {
            logger.warn("位置精确度过低: {}", accuracy);
        }
    }

    // 异步保存定位信息到mysql 并使用 重试 + RocketMQ + Redis List 作为缓存一致性的双重兜底策略
    public void asyncSaveLocationWithCompensate(LocationUpdateDTO locationUpdateDTO) {
        CompletableFuture.runAsync(() -> {
            int retry = 0;
            boolean success = false;
            while (retry < MAX_RETRY_TIMES && !success) {
                try {
                    // 保存定位信息到mysql
                    saveLocationTrack(locationUpdateDTO);
                    success = true;
                } catch (Exception e) {
                    retry++;
                    logger.error("异步保存位置轨迹失败：{}，重试次数：{}",
                            locationUpdateDTO, retry, e);
                }
            }

            // 使用RocketMQ作为补偿
            if (!success) {
                try {
                    rocketMQMessageSender.sendMessage(
                            COMPENSATE_TOPIC,
                            "compensate",
                            locationUpdateDTO.getAlarmNo(),
                            JSON.toJSONString(locationUpdateDTO));
                } catch (Exception mqEx) {
                    logger.error("补偿消息发送到RocketMQ失败，写入Redis List兜底：{}",
                            locationUpdateDTO, mqEx);
                    pushToCompensateQueue(locationUpdateDTO);
                }
            }
        });
    }

    public void pushToCompensateQueue(LocationUpdateDTO locationUpdateDTO) {
        Map<String, Object> map = new HashMap<>(JSON.parseObject(JSON.toJSONString(locationUpdateDTO)));
        map.put("retryCount", 0);
        redisTemplate.opsForList().rightPush("location:compensate:redis", map);
    }

    /**
     * 获取历史轨迹
     * 
     * @param alarmNo   报警编号
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 位置轨迹列表
     */
    @Override
    public List<LocationTrack> getLocationHistory(String alarmNo, Date startTime, Date endTime) {
        return locationTrackMapper.selectByTimeRange(alarmNo, startTime, endTime);
    }

    /**
     * 获取当前位置
     * 
     * @param alarmNo 报警编号
     * @return 当前位置点，如果不存在则返回null
     */
    @Override
    public Point getCurrentLocation(String alarmNo) {
        String geoKey = AlarmConstants.REDIS_KEYS_ALARM_GEO + alarmNo;
        List<Point> positions = redisTemplate.opsForGeo().position(geoKey, alarmNo);
        return positions.isEmpty() ? null : positions.get(0);
    }

    @Override
    public Integer insert(LocationTrack track) {
        if (track.getLatitude() == null || track.getLongitude() == null
                || track.getAlarmNo() == null) {
            logger.error("LocationTrack信息不完整，LocationTrack:{}", track);
            throw new BusinessException(ResultCode.LOCATION_TRACK_SAVE_FAILED);
        }

        Integer affectedRow = locationTrackMapper.insert(track);
        if (affectedRow < 0) {
            throw new SystemException(ResultCode.LOCATION_TRACK_SAVE_FAILED);
        }

        return affectedRow;
    }

    @Override
    public List<LocationTrack> selectByTimeRange(String alarmNo, Date startTime, Date endTime) {
        if (alarmNo == null || startTime == null || endTime == null) {
            logger.error("查询参数不完整，alarmNo: {}, startTime: {}, endTime: {}", alarmNo, startTime, endTime);
            throw new BusinessException(ResultCode.PARAMETER_ERROR);
        }

        if (startTime.after(endTime)) {
            logger.error("时间范围错误，startTime:{}, endTIme:{}", startTime, endTime);
            throw new BusinessException(ResultCode.PARAMETER_ERROR);
        }

        return locationTrackMapper.selectByAlarmNoAndTimeRange(alarmNo, startTime, endTime);
    }

    @Override
    public LocationTrack selectById(Long id) {
        return locationTrackMapper.selectById(id);
    }

    /**
     * 保存或合并mysql的轨迹点（静止合并）
     * 
     * @param locationUpdateDTO 新轨迹点
     * @return 0-丢弃该无效点 1=插入新点，2=合并更新
     */
    public int saveOrUpdateTrackWithMerge(LocationUpdateDTO locationUpdateDTO) {
        String alarmNo = locationUpdateDTO.getAlarmNo();
        if (alarmNo == null)
            throw new BusinessException("alarmNo不能为空");

        Integer changeUnit = locationUpdateDTO.getChangeUnit();
        if (changeUnit != null && changeUnit <= 0) {
            logger.error("该位置信息的changeUnit不合法，locationUpdateDTO: {}", locationUpdateDTO);
            return 0;
        }

        LocationTrack lastTrack = locationTrackMapper.selectLastByAlarmNo(alarmNo);
        Date now = locationUpdateDTO.getLocationTime();
        if (shouldBeMerged(lastTrack, locationUpdateDTO)) {
            // 合并静止段
            lastTrack.setEndLocationTime(now);
            lastTrack.setChangeUnit(locationUpdateDTO.getChangeUnit());
            locationTrackMapper.update(lastTrack);
            return 2;

        } else {
            // 新增轨迹点
            LocationTrack track = new LocationTrack();
            BeanUtil.copyProperties(locationUpdateDTO, track);
            track.setFirstLocationTime(now);
            track.setEndLocationTime(now);
            track.setCreatedAt(new Date());
            locationTrackMapper.insert(track);
            return 1;
        }
    }

    private boolean shouldBeMerged(LocationTrack lastTrack, LocationUpdateDTO locationUpdateDTO) {
        return lastTrack != null
                && lastTrack.getLatitude() != null && lastTrack.getLongitude() != null
                && lastTrack.getLatitude().equals(locationUpdateDTO.getLatitude())
                && lastTrack.getLongitude().equals(locationUpdateDTO.getLongitude());
    }

    @Override
    public void expireLocationCacheByAlarmNo(String alarmNo) {
        String geoKey = AlarmConstants.REDIS_KEYS_ALARM_GEO + alarmNo;
        String trackTimeKey = AlarmConstants.REDIS_KEY_ALARM_TRACK_TIME + alarmNo;

        // 设置30分钟后过期
        redisTemplate.expire(geoKey, 30, TimeUnit.MINUTES);
        redisTemplate.expire(trackTimeKey, 30, TimeUnit.MINUTES);

        logger.info("设置 alarmNo= {} 的GEO和ZSet缓存30分钟后过期", alarmNo);
    }

    @Override
    public List<LocationTrack> selectWithLimitByAlarmNo(String alarmNo, Integer amount) {
        if (StringUtils.isBlank(alarmNo)) {
            logger.error("alarmNo为空");
            throw new BusinessException(ResultCode.PARAMETER_ERROR);
        }

        if (amount <= 0) {
            logger.error("amount不合法");
        }

        return locationTrackMapper.selectWithLimitByAlarmNo(alarmNo, amount);
    }

    @Override
    public LatestLocationVO selectLastByAlarmNo(String alarmNo) {
        LatestLocationVO latestLocationVO = null;

        // 先从缓存中读取经纬度信息
        Point latestPoint = searchFromCache(alarmNo);
        if (latestPoint != null) {
            latestLocationVO = LatestLocationVO.builder()
                    .latitude(latestPoint.getY())
                    .longitude(latestPoint.getX())
                    .build();
        }

        // 从数据库中获取经纬度信息 + 定位精度
        LocationTrack locationTrackFromDB = locationTrackMapper.selectLastByAlarmNo(alarmNo);

        // 综合缓存的结果和数据库的位置信息
        if (locationTrackFromDB == null) {
            // 数据库没有数据，直接返回缓存数据
            return latestLocationVO;
        }

        Double locationAccuracy = locationTrackFromDB.getLocationAccuracy();

        if (latestLocationVO != null) {
            // 缓存有数据，补充精度信息
            if (locationAccuracy != null) {
                latestLocationVO.setLocationAccuracy(locationAccuracy);
            }
            return latestLocationVO;
        } else {
            // 缓存没有数据，使用数据库数据
            latestLocationVO = LatestLocationVO.builder()
                    .longitude(locationTrackFromDB.getLongitude())
                    .latitude(locationTrackFromDB.getLatitude())
                    .locationAccuracy(locationAccuracy)
                    .build();
            return latestLocationVO;
        }
    }

    private Point searchFromCache(String alarmNo) {
        String geoKey = AlarmConstants.REDIS_KEYS_ALARM_GEO + alarmNo;
        String trackTimeKey = AlarmConstants.REDIS_KEY_ALARM_TRACK_TIME + alarmNo;

        // 获取ZSet中最新的pointId(score最大的）
        Set<ZSetOperations.TypedTuple<Object>> latestPointSet = redisTemplate.opsForZSet()
                .reverseRangeWithScores(trackTimeKey, 0, 0);

        if (latestPointSet == null || latestPointSet.isEmpty()) {
            logger.warn("缓存中未找到最新位置信息，alarmNo: {}", alarmNo);
            return null;
        }

        String latestPointId = (String) latestPointSet.iterator().next().getValue();
        Double latestScore = latestPointSet.iterator().next().getScore();

        List<Point> pointList = redisTemplate.opsForGeo().position(geoKey, latestPointId);

        if (pointList == null || pointList.isEmpty() || pointList.get(0) == null) {
            logger.warn("缓存的GEO中位找到对应坐标，alarmNo:{}, pointId:{}", alarmNo, latestPointId);
            return null;
        }

        Point latestPoint = pointList.get(0);
        assert latestScore != null;
        logger.info("从缓存中成功获取到最新为止：alarmNo:{}, pointId={}, time={}, lat={}, lon={}",
                alarmNo, latestPointId, new Date(latestScore.longValue()),
                latestPoint.getY(), latestPoint.getX());

        return latestPoint;
    }
}
