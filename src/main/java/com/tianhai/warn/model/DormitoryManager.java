package com.tianhai.warn.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 宿管实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DormitoryManager implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Integer id; // 主键ID
    private String managerId; // 工号
    private String name; // 姓名
    private String building; // 负责宿舍楼
    private String phone; // 联系电话
    private String email; // 电子邮箱
    private String status; // 状态：ON_DUTY表示在职/ OFF_DUTY表示离职
    private String password; // 登录密码

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime; // 创建时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime; // 更新时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastLoginTime; // 最后登录时间

    private Integer version; // 版本号

    @Override
    public String toString() {
        return "DormitoryManager{" +
                "id=" + id +
                ", managerId='" + managerId + '\'' +
                ", name='" + name + '\'' +
                ", building='" + building + '\'' +
                ", phone='" + phone + '\'' +
                ", email='" + email + '\'' +
                ", status='" + status + '\'' +
                ", password='" + password + '\'' +
                ", createTime=" + createTime +
                ", updateTime=" + updateTime +
                ", version=" + version +
                '}';
    }
}