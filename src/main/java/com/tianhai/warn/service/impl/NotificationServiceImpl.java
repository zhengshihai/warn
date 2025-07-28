package com.tianhai.warn.service.impl;


import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.PageInfo;
import com.tianhai.warn.constants.Constants;
import com.tianhai.warn.dto.NotificationDTO;
import com.tianhai.warn.enums.*;
import com.tianhai.warn.exception.BusinessException;
import com.tianhai.warn.exception.SystemException;
import com.tianhai.warn.mapper.NotificationMapper;
import com.tianhai.warn.mapper.NotificationReceiverMapper;
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

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 通知信息服务实现类
 */
@Service
public class NotificationServiceImpl implements NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private static final String invalidRole = "invalidRole";
    private static final String INDEX_NAME = "notifications";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

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

    @Autowired
    private NotificationReceiverMapper notificationReceiverMapper;

    @Autowired
    private ElasticsearchClient elasticsearchClient;

    @Autowired
    private ObjectMapper objectMapper;

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
        Map<String, PageResult<NotificationVO>> pageResultMap = processNotByReadStatusSync(allNotifications, query);

        // 通过logger调试
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

        // 手动实现内存分页
        int total = notificationVOList.size();
        int pageNum = query.getPageNum();
        int pageSize = query.getPageSize();

        // 计算分页起始和结束索引
        int startIndex = (pageNum - 1) * pageSize;
        int endIndex = Math.min(startIndex + pageSize, total);

        // 如果起始索引超出范围，返回空结果
        if (startIndex >= total) {
            PageResult<NotificationVO> emptyResult = new PageResult<>();
            emptyResult.setData(new ArrayList<>());
            emptyResult.setTotal(total);
            emptyResult.setPageNum(pageNum);
            emptyResult.setPageSize(pageSize);
            return emptyResult;
        }

        // 截取当前页的数据
        List<NotificationVO> pageData = notificationVOList.subList(startIndex, endIndex);

        // 构造分页结果
        PageResult<NotificationVO> result = new PageResult<>();
        result.setData(pageData);
        result.setTotal(total);
        result.setPageNum(pageNum);
        result.setPageSize(pageSize);

        return result;
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

        logger.info("一共向notification和notification_receiver表插入： {} 行数据", notInsertRow + notInsertRow);

        // 索引到Elasticsearch
        try {
            indexNotification(notification);
        } catch (Exception e) {
            logger.error("无法索引通知信息到Elasticsearch，可能是索引创建失败或网络问题", e);
        }

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
            List<SysUser> sysUserList = sysUserService.selectAllSysUserNoAndJobRole();
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Set<String>> sendNotification(NotificationDTO notificationDTO, NotificationSendMode sendMode) {
        // 构造无效的通知对象业务标识Map
        Map<String, Set<String>> invalidReceiverIdMap = new ConcurrentHashMap<>();

        // 如果采用receiverIDList的方式发送消息，则需要预处理形式上无效的receiverId
        if (notificationDTO.getReceiverIdList() != null && !notificationDTO.getReceiverIdList().isEmpty()) {
            List<String> validRoleReceiverIdList = filterReceiverIdList(notificationDTO.getReceiverIdList(),
                    invalidReceiverIdMap);
            notificationDTO.setNoticeIdList(validRoleReceiverIdList);
        }

        switch (sendMode) {
            case RECEIVER_ID_LIST -> {
                return sendWithReceiverIdList(notificationDTO, invalidReceiverIdMap);
            }

            case TARGET_TYPE -> {
                return sendWithTargetType(notificationDTO, invalidReceiverIdMap);
            }

            default -> throw new SystemException("暂时不支持该发送方式");
        }
    }

    // 通过receiverIdList发送
    // -- receiverId是包含不同的用户角色（学生 宿管 班级管理员），
    // 而且对这些receiverId所属的角色进行辨别，
    // 实际业务中应该让前端按角色，让用户分类导入，从而减少后端业务的复杂度
    protected Map<String, Set<String>> sendWithReceiverIdList(NotificationDTO notificationDTO,
                                                              Map<String, Set<String>> invalidReceiverIdMap) {
        // 执行发送通知
        doSendWithReceiverIdList(notificationDTO, invalidReceiverIdMap);

        // 调试
        invalidReceiverIdMap.forEach((key, valueSet) -> {
            logger.info("ReceiverIds 中存在部分无效receiverId, Key = {}, Value = {}", key, valueSet);
        });

        return invalidReceiverIdMap;
    }

    // 预处理receiverIdList
    // InvalidReceiverIdMap中，有以角色（具体到职位角色）作为Key，
    // 另外还额外设立一个不属于任何角色的Key，该Key名为invalidRole，
    // 注：这里的预处理仅是从正则表达式进行预处理过滤，而非结合数据库校验该用户是否存在。后续不同角色的无效receiverId会单独设置
    private List<String> filterReceiverIdList(List<String> receiverIdList,
                                              Map<String, Set<String>> invalidReceiverIdMap) {
        logger.info("开始预处理receiverIdList列表");

        // 形式上合规的receiverId集合
        Set<String> validRoleReceiverIdList = receiverIdList.stream()
                .filter(receiverId -> Arrays.stream(RoleMatcher.values())
                        .anyMatch(roleMatcher -> roleMatcher != RoleMatcher.ILLEGAL
                                && roleMatcher.matcher.test(receiverId)))
                .collect(Collectors.toSet());

        // 形式上不合规的receiverId集合
        Set<String> invalidRoleReceiverIdSet = receiverIdList.stream()
                .filter(receiverId -> Arrays.stream(RoleMatcher.values())
                        .noneMatch(roleMatcher -> roleMatcher != RoleMatcher.ILLEGAL
                                && roleMatcher.matcher.test(receiverId)))
                .collect(Collectors.toSet());

        invalidReceiverIdMap.put(invalidRole, invalidRoleReceiverIdSet);

        return validRoleReceiverIdList.stream().toList();
    }

    /**
     * 根据receiverIdList发送通知
     *
     * @param notificationDTO 通知对象DTO
     */
    private void doSendWithReceiverIdList(NotificationDTO notificationDTO,
                                          Map<String, Set<String>> invalidReceiverIdMap) {

        // 获取不同角色的业务标识Id集合
        Map<String, Set<String>> roleToListMap = distinguishReceiverIdByRole(
                new HashSet<>(notificationDTO.getReceiverIdList()));

        // 异步处理学生角色
        CompletableFuture.runAsync(
                () -> sendStuNotification(notificationDTO, roleToListMap, invalidReceiverIdMap),
                asyncTaskExecutor);

        // 同步处理宿管角色
        sendDorManNotification(notificationDTO, roleToListMap, invalidReceiverIdMap);

        // 同步处理班级管理员角色
        sendSysUserNotification(notificationDTO, roleToListMap, invalidReceiverIdMap);
    }

    // 按照学生 宿管 班级管理员划分receiverIdList //todo 这里要设置一个不属于任何角色的key
    private Map<String, Set<String>> distinguishReceiverIdByRole(Set<String> dtoReceiverIdSet) {
        return Arrays.stream(RoleMatcher.values())
                .collect(Collectors.toMap(
                        RoleMatcher::getRole,
                        roleMatcher -> roleMatcher.filter(dtoReceiverIdSet)));
    }

    // 处理学生角色
    private void sendStuNotification(NotificationDTO notificationDTO,
                                     Map<String, Set<String>> roleToListMap,
                                     Map<String, Set<String>> invalidReceiverIdMap) {
        Date now = new Date();

        // 获取导入的接收通知的学生业务标识id集合
        Set<String> stuReceiverIdInputSet = roleToListMap.getOrDefault(RoleMatcher.STUDENT.getRole(), Set.of());

        if (!stuReceiverIdInputSet.isEmpty()) {
            // 获取数据库中学生业务标识id集合
            Set<String> stuReceiverIdDBSet = studentService.selectAllStudentNo();

            // 过滤掉无效的学生业务标识id
            Set<String> validStuReceiverIdSet = filterValidReceiverIds(stuReceiverIdInputSet, stuReceiverIdDBSet);

            // 将无效的学生学号加入invalidReceiverIdMap
            if (validStuReceiverIdSet.size() < stuReceiverIdInputSet.size()) {
                addToInvalidReceiverIdMap(invalidReceiverIdMap,
                        validStuReceiverIdSet,
                        stuReceiverIdInputSet,
                        Constants.STUDENT);
            }

            if (validStuReceiverIdSet.isEmpty()) {
                logger.info("导入的通知对象中，没有有效的学生学号studentNo");
                return;
            } else {
                List<Notification> stuNotificationList = buildNotificationList(validStuReceiverIdSet,
                        notificationDTO, Constants.STUDENT, now);
                batchInsertNotifications(stuNotificationList);

                // 异步方式为学生角色的通知建立ES索引
                try {
                    CompletableFuture.runAsync(
                            () -> indexNotificationList(stuNotificationList),
                            asyncTaskExecutor
                    );
                } catch (Exception e) {
                    logger.error("无法索引学生通知信息到Elasticsearch，可能是索引创建失败或网络问题", e);
                }
            }

            // 向notification_receiver表插入信息
            String noticeId = notificationDTO.getNoticeId();
            processNotificationReceiver(validStuReceiverIdSet, noticeId, Constants.STUDENT);
        }

    }

    // 处理宿管角色
    private void sendDorManNotification(NotificationDTO notificationDTO,
                                        Map<String, Set<String>> roleToListMap,
                                        Map<String, Set<String>> invalidReceiverIdMap) {
        Date now = new Date();

        // 获取导入的接收通知的宿管业务标识id集合
        Set<String> dorManReceiverIdInputSet = roleToListMap.getOrDefault(RoleMatcher.DORMITORY_MANAGER.getRole(), Set.of());

        if (!dorManReceiverIdInputSet.isEmpty()) {
            // 获取数据库中宿管业务标识id集合
            Set<String> dorManReceiverIdDBSet = dormitoryManagerService.selectAllManagerId(); // 此处不做用户状态校验，因为用户登录时已经做了校验

            // 过滤无效的宿管业务标识id
            Set<String> validDorManReceiverIdSet = filterValidReceiverIds(dorManReceiverIdInputSet, dorManReceiverIdDBSet);

            // 将无效的宿管工号加入invalidReceiverIdMap
            if (validDorManReceiverIdSet.size() < dorManReceiverIdInputSet.size()) {
                addToInvalidReceiverIdMap(invalidReceiverIdMap,
                        validDorManReceiverIdSet,
                        dorManReceiverIdInputSet,
                        Constants.DORMITORY_MANAGER);
            }

            if (validDorManReceiverIdSet.isEmpty()) {
                logger.info("导入的通知对象中，没有有效的宿管业务标识id");
                return;
            } else {
                List<Notification> dorManNotificationList = buildNotificationList(validDorManReceiverIdSet,
                        notificationDTO, Constants.DORMITORY_MANAGER, now);

                batchInsertNotifications(dorManNotificationList);

                // 异步方式为宿管角色的通知建立ES索引
                try {
                    CompletableFuture.runAsync(
                            () -> indexNotificationList(dorManNotificationList),
                            asyncTaskExecutor
                    );
                } catch (Exception e) {
                    logger.error("无法索引宿管通知信息到Elasticsearch，可能是索引创建失败或网络问题", e);
                }
            }

            // 向notification_receiver表插入信息
            String noticeId = notificationDTO.getNoticeId();
            processNotificationReceiver(validDorManReceiverIdSet, noticeId, Constants.DORMITORY_MANAGER);
        }
    }

    // 处理班级管理员角色  这个角色比较特别，因为辅导员 班主任 院系领导都在同一张sys_user表
    private void sendSysUserNotification(NotificationDTO notificationDTO,
                                         Map<String, Set<String>> roleToListMap,
                                         Map<String, Set<String>> invalidReceiverIdMap) {
        Set<String> sysUserReceiverIdInputSet = roleToListMap.getOrDefault(RoleMatcher.SYS_USER.getRole(), Set.of());

        // 获取数据库中班级管理员集合
        // 获取所有班级管理员的sysUserNo和jobRole属性
        List<SysUser> sysUserDBList = sysUserService.selectAllSysUserNoAndJobRole(); // 此处不做用户状态校验，因为用户登录时已经做了校验
        if (!sysUserDBList.isEmpty()) {
            processSysUserReceiverIds(invalidReceiverIdMap, sysUserReceiverIdInputSet,
                    sysUserDBList, notificationDTO);
        }
    }

    // 根据角色构造无效的通知对象的Map 此处的role具体到职位角色
    private void addToInvalidReceiverIdMap(Map<String, Set<String>> invalidReceiverIdMap,
                                           Set<String> validReceiverIdSet,
                                           Set<String> inputReceiverIdSet,
                                           String role) {
        // 计算无效的业务标识id
        Set<String> invalidReceiverIdSet = inputReceiverIdSet.stream()
                .filter(id -> !validReceiverIdSet.contains(id))
                .collect(Collectors.toSet());

        if (!invalidReceiverIdSet.isEmpty()) {
            invalidReceiverIdMap.put(role, invalidReceiverIdSet);
            logger.warn("导入的通知对象中，存在无效的 {} 业务标识id: {}", role, invalidReceiverIdSet);
        }
    }

    // 过滤有效的业务标识id
    private Set<String> filterValidReceiverIds(Set<String> inputReceiverIdSet, Set<String> validReceiverIdSetInDB) {
        return inputReceiverIdSet.stream()
                .filter(validReceiverIdSetInDB::contains)
                .collect(Collectors.toSet());
    }

    // TargetScope为 specialUser 条件下的构造通知列表
    private List<Notification> buildNotificationList(Set<String> targetIdSet,
                                                     NotificationDTO notificationDTO,
                                                     String targetType,
                                                     Date now) {
        return targetIdSet.stream()
                .map(targetId -> {
                    Notification notification = new Notification();
                    notification.setNoticeId(notificationDTO.getNoticeId());
                    notification.setTitle(notificationDTO.getTitle());
                    notification.setContent(notificationDTO.getContent());
                    notification.setNoticeType(notificationDTO.getNoticeType());
                    notification.setTargetType(targetType);
                    notification.setTargetId(targetId);
                    notification.setTargetScope(TargetScope.SPECIAL_USER.getCode());
                    notification.setCreateTime(now);
                    notification.setUpdateTime(now);
                    return notification;
                })
                .toList();
    }

    // 分批插入 列表大小超过500条就分批插入
    private void batchInsertNotifications(List<Notification> notificationList) {
        int batchSize = 500;
        if (notificationList.isEmpty()) {
            logger.info("通知列表为空，不执行插入");
            return;
        }

        if (notificationList.size() > batchSize) {
            for (int i = 0; i < notificationList.size(); i += batchSize) {
                int end = Math.min(i + batchSize, notificationList.size());
                List<Notification> batch = notificationList.subList(i, end);
                notificationMapper.insertBatch(batch);
            }
        } else {
            notificationMapper.insertBatch(notificationList);
        }
    }

    // 分批插入 列表大小超过500条就分批插入
    private void batchInsertNotRecs(List<NotificationReceiver> notRecList) {
        int batchSize = 500;
        if (notRecList.isEmpty()) {
            logger.info("通知列表为空，不执行插入");
            return;
        }

        if (notRecList.size() > batchSize) {
            for (int i = 0; i < notRecList.size(); i += batchSize) {
                int end = Math.min(i + batchSize, notRecList.size());
                List<NotificationReceiver> batch = notRecList.subList(i, end);
                notificationReceiverMapper.insertBatch(batch);
            }
        } else {
            notificationReceiverMapper.insertBatch(notRecList);
        }
    }

    // 处理班级管理员角色的接收通知
    private void processSysUserReceiverIds(Map<String, Set<String>> invalidReceiverIdMap,
                                           Set<String> sysUserReceiverIdInputSet,
                                           List<SysUser> sysUserDBList,
                                           NotificationDTO notificationDTO) {
        // 过滤掉无效的班级管理员
        List<SysUser> filteredSysUserList = sysUserDBList.stream()
                .filter(sysUser -> sysUserReceiverIdInputSet.contains(sysUser.getSysUserNo()))
                .toList();

        // 将无效的班级管理员工号加入invalidReceiverIdMap
        if (filteredSysUserList.size() < sysUserReceiverIdInputSet.size()) {
            // 提取有效的班级管理员的业务标识id组成集合
            Set<String> sysUserNoFromDBSet = filteredSysUserList.stream()
                    .map(SysUser::getSysUserNo)
                    .collect(Collectors.toSet());
            // 获得无效的班级管理员的业务标识id组成集合
            Set<String> invalidSysUserNoSet = sysUserReceiverIdInputSet.stream()
                    .filter(sysUserNo -> !sysUserNoFromDBSet.contains(sysUserNo))
                    .collect(Collectors.toSet());

            logger.warn("导入的通知对象中，存在无效的 {} 业务标实id: {}", Constants.SYSTEM_USER, invalidSysUserNoSet);

            invalidReceiverIdMap.put(Constants.SYSTEM_USER, invalidSysUserNoSet);
        }

        List<Notification> allSysUserNotificationList = new ArrayList<>(filteredSysUserList.size());

        // key - 具体到职位角色的角色名称 value - 该角色的通知接收对象的业务标识id集合
        Map<String, Set<String>> roleToReceiverIdSetMap = filteredSysUserList.stream()
                .collect(Collectors.groupingBy(
                        SysUser::getJobRole,
                        Collectors.mapping(SysUser::getSysUserNo, Collectors.toSet())));

        // 构造通知列表
        for (Map.Entry<String, Set<String>> entry : roleToReceiverIdSetMap.entrySet()) {
            Date now = new Date();
            String jobRole = entry.getKey();
            Set<String> receiverIdSet = entry.getValue();

            // 向notification_receiver表插入信息
            String noticeId = notificationDTO.getNoticeId();
            processNotificationReceiver(receiverIdSet, noticeId, jobRole);

            List<Notification> singleRolenotificationList = buildNotificationList(receiverIdSet, notificationDTO, jobRole, now);
            allSysUserNotificationList.addAll(singleRolenotificationList);
        }

        if (!allSysUserNotificationList.isEmpty()) {
            notificationMapper.insertBatch(allSysUserNotificationList);

            // 异步方式为班级管理员角色的通知建立ES索引
            try {
                CompletableFuture.runAsync(
                        () -> indexNotificationList(allSysUserNotificationList),
                        asyncTaskExecutor
                );
            } catch (Exception e) {
                logger.error("无法索引班级管理员通知信息到Elasticsearch，可能是索引创建失败或网络问题", e);
            }

        } else {
            logger.info("通知列表为空，不执行插入");
        }
    }

    // 向notification_receiver表中插入信息
    private void processNotificationReceiver(Set<String> receiverIdSet,
                                             String noticeId,
                                             String receiverRole) {
        if (receiverIdSet.isEmpty()) {
            logger.info("没有有效的通知接收人，无法插入notification_receiver表");
            return;
        }

        Date now = new Date();
        List<NotificationReceiver> notificationReceiverList = receiverIdSet.stream()
                .map(receiverId -> {
                    NotificationReceiver notRec = new NotificationReceiver();
                    notRec.setNoticeId(noticeId);
                    notRec.setReceiverId(receiverId);
                    notRec.setReceiverRole(receiverRole);
                    notRec.setReadStatus("UNREAD");
                    notRec.setCreateTime(now);
                    notRec.setUpdateTime(now);
                    return notRec;
                })
                .toList();

        if (!notificationReceiverList.isEmpty()) {
            notificationReceiverService.insertBatch(notificationReceiverList);
        } else {
            logger.info("通知接收列表为空，不执行插入");
        }
    }

    // 根据角色从班级管理员中获取接收人业务标识id集合
    private Map<String, Set<String>> getJobRoleToReceiverIdSetMap() {
        List<SysUser> sysUserNoAndJobRoleList = sysUserService.selectAllSysUserNoAndJobRole();

        Map<String, Set<String>> jobRoleToReceiverIdSetMap = sysUserNoAndJobRoleList.stream()
                .collect(Collectors.groupingBy(
                        SysUser::getJobRole,
                        Collectors.mapping(SysUser::getSysUserNo, Collectors.toSet())));

        Set<String> counselorSysUserNoSet, claTeaSysUserNoSet, deanSysUserNoSet;
        if (!jobRoleToReceiverIdSetMap.containsKey(Constants.JOB_ROLE_COUNSELOR)) {
            logger.info("班级管理员中没有辅导员信息，无法给辅导员角色发送通知");
        } else {
            counselorSysUserNoSet = jobRoleToReceiverIdSetMap.get(Constants.JOB_ROLE_COUNSELOR);
            logger.info("在班级管理员中，一共有{}条辅导员信息", counselorSysUserNoSet.size());
        }

        if (!jobRoleToReceiverIdSetMap.containsKey(Constants.JOB_ROLE_CLASS_TEACHER)) {
            logger.info("班级管理员中没有班主任信息，无法给班主任角色发送通知");
        } else {
            claTeaSysUserNoSet = jobRoleToReceiverIdSetMap.get(Constants.JOB_ROLE_CLASS_TEACHER);
            logger.info("在班级管理员中，一共有{}条班主任信息", claTeaSysUserNoSet.size());
        }

        if (!jobRoleToReceiverIdSetMap.containsKey(Constants.JOB_ROLE_DEAN)) {
            logger.info("班级管理员中没有院系领导信息，无法给院系领导角色发送通知");
        } else {
            deanSysUserNoSet = jobRoleToReceiverIdSetMap.get(Constants.JOB_ROLE_DEAN);
            logger.info("班级管理员中，一共有{}条院系领导信息", deanSysUserNoSet.size());
        }

        return jobRoleToReceiverIdSetMap;

    }

    // 通过targetType发送（排除超级管理员角色）
    private Map<String, Set<String>> sendWithTargetType(NotificationDTO notificationDTO,
                                                        Map<String, Set<String>> invalidReceiverIdMap) {
        if (notificationDTO.getTargetType().equalsIgnoreCase(Constants.STUDENT)) {
            // todo 筛选在校生

            Date now = new Date();
            int batchSize = 500;
            Set<String> receiverIdSet = new HashSet<>();

            String targetType = notificationDTO.getTargetType().toLowerCase();
            Map<String, Set<String>> jobRoleToReceiverIdSetMap = getJobRoleToReceiverIdSetMap();

            switch (targetType) {
                // 学生
                case Constants.STUDENT -> handleNotAndNotRecForSpecialRole(
                        notificationDTO, Constants.STUDENT, () -> studentService.selectAllStudentNo());

                // 宿管
                case Constants.DORMITORY_MANAGER -> handleNotAndNotRecForSpecialRole(
                        notificationDTO, Constants.DORMITORY_MANAGER,
                        () -> dormitoryManagerService.selectAllManagerId());

                // 辅导员
                case Constants.JOB_ROLE_COUNSELOR -> handleNotAndNotRecForSpecialRole(
                        notificationDTO, Constants.JOB_ROLE_COUNSELOR,
                        () -> jobRoleToReceiverIdSetMap.get(Constants.JOB_ROLE_COUNSELOR));

                // 班主任
                case Constants.JOB_ROLE_CLASS_TEACHER -> handleNotAndNotRecForSpecialRole(
                        notificationDTO, Constants.JOB_ROLE_COUNSELOR,
                        () -> jobRoleToReceiverIdSetMap.get(Constants.JOB_ROLE_CLASS_TEACHER));

                // 院系领导
                case Constants.JOB_ROLE_DEAN -> handleNotAndNotRecForSpecialRole(
                        notificationDTO, Constants.JOB_ROLE_DEAN,
                        () -> jobRoleToReceiverIdSetMap.get(Constants.JOB_ROLE_DEAN));

                default ->
                        logger.error("给特定角色发送消息功能中，暂时不支持该targetType: {}", targetType);
            }
        }

        return invalidReceiverIdMap;
    }

    // 执行给特定角色发送通知和插入通知接收信息
    private void handleNotAndNotRecForSpecialRole(NotificationDTO notificationDTO,
                                                  String specialRole,
                                                  Supplier<Set<String>> receiverIdSupplier) {
        Set<String> receiverIdSet = receiverIdSupplier.get();

        Notification notification = buildNotification(
                notificationDTO, specialRole, TargetScope.SPECIAL_ROLE.getCode());
        notificationMapper.insert(notification);

        // 为该通知建立ES索引
        try {
            CompletableFuture.runAsync(() -> indexNotification(notification), asyncTaskExecutor);
        } catch (Exception e) {
            logger.error("无法索引通知信息到Elasticsearch，可能是索引创建失败或网络问题", e);
        }

        List<NotificationReceiver> notRecList = buildNotRecList(
                new ArrayList<>(receiverIdSet), notification.getNoticeId(), specialRole);

        // 同步插入
        // 【注】这里如果是学生角色，因为学生数量会比较多，应该使用异步，但考虑到事务的回滚，这里应该使用分布式事务，但为了降低开发复杂度 这里直接使用同步方式执行
        notificationReceiverService.insertBatch(notRecList);
    }

    // TargetScope为SpecialRole条件下构造通知
    private Notification buildNotification(NotificationDTO dto, String targetType, String targetScopeCode) {
        Date now = new Date();

        Notification notification = new Notification();
        notification.setNoticeId(dto.getNoticeId());
        notification.setTitle(dto.getTitle());
        notification.setContent(dto.getContent());
        notification.setNoticeType(dto.getNoticeType());
        notification.setTargetType(targetType);
        notification.setTargetScope(targetScopeCode);
        notification.setCreateTime(now);
        notification.setUpdateTime(now);

        return notification;
    }

    // 批量删除通知
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteBatch(NotificationDTO notificationDTO) {
        // 这里包含notification表和notification_receiver表
        int deletedRows = 0;

        try {
            // 判断是使用时间范围删除，还是使用通知业务id删除
            if (notificationDTO.getCreateTimeStart() != null) {
                deletedRows = doDeleteByTimeRange(notificationDTO);
            }

            if (notificationDTO.getNoticeIdList() != null) {
                deletedRows = doDeleteByNotificationIdList(notificationDTO);
            }
        } catch (Exception e) {
            logger.error("删除通知记录时出现异常", e);
            throw new SystemException(ResultCode.ERROR);
        }

        logger.info("在notification和notification_receiver表中，一共删除了 {} 条数据", deletedRows);

        return deletedRows;
    }

    // 执行根据时间范围删除通知
    private int doDeleteByTimeRange(NotificationDTO notificationDTO) {
        // 查询该时间范围的通知记录业务id
        NotificationQuery notQuery = NotificationQuery.builder()
                .createTimeStart(notificationDTO.getCreateTimeStart())
                .createTimeEnd(notificationDTO.getCreateTimeEnd())
                .build();
        List<String> notificationIdList = notificationMapper.selectByQuery(notQuery).stream()
                .map(Notification::getNoticeId)
                .toList();
        if (notificationIdList.isEmpty()) {
            logger.info("该时间范围内没有站内通知记录, {} - {}",
                    notificationDTO.getCreateTimeStart(), notificationDTO.getCreateTimeEnd());
            return 0;
        }

        // 删除ElasticSearch索引
        try {
            for (String noticeId : notificationDTO.getNoticeIdList()) {
                deleteEsIndex(noticeId);
            }
        } catch (Exception e) {
            logger.error("删除ElasticSearch索引失败", e);
        }

        // 删除notification表的记录
        int notDelRow = notificationMapper.deleteBatch(notQuery);
        logger.info("在该时间范围内，一共删除了{}条notification记录", notDelRow);

        // 删除notification_receiver表的记录
        NotificationReceiverQuery notRecQuery = NotificationReceiverQuery.builder()
                .noticeIdList(notificationIdList)
                .build();
        int notRecDelRow = notificationReceiverService.deleteBatch(notRecQuery);
        logger.info("在该时间范围内，一共删除了{}条notification_receiver记录", notRecDelRow);

        return notDelRow + notRecDelRow;
    }

    // 执行根据通知业务id列表删除通知
    private int doDeleteByNotificationIdList(NotificationDTO notificationDTO) {
        List<String> notificationIdList = notificationDTO.getNoticeIdList();
        NotificationQuery notQuery = NotificationQuery.builder()
                .noticeIdList(notificationIdList)
                .build();

        List<Notification> notificationList = notificationMapper.selectByQuery(notQuery);
        if (notificationList.isEmpty()) {
            logger.info("找不到符合条件的通知记录");
            return 0;
        }

        // 删除ElasticSearch索引
        try {
            for (String noticeId : notificationDTO.getNoticeIdList()) {
                deleteEsIndex(noticeId);
            }
        } catch (Exception e) {
            logger.error("删除ElasticSearch索引失败", e);
        }

        // 删除notification表的记录
        int notDelRow = notificationMapper.deleteBatch(notQuery);

        // 删除notification_receiver表的记录
        NotificationReceiverQuery notRecQuery = NotificationReceiverQuery.builder()
                .noticeIdList(notificationIdList)
                .build();
        int notRecRow = notificationReceiverService.deleteBatch(notRecQuery);
        logger.info("一共删除了{}条通知接收记录", notRecRow);

        return notDelRow + notRecRow;
    }

    @Override
    public void createIndex() {
        // 创建索引映射 - 适配java.util.Date和字符串形式的时间
        try {
            // 判断索引是否存在
            boolean esIndexExists = elasticsearchClient.indices().exists(e -> e.index(INDEX_NAME)).value();
            if (esIndexExists) {
                logger.info("索引已存在: {}", INDEX_NAME);
                return;
            }

            // 创建索引
            CreateIndexResponse response = elasticsearchClient.indices().create(c -> c.index(INDEX_NAME)
                    .mappings(m -> m.properties("noticeId", p -> p.keyword(k -> k))
                            .properties("title", p -> p.text(t -> t.analyzer("ik_max_word").searchAnalyzer("ik_smart")))
                            .properties("content", p -> p.text(t -> t.analyzer("ik_max_word").searchAnalyzer("ik_smart")))
                            .properties("noticeType", p -> p.keyword(k -> k))
                            .properties("targetType", p -> p.keyword(k -> k))
                            .properties("targetScope", p -> p.keyword(k -> k))
                    )
            );

            if (response.acknowledged()) {
                logger.info("成功创建索引: {}", INDEX_NAME);
            }
        } catch (Exception e) {
            logger.error("创建索引失败: {}", INDEX_NAME, e);
            throw new SystemException(ResultCode.NOTIFICATION_INDEX_FAILED);
        }
    }

    /**
     * ES索引通知信息 //todo 需要实现定期同步，必须先执行这个方法
     * @param notification      通知信息
     */
    @Override
    public void indexNotification(Notification notification) {
        if (StringUtils.isBlank(notification.getNoticeId())) {
            logger.error("notificationId无效，无法为该通知建立ES索引");
            throw new BusinessException(ResultCode.PARAMETER_ERROR);
        }

        // 转换为Map进行索引
        Map<String, Object> document = convertToMap(notification);
        try {
            elasticsearchClient.index(i -> i.index(INDEX_NAME)
                    .id(notification.getNoticeId())
                    .document(document)
            );

            logger.info("成功索引通知信息，ID: {}", notification.getNoticeId());
        } catch (Exception e) {
            logger.error("索引通知信息失败，ID: {}", notification.getNoticeId(), e);
            throw new SystemException(ResultCode.NOTIFICATION_INDEX_FAILED);
        }
    }

    /**
     * ES索引通知信息列表
     *
     * @param notificationList  通知信息列表
     */
    @Override
    public void indexNotificationList(List<Notification> notificationList) {
        try {
            BulkRequest.Builder builder = new BulkRequest.Builder();

            for (Notification notification : notificationList) {
                Map<String, Object> document = convertToMap(notification);
                builder.operations(operation -> operation.index(idx -> idx.index(INDEX_NAME)
                        .id(notification.getNoticeId())
                        .document(document)));
            }

            BulkResponse result = elasticsearchClient.bulk(builder.build());

            if (result.errors()) {
                logger.error("批量索引通知信息失败");
                result.items().forEach(item -> {
                    if (item.error() != null) {
                        logger.error("索引失败，ID: {}, 错误: {}", item.id(), item.error().reason());
                    }
                });
            } else {
                logger.info("成功批量索引通知信息，数量: {}", notificationList.size());
            }
        } catch (Exception e) {
            logger.error("批量索引通知信息失败", e);
            throw new SystemException(ResultCode.NOTIFICATION_INDEX_FAILED);
        }
    }

    /**
     * 使用ES查询通知列表
     * 前端仅使用以下字段参与 Elasticsearch（ES）查询
     * titleLike（通知标题模糊匹配）， contentLike（通知内容模糊匹配），noticeType（通知类型精确匹配）
     *
     * @param query     查询条件
     * @return          分页结果
     */
    @Override
    public PageResult<NotificationVO> searchNotificationListPageByEs(NotificationQuery query) {
        // 校验检索人的身份信息是否有效
        checkSearcherRoleAndNo(query.getTargetType(), query.getTargetId());

        SearchResponse<Map> response;
        try {
            // 构建查询
            BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();

            // 关键词搜索
            if (StringUtils.isNotBlank(query.getTitleLike())) {
                boolQueryBuilder.must(m ->
                        m.match(mt -> mt.field("title").query(query.getTitleLike())));
            }
            if (StringUtils.isNotBlank(query.getContentLike())) {
                boolQueryBuilder.must(m ->
                        m.match(mt -> mt.field("content").query(query.getContentLike())));
            }
            if (StringUtils.isNotBlank(query.getNoticeType())) {
                boolQueryBuilder.filter(f ->  // noticeType为keyword类型
                        f.term(t -> t.field("noticeType.keyword").value(query.getNoticeType())));
            }

            // 执行搜索 - 获取所有匹配的通知
            response = elasticsearchClient.search(s -> s
                    .index(INDEX_NAME)
                    .query(boolQueryBuilder.build()._toQuery())
                    .size(1000) // 获取更多结果用于后续过滤
                    .sort(sortField ->
                            sortField.field(f -> f
                                    .field("createTime")
                                    .order(SortOrder.Desc)
                            )
                    )
                    // 高亮配置
                    .highlight(h -> h.preTags("<em>").postTags("</em>")
                            .fields("title", f -> f)
                            .fields("content", f -> f)),

                    Map.class);
        } catch (Exception e) {
            logger.error("使用Elasticsearch查询通知列表失败", e);
            throw new SystemException(ResultCode.NOTIFICATION_ELASTICSEARCH_FAILED);
        }

        try {
            // 转换为基础Notification列表
//            List<Notification> allNotificationList = response.hits().hits().stream()
//                    .map(hit -> convertMapToNotification(hit.source()))
//                    .collect(Collectors.toList());
            List<Notification> allNotificationList = response.hits().hits().stream()
                    .map(hit -> {
                        Map<String, Object> source = hit.source();
                        if (source == null) {
                            logger.warn("ES查询返回的hit中source为null，跳过该记录，id: {}", hit.id());
                            return null;
                        }

                        Notification notification = convertMapToNotification(hit.source());
                        Map<String, List<String>> highlightMap = hit.highlight();

                        if (highlightMap != null) {
                            if (highlightMap.containsKey("title")) {
                                notification.setTitle(highlightMap.get("title").get(0)); // 使用高亮片段替换标题
                            }
                            if (highlightMap.containsKey("content")) {
                                notification.setContent(highlightMap.get("content").get(0)); // 使用高亮片段替换内容
                            }
                        }
                        return notification;
                    }).collect(Collectors.toList());

            logger.info("使用ES查询到尚未过滤的 {} 条通知", allNotificationList.size());

            // 根据用户角色和业务标识ID进行过滤
            List<Notification> filteredEsNotificationList = filterEsNotificationByUser(
                    allNotificationList, query.getTargetType(), query.getTargetId());
            logger.info("过滤ES的通知后剩余 {} 条通知", filteredEsNotificationList.size());

            // 手动实现分页
            return translateEsPageResult(filteredEsNotificationList, query);
        } catch (Exception e) {
            logger.error("转换ES查询结果为通知列表失败", e);
            throw new SystemException(ResultCode.NOTIFICATION_CONVERT_FAILED);
        }
    }

    /**
     * 检查搜索者的角色和业务标识ID是否有效
     * @param targetType        搜索者的目标类型（如学生、宿管等）
     * @param targetId          搜索者的业务标识ID（如学号、工号等）
     */
    private void checkSearcherRoleAndNo(String targetType, String targetId) {
        if (targetType.equalsIgnoreCase(UserRole.STUDENT.getCode())) {
            Student student = studentService.selectByStudentNo(targetId);
            if (student == null) {
                logger.error("无效的学生业务标识ID: {}", targetId);
                throw new BusinessException(ResultCode.PARAMETER_ERROR);
            }
        }

        if (targetType.equalsIgnoreCase(UserRole.DORMITORY_MANAGER.getCode())) {
            DormitoryManager dormitoryManager =
                    dormitoryManagerService.selectByManagerId(targetId);
            if (dormitoryManager == null
                    || dormitoryManager.getStatus().equalsIgnoreCase(Constants.OFF_DUTY)) {
                logger.error("无效的宿管业务标识ID: {}", targetId);
                throw new BusinessException(ResultCode.PARAMETER_ERROR);
            }
        }

        if (targetType.equalsIgnoreCase(JobRole.COUNSELOR.getCode())) {
            SysUser sysUser = sysUserService.selectBySysUserNo(targetId);
            if (sysUser == null
                    || sysUser.getStatus().equalsIgnoreCase(Constants.DISABLE_STR)) {
                logger.error("无效的班级管理员业务标识ID: {}", targetId);
                throw new BusinessException(ResultCode.PARAMETER_ERROR);
            }
        }
    }

    /**
     * 转换为Notification对象
     * @param source            源Map数据
     * @return                  Notification对象
     */
    private Notification convertMapToNotification(Map<String, Object> source) {
        Notification notification = new Notification();

        if (source.get("noticeId") != null) {
            notification.setNoticeId(source.get("noticeId").toString());
        }
        if (source.get("title") != null) {
            notification.setTitle(source.get("title").toString());
        }
        if (source.get("content") != null) {
            notification.setContent(source.get("content").toString());
        }
        if (source.get("noticeType") != null) {
            notification.setNoticeType(source.get("noticeType").toString());
        }
        if (source.get("targetType") != null) {
            notification.setTargetType(source.get("targetType").toString());
        }
        if (source.get("targetId") != null) {
            notification.setTargetId(source.get("targetId").toString());
        }
        if (source.get("targetScope") != null) {
            notification.setTargetScope(source.get("targetScope").toString());
        }

        // 将字符串格式转换回java.util.Date
        if (source.get("createTime") != null) {
            try {
                notification.setCreateTime(DATE_FORMAT.parse(source.get("createTime").toString()));
            } catch (Exception e) {
                logger.warn("无法解析createTime: {}", source.get("createTime"), e);
            }
        }
        if (source.get("updateTime") != null) {
            try {
                notification.setUpdateTime(DATE_FORMAT.parse(source.get("updateTime").toString()));
            } catch (Exception e) {
                logger.warn("无法解析updateTime: {}", source.get("updateTime"), e);
            }
        }

        return notification;
    }

    /**
     * 过滤ES查询结果中的通知列表
     *
     * @param sourceList        源通知列表
     * @param searcherRole      搜索者角色（这里具体到职位角色）
     * @param searcherNo        搜索者业务标识ID
     * @return                  过滤后的通知列表
     */
    private List<Notification> filterEsNotificationByUser(List<Notification> sourceList,
                                                          String searcherRole,
                                                          String searcherNo) {
        return sourceList.stream()
                .filter(notification -> {
                    String targetScope = notification.getTargetScope();
                    String targetType = notification.getTargetType();
                    String targetId = notification.getTargetId();

                    // 匹配的特定角色的通知
                    boolean isSpecialRoleMatch = searcherRole.equalsIgnoreCase(targetType) &&
                            TargetScope.SPECIAL_ROLE.getCode().equalsIgnoreCase(targetScope);

                    // 全体用户的通知
                    boolean isAllUsers = TargetScope.ALL_USERS.getCode().equalsIgnoreCase(targetScope);

                    // 匹配的特定用户的通知
                    boolean isSpecialUserMatch = searcherNo.equals(targetId) &&
                            TargetScope.SPECIAL_USER.getCode().equalsIgnoreCase(targetScope);

                    return isSpecialRoleMatch || isAllUsers || isSpecialUserMatch;
                })
                .collect(Collectors.toList());
    }


    /**
     * 实现对ES的结果进行转换和分页
     *
     * 结合notification_receiver表，把List<Notification> notificationList 转换为List<NotificationVO>
     * @param notificationList      通知列表
     * @param query                 查询条件
     * @return                      分页结果
     */
    private PageResult<NotificationVO> translateEsPageResult(List<Notification> notificationList,
                                                             NotificationQuery query) {
        int total = notificationList.size();
        int startIndex = (query.getPageNum() - 1) * query.getPageSize();
        int endIndex = Math.min(startIndex + query.getPageSize(), total);

        List<Notification> pageNotificationList;
        if (startIndex >= total) {
            pageNotificationList = new ArrayList<>();
        } else {
            pageNotificationList = notificationList.subList(startIndex, endIndex);
        }

        // 转换为NotificationVO并添加阅读状态
        // 该方法后面两个参数，用于筛选该用户有阅读权限的通知
        List<NotificationVO> notificationVOs = convertToNotificationVOList(
                pageNotificationList, query.getTargetType(), query.getTargetId());

        // 构造分页结果
        PageResult<NotificationVO> result = new PageResult<>();
        result.setData(notificationVOs);
        result.setTotal(total);
        result.setPageNum(query.getPageNum());
        result.setPageSize(query.getPageSize());

        return result;
    }

    /**
     * 转换为NotificationVO并添加阅读状态
     *
     * 此处英文前端需要展示通知阅读状态，所以需要将Notification包装成NotificationVO
     * @param sourNotificationList              通知列表
     * @param selfTargetType                通知接收者的目标类型
     * @param selfTargetId                  通知接收者的业务标识ID
     * @return                              转换后的NotificationVO列表
     */
    private List<NotificationVO> convertToNotificationVOList(List<Notification> sourNotificationList,
                                                             String selfTargetType,
                                                             String selfTargetId) {
        // 结合notification_receiver表，异步筛选取未读的通知列表
        NotificationQuery notReadQueryWrapper = NotificationQuery.builder()
                .targetType(selfTargetType)
                .targetId(selfTargetId)
                .readStatus("UNREAD")
                .build();
        CompletableFuture<List<Notification>> notReadFuture = CompletableFuture.supplyAsync(
                () -> distinguishReadOrUnread(sourNotificationList, notReadQueryWrapper),
                asyncTaskExecutor
        );
        CompletableFuture<List<NotificationVO>> notReadVOFuture = notReadFuture.thenApply(
                notReadNotificationList ->
                        notReadNotificationList.stream()
                                .map(notification -> {
                                    NotificationVO notificationVO = new NotificationVO();
                                    BeanUtils.copyProperties(notification, notificationVO);
                                    notificationVO.setReadStatus("UNREAD");
                                    return notificationVO;
                                })
                                .collect(Collectors.toList())
        );

        // 结合notification_receiver表，异步筛选取已读的通知列表
        NotificationQuery readQueryWrapper = NotificationQuery.builder()
                .targetType(selfTargetType)
                .targetId(selfTargetId)
                .readStatus("READ")
                .build();
        CompletableFuture<List<Notification>> readFuture = CompletableFuture.supplyAsync(
                () -> distinguishReadOrUnread(sourNotificationList, readQueryWrapper),
                asyncTaskExecutor
        );
        CompletableFuture<List<NotificationVO>> readVOFuture = readFuture.thenApply(
                notReadNotificationList ->
                        notReadNotificationList.stream()
                                .map(notification -> {
                                    NotificationVO notificationVO = new NotificationVO();
                                    BeanUtils.copyProperties(notification, notificationVO);
                                    notificationVO.setReadStatus("READ");
                                    return notificationVO;
                                })
                                .collect(Collectors.toList())
        );

        // 合并结果
        List<NotificationVO> allNotificationVOList = Collections.emptyList();
        try {
            allNotificationVOList = notReadVOFuture.thenCombine(readVOFuture,
                    (unreadVOList, readVOList) -> {
                        List<NotificationVO> combinedList = new ArrayList<>();
                        combinedList.addAll(unreadVOList);
                        combinedList.addAll(readVOList);
                        return combinedList;
                    }).get();
            logger.info("通过ES成功获取未读和已读状态的通知列表，数量: {}", allNotificationVOList.size());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("通过ES获取未读/已读状态通知的任务被中断", e);
            throw new SystemException(ResultCode.ERROR);
        } catch (ExecutionException e) {
            logger.error("执行ES获取未读/已读状态通知的任务发生错误", e);
            throw new SystemException(ResultCode.ERROR);
        }

        return allNotificationVOList;
    }

    /**
     * 重建ES索引
     */
    @Override
    public void rebuildIndex() {
        try {
            // 删除现有索引
            if (validateIndexExists()) {
                elasticsearchClient.indices().delete(document -> document.index(INDEX_NAME));
                logger.info("成功删除现有索引: {}", INDEX_NAME);
            }

            // 创建索引
            createIndex();

            // 从数据库重新同步数据
            List<Notification> allNotifications = notificationMapper.selectAllNotificationMainInfo();
            indexNotificationList(allNotifications);

            logger.info("成功重建索引: {}", INDEX_NAME);
        } catch (Exception e) {
            logger.error("重建索引失败", e);
            throw new SystemException(ResultCode.NOTIFICATION_INDEX_FAILED);
        }
    }

    /**
     * 判断ES索引是否存在
     * @return      true 如果索引存在，false 如果索引不存在
     */
    @Override
    public boolean validateIndexExists() {
        try {
            return elasticsearchClient.indices().exists(e -> e.index(INDEX_NAME)).value();
        } catch (Exception e) {
            logger.error("检查ES索引是否存在时发生错误", e);
            return false;
        }
    }

    /**
     * 将Notification对象转换为Map
     * @param notification      通知对象
     * @return                  转换后的Map
     */
    private Map<String, Object> convertToMap(Notification notification) {
        Map<String, Object> map = new HashMap<>();
        map.put("noticeId", notification.getNoticeId());
        map.put("title", notification.getTitle());
        map.put("content", notification.getContent());
        map.put("noticeType", notification.getNoticeType());
        map.put("targetType", notification.getTargetType());
        map.put("targetId", notification.getTargetId());
        map.put("targetScope", notification.getTargetScope());
        map.put("createTime", notification.getCreateTime());
        map.put("updateTime", notification.getUpdateTime());

        return map;
    }

    /**
     * 删除ES索引
     * @param notificationId    通知业务ID
     */
    @Override
    public void deleteEsIndex(String notificationId) {
        try {
            elasticsearchClient.delete(d -> d.index(INDEX_NAME).id(notificationId));
            logger.info("成功删除通知ES索引, notificationId：{}", notificationId);
        } catch (Exception e) {
            logger.error("无法删除ES索引, notificationId: {}", notificationId);
            throw new SystemException(ResultCode.NOTIFICATION_INDEX_DELETE_FAILED);
        }
    }

    // todo 批量删除ES索引

    /**
     * ES全文检索通知列表（此处不返回通知的阅读状态）
     * @param keyword       检索关键词
     * @return              通知列表
     */
    @Override
    public List<Notification> fullTextSearchWithoutLimited(String keyword) {
        List<Notification> notificationList = fullTextSearch(keyword);
        logger.info("执行ES全文检索，检索到 {} 条通知", notificationList.size());

        return notificationList;
    }

    /**
     * ES全文检索通知（仅限当前职位角色和业务标识ID）
     * @param keyword           检索关键词
     * @param searcherRole      检索者职位角色（具体到用户角色）
     * @param searcherNo        检索者业务标识ID（如学号、工号等）
     * @return                  通知信息列表
     */
    @Override
    public List<Notification> fullTextSearchWithRoleLimited(String keyword,
                                                            String searcherRole,
                                                            String searcherNo) {
        List<Notification> notificationList = fullTextSearch(keyword);

        List<Notification> filterList = filterEsNotificationByUser(notificationList, searcherRole, searcherNo);

        logger.info("执行ES全文检索，检索到 {} 条通知，经过角色和业务标识ID过滤后剩余 {} 条通知",
                notificationList.size(), filterList.size());

        return filterList;
    }

    /**
     * 执行全文检索
     * @param keyword       检索关键词
     * @return              通知列表
     */
    private List<Notification> fullTextSearch(String keyword) {
        // 构建全文搜索查询
        BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();
        boolQueryBuilder.should(MatchQuery.of(m ->
                m.field("title").query(keyword))._toQuery());
        boolQueryBuilder.should(MatchQuery.of(m ->
                m.field("content").query(keyword))._toQuery());

        SearchResponse<Map> response = null;
        try {
             response = elasticsearchClient.search(s -> s
                    .index(INDEX_NAME)
                    .query(boolQueryBuilder.build()._toQuery())
                    .size(100), Map.class);
        } catch (Exception e) {
            logger.error("执行全文检索时发生错误", e);
            throw new SystemException(ResultCode.NOTIFICATION_ELASTICSEARCH_FAILED);
        }

        return response.hits().hits().stream()
                .map(hit -> convertMapToNotification(hit.source()))
                .toList();
    }
}

