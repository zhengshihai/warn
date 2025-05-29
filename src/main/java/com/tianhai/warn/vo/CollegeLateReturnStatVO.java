package com.tianhai.warn.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 晚归学院分布统计VO
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CollegeLateReturnStatVO {
    private String college; // 学院名称

    private Integer count; // 晚归人数

    private Double percentage; // 占比（单位：百分比，例如：23.56 表示 23.56%）
}
