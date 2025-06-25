package com.tianhai.warn.query;

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
public class SuperAdminQuery extends BaseQuery{
    /**
     * 支持根据多个 ID 批量查询
     */
    private List<Integer> idList;

    /**
     * 姓名模糊查询
     */
    private String nameLike;

    /**
     * 邮箱模糊查询
     */
    private String emailLike;

    /**
     * 启用状态（0=禁用，1=启用）
     */
    private Integer enabled;

    /**
     * 创建时间范围起始
     */
    private Date createTimeStart;

    /**
     * 创建时间范围结束
     */
    private Date createTimeEnd;

    /**
     * 最后登录时间范围起始
     */
    private Date lastLoginTimeStart;

    /**
     * 最后登录时间范围结束
     */
    private Date lastLoginTimeEnd;

}
