package com.tianhai.warn.query;

import com.tianhai.warn.annotation.AtLeastOneFieldNotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@AtLeastOneFieldNotNull
public class SysUserQuery extends BaseQuery {

    private Integer id;
    private String name;
    private String sysUserNo; // 工号
    private String phone; // 联系电话
    private String email; // 邮箱（用作登录标识）
    private String jobRole; // 职位角色 (辅导员，班主任，院级领导等其他）
    private String status; // 状态：启用/禁用

    private String nameLike; // 姓名模糊查询

    private List<String> ids; // id批量查询
    private List<String> sysUserNos; // 工号批量查询
    private List<String> emails; // 邮箱批量查询
    private List<String> jobRoles; // 多种职位角色查询

}
