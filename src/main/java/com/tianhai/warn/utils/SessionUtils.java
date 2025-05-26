package com.tianhai.warn.utils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * HttpSession 工具类
 * 使用该工具类需要确保几点
 1-保证 RequestContextHolder 可用（也就是请求必须来自 HTTP 请求线程）；
 2-确保 session 中放的对象是序列化的；
 3-避免在非请求线程（比如异步任务、MQ listener）中调用该工具类，否则会拿不到 RequestContextHolder。
 */
public class SessionUtils {

    /**
     * 获取当前HttpServletRequest
     * @return      HttpServletRequest
     */
    public static HttpServletRequest getRequest() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes instanceof ServletRequestAttributes servletRequestAttributes) {
            return servletRequestAttributes.getRequest();
        }

        return null;
    }

    /**
     * 获取HttpSession
     * @param create 若为 true，则没有 session 时会自动创建
     * @return HttpSession 或 null
     */
    public static HttpSession getSession(boolean create) {
        HttpServletRequest request = getRequest();
        return (request != null) ? request.getSession(create) : null;
    }

    /**
     * 快捷获取当前 session（如果存在）
     */
    public static HttpSession getSession() {
        return getSession(false);
    }

    /**
     * 从 session 获取属性
     */
    public static Object getAttribute(String name) {
        HttpSession session = getSession(false);
        return (session != null) ? session.getAttribute(name) : null;
    }

    /**
     * 向 session 设置属性
     */
    public static void setAttribute(String name, Object value) {
        HttpSession session = getSession(true);
        if (session != null) {
            session.setAttribute(name, value);
        }
    }

    /**
     * 移除 session 中的某个属性
     */
    public static void removeAttribute(String name) {
        HttpSession session = getSession(false);
        if (session != null) {
            session.removeAttribute(name);
        }
    }

    /**
     * 使当前 session 失效
     */
    public static void invalidate() {
        HttpSession session = getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }
}
