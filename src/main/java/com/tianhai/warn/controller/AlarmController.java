package com.tianhai.warn.controller;

import com.tianhai.warn.annotation.LogOperation;
import com.tianhai.warn.annotation.RequirePermission;
import com.tianhai.warn.constants.AlarmConstants;
import com.tianhai.warn.constants.Constants;
import com.tianhai.warn.dto.CancelAlarmDTO;
import com.tianhai.warn.dto.LocationDTO;
import com.tianhai.warn.dto.OneClickAlarmDTO;
import com.tianhai.warn.enums.ResultCode;
import com.tianhai.warn.exception.BusinessException;
import com.tianhai.warn.exception.SystemException;
import com.tianhai.warn.model.AlarmConfig;
import com.tianhai.warn.service.AlarmConfigService;
import com.tianhai.warn.service.AlarmService;
import com.tianhai.warn.utils.Result;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Objects;

@Controller
@RequestMapping("/alarm")
public class AlarmController {

    private static final Logger logger = LoggerFactory.getLogger(AlarmController.class);

    @Autowired
    private AlarmService alarmService;

    @Autowired
    private AlarmConfigService alarmConfigService;


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



    //获取高德地图配置信息
    @GetMapping("/config/map")
    @ResponseBody
    @RequirePermission(roles = {Constants.STUDENT, Constants.DORMITORY_MANAGER, Constants.SYSTEM_USER})
    public Result<AlarmConfig> getMapConfig() {
        AlarmConfig mapConfig = alarmConfigService.selectByApiProvider(AlarmConstants.ALARM_LBS_MAP);

        if (mapConfig == null || !Objects.equals(mapConfig.getIsActive(), AlarmConstants.ALARM_CONFIG_ACTIVE)) {
            logger.error("高德地图API配置未启用或不存在");
            throw new SystemException(ResultCode.ALARM_CONFIG_NOT_FOUNT);
        }

        return Result.success(mapConfig);
    }

    // 获取地图信息
//    @PostMapping("/map/info")
//    @ResponseBody
//    @RequirePermission(roles = {Constants.STUDENT, Constants.DORMITORY_MANAGER, Constants.SYSTEM_USER})
//    public Result<Map<String, Object>> getMapInfo(@RequestBody LocationDTO locationDTO) {
//        // 获取高德地图配置
//        AlarmConfig mapConfig = alarmConfigService.selectByApiProvider(AlarmConstants.ALARM_LBS_MAP);
//
//        if (mapConfig == null || !Objects.equals(mapConfig.getIsActive(), AlarmConstants.ALARM_CONFIG_ACTIVE)) {
//            logger.error("高德地图API配置未启用或不存在");
//            throw new SystemException(ResultCode.ALARM_CONFIG_NOT_FOUNT);
//        }
//
//        // 调用高德地图API获取位置信息
//        Map<String, Object> mapInfo = alarmService.getMapInfo(locationDTO, mapConfig.getApiSecret());
//
//        return Result.success(mapInfo);
//    }
}
