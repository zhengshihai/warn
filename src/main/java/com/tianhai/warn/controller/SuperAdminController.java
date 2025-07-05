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
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

/**
 * 超级管理员信息控制器
 */
@Controller
@RequestMapping("/super-admin")
public class SuperAdminController {
    private static final Logger logger = LoggerFactory.getLogger(SuperAdminController.class);

    @Autowired
    private SuperAdminService superAdminService;

    private static final String VALID_COUNT = "validCount";
    private static final String INVALID_LIST = "invalidList";
    private static final String TOTAL_COUNT = "totalCount";

    @GetMapping
    public String superAdmin() {
        return "super-admin";
    }

    /**
     * 查找全部超级管理员
     * 
     * @return 超级管理员列表
     */
    @GetMapping("/list")
    @ResponseBody
    @RequirePermission(roles = Constants.SUPER_ADMIN)
    @LogOperation("查找全部管理员信息")
    public Result<List<SuperAdmin>> searchAllSuperAdmin() {
        List<SuperAdmin> allSuperAdmins = superAdminService.selectAll();

        return Result.success(allSuperAdmins);
    }

    /**
     * 更新超级管理员信息
     * 这里允许超级管理员和非超级管理员的邮箱发生重叠
     * 
     * @param updateSuperAdmin 更新信息
     * @return 更新结果
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

    @PostMapping("/import-students")
    @ResponseBody
    @RequirePermission(roles = Constants.SUPER_ADMIN)
    @LogOperation("超级管理员批量导入用户信息")
    public Result<?> importUserInfoBatch(@RequestParam("file") MultipartFile file,
            @RequestParam("insertUserRole") String insertUserRole) {
        // 校验插入的角色是否合规
        if (StringUtils.isBlank(insertUserRole)) {
            logger.error("批量导入的用户角色不合规， insertRole: {}", insertUserRole);
            throw new BusinessException(ResultCode.USER_ROLE_DISABLE);
        }

        Set<String> roleSet = new HashSet<>(Arrays.asList(Constants.SUPER_ADMIN,
                Constants.STUDENT,
                Constants.DORMITORY_MANAGER,
                Constants.SYSTEM_USER));
        boolean isRequiredUserRole = roleSet.stream()
                .anyMatch(role -> role.equalsIgnoreCase(insertUserRole));
        if (!isRequiredUserRole) {
            logger.error("批量导入的用户角色不合规， insertRole: {}", insertUserRole);
            throw new BusinessException(ResultCode.USER_ROLE_DISABLE);
        }

        // 校验上传的文件类型和大小是否合规（不大于10MB）
        String fileName = file.getOriginalFilename();
        if (fileName == null || !(fileName.endsWith(".xls") || fileName.endsWith(".xlsx"))) {
            logger.error("上传的文件格式不符合要求， fileName: {}", fileName);
            throw new BusinessException(ResultCode.FILE_TYPE_ERROR);
        }
        if (file.getSize() > 10 * 1024 * 1024) {
            logger.error("单次上传的文件大小不能超过10MB");
            throw new BusinessException(ResultCode.FILE_SIZE_ERROR_10MB);
        }

        // 批量导入用户信息
        Map<String, Object> importResult = superAdminService.importExcelInfoBatch(file, insertUserRole);

        // 获取导入结果
        Integer validCount = (Integer) importResult.get(VALID_COUNT);
        List<?> invalidList = (List<?>) importResult.get(INVALID_LIST);
        Integer totalCount = (Integer) importResult.get(TOTAL_COUNT);

        // 构建返回消息
        String message = String.format("导入完成！总计：%d条，成功：%d条，失败：%d条",
                totalCount, validCount, invalidList.size());

        // 如果有失败的数据，返回详细信息
        if (!invalidList.isEmpty()) {
            return Result.success(importResult, message);
        } else {
            return Result.success(importResult, message);
        }
    }
}
