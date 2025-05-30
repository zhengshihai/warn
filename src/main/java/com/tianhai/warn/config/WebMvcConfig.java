package com.tianhai.warn.config;

import com.tianhai.warn.controller.FileStorageController;
import com.tianhai.warn.interceptor.LoginInterceptor;
import com.tianhai.warn.listeners.AuditEventListener;
import jakarta.servlet.FilterRegistration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.*;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Web MVC配置
 */
@Configuration
@EnableWebMvc
@ComponentScan(basePackages = "com.tianhai.warn")
public class WebMvcConfig implements WebMvcConfigurer {

        // 暂时写死配置值
        private final String basePath = "E:/Warning/Warn/uploads";
        private final String baseUrl = "/uploads";

        @Autowired
        private ApplicationContext applicationContext;

        @Override
        public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(new LoginInterceptor())
                                .addPathPatterns("/**") // 拦截所有请求
                                .excludePathPatterns( // 排除不需要拦截的路径
                                                "/login",
                                                "/logout",
                                                "/css/**",
                                                "/js/**",
                                                "/images/**");
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
                registry.addResourceHandler(baseUrl + "/**")
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
                        List<org.springframework.http.converter.HttpMessageConverter<?>> converters) {
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
        public FileStorageController fileStorageController() {
                FileStorageController controller = new FileStorageController();
                // 手动设置属性值
                controller.setBasePath(basePath);
                controller.setBaseUrl(baseUrl);
                return controller;
        }

        @Bean
        public ApplicationEventPublisher applicationEventPublisher() {
                return applicationContext;
        }

        @Bean
        public AsyncTaskExecutor asyncTaskExecutor() {
                ThreadPoolTaskExecutor asyncTaskExecutor = new ThreadPoolTaskExecutor();
                asyncTaskExecutor.setCorePoolSize(5);
                asyncTaskExecutor.setMaxPoolSize(10);
                asyncTaskExecutor.setQueueCapacity(100);
                asyncTaskExecutor.initialize();

                return asyncTaskExecutor;
        }

        @Bean
        public AuditEventListener auditEventListener() {
                return new AuditEventListener();
        }

}