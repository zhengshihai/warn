package com.tianhai.warn.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.jhlabs.composite.DodgeComposite;
import com.tianhai.warn.constants.Constants;
import com.tianhai.warn.enums.ResultCode;
import com.tianhai.warn.exception.BusinessException;
import com.tianhai.warn.mapper.NotificationMapper;
import com.tianhai.warn.model.DormitoryManager;
import com.tianhai.warn.model.Notification;
import com.tianhai.warn.model.Student;
import com.tianhai.warn.model.SysUser;
import com.tianhai.warn.query.NotificationQuery;
import com.tianhai.warn.service.NotificationService;
import com.tianhai.warn.utils.PageResult;
import com.tianhai.warn.utils.RoleObjectCaster;
import org.aspectj.weaver.ast.Not;
import org.eclipse.tags.shaded.org.apache.bcel.generic.IF_ACMPEQ;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.lang.constant.Constable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 通知信息服务实现类
 */
@Service
//todo 插入通知时，需要更新到redis
public class NotificationServiceImpl implements NotificationService {

    private static final String NOTIFICATION_CACHE_KEY = "notifications:";

    private static final long CACHE_EXPIRE_TIME = 600;

    @Autowired
    private NotificationMapper notificationMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public PageResult<Notification> selectByPageQuery(String userRoleStr,
                                                      Object currentUser,
                                                      NotificationQuery query) {

        String cacheKey = null;
        if (userRoleStr.equalsIgnoreCase(Constants.STUDENT)) {
             Student student = RoleObjectCaster.cast(userRoleStr, currentUser);
             cacheKey = NOTIFICATION_CACHE_KEY + Constants.STUDENT + ":" + student.getStudentNo();

             query.setTargetId(student.getStudentNo());
        }

        if (userRoleStr.equalsIgnoreCase(Constants.DORMITORY_MANAGER)) {
            DormitoryManager dormitoryManager = RoleObjectCaster.cast(userRoleStr, currentUser);
            cacheKey = NOTIFICATION_CACHE_KEY + Constants.DORMITORY_MANAGER + ":" + dormitoryManager.getManagerId();

            query.setTargetId(dormitoryManager.getManagerId());
        }

        if (userRoleStr.equalsIgnoreCase(Constants.SYSTEM_USER)) {
            SysUser sysUser = RoleObjectCaster.cast(userRoleStr, currentUser);
            cacheKey = NOTIFICATION_CACHE_KEY + Constants.SYSTEM_USER + ":" + sysUser.getSysUserNo();

            query.setTargetId(sysUser.getSysUserNo());
        }

        if (cacheKey == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXISTS);
        }

        // 尝试从缓存中获取通知列表
//        @SuppressWarnings("unchecked")
//        List<Notification> cachedNotifications =
//                (List<Notification>) redisTemplate.opsForValue().get(cacheKey);
//        if (cachedNotifications != null) {
//            int fromIndex = (query.getPageNum() - 1) * query.getPageSize();
//            int toIndex = Math.min(fromIndex + query.getPageSize(), cachedNotifications.size());
//            List<Notification> pageCachedNotifications = cachedNotifications.subList(fromIndex, toIndex);
//
//            return buildPageResult(pageCachedNotifications);
//        }

        // 如果缓存中没有，则从数据库中查询
        List<Notification> notifications;
        PageResult<Notification> result;
        try (Page<Notification> page = PageHelper.startPage(query.getPageNum(), query.getPageSize())) {
            notifications = notificationMapper.selectByCondition(query);
            result = buildPageResult(notifications);
        }

        //存入缓存 10分钟有效期
        redisTemplate.opsForValue().set(cacheKey, notifications, CACHE_EXPIRE_TIME, TimeUnit.SECONDS);

