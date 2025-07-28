package com.tianhai.warn.component;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.lang.reflect.Method;
import java.util.function.Function;

/**
 * MapperInvoker 是一个 Spring 组件，用于根据索引名（通常是表名）动态调用对应 Mapper 的 countAll 方法。
 *
 * 使用场景：
 * - 传入一个字符串形式的索引名（如 "user"）
 * - 通过 Spring 容器获取对应的 Mapper Bean（如 "userMapper"）
 * - 通过反射调用该 Mapper 的 countAll() 方法
 * - 返回记录数（int）
 *
 * 要求：
 * - 相关 Mapper 命名遵循 "{indexName}Mapper" 的形式
 * - Mapper 中必须存在 public 的 countAll() 方法
 */
public class MapperInvoker implements ApplicationContextAware {
    // 静态引用 Spring 应用上下文，供静态方法使用
    private static ApplicationContext context;

    /**
     * 实现 ApplicationContextAware 接口，Spring 容器启动时会注入当前上下文。
     *
     * @param applicationContext 当前 Spring 容器上下文
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext)  {
        MapperInvoker.context = applicationContext;
    }

    /**
     * 核心函数：接收一个索引名 indexName，动态获取对应 Mapper 并调用其 countAll() 方法。
     *
     * 示例：
     *   int count = MapperInvoker.countAllFunction.apply("user");
     *   // 等价于 userMapper.countAll()
     *
     * @implNote 使用反射调用方法，要求 countAll 方法无参数，返回值为 int
     */
    public static final Function<String, Integer> countAllFunction = (indexName) -> {
        // 拼接出 Bean 名，如 "userMapper"
        String beanName = indexName + "Mapper";

        // 从 Spring 容器中获取 Mapper Bean
        Object mapper = context.getBean(beanName);

        try {
            // 查找名为 countAll 的无参方法
            Method countMethod = mapper.getClass().getMethod("countAll");

            // 调用方法，获取结果
            Object result = countMethod.invoke(mapper);

            // 返回结果，强转为 Integer
            return (Integer) result;

        } catch (Exception e) {
            throw new RuntimeException("无法调用 " + beanName + " 的 countAll 方法", e);
        }
    };
}
