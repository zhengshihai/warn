package com.tianhai.warn.controller;

import com.tianhai.warn.annotation.LogOperation;
import com.tianhai.warn.annotation.RequirePermission;
import com.tianhai.warn.constants.Constants;
import com.tianhai.warn.enums.ResultCode;
import com.tianhai.warn.exception.BusinessException;
import com.tianhai.warn.model.SysUser;
import com.tianhai.warn.query.SysUserQuery;
import com.tianhai.warn.service.SuperAdminService;
import com.tianhai.warn.service.SysUserService;
import com.tianhai.warn.service.VerificationService;
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
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 班级管理员管理控制器
 */
@Controller
@RequestMapping("/sysuser")
public class SysUserController {

    private static final Logger logger = LoggerFactory.getLogger(SysUserController.class);

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private SuperAdminService superAdminService;

    @Autowired
    private VerificationService verificationService;

    @GetMapping
    public String sysUser(HttpSession session, Model model) {
        Object sysUser = session.getAttribute(Constants.SESSION_ATTRIBUTE_USER);
        if (sysUser instanceof SysUser user) {
            model.addAttribute("name", user.getName());
            model.addAttribute("email", user.getEmail());
            model.addAttribute(Constants.SESSION_ATTRIBUTE_ROLE,
                    session.getAttribute(Constants.SESSION_ATTRIBUTE_ROLE));
        }

        return "staff-dashboard";
    }

    /**
     * 根据ID获取班级管理员信息
     */
    @GetMapping("/{id}")
    @ResponseBody
    @RequirePermission(roles = Constants.SUPER_ADMIN)
    @LogOperation("超级管理员获取班级管理员信息")
    public Result<SysUser> getSysUserById(@PathVariable Integer id) {
        return Result.success(sysUserService.getSysUserById(id));
    }

    /**
     * 根据邮箱获取班级管理员信息
     */
    @GetMapping("/email/{email}")
    @ResponseBody
    public Result<SysUser> getSysUserByEmail(@PathVariable String email) {
        return Result.success(sysUserService.getSysUserByEmail(email));
    }

    /**
     * 获取所有班级管理员列表
     */
    @GetMapping("/page-list")
    @ResponseBody
    @RequirePermission(roles = Constants.SUPER_ADMIN)
    @LogOperation("超级管理员分页查询班级管理员信息")
    public Result<PageResult<SysUser>> getSysUserListPage(SysUserQuery query) {
        if (query == null) {
            logger.error("查询条件不合规， query: {}", query);
            throw new BusinessException(ResultCode.PARAMETER_ERROR);
        }

        // 分页参数校验
        if (query.getPageNum() == null || query.getPageNum() < 1) {
            query.setPageNum(Constants.DEFAULT_PAGE_NUM);
        }
        if (query.getPageSize() == null || query.getPageSize() < 1) {
            query.setPageSize(Constants.DEFAULT_PAGE_SIZE);
        }

        PageResult<SysUser> sysUserList = sysUserService.selectByPageQuery(query);
        // 没有数据时返回空列表
        if (sysUserList == null || sysUserList.getData() == null
                || sysUserList.getData().isEmpty()) {
            return Result.success(new PageResult<>());
        }

        return Result.success(sysUserList);
    }

    /**
     * 条件查询班级管理员
     */
    @PostMapping("/search")
    @ResponseBody
    public Result<List<SysUser>> searchSysUsers(@RequestBody SysUserQuery query) {
        return Result.success(sysUserService.selectByCondition(query));
    }

    /**
     * 新增班级管理员
     */
    @PostMapping
    @ResponseBody
    public Result<SysUser> insertSysUser(@RequestBody SysUser sysUser) {
        return Result.success(sysUserService.insertSysUser(sysUser));
    }

