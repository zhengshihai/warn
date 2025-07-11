package com.tianhai.warn.service.impl;

import com.tianhai.warn.enums.JobRole;
import com.tianhai.warn.enums.ResultCode;
import com.tianhai.warn.enums.UserRole;
import com.tianhai.warn.exception.BusinessException;
import com.tianhai.warn.exception.SystemException;
import com.tianhai.warn.mapper.NotificationReceiverMapper;
import com.tianhai.warn.model.NotificationReceiver;
import com.tianhai.warn.query.NotificationReceiverQuery;
import com.tianhai.warn.service.NotificationReceiverService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationReceiverServiceImpl implements NotificationReceiverService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationReceiverServiceImpl.class);

    @Autowired
    private NotificationReceiverMapper notificationReceiverMapper;

    @Override
    public List<NotificationReceiver> selectByCondition(NotificationReceiverQuery query) {
        if(query == null) {
            logger.error("查询参数为空, notificationReceiver: {}", query);
            throw new BusinessException(ResultCode.PARAMETER_ERROR);
        }

        if (query.getId() != null && query.getId() <= 0) {
            logger.error("查询参数id不合法, notificationReceiver: {}", query);
            throw new BusinessException(ResultCode.PARAMETER_ERROR);
        }

        String receiverRole = query.getReceiverRole();
        if (receiverRole != null && !validateReceiverRole(receiverRole)) {
            logger.error("查询参数receiverRole不合法, notificationReceiver: {}", query);
        }

        return notificationReceiverMapper.selectByCondition(query);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insert(NotificationReceiver notRec) {
        if (notRec == null) {
            logger.error("插入参数为空, notificationReceiver: {}", notRec);
            throw new BusinessException(ResultCode.PARAMETER_ERROR);
        }

        int affectRow;
        try {
            affectRow = notificationReceiverMapper.insert(notRec);
            return affectRow;
        } catch (Exception e) {
            logger.error("往notification插入信息失败, notificationReceiver: {}", notRec, e);
            throw new SystemException(ResultCode.ERROR);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertBatch(List<NotificationReceiver> notificationReceiverList) {
        if (notificationReceiverList == null || notificationReceiverList.isEmpty()) {
            logger.error("批量插入参数为空, notificationReceiverList: {}", notificationReceiverList);
            throw new BusinessException(ResultCode.PARAMETER_ERROR);
        }

        int affectRow;
        try {
            affectRow = notificationReceiverMapper.insertBatch(notificationReceiverList);
            return affectRow;
        } catch (Exception e) {
            logger.error("往notification批量插入信息失败, notificationReceiverList: {}", notificationReceiverList, e);
            throw new SystemException(ResultCode.ERROR);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateBatch(List<NotificationReceiver> existingNotRecList) {
        if (existingNotRecList == null || existingNotRecList.isEmpty()) {
            logger.error("批量更新参数为空, existingNotRecList: {}", existingNotRecList);
            throw new BusinessException(ResultCode.PARAMETER_ERROR);
        }

        int affectRow;
        try {
            affectRow = notificationReceiverMapper.updateBatch(existingNotRecList);
            return affectRow;
        } catch (Exception e) {
            logger.error("往notification批量更新信息失败, existingNotRecList: {}", existingNotRecList, e);
            throw new SystemException(ResultCode.ERROR);
        }
    }

    private boolean validateReceiverRole(String receiverRole) {
        // 判断是否属于基本的用户角色
        if (UserRole.isValidRole(receiverRole.toLowerCase())) {
            return true;
        } else {
            return JobRole.isValidRole(receiverRole.toLowerCase());
        }
    }
}
