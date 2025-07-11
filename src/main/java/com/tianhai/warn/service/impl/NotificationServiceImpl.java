package com.tianhai.warn.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.tianhai.warn.constants.Constants;
import com.tianhai.warn.dto.NotificationDTO;
import com.tianhai.warn.enums.JobRole;
import com.tianhai.warn.enums.ResultCode;
import com.tianhai.warn.enums.TargetScope;
import com.tianhai.warn.exception.BusinessException;
import com.tianhai.warn.exception.SystemException;
import com.tianhai.warn.mapper.NotificationMapper;
import com.tianhai.warn.model.*;
import com.tianhai.warn.query.NotificationQuery;
import com.tianhai.warn.query.NotificationReceiverQuery;
import com.tianhai.warn.service.*;
import com.tianhai.warn.utils.NoticeIdGenerator;
import com.tianhai.warn.utils.PageResult;
import com.tianhai.warn.utils.PageUtils;
import com.tianhai.warn.utils.ProfileUtils;
import com.tianhai.warn.vo.NotificationVO;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * 通知信息服务实现类
 */
@Service
public class NotificationServiceImpl implements NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationServiceImpl.class);

    @Autowired
    private NotificationMapper notificationMapper;

    @Autowired
    private NotificationReceiverService notificationReceiverService;

    @Autowired
    private VerificationService verificationService;

    @Autowired
    private StudentService studentService;

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private DormitoryManagerService dormitoryManagerService;

    @Autowired
    private ReceiverIdProviderRegistry receiverIdProviderRegistry;

    @Autowired
    private AsyncTaskExecutor asyncTaskExecutor;

    // 分页查询属于某个特定用户的通知
    @Override
    public PageResult<NotificationVO> searchSpecialUserPageList(NotificationQuery query) {
        // 规范化分页参数
        PageUtils.normalizePageNums(query);

        // 保存原始查询参数
        String originalTargetId = query.getTargetId();
        String originalTargetType = query.getTargetType();
        String originalReadStatus = query.getReadStatus(); // 这里后续业务代码会改变query的状态 所以保存请求的数据

        // 因为不是单独查询某条通知记录，所以手动补充设置noticeId为空
        query.setNoticeId(null);

        // 获取三种不同类型的通知
        List<Notification> allUserNotifications = getAllUserNotifications(query);
        List<Notification> specialRoleNotifications = getSpecialRoleNotifications(query, originalTargetType);
        List<Notification> specialUserNotifications = getSpecialUserNotifications(query, originalTargetId,
                originalTargetType);

        // 合并通知列表
        List<Notification> allNotifications = combineNotificationLists(
                allUserNotifications, specialRoleNotifications, specialUserNotifications);

        // 根据读取状态处理结果
        Map<String, PageResult<NotificationVO>> pageResultMap =
                processNotByReadStatusSync(allNotifications, query);

        // 调试
        if (ProfileUtils.isProfileActive("dev") || ProfileUtils.isProfileActive("debug")) {
            // 开发环境下打印pageResultMap的内容
            pageResultMap.forEach((status, pageResult) -> {
                logger.info("所有阅读状态的通知的分页结果：Read Status: {}, Total: {}, PageNum: {}, PageSize: {}",
                        status, pageResult.getTotal(), pageResult.getPageNum(), pageResult.getPageSize());
                logger.info("打印 {} 状态的通知数据： {}", status, Arrays.toString(pageResult.getData().toArray()));
            });

            // 打印查询参数和Map的key
            logger.info("查询参数中的readStatus: '{}'", originalReadStatus);
            logger.info("Map中的所有key: {}", pageResultMap.keySet());

            // 打印pageResultMap.get(query.getReadStatus())的内容
            PageResult<NotificationVO> result = pageResultMap.get(query.getReadStatus());
            if (result != null) {
                logger.info("查询条件下的通知的分页结果：{}", result.getData());
            } else {
                logger.info("没有找到对应的通知结果，Read Status: {}", query.getReadStatus());
            }
        }

        return pageResultMap.get(originalReadStatus);
    }

    /**
     * 获取全体用户类型的通知
     */
    private List<Notification> getAllUserNotifications(NotificationQuery query) {
        query.setTargetId(null);
        query.setTargetType(null);
        query.setTargetScope(TargetScope.ALL_USERS.getCode());

        return notificationMapper.selectByQuery(query);
    }

    /**
     * 获取特定角色类型的通知
     */
    private List<Notification> getSpecialRoleNotifications(NotificationQuery query, String targetType) {
        query.setTargetId(null);
        query.setTargetType(targetType);
        query.setTargetScope(TargetScope.SPECIAL_ROLE.getCode());

        return notificationMapper.selectByQuery(query);
    }

    /**
     * 获取特定用户类型的通知
     */
    private List<Notification> getSpecialUserNotifications(NotificationQuery query, String targetId,
            String targetType) {
        query.setTargetId(targetId);
        query.setTargetType(targetType);
        query.setTargetScope(TargetScope.SPECIAL_USER.getCode());

        return notificationMapper.selectByQuery(query);
    }

    /**
     * 合并三个通知列表
     */
    private List<Notification> combineNotificationLists(
            List<Notification> listOne,
            List<Notification> listTwo,
            List<Notification> listThree) {
        int totalListSize = listOne.size() + listTwo.size() + listThree.size();
        List<Notification> combined = new ArrayList<>(totalListSize);
        combined.addAll(listOne);
        combined.addAll(listTwo);
        combined.addAll(listThree);

        return combined;
    }

    /**
     * 根据读取状态同步处理通知列表
     */
    private Map<String, PageResult<NotificationVO>> processNotByReadStatusSync(
            List<Notification> notificationList,
            NotificationQuery query) {

        // 获取已读状态的NotificationVO列表
        PageResult<NotificationVO> readPageResult = buildPageResultByReadStatus("READ", notificationList, query);

        // 获取未读状态的NotificationVO列表
        PageResult<NotificationVO> unreadPageResult = buildPageResultByReadStatus("UNREAD", notificationList, query);

        // 合并未读和已读NotificationVO列表
        List<NotificationVO> combinedList = new ArrayList<>();
        combinedList.addAll(readPageResult.getData());
        combinedList.addAll(unreadPageResult.getData());

        // 手动实现分页
        PageResult<NotificationVO> readAndUnreadPageResult = buildPageResult(combinedList);

        // 构建返回Map
        Map<String, PageResult<NotificationVO>> resultMap = new HashMap<>();
        resultMap.put("READ", readPageResult);
        resultMap.put("UNREAD", unreadPageResult);
        resultMap.put("ALL", readAndUnreadPageResult);

        return resultMap;
    }

    /**
     * 根据读取状态异步处理通知列表
     */
    private Map<String, PageResult<NotificationVO>> processNotByReadStatusAsync(List<Notification> notificationList,
            NotificationQuery query) {

        // 异步获取已读状态的NotificationVO列表
        CompletableFuture<PageResult<NotificationVO>> readFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return buildPageResultByReadStatus("READ", notificationList, query);
            } catch (Exception e) {
                logger.error("异步获取已读状态的NotificationVO列表失败", e);
                return null; // 如果失败则返回空的分页结果
            }
        }, asyncTaskExecutor);

        // 异步获取未读状态的NotificationVO列表
        CompletableFuture<PageResult<NotificationVO>> unreadFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return buildPageResultByReadStatus("UNREAD", notificationList, query);
            } catch (Exception e) {
                logger.error("异步获取未读状态的NotificationVO列表失败", e);
                return null; // 如果失败则返回空的分页结果
            }
        }, asyncTaskExecutor);

        // 合并未读和已读NotificationVO列表
        CompletableFuture<Map<String, PageResult<NotificationVO>>> combinedFuture = readFuture.thenCombine(unreadFuture,
                (readResult, unreadResult) -> {
                    Map<String, PageResult<NotificationVO>> resultMap = new HashMap<>();
                    List<NotificationVO> combinedList = new ArrayList<>();

                    if (readResult == null) {
                        resultMap.put("READ", PageUtils.emptyPageResult());
                    } else {
                        resultMap.put("READ", readResult);
                        combinedList.addAll(readResult.getData());
                    }

                    if (unreadResult == null) {
                        resultMap.put("UNREAD", PageUtils.emptyPageResult());
                    } else {
                        resultMap.put("UNREAD", unreadResult);
                        combinedList.addAll(unreadResult.getData());
                    }

                    PageResult<NotificationVO> allResult = buildPageResult(combinedList);
                    resultMap.put("ALL", allResult);

                    return resultMap;
                });

        try {
            // 最长等待5秒
            int thresholdTime = 5;
            return combinedFuture.get(thresholdTime, TimeUnit.SECONDS);
        } catch (TimeoutException te) {
            logger.error("获取通知列表超时", te);
            throw new SystemException(ResultCode.ERROR);
        } catch (Exception e) {
            logger.error("获取通知列表失败", e);
            throw new SystemException(ResultCode.ERROR);
        }
    }

    /**
     * 构建特定状态的额NotificationVO分页结果
     */
    private PageResult<NotificationVO> buildPageResultByReadStatus(String readStatus,
            List<Notification> notifications,
            NotificationQuery query) {
        boolean validReadStatus = "READ".equalsIgnoreCase(readStatus)
                || "UNREAD".equalsIgnoreCase(readStatus);
        if (!validReadStatus) {
            logger.error("query的readStatus不合规: {}", readStatus);
            throw new IllegalArgumentException("NotificationQuery的readStatus不合规");
        }

        query.setReadStatus(readStatus);
        List<Notification> filteredList = distinguishReadOrUnread(notifications, query);

        List<NotificationVO> notificationVOList = filteredList.stream()
                .map(notification -> {
                    NotificationVO notVO = new NotificationVO();
                    BeanUtils.copyProperties(notification, notVO);
                    notVO.setReadStatus(readStatus);
                    return notVO;
                })
                .toList();

        // 构造分页结果
        try (Page<NotificationVO> page = PageHelper.startPage(query.getPageNum(), query.getPageSize())) {
            return buildPageResult(notificationVOList);
        }
    }

    /**
     * 返回分页结果
     */
    private PageResult<NotificationVO> buildPageResult(List<NotificationVO> notificationVOList) {
        PageInfo<NotificationVO> pageInfo = new PageInfo<>(notificationVOList);

        PageResult<NotificationVO> result = new PageResult<>();
        result.setData(pageInfo.getList());
        result.setTotal((int) pageInfo.getTotal());
        result.setPageNum(pageInfo.getPageNum());
        result.setPageSize(pageInfo.getPageSize());

        return result;
    }

    /**
     * 结合notification_receiver表 筛选已读或未读的通知
     */
    private List<Notification> distinguishReadOrUnread(List<Notification> allNotificationList,
            NotificationQuery notQuery) {
        // 校验特定用户的信息
        String targetType = notQuery.getTargetType();
        String targetId = notQuery.getTargetId();
        if (StringUtils.isBlank(targetType) || StringUtils.isBlank(targetId)) {
            logger.error("若要筛选已读和未读状态，必须提供targetType和targetId! targetType:{}, targetId:{}", targetType, targetId);
            throw new BusinessException(ResultCode.PARAMETER_ERROR);
        }

        NotificationReceiverQuery notRecQuery = NotificationReceiverQuery.builder()
                .receiverId(notQuery.getTargetId())
                .receiverRole(notQuery.getTargetType())
                .readStatus(notQuery.getReadStatus())
                .build();

        // 获取符合条件的noticeId列表
        List<NotificationReceiver> notificationReceiverList = notificationReceiverService
                .selectByCondition(notRecQuery);
        List<String> requiredNoticeIdList = notificationReceiverList.stream()
                .map(NotificationReceiver::getNoticeId)
                .toList();

        // 获取符合阅读状态的notification列表
        return allNotificationList.stream()
                .filter(notification -> requiredNoticeIdList.contains(notification.getNoticeId()))
                .toList();
    }

    @Override
    public List<Notification> selectByCondition(NotificationQuery notificationQuery) {
        if (notificationQuery == null) {
            logger.error("查询参数不合规, notificationQuery: {}", notificationQuery);
            throw new BusinessException(ResultCode.PARAMETER_ERROR);
        }

        return notificationMapper.selectByQuery(notificationQuery);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insert(Notification notification) {
        if (notification.getNoticeId() == null) {
            notification.setNoticeId(NoticeIdGenerator.generate());
        }

        // 验证通知信息是否合规
        verificationService.validateNotification(notification);

        // 往notification插入通知信息
        int notInsertRow = notificationMapper.insert(notification);

        // 往notification_receiver插入通知接收信息
        int notRecRow = insertToNotificationReceiver(notification);

        logger.info("一共想notification和notification_receiver表插入： {} 行数据", notInsertRow + notInsertRow);

        return notInsertRow + notRecRow;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int markRead(NotificationDTO notificationDTO) {
        // 过滤掉不存在的noticeId
        List<String> dtoNoticeIdList = notificationDTO.getNoticeIdList();
        NotificationQuery notificationQuery = NotificationQuery.builder()
                .noticeIdList(dtoNoticeIdList)
                .build();

        List<Notification> existingNotificationList = notificationMapper.selectByQuery(notificationQuery);
        if (existingNotificationList.isEmpty()) {
            logger.error("没有找到对应的通知信息，无法标记为已读，noticeIdList: {}", dtoNoticeIdList);
            throw new BusinessException(ResultCode.NOTIFICATION_UPDATE_FAILED);
        }

        Set<String> existingNoticeIdSet = existingNotificationList.stream()
                .map(Notification::getNoticeId)
                .collect(Collectors.toSet());
        List<String> validNoticeIdList = dtoNoticeIdList.stream()
                .filter(existingNoticeIdSet::contains)
                .toList();
        if (validNoticeIdList.isEmpty()) {
            logger.info("不存在有效的noticeId");
            return 0;
        }

        // 更新notification_receiver表的阅读状态
        String targetId = notificationDTO.getReceiverId();
        String targetType = notificationDTO.getTargetType();
        NotificationReceiverQuery notRecQuery = NotificationReceiverQuery.builder()
                .receiverId(targetId)
                .receiverRole(targetType)
                .noticeIdList(validNoticeIdList)
                .build();

        List<NotificationReceiver> existingNotRecList = notificationReceiverService.selectByCondition(notRecQuery);

        if (validNoticeIdList.size() != existingNotRecList.size()) {
            logger.warn("存在部分通知接收记录未找到，可能是已被删除，validNoticeIdList: {}, existingNotRecList: {}",
                    validNoticeIdList, existingNotRecList);
        }

        Date now = new Date();
        existingNotRecList.forEach(notRec -> {
            notRec.setReadStatus("READ");
            notRec.setReadTime(now);
            notRec.setUpdateTime(now);
        });

        int markedCount = notificationReceiverService.updateBatch(existingNotRecList);
        logger.info("成功更新 {} 条通知接收信息", markedCount);

        return markedCount;
    }

    // 根据通知接收类型往notification_receiver插入对应数据
    private int insertToNotificationReceiver(Notification notification) {
        String targetScope = notification.getTargetScope();
        String noticeId = notification.getNoticeId();

        int insertRow = 0;

        if (targetScope.equalsIgnoreCase(TargetScope.SPECIAL_USER.getCode())) {
            // 1 特定用户
            insertRow = processSpecialUser(notification);
        } else if (targetScope.equalsIgnoreCase(TargetScope.SPECIAL_ROLE.getCode())) {
            // 2 特定角色（如果是班级管理员 则具体到职位角色）
            insertRow = processSpecialRole(notification);
        } else if (targetScope.equalsIgnoreCase(TargetScope.ALL_USERS.getCode())) {
            // 3 全体用户
            CompletableFuture.supplyAsync(() -> {
                return processAllUsers(notification);
            }, asyncTaskExecutor);
        }

        logger.info("成功插入通知接收记录，noticeId: {}, targetScope: {}, 插入行数: {}",
                noticeId, targetScope, insertRow);

        return insertRow;
    }

    // 处理特定用户的插入
    private int processSpecialUser(Notification notification) {
        NotificationReceiver notRec = new NotificationReceiver();
        notRec.setNoticeId(notification.getNoticeId());
        notRec.setReceiverId(notification.getTargetId());
        notRec.setReceiverRole(notification.getTargetType());
        notRec.setReadStatus("UNREAD");
        notRec.setCreateTime(new Date());

        return notificationReceiverService.insert(notRec);
    }

    // 处理特定角色的插入
    private int processSpecialRole(Notification notification) {
        String targetType = notification.getTargetType();
        String targetScope = notification.getTargetScope();
        String noticeId = notification.getNoticeId();
        int insertRow = 0;

        // 获取所有班级管理员用户列表
        // -- 因为辅导员 班主任 院系领导角色都在sys_user表中，
        // 所以需要查询该表并按职位角色进行分类，从而得到具体角色的业务标识id列表
        Map<String, List<String>> jobRoleToSysUserNoListMap = new HashMap<>();
        if (targetScope.equalsIgnoreCase(TargetScope.SPECIAL_ROLE.getCode())) {
            List<SysUser> sysUserList = sysUserService.selectAll();
            jobRoleToSysUserNoListMap = sysUserList.stream().collect(
                    Collectors.groupingBy(SysUser::getJobRole,
                            Collectors.mapping(SysUser::getSysUserNo, Collectors.toList())));
        }

        // 获取学生角色的所有用户的业务标识id
        if (targetType.equalsIgnoreCase(Constants.STUDENT)) {
            List<String> studentNoList = studentService.selectAll().stream()
                    .map(Student::getStudentNo)
                    .toList();
            // 批量生成通知接收记录
            List<NotificationReceiver> notificationReceiverList = buildNotRecList(studentNoList, noticeId,
                    Constants.STUDENT);
            insertRow = notificationReceiverService.insertBatch(notificationReceiverList);
        }

        // 获取宿管角色的所有用户的业务标识id
        if (targetType.equalsIgnoreCase(Constants.DORMITORY_MANAGER)) {
            List<String> dorManNoList = dormitoryManagerService.selectAll().stream()
                    .map(DormitoryManager::getManagerId)
                    .toList();
            // 批量生成通知接收记录
            List<NotificationReceiver> notificationReceiverList = buildNotRecList(dorManNoList, noticeId,
                    Constants.DORMITORY_MANAGER);
            insertRow = notificationReceiverService.insertBatch(notificationReceiverList);
        }

        // 获取辅导员角色的所有用户的业务标识id
        if (targetType.equalsIgnoreCase(Constants.JOB_ROLE_COUNSELOR)) {
            List<String> counselorNoList = jobRoleToSysUserNoListMap.get(JobRole.COUNSELOR.getCode());
            // 批量生成通知接收记录
            List<NotificationReceiver> notificationReceiverList = buildNotRecList(counselorNoList, noticeId,
                    Constants.JOB_ROLE_COUNSELOR);
            insertRow = notificationReceiverService.insertBatch(notificationReceiverList);
        }

        // 获取班主任角色的所有用户的业务标识id
        if (targetType.equalsIgnoreCase(Constants.JOB_ROLE_CLASS_TEACHER)) {
            List<String> classTeacherNoList = jobRoleToSysUserNoListMap.get(JobRole.CLASS_TEACHER.getCode());
            // 批量生成通知接收记录
            List<NotificationReceiver> notificationReceiverList = buildNotRecList(classTeacherNoList, noticeId,
                    Constants.JOB_ROLE_CLASS_TEACHER);
            insertRow = notificationReceiverService.insertBatch(notificationReceiverList);
        }

        // 获取院系领导角色的所有用户的业务标识id
        if (targetType.equalsIgnoreCase(Constants.JOB_ROLE_DEAN)) {
            List<String> deanNoList = jobRoleToSysUserNoListMap.get(JobRole.DEAN.getCode());
            // 批量生成通知接收记录
            List<NotificationReceiver> notificationReceiverList = buildNotRecList(deanNoList, noticeId,
                    Constants.JOB_ROLE_DEAN);
            insertRow = notificationReceiverService.insertBatch(notificationReceiverList);
        }

        return insertRow;
    }

    // 处理所有用户的插入
    private int processAllUsers(Notification notification) {
        String noticeId = notification.getNoticeId();

        // 获取所有用户角色的业务标识id
        Map<String, List<String>> roleToReceiverIdListMap = receiverIdProviderRegistry.getAllRoleReceiverIdMap();

        List<NotificationReceiver> stuNotRecList = buildNotRecList(
                roleToReceiverIdListMap.get(Constants.STUDENT), noticeId, Constants.STUDENT);

        List<NotificationReceiver> dorManNotRecList = buildNotRecList(
                roleToReceiverIdListMap.get(Constants.DORMITORY_MANAGER), noticeId, Constants.DORMITORY_MANAGER);

        List<NotificationReceiver> sysUserNotRecList = buildNotRecList(
                roleToReceiverIdListMap.get(Constants.SYSTEM_USER), noticeId, Constants.SYSTEM_USER);

        logger.info("开始执行所有用户的通知接收信息插入");

        // 批量插入通知接收记录
        int stuInsertRows = notificationReceiverService.insertBatch(stuNotRecList);
        logger.info("批量插入学生通知接收记录，插入行数: {}", stuInsertRows);

        int dorManInsertRows = notificationReceiverService.insertBatch(dorManNotRecList);
        logger.info("批量插入宿管通知接收记录，插入行数: {}", dorManInsertRows);

        int sysUserInsertRows = notificationReceiverService.insertBatch(sysUserNotRecList);
        logger.info("批量插入系统用户通知接收记录，插入行数: {}", sysUserInsertRows);

        int totalInsertRows = stuInsertRows + dorManInsertRows + sysUserInsertRows;
        logger.info("已成功插入通知接收记录，noticeId: {}, targetScope: {}, 插入行数: {}",
                noticeId, notification.getTargetScope(), totalInsertRows);

        return totalInsertRows;
    }

    // 构建通知接收对象列表
    private List<NotificationReceiver> buildNotRecList(List<String> receiverIdList,
            String noticeId,
            String receiverRole) {
        if (receiverIdList == null || receiverIdList.isEmpty()) {
            return new ArrayList<>();
        }

        Date now = new Date();

        return receiverIdList.stream()
                .map(receiverId -> {
                    NotificationReceiver notRec = new NotificationReceiver();
                    notRec.setNoticeId(noticeId);
                    notRec.setReceiverId(receiverId);
                    notRec.setReceiverRole(receiverRole);
                    notRec.setReadStatus("UNREAD");
                    notRec.setCreateTime(now);

                    return notRec;
                })
                .toList();
    }

}