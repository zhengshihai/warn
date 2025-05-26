package com.tianhai.warn.exception;

import com.tianhai.warn.enums.IResultCode;

public class SystemException extends RuntimeException {

    private IResultCode resultCode;

    public SystemException(String message) {
        super(message);
    }

    public SystemException(IResultCode resultCode) {
        super(resultCode.getMessage());
        this.resultCode = resultCode;
    }

    public IResultCode getResultCode() {
        return resultCode;
    }
}
