package com.tianhai.warn.config;

import com.google.code.kaptcha.impl.DefaultKaptcha;
import com.google.code.kaptcha.util.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

@Configuration
public class KaptchaConfig {
    @Bean
    public DefaultKaptcha captchaProducer() {
        DefaultKaptcha defaultKaptcha = new DefaultKaptcha();
        Properties properties = new Properties();
        // 验证码宽度
        properties.setProperty("kaptcha.image.width", "120");
        // 验证码高度
        properties.setProperty("kaptcha.image.height", "45");
        // 生成验证码内容范围
        properties.setProperty("kaptcha.textproducer.char.string", "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ");
        // 验证码长度
        properties.setProperty("kaptcha.textproducer.char.length", "4");
        // 验证码字体颜色
        properties.setProperty("kaptcha.textproducer.font.color", "black");
        // 验证码字体大小
        properties.setProperty("kaptcha.textproducer.font.size", "32");
        // 验证码所属字体样式
        properties.setProperty("kaptcha.textproducer.font.names", "Arial");
        // 干扰线颜色
        properties.setProperty("kaptcha.noise.color", "blue");
        // 验证码文本字符间距
        properties.setProperty("kaptcha.textproducer.char.space", "4");
        // 图片样式
        properties.setProperty("kaptcha.obscurificator.impl", "com.google.code.kaptcha.impl.ShadowGimpy");

        Config config = new Config(properties);
        defaultKaptcha.setConfig(config);
        return defaultKaptcha;
    }
}