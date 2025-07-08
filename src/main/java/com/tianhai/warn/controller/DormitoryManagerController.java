package com.tianhai.warn.controller;

import com.tianhai.warn.annotation.LogOperation;
import com.tianhai.warn.annotation.RequirePermission;
import com.tianhai.warn.constants.Constants;
import com.tianhai.warn.enums.ResultCode;
import com.tianhai.warn.exception.BusinessException;
import com.tianhai.warn.exception.SystemException;
import com.tianhai.warn.model.DormitoryManager;
import com.tianhai.warn.model.SysUser;
import com.tianhai.warn.query.DormitoryManagerQuery;
import com.tianhai.warn.service.DormitoryManagerService;
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
 * 宿管信息控制器
 */
@Controller
@RequestMapping("/dorman")
public class DormitoryManagerController {
    private static final Logger logger = LoggerFactory.getLogger(DormitoryManagerController.class);

    @Autowired
    private DormitoryManagerService dormitoryManagerService;

    @Autowired
    private VerificationService verificationService;


    @GetMapping
    public String dorMan(HttpSession session, Model model) {
        Object dorMan = session.getAttribute(Constants.SESSION_ATTRIBUTE_USER);

        if (dorMan instanceof DormitoryManager dormitoryManager) {
            model.addAttribute("name", dormitoryManager.getName());
            model.addAttribute("email", dormitoryManager.getEmail());
            model.addAttribute("role", session.getAttribute("role"));
        }

        return "staff-dashboard";
    }

