package com.tianhai.warn.config;

import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

// Xxl-job配置 这个功能有bug
@Configuration
public class XxlJobConfig {

    private static final Logger logger = LoggerFactory.getLogger(XxlJobConfig.class);

//    @Value("${xxl.job.admin.addresses}")
//    private String addresses;
//
//    @Value("${xxl.job.executor.appname}")
//    private String appName;
//
//    @Value("${xxl.job.executor.address}")
//    private String address;
//
//    @Value("${xxl.job.executor.ip}")
//    private String ip;
//
//    @Value("${xxl.job.executor.port}")
//    private String port;
//
//    @Value("${xxl.job.accessToken}")
//    private String accessToken;
//
//    @Value("${xxl.job.executor.logpath}")
//    private String logPath;
//
//    @Value("${xxl.job.executor.logretentiondays}")
//    private String logRetentionDays;

    @Bean
    public XxlJobSpringExecutor xxlJobSpringExecutor() {
        logger.info("初始化执行器，Initializing XXL-JOB Spring Executor...");

        XxlJobSpringExecutor executor = new XxlJobSpringExecutor();
//
//        executor.setAdminAddresses(addresses);
//        executor.setAppname(appName);
//        executor.setAddress(address);
//        executor.setIp(ip);
//        executor.setPort(Integer.parseInt(port));
//        executor.setAccessToken(accessToken);
//        executor.setLogPath(logPath);
//        executor.setLogRetentionDays(Integer.parseInt(logRetentionDays));
//
//        logger.info("执行器executor构造成功：{}", executor);
//
//        return executor;

        // 调度中心地址
        executor.setAdminAddresses("http://localhost:9777/xxl-job-admin");

        // 执行器名称
        executor.setAppname("late-return-face-executor");

        // 执行器地址（一般为空，系统自动拼接）
        executor.setAddress(null);

        // 执行器IP
        executor.setIp("127.0.0.1");

        // 执行器端口
        executor.setPort(11223);

        // 通信TOKEN
        executor.setAccessToken("default_token");

        // 日志保存路径
        executor.setLogPath("E:/Warning/Warn/xxl-job/logs");

        // 日志保存天数
        executor.setLogRetentionDays(30);

        return executor;
    }
}
