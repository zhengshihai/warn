package com.tianhai.warn.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
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

    @Override
    public String toString() {
        return "SysUserQuery{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", sysUserNo='" + sysUserNo + '\'' +
                ", phone='" + phone + '\'' +
                ", email='" + email + '\'' +
                ", jobRole='" + jobRole + '\'' +
                ", status='" + status + '\'' +
                ", nameLike='" + nameLike + '\'' +
                ", ids=" + ids +
                ", sysUserNos=" + sysUserNos +
                ", emails=" + emails +
                ", jobRoles=" + jobRoles +
                '}';
    }
}
