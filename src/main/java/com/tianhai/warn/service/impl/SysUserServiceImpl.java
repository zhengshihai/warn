package com.tianhai.warn.service.impl;

import com.tianhai.warn.enums.ResultCode;
import com.tianhai.warn.exception.BusinessException;
import com.tianhai.warn.exception.SystemException;
import com.tianhai.warn.mapper.SysUserMapper;
import com.tianhai.warn.model.SysUser;
import com.tianhai.warn.query.SysUserQuery;
import com.tianhai.warn.service.SysUserService;
import com.tianhai.warn.utils.EmailValidator;
import com.tianhai.warn.utils.Result;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 系统用户服务实现类
 */
@Service
public class SysUserServiceImpl implements SysUserService {

    private static final Logger logger = LoggerFactory.getLogger(SysUserServiceImpl.class);

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private EmailValidator emailValidator;

    @Autowired
    private RedissonClient redissonClient;

    @Override
    public SysUser getSysUserById(Integer id) {
        return sysUserMapper.selectById(id);
    }

    @Override
    public SysUser getSysUserByEmail(String email) {
        return sysUserMapper.selectByEmail(email);
    }

    @Override
    public List<SysUser> getAllSysUsers() {
        return sysUserMapper.selectAll();
    }

    @Override
    public List<SysUser> selectByCondition(SysUserQuery query) {
        logger.debug("Selecting users by condition: {}", query);
        if (query.getSysUserNos() != null) {
            logger.debug("sysUserNos size: {}", query.getSysUserNos().size());
            logger.debug("sysUserNos content: {}", query.getSysUserNos());
        }
        List<SysUser> users = sysUserMapper.selectByCondition(query);
        logger.debug("Found {} users", users.size());
        if (users.isEmpty()) {
            logger.debug("No users found with the given conditions");
        } else {
            logger.debug("Found users: {}", users);
        }
        return users;
    }

    @Override
    @Transactional
    public SysUser insertSysUser(SysUser user) {
        user.setCreateTime(new Date());
        user.setUpdateTime(new Date());
        sysUserMapper.insert(user);
        return user;
    }

    @Override
    @Transactional
    public SysUser updateSysUser(SysUser user) {
        user.setUpdateTime(new Date());
        sysUserMapper.update(user);
        return user;
    }

    @Override
    @Transactional
    public void deleteSysUser(Integer id) {
        sysUserMapper.deleteById(id);
    }

    @Override
    public List<SysUser> getSysUsersByRole(String jobRole) {
        return sysUserMapper.selectByRole(jobRole);
    }

    @Override
    @Transactional
    public void updateLastLoginTime(Integer id) {
        sysUserMapper.updateLastLoginTime(id);
    }

    @Override
    public void updatePersonalInfo(SysUser sysUser, String currentEmail) {
        // 检查数据是否被修改
        SysUser currentSysUser = getSysUserByEmail(currentEmail);
        if (currentSysUser == null) {
            throw new BusinessException(ResultCode.USER_NOT_EXISTS);
        }

        // 检查邮箱是否可用
        if (!emailValidator.isEmailAvailable(sysUser.getEmail(), currentEmail)) {
            throw new BusinessException(ResultCode.EMAIL_USED);
        }

        // 本项目设定不同用户是使用唯一的邮箱，考虑到可能的并发修改问题，使用redisson加锁
        String lockKey = "lock:email:" + sysUser.getEmail();
        RLock lock = null;
        boolean isLocked = false;
        int waitTime = 5;
        int lockTime = 3;
        int updateResult;

        try {
            // 只有邮箱发生变化时，才需要加锁
            if (!currentEmail.equals(sysUser.getEmail())) {
                lock = redissonClient.getLock(lockKey);
                isLocked = lock.tryLock(waitTime, lockTime, TimeUnit.SECONDS);
                if (!isLocked) {
                    logger.error("该邮箱正在被修改");
                    throw new BusinessException(ResultCode.EMAIL_UPDATING);
                }
            }

            // 如果密码不为空，则更新密码
            if (sysUser.getPassword() != null && !sysUser.getPassword().trim().isEmpty()) {
                // 对密码进行加密
                sysUser.setPassword(DigestUtils.md5DigestAsHex(sysUser.getPassword().getBytes()));
            } else {
                // 如果密码为空，保持原密码不变
                sysUser.setPassword(currentSysUser.getPassword());
            }

            // 设置更新时间
            sysUser.setUpdateTime(new Date());

            // 更新学生信息
            updateResult = sysUserMapper.update(sysUser);
        } catch (InterruptedException e) {
            logger.error("获取邮箱锁失败");
            throw new SystemException(ResultCode.EMAIL_LOCKED_FAIL);
        } finally {
            if (isLocked) {
                try {
                    lock.unlock();
                } catch (IllegalMonitorStateException e) {
                    logger.warn("Redisson锁释放失败", e);
                }
            }
        }

        if (updateResult > 0) {
            // 如果邮箱发生变化，需要更新其他相关表的数据
            if (!currentEmail.equals(sysUser.getEmail())) {
                // TODO: 更新其他相关表的数据
            }
        } else {
            throw new BusinessException(ResultCode.USER_UPDATE_FAILED);
        }
    }

    @Override
    public List<SysUser> selectBySysUserNos(List<String> sysUserNos) {
        logger.debug("Selecting users by sysUserNos: {}", sysUserNos);
        List<SysUser> users = sysUserMapper.selectBySysUserNos(sysUserNos);
        logger.debug("Found {} users", users.size());
        return users;
    }
}