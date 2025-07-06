package com.tianhai.warn.service;

import com.tianhai.warn.dto.*;
import com.tianhai.warn.enums.BusinessType;
import com.tianhai.warn.model.SuperAdmin;
import com.tianhai.warn.utils.Result;

import java.util.List;

public interface VerificationService {
    /**
     * 生成图形验证码
     * 
     * @param sessionId    会话ID
     * @param businessType 业务类型
     * @return 验证码
     */
    Result<String> generateSessionCaptcha(String sessionId, BusinessType businessType);

    /**
     * 生成邮箱验证码
     * 
     * @param email        邮箱地址
     * @param businessType 业务类型
     * @return 验证码
     */
    Result<String> generateEmailCaptcha(String email, BusinessType businessType);

    /**
     * 验证图形验证码
     * 
     * @param sessionId    会话ID
     * @param captcha      用户输入的验证码
     * @param businessType 业务类型
     * @return 验证结果
     */
    Result<Boolean> validateImageCaptcha(String sessionId, String captcha, BusinessType businessType);

    /**
     * 验证邮箱验证码
     * 
     * @param email        邮箱地址
     * @param captcha      用户输入的验证码
     * @param businessType 业务类型
     * @return 验证结果
     */
    Result<Boolean> validateEmailCaptcha(String email, String captcha, BusinessType businessType);

    Result<Boolean> checkRegisterLimit(String sessionId, BusinessType businessType);

    void cleanupRegistrationCodes(String sessionId, String email, BusinessType businessType);

    /**
     * 校验Excel文件的学生信息
     * @param studentExcelDTOList    待校验的学生信息
     * @return                       校验结果
     */
    List<StudentInfoValidateResult> validateStudentExcelInfo(List<StudentExcelDTO> studentExcelDTOList);

    /**
     * 校验Excel文件的超级管理员信息
     * @param superAdminExcelDTOList       待校验的超级管理员信息
     * @return                             校验结果
     */
    List<SuperAdminInfoValidateResult> validateSuperAdminExcelInfo(List<SuperAdminExcelDTO> superAdminExcelDTOList);

    /**
     * 校验Excel文件的宿管信息
     * @param allExcelDormitoryManagerInfoList    待校验的宿管信息
     * @return                                    校验结果
     */
    List<DorManInfoValidateResult> validateDorManExcelInfo(List<DormitoryManagerExcelDTO> allExcelDormitoryManagerInfoList);

    /**
     * 校验Excel文件的班级管理员信息
     * @param allExcelSysUserInfoList    待校验的班级管理员信息
     * @return                           校验结果
     */
    List<SysUserInfoValidateResult> validateSysUserExcelInfo(List<SysUserExcelDTO> allExcelSysUserInfoList);

    /**
     * 校验当前超级管理员的状态
     */
    void checkSuperAdminStatus();
}
