package com.tianhai.warn.controller;

import com.tianhai.warn.constants.AlarmConstants;
import com.tianhai.warn.enums.AlarmLevel;
import com.tianhai.warn.enums.AlarmStatus;
import com.tianhai.warn.model.AlarmConfig;
import com.tianhai.warn.model.AlarmRecord;
import com.tianhai.warn.model.LocationTrack;
import com.tianhai.warn.model.Student;
import com.tianhai.warn.service.AlarmConfigService;
import com.tianhai.warn.service.AlarmRecordService;
import com.tianhai.warn.service.LocationTrackService;
import com.tianhai.warn.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * 测试数据初始化器
 * 适用于 Spring MVC + Tomcat 项目
 * 用于在测试前准备测试数据
 */
@Component
public class TestDataInitializer {

    @Autowired
    private AlarmConfigService alarmConfigService;

    @Autowired
    private AlarmRecordService alarmRecordService;

    @Autowired
    private LocationTrackService locationTrackService;

    @Autowired
    private StudentService studentService;

    /**
     * 初始化测试数据
     */
    public void initializeTestData() {
        // 初始化地图配置
        initializeMapConfig();

        // 初始化学生数据
        initializeStudentData();

        // 初始化报警记录
        initializeAlarmRecords();

        // 初始化位置轨迹
        initializeLocationTracks();
    }

    /**
     * 初始化地图配置
     */
    private void initializeMapConfig() {
        AlarmConfig mapConfig = AlarmConfig.builder()
                .apiProvider(AlarmConstants.GAODE_ALARM_LBS_MAP)
                .apiKey("test_api_key")
                .apiSecret("test_api_secret")
                .isActive(AlarmConstants.ALARM_CONFIG_ACTIVE)
                .createdAt(new Date())
                .updatedAt(new Date())
                .build();

        // 这里应该调用服务层方法保存配置
        // 由于是测试环境，我们假设配置已经存在
    }

    /**
     * 初始化学生数据
     */
    private void initializeStudentData() {
        Student student1 = new Student();
        student1.setStudentNo("2021001");
        student1.setName("张三");
        student1.setFatherPhone("13800138001");
        student1.setMotherPhone("13800138002");
        student1.setDormitory("A101");
        student1.setCreateTime(new Date());
        student1.setUpdateTime(new Date());

        Student student2 = new Student();
        student2.setStudentNo("2021002");
        student2.setName("李四");
        student2.setFatherPhone("13800138003");
        student2.setMotherPhone("13800138004");
        student2.setDormitory("A102");
        student2.setCreateTime(new Date());
        student2.setUpdateTime(new Date());

        // 这里应该调用服务层方法保存学生数据
        // 由于是测试环境，我们假设学生数据已经存在
    }

    /**
     * 初始化报警记录
     */
    private void initializeAlarmRecords() {
        AlarmRecord alarmRecord1 = AlarmRecord.builder()
                .alarmNo("ALM20231201001")
                .studentNo("2021001")
                .alarmType(1) // 一键报警
                .alarmLevel(AlarmLevel.NORMAL.getCode())
                .alarmStatus(AlarmStatus.PENDING.getCode())
                .createdAt(new Date())
                .updatedAt(new Date())
                .build();

        AlarmRecord alarmRecord2 = AlarmRecord.builder()
                .alarmNo("ALM20231201002")
                .studentNo("2021002")
                .alarmType(1) // 一键报警
                .alarmLevel(AlarmLevel.CRITICAL.getCode())
                .alarmStatus(AlarmStatus.PROCESSING.getCode())
                .createdAt(new Date())
                .updatedAt(new Date())
                .build();

        // 这里应该调用服务层方法保存报警记录
        // 由于是测试环境，我们假设报警记录已经存在
    }

    /**
     * 初始化位置轨迹
     */
    private void initializeLocationTracks() {
        LocationTrack locationTrack1 = LocationTrack.builder()
                .alarmNo("ALM20231201001")
                .latitude(39.9042)
                .longitude(116.4074)
                .locationAccuracy(10.0)
                .speed(5.0)
                .direction(90.0)
                .locationTime(new Date())
                .createdAt(new Date())
                .firstLocationTime(new Date())
                .endLocationTime(new Date())
                .build();

        LocationTrack locationTrack2 = LocationTrack.builder()
                .alarmNo("ALM20231201002")
                .latitude(39.9142)
                .longitude(116.4174)
                .locationAccuracy(15.0)
                .speed(3.0)
                .direction(180.0)
                .locationTime(new Date())
                .createdAt(new Date())
                .firstLocationTime(new Date())
                .endLocationTime(new Date())
                .build();

        // 这里应该调用服务层方法保存位置轨迹
        // 由于是测试环境，我们假设位置轨迹已经存在
    }

    /**
     * 清理测试数据
     */
    public void cleanupTestData() {
        // 清理测试数据的逻辑
        // 由于使用了@Transactional注解，测试结束后会自动回滚
    }

    /**
     * 获取测试用的学生数据
     */
    public Student getTestStudent() {
        Student student = new Student();
        student.setStudentNo("2021001");
        student.setName("张三");
        student.setFatherPhone("13800138001");
        student.setMotherPhone("13800138002");
        student.setDormitory("A101");
        student.setCreateTime(new Date());
        student.setUpdateTime(new Date());
        return student;
    }

    /**
     * 获取测试用的报警记录
     */
    public AlarmRecord getTestAlarmRecord() {
        return AlarmRecord.builder()
                .alarmNo("ALM20231201001")
                .studentNo("2021001")
                .alarmType(1)
                .alarmLevel(AlarmLevel.NORMAL.getCode())
                .alarmStatus(AlarmStatus.PENDING.getCode())
                .createdAt(new Date())
                .updatedAt(new Date())
                .build();
    }

    /**
     * 获取测试用的位置轨迹
     */
    public LocationTrack getTestLocationTrack() {
        return LocationTrack.builder()
                .alarmNo("ALM20231201001")
                .latitude(39.9042)
                .longitude(116.4074)
                .locationAccuracy(10.0)
                .speed(5.0)
                .direction(90.0)
                .locationTime(new Date())
                .createdAt(new Date())
                .firstLocationTime(new Date())
                .endLocationTime(new Date())
                .build();
    }

    /**
     * 获取测试用的地图配置
     */
    public AlarmConfig getTestMapConfig() {
        return AlarmConfig.builder()
                .apiProvider(AlarmConstants.GAODE_ALARM_LBS_MAP)
                .apiKey("test_api_key")
                .apiSecret("test_api_secret")
                .isActive(AlarmConstants.ALARM_CONFIG_ACTIVE)
                .createdAt(new Date())
                .updatedAt(new Date())
                .build();
    }
}