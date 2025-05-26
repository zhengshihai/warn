package com.tianhai.warn.exception;

import com.tianhai.warn.enums.IResultCode;

public class BusinessException extends RuntimeException{

    private IResultCode resultCode;

    public BusinessException(String message) {
        super(message);
    }

    public BusinessException(IResultCode resultCode) {
        super(resultCode.getMessage());
        this.resultCode = resultCode;
    }

    public IResultCode getResultCode() {
        return resultCode;
    }
}
