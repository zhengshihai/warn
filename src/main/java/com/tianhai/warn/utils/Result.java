package com.tianhai.warn.utils;

import com.tianhai.warn.enums.IResultCode;
import com.tianhai.warn.enums.ResultCode;
import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;

@Getter
public class Result<T> implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Integer code; // 状态码
    private String message; // 提示信息
    private T data; // 数据
    private boolean success; // 是否成功

    // 成功静态方法
    public static <T> Result<T> success() {
        return new Result<T>()
                .setCode(ResultCode.SUCCESS.getCode())
                .setMessage(ResultCode.SUCCESS.getMessage())
                .setSuccess(true);
    }

    public static <T> Result<T> success(T data) {
        return new Result<T>()
                .setCode(ResultCode.SUCCESS.getCode())
                .setMessage(ResultCode.SUCCESS.getMessage())
                .setData(data)
                .setSuccess(true);
    }

    public static <T> Result<T> success(T data, String message) {
        return new Result<T>()
                .setCode(ResultCode.SUCCESS.getCode())
                .setMessage(message)
                .setData(data)
                .setSuccess(true);
    }

    // 失败静态方法
    public static <T> Result<T> error() {
        return new Result<T>()
                .setCode(ResultCode.ERROR.getCode())
                .setMessage(ResultCode.ERROR.getMessage())
                .setSuccess(false);
    }

    public static <T> Result<T> error(String message) {
        return new Result<T>()
                .setCode(ResultCode.ERROR.getCode())
                .setMessage(message)
                .setSuccess(false);
    }

    public static <T> Result<T> error(IResultCode resultCode) {
        return new Result<T>()
                .setCode(resultCode.getCode())
                .setMessage(resultCode.getMessage())
                .setSuccess(false);
    }

    public static <T> Result<T> error(Integer code, String message) {
        return new Result<T>()
                .setCode(code)
                .setMessage(message)
                .setSuccess(false);
    }

    // getter和setter方法
    // 使用链式调用
    public Result<T> setCode(Integer code) {
        this.code = code;
        return this;
    }


    public Result<T> setMessage(String message) {
        this.message = message;
        return this;
    }



    public Result<T> setData(T data) {
        this.data = data;
        return this;
    }

    public Result<T> setSuccess(boolean success) {
        this.success = success;
        return this;
    }
}