package com.tianhai.warn.config;

import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

//// Xxl-job配置 这个功能有bug
//@Configuration
//public class XxlJobConfig {
//
//    private static final Logger logger = LoggerFactory.getLogger(XxlJobConfig.class);
//
//    @Autowired
//    private Environment environment;
//
//
//    @Bean(initMethod = "start", destroyMethod = "destroy")
//    public XxlJobSpringExecutor xxlJobSpringExecutor() {
//        logger.info("初始化执行器，Initializing XXL-JOB Spring Executor...");
//
//        XxlJobSpringExecutor executor = new XxlJobSpringExecutor();
//
////        String addresses = environment.getProperty("xxl.job.admin.addresses");
////        String appName = environment.getProperty("xxl.job.executor.appname");
////        String address = environment.getProperty("xxl.job.executor.address");
////        String ip = environment.getProperty("xxl.job.executor.ip");
////        Integer port = environment.getProperty("xxl.job.executor.port", Integer.class, 9888);
////        String accessToken = environment.getProperty("xxl.job.executor.accessToken");
////        String logPath = environment.getProperty("xxl.job.executor.logpath");
////        Integer logRetentionDays = environment.getProperty("xxl.job.executor.logretentiondays", Integer.class, 30);
////
////        executor.setAdminAddresses(addresses);
////        executor.setAppname(appName);
////        executor.setAddress(address);
////        executor.setIp(ip);
////        executor.setPort(port);
////        executor.setAccessToken(accessToken);
////        executor.setLogPath(logPath);
////        executor.setLogRetentionDays(logRetentionDays);
////
////        return executor;
//
//        // 调度中心地址
//        executor.setAdminAddresses("http://localhost:9777/xxl-job-admin");
//
//        // 执行器名称
//        executor.setAppname("late-return-face-executor");
//
//        // 执行器地址（一般为空，系统自动拼接）
//        executor.setAddress(null);
//
//        // 执行器IP
//        executor.setIp("127.0.0.1");
//
//        // 执行器端口
//        executor.setPort(9888);
//
//        // 通信TOKEN
//        executor.setAccessToken("your-access-token");
//
//        // 日志保存路径
//        executor.setLogPath("E:/Warning/Warn/logs/xxl-job/job-handler");
//
//        // 日志保存天数
//        executor.setLogRetentionDays(30);
//
//        return executor;
//    }
//}
