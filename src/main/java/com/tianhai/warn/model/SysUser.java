package com.tianhai.warn.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * 班级管理员实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SysUser implements Serializable {
    private static final long serialVersionUID = 1L;

    // 角色常量定义
    public static final String ROLE_COUNSELOR = "COUNSELOR";      // 辅导员
    public static final String ROLE_TEACHER = "CLASS_TEACHER";          // 班主任
    public static final String ROLE_DORMITORY_MANAGER = "DORMITORY_MANAGER";  // 宿管
    public static final String ROLE_DEAN = "DEAN";                // 院级领导
    public static final String ROLE_OTHER = "OTHER";

    private Integer id; // 主键ID
    private String password; // 密码
    private String name; // 姓名
    private String sysUserNo; // 工号
    private String phone; // 联系电话
    private String email; // 邮箱（用作登录标识）
    private String jobRole; // 职位角色 (辅导员，班主任，院级领导等其他）
    private String status; // 状态：启用/禁用
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastLoginTime; // 最后登录时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime; // 创建时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime; // 更新时间

    @Override
    public String toString() {
        return "SysUser{" +
                "id=" + id +
                ", password='" + password + '\'' +
                ", name='" + name + '\'' +
                ", phone='" + phone + '\'' +
                ", email='" + email + '\'' +
                ", role='" + jobRole + '\'' +
                ", status='" + status + '\'' +
                ", createTime=" + createTime +
                ", updateTime=" + updateTime +
                '}';
    }
}