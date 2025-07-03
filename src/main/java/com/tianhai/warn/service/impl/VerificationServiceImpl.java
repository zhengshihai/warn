package com.tianhai.warn.service.impl;

import com.tianhai.warn.constants.ValidationMessages;
import com.tianhai.warn.dto.StudentExcelDTO;
import com.tianhai.warn.dto.StudentInfoValidateResult;
import com.tianhai.warn.enums.BusinessType;
import com.tianhai.warn.service.StudentService;
import com.tianhai.warn.service.VerificationService;
import com.tianhai.warn.utils.CaptchaUtils;
import com.tianhai.warn.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class VerificationServiceImpl implements VerificationService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private StudentService studentService;

    private static final String ITEM = "warn:";
    private static final String CAPTCHA_PREFIX = ITEM + "captcha:";
    private static final String EMAIL_PREFIX = ITEM + "email:";
    private static final long CAPTCHA_EXPIRE = 10; // 10分钟
    private static final long EMAIL_EXPIRE = 15; // 15分钟
    private static final long LIMIT_EXPIRE = 60; // 60秒
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^\\d{6,20}$");

    /**
     * 生成图形验证码
     * 
     * @param sessionId    会话ID
     * @param businessType 业务类型
     * @return 验证码
     */
    @Override
    public Result<String> generateSessionCaptcha(String sessionId, BusinessType businessType) {
        if (sessionId == null || businessType == null) {
            return Result.error("参数不能为空");
        }

        String captcha = CaptchaUtils.generateCaptcha();
        String hashKey = String.format("%s%s:captcha", ITEM, businessType.getCode());

        // 使用Hash存储验证码
        redisTemplate.opsForHash().put(hashKey, sessionId, captcha);
        // 设置过期时间
        redisTemplate.expire(hashKey, CAPTCHA_EXPIRE, TimeUnit.MINUTES);

        return Result.success(captcha);
    }

    /**
     * 生成邮箱验证码
     * 
     * @param email        邮箱地址
     * @param businessType 业务类型
     * @return 验证码
     */
    @Override
    public Result<String> generateEmailCaptcha(String email, BusinessType businessType) {
        if (email == null || businessType == null) {
            return Result.error("参数不能为空");
        }

        String captcha = CaptchaUtils.generateCaptcha();
        String key = String.format("%s%s:email:%s", ITEM, businessType.getCode(), email);

        redisTemplate.opsForValue().set(key, captcha, EMAIL_EXPIRE, TimeUnit.MINUTES);
        return Result.success(captcha);
    }

    /**
     * 验证图形验证码
     * 
     * @param sessionId    会话ID
     * @param captcha      用户输入的验证码
     * @param businessType 业务类型
     * @return 验证结果
     */
    @Override
    public Result<Boolean> validateImageCaptcha(String sessionId, String captcha, BusinessType businessType) {
        if (sessionId == null || captcha == null || businessType == null) {
            return Result.error("参数不能为空");
        }

        String hashKey = String.format("%s%s:captcha", ITEM, businessType.getCode());
        String realCaptcha = (String) redisTemplate.opsForHash().get(hashKey, sessionId);

        if (realCaptcha == null) {
            return Result.error("验证码已过期");
        }

        boolean isValid = realCaptcha.equals(captcha);
        if (isValid) {
            // 验证成功后删除验证码
            redisTemplate.opsForHash().delete(hashKey, sessionId);
            return Result.success(true);
        }
        return Result.error("验证码错误");
    }

    /**
     * 验证邮箱验证码
     * 
     * @param email        邮箱地址
     * @param captcha      用户输入的验证码
     * @param businessType 业务类型
     * @return 验证结果
     */
    @Override
    public Result<Boolean> validateEmailCaptcha(String email, String captcha, BusinessType businessType) {
        if (email == null || captcha == null || businessType == null) {
            return Result.error("参数不能为空");
        }

        String key = String.format("%s%s:email:%s", ITEM, businessType.getCode(), email);
        String realCaptcha = (String) redisTemplate.opsForValue().get(key);

        if (realCaptcha == null) {
            return Result.error("验证码已过期");
        }

        boolean isValid = realCaptcha.equals(captcha);
        if (isValid) {
            // 验证成功后删除验证码
            redisTemplate.delete(key);
            return Result.success(true);
        }
        return Result.error("验证码错误");
    }

    /**
     * 检查注册频率限制
     * 限制同一会话在60秒内最多进行20次注册操作
     * 
     * @param sessionId    会话ID
     * @param businessType 业务类型
     * @return 检查结果
     */
    public Result<Boolean> checkRegisterLimit(String sessionId, BusinessType businessType) {
        // 获取频率限制的Redis hash key
        String hashKey = String.format("%s%s:limit", ITEM, businessType.getCode());
        String countStr = (String) redisTemplate.opsForHash().get(hashKey, sessionId);
        int count = countStr != null ? Integer.parseInt(countStr) : 0;

        // 检查是否超过限制（20次）
        if (count >= 20) {
            return Result.error("操作过于频繁，请稍后再试");
        }

        // 更新访问计数
        if (count == 0) {
            // 第一次访问，设置计数器和过期时间
            redisTemplate.opsForHash().put(hashKey, sessionId, "1");
            redisTemplate.expire(hashKey, LIMIT_EXPIRE, TimeUnit.SECONDS);
        } else {
            // 增加计数
            redisTemplate.opsForHash().increment(hashKey, sessionId, 1);
        }

        return Result.success(true);
    }

    /**
     * 清理注册相关的所有数据
     * 包括图形验证码、邮箱验证码和频率限制记录
     * 
     * @param sessionId    会话ID
     * @param email        邮箱地址
     * @param businessType 业务类型
     */
    public void cleanupRegistrationCodes(String sessionId, String email, BusinessType businessType) {
        // 构建所有相关的Redis key
        String captchaHashKey = String.format("%s%s:captcha", ITEM, businessType.getCode());
        String limitHashKey = String.format("%s%s:limit", ITEM, businessType.getCode());
        String emailKey = String.format("%s%s:email:%s", ITEM, businessType.getCode(), email);

        // 清理所有数据
        redisTemplate.opsForHash().delete(captchaHashKey, sessionId);
        redisTemplate.opsForHash().delete(limitHashKey, sessionId);
        redisTemplate.delete(emailKey);
    }

    @Override
    public List<StudentInfoValidateResult> validateStudentExcelInfo(List<StudentExcelDTO> studentExcelDTOList) {
        Set<String> existingEmailSet = ConcurrentHashMap.newKeySet();
        Set<String> existingStudentNoSet = ConcurrentHashMap.newKeySet();

        // 获取数据库已有的学生邮箱和学号
        existingEmailSet.addAll(studentService.selectAllEmail());
        existingStudentNoSet.addAll(studentService.selectAllStudentNo());

        // 用于本次批量导入的重复检查
        Set<String> batchEmailSet = ConcurrentHashMap.newKeySet();
        Set<String> batchStudentNoSet = ConcurrentHashMap.newKeySet();

        // 并行流校验逻辑...
        return studentExcelDTOList.parallelStream()
                .map(dto -> validateStudent(dto, existingEmailSet, existingStudentNoSet, batchEmailSet, batchStudentNoSet))
                .collect(Collectors.toList());
    }

    /**
     * 校验单个学生信息
     */
    private StudentInfoValidateResult validateStudent(StudentExcelDTO dto,
            Set<String> existingEmailSet,
            Set<String> existingStudentNoSet,
            Set<String> batchEmailSet,
            Set<String> batchStudentNoSet) {
        StudentInfoValidateResult result = new StudentInfoValidateResult();
        result.setStudentExcelDTO(dto);
        result.setValid(true);
        List<String> errors = new ArrayList<>();

        // 必填校验
        if (isBlank(dto.getStudentNo())) {
            errors.add(ValidationMessages.STUDENT_NO_EMPTY);
            result.setValid(false);
        }
        if (isBlank(dto.getName())) {
            errors.add(ValidationMessages.NAME_EMPTY);
            result.setValid(false);
        }
        if (isBlank(dto.getEmail())) {
            errors.add(ValidationMessages.EMAIL_EMPTY);
            result.setValid(false);
        }
        if (isBlank(dto.getClassName())) {
            errors.add(ValidationMessages.CLASS_NAME_EMPTY);
            result.setValid(false);
        }
        if (isBlank(dto.getDormitory())) {
            errors.add(ValidationMessages.DORMITORY_EMPTY);
            result.setValid(false);
        }
        if (isBlank(dto.getPassword())) {
            errors.add(ValidationMessages.PASSWORD_EMPTY);
            result.setValid(false);
        }

        // 长度校验
        if (!isBlank(dto.getStudentNo()) && dto.getStudentNo().length() > 20) {
            errors.add(ValidationMessages.STUDENT_NO_TOO_LONG);
            result.setValid(false);
        }
        if (!isBlank(dto.getName()) && dto.getName().length() > 50) {
            errors.add(ValidationMessages.NAME_TOO_LONG);
            result.setValid(false);
        }
        if (!isBlank(dto.getEmail()) && dto.getEmail().length() > 50) {
            errors.add(ValidationMessages.EMAIL_TOO_LONG);
            result.setValid(false);
        }
        if (!isBlank(dto.getClassName()) && dto.getClassName().length() > 50) {
            errors.add(ValidationMessages.CLASS_NAME_TOO_LONG);
            result.setValid(false);
        }
        if (!isBlank(dto.getDormitory()) && dto.getDormitory().length() > 50) {
            errors.add(ValidationMessages.DORMITORY_TOO_LONG);
            result.setValid(false);
        }
        if (!isBlank(dto.getPassword()) && dto.getPassword().length() > 100) {
            errors.add(ValidationMessages.PASSWORD_TOO_LONG);
            result.setValid(false);
        }

        // 格式校验
        if (!isBlank(dto.getEmail()) && !isValidEmail(dto.getEmail())) {
            errors.add(ValidationMessages.EMAIL_FORMAT_INVALID);
            result.setValid(false);
        }
        if (!isBlank(dto.getPhone()) && !isValidPhone(dto.getPhone())) {
            errors.add(ValidationMessages.PHONE_FORMAT_INVALID);
            result.setValid(false);
        }

        // 唯一性校验（数据库+本次批量）
        if (!isBlank(dto.getStudentNo())) {
            if (existingStudentNoSet.contains(dto.getStudentNo())) {
                errors.add(ValidationMessages.STUDENT_NO_EXISTS);
                result.setValid(false);
            } else if (!batchStudentNoSet.add(dto.getStudentNo())) {
                errors.add(ValidationMessages.STUDENT_NO_DUPLICATE);
                result.setValid(false);
            }
        }

        if (!isBlank(dto.getEmail())) {
            if (existingEmailSet.contains(dto.getEmail())) {
                errors.add(ValidationMessages.EMAIL_EXISTS);
                result.setValid(false);
            } else if (!batchEmailSet.add(dto.getEmail())) {
                errors.add(ValidationMessages.EMAIL_DUPLICATE);
                result.setValid(false);
            }
        }

        result.setErrors(errors);
        return result;
    }

    /**
     * 检查字符串是否为空
     */
    private boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * 校验邮箱格式
     */
    private boolean isValidEmail(String email) {
        String emailRegex = "^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$";
        return email.matches(emailRegex);
    }

    /**
     * 校验电话号码格式
     */
    private boolean isValidPhone(String phone) {
        String phoneRegex = "^\\d{6,20}$";
        return phone.matches(phoneRegex);
    }
}