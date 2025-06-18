package com.tianhai.warn.service;

import com.tianhai.warn.model.AlarmConfig;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface AlarmConfigService {
    /**
     * 根据ID查询配置
     */
    AlarmConfig selectById(Long id);

    /**
     * 根据API提供商标识查询配置
     */
    AlarmConfig selectByApiProvider(String apiProvider);

    /**
     * 查询所有配置
     */
    List<AlarmConfig> selectAll();

    /**
     * 查询所有启用的配置
     */
    List<AlarmConfig> selectActive();

    /**
     * 新增配置
     * 
     * @return 是否成功
     */
    Integer insert(AlarmConfig config);

    /**
     * 更新配置
     * 
     * @return 是否成功
     */
    Integer update(AlarmConfig config);

    /**
     * 删除配置
     * 
     * @return 是否成功
     */
    Integer delete(Long id);

    @Transactional(rollbackFor = Exception.class)
    Integer enable(String apiProvider);

    @Transactional(rollbackFor = Exception.class)
    Integer disable(String apiProvider);

    /**
     * 更新配置状态
     * 
     * @param id     配置ID
     * @param status 状态值（0-禁用，1-启用）
     * @return 是否成功
     */
    Integer updateStatus(Long id, Integer status);
}
