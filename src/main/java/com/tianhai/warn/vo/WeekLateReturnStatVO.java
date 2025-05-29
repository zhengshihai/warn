package com.tianhai.warn.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeekLateReturnStatVO {
    private String weekday;// 缩写 例如： Mon

    private Integer lateReturnCount;

}
