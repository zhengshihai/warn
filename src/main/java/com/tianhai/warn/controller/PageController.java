package com.tianhai.warn.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 页面跳转控制器 - 仅用于查看静态页面
 */
@Controller
public class PageController {
     // zsh774538399@gmail.com ZSH774538399@gmail.com

    @GetMapping({ "/", "/index" })
    public String index() {
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/deal-late-record")
    public String warningDetail() {
        return "deal-late-record";
    }

    @GetMapping("/staff-dashboard")
    public String staffDashboard() {
        return "staff-dashboard";
    }

    @GetMapping("/sysuser-panel")
    public String sysuserPanel() {
        return "sysuser-panel";
    }


    @GetMapping("/messages")
    public String messages() {
        return "messages";
    }



    @GetMapping("/dashboard")
    public String dashboard() {
        return "dashboard";
    }

    @GetMapping("/late-records")
    public String lateRecords() {
        return "late-records";
    }

    @GetMapping("/rule")
    public String rule() {
        return "rule";
    }

    @GetMapping("/super-admin")
    public String superAdmin() {
        return "super-admin";
    }

    @GetMapping("/system")
    public String system() {
        return "system";
    }

    @GetMapping("/dean")
    public String deanPage() {
        return "dean";
    }
}