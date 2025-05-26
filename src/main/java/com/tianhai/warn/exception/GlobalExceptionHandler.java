package com.tianhai.warn.exception;

import com.tianhai.warn.enums.ResultCode;
import com.tianhai.warn.utils.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@ControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // @ExceptionHandler(BusinessException.class)
    // @ResponseBody
    // public Result<?> handleBusinessException(BusinessException e) {
    // logger.warn("业务异常：{}", e.getMessage());
    //// if (e.getResultCode() != null) {
    //// return Result.error(e.getResultCode());
    //// }
    //
    // return Result.error(e.getMessage());
    // }
    //
    // @ExceptionHandler(SystemException.class)
    // @ResponseBody
    // public Result<?> handleSystemException(SystemException e) {
    // logger.error("系统异常：" , e);
    // return Result.error(ResultCode.ERROR);
    // }
    //
    // @ExceptionHandler(MethodArgumentNotValidException.class)
    // @ResponseBody
    // public Result<?> handleValidException(MethodArgumentNotValidException e) {
    // BindingResult bindingResult = e.getBindingResult();
    // String message = null;
    // if (bindingResult.hasErrors()) {
    // FieldError fieldError = bindingResult.getFieldError();
    // if (fieldError != null) {
    // message = fieldError.getField() + fieldError.getDefaultMessage();
    // }
    // }
    //
    // return Result.error(ResultCode.VALIDATE_FAILED.getCode(), message);
    // }
    //
    // @ExceptionHandler(Exception.class)
    // @ResponseBody
    // public Result<?> handleException(Exception e) {
    // logger.error("未知异常：", e);
    //
    // return Result.error(ResultCode.ERROR);
    // }

    @ExceptionHandler(BusinessException.class)
    // @ResponseBody
    public Object handleBusinessException(BusinessException e,
            HttpServletRequest request) {
        logger.warn("业务异常：{}", e.getMessage());

        request.setAttribute("errorMsg", e.getMessage());

        if (isAjaxRequest(request)) {
            return Result.error(e.getMessage());
        } else {
            return redirectBackWithErrorMessage(request, e.getMessage());
        }
    }

    @ExceptionHandler(SystemException.class)
    // @ResponseBody
    public Object handleSystemException(SystemException e,
            HttpServletRequest request) {
        logger.error("系统异常：", e);

        request.setAttribute("errorMsg", e.getMessage());

        if (isAjaxRequest(request)) {
            return Result.error(ResultCode.ERROR);
        } else {
            return redirectBackWithErrorMessage(request, e.getMessage());
        }
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseBody
    public Result<?> handleValidException(MethodArgumentNotValidException e,
            HttpServletRequest request) {
        BindingResult bindingResult = e.getBindingResult();
        StringBuilder message = new StringBuilder();
        if (bindingResult.hasErrors()) {
            List<FieldError> fieldErrors = bindingResult.getFieldErrors();
            for (FieldError fieldError : fieldErrors) {
                message.append(fieldError.getDefaultMessage()).append("; ");
            }
        }
        return Result.error(ResultCode.VALIDATE_FAILED.getCode(), message.toString());
    }

    @ExceptionHandler(Exception.class)
    // @ResponseBody
    public Object handleException(Exception e,
            HttpServletRequest request) {
        logger.error("未知异常：", e);
        if (isAjaxRequest(request)) {
            return Result.error(ResultCode.ERROR);
        } else {
            return redirectBackWithErrorMessage(request, e.getMessage());
        }
    }

    /**
     * 判断是否为Ajax请求
     * 
     * @param request 请求
     * @return 判断结果
     */
    private boolean isAjaxRequest(HttpServletRequest request) {
        String header = request.getHeader("X-Requested-With");
        return "XMLHttpRequest".equalsIgnoreCase(header);
    }

    /**
     * 通用跳转方法
     * 
     * @param request  请求
     * @param errorMsg 错误信息
     * @return 重定向视图
     */
    private String redirectBackWithErrorMessage(HttpServletRequest request, String errorMsg) {
        String referer = request.getHeader("Referer");
        System.out.println("Referer: " + referer);
        if (referer != null && !referer.isEmpty() &&
                referer.contains(request.getServerName())) {
            request.getSession().setAttribute("errorMsg", errorMsg);
            return "redirect:" + referer;
        } else {
            request.setAttribute("errorMsg", errorMsg);
            return "login"; // 或者指定默认视图页
        }
    }
}
