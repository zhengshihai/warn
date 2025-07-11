package com.tianhai.warn.query;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.tianhai.warn.annotation.AtLeastOneFieldNotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.Date;
import java.util.List;

@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@AtLeastOneFieldNotNull
public class DormitoryManagerQuery extends BaseQuery{
    // 精确匹配字段
    private String managerId;       // 工号
    private String status;          // ON_DUTY / OFF_DUTY
    private String email;           // 邮箱

    // 模糊查询字段
    private String nameLike;        // 姓名模糊
    private String buildingLike;    // 宿舍楼模糊
    private String phoneLike;       // 电话模糊
    private String emailLike;       // 邮箱模糊

    // 批量查询
    private List<Integer> ids;
    private List<String> managerIds;

    // 时间范围筛选（创建时间 / 登录时间）
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTimeStart;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTimeEnd;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastLoginTimeStart;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastLoginTimeEnd;
}
