package com.tianhai.warn.controller;

import com.tianhai.warn.annotation.LogOperation;
import com.tianhai.warn.annotation.RequirePermission;
import com.tianhai.warn.constants.Constants;
import com.tianhai.warn.dto.NotificationDTO;
import com.tianhai.warn.enums.*;
import com.tianhai.warn.exception.BusinessException;
import com.tianhai.warn.model.Notification;
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

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    /**
     * 管理员简化版发布通知接口
     * - 若 targetId 非空：视为发送给单个目标用户
     * - 若 targetId 为空：视为发送给全体用户（ALL_USERS）
     */
    @PostMapping("/admin/publish")
    @ResponseBody
    @RequirePermission(roles = { Constants.SYSTEM_USER, Constants.SUPER_ADMIN })
    @LogOperation("管理员发布通知（简化版）")
    public Result<?> adminPublish(@RequestBody AdminPublishRequest request) {
        if (request == null) {
            logger.error("管理员发布通知参数不能为空");
            throw new BusinessException(ResultCode.PARAMETER_ERROR);
        }

        String title = request.getTitle();
        String content = request.getContent();
        String targetId = request.getTargetId();

        if (StringUtils.isBlank(title) || StringUtils.isBlank(content)) {
            logger.error("管理员发布通知标题或内容为空, title: {}, content: {}", title, content);
            throw new BusinessException(ResultCode.PARAMETER_ERROR);
        }

        Notification notification = new Notification();
        notification.setNoticeId(NoticeIdGenerator.generate());
        notification.setTitle(title.trim());
        notification.setContent(content.trim());

        Date now = new Date();
        notification.setCreateTime(now);
        notification.setUpdateTime(now);

        // targetId 非空：单个目标用户；为空：全体
        if (StringUtils.isNotBlank(targetId)) {
            notification.setTargetId(targetId.trim());
            notification.setTargetScope("SPECIAL_USER");
        } else {
            notification.setTargetId(null);
            notification.setTargetScope("ALL_USERS");
        }

        // 可选：统一设定为系统通知
        notification.setNoticeType("系统通知");

        notificationService.insert(notification);

        return Result.success();
    }

    /**
     * 管理员分页查询通知列表（支持按内容模糊搜索）
     * 展示字段包括：noticeId, title, content, targetId, updateTime 等
     */
    @PostMapping("/admin/page")
    @ResponseBody
    @RequirePermission(roles = { Constants.SYSTEM_USER, Constants.SUPER_ADMIN })
    @LogOperation("管理员分页查询通知列表")
    public Result<PageResult<Notification>> getAdminPageList(@RequestBody NotificationQuery query) {
        if (query == null) {
            logger.error("管理员查询通知参数不能为空");
            throw new BusinessException(ResultCode.PARAMETER_ERROR);
        }

        // 规范化分页参数
        PageUtils.normalizePageNums(query);

        // 使用 PageHelper 进行分页查询，支持 contentLike 等条件
        com.github.pagehelper.PageHelper.startPage(query.getPageNum(), query.getPageSize());
        List<Notification> notificationList = notificationService.selectByCondition(query);

        PageResult<Notification> pageResult = PageUtils.buildPageResult(notificationList);

        return Result.success(pageResult);
    }

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

    // 简化版：分页获取通知（仅根据targetId查询，返回createTime和content）
    @PostMapping("/simple/page")
    @ResponseBody
    @RequirePermission
    @LogOperation("简化版分页获取通知")
    public Result<PageResult<Notification>> getSimplePageList(@RequestBody NotificationQuery query) {
        // 校验参数
        if (query == null) {
            logger.error("查询参数不能为空");
            throw new BusinessException(ResultCode.PARAMETER_ERROR);
        }

        // 校验targetId
        if (StringUtils.isBlank(query.getTargetId())) {
            logger.error("targetId不能为空");
            throw new BusinessException(ResultCode.PARAMETER_ERROR);
        }

        // 规范化分页参数
        PageUtils.normalizePageNums(query);

        // 使用PageHelper进行分页查询
        com.github.pagehelper.PageHelper.startPage(query.getPageNum(), query.getPageSize());

        // --- 原逻辑：仅按 targetId 精确匹配 notification.target_id（不包含全体用户广播）---
        // NotificationQuery notificationQuery = NotificationQuery.builder()
        //         .targetId(query.getTargetId())
        //         .build();
        // List<Notification> notificationList = notificationService.selectByCondition(notificationQuery);

        // 新逻辑：定向给该 targetId 的记录，或 target_scope 为全体用户（ALL_USERS / 历史 allusers）
        List<Notification> notificationList = notificationService.selectSimplePageForTarget(query.getTargetId());

        // 构建分页结果
        PageResult<Notification> pageResult = PageUtils.buildPageResult(notificationList);

        return Result.success(pageResult);
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
    @RequirePermission(roles = { Constants.SYSTEM_USER, Constants.SUPER_ADMIN })
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
        NotificationSendMode sendMode = hasReceiverIdList ? NotificationSendMode.RECEIVER_ID_LIST
                : NotificationSendMode.TARGET_TYPE;

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
            if (role != null) {
                role = role.trim();
            }
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
    @RequirePermission(roles = { Constants.SYSTEM_USER, Constants.SUPER_ADMIN })
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
        boolean hasNotificationIdList = notificationDTO.getNoticeIdList() != null
                && !notificationDTO.getNoticeIdList().isEmpty();
        boolean hasDelTimeRange = notificationDTO.getCreateTimeStart() != null
                && notificationDTO.getCreateTimeEnd() != null;
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

    // --------------------

    // 简化业务 放弃使用ES
    /*
     *
     * {
     * "targetId":"2420710220XS",
     * "targetType":"student",
     * "targetScope":"specialUser",
     * "title":"es建立索引测试2",
     * "noticeId": "NT20250728345850",
     * "noticeType":"系统通知",
     * "content":"注意了，最近学校对晚归这方面要求非常严格，请大家合理安排时间出行"
     * }
     *
     */
    @PutMapping("/es/pre")
    @ResponseBody
    @RequirePermission
    @LogOperation("执行建立索引前的预处理")
    public Result<?> preEs(@RequestParam String indexName, @RequestParam String className) {
        // notificationService.createIndex(indexName, className);

        return Result.success();
    }

    // 简化业务 放弃使用ES
    @PostMapping("/es/index")
    @ResponseBody
    @RequirePermission
    @LogOperation("建立ES索引")
    public Result<?> buildEsIndex(@RequestBody Notification notification) {
        // 此控制层接口在开发环境下简便测试（未来使用单元测试和集成测试代替）
        // if (!ProfileUtils.isProfileActive("dev")) {
        // logger.error("当前环境不允许直接建立ES索引");
        // throw new BusinessException(ResultCode.FORBIDDEN);
        // }
        //
        // if (StringUtils.isBlank(notification.getNoticeId())) {
        // notification.setNoticeId(NoticeIdGenerator.generate());
        // }
        //
        // notificationService.indexNotification(notification);

        return Result.success();
    }

    // 简化业务 放弃使用ES
    @PostMapping("/es/index-batch")
    @ResponseBody
    @LogOperation("批量建立ES索引")
    public Result<?> buildBatchEsIndex(@RequestBody List<Notification> notificationList) {
        // // 此控制层接口在开发环境下简便测试（未来使用单元测试和集成测试代替）
        // if (!ProfileUtils.isProfileActive("dev")) {
        // logger.error("当前环境不允许直接建立ES索引");
        // throw new BusinessException(ResultCode.FORBIDDEN);
        // }
        //
        // notificationService.indexNotificationList(notificationList);

        return Result.success();
    }

    // 简化业务 放弃使用ES
    @PutMapping("/es/rebuild")
    @ResponseBody
    @RequirePermission
    @LogOperation("重建ES索引")
    public Result<?> rebuildEsIndex() {
        // // 此控制层接口在开发环境下简便测试
        // if (!ProfileUtils.isProfileActive("dev")) {
        // logger.error("当前环境不允许直接重建ES索引");
        // throw new BusinessException(ResultCode.FORBIDDEN);
        // }
        //
        // notificationService.rebuildIndex("notification", "Notification");

        return Result.success();
    }

    // 简化业务 放弃使用ES
    @DeleteMapping
    @ResponseBody
    @RequirePermission
    @LogOperation("删除ES索引")
    public Result<?> deleteEsIndex(@RequestParam String notificationId) {
        // if (StringUtils.isBlank(notificationId)) {
        // logger.error("通知业务id不能为空");
        // throw new BusinessException(ResultCode.PARAMETER_ERROR);
        // }
        //
        // // 此控制层接口在开发环境下简便测试
        // if (!ProfileUtils.isProfileActive("dev")) {
        // logger.error("当前环境不允许直接删除ES索引");
        // throw new BusinessException(ResultCode.FORBIDDEN);
        // }
        //
        // notificationService.deleteEsIndexByNotId(notificationId);

        return Result.success();
    }
    // --------------------

    // 简化业务 放弃使用ES
    @PostMapping("/es/search-page")
    @ResponseBody
    @RequirePermission
    @LogOperation("通过ES分页查询通知")
    public Result<PageResult<NotificationVO>> searchPageListByEs(@RequestBody NotificationQuery notificationQuery) {
        // // 参数校验
        // checkEsQueryWrapper(notificationQuery);
        //
        // PageResult<NotificationVO> pageResult =
        // notificationService.searchNotificationListPageByEs(notificationQuery);
        //
        // return Result.success(pageResult);
        return Result.success();
    }

    // 校验ES查询参数
    private void checkEsQueryWrapper(NotificationQuery notificationQuery) {
        if (notificationQuery == null) {
            logger.error("ES查询参数不能为空");
            throw new BusinessException(ResultCode.PARAMETER_ERROR);
        }

        // 规范化分页参数
        PageUtils.normalizePageNums(notificationQuery);

        // 判断searcherRole是否在允许的角色列表中
        String targetType = notificationQuery.getTargetType();
        if (RoleUtils.validateConcreteRole(targetType)) {
            logger.error("无效的查询角色, searcherRole: {}", targetType);
            throw new BusinessException(ResultCode.PARAMETER_ERROR);
        }

        // 校验检索人业务标识id
        String searcherNo = notificationQuery.getTargetId();
        if (StringUtils.isBlank(searcherNo)) {
            logger.error("检索人业务标识id不能为空");
            throw new BusinessException(ResultCode.PARAMETER_ERROR);
        }

        // 校验通知检索范围
        boolean contentLikeValidate = StringUtils.isBlank(notificationQuery.getContentLike());
        boolean titleLikeValidate = StringUtils.isBlank(notificationQuery.getTitleLike());
        boolean targetTypeValidate = StringUtils.isBlank(notificationQuery.getTargetType());
        if (contentLikeValidate && titleLikeValidate && targetTypeValidate) {
            logger.error("通知检索范围参数不能全为空, contentLike: {}, titleLike: {}, targetType: {}",
                    notificationQuery.getContentLike(), notificationQuery.getTitleLike(),
                    notificationQuery.getTargetType());

            throw new BusinessException(ResultCode.PARAMETER_ERROR);
        }
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
            if (!readStatusSet.contains(readStatus.toUpperCase())) {
                logger.error("通知的阅读状态不合规, readStatus: {}", readStatus);
                throw new BusinessException(ResultCode.PARAMETER_ERROR);
            }
        }
    }

    /**
     * 管理员发布通知请求体（简化版）
     */
    public static class AdminPublishRequest {
        private String title;
        private String content;
        /**
         * 目标业务ID（学号/工号等），为空则视为全体
         */
        private String targetId;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public String getTargetId() {
            return targetId;
        }

        public void setTargetId(String targetId) {
            this.targetId = targetId;
        }
    }

}