package com.tianhai.warn.utils;

import com.tianhai.warn.model.DormitoryManager;
import com.tianhai.warn.model.Student;
import com.tianhai.warn.model.SuperAdmin;
import com.tianhai.warn.model.SysUser;

import java.util.HashMap;
import java.util.Map;

/**
 * 根据角色类型名称将对象转为具体的角色对象
 */
public class RoleObjectCaster {

    private static final Map<String, Class<?>> roleClassMap = new HashMap<>();

    static {
        roleClassMap.put("student", Student.class);

        roleClassMap.put("systemUser", SysUser.class);
        roleClassMap.put("dormitoryManager", DormitoryManager.class);
        roleClassMap.put("superAdmin", SuperAdmin.class);

        roleClassMap.put("systemuser", SysUser.class);
        roleClassMap.put("dormitorymanager", DormitoryManager.class);
        roleClassMap.put("superadmin", SuperAdmin.class); // 历史包袱
    }

    /**
     * 将对象转换为指定角色类型
     *
     * @param roleName 角色名称
     * @param object   要转换的对象
     * @param <T>      转换后的类型
     * @return 转换后的对象
     * @throws InterruptedException 如果角色未注册或对象类型不匹配
     */
    @SuppressWarnings("unchecked")
    public static <T> T cast(String roleName, Object object) {
        Class<?> targetClass = roleClassMap.get(roleName);
        if (targetClass == null) {
            throw new IllegalArgumentException("不存在的角色名称" + roleName);
        }

        // A.isInstance(B) 判断对象B是否是类A的实例 或是A的子类/实现类的实例
        if (!targetClass.isInstance(object)) {
            throw new IllegalArgumentException("对象不是类型 " + targetClass.getName() + " 的实例");
        }

        return (T) targetClass.cast(object);
    }
}
