package com.tianhai.warn.service.impl;


import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Result;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import co.elastic.clients.elasticsearch._types.mapping.Property;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.pagehelper.PageInfo;
import com.tianhai.warn.annotation.EsField;
import com.tianhai.warn.component.ReceiverIdProviderRegistry;
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

import java.lang.reflect.Field;
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

    private static final String CLASS_PACKAGE_NAME = "com.tianhai.warn.model";
    private static final String invalidRole = "invalidRole";
    private static final String INDEX_NAME = "notification";
    private static final Integer DEFAULT_BATCH_SIZE = 1000;
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

//    @Autowired
//    private ElasticsearchClient elasticsearchClient;

//    @Autowired
//    private ObjectMapper objectMapper;

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
    public List<Notification> selectSimplePageForTarget(String targetId) {
        if (StringUtils.isBlank(targetId)) {
            logger.error("selectSimplePageForTarget: targetId 不能为空");
            throw new BusinessException(ResultCode.PARAMETER_ERROR);
        }
        return notificationMapper.selectSimplePageForTarget(targetId.trim());
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

//        // 索引到Elasticsearch
//        try {
//            indexNotification(notification);
//        } catch (Exception e) {
//            logger.error("无法索引通知信息到Elasticsearch，可能是索引创建失败或网络问题", e);
//        }

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

//                // 异步方式为学生角色的通知建立ES索引
//                try {
//                    CompletableFuture.runAsync(
//                            () -> indexNotificationList(stuNotificationList),
//                            asyncTaskExecutor
//                    );
//                } catch (Exception e) {
//                    logger.error("无法索引学生通知信息到Elasticsearch，可能是索引创建失败或网络问题", e);
//                }
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

//                // 异步方式为宿管角色的通知建立ES索引
//                try {
//                    CompletableFuture.runAsync(
//                            () -> indexNotificationList(dorManNotificationList),
//                            asyncTaskExecutor
//                    );
//                } catch (Exception e) {
//                    logger.error("无法索引宿管通知信息到Elasticsearch，可能是索引创建失败或网络问题", e);
//                }
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

//            // 异步方式为班级管理员角色的通知建立ES索引
//            try {
//                CompletableFuture.runAsync(
//                        () -> indexNotificationList(allSysUserNotificationList),
//                        asyncTaskExecutor
//                );
//            } catch (Exception e) {
//                logger.error("无法索引班级管理员通知信息到Elasticsearch，可能是索引创建失败或网络问题", e);
//            }

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

//        // 为该通知建立ES索引
//        try {
//            CompletableFuture.runAsync(() -> indexNotification(notification), asyncTaskExecutor);
//        } catch (Exception e) {
//            logger.error("无法索引通知信息到Elasticsearch，可能是索引创建失败或网络问题", e);
//        }

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
//        try {
//            for (String noticeId : notificationDTO.getNoticeIdList()) {
//                deleteEsIndexByNotId(noticeId);
//            }
//        } catch (Exception e) {
//            logger.error("删除ElasticSearch索引失败", e);
//        }

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
//        try {
//            for (String noticeId : notificationDTO.getNoticeIdList()) {
//                deleteEsIndexByNotId(noticeId);
//            }
//        } catch (Exception e) {
//            logger.error("删除ElasticSearch索引失败", e);
//        }

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
    public int countAll() {
        return notificationMapper.countAll();
    }

//    // 本项目设定只为notification表建立ES检索功能，所以这里直接直接为其建立ES文档
//    @Override
//    public void createIndex(String indexName, String className) {
//        // 创建索引映射 - 适配java.util.Date和字符串形式的时间
//        try {
//            // 判断索引是否存在
//            boolean esIndexExists = elasticsearchClient.indices().exists(e -> e.index(indexName)).value();
//            if (esIndexExists) {
//                logger.info("索引已存在: {}", indexName);
//                return;
//            }
//
//            // 创建索引
//            CreateIndexResponse response = elasticsearchClient.indices().create(c -> c.index(indexName)
//                    .mappings(m -> m.properties("noticeId", p -> p.keyword(k -> k))
//                            .properties("title", p -> p.text(t -> t.analyzer("ik_max_word").searchAnalyzer("ik_smart")))
//                            .properties("content", p -> p.text(t -> t.analyzer("ik_max_word").searchAnalyzer("ik_smart")))
//                            .properties("noticeType", p -> p.keyword(k -> k))
//                            .properties("targetType", p -> p.keyword(k -> k))
//                            .properties("targetScope", p -> p.keyword(k -> k))
//                    )
//            );
//
//            if (response.acknowledged()) {
//                logger.info("成功创建索引: {}", indexName);
//            }
//        } catch (Exception e) {
//            logger.error("创建索引失败: {}", indexName, e);
//            throw new SystemException(ResultCode.NOTIFICATION_INDEX_FAILED);
//        }
//    }
//
//    /**
//     * 动态创建 ES 索引，并基于实体类字段上的 @EsField 注解生成 mapping
//     *
//     * @param indexName ES 索引名称（如 "notification"）
//     * @param className 实体类名称（不带包名，如 "Notification"）
//     */
//    public void createIndexDynamically(String indexName, String className) {
//        try {
//            // 检查ES索引是否已存在，若存在则不重复创建
//            boolean exists = elasticsearchClient.indices().exists(e -> e.index(indexName)).value();
//            if (exists) {
//                logger.info("该ES索引已存在: {}", indexName);
//                return;
//            }
//
//            // 通过类名动态加载实体类
//            Class<?> clazz = Class.forName(CLASS_PACKAGE_NAME + "." + className);
//
//            // 遍历字段并构建字段映射（mapping）
//            Map<String, Property> propertyMap = new HashMap<>();
//            for (Field field : clazz.getDeclaredFields()) {
//                EsField esField = field.getAnnotation(EsField.class);
//                if (esField == null) {
//                    continue; // 没注解就跳过
//                }
//
//                Property esProperty = createEsPropertyFromAnnotation(esField);
//                propertyMap.put(field.getName(), esProperty);
//            }
//
//            // 创建索引并设置动态构造的 mappings
//            CreateIndexResponse response = elasticsearchClient.indices().create(c -> c
//                    .index(indexName)
//                    .mappings(m -> m.properties(propertyMap))
//            );
//
//            if (response.acknowledged()) {
//                logger.info("动态方式成功创建ES索引: {}", indexName);
//            }
//        } catch (Exception e) {
//            logger.error("创建ES索引失败: {}", indexName, e);
//            throw new SystemException(ResultCode.NOTIFICATION_INDEX_FAILED);
//        }
//    }
//
//    /**
//     * 将一个字段上的 @EsField 注解信息转换为对应的 Elasticsearch 字段映射（Property）
//     *
//     * 支持的字段类型包括：
//     * - TEXT：支持全文检索，可配置 analyzer、searchAnalyzer
//     * - KEYWORD：精确匹配，不分词
//     * - INTEGER / LONG：整型字段
//     * - DATE：日期字段，可设置 format
//     * - BOOLEAN：布尔值字段
//     *
//     * 注意：
//     * - Elasticsearch Java Client 的 DSL 构造器要求 lambda 表达式返回 builder 本身，而不是 build() 出来的对象
//     * - Property.of(...) 接受的 lambda 返回值必须是 ObjectBuilder 类型（如 t -> t.xxx().yyy() 返回 t 本身）
//     *
//     * @param esField 从字段读取的注解对象，包含类型、分词器等设置
//     * @return 对应的 Property 映射配置，用于构造 Elasticsearch 索引映射
//     */
//    private Property createEsPropertyFromAnnotation(EsField esField) {
//        EsFieldType type = esField.type();
//
//        return switch (type) {
//            case TEXT -> Property.of(p -> p.text(t -> {
//                if (!esField.analyzer().isEmpty()) {
//                    t.analyzer(esField.analyzer());
//                }
//                if (!esField.searchAnalyzer().isEmpty()) {
//                    t.searchAnalyzer(esField.searchAnalyzer());
//                }
//                return t;
//            }));
//
//            case KEYWORD -> Property.of(p -> p.keyword(k -> k));
//
//            case INTEGER -> Property.of(p -> p.integer(i -> i));
//
//            case LONG -> Property.of(p -> p.long_(l -> l));
//
//            case DATE -> Property.of(p -> p.date(d -> {
//                if (!esField.format().isEmpty()) {
//                    d.format(esField.format());
//                }
//                return d;
//            }));
//
//            case BOOLEAN -> Property.of(p -> p.boolean_(b -> b));
//        };
//    }
//
//    /**
//     * ES索引通知信息 //todo 需要实现定期同步，必须先执行这个方法
//     * @param notification      通知信息
//     */
//    @Override
//    public void indexNotification(Notification notification) {
//        if (StringUtils.isBlank(notification.getNoticeId())) {
//            logger.error("notificationId无效，无法为该通知建立ES索引");
//            throw new BusinessException(ResultCode.PARAMETER_ERROR);
//        }
//
//        // 转换为Map进行索引
//        Map<String, Object> document = convertToMap(notification);
//        try {
//            IndexResponse response = elasticsearchClient.index(i -> i
//                    .index(INDEX_NAME)
//                    .id(notification.getNoticeId())
//                    .document(document)
//            );
//
//            // 只有成功写入 ES 才更新数据库状态
//            if (response.result() == Result.Created || response.result() == Result.Updated) {
//                markAsIndexed(notification.getNoticeId());
//                logger.info("成功索引通知信息，ID: {}", notification.getNoticeId());
//            } else {
//                logger.error("ES索引操作未成功，ID: {}, result: {}", notification.getNoticeId(), response.result().jsonValue());
//                throw new Exception();
//            }
//        } catch (Exception e) {
//            logger.error("索引通知信息失败，ID: {}", notification.getNoticeId(), e);
//            throw new SystemException(ResultCode.NOTIFICATION_INDEX_FAILED);
//        }
//    }
//
//    /**
//     * ES索引通知信息列表
//     *
//     * @param notificationList  通知信息列表
//     */
//    @Override
//    public void indexNotificationList(List<Notification> notificationList) {
//        logger.info("即将批量索引通知信息，数量：{}", notificationList.size());
//        try {
//            BulkRequest.Builder builder = new BulkRequest.Builder();
//
//            for (Notification notification : notificationList) {
//                Map<String, Object> document = convertToMap(notification);
//                builder.operations(operation -> operation.index(idx -> idx.index(INDEX_NAME)
//                        .id(notification.getNoticeId())
//                        .document(document)));
//            }
//
//            BulkResponse result = elasticsearchClient.bulk(builder.build());
//
//            if (result.errors()) {
//                logger.error("批量索引通知信息失败");
//                result.items().forEach(item -> {
//                    if (item.error() != null) {
//                        logger.error("索引失败，ID: {}, 错误: {}", item.id(), item.error().reason());
//                    }
//                });
//            } else {
//                // 批量更新数据库中的索引状态
//                List<String> noticeIds = notificationList.stream()
//                        .map(Notification::getNoticeId)
//                        .collect(Collectors.toList());
//                markBatchAsIndexed(noticeIds);
//                logger.info("成功批量索引通知信息，数量: {}", notificationList.size());
//            }
//        } catch (Exception e) {
//            logger.error("批量索引通知信息失败，可能是批量索引过程出错或者更新索引状态出错", e);
//            throw new SystemException(ResultCode.NOTIFICATION_INDEX_FAILED);
//        }
//    }
//
//    /**
//     * 使用ES查询通知列表
//     * 前端仅使用以下字段参与 Elasticsearch（ES）查询
//     * titleLike（通知标题模糊匹配）， contentLike（通知内容模糊匹配），noticeType（通知类型精确匹配）
//     *
//     * @param query     查询条件
//     * @return          分页结果
//     */
//    @Override
//    public PageResult<NotificationVO> searchNotificationListPageByEs(NotificationQuery query) {
//        // 校验检索人的身份信息是否有效
//        checkSearcherRoleAndNo(query.getTargetType(), query.getTargetId());
//
//        SearchResponse<Map> response;
//        try {
//            // 构建查询
//            BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();
//
//            // 关键词搜索
//            if (StringUtils.isNotBlank(query.getTitleLike())) {
//                boolQueryBuilder.must(m ->
//                        m.match(mt -> mt.field("title").query(query.getTitleLike())));
//            }
//            if (StringUtils.isNotBlank(query.getContentLike())) {
//                boolQueryBuilder.must(m ->
//                        m.match(mt -> mt.field("content").query(query.getContentLike())));
//            }
//            if (StringUtils.isNotBlank(query.getNoticeType())) {
//                boolQueryBuilder.filter(f ->  // noticeType为keyword类型
//                        f.term(t -> t.field("noticeType.keyword").value(query.getNoticeType())));
//            }
//
//            // 执行搜索 - 获取所有匹配的通知
//            response = elasticsearchClient.search(s -> s
//                    .index(INDEX_NAME)
//                    .query(boolQueryBuilder.build()._toQuery())
//                    .size(1000) // 获取更多结果用于后续过滤
//                    .sort(sortField ->
//                            sortField.field(f -> f
//                                    .field("createTime")
//                                    .order(SortOrder.Desc)
//                            )
//                    )
//                    // 高亮配置
//                    .highlight(h -> h.preTags("<em>").postTags("</em>")
//                            .fields("title", f -> f)
//                            .fields("content", f -> f)),
//
//                    Map.class);
//        } catch (Exception e) {
//            logger.error("使用Elasticsearch查询通知列表失败", e);
//            throw new SystemException(ResultCode.NOTIFICATION_ELASTICSEARCH_FAILED);
//        }
//
//        try {
//            // 转换为基础Notification列表
//            List<Notification> allNotificationList = response.hits().hits().stream()
//                    .map(hit -> {
//                        Map<String, Object> source = hit.source();
//                        if (source == null) {
//                            logger.warn("ES查询返回的hit中source为null，跳过该记录，id: {}", hit.id());
//                            return null;
//                        }
//
//                        Notification notification = convertMapToNotification(hit.source());
//                        Map<String, List<String>> highlightMap = hit.highlight();
//
//                        if (highlightMap != null) {
//                            if (highlightMap.containsKey("title")) {
//                                notification.setTitle(highlightMap.get("title").get(0)); // 使用高亮片段替换标题
//                            }
//                            if (highlightMap.containsKey("content")) {
//                                notification.setContent(highlightMap.get("content").get(0)); // 使用高亮片段替换内容
//                            }
//                        }
//                        return notification;
//                    }).collect(Collectors.toList());
//
//            logger.info("使用ES查询到尚未过滤的 {} 条通知", allNotificationList.size());
//
//            // 根据用户角色和业务标识ID进行过滤
//            List<Notification> filteredEsNotificationList = filterEsNotificationByUser(
//                    allNotificationList, query.getTargetType(), query.getTargetId());
//            logger.info("过滤ES的通知后剩余 {} 条通知", filteredEsNotificationList.size());
//
//            // 手动实现分页
//            return translateEsPageResult(filteredEsNotificationList, query);
//        } catch (Exception e) {
//            logger.error("转换ES查询结果为通知列表失败", e);
//            throw new SystemException(ResultCode.NOTIFICATION_CONVERT_FAILED);
//        }
//    }
//
//    /**
//     * 检查搜索者的角色和业务标识ID是否有效
//     * @param targetType        搜索者的目标类型（如学生、宿管等）
//     * @param targetId          搜索者的业务标识ID（如学号、工号等）
//     */
//    private void checkSearcherRoleAndNo(String targetType, String targetId) {
//        if (targetType.equalsIgnoreCase(UserRole.STUDENT.getCode())) {
//            Student student = studentService.selectByStudentNo(targetId);
//            if (student == null) {
//                logger.error("无效的学生业务标识ID: {}", targetId);
//                throw new BusinessException(ResultCode.PARAMETER_ERROR);
//            }
//        }
//
//        if (targetType.equalsIgnoreCase(UserRole.DORMITORY_MANAGER.getCode())) {
//            DormitoryManager dormitoryManager =
//                    dormitoryManagerService.selectByManagerId(targetId);
//            if (dormitoryManager == null
//                    || dormitoryManager.getStatus().equalsIgnoreCase(Constants.OFF_DUTY)) {
//                logger.error("无效的宿管业务标识ID: {}", targetId);
//                throw new BusinessException(ResultCode.PARAMETER_ERROR);
//            }
//        }
//
//        if (targetType.equalsIgnoreCase(JobRole.COUNSELOR.getCode())) {
//            SysUser sysUser = sysUserService.selectBySysUserNo(targetId);
//            if (sysUser == null
//                    || sysUser.getStatus().equalsIgnoreCase(Constants.DISABLE_STR)) {
//                logger.error("无效的班级管理员业务标识ID: {}", targetId);
//                throw new BusinessException(ResultCode.PARAMETER_ERROR);
//            }
//        }
//    }
//
//    /**
//     * 转换为Notification对象
//     * @param source            源Map数据
//     * @return                  Notification对象
//     */
//    private Notification convertMapToNotification(Map<String, Object> source) {
//        Notification notification = new Notification();
//
//        if (source.get("noticeId") != null) {
//            notification.setNoticeId(source.get("noticeId").toString());
//        }
//        if (source.get("title") != null) {
//            notification.setTitle(source.get("title").toString());
//        }
//        if (source.get("content") != null) {
//            notification.setContent(source.get("content").toString());
//        }
//        if (source.get("noticeType") != null) {
//            notification.setNoticeType(source.get("noticeType").toString());
//        }
//        if (source.get("targetType") != null) {
//            notification.setTargetType(source.get("targetType").toString());
//        }
//        if (source.get("targetId") != null) {
//            notification.setTargetId(source.get("targetId").toString());
//        }
//        if (source.get("targetScope") != null) {
//            notification.setTargetScope(source.get("targetScope").toString());
//        }
//
//        // 将字符串格式转换回java.util.Date
//        if (source.get("createTime") != null) {
//            try {
//                notification.setCreateTime(DATE_FORMAT.parse(source.get("createTime").toString()));
//            } catch (Exception e) {
//                logger.warn("无法解析createTime: {}", source.get("createTime"), e);
//            }
//        }
//        if (source.get("updateTime") != null) {
//            try {
//                notification.setUpdateTime(DATE_FORMAT.parse(source.get("updateTime").toString()));
//            } catch (Exception e) {
//                logger.warn("无法解析updateTime: {}", source.get("updateTime"), e);
//            }
//        }
//
//        return notification;
//    }
//
//    /**
//     * 过滤ES查询结果中的通知列表
//     *
//     * @param sourceList        源通知列表
//     * @param searcherRole      搜索者角色（这里具体到职位角色）
//     * @param searcherNo        搜索者业务标识ID
//     * @return                  过滤后的通知列表
//     */
//    private List<Notification> filterEsNotificationByUser(List<Notification> sourceList,
//                                                          String searcherRole,
//                                                          String searcherNo) {
//        return sourceList.stream()
//                .filter(notification -> {
//                    String targetScope = notification.getTargetScope();
//                    String targetType = notification.getTargetType();
//                    String targetId = notification.getTargetId();
//
//                    // 匹配的特定角色的通知
//                    boolean isSpecialRoleMatch = searcherRole.equalsIgnoreCase(targetType) &&
//                            TargetScope.SPECIAL_ROLE.getCode().equalsIgnoreCase(targetScope);
//
//                    // 全体用户的通知
//                    boolean isAllUsers = TargetScope.ALL_USERS.getCode().equalsIgnoreCase(targetScope);
//
//                    // 匹配的特定用户的通知
//                    boolean isSpecialUserMatch = searcherNo.equals(targetId) &&
//                            TargetScope.SPECIAL_USER.getCode().equalsIgnoreCase(targetScope);
//
//                    return isSpecialRoleMatch || isAllUsers || isSpecialUserMatch;
//                })
//                .collect(Collectors.toList());
//    }
//
//
//    /**
//     * 实现对ES的结果进行转换和分页
//     *
//     * 结合notification_receiver表，把List<Notification> notificationList 转换为List<NotificationVO>
//     * @param notificationList      通知列表
//     * @param query                 查询条件
//     * @return                      分页结果
//     */
//    private PageResult<NotificationVO> translateEsPageResult(List<Notification> notificationList,
//                                                             NotificationQuery query) {
//        int total = notificationList.size();
//        int startIndex = (query.getPageNum() - 1) * query.getPageSize();
//        int endIndex = Math.min(startIndex + query.getPageSize(), total);
//
//        List<Notification> pageNotificationList;
//        if (startIndex >= total) {
//            pageNotificationList = new ArrayList<>();
//        } else {
//            pageNotificationList = notificationList.subList(startIndex, endIndex);
//        }
//
//        // 转换为NotificationVO并添加阅读状态
//        // 该方法后面两个参数，用于筛选该用户有阅读权限的通知
//        List<NotificationVO> notificationVOs = convertToNotificationVOList(
//                pageNotificationList, query.getTargetType(), query.getTargetId());
//
//        // 构造分页结果
//        PageResult<NotificationVO> result = new PageResult<>();
//        result.setData(notificationVOs);
//        result.setTotal(total);
//        result.setPageNum(query.getPageNum());
//        result.setPageSize(query.getPageSize());
//
//        return result;
//    }
//
//    /**
//     * 转换为NotificationVO并添加阅读状态
//     *
//     * 此处英文前端需要展示通知阅读状态，所以需要将Notification包装成NotificationVO
//     * @param sourNotificationList              通知列表
//     * @param selfTargetType                通知接收者的目标类型
//     * @param selfTargetId                  通知接收者的业务标识ID
//     * @return                              转换后的NotificationVO列表
//     */
//    private List<NotificationVO> convertToNotificationVOList(List<Notification> sourNotificationList,
//                                                             String selfTargetType,
//                                                             String selfTargetId) {
//        // 结合notification_receiver表，异步筛选取未读的通知列表
//        NotificationQuery notReadQueryWrapper = NotificationQuery.builder()
//                .targetType(selfTargetType)
//                .targetId(selfTargetId)
//                .readStatus("UNREAD")
//                .build();
//        CompletableFuture<List<Notification>> notReadFuture = CompletableFuture.supplyAsync(
//                () -> distinguishReadOrUnread(sourNotificationList, notReadQueryWrapper),
//                asyncTaskExecutor
//        );
//        CompletableFuture<List<NotificationVO>> notReadVOFuture = notReadFuture.thenApply(
//                notReadNotificationList ->
//                        notReadNotificationList.stream()
//                                .map(notification -> {
//                                    NotificationVO notificationVO = new NotificationVO();
//                                    BeanUtils.copyProperties(notification, notificationVO);
//                                    notificationVO.setReadStatus("UNREAD");
//                                    return notificationVO;
//                                })
//                                .collect(Collectors.toList())
//        );
//
//        // 结合notification_receiver表，异步筛选取已读的通知列表
//        NotificationQuery readQueryWrapper = NotificationQuery.builder()
//                .targetType(selfTargetType)
//                .targetId(selfTargetId)
//                .readStatus("READ")
//                .build();
//        CompletableFuture<List<Notification>> readFuture = CompletableFuture.supplyAsync(
//                () -> distinguishReadOrUnread(sourNotificationList, readQueryWrapper),
//                asyncTaskExecutor
//        );
//        CompletableFuture<List<NotificationVO>> readVOFuture = readFuture.thenApply(
//                notReadNotificationList ->
//                        notReadNotificationList.stream()
//                                .map(notification -> {
//                                    NotificationVO notificationVO = new NotificationVO();
//                                    BeanUtils.copyProperties(notification, notificationVO);
//                                    notificationVO.setReadStatus("READ");
//                                    return notificationVO;
//                                })
//                                .collect(Collectors.toList())
//        );
//
//        // 合并结果
//        List<NotificationVO> allNotificationVOList = Collections.emptyList();
//        try {
//            allNotificationVOList = notReadVOFuture.thenCombine(readVOFuture,
//                    (unreadVOList, readVOList) -> {
//                        List<NotificationVO> combinedList = new ArrayList<>();
//                        combinedList.addAll(unreadVOList);
//                        combinedList.addAll(readVOList);
//                        return combinedList;
//                    }).get();
//            logger.info("通过ES成功获取未读和已读状态的通知列表，数量: {}", allNotificationVOList.size());
//
//        } catch (InterruptedException e) {
//            Thread.currentThread().interrupt();
//            logger.error("通过ES获取未读/已读状态通知的任务被中断", e);
//            throw new SystemException(ResultCode.ERROR);
//        } catch (ExecutionException e) {
//            logger.error("执行ES获取未读/已读状态通知的任务发生错误", e);
//            throw new SystemException(ResultCode.ERROR);
//        }
//
//        return allNotificationVOList;
//    }
//
//    /**
//     * 重建ES索引
//     */
//    @Override
//    public void rebuildIndex(String indexName, String className) {
//        // 删除现有索引
//        try {
//            if (validateIndexExists(indexName)) {
//                elasticsearchClient.indices().delete(document -> document.index(indexName));
//                logger.info("成功删除现有索引: {}", indexName);
//            }
//        } catch (Exception e) {
//            logger.error("删除现有索引失败: {}", indexName, e);
//            throw new SystemException(ResultCode.NOTIFICATION_INDEX_FAILED);
//        }
//
//        // 创建索引
//        createIndex(indexName, className);
//
//        // 获取该索引对应的总文档数
//        int totalDocCount;
//        try {
//            totalDocCount = countDocuments(indexName);
//        } catch (Exception e) {
//            logger.error("数据库发生异常，无法获取该索引对应的总文档数, indexName:{}", indexName);
//            return;
//        }
//        if (totalDocCount == 0) {
//            logger.info("索引 {} 中没有文档，跳过索引重建", indexName);
//            return;
//        }
//
//        // 为ES索引异步执行全量同步
//        CompletableFuture.runAsync(() -> {
//            try {
//                performFullSyncForEsIndex(totalDocCount, indexName);
//            } catch (Exception e) {
//                logger.error("重建ES索引时发生错误", e);
//                deleteEsIndexByIndexName(INDEX_NAME); // 回滚操作
//                throw new SystemException(ResultCode.INDEX_BUILD_FAILED);
//            }
//        }, asyncTaskExecutor);
//
//
//    }
//
//
//    /**
//     * 统计数据表记录数
//     * 根据索引名统计该索引对应的数据库表有多少条记录（多少个文档）
//     * @param indexName     索引名（如：notification） 这里对应表名（如果表名有英文下划线，需要另外处理）
//     * @return      文档数
//     */
//    private int countDocuments(String indexName) {
//        //本项目只为notification表实现ES索引
////        return MapperInvoker.countAllFunction.apply(indexName);
//        return notificationMapper.countAll();
//    }
//
//    /**
//     * 执行全量同步
//     * @param totalCount            总行数（总文档数）
//     * @param indexName             索引名（数据库表名）
//     */
//    private void performFullSyncForEsIndex(int totalCount, String indexName) {
//        int batchSize = DEFAULT_BATCH_SIZE;
//        int totalBatches = (int) Math.ceil((double) totalCount / batchSize); // 向上取整
//
//        for (int batchIndex = 0; batchIndex < totalBatches; batchIndex++) {
//            int offset = batchIndex * batchSize;
//            int limit = Math.min(batchSize, totalCount - offset);
//
//            // 执行分页查询
//            // 【注】本项目设定只为notification表建立ES检索，如果有为多张表建立ES检索，
//            // 则需要建立indexName和表名或者接口的映射关系，实现方案参考本类的countDocuments方法
//            List<Notification> batchList = null;
//            try {
//                batchList = notificationMapper.selectBatchWithOffset(offset, limit);
//            } catch (Exception e) {
//                logger.error("查询批次数据失败，批次索引：{}，偏移量：{}，限制：{}", batchIndex, offset, limit, e);
//
//            }
//
//            // 为当前批次的数据建立ES索引
//            try {
//                indexNotificationList(batchList);
//                logger.info("批次 {}/{} 同步完成建立ES索引，记录数：{}",
//                        batchIndex + 1, totalBatches, batchList.size());
//            } catch (Exception e) {
//                logger.error("批次 {} 同步失败", batchIndex, e);
//                throw new RuntimeException(e);
//            }
//        }
//
//        logger.info("建立ES索引全量同步任务已启动，共 {} 个批次", totalBatches);
//    }
//
//
//
//    /**
//     * 判断ES索引是否存在
//     * @param       indexName   索引名
//     * @return      true 如果索引存在，false 如果索引不存在
//     */
//    @Override
//    public boolean validateIndexExists(String indexName) {
//        try {
//            return elasticsearchClient.indices().exists(e -> e.index(indexName)).value();
//        } catch (Exception e) {
//            logger.error("检查ES索引是否存在时发生错误", e);
//            return false;
//        }
//    }
//
//
//    /**
//     * 将Notification对象转换为Map
//     * @param notification      通知对象
//     * @return                  转换后的Map
//     */
//    private Map<String, Object> convertToMap(Notification notification) {
//        Map<String, Object> map = new HashMap<>();
//        map.put("noticeId", notification.getNoticeId());
//        map.put("title", notification.getTitle());
//        map.put("content", notification.getContent());
//        map.put("noticeType", notification.getNoticeType());
//        map.put("targetType", notification.getTargetType());
//        map.put("targetId", notification.getTargetId());
//        map.put("targetScope", notification.getTargetScope());
//        map.put("createTime", notification.getCreateTime());
//        map.put("updateTime", notification.getUpdateTime());
//
//        return map;
//    }
//
//    /**
//     * 删除ES文档
//     * @param noticeId    通知业务ID（ES文档的id）
//     */
//    @Override
//    public void deleteEsIndexByNotId(String noticeId) {
//        if (StringUtils.isBlank(noticeId)) {
//            logger.info("通知业务id（文档id）为空，跳过删除ES索引操作");
//            return;
//        }
//
//        try {
//            elasticsearchClient.delete(d -> d.index(INDEX_NAME).id(noticeId));
//            logger.info("成功删除通知ES文档, notificationId：{}", noticeId);
//        } catch (Exception e) {
//            logger.error("无法删除ES文档, notificationId: {}", noticeId);
//            throw new SystemException(ResultCode.NOTIFICATION_INDEX_DELETE_FAILED);
//        }
//    }
//
//    /**
//     * 批量删除ES的通知索引的文档
//     * @param noticeIdList      通知业务ID列表（ES文档id列表）
//     */
//    public void deleteEsIndexByNotIdList(List<String> noticeIdList) {
//        if (noticeIdList == null || noticeIdList.isEmpty()) {
//            logger.info("通知业务id列表为空，跳过批量删除ES索引操作");
//            return;
//        }
//
//        try {
//            BulkRequest.Builder bulkRequest = new BulkRequest.Builder();
//            for (String noticeId : noticeIdList) {
//                bulkRequest.operations(op -> op.delete(d -> d.index(INDEX_NAME).id(noticeId)));
//            }
//
//            BulkResponse response = elasticsearchClient.bulk(bulkRequest.build());
//
//            if (response.errors()) {
//                logger.error("批量删除ES索引文档失败");
//                response.items().forEach(item -> {
//                    if (item.error() != null) {
//                        logger.error("删除失败，ID: {}, 错误: {}", item.id(), item.error().reason());
//                    }
//                });
//            } else {
//                logger.info("成功批量删除通知ES文档，数量: {}", noticeIdList.size());
//            }
//        } catch (Exception e) {
//            logger.error("批量删除ES索引文档失败", e);
//            throw new SystemException(ResultCode.NOTIFICATION_INDEX_DELETE_FAILED);
//        }
//    }
//
//    /**
//     * ES全文检索通知列表（此处不返回通知的阅读状态）
//     * @param keyword       检索关键词
//     * @return              通知列表
//     */
//    @Override
//    public List<Notification> fullTextSearchWithoutLimited(String keyword) {
//        List<Notification> notificationList = fullTextSearch(keyword);
//        logger.info("执行ES全文检索，检索到 {} 条通知", notificationList.size());
//
//        return notificationList;
//    }
//
//    /**
//     * ES全文检索通知（仅限当前职位角色和业务标识ID）
//     * @param keyword           检索关键词
//     * @param searcherRole      检索者职位角色（具体到用户角色）
//     * @param searcherNo        检索者业务标识ID（如学号、工号等）
//     * @return                  通知信息列表
//     */
//    @Override
//    public List<Notification> fullTextSearchWithRoleLimited(String keyword,
//                                                            String searcherRole,
//                                                            String searcherNo) {
//        List<Notification> notificationList = fullTextSearch(keyword);
//
//        List<Notification> filterList = filterEsNotificationByUser(notificationList, searcherRole, searcherNo);
//
//        logger.info("执行ES全文检索，检索到 {} 条通知，经过角色和业务标识ID过滤后剩余 {} 条通知",
//                notificationList.size(), filterList.size());
//
//        return filterList;
//    }
//
//    /**
//     * 执行全文检索
//     * @param keyword       检索关键词
//     * @return              通知列表
//     */
//    private List<Notification> fullTextSearch(String keyword) {
//        // 构建全文搜索查询
//        BoolQuery.Builder boolQueryBuilder = new BoolQuery.Builder();
//        boolQueryBuilder.should(MatchQuery.of(m ->
//                m.field("title").query(keyword))._toQuery());
//        boolQueryBuilder.should(MatchQuery.of(m ->
//                m.field("content").query(keyword))._toQuery());
//
//        SearchResponse<Map> response = null;
//        try {
//             response = elasticsearchClient.search(s -> s
//                    .index(INDEX_NAME)
//                    .query(boolQueryBuilder.build()._toQuery())
//                    .size(100), Map.class);
//        } catch (Exception e) {
//            logger.error("执行全文检索时发生错误", e);
//            throw new SystemException(ResultCode.NOTIFICATION_ELASTICSEARCH_FAILED);
//        }
//
//        return response.hits().hits().stream()
//                .map(hit -> convertMapToNotification(hit.source()))
//                .toList();
//    }
//
//    /**
//     * 删除ES索引（通过索引名）
//     * @param indexName     索引名
//     * @return              是否删除成功
//     */
//    @Override
//    public boolean deleteEsIndexByIndexName(String indexName) {
//        if (StringUtils.isBlank(indexName)) {
//            logger.warn("索引名为空，跳过删除操作");
//            return true;
//        }
//
//        try {
//            // 检查索引是否存在
//            boolean exists = elasticsearchClient.indices().exists(e -> e.index(indexName)).value();
//
//            if (!exists) {
//                logger.warn("索引 {} 不存在，跳过删除操作", indexName);
//                return true;
//            }
//
//            // 删除索引
//            elasticsearchClient.indices().delete(d -> d.index(indexName));
//            logger.info("成功删除ES索引: {}", indexName);
//
//        } catch (Exception e) {
//            logger.error("删除ES索引失败，索引名: {}", indexName, e);
//            throw new SystemException(ResultCode.INDEX_DELETE_FAILED);
//        }
//
//        return true;
//    }
//
//    @Override
//    public void updateEsIndexed(String noticeId, Integer esIndexed) {
//        try {
//            if (StringUtils.isBlank(noticeId)) {
//                logger.warn("通知ID为空，无法更新ES索引状态");
//                return;
//            }
//
//            if (esIndexed == null) {
//                logger.warn("ES索引状态为空，无法更新");
//                return;
//            }
//
//            int result = notificationMapper.updateEsIndexed(noticeId, esIndexed);
//            if (result > 0) {
//                logger.info("成功更新通知 {} 的ES索引状态为: {}", noticeId, esIndexed);
//            } else {
//                logger.warn("未找到通知 {}，无法更新ES索引状态", noticeId);
//            }
//        } catch (Exception e) {
//            logger.error("更新ES索引状态失败，通知ID: {}, 状态: {}", noticeId, esIndexed, e);
//            throw new SystemException(ResultCode.NOTIFICATION_UPDATE_FAILED);
//        }
//    }
//
//    @Override
//    @Transactional(rollbackFor = Exception.class)
//    public void updateBatchEsIndexed(List<String> noticeIdList, Integer esIndexed) {
//        try {
//            if (noticeIdList == null || noticeIdList.isEmpty()) {
//                logger.warn("通知ID列表为空，无法批量更新ES索引状态");
//                return;
//            }
//
//            if (esIndexed == null) {
//                logger.warn("ES索引状态为空，无法批量更新");
//                return;
//            }
//
//            // 过滤掉空的通知ID
//            List<String> validNoticeIds = noticeIdList.stream()
//                    .filter(StringUtils::isNotBlank)
//                    .collect(Collectors.toList());
//
//            if (validNoticeIds.isEmpty()) {
//                logger.warn("没有有效的通知ID，跳过批量更新");
//                return;
//            }
//
//            int result = notificationMapper.updateBatchEsIndexed(validNoticeIds, esIndexed);
//            logger.info("成功批量更新 {} 个通知的ES索引状态为: {}", result, esIndexed);
//
//        } catch (Exception e) {
//            logger.error("批量更新ES索引状态失败，通知ID列表: {}, 状态: {}", noticeIdList, esIndexed, e);
//            throw new SystemException(ResultCode.NOTIFICATION_UPDATE_FAILED);
//        }
//    }
//
//    @Override
//    public List<Notification> selectUnindexedNotifications() {
//        try {
//            List<Notification> notifications = notificationMapper.selectUnindexedNotifications();
//            logger.info("查询到 {} 个未索引的通知", notifications.size());
//            return notifications;
//        } catch (Exception e) {
//            logger.error("查询未索引的通知失败", e);
//            throw new SystemException(ResultCode.NOTIFICATION_QUERY_FAILED);
//        }
//    }
//
//    @Override
//    public List<Notification> selectBatchUnindexed(int batchSize) {
//        try {
//            if (batchSize <= 0) {
//                logger.warn("批次大小必须大于0，当前值: {}", batchSize);
//                return new ArrayList<>();
//            }
//
//            List<Notification> notifications = notificationMapper.selectBatchUnindexed(batchSize);
//            logger.info("查询到 {} 个未索引的通知（批次大小: {}）", notifications.size(), batchSize);
//            return notifications;
//        } catch (Exception e) {
//            logger.error("分批次查询未索引的通知失败，批次大小: {}", batchSize, e);
//            throw new SystemException(ResultCode.NOTIFICATION_QUERY_FAILED);
//        }
//    }
//
//    @Override
//    public int countUnindexedNotifications() {
//        try {
//            int count = notificationMapper.countUnindexedNotifications();
//            logger.info("未索引的通知数量: {}", count);
//            return count;
//        } catch (Exception e) {
//            logger.error("统计未索引的通知数量失败", e);
//            throw new SystemException(ResultCode.NOTIFICATION_QUERY_FAILED);
//        }
//    }
//
//    @Override
//    public List<Notification> selectIndexedNotifications() {
//        try {
//            List<Notification> notifications = notificationMapper.selectIndexedNotifications();
//            logger.info("查询到 {} 个已索引的通知", notifications.size());
//            return notifications;
//        } catch (Exception e) {
//            logger.error("查询已索引的通知失败", e);
//            throw new SystemException(ResultCode.NOTIFICATION_QUERY_FAILED);
//        }
//    }
//
//    /**
//     * 标记通知为已索引
//     * @param noticeId 通知ID
//     */
//    public void markAsIndexed(String noticeId) {
//        updateEsIndexed(noticeId, 1);
//    }
//
//    /**
//     * 标记通知为未索引
//     * @param noticeId 通知ID
//     */
//    public void markAsUnindexed(String noticeId) {
//        updateEsIndexed(noticeId, 0);
//    }
//
//    /**
//     * 批量标记通知为已索引
//     * @param noticeIdList 通知ID列表
//     */
//    public void markBatchAsIndexed(List<String> noticeIdList) {
//        updateBatchEsIndexed(noticeIdList, 1);
//    }
//
//    /**
//     * 批量标记通知为未索引
//     * @param noticeIdList 通知ID列表
//     */
//    public void markBatchAsUnindexed(List<String> noticeIdList) {
//        updateBatchEsIndexed(noticeIdList, 0);
//    }


}

