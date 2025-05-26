package com.tianhai.warn.controller;

import com.tianhai.warn.constants.Constants;
import com.tianhai.warn.enums.ResultCode;
import com.tianhai.warn.exception.BusinessException;
import com.tianhai.warn.model.DormitoryManager;
import com.tianhai.warn.service.DormitoryManagerService;
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
import org.springframework.util.DigestUtils;

import java.util.ArrayList;
import java.util.Date;
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


    @GetMapping
    public String dorman(HttpSession session, Model model) {
//        Object dorman = session.getAttribute("user");
//        session = SessionUtils.getSession(true);
        Object dorman = session.getAttribute(Constants.SESSION_ATTRIBUTE_USER);

        if (dorman instanceof DormitoryManager dormitoryManager) {
            model.addAttribute("name", dormitoryManager.getName());
            model.addAttribute("email", dormitoryManager.getEmail());
            model.addAttribute("role", session.getAttribute("role"));
        }

        return "staff-dashboard";
    }

    /**
     * 跳转到宿管列表页面
     */
    @GetMapping("/list")
    public String list(Model model) {
        List<DormitoryManager> managers = dormitoryManagerService.selectAll();
        model.addAttribute("managers", managers);
        return "dormitory-manager/list";
    }

    /**
     * 跳转到添加宿管页面
     */
    @GetMapping("/add")
    public String toAdd() {
        return "dormitory-manager/add";
    }

    /**
     * 添加宿管
     */
    @PostMapping("/add")
    @ResponseBody
    public String add(@RequestBody DormitoryManager manager) {
        try {
            dormitoryManagerService.insert(manager);
            return "success";
        } catch (Exception e) {
            return "error";
        }
    }

    /**
     * 跳转到编辑宿管页面
     */
    @GetMapping("/edit/{id}")
    public String toEdit(@PathVariable Integer id, Model model) {
        DormitoryManager manager = dormitoryManagerService.selectById(id);
        model.addAttribute("manager", manager);
        return "dormitory-manager/edit";
    }

    /**
     * 更新宿管信息
     */
    @PostMapping("/update/per-info")
    @ResponseBody
    public Result<?> update(@RequestBody DormitoryManager manager) {
        // 获取当前登录用户
        HttpSession session = SessionUtils.getSession(false);
        Object user = session.getAttribute(Constants.SESSION_ATTRIBUTE_USER);

        if (!(user instanceof DormitoryManager)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        validateUpdateInfo(manager);

        // 设置当前用户的 ID
        DormitoryManager currentManager = (DormitoryManager) user;
        manager.setId(currentManager.getId());

        // 调用 Service 层处理更新
        dormitoryManagerService.updatePersonalInfo(manager, currentManager.getEmail());

        manager.setPassword(null);
        session.setAttribute(Constants.SESSION_ATTRIBUTE_USER, manager);

        return Result.success(ResultCode.SUCCESS);
    }

    /**
     * 删除宿管
     */
    @PostMapping("/delete/{id}")
    @ResponseBody
    public String delete(@PathVariable Integer id) {
        try {
            dormitoryManagerService.deleteById(id);
            return "success";
        } catch (Exception e) {
            return "error";
        }
    }

    /**
     * 根据条件查询宿管
     */
    @PostMapping("/search")
    @ResponseBody
    public List<DormitoryManager> search(@RequestBody DormitoryManager manager) {
        return dormitoryManagerService.selectByCondition(manager);
    }

    /**
     * 根据宿舍楼查询宿管
     */
    @GetMapping("/building/{building}")
    @ResponseBody
    public List<DormitoryManager> getByBuilding(@PathVariable String building) {
        return dormitoryManagerService.selectByBuilding(building);
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