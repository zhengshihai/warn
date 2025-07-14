package com.tianhai.warn.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.tianhai.warn.dto.SysUserExcelDTO;
import com.tianhai.warn.enums.ResultCode;
import com.tianhai.warn.exception.BusinessException;
import com.tianhai.warn.exception.SystemException;
import com.tianhai.warn.mapper.SysUserMapper;
import com.tianhai.warn.model.Student;
import com.tianhai.warn.model.SysUser;
import com.tianhai.warn.model.SysUserClass;
import com.tianhai.warn.query.SysUserQuery;
import com.tianhai.warn.service.SysUserClassService;
import com.tianhai.warn.service.SysUserService;
import com.tianhai.warn.utils.EmailValidator;
import com.tianhai.warn.utils.PageResult;
import com.tianhai.warn.utils.Result;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.tags.shaded.org.apache.bcel.generic.IF_ACMPEQ;
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

    @Autowired
    private SysUserClassService sysUserClassService;

    @Override
    public SysUser getSysUserById(Integer id) {
        return sysUserMapper.selectById(id);
    }

    @Override
    public SysUser getSysUserByEmail(String email) {
        return sysUserMapper.selectByEmail(email);
    }

    @Override
    public List<SysUser> selectAll() {
        return sysUserMapper.selectAll();
    }

    @Override
    public SysUser selectById(Integer id) {
        SysUser sysUser = sysUserMapper.selectById(id);
        sysUser.setPassword(null);

        return sysUser;
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
    @Transactional(rollbackFor = Exception.class)
    public SysUser insertSysUser(SysUser user) {
        user.setCreateTime(new Date());
        user.setUpdateTime(new Date());
        sysUserMapper.insert(user);
        return user;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysUser updateSysUser(SysUser sysUser) {
        if (sysUser.getId() == null || sysUser.getId() <= 0) {
            logger.error("修改信息不合规， sysUser:{}", sysUser);
            throw new BusinessException(ResultCode.PARAMETER_ERROR);
        }

        sysUser.setUpdateTime(new Date());

        sysUserMapper.update(sysUser);

        return sysUser;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteById(Integer id) {
        // 从班级管理员查找该用户
        SysUser existingSysUser = sysUserMapper.selectById(id);
        if (existingSysUser == null) {
            logger.info("班级管理员表没有该用户，id:{}", id);
        }

        // 从班级管理员-班级表查找该用户
        int sysUserClassCount = 0;
        if (existingSysUser != null) {
            String sysUserNo = existingSysUser.getSysUserNo();
            sysUserClassCount = sysUserClassService.countBySysUserNo(sysUserNo);
            if (sysUserClassCount == 0) {
                logger.info("班级管理员-班级表没有相关信息，sysUserNo:{}", sysUserNo);
            }
        }

        // 删除班级管理员-班级表的信息
        if (sysUserClassCount > 0) {
            int sysUserClassRow = sysUserClassService.deleteBySysUserNo(existingSysUser.getSysUserNo());
            if (sysUserClassRow < sysUserClassCount) {
                logger.error("在班级管理员-班级表删除班级管理员出现异常，sysUserNo:{}",
                        existingSysUser.getSysUserNo());
                throw new SystemException(ResultCode.ERROR);
            }
            logger.info("在班级管理员-班级表中成功删除 {} 条数据", sysUserClassRow);
        }

        // 删除班级管理员表的信息
        if (existingSysUser != null) {
            int sysUserRow = sysUserMapper.deleteById(id);
            if (sysUserRow <= 0) {
                logger.error("在班级管理员表删除班级管理员出现异常，id:{}", id);
                throw new SystemException(ResultCode.ERROR);
            }
            logger.info("在班级管理员表中成功删除 {} 条数据", sysUserRow);
        }
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
    @Transactional(rollbackFor = Exception.class)
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
    @Transactional(rollbackFor = Exception.class)
    public void updatePersonalInfoBySuperAdmin(SysUser newSysUserInfo) {
        Integer id = newSysUserInfo.getId();
        SysUser sysUserExisting = sysUserMapper.selectById(id);
        if (sysUserExisting == null) {
            logger.info("找不到 id 为 {} 的该班级管理员", id);
            throw new BusinessException(ResultCode.USER_NOT_EXISTS);
        }

        // 检查新邮箱是否已被其他用户（班级管理员 学生 宿管）占用
        if (!emailValidator.isEmailAvailable(newSysUserInfo.getEmail(), sysUserExisting.getEmail())) {
            throw new BusinessException(ResultCode.EMAIL_USED);
        }

        // 本项目设定不同用户是使用唯一的邮箱，考虑到可能的并发修改问题，使用redisson加锁
        String newEmailLockKey = "lock:email:" + newSysUserInfo.getEmail();
        RLock newEmailLock = null;
        boolean newEmailIsLocked = false;
        int waitTime = 5;
        int lockTime = 3;

        // 保证sys_user表和sys_user_class表并发更新安全
        String oldSysUserNo = sysUserExisting.getSysUserNo();
        String newSysUserNo = newSysUserInfo.getSysUserNo();

        String oldSysUserLockKey = "lock:sysUserNo:" + oldSysUserNo;
        String newSysUserLockKey = "lock:sysUserNo:" + newSysUserNo;
        RLock oldSysUserLock = null, newSysUserLock = null;
        boolean oldSysUserIsLocked = false, newSysUserIsLocked = false;

        try {
            // 只有当邮箱发生变化时，才对新邮箱加分布式锁，防止并发下多个用户同时把邮箱改成同一个
            if (!sysUserExisting.getEmail().equals(newSysUserInfo.getEmail())) {
                newEmailLock = redissonClient.getLock(newEmailLockKey);
                newEmailIsLocked = newEmailLock.tryLock(waitTime, lockTime, TimeUnit.SECONDS);
                if (!newEmailIsLocked) {
                    logger.error("获取并发修改email的RLock锁失败, lockKey: {}", newEmailLockKey);
                    throw new SystemException(ResultCode.EMAIL_LOCKED_FAIL);
                }
            }

            // 如果前端传了新密码，则加密后存储，否则保持原密码不变
            boolean passwordNotEmpty = StringUtils.isNotBlank(newSysUserInfo.getPassword());
            if (passwordNotEmpty) {
                newSysUserInfo.setPassword(DigestUtils.md5DigestAsHex(newSysUserInfo.getPassword().getBytes()));
            } else {
                newSysUserInfo.setPassword(sysUserExisting.getPassword());
            }

            // 设置更新时间
            newSysUserInfo.setUpdateTime(new Date());

            // 如果 sysUserNo 发生变化，需要同时锁定旧的和新的 sysUserNo
            if (!oldSysUserNo.equals(newSysUserNo)) {
                oldSysUserLock = redissonClient.getLock(oldSysUserLockKey);
                newSysUserLock = redissonClient.getLock(newSysUserLockKey);
                oldSysUserIsLocked = oldSysUserLock.tryLock(waitTime, lockTime, TimeUnit.SECONDS);
                newSysUserIsLocked = newSysUserLock.tryLock(waitTime, lockTime, TimeUnit.SECONDS);

                if (!oldSysUserIsLocked || !newSysUserIsLocked) {
                    logger.error("获取并发修改sysUserNo的RLock锁失败, oldLockKey: {}, newLockKey: {}",
                            oldSysUserLockKey, newSysUserLockKey);
                    throw new SystemException(ResultCode.SYS_USER_NO_LOCKED_FAIL);
                }
            }

            // 更新信息
            int sysUserUpdateRow, sysUserClassUpdateRow;
            sysUserUpdateRow = sysUserMapper.update(newSysUserInfo);
            if (sysUserUpdateRow == 0) {
                logger.warn("未找到 sys_user_no 为 {} 的班级管理员，无法更新信息", newSysUserInfo.getSysUserNo());
            }

            if (!oldSysUserNo.equals(newSysUserNo)) {
                sysUserClassUpdateRow = sysUserClassService.updateSysUserNo(oldSysUserNo, newSysUserNo, new Date());
                if (sysUserClassUpdateRow == 0) {
                    logger.warn("未找到 sys_user_no 为 {} 的班级管理员，无法更新班级管理员信息", oldSysUserNo);
                }
            }

        } catch (InterruptedException e) {
            logger.error("获取邮箱锁或者班级管理员锁失败", e);
            throw new SystemException(ResultCode.ERROR);
        } catch (Exception e) {
            logger.error("更新班级管理员信息失败，可能是部分信息不符合要求， newSysUser: {}", newSysUserInfo);
            throw new BusinessException(ResultCode.PARAMETER_ERROR);
        } finally {
            // 释放email锁
            if (newEmailIsLocked) {
                try {
                    newEmailLock.unlock();
                } catch (IllegalMonitorStateException e) {
                    logger.warn("Redisson锁释放失败", e);
                }
            }

            // 释放sysUserNo锁
            if (oldSysUserIsLocked)
                oldSysUserLock.unlock();
            if (newSysUserIsLocked)
                newSysUserLock.unlock();
        }

    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertBatch(List<SysUser> sysUserList) {
        if (sysUserList.isEmpty()) {
            return 0;
        }

        return sysUserMapper.insertBatch(sysUserList);
    }

    @Override
    public List<SysUser> selectBySysUserNos(List<String> sysUserNos) {
        logger.debug("Selecting users by sysUserNos: {}", sysUserNos);
        List<SysUser> users = sysUserMapper.selectBySysUserNos(sysUserNos);
        logger.debug("Found {} users", users.size());
        return users;
    }

    @Override
    public PageResult<SysUser> selectByPageQuery(SysUserQuery query) {
        List<SysUser> sysUserList;
        PageResult<SysUser> result;

        try (Page<SysUser> page = PageHelper.startPage(query.getPageNum(), query.getPageSize())) {
            sysUserList = sysUserMapper.selectByCondition(query);
            result = buildPageResult(sysUserList);
        }

        return result;
    }

    @Override
    public List<SysUser> selectAllSysUserNoAndJobRole() {
        List<SysUser> sysUserList = sysUserMapper.selectAllSysUserNoAndJobRole();

        if (sysUserList.isEmpty()) {
            logger.info("班级管理员表中找不到任何班级管理员信息");
        }

        return sysUserList;
    }

    // 构建分页结果
    private PageResult<SysUser> buildPageResult(List<SysUser> sysUserList) {
        PageInfo<SysUser> pageInfo = new PageInfo<>(sysUserList);

        PageResult<SysUser> result = new PageResult<>();
        result.setData(pageInfo.getList());
        result.setTotal((int) pageInfo.getTotal());
        result.setPageNum(pageInfo.getPageNum());
        result.setPageSize(pageInfo.getPageSize());

        return result;
    }


}