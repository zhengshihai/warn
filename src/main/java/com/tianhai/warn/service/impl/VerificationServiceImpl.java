package com.tianhai.warn.service.impl;

import com.tianhai.warn.constants.ValidationMessages;
import com.tianhai.warn.dto.*;
import com.tianhai.warn.enums.BusinessType;
import com.tianhai.warn.model.DormitoryManager;
import com.tianhai.warn.model.SuperAdmin;
import com.tianhai.warn.model.SysUser;
import com.tianhai.warn.service.*;
import com.tianhai.warn.utils.CaptchaUtils;
import com.tianhai.warn.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
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

    @Autowired
    private SuperAdminService superAdminService;

    @Autowired
    private DormitoryManagerService dormitoryManagerService;

    @Autowired
    private SysUserService sysUserService;

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
                .map(dto -> validateStudent(dto, existingEmailSet, existingStudentNoSet,
                        batchEmailSet, batchStudentNoSet))
                .collect(Collectors.toList());
    }

    @Override
    public List<SuperAdminInfoValidateResult> validateSuperAdminExcelInfo(
            List<SuperAdminExcelDTO> superAdminExcelDTOList) {
        // 获取数据库已有的超级管理员的邮箱
        Set<String> existingEmailSet = superAdminService.selectAll().stream()
                .map(SuperAdmin::getEmail)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 用于本次批量导入的重复检查
        Set<String> batchEmailSet = new HashSet<>();

        return superAdminExcelDTOList.stream()
                .map(dto -> validateSuperAdmin(dto, existingEmailSet, batchEmailSet))
                .collect(Collectors.toList());
    }

    @Override
    public List<DorManInfoValidateResult> validateDorManExcelInfo(
            List<DormitoryManagerExcelDTO> allExcelDormitoryManagerInfoList) {
        // 获取全部宿管信息
        List<DormitoryManager> existingDorManList = dormitoryManagerService.selectAll();

        // 获取数据库已有的宿管的邮箱
        Set<String> existingEmailSet = existingDorManList.stream()
                .map(DormitoryManager::getEmail)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 获取数据库已有的宿管的工号
        Set<String> existingManagerId = existingDorManList.stream()
                .map(DormitoryManager::getManagerId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 用于本次批量导入的重复检查
        Set<String> batchEmailSet = new HashSet<>();
        Set<String> batchManagerId = new HashSet<>();

        return allExcelDormitoryManagerInfoList.stream()
                .map(dto -> validateDormitoryManager(dto, existingEmailSet, existingManagerId,
                        batchEmailSet, batchManagerId))
                .collect(Collectors.toList());
    }

    @Override
    public List<SysUserInfoValidateResult> validateSysUserExcelInfo(
            List<SysUserExcelDTO> allExcelSysUserInfoList) {
        // 获取全部班级管理员信息
        List<SysUser> existingSysUserList = sysUserService.selectAll();

        // 获取数据库已有的班级管理员的邮箱
        Set<String> existingEmailSet = existingSysUserList.stream()
                .map(SysUser::getEmail)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 获取数据库已有的班级管理员的工号
        Set<String> existingSysUserNoSet = existingSysUserList.stream()
                .map(SysUser::getSysUserNo)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        // 用于本地批量导入的重复检查
        Set<String> batchEmailSet = new HashSet<>();
        Set<String> batchSysUserNoSet = new HashSet<>();

        return allExcelSysUserInfoList.stream()
                .map(dto -> validateSysUser(dto, existingEmailSet, existingSysUserNoSet,
                        batchEmailSet, batchSysUserNoSet))
                .collect(Collectors.toList());

    }

    /**
     * 校验单个班级管理员的信息
     */
    private SysUserInfoValidateResult validateSysUser(SysUserExcelDTO dto,
            Set<String> existingEmailSet,
            Set<String> existingSysUserNoSet,
            Set<String> batchEmailSet,
            Set<String> batchSysUserNoSet) {
        SysUserInfoValidateResult result = new SysUserInfoValidateResult();
        result.setSysUserExcelDTO(dto);
        result.setValid(true);
        List<String> errors = new ArrayList<>();

        // 必填校验
        if (isBlank(dto.getSysUserNo())) {
            errors.add(ValidationMessages.SYS_USER_NO_EMPTY);
            result.setValid(false);
        }
        if (isBlank(dto.getName())) {
            errors.add(ValidationMessages.NAME_EMPTY);
            result.setValid(false);
        }
        if (isBlank(dto.getPassword())) {
            errors.add(ValidationMessages.PASSWORD_EMPTY);
            result.setValid(false);
        }
        if (isBlank(dto.getPhone())) {
            errors.add(ValidationMessages.PHONE_EMPTY);
            result.setValid(false);
        }
        if (isBlank(dto.getEmail())) {
            errors.add(ValidationMessages.EMAIL_EMPTY);
            result.setValid(false);
        }
        if (isBlank(dto.getJobRole())) {
            errors.add(ValidationMessages.JOR_ROLE_EMPTY);
            result.setValid(false);
        }

        // 长度校验
        if (!isBlank(dto.getSysUserNo()) && dto.getSysUserNo().length() > 40) {
            errors.add(ValidationMessages.SYS_USER_NO_TOO_LONG);
            result.setValid(false);
        }
        if (!isBlank(dto.getName()) && dto.getName().length() > 50) {
            errors.add(ValidationMessages.NAME_TOO_LONG);
            result.setValid(false);
        }
        if (!isBlank(dto.getPhone()) && dto.getPhone().length() > 20) {
            errors.add(ValidationMessages.PHONE_TOO_LONG);
            result.setValid(false);
        }
        if (!isBlank(dto.getEmail()) && dto.getEmail().length() > 100) {
            errors.add(ValidationMessages.EMAIL_TOO_LONG);
            result.setValid(false);
        }
        if (!isBlank(dto.getJobRole()) && dto.getJobRole().length() > 20) {
            errors.add(ValidationMessages.JOB_ROLE_TOO_LONG);
            result.setValid(false);
        }
        if (!isBlank(dto.getPassword()) && dto.getPassword().length() > 100) {
            errors.add(ValidationMessages.PASSWORD_TOO_LONG);
            result.setValid(false);
        }

        // 职位角色校验
        if (!isBlank(dto.getJobRole())) {
            if (!dto.getJobRole().equals("辅导员")
                && !dto.getJobRole().equals("班主任")
                && !dto.getJobRole().equals("院级领导")
                && !dto.getJobRole().equals("其他角色")) {
                errors.add(ValidationMessages.JOB_ROLE_DISABLE);
                result.setValid(false);
            }
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
        if (!isBlank(dto.getSysUserNo())) {
            if (existingSysUserNoSet.contains(dto.getSysUserNo())) {
                errors.add(ValidationMessages.SYS_USER_NO_EXISTS);
                result.setValid(false);
            } else if (!batchSysUserNoSet.add(dto.getSysUserNo())) {
                errors.add(ValidationMessages.SYS_USER_NO_DUPLICATE);
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
     * 校验单个宿管的信息
     * 因需要实现封装错误信息传回前端回显，故这里直接使用条件判断代替注解方式的判断
     */
    private DorManInfoValidateResult validateDormitoryManager(DormitoryManagerExcelDTO dto,
            Set<String> existingEmailSet,
            Set<String> existingManagerIdSet,
            Set<String> batchEmailSet,
            Set<String> batchManagerId) {
        DorManInfoValidateResult result = new DorManInfoValidateResult();
        result.setDormitoryManagerExcelDTO(dto);
        result.setValid(true);
        List<String> errors = new ArrayList<>();

        // 必填校验
        if (isBlank(dto.getManagerId())) {
            errors.add(ValidationMessages.MANAGER_ID_EMPTY);
            result.setValid(false);
        }
        if (isBlank(dto.getName())) {
            errors.add(ValidationMessages.NAME_EMPTY);
            result.setValid(false);
        }
        if (isBlank(dto.getBuilding())) {
            errors.add(ValidationMessages.BUILDING_EMPTY);
            result.setValid(false);
        }
        if (isBlank(dto.getPhone())) {
            errors.add(ValidationMessages.PHONE_EMPTY);
            result.setValid(false);
        }
        if (isBlank(dto.getPassword())) {
            errors.add(ValidationMessages.PASSWORD_EMPTY);
            result.setValid(false);
        }
        if (isBlank(dto.getEmail())) {
            errors.add(ValidationMessages.EMAIL_EMPTY);
            result.setValid(false);
        }

        // 长度校验
        if (!isBlank(dto.getManagerId()) && dto.getManagerId().length() > 20) {
            errors.add(ValidationMessages.MANAGER_ID_TOO_LONG);
            result.setValid(false);
        }
        if (!isBlank(dto.getName()) && dto.getName().length() > 50) {
            errors.add(ValidationMessages.NAME_TOO_LONG);
            result.setValid(false);
        }
        if (!isBlank(dto.getBuilding()) && dto.getBuilding().length() > 50) {
            errors.add(ValidationMessages.BUILDING_TOO_LONG);
            result.setValid(false);
        }
        if (!isBlank(dto.getPhone()) && dto.getPhone().length() > 20) {
            errors.add(ValidationMessages.PHONE_TOO_LONG);
            result.setValid(false);
        }
        if (!isBlank(dto.getEmail()) && dto.getEmail().length() > 100) {
            errors.add(ValidationMessages.EMAIL_TOO_LONG);
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
        if (!isBlank(dto.getManagerId())) {
            if (existingManagerIdSet.contains(dto.getManagerId())) {
                errors.add(ValidationMessages.MANAGER_ID_EXISTS);
                result.setValid(false);
            } else if (!batchManagerId.add(dto.getManagerId())) {
                errors.add(ValidationMessages.MANAGER_ID_DUPLICATE);
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
     * 校验超级管理员信息
     */
    private SuperAdminInfoValidateResult validateSuperAdmin(SuperAdminExcelDTO dto,
            Set<String> existingEmailSet,
            Set<String> batchEmailSet) {
        SuperAdminInfoValidateResult result = new SuperAdminInfoValidateResult();
        result.setSuperAdminExcelDTO(dto);
        result.setValid(true);
        List<String> errors = new ArrayList<>();

        // 必填校验
        if (isBlank(dto.getName())) {
            errors.add(ValidationMessages.NAME_EMPTY);
            result.setValid(false);
        }
        if (isBlank(dto.getEmail())) {
            errors.add(ValidationMessages.EMAIL_EMPTY);
            result.setValid(false);
        }
        if (isBlank(dto.getPassword())) {
            errors.add(ValidationMessages.PASSWORD_EMPTY);
            result.setValid(false);
        }

        // 长度校验
        if (!isBlank(dto.getName()) && dto.getName().length() > 50) {
            errors.add(ValidationMessages.NAME_TOO_LONG);
            result.setValid(false);
        }
        if (!isBlank(dto.getEmail()) && dto.getEmail().length() > 50) {
            errors.add(ValidationMessages.EMAIL_TOO_LONG);
            result.setValid(false);
        }
        if (!isBlank(dto.getPassword()) && dto.getPassword().length() > 100) {
            errors.add(ValidationMessages.PASSWORD_TOO_LONG);
            result.setValid(false);
        }

        // 唯一性校验（数据库+本次批量）
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