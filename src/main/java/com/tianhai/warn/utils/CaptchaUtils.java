package com.tianhai.warn.utils;

import java.security.SecureRandom;
import java.util.Random;

/**
 * 验证码生成工具类
 */
public class CaptchaUtils {
    private static final String NUMBERS = "0123456789";
    private static final String LETTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String NUMBERS_AND_LETTERS = NUMBERS + LETTERS;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * 生成指定长度的纯数字验证码
     * 
     * @param length 验证码长度
     * @return 纯数字验证码
     */
    public static String generateNumericCaptcha(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("Length must be positive");
        }
        StringBuilder captcha = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            captcha.append(NUMBERS.charAt(SECURE_RANDOM.nextInt(NUMBERS.length())));
        }
        return captcha.toString();
    }

    /**
     * 生成指定长度的数字和字母混合验证码
     * 
     * @param length 验证码长度
     * @return 数字和字母混合验证码
     */
    public static String generateMixedCaptcha(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("Length must be positive");
        }
        StringBuilder captcha = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            captcha.append(NUMBERS_AND_LETTERS.charAt(SECURE_RANDOM.nextInt(NUMBERS_AND_LETTERS.length())));
        }
        return captcha.toString();
    }

    /**
     * 生成指定长度的纯字母验证码
     * 
     * @param length 验证码长度
     * @return 纯字母验证码
     */
    public static String generateLetterCaptcha(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("Length must be positive");
        }
        StringBuilder captcha = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            captcha.append(LETTERS.charAt(SECURE_RANDOM.nextInt(LETTERS.length())));
        }
        return captcha.toString();
    }

    /**
     * 生成默认的4位数字验证码
     * 
     * @return 4位数字验证码
     */
    public static String generateCaptcha() {
        return generateNumericCaptcha(4);
    }

    /**
     * 生成指定范围内的随机数字验证码
     * 
     * @param min 最小值（包含）
     * @param max 最大值（包含）
     * @return 指定范围内的随机数字
     */
    public static String generateRangeCaptcha(int min, int max) {
        if (min >= max) {
            throw new IllegalArgumentException("Max must be greater than min");
        }
        return String.valueOf(SECURE_RANDOM.nextInt(max - min + 1) + min);
    }

    /**
     * 验证码是否匹配（大小写不敏感）
     * 
     * @param captcha 原始验证码
     * @param input   用户输入的验证码
     * @return 是否匹配
     */
    public static boolean validateCaptcha(String captcha, String input) {
        if (captcha == null || input == null) {
            return false;
        }
        return captcha.equalsIgnoreCase(input.trim());
    }
}
