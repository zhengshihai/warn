package com.tianhai.warn.controller;

import com.tianhai.warn.model.SystemLog;
import com.tianhai.warn.service.SystemLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

/**
 * 系统日志控制器
 */
@Controller
@RequestMapping("/system-log")
public class SystemLogController {

    @Autowired
    private SystemLogService systemLogService;

    /**
     * 跳转到系统日志列表页面
     */
    @GetMapping("/list")
    public String list(Model model) {
        List<SystemLog> logs = systemLogService.selectAll();
        model.addAttribute("logs", logs);
        return "system-log/list";
    }

    /**
     * 根据条件查询系统日志
     */
    @PostMapping("/search")
    @ResponseBody
    public List<SystemLog> search(@RequestBody SystemLog log) {
        return systemLogService.selectByCondition(log);
    }

    /**
     * 根据用户名查询系统日志
     */
    @GetMapping("/username/{username}")
    @ResponseBody
    public List<SystemLog> getByUsername(@PathVariable String username) {
        return systemLogService.selectByUsername(username);
    }

    /**
     * 根据操作内容查询系统日志
     */
    @GetMapping("/operation/{operation}")
    @ResponseBody
    public List<SystemLog> getByOperation(@PathVariable String operation) {
        return systemLogService.selectByOperation(operation);
    }

    /**
     * 根据状态查询系统日志
     */
    @GetMapping("/status/{status}")
    @ResponseBody
    public List<SystemLog> getByStatus(@PathVariable String status) {
        return systemLogService.selectByStatus(status);
    }

    /**
     * 根据时间范围查询系统日志
     */
    @GetMapping("/time-range")
    @ResponseBody
    public List<SystemLog> getByTimeRange(@RequestParam Date startTime, @RequestParam Date endTime) {
        return systemLogService.selectByTimeRange(startTime, endTime);
    }

    /**
     * 删除系统日志
     */
    @PostMapping("/delete/{id}")
    @ResponseBody
    public String delete(@PathVariable Integer id) {
        try {
            systemLogService.deleteById(id);
            return "success";
        } catch (Exception e) {
            return "error";
        }
    }

    /**
     * 批量删除系统日志
     */
    @PostMapping("/batch-delete")
    @ResponseBody
    public String batchDelete(@RequestParam List<Integer> ids) {
        try {
            systemLogService.batchDelete(ids);
            return "success";
        } catch (Exception e) {
            return "error";
        }
    }

    /**
     * 清空指定时间之前的系统日志
     */
    @PostMapping("/clear-before")
    @ResponseBody
    public String clearBeforeTime(@RequestParam Date time) {
        try {
            systemLogService.deleteBeforeTime(time);
            return "success";
        } catch (Exception e) {
            return "error";
        }
    }
}