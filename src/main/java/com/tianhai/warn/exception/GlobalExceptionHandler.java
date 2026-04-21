package com.tianhai.warn.exception;

import com.tianhai.warn.enums.IResultCode;
import com.tianhai.warn.enums.ResultCode;
import com.tianhai.warn.utils.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.util.List;

/**
 * 业务代码抛出异常
 *     ↓
 * 全局异常处理器拦截
 *     ↓
 * 判断异常类型（业务/系统/参数/未知）
 *     ↓
 * 判断请求类型（AJAX / 页面）
 *     ↓
 * AJAX → 返回统一JSON结果
 * 页面 → 重定向回原页面 + 携带错误信息
 *     ↓
 * 自动设置HTTP状态码 + 打印规范日志
 */
@ControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    @ResponseBody  // 加@ResponseBody，确保AJAX请求时返回JSON，防止返回视图名导致404
    public Object handleBusinessException(BusinessException e,
                                          HttpServletRequest request,
                                          HttpServletResponse response) {
        logger.error("业务异常：{}", e.getMessage());

        request.setAttribute("errorMsg", e.getMessage());
        
        response.setStatus(mapResultCodeToHttpStatus(e.getResultCode()));

        if (isAjaxRequest(request)) {
            return Result.error(e.getResultCode());
        } else {
            return redirectBackWithErrorMessage(request, e.getMessage(), response);
        }
    }

    @ExceptionHandler(SystemException.class)
    //  加@ResponseBody，确保AJAX请求时返回JSON，防止返回视图名导致404
    @ResponseBody
    public Object handleSystemException(SystemException e,
                                        HttpServletRequest request,
                                        HttpServletResponse response) {
        logger.error("系统异常：", e);

        request.setAttribute("errorMsg", e.getMessage());

        if (isAjaxRequest(request)) {
            return Result.error(ResultCode.ERROR);
        } else {
            return redirectBackWithErrorMessage(request, e.getMessage(), response);
        }
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseBody
    public Result<?> handleValidException(MethodArgumentNotValidException e,
                                          HttpServletRequest request,
                                          HttpServletResponse response) {
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
    @ResponseBody // 加@ResponseBody，确保AJAX请求时返回JSON，防止返回视图名导致404
    public Object handleException(Exception e,
                                  HttpServletRequest request,
                                  HttpServletResponse response) {
        logger.error("未知异常：", e);
        if (isAjaxRequest(request)) {
            return Result.error(ResultCode.ERROR);
        } else {
            return redirectBackWithErrorMessage(request, e.getMessage(), response);
        }
    }

    private int mapResultCodeToHttpStatus(IResultCode resultCode) {
        if (resultCode == ResultCode.UNAUTHORIZED) {
            return HttpServletResponse.SC_UNAUTHORIZED; // 401
        } else if (resultCode == ResultCode.FORBIDDEN) {
            return HttpServletResponse.SC_FORBIDDEN; // 403
        } else if (resultCode == ResultCode.ERROR) {
            return HttpServletResponse.SC_INTERNAL_SERVER_ERROR; // 500
        } else if (resultCode == ResultCode.SUCCESS) {
            return HttpServletResponse.SC_OK; // 200
        } else {
            // 其它所有业务错误都用 400
            return HttpServletResponse.SC_BAD_REQUEST; // 400
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
    private String redirectBackWithErrorMessage(HttpServletRequest request,
                                                String errorMsg,
                                                HttpServletResponse response) {
        String referer = request.getHeader("Referer");
        logger.info("Referer: " + referer);
        if (referer != null && !referer.isEmpty() &&
                referer.contains(request.getServerName())) {
            try {
                response.sendRedirect(referer);
                return null; // 返回 null 表示不再进入视图解析流程
            } catch (IOException e) {
                logger.error("重定向回原页面失败", e);
                request.getSession().setAttribute("errorMsg", errorMsg);
                return "login"; // 或者指定默认视图页
            }
        } else {
            request.setAttribute("errorMsg", errorMsg);
            return "login"; // 或者指定默认视图页
        }
    }
}
