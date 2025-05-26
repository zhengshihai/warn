package com.tianhai.warn.controller;

import com.tianhai.warn.model.SystemRule;
import com.tianhai.warn.service.SystemRuleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 系统规则控制器
 */
@Controller
@RequestMapping("/system-rule")
public class SystemRuleController {

    @Autowired
    private SystemRuleService systemRuleService;

    /**
     * 跳转到系统规则列表页面
     */
    @GetMapping("/list")
    public String list(Model model) {
        List<SystemRule> rules = systemRuleService.selectAll();
        model.addAttribute("rules", rules);
        return "system-rule/list";
    }

    /**
     * 跳转到添加系统规则页面
     */
    @GetMapping("/add")
    public String toAdd() {
        return "system-rule/add";
    }

    /**
     * 添加系统规则
     */
    @PostMapping("/add")
    @ResponseBody
    public String add(@RequestBody SystemRule rule) {
        try {
            systemRuleService.insert(rule);
            return "success";
        } catch (Exception e) {
            return "error";
        }
    }

    /**
     * 跳转到编辑系统规则页面
     */
    @GetMapping("/edit/{id}")
    public String toEdit(@PathVariable Integer id, Model model) {
        SystemRule rule = systemRuleService.selectById(id);
        model.addAttribute("rule", rule);
        return "system-rule/edit";
    }

    /**
     * 更新系统规则
     */
    @PostMapping("/update")
    @ResponseBody
    public String update(@RequestBody SystemRule rule) {
        try {
            systemRuleService.update(rule);
            return "success";
        } catch (Exception e) {
            return "error";
        }
    }

    /**
     * 删除系统规则
     */
    @PostMapping("/delete/{id}")
    @ResponseBody
    public String delete(@PathVariable Integer id) {
        try {
            systemRuleService.deleteById(id);
            return "success";
        } catch (Exception e) {
            return "error";
        }
    }

    /**
     * 根据条件查询系统规则
     */
    @PostMapping("/search")
    @ResponseBody
    public List<SystemRule> search(@RequestBody SystemRule rule) {
        return systemRuleService.selectByCondition(rule);
    }

    /**
     * 根据规则键查询系统规则
     */
    @GetMapping("/key/{ruleKey}")
    @ResponseBody
    public SystemRule getByRuleKey(@PathVariable String ruleKey) {
        return systemRuleService.selectByRuleKey(ruleKey);
    }

    /**
     * 根据规则键更新规则值
     */
    @PostMapping("/update-value")
    @ResponseBody
    public String updateRuleValue(@RequestParam String ruleKey, @RequestParam String ruleValue) {
        try {
            systemRuleService.updateRuleValue(ruleKey, ruleValue);
            return "success";
        } catch (Exception e) {
            return "error";
        }
    }
}