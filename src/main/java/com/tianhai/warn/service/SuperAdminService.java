package com.tianhai.warn.service;

import com.tianhai.warn.model.SuperAdmin;
import com.tianhai.warn.query.SuperAdminQuery;
import com.tianhai.warn.utils.PageResult;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 超级管理员接口
 */
public interface SuperAdminService {
    // 查询超级管理员的信息（含密码）
    SuperAdmin getByIdWithPassword(Integer id);

    // 查询超级管理员的的脱敏信息（不含密码）
    SuperAdmin selectByIdWithoutPassword(Integer id);


    List<SuperAdmin> selectByCondition(SuperAdmin superAdmin);

    List<SuperAdmin> selectAll();

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

    /**
     * 批量导入超级管理员信息
     *
     * @param file             超级管理员信息文件
     * @param insertUserRole   角色
     * @return 导入结果
     */
    Map<String, Object> importExcelInfoBatch(MultipartFile file, String insertUserRole);

    /**
     * 批量插入超级管理员信息
     * @param superAdminList    超级管理员列表
     * @return                  插入行数
     */
    int insertBatch(List<SuperAdmin> superAdminList);
}
