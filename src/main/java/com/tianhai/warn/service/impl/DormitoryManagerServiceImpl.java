package com.tianhai.warn.service.impl;

import com.tianhai.warn.enums.ResultCode;
import com.tianhai.warn.exception.BusinessException;
import com.tianhai.warn.exception.SystemException;
import com.tianhai.warn.mapper.DormitoryManagerMapper;
import com.tianhai.warn.model.DormitoryManager;
import com.tianhai.warn.service.DormitoryManagerService;
import com.tianhai.warn.utils.EmailValidator;
import com.tianhai.warn.utils.Result;
import lombok.extern.flogger.Flogger;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 宿管信息服务实现类
 */
@Service
public class DormitoryManagerServiceImpl implements DormitoryManagerService {

    private static final Logger logger = LoggerFactory.getLogger(DormitoryManager.class);

    @Autowired
    private DormitoryManagerMapper dormitoryManagerMapper;

    @Autowired
    private EmailValidator emailValidator;

    @Autowired
    private RedissonClient redissonClient;

    @Override
    public DormitoryManager selectById(Integer id) {
        DormitoryManager dormitoryManager = dormitoryManagerMapper.selectById(id);
        dormitoryManager.setPassword(null);

        return dormitoryManager;
    }

    @Override
    public DormitoryManager selectByManagerId(String managerId) {
        return dormitoryManagerMapper.selectByManagerId(managerId);
    }

    @Override
    public DormitoryManager selectByUsername(String name) {
        return dormitoryManagerMapper.selectByName(name);
    }

    @Override
    public List<DormitoryManager> selectAll() {
        return dormitoryManagerMapper.selectAll();
    }

    @Override
    public List<DormitoryManager> selectByCondition(DormitoryManager manager) {
        return dormitoryManagerMapper.selectByCondition(manager);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insert(DormitoryManager manager) {
        manager.setCreateTime(new Date());
        manager.setUpdateTime(new Date());
        return dormitoryManagerMapper.insert(manager);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int update(DormitoryManager manager) {
        manager.setUpdateTime(new Date());
        // 更新信息
        return dormitoryManagerMapper.update(manager);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateLastLoginTime(Integer id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ResultCode.VALIDATE_FAILED);
        }

        dormitoryManagerMapper.updateLastLoginTime(id);
    }

    @Override
    public DormitoryManager getDormanByEmail(String email) {
        return dormitoryManagerMapper.selectByEmail(email);
    }

    // todo
    @Override
    public List<String> getManagedDormitories(String managerId) {
        return List.of();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePersonalInfo(DormitoryManager manager, String currentEmail) {
        // 检查数据是否被修改
        DormitoryManager currentManager = getDormanByEmail(currentEmail);
        if (currentManager == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXISTS);
        }

        // 检查邮箱是否可用
        if (!emailValidator.isEmailAvailable(manager.getEmail(), currentEmail)) {
            throw new BusinessException(ResultCode.EMAIL_USED);
        }

        // 本项目设定不同用户是使用唯一的邮箱，考虑到可能的并发修改问题，使用redisson加锁
        String lockKey = "lock:email:" + manager.getEmail();
        RLock lock = null;
        boolean isLocked = false;
        int waitTime = 5;
        int lockTime = 3;
        int updateResult;

        try {
            if (!currentEmail.equals(manager.getEmail())) {
                lock = redissonClient.getLock(lockKey);
                isLocked = lock.tryLock(waitTime, lockTime, TimeUnit.SECONDS);
                if (!isLocked) {
                    logger.error("该邮箱正在被修改");
                    throw new BusinessException(ResultCode.EMAIL_UPDATING);
                }
            }

            // 如果修改了密码，需要加密
            if (manager.getPassword() != null && !manager.getPassword().trim().isEmpty()) {
                manager.setPassword(DigestUtils.md5DigestAsHex(manager.getPassword().getBytes()));
            } else {
                // 如果没有修改密码，保持原密码不变
                manager.setPassword(currentManager.getPassword());
            }

            // 设置乐观锁版本号
            manager.setUpdateTime(new Date());
            updateResult = dormitoryManagerMapper.update(manager);
        } catch (InterruptedException e) {
            logger.error("获取邮箱锁失败");
            throw new SystemException(ResultCode.EMAIL_LOCKED_FAIL);
        } finally {
            if (isLocked) {
                try {
                    lock.unlock();
                } catch (IllegalMonitorStateException e) {
                    logger.error("Redisson锁释放失败", e);
                }
            }
        }

        // 更新信息
        if (updateResult > 0) {
            // todo 如果邮箱发生变化，需要更新其他相关表的数据
            if (!currentEmail.equals(manager.getEmail())) {
            }
        } else {
            throw new BusinessException(ResultCode.USER_UPDATE_FAILED);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertBatch(List<DormitoryManager> dormitoryManagerList) {
        if (dormitoryManagerList == null || dormitoryManagerList.isEmpty()) {
            logger.error("批量插入的宿管信息为空");
            return 0;
        }

        return dormitoryManagerMapper.insertBatch(dormitoryManagerList);
    }

    @Override
    @Transactional
    public int deleteById(Integer id) {
        return dormitoryManagerMapper.deleteById(id);
    }

    @Override
    public List<DormitoryManager> selectByBuilding(String building) {
        return dormitoryManagerMapper.selectByBuilding(building);
    }

    @Override
    public DormitoryManager getByEmail(String email) {
        return dormitoryManagerMapper.selectByEmail(email);
    }

}