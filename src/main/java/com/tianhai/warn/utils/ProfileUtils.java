package com.tianhai.warn.utils;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * 环境获取工具类
 */
@Component
public class ProfileUtils {
    private static Environment environment;

    @Autowired
    private void setEnvironment(Environment env) {
        ProfileUtils.environment = env;
    }

    /**
     * 获取所有激活的 Spring Profiles
     * @return 不为 null 的 String 数组，若无激活环境返回空数组
     */
    public static String[] getActiveProfiles() {
        if (environment == null) {
            return new String[0];
        }

        return environment.getActiveProfiles();
    }

    /**
     * 获取第一个激活的 Profile（一般是 dev、test、prod 之一）
     * 注意：设置JAM启动参数时，dev/test/prod这些必须设置在首位
     * @return 激活的第一个 profile，若未设置返回 null
     */
    public static String getFirstActiveProfile() {
        String[] profiles = getActiveProfiles();

        return profiles.length > 0 ? profiles[0] : null;
    }


    /**
     * 判断指定 profile 是否被激活
     * @param profile 要判断的 profile 名称
     * @return true 如果激活了该 profile，false 否则
     */
    public static boolean isProfileActive(String profile) {
        if (StringUtils.isBlank(profile)) {
            return false;
        }

        String[] profiles = getActiveProfiles();
        for (String activeProfile : profiles) {
            if (profile.equalsIgnoreCase(activeProfile)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 判断是否存在任何激活的 profile
     * @return true 有激活的 profile，false 无激活
     */
    public static boolean hasActiveProfiles() {
        return getActiveProfiles().length > 0;
    }
}
