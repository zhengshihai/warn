package com.tianhai.warn.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;


import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class RedisUtils {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String CONTACT_KEY_PREFIX = "student:contact:";
    private static final long CONTACT_EXPIRE_TIME = 72; // 72小时过期

    /**
     * 存储学生联系人信息
     */
    public void setStudentContacts(String studentNo, List<String> parentPhones, List<String> managerPhones) {
        String key = CONTACT_KEY_PREFIX + studentNo;
        StudentContacts contacts = new StudentContacts(parentPhones, managerPhones);
        redisTemplate.opsForValue().set(key, contacts, CONTACT_EXPIRE_TIME, TimeUnit.HOURS);
    }

    /**
     * 获取学生联系人信息
     */
    public StudentContacts getStudentContacts(String studentNo) {
        String key = CONTACT_KEY_PREFIX + studentNo;
        return (StudentContacts) redisTemplate.opsForValue().get(key);
    }

    /**
     * 删除学生联系人信息
     */
    public void deleteStudentContacts(String studentNo) {
        String key = CONTACT_KEY_PREFIX + studentNo;
        redisTemplate.delete(key);
    }

    /**
     * 联系人信息内部类
     */
    public static class StudentContacts {
        private List<String> parentPhones;
        private List<String> managerPhones;

        public StudentContacts(List<String> parentPhones, List<String> managerPhones) {
            this.parentPhones = parentPhones;
            this.managerPhones = managerPhones;
        }

        public List<String> getParentPhones() {
            return parentPhones;
        }

        public List<String> getManagerPhones() {
            return managerPhones;
        }
    }
}