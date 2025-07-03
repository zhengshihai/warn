package com.tianhai.warn.config;

import com.tianhai.warn.interceptor.LoginInterceptor;
import com.tianhai.warn.listeners.AuditEventListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.*;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.springframework.http.converter.HttpMessageConverter;


import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Web MVC配置

 * 为了避免配置冲突，这里不使用@EnableWebMvc注解，而是通过XML配置来启用Spring MVC功能
 */
@Configuration
// @EnableWebMvc // 注释掉，避免与XML配置冲突
@ComponentScan(basePackages = "com.tianhai.warn")
@EnableAsync
public class WebMvcConfig implements WebMvcConfigurer {

        @Autowired
        private LoginInterceptor loginInterceptor;

        @Value("${file.upload.base-path}")
        private String basePath;

        @Value("${file.upload.base-url}")
        private String baseUrl;

        @Autowired
        private ApplicationContext applicationContext;

        @Override
        public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(loginInterceptor)
                                .addPathPatterns("/**") // 拦截所有请求
                                .excludePathPatterns( // 排除不需要拦截的路径
                                                "/login",
                                                "/logout",
                                                "/css/**",
                                                "/js/**",
                                                "/images/**", // todo
                                                "/static/test-websocket.html", // 排除WebSocket测试页面
                                                "/ws/**"); // 排除WebSocket连接
        }

        @Override
        public void addResourceHandlers(ResourceHandlerRegistry registry) {
                // 配置 webapp/static 目录下的静态资源
                registry.addResourceHandler("/static/**")
                                .addResourceLocations("/static/");

                // 配置 webapp/static/js 目录下的 JavaScript 文件
                registry.addResourceHandler("/js/**")
                                .addResourceLocations("/static/js/");

                // 配置 webapp/static/css 目录下的 CSS 文件
                registry.addResourceHandler("/css/**")
                                .addResourceLocations("/static/css/");

                // 配置文件上传目录的访问
                // 使用Spring 6兼容的路径模式，避免PathPatternParser解析错误
                registry.addResourceHandler("/uploads/**")
                                .addResourceLocations("file:" + basePath + "/");
        }

        @Override
        public void configureViewResolvers(ViewResolverRegistry registry) {
                InternalResourceViewResolver resolver = new InternalResourceViewResolver();
                resolver.setPrefix("/WEB-INF/views/");
                resolver.setSuffix(".jsp");
                registry.viewResolver(resolver);
        }

        @Override
        public void configureMessageConverters(
                        List<HttpMessageConverter<?>> converters) {
                // 配置StringHttpMessageConverter
                StringHttpMessageConverter stringConverter = new StringHttpMessageConverter(StandardCharsets.UTF_8);
                stringConverter.setSupportedMediaTypes(List.of(
                                org.springframework.http.MediaType.TEXT_HTML,
                                org.springframework.http.MediaType.APPLICATION_JSON));
                converters.add(stringConverter);

                // 配置MappingJackson2HttpMessageConverter
                MappingJackson2HttpMessageConverter jsonConverter = new MappingJackson2HttpMessageConverter();
                jsonConverter.setSupportedMediaTypes(List.of(
                                org.springframework.http.MediaType.APPLICATION_JSON));
                converters.add(jsonConverter);
        }


        @Bean
        public ApplicationEventPublisher applicationEventPublisher() {
                return applicationContext;
        }

        @Bean("asyncTaskExecutor")
        public AsyncTaskExecutor asyncTaskExecutor() {
                ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
                // 核心线程数：线程池创建时初始化的线程数
                executor.setCorePoolSize(5);
                // 最大线程数：线程池最大的线程数，只有在缓冲队列满了之后才会申请超过核心线程数的线程
                executor.setMaxPoolSize(10);
                // 缓冲队列：用来缓冲执行任务的队列
                executor.setQueueCapacity(100);
                // 允许线程的空闲时间：超过核心线程数的线程在空闲时间到达之后会被销毁
                executor.setKeepAliveSeconds(60);
                // 线程池名的前缀
                executor.setThreadNamePrefix("AsyncTask-");
                // 缓冲队列满了之后的拒绝策略
                executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
                // 等待所有任务结束后再关闭线程池
                executor.setWaitForTasksToCompleteOnShutdown(true);
                executor.setAwaitTerminationSeconds(60);
                // 初始化
                executor.initialize();
                return executor;
        }

        @Bean
        public AuditEventListener auditEventListener() {
                return new AuditEventListener();
        }

}