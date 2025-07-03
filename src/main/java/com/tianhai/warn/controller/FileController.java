package com.tianhai.warn.controller;

import com.tianhai.warn.annotation.LogOperation;
import com.tianhai.warn.annotation.RequirePermission;
import com.tianhai.warn.enums.ResultCode;
import com.tianhai.warn.exception.BusinessException;
import com.tianhai.warn.service.impl.LocalFileServiceImpl;

import jakarta.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.File;
import java.nio.file.Paths;
import java.nio.file.Path;

@Controller
@RequestMapping("/file")
public class FileController {
    private static final Logger logger = LoggerFactory.getLogger(FileController.class);

    @Value("${file.upload.base-path}")
    private String basePath;

    @Value("${file.upload.base-url}")
    private String baseUrl;

    @Autowired
    private LocalFileServiceImpl localFileStorageService;

    @GetMapping("/download/**")
    @RequirePermission
    @LogOperation("下载文件")
    public ResponseEntity<Resource> downloadFile(HttpServletRequest request) {
        String filePath = extractFilePath(request);
        if (StringUtils.isBlank(filePath)) {
            logger.error("文件路径不正确");
            throw new BusinessException(ResultCode.VALIDATE_FAILED);
        }

        logger.info("收到下载请求，原始路径: {}", filePath);
        logger.info("当前配置 - basePath: {}, baseUrl: {}", basePath, baseUrl);

        // 移除开头的 uploads/ 前缀（如果存在）
        String relativePath = filePath;
        if (filePath.startsWith("uploads/")) {
            relativePath = filePath.substring("uploads/".length());
        }
        // 路径安全校验
        if (relativePath.contains("..") || relativePath.startsWith("/") || relativePath.startsWith("\\")) {
            logger.error("非法的文件路径: {}", relativePath);
            throw new BusinessException(ResultCode.VALIDATE_FAILED);
        }
        // 规范化路径
        Path fullPath = Paths.get(basePath, relativePath).normalize();
        File file = fullPath.toFile();
        logger.info("尝试访问文件: {}", fullPath);

        if (!file.exists()) {
            logger.error("文件不存在: {}", fullPath);
            throw new BusinessException(ResultCode.FILE_NOT_EXISTS);
        }

        return localFileStorageService.buildDownloadResponse(file);
    }

    @GetMapping("/preview/**")
    @RequirePermission
    @LogOperation("在线预览文件")
    public ResponseEntity<Resource> previewFile(HttpServletRequest request) {
        String filePath = extractFilePath(request);
        if (StringUtils.isBlank(filePath)) {
            logger.error("文件路径不正确");
            throw new BusinessException(ResultCode.VALIDATE_FAILED);
        }

        logger.info("收到预览请求，原始路径: {}", filePath);
        logger.info("当前配置 - basePath: {}, baseUrl: {}", basePath, baseUrl);

        // 移除开头的 uploads/ 前缀（如果存在）
        String relativePath = filePath;
        if (filePath.startsWith("uploads/")) {
            relativePath = filePath.substring("uploads/".length());
        }
        // 路径安全校验
        if (relativePath.contains("..") || relativePath.startsWith("/") || relativePath.startsWith("\\")) {
            logger.error("非法的文件路径: {}", relativePath);
            throw new BusinessException(ResultCode.VALIDATE_FAILED);
        }
        // 规范化路径
        Path fullPath = Paths.get(basePath, relativePath).normalize();
        File file = fullPath.toFile();
        logger.info("尝试预览文件: {}", fullPath);

        if (!file.exists()) {
            logger.error("文件不存在: {}", fullPath);
            throw new BusinessException(ResultCode.FILE_NOT_EXISTS);
        }

        return localFileStorageService.buildPreviewResponse(file);
    }

    /**
     * 从请求中提取文件路径
     */
    private String extractFilePath(HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        String contextPath = request.getContextPath();

        // 移除上下文路径
        String path = requestURI.substring(contextPath.length());

        // 移除 /file/preview/ 或 /file/download/ 前缀
        if (path.startsWith("/file/preview/")) {
            path = path.substring("/file/preview/".length());
        } else if (path.startsWith("/file/download/")) {
            path = path.substring("/file/download/".length());
        }

        // 移除开头的斜杠
        path = path.replaceAll("^/+", "");

        logger.info("提取的文件路径: {}", path);
        return path;
    }
}
