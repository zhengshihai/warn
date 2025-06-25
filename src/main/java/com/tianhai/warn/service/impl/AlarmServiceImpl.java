package com.tianhai.warn.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.tianhai.warn.constants.AlarmConstants;
import com.tianhai.warn.constants.Constants;
import com.tianhai.warn.dto.CancelAlarmDTO;
import com.tianhai.warn.dto.LocationUpdateDTO;
import com.tianhai.warn.dto.OneClickAlarmDTO;
import com.tianhai.warn.enums.AlarmStatus;
import com.tianhai.warn.enums.ResultCode;
import com.tianhai.warn.exception.BusinessException;
import com.tianhai.warn.exception.SystemException;
import com.tianhai.warn.mapper.AlarmRecordMapper;
import com.tianhai.warn.mapper.StudentMapper;
import com.tianhai.warn.model.AlarmRecord;
import com.tianhai.warn.model.DormitoryManager;
import com.tianhai.warn.model.Student;
import com.tianhai.warn.mq.AlarmContext;
import com.tianhai.warn.query.StudentQuery;
import com.tianhai.warn.service.*;
import com.tianhai.warn.vo.StudentAlarmContactsVO;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.resource.ResourceUrlProvider;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    @Autowired
    private SysUserClassService sysUserClassService;

    @Autowired
    private StudentService studentService;

    @Autowired
    private DormitoryManagerService dormitoryManagerService;

    @Autowired
    private AlarmRecordMapper alarmRecordMapper;


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


        //  删除redis中一键报警状态 
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

        // 取消报警后，设置位置信息缓存过期
        locationTrackService.expireLocationCacheByAlarmNo(alarmNo);
    }



    /**
     * 获取学生 报警 紧急联系人的基本信息
     * @param helperNo      紧急联系人（宿管，父母）
     * @param role          紧急联系人角色（班级管理员，宿管）
     * @return              基本信息
     */
    @Override
    public List<StudentAlarmContactsVO> searchStudentAlarmContactInfo(String helperNo, String role) {
        // 宿舍管理员管辖范围
        if (Constants.DORMITORY_MANAGER.equalsIgnoreCase(role)) {
            return searchForDormitoryManager(helperNo);
        }

        // 班级管理员管辖范围
        if (Constants.SYSTEM_USER.equalsIgnoreCase(role)) {
            return searchForClassManager(helperNo);
        }

        // todo 其他角色待实现
        return Collections.emptyList();
    }


    private List<StudentAlarmContactsVO> searchForDormitoryManager(String helperNo) {
        // 获取未结束的报警记录
        List<AlarmRecord> alarmRecordList =  alarmRecordMapper.selectNotEndedAlarms();
        if (alarmRecordList.isEmpty()) {
            logger.info("没有未结束的报警记录");
            return Collections.emptyList();
        }

        // 获取和报警记录有关的学号列表
        List<String> studentNoList = alarmRecordList.stream()
                .map(AlarmRecord::getStudentNo)
                .distinct()
                .toList();

        // 查询学号对应的学生信息
        StudentQuery studentQuery = StudentQuery.builder().studentNos(studentNoList).build();
        List<Student> studentList = studentService.searchByStudentQuery(studentQuery);

        // 获取管理的宿舍楼
        DormitoryManager dormitoryManager = dormitoryManagerService.selectByManagerId(helperNo);
        String managedBuilding = dormitoryManager.getBuilding();

        // 过滤掉不在宿舍管理员管理楼栋的学生
        List<Student> matchedStudentList = studentList.stream()
                .filter(student -> {
                    String dormitory = student.getDormitory();

                    return StringUtils.isNotBlank(dormitory)
                            && dormitory.charAt(0) == managedBuilding.charAt(0);
                })
                .toList();

        Map<String, Student> studentMap = matchedStudentList.stream()
                .collect(Collectors.toMap(
                        Student::getStudentNo,
                        Function.identity(),
                        (existing, duplicate) -> existing // 学号重复是保留第一个学生对象
                ));

        // 构造结果
        return alarmRecordList.stream()
                .map(alarmRecord -> {
                    Student student = studentMap.get(alarmRecord.getStudentNo());
                    if (student == null) return null;

                    return StudentAlarmContactsVO.builder()
                            .studentName(student.getName())
                            .studentNo(student.getStudentNo())
                            .alarmNo(alarmRecord.getAlarmNo())
                            .fatherPhone(student.getFatherPhone())
                            .motherPhone(student.getMotherPhone())
                            .build();
                })
                .filter(Objects::nonNull)
                .toList();

    }

    private List<StudentAlarmContactsVO> searchForClassManager(String helperNo) {
        // 获取班级信息
        List<String> classNameList = sysUserClassService.getUserClasses(helperNo);
        if (classNameList == null || classNameList.isEmpty()) {
            logger.warn("该班级管理员没有管理班级，helperNo: {}", helperNo);
            return Collections.emptyList();
        }

        // 获取未结束的报警记录
        List<AlarmRecord> alarmRecordList =  alarmRecordMapper.selectNotEndedAlarms();
        if (alarmRecordList.isEmpty()) {
            logger.info("没有未结束的报警记录");
            return Collections.emptyList();
        }

        // 获取和报警记录有关的学号列表
        List<String> studentNoList = alarmRecordList.stream()
                .map(AlarmRecord::getStudentNo)
                .distinct()
                .toList();

        // 查询学号对应的学生信息
        StudentQuery studentQuery = StudentQuery.builder().studentNos(studentNoList).build();
        List<Student> studentList = studentService.searchByStudentQuery(studentQuery);

        // 过滤掉不在班级管理员管理班级的学生
        Set<String> classNameSet = new HashSet<>(classNameList);
        Map<String, Student> studentMap = studentList.stream()
                .filter(student -> classNameSet.contains(student.getClassName()))
                .collect(Collectors.toMap(
                        Student::getStudentNo,
                        Function.identity(),
                        (existing, duplicate) -> existing // 学号重复是保留第一个学生对象
                ));

        // 构造结果
        return alarmRecordList.stream()
                .map(alarmRecord -> {
                    Student student = studentMap.get(alarmRecord.getStudentNo());
                    if (student == null) return null;

                    return StudentAlarmContactsVO.builder()
                            .studentName(student.getName())
                            .studentNo(student.getStudentNo())
                            .alarmNo(alarmRecord.getAlarmNo())
                            .fatherPhone(student.getFatherPhone())
                            .motherPhone(student.getMotherPhone())
                            .build();
                })
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * 获取所有未结束报警相关的学生信息和报警记录
     * @return Pair<报警记录列表, 学生信息列表>
     */
    private Pair<List<AlarmRecord>, List<Student>> getNotEndedAlarmRecordsAndStudents() {
        List<AlarmRecord> alarmRecordList = alarmRecordMapper.selectNotEndedAlarms();
        if (alarmRecordList.isEmpty()) {
            logger.info("没有未结束的报警记录");
            return Pair.of(Collections.emptyList(), Collections.emptyList());
        }

        List<String> studentNoList = alarmRecordList.stream()
                .map(AlarmRecord::getStudentNo)
                .distinct()
                .toList();

        StudentQuery studentQuery = StudentQuery.builder().studentNos(studentNoList).build();
        List<Student> studentList = studentService.searchByStudentQuery(studentQuery);

        return Pair.of(alarmRecordList, studentList);
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

}
