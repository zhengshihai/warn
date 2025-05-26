package com.tianhai.warn.utils;


import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.Date;

import static org.junit.Assert.*;

public class IdValidatorTest {

    @Test
    public void testValidFormat() {
        // 测试2位前缀的ID
        assertTrue("EX前缀ID格式应该合法", IdValidator.isValidFormat("EX20240315123456"));
        assertTrue("AP前缀ID格式应该合法", IdValidator.isValidFormat("AP20240315123456"));
        assertTrue("NT前缀ID格式应该合法", IdValidator.isValidFormat("NT20240315123456"));
        assertTrue("LR前缀ID格式应该合法", IdValidator.isValidFormat("LR20240315123456"));

        // 测试3位前缀的ID
        assertTrue("LOG前缀ID格式应该合法", IdValidator.isValidFormat("LOG20240315123456"));

        // 测试无效格式
        assertFalse("ID长度不正确应该无效", IdValidator.isValidFormat("EX2024031512345")); // 少一位
        assertFalse("ID长度不正确应该无效", IdValidator.isValidFormat("EX202403151234567")); // 多一位
        assertFalse("非数字部分应该无效", IdValidator.isValidFormat("EX20240315ABCDEF")); // 随机数部分不是数字
        assertFalse("非数字部分应该无效", IdValidator.isValidFormat("EX2024031X123456")); // 日期部分不是数字
    }

    @Test
    public void testValidPrefix() {
        // 测试有效前缀
        assertTrue("EX前缀应该有效", IdValidator.isValidPrefix("EX20240315123456"));
        assertTrue("AP前缀应该有效", IdValidator.isValidPrefix("AP20240315123456"));
        assertTrue("NT前缀应该有效", IdValidator.isValidPrefix("NT20240315123456"));
        assertTrue("LR前缀应该有效", IdValidator.isValidPrefix("LR20240315123456"));
        assertTrue("LOG前缀应该有效", IdValidator.isValidPrefix("LOG20240315123456"));

        // 测试无效前缀
        assertFalse("未知前缀应该无效", IdValidator.isValidPrefix("XX20240315123456"));
        assertFalse("小写前缀应该无效", IdValidator.isValidPrefix("ex20240315123456"));
    }

    @Test
    public void testValidDate() {
        // 测试当前日期
        String today = new SimpleDateFormat("yyyyMMdd").format(new Date());
        assertTrue("当前日期的ID应该有效", IdValidator.isValidDate("EX" + today + "123456"));

        // 测试过去日期
        assertTrue("过去日期的ID应该有效", IdValidator.isValidDate("EX20240301123456"));

        // 测试将来日期
        assertFalse("将来日期的EX前缀ID应该无效", IdValidator.isValidDate("EX2095015123456"));
        assertTrue("将来日期的AP前缀ID应该有效", IdValidator.isValidDate("AP20250315123456"));
        assertTrue("将来日期的NT前缀ID应该有效", IdValidator.isValidDate("NT20250315123456"));

        // 测试无效日期
        assertFalse("无效日期应该无效", IdValidator.isValidDate("EX20241315123456")); // 13月
        assertFalse("无效日期应该无效", IdValidator.isValidDate("EX20240230123456")); // 2月30日
    }

    @Test
    public void testValidType() {
        // 测试EX类型
        assertTrue("EX类型ID应该有效", IdValidator.isValidType("EX20240315123456", "EX"));
        assertFalse("EX类型ID不应该匹配其他前缀", IdValidator.isValidType("EX20240315123456", "AP"));

        // 测试LOG类型
        assertTrue("LOG类型ID应该有效", IdValidator.isValidType("LOG20240315123456", "LOG"));
        assertFalse("LOG类型ID不应该匹配其他前缀", IdValidator.isValidType("LOG20240315123456", "EX"));

        // 测试将来日期
        assertFalse("EX类型将来日期应该无效", IdValidator.isValidType("EX2020315123456", "EX"));
        assertTrue("AP类型将来日期应该有效", IdValidator.isValidType("AP20250315123456", "AP"));
        assertTrue("NT类型将来日期应该有效", IdValidator.isValidType("NT20250315123456", "NT"));
    }

    @Test
    public void testValid() {
        // 测试完全有效的ID
        assertTrue("完全有效的ID应该通过验证", IdValidator.isValid("EX20240315123456"));
        assertTrue("完全有效的ID应该通过验证", IdValidator.isValid("LOG20240315123456"));

        // 测试各种无效情况
        assertFalse("null应该无效", IdValidator.isValid(null));
        assertFalse("空字符串应该无效", IdValidator.isValid(""));
        assertFalse("格式不正确应该无效", IdValidator.isValid("EX2024031512345"));
        assertFalse("前缀不正确应该无效", IdValidator.isValid("XX20240315123456"));
        assertFalse("日期不正确应该无效", IdValidator.isValid("EX20241315123456"));
    }

}