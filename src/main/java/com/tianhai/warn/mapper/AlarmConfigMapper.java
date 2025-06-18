package com.tianhai.warn.mapper;

import com.tianhai.warn.model.AlarmConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AlarmConfigMapper {
    /**
     * 根据ID查询配置
     */
    AlarmConfig selectById(@Param("id") Long id);


    /**
     * 查询所有配置
     */
    List<AlarmConfig> selectAll();

    /**
     * 查询所有启用的配置
     */
    List<AlarmConfig> selectActive();



    /**
     * 更新配置
     * 
     * @return 更新的记录数，失败返回null
     */
    Integer update(AlarmConfig config);

    /**
     * 根据ID删除配置
     * 
     * @return 删除的记录数，失败返回null
     */
    Integer deleteById(@Param("id") Long id);

    /**
     * 更新配置状态
     * 
     * @return 更新的记录数，失败返回null
     */
    Integer updateStatus(AlarmConfig config);

    /**
     * 根据API提供商标识查询配置
     *
     * @param apiProvider API提供商标识
     * @return 配置信息
     */
    AlarmConfig selectByApiProvider(@Param("apiProvider") String apiProvider);

    /**
     * 根据主键查询
     *
     * @param id 主键ID
     * @return 配置信息
     */
    AlarmConfig selectByPrimaryKey(@Param("id") Long id);

    /**
     * 插入配置
     *
     * @param record 配置信息
     * @return 影响行数
     */
    int insert(AlarmConfig record);

    /**
     * 更新配置
     *
     * @param record 配置信息
     * @return 影响行数
     */
    int updateByPrimaryKey(AlarmConfig record);

    /**
     * 删除配置
     *
     * @param id 主键ID
     * @return 影响行数
     */
    int deleteByPrimaryKey(@Param("id") Long id);
}