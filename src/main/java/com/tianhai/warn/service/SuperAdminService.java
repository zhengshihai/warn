package com.tianhai.warn.service;

import com.tianhai.warn.model.SuperAdmin;

import java.util.List;

/**
 * 超级管理员接口
 */
public interface SuperAdminService {

    SuperAdmin getById(Integer id);

    List<SuperAdmin> selectByCondition(SuperAdmin superAdmin);

    List<SuperAdmin> listAll();

    Integer insert(SuperAdmin admin);

    Integer update(SuperAdmin admin);

    Integer deleteById(Integer id);

    SuperAdmin getByEmail(String email);

    void updateLastLoginTime(Integer id);

    SuperAdmin selectById(Integer id);
}
