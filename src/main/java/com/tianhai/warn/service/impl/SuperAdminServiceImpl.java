package com.tianhai.warn.service.impl;

import com.tianhai.warn.constants.Constants;
import com.tianhai.warn.enums.ResultCode;
import com.tianhai.warn.exception.BusinessException;
import com.tianhai.warn.exception.SystemException;
import com.tianhai.warn.mapper.SuperAdminMapper;
import com.tianhai.warn.model.SuperAdmin;
import com.tianhai.warn.service.SuperAdminService;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.tags.shaded.org.apache.regexp.RE;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.util.*;
import java.util.stream.Stream;

@Service
public class SuperAdminServiceImpl implements SuperAdminService {

    private static final Logger logger = LoggerFactory.getLogger(SuperAdminServiceImpl.class);

    @Autowired
    private SuperAdminMapper superAdminMapper;


    @Override
    public SuperAdmin getById(Integer id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ResultCode.VALIDATE_FAILED);
        }
        SuperAdmin superAdmin =  superAdminMapper.selectById(id);
        if (superAdmin == null) {
            throw new BusinessException(ResultCode.SUPER_ADMIN_NOT_FOUND);
        }

        return superAdmin;
    }

    @Override
    public SuperAdmin getByEmail(String email) {
        if (!checkEmailFormat(email)) {
            throw new BusinessException(ResultCode.VALIDATE_FAILED);
        }

        SuperAdmin query = SuperAdmin.builder().email(email).build();

        List<SuperAdmin> superAdmins = selectByCondition(query);
        if (superAdmins.isEmpty()) {
            return new SuperAdmin();
        }

        return superAdmins.get(0);
    }

    @Override
    public void updateLastLoginTime(Integer id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ResultCode.VALIDATE_FAILED);
        }

        superAdminMapper.updateLastLoginTime(new Date());
    }

    @Override
    public List<SuperAdmin> listAll() {
        List<SuperAdmin> adminList =  superAdminMapper.selectAll();
        if (adminList.isEmpty()) {
            logger.warn("不存在任何超级管理员");
        }

        return adminList;
    }

    @Override
    //todo 需要增加对无限邮的防护
    public Integer insert(SuperAdmin admin) {
        if (admin == null) {
            throw new BusinessException("超级管理员信息不能为空");
        }
        if (StringUtils.isBlank(admin.getEmail())) {
            throw new BusinessException("邮箱不能为空");
        }

        SuperAdmin query =SuperAdmin.builder().email(admin.getEmail()).build();
        List<SuperAdmin> dbSuperAdmin = superAdminMapper.selectByCondition(query);

        if (dbSuperAdmin != null) {
            throw new BusinessException("邮箱已被注册");
        }

        admin.setPassword(DigestUtils.md5DigestAsHex(admin.getPassword().getBytes()));

        if (admin.getEnabled() == null) {
            admin.setEnabled(1); // 默认启用
        }
        admin.setCreateTime(new Date());
        admin.setUpdateTime(new Date());

        try {
            int affectedRow = superAdminMapper.insert(admin);
            if (affectedRow <= 0) {
                throw new SystemException(ResultCode.ERROR);
            }
            return affectedRow;
        } catch (DuplicateKeyException e) {
            throw new BusinessException("该邮箱已被注册");
        }
    }


    /**
     * 更新信息
     * 本项目设定超级管理员邮箱可以和班级管理员（SysUser)的邮箱相同，
     * 而宿管 班级管理员 学生三者之间的邮箱不能相同
     * @param newSuperAdminInfo          更新信息
     * @return                          成功更新的行数
     */
    @Override
    public Integer update(SuperAdmin newSuperAdminInfo) {
        if (newSuperAdminInfo == null || newSuperAdminInfo.getId() == null) {
            logger.error("admin的id不能为空");
            throw new BusinessException(ResultCode.VALIDATE_FAILED);
        }

        // 通过id判断是否存在该超级管理员
        SuperAdmin existingAdmin = superAdminMapper.selectById(newSuperAdminInfo.getId());
        if (existingAdmin == null) {
            throw new BusinessException("更新失败，该超级管理员账号不存在");
        }

        // 通过email查询超级管理员
        SuperAdmin query = SuperAdmin.builder().email(newSuperAdminInfo.getEmail()).build();
        List<SuperAdmin> adminListByEmail = superAdminMapper.selectByCondition(query);

        SuperAdmin adminByEmail = new SuperAdmin();
        if (adminListByEmail != null) {
            adminByEmail = adminListByEmail.get(0);
        }

        // 邮箱校验以及唯一性判断
        // emailNeedUpdate 更新信息是否包含邮箱
        boolean emailNeedUpdate = newSuperAdminInfo.getEmail() != null &&
                !newSuperAdminInfo.getEmail().equals(existingAdmin.getEmail());
        if (emailNeedUpdate) {
            boolean formatValid = checkEmailFormat(newSuperAdminInfo.getEmail());

            // emailUsed 新的邮箱是否已被使用
            boolean emailUsed = adminByEmail != null &&
                    !Objects.equals(adminByEmail.getId(), newSuperAdminInfo.getId());
            if (emailUsed) {
                logger.error("新设定的邮箱已被占用");
                throw new BusinessException(ResultCode.EMAIL_USED);
            }
        }

        if (StringUtils.isNotBlank(newSuperAdminInfo.getPassword())) {
            newSuperAdminInfo.setPassword(DigestUtils.md5DigestAsHex(
                    newSuperAdminInfo.getPassword().getBytes()));
        }

        // 因项目未设置单设备登录等原因 故此处使用乐观锁安全并发更新
        newSuperAdminInfo.setUpdateTime(new Date());
        newSuperAdminInfo.setVersion(existingAdmin.getVersion());

        int affectedRows = superAdminMapper.update(newSuperAdminInfo);
        if (affectedRows == 0) {
            throw new ConcurrentModificationException("更新失败，数据可能被其他用户更改");
        }

        return affectedRows;
    }

    /**
     * 校验邮箱格式是否正确
     * @param email         邮箱格式
     * @return              校验结果
     */
    private boolean checkEmailFormat(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }

        return email.matches(Constants.EMAIL_REGEX);
    }

    /**
     * 删除信息
     * @param id    主键id
     * @return      删除结果
     */
    @Override
    public Integer deleteById(Integer id) {
        if (id == null || id <= 0) {
            logger.error("无效的超级管理员id");
            throw new BusinessException(ResultCode.VALIDATE_FAILED);
        }

        SuperAdmin existingAdmin = superAdminMapper.selectById(id);
        if (existingAdmin == null) {
            throw new BusinessException(ResultCode.SUPER_ADMIN_NOT_FOUND);
        }

        // 防止删除唯一的超级管理员
        int adminCount = superAdminMapper.countAll();
        if (adminCount <= 1) {
            logger.error("不能删除唯一的超级管理员");
            throw new SystemException(ResultCode.SUPER_ADMIN_DELETE_FAILED);
        }

        int affectedRows = superAdminMapper.deleteById(id);
        if (affectedRows == 0) {
            throw new SystemException(ResultCode.ERROR);
        }

        return affectedRows;
    }

    @Override
    public List<SuperAdmin> selectByCondition(SuperAdmin superAdmin) {
        if (superAdmin == null || isEmptyCondition(superAdmin)) {
            logger.error("请至少提供一个查询条件");
            throw new BusinessException(ResultCode.VALIDATE_FAILED);
        }

        List<SuperAdmin> adminList = superAdminMapper.selectByCondition(superAdmin);

        if (adminList.isEmpty()) {
            logger.info("不存在满足该条件的超级管理员");
            return new ArrayList<>();
        }

        return adminList;
    }

    /**
     * 判断筛选条件是否全为空（version属性例外）
     * @param admin     筛选条件
     * @return          条件是否全为空
     */
    private boolean isEmptyCondition(SuperAdmin admin) {
        return Stream.of(
                admin.getId(),
                StringUtils.stripToNull(admin.getName()),
                StringUtils.stripToNull(admin.getEmail()),
                admin.getEnabled(),
                admin.getCreateTime(),
                admin.getUpdateTime()
        ).allMatch(Objects::isNull);
    }
}
