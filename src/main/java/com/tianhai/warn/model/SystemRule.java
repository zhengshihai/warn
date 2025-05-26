package com.tianhai.warn.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 系统规则实体类
 */
@Data
public class SystemRule implements Serializable {
    private static final long serialVersionUID = 1L;

    private Integer id; // 主键ID
    private String ruleKey; // 规则键
    private String ruleValue; // 规则值
    private String description; // 规则描述
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime; // 创建时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime; // 更新时间


    @Override
    public String toString() {
        return "SystemRule{" +
                "id=" + id +
                ", ruleKey='" + ruleKey + '\'' +
                ", ruleValue='" + ruleValue + '\'' +
                ", description='" + description + '\'' +
                ", createTime=" + createTime +
                ", updateTime=" + updateTime +
                '}';
    }
}