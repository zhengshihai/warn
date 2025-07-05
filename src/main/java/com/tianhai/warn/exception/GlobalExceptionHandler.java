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

   

    @ExceptionHandler(BusinessException.class)
    // @ResponseBody
     /**
     * 修改：加@ResponseBody，确保AJAX请求时返回JSON，防止返回视图名导致404
     */
    @ResponseBody
    public Object handleBusinessException(BusinessException e, HttpServletRequest request) {
        logger.error("业务异常：{}", e.getMessage());

        request.setAttribute("errorMsg", e.getMessage());

        if (isAjaxRequest(request)) {
            return Result.error(e.getResultCode());
        } else {
            return redirectBackWithErrorMessage(request, e.getMessage());
        }
    }

    @ExceptionHandler(SystemException.class)
    // @ResponseBody
     /**
     * 修改：加@ResponseBody，确保AJAX请求时返回JSON，防止返回视图名导致404
     */
    @ResponseBody
    public Object handleSystemException(SystemException e, HttpServletRequest request) {
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
     /**
     * 修改：加@ResponseBody，确保AJAX请求时返回JSON，防止返回视图名导致404
     */
    @ResponseBody
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
        logger.info("Referer: " + referer);
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
