package com.tianhai.warn.controller;

import com.tianhai.warn.constants.Constants;
import com.tianhai.warn.enums.ResultCode;
import com.tianhai.warn.exception.BusinessException;
import com.tianhai.warn.model.CalculationResult;
import com.tianhai.warn.model.WarningRule;
import com.tianhai.warn.service.WarningRuleService;
import com.tianhai.warn.utils.DateUtils;
import com.tianhai.warn.utils.Result;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 预警规则控制器
 */
@Controller
@RequestMapping("/warning-rule")
public class WarningRuleController {

    private static final Logger logger = LoggerFactory.getLogger(WarningRuleController.class);

    @Autowired
    private WarningRuleService warningRuleService;

    /**
     * 跳转到预警规则列表页面
     */
    @GetMapping("/list")
    public String list(Model model) {
        List<WarningRule> rules = warningRuleService.selectAll();
        model.addAttribute("rules", rules);
        return "warning-rule/list";
    }

    /**
     * 跳转到添加预警规则页面
     */
    @GetMapping("/add")
    public String toAdd() {
        return "warning-rule/add";
    }

    /**
     * 添加预警规则
     */
    @PostMapping("/add")
    @ResponseBody
    public String add(@RequestBody WarningRule rule) {
        try {
            warningRuleService.insert(rule);
            return "success";
        } catch (Exception e) {
            return "error";
        }
    }

    /**
     * 跳转到编辑预警规则页面
     */
    @GetMapping("/edit/{id}")
    public String toEdit(@PathVariable Integer id, Model model) {
        WarningRule rule = warningRuleService.selectById(id);
        model.addAttribute("rule", rule);
        return "warning-rule/edit";
    }

    /**
     * 更新预警规则
     */
    @PostMapping("/update")
    @ResponseBody
    public String update(@RequestBody WarningRule rule) {
        try {
            warningRuleService.update(rule);
            return "success";
        } catch (Exception e) {
            return "error";
        }
    }

    /**
     * 删除预警规则
     */
    @PostMapping("/delete/{id}")
    @ResponseBody
    public String delete(@PathVariable Integer id) {
        try {
            warningRuleService.deleteById(id);
            return "success";
        } catch (Exception e) {
            return "error";
        }
    }

    /**
     * 根据条件查询预警规则
     */
    @PostMapping("/search")
    @ResponseBody
    public List<WarningRule> search(@RequestBody WarningRule rule) {
        return warningRuleService.selectByCondition(rule);
    }

    /**
     * 根据状态查询预警规则
     */
    @GetMapping("/status/{status}")
    @ResponseBody
    public List<WarningRule> getByStatus(@PathVariable String status) {
        return warningRuleService.selectByStatus(status);
    }

    /**
     * 更新预警规则状态
     */
    @PostMapping("/update-status")
    @ResponseBody
    public String updateStatus(@RequestParam Integer id, @RequestParam String status) {
        try {
            warningRuleService.updateStatus(id, status);
            return "success";
        } catch (Exception e) {
            return "error";
        }
    }
}