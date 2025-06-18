package com.tianhai.warn.service.impl;

import com.tianhai.warn.constants.AlarmConstants;
import com.tianhai.warn.enums.ResultCode;
import com.tianhai.warn.exception.BusinessException;
import com.tianhai.warn.exception.SystemException;
import com.tianhai.warn.mapper.AlarmConfigMapper;
import com.tianhai.warn.model.AlarmConfig;
import com.tianhai.warn.service.AlarmConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Objects;

@Service
public class AlarmConfigServiceImpl implements AlarmConfigService {
    private static final Logger logger = LoggerFactory.getLogger(AlarmConfigServiceImpl.class);

    @Autowired
    private AlarmConfigMapper alarmConfigMapper;

    @Override
    public AlarmConfig selectById(Long id) {
        return alarmConfigMapper.selectById(id);
    }

    @Override
    public AlarmConfig selectByApiProvider(String apiProvider) {
        return alarmConfigMapper.selectByApiProvider(apiProvider);
    }

    @Override
    public List<AlarmConfig> selectAll() {
        return alarmConfigMapper.selectAll();
    }

    @Override
    public List<AlarmConfig> selectActive() {
        return alarmConfigMapper.selectActive();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer insert(AlarmConfig config) {
        try {
            AlarmConfig existingConfig = alarmConfigMapper.selectByApiProvider(config.getApiProvider());
            if (existingConfig != null) {
                logger.error("API提供商标识已存在: {}", config.getApiProvider());
                throw new BusinessException(ResultCode.ALARM_CONFIG_KEY_EXISTS);
            }
            if (config.getIsActive() == null) {
                config.setIsActive(AlarmConstants.ALARM_CONFIG_ACTIVE);
            }
            Date now = new Date();
            config.setCreatedAt(now);
            config.setUpdatedAt(now);
            config.setVersion(1);
            config.setLastModifiedBy(getCurrentUser());
            int result = alarmConfigMapper.insert(config);
            if (result <= 0) {
                logger.error("保存配置失败: {}", config);
            } else {
                logger.info("配置保存成功: {}", config);
            }
            return result;
        } catch (Exception e) {
            logger.error("保存配置时发生异常", e);
            throw new SystemException(ResultCode.ERROR);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer update(AlarmConfig config) {
        try {
            // 检查配置是否存在
            AlarmConfig existingConfig = alarmConfigMapper.selectById(config.getId());
            if (existingConfig == null) {
                logger.error("配置不存在, id: {}", config.getId());
                throw new BusinessException(ResultCode.ALARM_CONFIG_NOT_FOUNT);
            }

            // 检查版本号
            if (!existingConfig.getVersion().equals(config.getVersion())) {
                logger.error("配置版本不匹配, id: {}, 当前版本: {}, 传入版本: {}",
                        config.getId(), existingConfig.getVersion(), config.getVersion());
                throw new BusinessException(ResultCode.ALARM_CONFIG_VERSION_MISMATCH);
            }

            // 如果修改了API提供商标识，需要检查新标识是否已存在
            if (!existingConfig.getApiProvider().equals(config.getApiProvider())) {
                AlarmConfig providerExists = alarmConfigMapper.selectByApiProvider(config.getApiProvider());
                if (providerExists != null) {
                    logger.error("API提供商标识已存在: {}", config.getApiProvider());
                    throw new BusinessException(ResultCode.ALARM_CONFIG_KEY_EXISTS);
                }
            }
            config.setUpdatedAt(new Date());
            config.setLastModifiedBy(getCurrentUser());
            int result = alarmConfigMapper.updateByPrimaryKey(config);
            if (result <= 0) {
                logger.error("更新配置失败: {}", config);
            } else {
                logger.info("配置更新成功: {}", config);
            }
            return result;
        } catch (Exception e) {
            logger.error("更新配置时发生异常", e);
            throw new SystemException(ResultCode.ERROR);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer delete(Long id) {
        try {
            AlarmConfig config = alarmConfigMapper.selectById(id);
            if (config == null) {
                logger.error("配置不存在, id: {}", id);
                throw new BusinessException(ResultCode.ALARM_CONFIG_NOT_FOUNT);
            }
            int result = alarmConfigMapper.deleteByPrimaryKey(id);
            if (result <= 0) {
                logger.error("删除配置失败, id: {}", id);
            } else {
                logger.info("配置删除成功, id: {}", id);
            }
            return result;
        } catch (Exception e) {
            logger.error("删除配置时发生异常", e);
            throw new SystemException(ResultCode.ERROR);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer enable(String apiProvider) {
        try {
            AlarmConfig config = new AlarmConfig();
            config.setApiProvider(apiProvider);
            config.setIsActive(AlarmConstants.ALARM_CONFIG_ACTIVE);
            config.setUpdatedAt(new Date());
            config.setLastModifiedBy(getCurrentUser());
            int result = alarmConfigMapper.updateByPrimaryKey(config);
            if (result <= 0) {
                logger.error("启用配置失败, apiProvider: {}", apiProvider);
            } else {
                logger.info("配置启用成功, apiProvider: {}", apiProvider);
            }
            return result;
        } catch (Exception e) {
            logger.error("启用配置时发生异常, apiProvider: {}", apiProvider, e);
            throw new SystemException(ResultCode.ERROR);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer disable(String apiProvider) {
        try {
            AlarmConfig config = new AlarmConfig();
            config.setApiProvider(apiProvider);
            config.setIsActive(AlarmConstants.ALARM_CONFIG_INACTIVE);
            config.setUpdatedAt(new Date());
            config.setLastModifiedBy(getCurrentUser());
            int result = alarmConfigMapper.updateByPrimaryKey(config);
            if (result <= 0) {
                logger.error("禁用配置失败, apiProvider: {}", apiProvider);
            } else {
                logger.info("配置禁用成功, apiProvider: {}", apiProvider);
            }
            return result;
        } catch (Exception e) {
            logger.error("禁用配置时发生异常, apiProvider: {}", apiProvider, e);
            throw new SystemException(ResultCode.ERROR);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer updateStatus(Long id, Integer status) {
        try {
            AlarmConfig config = alarmConfigMapper.selectById(id);
            if (config == null) {
                logger.error("配置不存在, id: {}", id);
                throw new BusinessException(ResultCode.ALARM_CONFIG_NOT_FOUNT);
            }
            if (!Objects.equals(status, AlarmConstants.ALARM_CONFIG_ACTIVE)
                    && !Objects.equals(status, AlarmConstants.ALARM_CONFIG_INACTIVE)) {
                logger.error("无效的状态值: {}", status);
                throw new BusinessException(ResultCode.ALARM_CONFIG_STATUS_INVALID);
            }
            if (config.getIsActive().equals(status)) {
                return 1;
            }
            config.setIsActive(status);
            config.setUpdatedAt(new Date());
            config.setLastModifiedBy(getCurrentUser());
            int result = alarmConfigMapper.updateByPrimaryKey(config);
            if (result <= 0) {
                logger.error("更新配置状态失败, id: {}, status: {}", id, status);
            } else {
                logger.info("配置状态更新成功, id: {}, status: {}", id, status);
            }
            return result;
        } catch (Exception e) {
            logger.error("更新配置状态时发生异常", e);
            throw new SystemException(ResultCode.ERROR);
        }
    }

    /**
     * 获取当前用户
     */
    private String getCurrentUser() {
        // TODO: 实现获取当前用户的逻辑
        return "system";
    }
}
