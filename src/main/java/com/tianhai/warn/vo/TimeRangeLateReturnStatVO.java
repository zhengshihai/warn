package com.tianhai.warn.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeRangeLateReturnStatVO {
    private String hourRange; // 小时范围，例如 "00:00-01:00"

    private Integer lateReturnCount; // 晚归次数
}
