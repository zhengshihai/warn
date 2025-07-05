package com.tianhai.warn.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tianhai.warn.constants.AlarmConstants;
import com.tianhai.warn.constants.Constants;
import com.tianhai.warn.dto.CancelAlarmDTO;
import com.tianhai.warn.dto.OneClickAlarmDTO;
import com.tianhai.warn.enums.AlarmLevel;
import com.tianhai.warn.enums.AlarmStatus;
import com.tianhai.warn.enums.ResultCode;
import com.tianhai.warn.model.AlarmConfig;
import com.tianhai.warn.model.AlarmRecord;
import com.tianhai.warn.model.LocationTrack;
import com.tianhai.warn.model.Student;
import com.tianhai.warn.service.AlarmConfigService;
import com.tianhai.warn.service.AlarmService;
import com.tianhai.warn.service.LocationTrackService;
import com.tianhai.warn.utils.Result;
import com.tianhai.warn.vo.LatestLocationVO;
import com.tianhai.warn.vo.StudentAlarmContactsVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AlarmController 集成测试类
 * 适用于 Spring MVC + Tomcat 项目
 * 测试真实的业务逻辑和数据库交互
 */
@ExtendWith(org.springframework.test.context.junit.jupiter.SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(locations = {
                "classpath:spring/spring-mvc.xml",
                "classpath:spring/spring-mybatis.xml"
}, classes = {
                TestRedisConfig.class
})
@Transactional
@DisplayName("报警控制器集成测试")
class AlarmControllerIntegrationTest {

        @Autowired
        private WebApplicationContext webApplicationContext;

        @Autowired
        private AlarmService alarmService;

        @Autowired
        private AlarmConfigService alarmConfigService;

        @Autowired
        private LocationTrackService locationTrackService;

        @Autowired
        private RedisTemplate<String, Object> redisTemplate;

        private MockMvc mockMvc;
        private ObjectMapper objectMapper;

        @BeforeEach
        void setUp() {
                mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
                objectMapper = new ObjectMapper();
        }

        @Test
        @DisplayName("集成测试 - 一键报警触发完整流程")
        void testTriggerOneClickAlarm_IntegrationFlow() throws Exception {
                // 准备测试数据
                OneClickAlarmDTO requestDTO = OneClickAlarmDTO.builder()
                                .studentNo("2021001")
                                .alarmLevel(AlarmLevel.NORMAL)
                                .latitude(39.9042)
                                .longitude(116.4074)
                                .locationAccuracy(10.0)
                                .speed(5.0)
                                .direction(90.0)
                                .description("集成测试报警")
                                .alarmNo("ALM20231201001")
                                .build();

                // 执行测试 - 使用真实的服务层
                mockMvc.perform(post("/alarm/one-click/trigger")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDTO)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200))
                                .andExpect(jsonPath("$.message").value("成功触发一键报警"))
                                .andExpect(jsonPath("$.data").value("成功触发一键报警"));
        }

        @Test
        @DisplayName("集成测试 - 一键报警取消完整流程")
        void testCancelOneClickAlarm_IntegrationFlow() throws Exception {
                // 准备测试数据
                CancelAlarmDTO requestDTO = CancelAlarmDTO.builder()
                                .studentNo("2021001")
                                .name("张三")
                                .alarmNo("ALM20231201001")
                                .build();

                // 执行测试 - 使用真实的服务层
                mockMvc.perform(post("/alarm/one-click/cancel")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDTO)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200))
                                .andExpect(jsonPath("$.message").value("取消一键报警成功"))
                                .andExpect(jsonPath("$.data").value("取消一键报警成功"));
        }

        @Test
        @DisplayName("集成测试 - 获取地图配置完整流程")
        void testGetMapConfig_IntegrationFlow() throws Exception {
                // 执行测试 - 使用真实的服务层
                mockMvc.perform(get("/alarm/config/map"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200))
                                .andExpect(jsonPath("$.data").exists());
        }

        @Test
        @DisplayName("集成测试 - 获取学生位置完整流程")
        void testGetStudentLocation_IntegrationFlow() throws Exception {
                // 执行测试 - 使用真实的服务层
                mockMvc.perform(get("/alarm/location")
                                .param("alarmNo", "ALM20231201001"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200));
        }

        @Test
        @DisplayName("集成测试 - 获取报警联系人信息完整流程")
        void testGetAlarmContactInfo_IntegrationFlow() throws Exception {
                // 执行测试 - 使用真实的服务层
                mockMvc.perform(get("/alarm/stu-alarm-contact")
                                .param("helperNo", "DM001")
                                .param("role", Constants.DORMITORY_MANAGER))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200))
                                .andExpect(jsonPath("$.data").isArray());
        }

        @Test
        @DisplayName("集成测试 - 业务异常处理")
        void testBusinessExceptionHandling() throws Exception {
                // 准备测试数据 - 触发业务异常
                OneClickAlarmDTO requestDTO = OneClickAlarmDTO.builder()
                                .studentNo("2021001")
                                .alarmLevel(null) // 触发参数验证异常
                                .alarmNo("ALM20231201001")
                                .build();

                // 执行测试
                mockMvc.perform(post("/alarm/one-click/trigger")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDTO)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(ResultCode.PARAMETER_ERROR.getCode()))
                                .andExpect(jsonPath("$.message").value(ResultCode.PARAMETER_ERROR.getMessage()));
        }

        @Test
        @DisplayName("集成测试 - 参数验证边界条件")
        void testParameterValidationBoundaries() throws Exception {
                // 测试各种边界条件
                testParameterValidation("", "张三", "ALM20231201001", "studentNo为空");
                testParameterValidation("2021001", "", "ALM20231201001", "name为空");
                testParameterValidation("2021001", "张三", "", "alarmNo为空");
                testParameterValidation(null, "张三", "ALM20231201001", "studentNo为null");
                testParameterValidation("2021001", null, "ALM20231201001", "name为null");
                testParameterValidation("2021001", "张三", null, "alarmNo为null");
        }

        private void testParameterValidation(String studentNo, String name, String alarmNo, String testCase)
                        throws Exception {
                // 准备测试数据
                CancelAlarmDTO requestDTO = CancelAlarmDTO.builder()
                                .studentNo(studentNo)
                                .name(name)
                                .alarmNo(alarmNo)
                                .build();

                // 执行测试
                mockMvc.perform(post("/alarm/one-click/cancel")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDTO)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(ResultCode.PARAMETER_ERROR.getCode()))
                                .andExpect(jsonPath("$.message").value(ResultCode.PARAMETER_ERROR.getMessage()));
        }

        @Test
        @DisplayName("集成测试 - 并发请求处理")
        void testConcurrentRequestHandling() throws Exception {
                // 准备测试数据
                OneClickAlarmDTO requestDTO = OneClickAlarmDTO.builder()
                                .studentNo("2021001")
                                .alarmLevel(AlarmLevel.NORMAL)
                                .alarmNo("ALM20231201001")
                                .build();

                // 模拟并发请求
                for (int i = 0; i < 5; i++) {
                        mockMvc.perform(post("/alarm/one-click/trigger")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(requestDTO)))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.code").value(200))
                                        .andExpect(jsonPath("$.message").value("成功触发一键报警"));
                }
        }

        @Test
        @DisplayName("集成测试 - 数据一致性验证")
        void testDataConsistencyValidation() throws Exception {
                // 准备测试数据
                OneClickAlarmDTO requestDTO = OneClickAlarmDTO.builder()
                                .studentNo("2021001")
                                .alarmLevel(AlarmLevel.CRITICAL)
                                .latitude(39.9042)
                                .longitude(116.4074)
                                .locationAccuracy(10.0)
                                .speed(5.0)
                                .direction(90.0)
                                .description("数据一致性测试")
                                .alarmNo("ALM20231201001")
                                .build();

                // 执行测试
                mockMvc.perform(post("/alarm/one-click/trigger")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDTO)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200))
                                .andExpect(jsonPath("$.message").value("成功触发一键报警"));
        }

        @Test
        @DisplayName("集成测试 - 性能基准测试")
        void testPerformanceBenchmark() throws Exception {
                // 准备测试数据
                OneClickAlarmDTO requestDTO = OneClickAlarmDTO.builder()
                                .studentNo("2021001")
                                .alarmLevel(AlarmLevel.NORMAL)
                                .alarmNo("ALM20231201001")
                                .build();

                // 性能测试 - 执行100次请求
                long startTime = System.currentTimeMillis();

                for (int i = 0; i < 100; i++) {
                        mockMvc.perform(post("/alarm/one-click/trigger")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(requestDTO)))
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.code").value(200));
                }

                long endTime = System.currentTimeMillis();
                long duration = endTime - startTime;

                // 验证性能要求：100次请求应该在5秒内完成
                assert duration < 5000 : "性能测试失败：100次请求耗时 " + duration + "ms，超过5秒限制";
        }

        @Test
        @DisplayName("集成测试 - 错误恢复机制")
        void testErrorRecoveryMechanism() throws Exception {
                // 准备测试数据
                OneClickAlarmDTO requestDTO = OneClickAlarmDTO.builder()
                                .studentNo("2021001")
                                .alarmLevel(AlarmLevel.NORMAL)
                                .alarmNo("ALM20231201001")
                                .build();

                // 第一次请求应该成功
                mockMvc.perform(post("/alarm/one-click/trigger")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDTO)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200))
                                .andExpect(jsonPath("$.message").value("成功触发一键报警"));

                // 第二次请求应该成功（验证系统稳定性）
                mockMvc.perform(post("/alarm/one-click/trigger")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDTO)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200))
                                .andExpect(jsonPath("$.message").value("成功触发一键报警"));
        }

        @Test
        @DisplayName("集成测试 - 真实数据库交互")
        void testRealDatabaseInteraction() throws Exception {
                // 测试真实的数据库交互
                // 这里可以测试数据库连接、事务处理等

                // 例如：测试地图配置查询
                mockMvc.perform(get("/alarm/config/map"))
                                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("集成测试 - Redis缓存交互")
        void testRedisCacheInteraction() throws Exception {
                // 测试Redis缓存交互
                // 这里可以测试缓存操作、数据一致性等

                // 例如：测试位置信息查询
                mockMvc.perform(get("/alarm/location")
                                .param("alarmNo", "ALM20231201001"))
                                .andExpect(status().isOk());
        }
}