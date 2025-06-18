package com.tianhai.warn.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

// 报警配置实体类
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlarmConfig {
    /**
     * 主键ID
     */
    private Long id;

    /**
     * API提供商标识
     */
    private String apiProvider;

    /**
     * API密钥
     */
    private String apiKey;

    /**
     * API密钥
     */
    private String apiSecret;

    /**
     * 配置描述
     */
    private String description;

    /**
     * 是否启用：0-禁用，1-启用
     */
    private Integer isActive;

    /**
     * 版本号
     */
    private Integer version;

    /**
     * 最后修改人
     */
    private String lastModifiedBy;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createdAt;

    /**
     * 更新时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updatedAt;
}