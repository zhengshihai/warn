package com.tianhai.warn.listeners;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;

/**
 * JDBC驱动清理监听器
 *
 */
public class JdbcDriverCleanupListener implements ServletContextListener {

    private static final Logger logger = LoggerFactory.getLogger(JdbcDriverCleanupListener.class);



    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        // 获取当前线程的类加载器（即WebApp的ClassLoader）
        ClassLoader webAppClassLoader = Thread.currentThread().getContextClassLoader();

        Enumeration<Driver> drivers = DriverManager.getDrivers();
        while (drivers.hasMoreElements()) {
            Driver driver = drivers.nextElement();
            // 如果驱动是由当前Web应用注册的，就注销它
            if (driver.getClass().getClassLoader() == webAppClassLoader) {
                try {
                    DriverManager.deregisterDriver(driver);
                    System.out.println("[INFO] 成功注销JDBC驱动: " + driver);
                } catch (SQLException e) {
                    System.err.println("[ERROR] 注销JDBC驱动失败: " + driver);
                    logger.error("注销JDBC驱动失败: {}", driver, e);
                }
            }
        }
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {

    }


}