package com.tianhai.warn.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

// 报警处理方配置实体类
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlarmHandlerConfig {
    /**
     * 主键ID
     */
    private Long id;

    /**
     * 处理方类型：0-系统运营，1-学校安保，2-医疗，3-警方
     */
    private Integer handlerType;

    /**
     * 处理方名称
     */
    private String handlerName;

    /**
     * 接口地址
     */
    private String apiUrl;

    /**
     * 接口密钥
     */
    private String apiKey;

    /**
     * 超时时间(毫秒)
     */
    private Integer timeout;

    /**
     * 优先级
     */
    private Integer priority;

    /**
     * 是否启用：0-禁用，1-启用
     */
    private Integer isActive;

    /**
     * 创建时间
     */
    private Date createdAt;

    /**
     * 更新时间
     */
    private Date updatedAt;
}