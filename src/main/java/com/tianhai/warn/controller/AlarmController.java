package com.tianhai.warn.controller;

import com.tianhai.warn.annotation.LogOperation;
import com.tianhai.warn.annotation.RequirePermission;
import com.tianhai.warn.constants.Constants;
import com.tianhai.warn.dto.LocationUpdateDTO;
import com.tianhai.warn.dto.OneClickAlarmDTO;
import com.tianhai.warn.enums.ResultCode;
import com.tianhai.warn.exception.BusinessException;
import com.tianhai.warn.service.AlarmService;
import com.tianhai.warn.utils.Result;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/alarm")
public class AlarmController {

    private static final Logger logger = LoggerFactory.getLogger(AlarmController.class);

    @Autowired
    private AlarmService alarmService;

    @PostMapping("/one-click")
    @ResponseBody
    @RequirePermission(roles = Constants.STUDENT)
    @LogOperation("学生一键报警")
    public Result<?> triggerOneClickAlarm(@RequestBody OneClickAlarmDTO oneClickAlarmDTO) {
        if (oneClickAlarmDTO.getAlarmLevel() == null) {
            logger.error("报警级别不能为空");
            throw new BusinessException(ResultCode.PARAMETER_ERROR);
        }

        alarmService.processOneClickAlarm(oneClickAlarmDTO);

        return Result.success();
    }
}
