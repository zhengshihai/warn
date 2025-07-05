package com.tianhai.warn.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tianhai.warn.constants.AlarmConstants;
import com.tianhai.warn.constants.Constants;
import com.tianhai.warn.dto.CancelAlarmDTO;
import com.tianhai.warn.dto.OneClickAlarmDTO;
import com.tianhai.warn.enums.AlarmLevel;
import com.tianhai.warn.enums.ResultCode;
import com.tianhai.warn.exception.BusinessException;
import com.tianhai.warn.exception.SystemException;
import com.tianhai.warn.model.AlarmConfig;
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
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AlarmController 测试类
 * 测试覆盖：
 * 1. 一键报警触发接口
 * 2. 一键报警取消接口
 * 3. 获取地图配置接口
 * 4. 获取学生位置接口
 * 5. 获取报警联系人信息接口
 */
@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(locations = {
                "classpath:spring/spring-mvc.xml",
                "classpath:spring/spring-mybatis.xml"
// 其他需要的配置
})
@DisplayName("报警控制器测试")
class AlarmControllerTest {

        @Autowired
        private WebApplicationContext wac;

        @MockBean
        private AlarmService alarmService;

        @MockBean
        private AlarmConfigService alarmConfigService;

        @MockBean
        private LocationTrackService locationTrackService;

        private MockMvc mockMvc;
        private ObjectMapper objectMapper;

        @BeforeEach
        void setUp() {
                mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
                objectMapper = new ObjectMapper();
        }

