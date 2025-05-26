package com.tianhai.warn.dto;

import lombok.Builder;
import lombok.Data;

/**
 * 更新晚归处理结果动作DTO
 */
@Data
@Builder
public class ProcessActionDTO {
    /**
     * 学生学号
     */
    private String studentNo;

    /**
     * 处理结果
     */
    private String processResult;

    /**
     * 晚归记录ID
     */
    private String lateReturnId;
}