    /**
     * 宿管更新宿管信息
     */
    @PostMapping("/update/per-info")
    @ResponseBody
    @RequirePermission(roles = {Constants.SYSTEM_USER, Constants.SUPER_ADMIN})
    @LogOperation("更新宿管信息")
    public Result<?> updateByOneself(@RequestBody DormitoryManager manager) {
        // 获取当前登录用户
        HttpSession session = SessionUtils.getSession(false);
        assert session != null;
        Object user = session.getAttribute(Constants.SESSION_ATTRIBUTE_USER);

        if (!(user instanceof DormitoryManager)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        validateUpdateInfo(manager);

        // 设置当前用户的 ID
        DormitoryManager currentManager = RoleObjectCaster.cast(Constants.DORMITORY_MANAGER, user);
        manager.setId(currentManager.getId());

        // 调用 Service 层处理更新
        dormitoryManagerService.updatePersonalInfo(manager, currentManager.getEmail());

        manager.setPassword(null);
        session.setAttribute(Constants.SESSION_ATTRIBUTE_USER, manager);

        return Result.success(ResultCode.SUCCESS);
    }

    /**
     * 超级管理员更新宿管信息 todo 有bug
     */
    @PostMapping("/super-admin/update/per-info")
    @ResponseBody
    @RequirePermission(roles = Constants.SUPER_ADMIN)
    @LogOperation("超级管理员更新宿管信息")
    public Result<?> updateBySuperAdmin(@RequestBody DormitoryManager newDorManInfo) {
        // 检查超级管理员状态
        verificationService.checkSuperAdminStatus();

        if (newDorManInfo.getId() == null || newDorManInfo.getId() <= 0) {
            // 密码脱敏
            newDorManInfo.setPassword(null);
            logger.error("提交的宿管信息缺少id或者id不合法，newDorManInfo: {}", newDorManInfo);
            throw new BusinessException(ResultCode.PARAMETER_ERROR);
        }

        int affectRow = dormitoryManagerService.updatePersonalInfoBySuperAdmin(newDorManInfo);
        if (affectRow <= 0) {
            logger.error("超级管理员更新宿管信息失败， newDorManInfo: {}", newDorManInfo);
            throw new SystemException(ResultCode.ERROR);
        }

        return Result.success();
    }

    /**
     * 超级管理员删除宿管
     */
    @DeleteMapping("delete/{id}")
    @ResponseBody
    @RequirePermission(roles = Constants.SUPER_ADMIN)
    @LogOperation("超级管理员删除宿管信息")
    public Result<?> deleteDormitoryManager(@PathVariable Integer id) {
        // 校验超级管理员状态
        verificationService.checkSuperAdminStatus();

        if (id == null || id <= 0) {
            logger.error("提交的宿管id不合法，id：{}", id);
            throw new BusinessException(ResultCode.PARAMETER_ERROR);
        }

        dormitoryManagerService.deleteById(id);

        return Result.success();
    }

    /**
     * 修改宿管状态
     */
    @GetMapping("update-status/{id}/{status}")
    @ResponseBody
    @RequirePermission(roles = Constants.SUPER_ADMIN)
    @LogOperation("超级管理员修改宿管状态")
    public Result<Void> updateStatus(@PathVariable Integer id, @PathVariable String status) {
        // 校验超级管理员状态
        verificationService.checkSuperAdminStatus();

        if (id == null || id <= 0) {
            logger.error("提交的宿管id不合法，id：{}", id);
            throw new BusinessException(ResultCode.PARAMETER_ERROR);
        }
        if (StringUtils.isBlank(status) || !(status.equals("ON_DUTY") || status.equals("OFF_DUTY"))) {
            logger.error("状态不合规, status: {}", status);
            throw new BusinessException(ResultCode.PARAMETER_ERROR);
        }

        DormitoryManager dormitoryManager = DormitoryManager.builder().id(id).status(status).build();
        dormitoryManagerService.updateStatus(dormitoryManager);

        return Result.success();
    }

    /**
     * 获取所有宿管分页列表
     */
    @GetMapping("/page-list")
    @ResponseBody
    @RequirePermission(roles = Constants.SUPER_ADMIN)
    @LogOperation("超级管理员分页查询宿管信息")
    public Result<PageResult<DormitoryManager>> getDorManListPage(DormitoryManagerQuery query) {
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

        PageResult<DormitoryManager> dorManList = dormitoryManagerService.selectByPageQuery(query);
        // 没有结果时返回空列表
        if (dorManList == null || dorManList.getData() == null
                || dorManList.getData().isEmpty()) {
            return Result.success(new PageResult<>());
        }

        return Result.success(dorManList);
    }

    /**
     * 根据条件查询宿管
     */
    @PostMapping("/search")
    @ResponseBody
    @RequirePermission(roles = {Constants.SUPER_ADMIN, Constants.DORMITORY_MANAGER})
    @LogOperation("根据条件查询宿管信息")
    public List<DormitoryManager> search(@RequestBody DormitoryManager manager) {
        if (manager == null) {
            logger.error("查询条件不合规, manager: {}", manager);
            throw new BusinessException(ResultCode.PARAMETER_ERROR);
        }

        return dormitoryManagerService.selectByCondition(manager);
    }

    /**
     * 校验修改信息
     * 
     * @param manager 修改信息
     */
    private void validateUpdateInfo(DormitoryManager manager) {
        // 创建错误信息列表
        List<String> errors = new ArrayList<>();

        // 验证必填字段
        if (StringUtils.isBlank(manager.getName())) {
            errors.add("姓名不能为空");
        }

        if (StringUtils.isBlank(manager.getBuilding())) {
            errors.add("宿舍楼不能为空");
        }

        if (StringUtils.isBlank(manager.getPhone())) {
            errors.add("手机号不能为空");
        } else if (!manager.getPhone().matches("^1[3-9]\\d{9}$")) {
            errors.add("手机号格式不正确");
        }

        if (StringUtils.isBlank(manager.getEmail())) {
            errors.add("邮箱不能为空");
        } else if (!manager.getEmail().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            errors.add("邮箱格式不正确");
        }

        if (!errors.isEmpty()) {
            throw new BusinessException(String.join("; ", errors));
        }
    }
}