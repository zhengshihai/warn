package com.tianhai.warn.mapper;

import com.tianhai.warn.model.SuperAdmin;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;


public interface SuperAdminMapper {
    SuperAdmin selectById(@Param("id") Integer id);

    List<SuperAdmin> selectAll();

    int insert(SuperAdmin admin);

    int update(SuperAdmin admin);

    int deleteById(@Param("id") Integer id);

    List<SuperAdmin> selectByCondition(SuperAdmin query);

    int updateWithVersion(SuperAdmin superAdmin);

    int countAll();

    int insertBatch(@Param("superAdminList") List<SuperAdmin> superAdminList);
}
