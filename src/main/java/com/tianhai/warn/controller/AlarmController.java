package com.tianhai.warn.controller;

import com.tianhai.warn.annotation.LogOperation;
import com.tianhai.warn.annotation.RequirePermission;
import com.tianhai.warn.constants.AlarmConstants;
import com.tianhai.warn.constants.Constants;
import com.tianhai.warn.dto.CancelAlarmDTO;
import com.tianhai.warn.dto.OneClickAlarmDTO;
import com.tianhai.warn.enums.ResultCode;
import com.tianhai.warn.exception.BusinessException;
import com.tianhai.warn.exception.SystemException;
import com.tianhai.warn.model.AlarmConfig;
import com.tianhai.warn.model.LocationTrack;
import com.tianhai.warn.service.AlarmConfigService;
import com.tianhai.warn.service.AlarmService;
import com.tianhai.warn.service.LocationTrackService;
import com.tianhai.warn.utils.Result;
import com.tianhai.warn.vo.LatestLocationVO;
import com.tianhai.warn.vo.StudentAlarmContactsVO;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@Controller
@RequestMapping("/alarm")
public class AlarmController {

    private static final Logger logger = LoggerFactory.getLogger(AlarmController.class);

    @Autowired
    private AlarmService alarmService;

    @Autowired
    private AlarmConfigService alarmConfigService;

    @Autowired
    private LocationTrackService locationTrackService;

    @PostMapping("/one-click/trigger")
    @ResponseBody
    @RequirePermission(roles = Constants.STUDENT)
    @LogOperation("学生触发一键报警")
    public Result<?> triggerOneClickAlarm(@RequestBody OneClickAlarmDTO oneClickAlarmDTO) {
        if (oneClickAlarmDTO.getAlarmLevel() == null) {
            logger.error("报警级别不能为空");
            throw new BusinessException(ResultCode.PARAMETER_ERROR);
        }

        alarmService.processOneClickAlarm(oneClickAlarmDTO);

        return Result.success("成功触发一键报警");
    }

    @PostMapping("/one-click/cancel")
    @ResponseBody
    @RequirePermission(roles = Constants.STUDENT)
    @LogOperation("学生取消一键报警")
    public Result<?> cancelOneClickAlarm(@RequestBody CancelAlarmDTO cancelAlarmDTO) {
        if (StringUtils.isBlank(cancelAlarmDTO.getAlarmNo())) {
            logger.error("无法取消一键报警，因为缺少 alarmNo");
            throw new BusinessException(ResultCode.PARAMETER_ERROR);
        }

        if (StringUtils.isBlank(cancelAlarmDTO.getStudentNo())) {
            logger.error("无法取消一键报警，因为缺少 studentNo");
            throw new BusinessException(ResultCode.PARAMETER_ERROR);
        }

        alarmService.cancelOneClickAlarm(cancelAlarmDTO);

        return Result.success("取消一键报警成功");
    }

    // 获取高德地图配置信息
    @GetMapping("/config/map")
    @ResponseBody
    @RequirePermission(roles = { Constants.STUDENT, Constants.DORMITORY_MANAGER, Constants.SYSTEM_USER })
    public Result<AlarmConfig> getMapConfig() {
        AlarmConfig mapConfig = alarmConfigService.selectByApiProvider(AlarmConstants.ALARM_LBS_MAP);

        if (mapConfig == null || !Objects.equals(mapConfig.getIsActive(), AlarmConstants.ALARM_CONFIG_ACTIVE)) {
            logger.error("高德地图API配置未启用或不存在");
            throw new SystemException(ResultCode.ALARM_CONFIG_NOT_FOUNT);
        }

        return Result.success(mapConfig);
    }

    /**
     * 查询学生报警时的最新位置信息 // todo
     * 
     * @param alarmNo 报警编号
     * @return Result<LocationTrack>
     */
    @GetMapping("/location")
    @ResponseBody
    @RequirePermission(roles = { Constants.DORMITORY_MANAGER, Constants.SYSTEM_USER })
    public Result<LatestLocationVO> getStudentLocation(@RequestParam String alarmNo) {
        if (StringUtils.isBlank(alarmNo)) {
            logger.error("查询学生位置失败，alarmNo为空");
            throw new BusinessException(ResultCode.PARAMETER_ERROR);
        }
        LatestLocationVO latestLocationVO = locationTrackService.selectLastByAlarmNo(alarmNo);
        if (latestLocationVO == null) {
            return Result.error(ResultCode.PARAMETER_ERROR.getCode(), "未找到该报警编号的位置信息");
        }
        return Result.success(latestLocationVO);
    }

    // 根据班级管理员或宿管的工号获取所管理学生的报警ID和家长联系方式
    @GetMapping("/stu-alarm-contact")
    @ResponseBody
    @RequirePermission(roles = { Constants.SYSTEM_USER, Constants.DORMITORY_MANAGER })
    public Result<List<StudentAlarmContactsVO>> getAlarmContactInfo(@RequestParam String helperNo,
            @RequestParam String role) {
        if (StringUtils.isBlank(helperNo)) {
            logger.error("班级管理员或宿管的工号不正确：helperNo: {}", helperNo);
            throw new BusinessException(ResultCode.PARAMETER_ERROR);
        }

        boolean validateRole = Constants.SYSTEM_USER.equalsIgnoreCase(role) ||
                Constants.DORMITORY_MANAGER.equalsIgnoreCase(role);
        if (!validateRole) {
            logger.error("用户角色不正确：role: {}", role);
            throw new BusinessException(ResultCode.PARAMETER_ERROR);
        }

        List<StudentAlarmContactsVO> studentAlarmContactsList = alarmService.searchStudentAlarmContactInfo(helperNo,
                role);

        return Result.success(studentAlarmContactsList);
    }
}
