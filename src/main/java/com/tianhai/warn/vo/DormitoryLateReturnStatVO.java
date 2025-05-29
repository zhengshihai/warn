package com.tianhai.warn.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DormitoryLateReturnStatVO {

    private String dormitory; // 宿舍门牌号 "A-101"

    private String dormitoryBuilding; // 宿舍楼名称 "A栋"

    private Integer totalCountByDormitory; // 按宿舍编号统计的总晚归次数

    private Integer totalCountByBuilding; // 按照宿舍楼统计的总晚归次数
}
