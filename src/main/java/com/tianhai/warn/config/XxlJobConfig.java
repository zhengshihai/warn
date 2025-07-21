package com.tianhai.warn.config;

import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class XxlJobConfig {

    private static final Logger logger = LoggerFactory.getLogger(XxlJobConfig.class);

    @Autowired
    private Environment environment;


    @Bean
    public XxlJobSpringExecutor xxlJobSpringExecutor() {
        logger.info("初始化执行器，Initializing XXL-JOB Spring Executor...");

        XxlJobSpringExecutor executor = new XxlJobSpringExecutor();

        String addresses = environment.getProperty("xxl.job.admin.addresses");
        String appName = environment.getProperty("xxl.job.executor.appname");
        String address = environment.getProperty("xxl.job.executor.address");
        String ip = environment.getProperty("xxl.job.executor.ip");
        Integer port = environment.getProperty("xxl.job.executor.port", Integer.class, 9888);
        String accessToken = environment.getProperty("xxl.job.executor.accessToken");
        String logPath = environment.getProperty("xxl.job.executor.logpath");
        Integer logRetentionDays = environment.getProperty("xxl.job.executor.logretentiondays", Integer.class, 30);

        executor.setAdminAddresses(addresses);
        executor.setAppname(appName);
        executor.setAddress(address);
        executor.setIp(ip);
        executor.setPort(port);
        executor.setAccessToken(accessToken);
        executor.setLogPath(logPath);
        executor.setLogRetentionDays(logRetentionDays);

        return executor;
    }
}
