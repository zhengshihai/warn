package com.tianhai.warn.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WeekWarnStatVO {

    private String weekday; // 缩写 例如： Mon

    private Integer waningCount; // 预警数量


}
