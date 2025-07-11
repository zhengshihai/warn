package com.tianhai.warn.controller;

import com.tianhai.warn.annotation.LogOperation;
import com.tianhai.warn.annotation.RequirePermission;
import com.tianhai.warn.constants.Constants;
import com.tianhai.warn.dto.NotificationDTO;
import com.tianhai.warn.enums.JobRole;
import com.tianhai.warn.enums.ResultCode;
import com.tianhai.warn.enums.TargetScope;
import com.tianhai.warn.enums.UserRole;
import com.tianhai.warn.exception.BusinessException;
import com.tianhai.warn.query.NotificationQuery;
import com.tianhai.warn.service.impl.VerificationServiceImpl;
import com.tianhai.warn.utils.*;
import com.tianhai.warn.vo.NotificationVO;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import com.tianhai.warn.service.NotificationService;

import java.util.*;

/**
 * 通知信息控制器
 */
@Controller
@RequestMapping("/notification")
public class NotificationController {

    private static final Logger logger = LoggerFactory.getLogger(NotificationController.class);

    @Autowired
    private NotificationService notificationService;
    @Autowired
    private VerificationServiceImpl verificationServiceImpl;

    // 分页获取属于特定用户的通知
    @PostMapping("/special-user/page")
    @ResponseBody
    @RequirePermission
    @LogOperation("分页获取特定用户的通知")
    public Result<PageResult<NotificationVO>> getSpecialUserPageList(@RequestBody NotificationQuery query) {
        // 校验查询参数
        validateNotificationQuery(query);

        // 校验分页条件
        PageUtils.normalizePageNums(query);

        PageResult<NotificationVO> notificationPageResult = notificationService.searchSpecialUserPageList(query);


        return Result.success(notificationPageResult);
    }

    // 标记通知为已读
    @PostMapping("/mark-read")
    @ResponseBody
    @RequirePermission
    @LogOperation("标记通知为已读")
    public Result<Void> markRead(@RequestBody NotificationDTO notificationDTO) {
        if (notificationDTO == null) {
            logger.error("通知参数为空");
            throw new BusinessException(ResultCode.PARAMETER_ERROR);
        }

        if (notificationDTO.getNoticeIdList() == null || notificationDTO.getNoticeIdList().isEmpty()) {
            logger.error("通知列表为空");
            throw new BusinessException(ResultCode.PARAMETER_ERROR);
        }

        if (StringUtils.isBlank(notificationDTO.getReceiverId())) {
            logger.error("通知接收对象为空");
            throw new BusinessException(ResultCode.PARAMETER_ERROR);
        }

        List<String> noticeIdList = notificationDTO.getNoticeIdList();
        boolean legal = noticeIdList.stream().allMatch(IdValidator::isValid);
        if (!legal) {
            logger.error("存在不合规的通知ID");
            throw new BusinessException(ResultCode.PARAMETER_ERROR);
        }

        notificationService.markRead(notificationDTO);

        return Result.success();
    }


    // 校验参数是否合规
    private void validateNotificationQuery(NotificationQuery query) {
        // 校验参数是否为空
        if (query == null) {
            logger.error("通知查询参数不合规, query: {}", query);
            throw new BusinessException(ResultCode.PARAMETER_ERROR);
        }

        // 校验角色
        if (StringUtils.isNotBlank(query.getTargetType())) {
            boolean validUserRole = UserRole.isValidRole(query.getTargetType().toLowerCase());
            boolean validJobRole = JobRole.isValidRole(query.getTargetType().toLowerCase());

            if (!(validJobRole || validUserRole)) {
                logger.error("无效的角色, role: {}", query.getTargetType());
                throw new BusinessException(ResultCode.PARAMETER_ERROR);
            }
        }

        // 校验通知范围
        if (StringUtils.isNotBlank(query.getTargetScope())) {
            boolean validTargetScope = TargetScope.isValidRole(query.getTargetScope().toLowerCase());
            if (!validTargetScope) {
                logger.error("无效的通知范围, targetScope: {}", query.getTargetScope());
                throw new BusinessException(ResultCode.PARAMETER_ERROR);
            }
        }

        // 校验创建时间范围
        Date createTimeStart = query.getCreateTimeStart();
        Date createTimeEnd = query.getCreateTimeEnd();
        if (createTimeStart != null && createTimeEnd != null) {
            if (createTimeStart.after(createTimeEnd)) {
                logger.error("无效的创建时间范围，createTimeStart:{}, createTimeEnd:{}", createTimeStart, createTimeEnd);
                throw new BusinessException(ResultCode.PARAMETER_ERROR);
            }
        }

        // 校验阅读状态
        String readStatus = query.getReadStatus();
        Set<String> readStatusSet = Set.of("UNREAD", "READ", "ALL");
        if (StringUtils.isNotBlank(readStatus)) {
            if(!readStatusSet.contains(readStatus.toUpperCase())) {
                logger.error("通知的阅读状态不合规, readStatus: {}", readStatus);
                throw new BusinessException(ResultCode.PARAMETER_ERROR);
            }
        }
    }



}