        return result;
    }

    /**
     * 构建分页结果
     * @param notifications
     * @return
     */
    private PageResult<Notification> buildPageResult(List<Notification> notifications) {
        PageInfo<Notification> pageInfo = new PageInfo<>(notifications);
        PageResult<Notification> result = new PageResult<>();
        result.setData(pageInfo.getList());
        result.setTotal((int) pageInfo.getTotal());
        result.setPageNum(pageInfo.getPageNum());
        result.setPageSize(pageInfo.getPageSize());

        return result;
    }

    /**
     * 检查用户是否有权限读取该通知
     * @param userRoleStr
     * @param currentUser
     * @param noticeId
     * @return
     */
    @Override
    public boolean hasPermissionToRead(String userRoleStr, Object currentUser, String noticeId) {
        NotificationQuery query = new NotificationQuery();
        query.setNoticeId(noticeId);

        if (userRoleStr.equalsIgnoreCase(Constants.STUDENT)) {
            Student student = RoleObjectCaster.cast(userRoleStr, currentUser);
            query.setTargetId(student.getStudentNo());
        }

        if (userRoleStr.equalsIgnoreCase(Constants.DORMITORY_MANAGER)) {
            DormitoryManager dormitoryManager = RoleObjectCaster.cast(userRoleStr, currentUser);
            query.setTargetId(dormitoryManager.getManagerId());
        }

        if (userRoleStr.equalsIgnoreCase(Constants.SYSTEM_USER)) {
            SysUser sysUser = RoleObjectCaster.cast(userRoleStr, currentUser);
            query.setTargetId(sysUser.getSysUserNo());
        }

        Notification notification = notificationMapper.selectByCondition(query).get(0);
        if (notification == null) {
            return false;
        }

        return isNotificationForUser(userRoleStr, currentUser, notification);

    }

    /**
     * 批量检查用户是否有权限读取通知
     * @param userRoleStr
     * @param currentUser
     * @param noticeIds
     * @return
     */
    @Override
    public boolean hasPermissionToReadBatch(String userRoleStr, Object currentUser, List<String> noticeIds) {
        if (noticeIds == null || noticeIds.isEmpty()) {
            return false;
        }

        NotificationQuery query = new NotificationQuery();
        query.setNoticeIds(noticeIds);
        List<Notification> notifications = notificationMapper.selectByCondition(query);

        //检查所有通知是否都针对该用户
        return notifications.stream()
                .allMatch(notification -> isNotificationForUser(userRoleStr, currentUser, notification));
    }

    /**
     * 检查通知是否属于当前用户
     * @param userRoleStr
     * @param currentUser
     * @param notification
     * @return
     */
    private boolean isNotificationForUser(String userRoleStr, Object currentUser, Notification notification) {
        if (userRoleStr.equalsIgnoreCase(Constants.STUDENT)) {
            Student student = RoleObjectCaster.cast(userRoleStr, currentUser);
            return notification.getTargetId().equals(student.getStudentNo());
        }

        if (userRoleStr.equalsIgnoreCase(Constants.DORMITORY_MANAGER)) {
            DormitoryManager dormitoryManager = RoleObjectCaster.cast(userRoleStr, currentUser);
            return notification.getTargetId().equals(dormitoryManager.getManagerId());
        }

        if (userRoleStr.equalsIgnoreCase(Constants.SYSTEM_USER)) {
            SysUser sysUser = RoleObjectCaster.cast(userRoleStr, currentUser);
            return notification.getTargetId().equals(sysUser.getSysUserNo());
        }

        return false;
    }

    @Override
    public Map<String, Object> getNotificationStats(Object currentUser) {
        Map<String, Object> statsMap = new HashMap<>();

        //todo 获取未读通知的数量

        //todo 获取各类型通知的数量

        return statsMap;
    }

    @Override
    public Notification selectById(Integer id) {
        return notificationMapper.selectById(id);
    }

    @Override
    public List<Notification> selectAll() {
        return notificationMapper.selectAll();
    }

    @Override
    public List<Notification> selectByCondition(NotificationQuery notificationQuery) {
        return notificationMapper.selectByCondition(notificationQuery);
    }

    @Override
    public List<Notification> selectByTarget(String targetType, String targetId) {
        return notificationMapper.selectByTarget(targetType, targetId);
    }

    @Override
    public List<Notification> selectByType(String type) {
        return notificationMapper.selectByType(type);
    }

    @Override
    public List<Notification> selectByStatus(String status) {
        return notificationMapper.selectByStatus(status);
    }

    @Override
    public int insert(Notification notification) {
        return notificationMapper.insert(notification);
    }

    @Override
    public int update(Notification notification) {
        return notificationMapper.update(notification);
    }

    @Override
    public int deleteById(Integer id) {
        return notificationMapper.deleteById(id);
    }

    @Override
    public int updateStatus(String noticeId, String status) {
        return notificationMapper.updateStatus(noticeId, status);
    }

    @Override
    public int batchUpdateStatus(List<String> ids, String status) {
        return notificationMapper.batchUpdateStatus(ids, status);
    }

    @Override
    public Notification selectByNoticeId(String noticeId) {
        return notificationMapper.selectByNoticeId(noticeId);
    }
}