package com.tianhai.warn.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 注册数据传输对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    /**
     * 用户名
     * 要求：中文，至少2个字符
     */
    @NotBlank(message = "用户名不能为空")
    @Pattern(regexp = "^[\\u4e00-\\u9fa5]{2,}$", message = "用户名必须为至少2个中文字符")
    private String name;

    /**
     * 邮箱地址
     * 要求：符合邮箱格式
     */
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    /**
     * 用户角色
     * 可选值：student（学生）、systemuser（班级管理员）、dormitorymanager（宿管）superadmin（超级管理员）
     */
    @NotBlank(message = "角色不能为空")
    private String role;

    /**
     * 邮箱验证码
     * 要求：不能为空，必须是4位数字
     */
    @NotBlank(message = "邮箱验证码不能为空")
    @Pattern(regexp = "^\\d{4}$", message = "邮箱验证码必须是4位数字")
    private String emailCaptcha;

    /**
     * 用户密码
     * 要求： 长度为6-25个字符 且包含大小写英文字母
     */
    @NotBlank(message = "密码不能为空")
//    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])[A-Za-z]{6,25}$",
//            message = "密码长度为6-25个字符，且必须包含大小写字母")
    private String password;

}
