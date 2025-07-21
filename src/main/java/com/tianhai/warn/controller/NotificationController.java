package com.tianhai.warn.controller;

import com.tianhai.warn.annotation.LogOperation;
import com.tianhai.warn.annotation.RequirePermission;
import com.tianhai.warn.constants.Constants;
import com.tianhai.warn.dto.NotificationDTO;
import com.tianhai.warn.enums.*;
import com.tianhai.warn.exception.BusinessException;
import com.tianhai.warn.query.NotificationQuery;
import com.tianhai.warn.service.VerificationService;
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
    private VerificationService verificationService;

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

    // 发送通知
    @PostMapping("/send")
    @ResponseBody
    @RequirePermission(roles = {Constants.SYSTEM_USER, Constants.SUPER_ADMIN})
    @LogOperation("发送站内通知")
    public Result<?> send(@RequestBody NotificationDTO notificationDTO) {
        // 校验通知标题和通知和内容
        if (StringUtils.isBlank(notificationDTO.getTitle()) || StringUtils.isBlank(notificationDTO.getContent())) {
            logger.error("通知标题或内容不能为空");
            throw new BusinessException(ResultCode.PARAMETER_ERROR);
        }

        // 校验发送模式：receiverIdList 和 targetType 不能同时存在
        boolean hasReceiverIdList = notificationDTO.getReceiverIdList() != null &&
                !notificationDTO.getReceiverIdList().isEmpty();
        boolean hasTargetType = StringUtils.isNotBlank(notificationDTO.getTargetType());
        NotificationSendMode sendMode = hasReceiverIdList ?
                NotificationSendMode.RECEIVER_ID_LIST : NotificationSendMode.TARGET_TYPE;

        if (hasReceiverIdList && hasTargetType) {
            logger.error("receiverIdList和targetType同时存在，存在冲突");
            throw new BusinessException(ResultCode.PARAMETER_ERROR);
        }

        if (!hasReceiverIdList && !hasTargetType) {
            logger.error("receiverIdList和targetType不能同时为空");
            throw new BusinessException(ResultCode.PARAMETER_ERROR);
        }

        // 如果使用targetType，则校验其合法性
        if (hasTargetType) {
            String role = notificationDTO.getTargetType();
            if (role != null) { role = role.trim(); }
            boolean validTargetType = UserRole.isValidRole(role) || JobRole.isValidRole(role);

            if (!validTargetType) {
                logger.error("通知接收人角色不合规, targetType: {}", role);
                throw new BusinessException(ResultCode.PARAMETER_ERROR);
            }
        }

        // 校验用户是否有权限发通知
        verificationService.checkSysUserStatus();

        // 生成通知业务标识id
        notificationDTO.setNoticeId(NoticeIdGenerator.generate());

        // 发送通知
        Map<String, Set<String>> invalidReceiverIdMap = notificationService.sendNotification(notificationDTO, sendMode);

        return Result.success(invalidReceiverIdMap);
    }

    // 删除站内通知
    @DeleteMapping("/del")
    @ResponseBody
    @RequirePermission(roles = {Constants.SYSTEM_USER, Constants.SUPER_ADMIN})
    @LogOperation("删除站内通知")
    public Result<?> delete(@RequestBody NotificationDTO notificationDTO) {
        // 校验要删除的通知
        List<String> needDeleteNoticeIdList = notificationDTO.getNoticeIdList();
        if (needDeleteNoticeIdList != null && !needDeleteNoticeIdList.isEmpty()) {
            // 校验通知业务ID是否合法
            boolean patternLegal = needDeleteNoticeIdList.stream()
                    .allMatch(notId -> notId.startsWith("NT") && IdValidator.isValid(notId));
            if (!patternLegal) {
                logger.error("要删除的通知列表中存在不合法的业务id");
                throw new BusinessException(ResultCode.PARAMETER_ERROR);
            }
        }

        // 校验删除范围 要么根据通知业务id删除 要么使用时间范围删除 两者不可同时存在
        boolean hasNotificationIdList = notificationDTO.getNoticeIdList() != null && !notificationDTO.getNoticeIdList().isEmpty();
        boolean hasDelTimeRange = notificationDTO.getCreateTimeStart() != null && notificationDTO.getCreateTimeEnd() != null;
        if (hasNotificationIdList && hasDelTimeRange) {
            logger.error("通知删除范围不合规，不能同时使用通知业务id列表和时间范围");
            throw new BusinessException(ResultCode.PARAMETER_ERROR);
        }

        // 校验时间范围
        if (hasDelTimeRange) {
            if (notificationDTO.getCreateTimeStart().after(notificationDTO.getCreateTimeEnd())) {
                logger.error("通知删除的时间范围不合法, startDate: {}, endDate:{}",
                        notificationDTO.getCreateTimeStart(), notificationDTO.getCreateTimeEnd());
                throw new BusinessException(ResultCode.PARAMETER_ERROR);
            }
        }

        if (hasDelTimeRange) {
            notificationDTO.setNoticeIdList(null);
        } else {
            notificationDTO.setCreateTimeStart(null);
            notificationDTO.setCreateTimeEnd(null);
        }

        notificationService.deleteBatch(notificationDTO);

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