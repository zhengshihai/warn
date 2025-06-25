package com.tianhai.warn.service;

import com.tianhai.warn.model.SuperAdmin;
import com.tianhai.warn.query.SuperAdminQuery;
import com.tianhai.warn.utils.PageResult;

import java.util.List;

/**
 * 超级管理员接口
 */
public interface SuperAdminService {
    // 查询超级管理员的信息（含密码）
    SuperAdmin getByIdWithPassword(Integer id);

    // 查询超级管理员的的脱敏信息（不含密码）
    SuperAdmin selectByIdWithoutPassword(Integer id);


    List<SuperAdmin> selectByCondition(SuperAdmin superAdmin);

    List<SuperAdmin> listAll();

    Integer insert(SuperAdmin admin);

    Integer update(SuperAdmin admin);

    Integer deleteById(Integer id);

    SuperAdmin getByEmail(String email);

    void updateLastLoginTime(Integer id);

    /**
     * 分页查询超级管理员信息
     * @param query
     * @return
     */
    PageResult<SuperAdmin> selectByPageQuery(SuperAdminQuery query);
}