    /**
     * 班级管理员更新班级管理员信息
     */
    @PostMapping("/update/per-info")
    @ResponseBody
    @RequirePermission(roles = Constants.SYSTEM_USER)
    @LogOperation("班级管理员修改班级管理员信息")
    public Result<Void> updateSysUserByOneself(@RequestBody SysUser sysUser) {
        // 获取当前登录的班级管理员的信息
        HttpSession session = SessionUtils.getSession(false);
        if (session == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        Object sessionUser = session.getAttribute(Constants.SESSION_ATTRIBUTE_USER);
        if (!(sessionUser instanceof SysUser)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        // 校验修改的信息是否合规
        validateUpdateInfo(sysUser);

        SysUser currentSysUser = RoleObjectCaster.cast(Constants.SYSTEM_USER, sessionUser);
        sysUser.setId(currentSysUser.getId());

        // 调用 Service 层处理更新
        sysUserService.updatePersonalInfo(sysUser, currentSysUser.getEmail());

        sysUser.setPassword(null); // 清除密码
        session.setAttribute(Constants.SESSION_ATTRIBUTE_USER, sysUser); // 更新 session 中的用户信息

        return Result.success();
    }

    @PostMapping("/super-admin/update/per-info")
    @ResponseBody
    @RequirePermission(roles = Constants.SUPER_ADMIN)
    @LogOperation("超级管理员修改班级管理员信息")
    public Result<?> updateSysUserBySuperAdmin(@RequestBody SysUser newSysUserInfo) {
        // 校验超级管理员状态
        verificationService.checkSuperAdminStatus();

        if (newSysUserInfo.getId() == null || newSysUserInfo.getId() <= 0) {
            // 密码脱敏
            newSysUserInfo.setPassword(null);
            logger.error("提交的班级管理员信息缺少id或者id不合法，newSysUserInfo:{}", newSysUserInfo);
            throw new BusinessException(ResultCode.PARAMETER_ERROR);
        }

        // 处理更新
        sysUserService.updatePersonalInfoBySuperAdmin(newSysUserInfo);

        return Result.success();
    }

    /**
     * 删除班级管理员
     */
    @DeleteMapping("/{id}")
    @ResponseBody
    @RequirePermission(roles = Constants.SUPER_ADMIN)
    @LogOperation("超级管理员删除班级管理员信息")
    public Result<Void> deleteSysUser(@PathVariable Integer id) {
        // 校验超级管理员状态
        verificationService.checkSuperAdminStatus();

        if (id == null || id <= 0) {
            logger.error("提交的班级管理员id不合法，id：{}", id);
            throw new BusinessException(ResultCode.PARAMETER_ERROR);
        }

        sysUserService.deleteById(id);

        return Result.success();
    }

    /**
     * 修改班级管理员状态
     */
    @GetMapping("/update-status/{id}/{status}")
    @ResponseBody
    @RequirePermission(roles = Constants.SUPER_ADMIN)
    @LogOperation("超级管理员修改班级管理员状态")
    public Result<Void> updateStatus(@PathVariable Integer id, @PathVariable String status) {
        // 校验超级管理员状态
        verificationService.checkSuperAdminStatus();

        if (id == null || id <= 0) {
            logger.error("班级管理员 id: {} 不合规，无法启用或禁用", id);
            throw new BusinessException(ResultCode.PARAMETER_ERROR);
        }
        if (StringUtils.isBlank(status) || !(status.equals("ENABLE") || status.equals("DISABLE"))) {
            logger.error("状态不合规，status: {}", status);
            throw new BusinessException(ResultCode.PARAMETER_ERROR);
        }

        SysUser sysUser = SysUser.builder().id(id).status(status).build();
        sysUserService.updateSysUser(sysUser);

        return Result.success();
    }

    /**
     * 根据角色查询班级管理员列表
     */
    @GetMapping("/role/{role}")
    @ResponseBody
    public Result<List<SysUser>> getSysUsersByRole(@PathVariable String role) {
        return Result.success(sysUserService.getSysUsersByRole(role));
    }

    /**
     * 更新班级管理员最后登录时间
     */
    @PutMapping("/{id}/last-login")
    @ResponseBody
    public Result<?> updateSysUserLastLoginTime(@PathVariable Integer id) {
        sysUserService.updateLastLoginTime(id);
        return Result.success();
    }

    private void validateUpdateInfo(SysUser sysUser) {
        // 创建错误信息列表
        List<String> errors = new ArrayList<>();

        // 验证必填字段 TODO 前端补充工号
        // sysUserNo , name, phone, email, password,
        // if (StringUtils.isBlank(sysUser.getSysUserNo())) {
        // errors.add("工号不能为空");
        // }
        if (StringUtils.isBlank(sysUser.getName())) {
            errors.add("姓名不能为空");
        }
        if (StringUtils.isBlank(sysUser.getPhone())) {
            errors.add("手机号不能为空");
        } else if (!sysUser.getPhone().matches("^[1][3-9][0-9]{9}$")) {
            errors.add("手机号格式不正确");
        }

        if (!errors.isEmpty()) {
            throw new BusinessException(String.join(";", errors));
        }

    }
}