        @Test
        @DisplayName("测试一键报警触发 - 正常情况")
        void testTriggerOneClickAlarm_Success() throws Exception {
                // 准备测试数据
                OneClickAlarmDTO requestDTO = OneClickAlarmDTO.builder()
                                .studentNo("2021001")
                                .alarmLevel(AlarmLevel.NORMAL)
                                .latitude(39.9042)
                                .longitude(116.4074)
                                .locationAccuracy(10.0)
                                .speed(5.0)
                                .direction(90.0)
                                .description("测试报警")
                                .alarmNo("ALM20231201001")
                                .build();

                // Mock服务层方法
                doNothing().when(alarmService).processOneClickAlarm(any(OneClickAlarmDTO.class));

                // 执行测试
                mockMvc.perform(post("/alarm/one-click/trigger")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDTO)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200))
                                .andExpect(jsonPath("$.message").value("操作成功"))
                                .andExpect(jsonPath("$.data").value("成功触发一键报警"));

                // 验证服务层方法被调用
                verify(alarmService, times(1)).processOneClickAlarm(any(OneClickAlarmDTO.class));
        }

        @Test
        @DisplayName("测试一键报警触发 - 报警级别为空")
        void testTriggerOneClickAlarm_AlarmLevelNull() throws Exception {
                // 准备测试数据 - 报警级别为空
                OneClickAlarmDTO requestDTO = OneClickAlarmDTO.builder()
                                .studentNo("2021001")
                                .alarmLevel(null) // 报警级别为空
                                .latitude(39.9042)
                                .longitude(116.4074)
                                .alarmNo("ALM20231201001")
                                .build();

                // 执行测试
                mockMvc.perform(post("/alarm/one-click/trigger")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDTO)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(ResultCode.PARAMETER_ERROR.getCode()))
                                .andExpect(jsonPath("$.message").value(ResultCode.PARAMETER_ERROR.getMessage()));

                // 验证服务层方法没有被调用
                verify(alarmService, never()).processOneClickAlarm(any(OneClickAlarmDTO.class));
        }

        @Test
        @DisplayName("测试一键报警触发 - 紧急级别报警")
        void testTriggerOneClickAlarm_CriticalLevel() throws Exception {
                // 准备测试数据 - 紧急级别报警
                OneClickAlarmDTO requestDTO = OneClickAlarmDTO.builder()
                                .studentNo("2021001")
                                .alarmLevel(AlarmLevel.CRITICAL)
                                .latitude(39.9042)
                                .longitude(116.4074)
                                .alarmNo("ALM20231201002")
                                .build();

                // Mock服务层方法
                doNothing().when(alarmService).processOneClickAlarm(any(OneClickAlarmDTO.class));

                // 执行测试
                mockMvc.perform(post("/alarm/one-click/trigger")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDTO)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200))
                                .andExpect(jsonPath("$.message").value("操作成功"));

                // 验证服务层方法被调用
                verify(alarmService, times(1)).processOneClickAlarm(any(OneClickAlarmDTO.class));
        }

        @Test
        @DisplayName("测试一键报警取消 - 正常情况")
        void testCancelOneClickAlarm_Success() throws Exception {
                // 准备测试数据
                CancelAlarmDTO requestDTO = CancelAlarmDTO.builder()
                                .studentNo("2021001")
                                .name("张三")
                                .alarmNo("ALM20231201001")
                                .build();

                // Mock服务层方法
                doNothing().when(alarmService).cancelOneClickAlarm(any(CancelAlarmDTO.class));

                // 执行测试
                mockMvc.perform(post("/alarm/one-click/cancel")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDTO)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200))
                                .andExpect(jsonPath("$.message").value("操作成功"))
                                .andExpect(jsonPath("$.data").value("取消一键报警成功"));

                // 验证服务层方法被调用
                verify(alarmService, times(1)).cancelOneClickAlarm(any(CancelAlarmDTO.class));
        }

        @Test
        @DisplayName("测试一键报警取消 - alarmNo为空")
        void testCancelOneClickAlarm_AlarmNoEmpty() throws Exception {
                // 准备测试数据 - alarmNo为空
                CancelAlarmDTO requestDTO = CancelAlarmDTO.builder()
                                .studentNo("2021001")
                                .name("张三")
                                .alarmNo("") // alarmNo为空
                                .build();

                // 执行测试
                mockMvc.perform(post("/alarm/one-click/cancel")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDTO)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(ResultCode.PARAMETER_ERROR.getCode()))
                                .andExpect(jsonPath("$.message").value(ResultCode.PARAMETER_ERROR.getMessage()));

                // 验证服务层方法没有被调用
                verify(alarmService, never()).cancelOneClickAlarm(any(CancelAlarmDTO.class));
        }

        @Test
        @DisplayName("测试一键报警取消 - studentNo为空")
        void testCancelOneClickAlarm_StudentNoEmpty() throws Exception {
                // 准备测试数据 - studentNo为空
                CancelAlarmDTO requestDTO = CancelAlarmDTO.builder()
                                .studentNo("") // studentNo为空
                                .name("张三")
                                .alarmNo("ALM20231201001")
                                .build();

                // 执行测试
                mockMvc.perform(post("/alarm/one-click/cancel")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDTO)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(ResultCode.PARAMETER_ERROR.getCode()))
                                .andExpect(jsonPath("$.message").value(ResultCode.PARAMETER_ERROR.getMessage()));

                // 验证服务层方法没有被调用
                verify(alarmService, never()).cancelOneClickAlarm(any(CancelAlarmDTO.class));
        }

        @Test
        @DisplayName("测试一键报警取消 - 参数为null")
        void testCancelOneClickAlarm_ParametersNull() throws Exception {
                // 准备测试数据 - 参数为null
                CancelAlarmDTO requestDTO = CancelAlarmDTO.builder()
                                .studentNo(null) // studentNo为null
                                .name("张三")
                                .alarmNo(null) // alarmNo为null
                                .build();

                // 执行测试
                mockMvc.perform(post("/alarm/one-click/cancel")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDTO)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(ResultCode.PARAMETER_ERROR.getCode()))
                                .andExpect(jsonPath("$.message").value(ResultCode.PARAMETER_ERROR.getMessage()));

                // 验证服务层方法没有被调用
                verify(alarmService, never()).cancelOneClickAlarm(any(CancelAlarmDTO.class));
        }

        @Test
        @DisplayName("测试获取地图配置 - 正常情况")
        void testGetMapConfig_Success() throws Exception {
                // 准备测试数据
                AlarmConfig mockConfig = AlarmConfig.builder()
                                .id(1L)
                                .apiProvider(AlarmConstants.ALARM_LBS_MAP)
                                .apiKey("test_api_key")
                                .apiSecret("test_api_secret")
                                .isActive(AlarmConstants.ALARM_CONFIG_ACTIVE)
                                .createdAt(new Date())
                                .build();

                // Mock服务层方法
                when(alarmConfigService.selectByApiProvider(AlarmConstants.ALARM_LBS_MAP))
                                .thenReturn(mockConfig);

                // 执行测试
                mockMvc.perform(get("/alarm/config/map"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200))
                                .andExpect(jsonPath("$.data.id").value(1))
                                .andExpect(jsonPath("$.data.apiProvider").value(AlarmConstants.ALARM_LBS_MAP))
                                .andExpect(jsonPath("$.data.apiKey").value("test_api_key"))
                                .andExpect(jsonPath("$.data.isActive").value(AlarmConstants.ALARM_CONFIG_ACTIVE));

                // 验证服务层方法被调用
                verify(alarmConfigService, times(1)).selectByApiProvider(AlarmConstants.ALARM_LBS_MAP);
        }

        @Test
        @DisplayName("测试获取地图配置 - 配置不存在")
        void testGetMapConfig_ConfigNotFound() throws Exception {
                // Mock服务层方法返回null
                when(alarmConfigService.selectByApiProvider(AlarmConstants.ALARM_LBS_MAP))
                                .thenReturn(null);

                // 执行测试
                mockMvc.perform(get("/alarm/config/map"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(ResultCode.ALARM_CONFIG_NOT_FOUNT.getCode()))
                                .andExpect(jsonPath("$.message").value(ResultCode.ALARM_CONFIG_NOT_FOUNT.getMessage()));

                // 验证服务层方法被调用
                verify(alarmConfigService, times(1)).selectByApiProvider(AlarmConstants.ALARM_LBS_MAP);
        }

        @Test
        @DisplayName("测试获取地图配置 - 配置未启用")
        void testGetMapConfig_ConfigInactive() throws Exception {
                // 准备测试数据 - 配置未启用
                AlarmConfig mockConfig = AlarmConfig.builder()
                                .id(1L)
                                .apiProvider(AlarmConstants.ALARM_LBS_MAP)
                                .apiKey("test_api_key")
                                .isActive(AlarmConstants.ALARM_CONFIG_INACTIVE) // 未启用
                                .build();

                // Mock服务层方法
                when(alarmConfigService.selectByApiProvider(AlarmConstants.ALARM_LBS_MAP))
                                .thenReturn(mockConfig);

                // 执行测试
                mockMvc.perform(get("/alarm/config/map"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(ResultCode.ALARM_CONFIG_NOT_FOUNT.getCode()))
                                .andExpect(jsonPath("$.message").value(ResultCode.ALARM_CONFIG_NOT_FOUNT.getMessage()));

                // 验证服务层方法被调用
                verify(alarmConfigService, times(1)).selectByApiProvider(AlarmConstants.ALARM_LBS_MAP);
        }

        @Test
        @DisplayName("测试获取学生位置 - 正常情况")
        void testGetStudentLocation_Success() throws Exception {
                // 准备测试数据
                LatestLocationVO mockLocation = LatestLocationVO.builder()
                                .latitude(39.9042)
                                .longitude(116.4074)
                                .locationAccuracy(10.0)
                                .build();

                // Mock服务层方法
                when(locationTrackService.selectLastByAlarmNo("ALM20231201001"))
                                .thenReturn(mockLocation);

                // 执行测试
                mockMvc.perform(get("/alarm/location")
                                .param("alarmNo", "ALM20231201001"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200))
                                .andExpect(jsonPath("$.data.latitude").value(39.9042))
                                .andExpect(jsonPath("$.data.longitude").value(116.4074))
                                .andExpect(jsonPath("$.data.locationAccuracy").value(10.0));

                // 验证服务层方法被调用
                verify(locationTrackService, times(1)).selectLastByAlarmNo("ALM20231201001");
        }

        @Test
        @DisplayName("测试获取学生位置 - alarmNo为空")
        void testGetStudentLocation_AlarmNoEmpty() throws Exception {
                // 执行测试 - alarmNo为空
                mockMvc.perform(get("/alarm/location")
                                .param("alarmNo", ""))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(ResultCode.PARAMETER_ERROR.getCode()))
                                .andExpect(jsonPath("$.message").value(ResultCode.PARAMETER_ERROR.getMessage()));

                // 验证服务层方法没有被调用
                verify(locationTrackService, never()).selectLastByAlarmNo(anyString());
        }

        @Test
        @DisplayName("测试获取学生位置 - 位置信息不存在")
        void testGetStudentLocation_LocationNotFound() throws Exception {
                // Mock服务层方法返回null
                when(locationTrackService.selectLastByAlarmNo("ALM20231201001"))
                                .thenReturn(null);

                // 执行测试
                mockMvc.perform(get("/alarm/location")
                                .param("alarmNo", "ALM20231201001"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(ResultCode.PARAMETER_ERROR.getCode()))
                                .andExpect(jsonPath("$.message").value("未找到该报警编号的位置信息"));

                // 验证服务层方法被调用
                verify(locationTrackService, times(1)).selectLastByAlarmNo("ALM20231201001");
        }

        @Test
        @DisplayName("测试获取报警联系人信息 - 宿管角色正常情况")
        void testGetAlarmContactInfo_DormitoryManager_Success() throws Exception {
                // 准备测试数据
                List<StudentAlarmContactsVO> mockContacts = Arrays.asList(
                                StudentAlarmContactsVO.builder()
                                                .studentName("张三")
                                                .studentNo("2021001")
                                                .alarmNo("ALM20231201001")
                                                .fatherPhone("13800138001")
                                                .motherPhone("13800138002")
                                                .build(),
                                StudentAlarmContactsVO.builder()
                                                .studentName("李四")
                                                .studentNo("2021002")
                                                .alarmNo("ALM20231201002")
                                                .fatherPhone("13800138003")
                                                .motherPhone("13800138004")
                                                .build());

                // Mock服务层方法
                when(alarmService.searchStudentAlarmContactInfo("DM001", Constants.DORMITORY_MANAGER))
                                .thenReturn(mockContacts);

                // 执行测试
                mockMvc.perform(get("/alarm/stu-alarm-contact")
                                .param("helperNo", "DM001")
                                .param("role", Constants.DORMITORY_MANAGER))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200))
                                .andExpect(jsonPath("$.data").isArray())
                                .andExpect(jsonPath("$.data.length()").value(2))
                                .andExpect(jsonPath("$.data[0].studentName").value("张三"))
                                .andExpect(jsonPath("$.data[0].studentNo").value("2021001"))
                                .andExpect(jsonPath("$.data[0].alarmNo").value("ALM20231201001"))
                                .andExpect(jsonPath("$.data[0].fatherPhone").value("13800138001"))
                                .andExpect(jsonPath("$.data[0].motherPhone").value("13800138002"));

                // 验证服务层方法被调用
                verify(alarmService, times(1)).searchStudentAlarmContactInfo("DM001", Constants.DORMITORY_MANAGER);
        }

        @Test
        @DisplayName("测试获取报警联系人信息 - 系统用户角色正常情况")
        void testGetAlarmContactInfo_SystemUser_Success() throws Exception {
                // 准备测试数据
                List<StudentAlarmContactsVO> mockContacts = Arrays.asList(
                                StudentAlarmContactsVO.builder()
                                                .studentName("王五")
                                                .studentNo("2021003")
                                                .alarmNo("ALM20231201003")
                                                .fatherPhone("13800138005")
                                                .motherPhone("13800138006")
                                                .build());

                // Mock服务层方法
                when(alarmService.searchStudentAlarmContactInfo("SU001", Constants.SYSTEM_USER))
                                .thenReturn(mockContacts);

                // 执行测试
                mockMvc.perform(get("/alarm/stu-alarm-contact")
                                .param("helperNo", "SU001")
                                .param("role", Constants.SYSTEM_USER))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200))
                                .andExpect(jsonPath("$.data").isArray())
                                .andExpect(jsonPath("$.data.length()").value(1))
                                .andExpect(jsonPath("$.data[0].studentName").value("王五"))
                                .andExpect(jsonPath("$.data[0].studentNo").value("2021003"));

                // 验证服务层方法被调用
                verify(alarmService, times(1)).searchStudentAlarmContactInfo("SU001", Constants.SYSTEM_USER);
        }

        @Test
        @DisplayName("测试获取报警联系人信息 - helperNo为空")
        void testGetAlarmContactInfo_HelperNoEmpty() throws Exception {
                // 执行测试 - helperNo为空
                mockMvc.perform(get("/alarm/stu-alarm-contact")
                                .param("helperNo", "")
                                .param("role", Constants.DORMITORY_MANAGER))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(ResultCode.PARAMETER_ERROR.getCode()))
                                .andExpect(jsonPath("$.message").value(ResultCode.PARAMETER_ERROR.getMessage()));

                // 验证服务层方法没有被调用
                verify(alarmService, never()).searchStudentAlarmContactInfo(anyString(), anyString());
        }

        @Test
        @DisplayName("测试获取报警联系人信息 - 角色无效")
        void testGetAlarmContactInfo_InvalidRole() throws Exception {
                // 执行测试 - 角色无效
                mockMvc.perform(get("/alarm/stu-alarm-contact")
                                .param("helperNo", "DM001")
                                .param("role", "INVALID_ROLE"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(ResultCode.PARAMETER_ERROR.getCode()))
                                .andExpect(jsonPath("$.message").value(ResultCode.PARAMETER_ERROR.getMessage()));

                // 验证服务层方法没有被调用
                verify(alarmService, never()).searchStudentAlarmContactInfo(anyString(), anyString());
        }

        @Test
        @DisplayName("测试获取报警联系人信息 - 角色大小写不敏感")
        void testGetAlarmContactInfo_RoleCaseInsensitive() throws Exception {
                // 准备测试数据
                List<StudentAlarmContactsVO> mockContacts = Arrays.asList(
                                StudentAlarmContactsVO.builder()
                                                .studentName("张三")
                                                .studentNo("2021001")
                                                .alarmNo("ALM20231201001")
                                                .fatherPhone("13800138001")
                                                .motherPhone("13800138002")
                                                .build());

                // Mock服务层方法
                when(alarmService.searchStudentAlarmContactInfo("DM001", "dormitory_manager"))
                                .thenReturn(mockContacts);

                // 执行测试 - 使用小写角色名
                mockMvc.perform(get("/alarm/stu-alarm-contact")
                                .param("helperNo", "DM001")
                                .param("role", "dormitory_manager"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200))
                                .andExpect(jsonPath("$.data").isArray())
                                .andExpect(jsonPath("$.data.length()").value(1));

                // 验证服务层方法被调用
                verify(alarmService, times(1)).searchStudentAlarmContactInfo("DM001", "dormitory_manager");
        }

        @Test
        @DisplayName("测试获取报警联系人信息 - 空结果")
        void testGetAlarmContactInfo_EmptyResult() throws Exception {
                // Mock服务层方法返回空列表
                when(alarmService.searchStudentAlarmContactInfo("DM001", Constants.DORMITORY_MANAGER))
                                .thenReturn(Arrays.asList());

                // 执行测试
                mockMvc.perform(get("/alarm/stu-alarm-contact")
                                .param("helperNo", "DM001")
                                .param("role", Constants.DORMITORY_MANAGER))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200))
                                .andExpect(jsonPath("$.data").isArray())
                                .andExpect(jsonPath("$.data.length()").value(0));

                // 验证服务层方法被调用
                verify(alarmService, times(1)).searchStudentAlarmContactInfo("DM001", Constants.DORMITORY_MANAGER);
        }

        @Test
        @DisplayName("测试服务层异常处理 - BusinessException")
        void testServiceLayerBusinessException() throws Exception {
                // 准备测试数据
                OneClickAlarmDTO requestDTO = OneClickAlarmDTO.builder()
                                .studentNo("2021001")
                                .alarmLevel(AlarmLevel.NORMAL)
                                .alarmNo("ALM20231201001")
                                .build();

                // Mock服务层抛出BusinessException
                doThrow(new BusinessException(ResultCode.ALARM_ONE_CLICK_RATE_TOO_HIGH))
                                .when(alarmService).processOneClickAlarm(any(OneClickAlarmDTO.class));

                // 执行测试
                mockMvc.perform(post("/alarm/one-click/trigger")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDTO)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(ResultCode.ALARM_ONE_CLICK_RATE_TOO_HIGH.getCode()))
                                .andExpect(jsonPath("$.message")
                                                .value(ResultCode.ALARM_ONE_CLICK_RATE_TOO_HIGH.getMessage()));

                // 验证服务层方法被调用
                verify(alarmService, times(1)).processOneClickAlarm(any(OneClickAlarmDTO.class));
        }

        @Test
        @DisplayName("测试服务层异常处理 - SystemException")
        void testServiceLayerSystemException() throws Exception {
                // 准备测试数据
                CancelAlarmDTO requestDTO = CancelAlarmDTO.builder()
                                .studentNo("2021001")
                                .name("张三")
                                .alarmNo("ALM20231201001")
                                .build();

                // Mock服务层抛出SystemException
                doThrow(new SystemException(ResultCode.ERROR))
                                .when(alarmService).cancelOneClickAlarm(any(CancelAlarmDTO.class));

                // 执行测试
                mockMvc.perform(post("/alarm/one-click/cancel")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDTO)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(ResultCode.ERROR.getCode()))
                                .andExpect(jsonPath("$.message").value(ResultCode.ERROR.getMessage()));

                // 验证服务层方法被调用
                verify(alarmService, times(1)).cancelOneClickAlarm(any(CancelAlarmDTO.class));
        }

        @Test
        @DisplayName("测试边界条件 - 最大经纬度值")
        void testBoundaryConditions_MaxCoordinates() throws Exception {
                // 准备测试数据 - 最大经纬度值
                OneClickAlarmDTO requestDTO = OneClickAlarmDTO.builder()
                                .studentNo("2021001")
                                .alarmLevel(AlarmLevel.NORMAL)
                                .latitude(90.0) // 最大纬度
                                .longitude(180.0) // 最大经度
                                .alarmNo("ALM20231201001")
                                .build();

                // Mock服务层方法
                doNothing().when(alarmService).processOneClickAlarm(any(OneClickAlarmDTO.class));

                // 执行测试
                mockMvc.perform(post("/alarm/one-click/trigger")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDTO)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200))
                                .andExpect(jsonPath("$.message").value("操作成功"));

                // 验证服务层方法被调用
                verify(alarmService, times(1)).processOneClickAlarm(any(OneClickAlarmDTO.class));
        }

        @Test
        @DisplayName("测试边界条件 - 最小经纬度值")
        void testBoundaryConditions_MinCoordinates() throws Exception {
                // 准备测试数据 - 最小经纬度值
                OneClickAlarmDTO requestDTO = OneClickAlarmDTO.builder()
                                .studentNo("2021001")
                                .alarmLevel(AlarmLevel.CRITICAL)
                                .latitude(-90.0) // 最小纬度
                                .longitude(-180.0) // 最小经度
                                .alarmNo("ALM20231201001")
                                .build();

                // Mock服务层方法
                doNothing().when(alarmService).processOneClickAlarm(any(OneClickAlarmDTO.class));

                // 执行测试
                mockMvc.perform(post("/alarm/one-click/trigger")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDTO)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200))
                                .andExpect(jsonPath("$.message").value("操作成功"));

                // 验证服务层方法被调用
                verify(alarmService, times(1)).processOneClickAlarm(any(OneClickAlarmDTO.class));
        }

        @Test
        @DisplayName("测试边界条件 - 特殊字符处理")
        void testBoundaryConditions_SpecialCharacters() throws Exception {
                // 准备测试数据 - 包含特殊字符
                CancelAlarmDTO requestDTO = CancelAlarmDTO.builder()
                                .studentNo("2021001")
                                .name("张三&李四") // 包含特殊字符
                                .alarmNo("ALM20231201001")
                                .build();

                // Mock服务层方法
                doNothing().when(alarmService).cancelOneClickAlarm(any(CancelAlarmDTO.class));

                // 执行测试
                mockMvc.perform(post("/alarm/one-click/cancel")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDTO)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200))
                                .andExpect(jsonPath("$.message").value("操作成功"));

                // 验证服务层方法被调用
                verify(alarmService, times(1)).cancelOneClickAlarm(any(CancelAlarmDTO.class));
        }

        @Test
        @DisplayName("测试边界条件 - 超长字符串")
        void testBoundaryConditions_LongStrings() throws Exception {
                // 准备测试数据 - 超长字符串
                String longString = "a".repeat(1000); // 1000个字符
                CancelAlarmDTO requestDTO = CancelAlarmDTO.builder()
                                .studentNo("2021001")
                                .name(longString)
                                .alarmNo("ALM20231201001")
                                .build();

                // Mock服务层方法
                doNothing().when(alarmService).cancelOneClickAlarm(any(CancelAlarmDTO.class));

                // 执行测试
                mockMvc.perform(post("/alarm/one-click/cancel")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(requestDTO)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.code").value(200))
                                .andExpect(jsonPath("$.message").value("操作成功"));

                // 验证服务层方法被调用
                verify(alarmService, times(1)).cancelOneClickAlarm(any(CancelAlarmDTO.class));
        }
}