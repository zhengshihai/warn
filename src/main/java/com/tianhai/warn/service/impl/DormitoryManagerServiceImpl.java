package com.tianhai.warn.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.tianhai.warn.constants.Constants;
import com.tianhai.warn.enums.ResultCode;
import com.tianhai.warn.exception.BusinessException;
import com.tianhai.warn.exception.SystemException;
import com.tianhai.warn.mapper.DormitoryManagerMapper;
import com.tianhai.warn.model.*;
import com.tianhai.warn.query.ApplicationQuery;
import com.tianhai.warn.query.DormitoryManagerQuery;
import com.tianhai.warn.query.ExplanationQuery;
import com.tianhai.warn.service.ApplicationService;
import com.tianhai.warn.service.DormitoryManagerService;
import com.tianhai.warn.service.ExplanationService;
import com.tianhai.warn.service.NotificationService;
import com.tianhai.warn.utils.EmailValidator;
import com.tianhai.warn.utils.PageResult;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;

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

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private ExplanationService explanationService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private AsyncTaskExecutor asyncTaskExecutor;

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
    public void updateLastLoginTime(Integer id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ResultCode.VALIDATE_FAILED);
        }

        dormitoryManagerMapper.updateLastLoginTime(id);
    }

    @Override
    public DormitoryManager getDorManByEmail(String email) {
        return dormitoryManagerMapper.selectByEmail(email);
    }

    // todo
    @Override
    public List<String> getManagedDormitories(String managerId) {
        return List.of();
    }

    @Override
    public Set<String> selectAllManagerId() {
        Set<String> managerIdSet = dormitoryManagerMapper.selectAllManagerId();
        if (managerIdSet.isEmpty()) {
            logger.info("宿管表找不到任何宿管信息");
        }

        return managerIdSet;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePersonalInfo(DormitoryManager manager, String currentEmail) {
        // 检查数据是否被修改
        DormitoryManager currentManager = getDorManByEmail(currentEmail);
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
            manager.setVersion(currentManager.getVersion());
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
    public PageResult<DormitoryManager> selectByPageQuery(DormitoryManagerQuery query) {
        List<DormitoryManager> dormitoryManagerList;
        PageResult<DormitoryManager> result;

        try(Page<DormitoryManager> page = PageHelper.startPage(query.getPageNum(), query.getPageSize())) {
            dormitoryManagerList = dormitoryManagerMapper.selectByPageQuery(query);
            result = buildPageResult(dormitoryManagerList);
        }

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class) // todo 有bug 重复修改会报错
    public int updatePersonalInfoBySuperAdmin(DormitoryManager newDorManInfo) {
        if (newDorManInfo == null || newDorManInfo.getId() == null) {
            logger.error("dormitoryManager的id不能为空");
            throw new BusinessException(ResultCode.PARAMETER_ERROR);
        }

        // 通过id判断是否存在该宿管
        DormitoryManager existingDorMan = dormitoryManagerMapper.selectById(newDorManInfo.getId());
        if (existingDorMan == null) {
            logger.error("更新失败，该宿管账号不存在");
            throw new BusinessException(ResultCode.USER_NOT_EXISTS);
        }

        // 邮箱校验以及唯一性判断
        // emailNeedUpdate 更新信息是否包含邮箱
        boolean emailNeedUpdate = newDorManInfo.getEmail() != null &&
                !newDorManInfo.getEmail().equals(existingDorMan.getEmail());
        if (emailNeedUpdate) {
            // 校验邮箱格式是否正确
            boolean formatValid = checkEmailFormat(newDorManInfo.getEmail());
            if (!formatValid) {
                logger.error("超级管理员新提交的邮箱不合规, email: {}", newDorManInfo.getEmail());
                throw new BusinessException(ResultCode.PARAMETER_ERROR);
            }

            // 通过email查询宿管
            DormitoryManager query = DormitoryManager.builder().email(newDorManInfo.getEmail()).build();
            List<DormitoryManager> dorManListByEmail = dormitoryManagerMapper.selectByCondition(query);
            DormitoryManager dorManByEmail = new DormitoryManager();
            if (!dorManListByEmail.isEmpty()) {
                dorManByEmail = dorManListByEmail.get(0);
            }

            // emailUsed 新的邮箱是否已经被使用
            boolean emailUsed = dorManByEmail != null &&
                    !Objects.equals(dorManByEmail.getId(), newDorManInfo.getId());
            if (emailUsed) {
                logger.error("新设定的邮箱已被占用");
                throw new BusinessException(ResultCode.EMAIL_USED);
            }
        }

        if (StringUtils.isNotBlank(newDorManInfo.getPassword())) {
            newDorManInfo.setPassword(DigestUtils.md5DigestAsHex(newDorManInfo.getPassword().getBytes()));
        }

        // 因项目未设置单设备登录等原因 故此处使用乐观锁安全并发更新
        newDorManInfo.setUpdateTime(new Date());

        // 如果数据库宿管记录版本号为空，则手动设置为0
        Integer version = existingDorMan.getVersion();
        if (version == null) { version = 0; }
        newDorManInfo.setVersion(version);

        int affectedRows = dormitoryManagerMapper.update(newDorManInfo);
        if (affectedRows == 0) {
            logger.error("更新失败，数据可能被其他用户更改或者更新信息不正确, newDorManInfo: {}", newDorManInfo);
            throw new SystemException(ResultCode.ERROR);
        }

        // 如果managerId发生改变，则异步更新其他表的宿管信息
        boolean managerIdNeedUpdate = newDorManInfo.getManagerId() != null &&
                !newDorManInfo.getManagerId().equals(existingDorMan.getManagerId());
        if (managerIdNeedUpdate) {
            asyncTaskExecutor.execute(() -> {
                try {
                    updateDorManInOtherTablesAsync(existingDorMan, newDorManInfo);
                } catch (Exception e) {
                    logger.error("异步更新其他表的宿管信息失败", e);
                    throw new SystemException(ResultCode.ERROR);
                }
            });
        }

        return affectedRows;
    }

    @Override
    public int updateStatus(DormitoryManager dormitoryManager) {
        DormitoryManager existingDorMan = dormitoryManagerMapper.selectById(dormitoryManager.getId());
        if (existingDorMan == null) {
            logger.error("不存在该宿管, dormitoryManager: {}", dormitoryManager);
            return 0;
        }

        String status = dormitoryManager.getStatus();
        if (status == null ||
                !(status.equals(Constants.ON_DUTY) || status.equals(Constants.OFF_DUTY))) {
            logger.error("宿管状态不合规, status: {}", status);
            throw new BusinessException(ResultCode.PARAMETER_ERROR);
        }

        return dormitoryManagerMapper.updateStatus(dormitoryManager);
    }

    // 异步更新其他表的宿管信息
    private void updateDorManInOtherTablesAsync(DormitoryManager oldDorMan, DormitoryManager newDorMan) {
        // 更新application由该宿管审核的晚归申请
        ApplicationQuery applicationQuery = ApplicationQuery.builder()
                .auditPerson(oldDorMan.getManagerId())
                .build();
        List<Application> applicationList = applicationService.selectByCondition(applicationQuery);
        if (!applicationList.isEmpty()) {
            List<Application> updatedList = applicationList.stream()
                            .map(application -> {
                                Application newApp = new Application();
                                newApp.setId(newApp.getId());
                                newApp.setAuditPerson(newDorMan.getManagerId());
                                return newApp;
                            }).toList();
            int updatedRow = applicationService.updateBatch(updatedList);
            logger.info("宿管工号发生变更，在application表异步更新 {} 条数据", updatedRow);
        }

        // 更新explanation由该宿管审核的晚归说明
        ExplanationQuery explanationQuery = ExplanationQuery.builder()
                .auditPerson(oldDorMan.getManagerId())
                .build();
        List<Explanation> explanationList = explanationService.selectByCondition(explanationQuery);
        if (!explanationList.isEmpty()) {
            List<Explanation> updatedList = explanationList.stream()
                    .map(explanation -> {
                        Explanation newExp = new Explanation();
                        newExp.setId(explanation.getId());
                        newExp.setAuditPerson(newDorMan.getManagerId());
                        return newExp;
                    }).toList();
            int updatedRow = explanationService.updateBatch(updatedList);
            logger.info("宿管工号发生变更，在explanation表异步更新 {} 条数据", updatedRow);
        }

        // 不更新 notification表 和 notification_receiver表
    }

    /**
     * 通用方法：更新某类业务实体中由指定审核人（auditPerson）负责的记录，将其更新为新的审核人。
     *
     * <p>适用于类似“由宿管负责的审核字段需要更新”的场景，避免重复写查询-构造-更新逻辑。</p>
     *
     * @param query             查询条件对象（如 ApplicationQuery、ExplanationQuery 等）
     * @param queryFunction     查询函数，传入 query，返回需要被更新的数据列表
     * @param buildUpdated      构造更新后对象的函数，接受原对象和新审核人ID，返回更新后的对象（通常只更新 auditPerson 字段和主键）
     * @param batchUpdateFunc   批量更新方法，对生成的新列表执行数据库更新操作，并返回更新数量
     * @param newAuditPerson    新的审核人ID，用于替换原记录中的 auditPerson 字段
     * @param <T>               业务实体类型（如 Application、Explanation）
     * @param <Q>               查询对象类型（如 ApplicationQuery、ExplanationQuery）
     * @return 实际更新的记录数量
     */
    private <T, Q> int updateAuditPersonInOtherTable(
            Q query,
            Function<Q, List<T>> queryFunction,
            BiFunction<T, String, T> buildUpdated,
            Function<List<T>, Integer> batchUpdateFunc,
            String newAuditPerson){

        List<T> oldList = queryFunction.apply(query);
        if (!oldList.isEmpty()) {
            List<T> updatedList = oldList.stream()
                    .map(item -> buildUpdated.apply(item, newAuditPerson))
                    .toList();
            return batchUpdateFunc.apply(updatedList);
        }

        return 0;
    }

    // 构建分页结果
    private PageResult<DormitoryManager> buildPageResult(List<DormitoryManager> dormitoryManagerList) {
        PageInfo<DormitoryManager> pageInfo = new PageInfo<>(dormitoryManagerList);

        PageResult<DormitoryManager> result = new PageResult<>();
        result.setData(pageInfo.getList());
        result.setTotal((int) pageInfo.getTotal());
        result.setPageNum(pageInfo.getPageNum());
        result.setPageSize(pageInfo.getPageSize());

        return result;
    }

    /**
     * 校验邮箱格式是否正确
     *
     * @param email 邮箱格式
     * @return 校验结果
     */
    private boolean checkEmailFormat(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }

        return email.matches(Constants.EMAIL_REGEX);
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