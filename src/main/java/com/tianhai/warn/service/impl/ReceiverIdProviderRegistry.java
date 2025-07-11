package com.tianhai.warn.service.impl;

import com.tianhai.warn.annotation.ReceiverRole;
import com.tianhai.warn.constants.Constants;
import com.tianhai.warn.service.ReceiverIdProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 根据用户角色（学生 宿管 班级管理员）获取业务标识Id注册类
@Component
public class ReceiverIdProviderRegistry {

    private final Map<String, ReceiverIdProvider> strategyMap = new HashMap<>();

    // 构造函数注册策略（ReceiverIdProvider会被Spring自动扫描）
    @Autowired
    public ReceiverIdProviderRegistry(List<ReceiverIdProvider> providers) {
        // Spring 根据@Component 注解手机所有策略实现类
        for (ReceiverIdProvider provider : providers) {
            String roleCode = resolveRoleFromClass(provider.getClass());
            strategyMap.put(roleCode, provider);
        }
    }

    // 根据用户角色获取该角色的所有用户的业务标识id 例如学生角色获取其studentNo
    public List<String> getReceiverIdsByRole(String roleCode) {
        ReceiverIdProvider provider = strategyMap.get(roleCode);
        if (provider == null) {
            throw new IllegalArgumentException("暂时不支持该用户角色: " + roleCode);
        }

        return provider.getReceiverIds();
    }

    // 获取所有角色（超级管理员除外）的所有用户的业务标识id列表
    public List<String> getAllRoleReceiverIdList() {
        return strategyMap.values().stream()
                .flatMap(provider -> provider.getReceiverIds().stream())
                .distinct()
                .toList();
    }

    // 获取所有角色（超级管理员除外）的所有用户的业务标识id列表，并按角色分类，然后组成Map
    public Map<String, List<String>> getAllRoleReceiverIdMap() {
        Map<String, List<String>> resultMap = new HashMap<>();

        List<String> studentNoList = getReceiverIdsByRole(Constants.STUDENT);
        List<String> dormitoryManagerIdList = getReceiverIdsByRole(Constants.DORMITORY_MANAGER);
        List<String> sysUserNoList = getReceiverIdsByRole(Constants.SYSTEM_USER);

        resultMap.put(Constants.STUDENT, studentNoList);
        resultMap.put(Constants.DORMITORY_MANAGER, dormitoryManagerIdList);
        resultMap.put(Constants.SYSTEM_USER, sysUserNoList);

        return resultMap;
    }

    private String resolveRoleFromClass(Class<?> clazz) {
        ReceiverRole annotation = clazz.getAnnotation(ReceiverRole.class);
        if (annotation == null) {
            throw new IllegalArgumentException("类 " + clazz.getName() + " 没有 @ReceiverRole 注解");
        }

        return annotation.value();
    }

}
