package com.tianhai.warn.controller;

import com.tianhai.warn.annotation.LogOperation;
import com.tianhai.warn.annotation.RequirePermission;
import com.tianhai.warn.constants.Constants;
import com.tianhai.warn.enums.ResultCode;
import com.tianhai.warn.exception.BusinessException;
import com.tianhai.warn.model.SuperAdmin;
import com.tianhai.warn.query.SuperAdminQuery;
import com.tianhai.warn.service.SuperAdminService;
import com.tianhai.warn.utils.PageResult;
import com.tianhai.warn.utils.Result;
import com.tianhai.warn.utils.RoleObjectCaster;
import com.tianhai.warn.utils.SessionUtils;
import jakarta.servlet.http.HttpSession;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 超级管理员信息控制器
 */
@Controller
@RequestMapping("/super-admin")
public class SuperAdminController {
    private static final Logger logger = LoggerFactory.getLogger(SuperAdminController.class);

    @Autowired
    private SuperAdminService superAdminService;

    @GetMapping
    public String superAdmin() {
        return "super-admin";
    }

    /**
     * 查找全部超级管理员
     * @return   超级管理员列表
     */
    @GetMapping("/list")
    @ResponseBody
    @RequirePermission(roles = Constants.SUPER_ADMIN)
    @LogOperation("查找全部管理员信息")
    public Result<List<SuperAdmin>> searchAllSuperAdmin() {
        List<SuperAdmin> allSuperAdmins = superAdminService.listAll();

        return Result.success(allSuperAdmins);
    }

    /**
     * 更新超级管理员信息
     * 这里允许超级管理员和非超级管理员的邮箱发生重叠
     * @param updateSuperAdmin        更新信息
     * @return                  更新结果
     */
    @PostMapping("/update/per-info")
    @ResponseBody
    @RequirePermission(roles = Constants.SUPER_ADMIN)
    @LogOperation("超级管理员更新个人信息")
    public Result<?> update(@RequestBody SuperAdmin updateSuperAdmin) {
        HttpSession session = SessionUtils.getSession(false);
        if (session == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        Object user = session.getAttribute(Constants.SESSION_ATTRIBUTE_USER);
        if (!(user instanceof SuperAdmin)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        validateUpdateInfo(updateSuperAdmin);

        SuperAdmin sessionSuperAdmin = RoleObjectCaster.cast(Constants.SUPER_ADMIN, user);

        if (StringUtils.isNotBlank(updateSuperAdmin.getName())) {
            sessionSuperAdmin.setName(updateSuperAdmin.getName());
        }
        if (StringUtils.isNotBlank(updateSuperAdmin.getEmail())) {
            sessionSuperAdmin.setEmail(updateSuperAdmin.getEmail());
        }
        if (StringUtils.isNotBlank(updateSuperAdmin.getPassword())) {
            sessionSuperAdmin.setPassword(updateSuperAdmin.getPassword());
        }

        superAdminService.update(sessionSuperAdmin);

        return Result.success();
    }

    // 校验修改信息是否符合要求
    private void validateUpdateInfo(SuperAdmin superAdmin) {
        if (StringUtils.isNotBlank(superAdmin.getName())) {
            if (superAdmin.getName().length() > 20) {
                logger.error("用户名超过规定长度，name:{}", superAdmin.getName());
                throw new BusinessException(ResultCode.PARAMETER_ERROR);
            }
        }

        if (StringUtils.isNotBlank(superAdmin.getEmail())) {
            if (!superAdmin.getEmail().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                logger.error("邮箱格式不正确，email{}", superAdmin.getEmail());
                throw new BusinessException(ResultCode.PARAMETER_ERROR);
            }
        }
    }

    @GetMapping("/page-list")
    @ResponseBody
    @RequirePermission(roles = Constants.SUPER_ADMIN)
    @LogOperation("超级管理员分页查询超级管理员信息")
    public Result<PageResult<SuperAdmin>> getSuperAdminListPage(SuperAdminQuery query) {
        if (query == null) {
            return Result.error(ResultCode.PARAMETER_ERROR);
        }

        // 分页参数校验
        if (query.getPageNum() == null || query.getPageNum() < 1) {
            query.setPageNum(Constants.DEFAULT_PAGE_NUM);
        }
        if (query.getPageSize() == null || query.getPageSize() < 1) {
            query.setPageSize(Constants.DEFAULT_PAGE_SIZE);
        }

        PageResult<SuperAdmin> superAdminPageResult = superAdminService.selectByPageQuery(query);
        // 没有数据时返回空列表
        if (superAdminPageResult == null || superAdminPageResult.getData() == null
                || superAdminPageResult.getData().isEmpty()) {
            return Result.success(new PageResult<>());
        }

        return Result.success(superAdminPageResult);

    }
}
