package com.tianhai.warn.controller;

import com.tianhai.warn.annotation.LogOperation;
import com.tianhai.warn.annotation.RequirePermission;
import com.tianhai.warn.constants.Constants;
import com.tianhai.warn.enums.ResultCode;
import com.tianhai.warn.exception.BusinessException;
import com.tianhai.warn.exception.SystemException;
import com.tianhai.warn.model.SysUser;
import com.tianhai.warn.query.SysUserQuery;
import com.tianhai.warn.service.SysUserService;
import com.tianhai.warn.utils.PageResult;
import com.tianhai.warn.utils.Result;

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
 * 系统用户管理控制器
 */
@Controller
@RequestMapping("/sysuser")
public class SysUserController {

    private static final Logger logger = LoggerFactory.getLogger(SysUserController.class);

    @Autowired
    private SysUserService sysUserService;

//    @Autowired
//    private HttpSession session;

    @GetMapping
    public String sysuser(HttpSession session, Model model) {
        Object sysuser = session.getAttribute("user");
        if (sysuser instanceof SysUser user) {
            model.addAttribute("name", user.getName());
            model.addAttribute("email", user.getEmail());
            model.addAttribute("role", session.getAttribute("role"));
        }

//        return "sysuser";
        return "staff-dashboard";
    }


    /**
     * 根据ID获取系统用户信息
     */
    @GetMapping("/{id}")
    @ResponseBody
    public Result<SysUser> getSysUserById(@PathVariable Integer id) {
        return Result.success(sysUserService.getSysUserById(id));
    }

    /**
     * 根据邮箱获取系统用户信息
     */
    @GetMapping("/email/{email}")
    @ResponseBody
    public Result<SysUser> getSysUserByEmail(@PathVariable String email) {
        return Result.success(sysUserService.getSysUserByEmail(email));
    }

    /**
     * 获取所有系统用户列表
     */
//    @GetMapping("/list")
//    @ResponseBody
//    public Result<List<SysUser>> getAllSysUsers() {
//        return Result.success(sysUserService.getAllSysUsers());
//    }
    @GetMapping("/page-list")
    @ResponseBody
    @RequirePermission(roles = Constants.SUPER_ADMIN)
    @LogOperation("超级管理员分页查询班级管理员信息")
    public Result<PageResult<SysUser>> getSysUserListPage(SysUserQuery query) {
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

        PageResult<SysUser> sysUserList = sysUserService.selectByPageQuery(query);
        // 没有数据时返回空列表
        if (sysUserList == null || sysUserList.getData() == null
             || sysUserList.getData().isEmpty()) {
            return Result.success(new PageResult<>());
        }

        return Result.success(sysUserList);
    }

    /**
     * 条件查询系统用户
     */
    @PostMapping("/search")
    @ResponseBody
    public Result<List<SysUser>> searchSysUsers(@RequestBody SysUserQuery query) {
        return Result.success(sysUserService.selectByCondition(query));
    }

    /**
     * 新增系统用户
     */
    @PostMapping
    @ResponseBody
    public Result<SysUser> insertSysUser(@RequestBody SysUser sysUser) {
        return Result.success(sysUserService.insertSysUser(sysUser));
    }

    /**
     * 更新班级管理员信息
     */
    @PostMapping("/update/per-info")
    @ResponseBody
    @LogOperation("更新班级管理员信息")
    public Result<?> updateSysUser(@RequestBody SysUser sysUser) {
        //获取当前登录的系统管理员信息
        HttpSession session = SessionUtils.getSession(false);
        if (session == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        Object user = session.getAttribute("user");
        if (!(user instanceof SysUser)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        validateUpdateInfo(sysUser);

        SysUser currentSysUser = (SysUser) user;
        sysUser.setId(currentSysUser.getId());

        //调用 Service 层处理更新
        sysUserService.updatePersonalInfo(
                sysUser, currentSysUser.getEmail());


        sysUser.setPassword(null); // 清除密码
        session.setAttribute("user", sysUser); // 更新 session 中的用户信息

        return Result.success(ResultCode.SUCCESS);
    }

    /**
     * 删除系统用户
     */
    @DeleteMapping("/{id}")
    @ResponseBody
    public Result<?> deleteSysUser(@PathVariable Integer id) {
        sysUserService.deleteSysUser(id);
        return Result.success();
    }

    /**
     * 根据角色查询系统用户列表
     */
    @GetMapping("/role/{role}")
    @ResponseBody
    public Result<List<SysUser>> getSysUsersByRole(@PathVariable String role) {
        return Result.success(sysUserService.getSysUsersByRole(role));
    }

    /**
     * 更新系统用户最后登录时间
     */
    @PutMapping("/{id}/last-login")
    @ResponseBody
    public Result<?> updateSysUserLastLoginTime(@PathVariable Integer id) {
        sysUserService.updateLastLoginTime(id);
        return Result.success();
    }

    private void validateUpdateInfo(SysUser sysUser) {
        //创建错误信息列表
        List<String> errors =  new ArrayList<>();

        //验证必填字段 TODO 前端补充工号
        //sysUserNo , name, phone, email, password,
//        if (StringUtils.isBlank(sysUser.getSysUserNo())) {
//            errors.add("工号不能为空");
//        }
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