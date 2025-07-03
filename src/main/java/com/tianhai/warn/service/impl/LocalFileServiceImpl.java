package com.tianhai.warn.service.impl;

import com.tianhai.warn.enums.ResultCode;
import com.tianhai.warn.exception.BusinessException;
import com.tianhai.warn.service.FileService;
import com.tianhai.warn.utils.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.nio.charset.StandardCharsets;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

@Service
public class LocalFileServiceImpl implements FileService {
    private static final Logger logger = LoggerFactory.getLogger(LocalFileServiceImpl.class);

    private final String basePath;
    private final String baseUrl;

    public LocalFileServiceImpl() {
        // 从系统属性中获取配置，如果没有则使用默认值
        this.basePath = System.getProperty("file.upload.base-path", "E:/Warning/Warn/uploads");
        this.baseUrl = System.getProperty("file.upload.base-url", "/uploads");

        // 确保上传目录存在
        try {
            Files.createDirectories(Paths.get(basePath));
        } catch (IOException e) {
            logger.error("创建上传目录失败: {}", basePath, e);
        }
    }

    @Override
    public String storeFile(MultipartFile file, String directory, String studentNo, String timeStamp,
            Date expectedReturnTime)
            throws IOException {
        // 生成文件名
        String originalFilename = file.getOriginalFilename();
        String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));

        // 格式化日期为yyyyMMdd格式
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        String dateStr = dateFormat.format(expectedReturnTime);

        // 使用学号和日期生成新文件名 学号+晚归日期+时间戳+文件类型
        String newFilename = studentNo + "-" + dateStr + "-" + timeStamp + fileExtension;

        // 创建目标目录
        Path targetDir = Paths.get(basePath, directory);
        try {
            Files.createDirectories(targetDir);
        } catch (IOException e) {
            logger.error("创建目录失败: {}", targetDir, e);
            throw new IOException("创建存储目录失败: " + e.getMessage());
        }

        // 保存文件
        Path targetPath = targetDir.resolve(newFilename);
        try {
            file.transferTo(targetPath.toFile());
        } catch (IOException e) {
            logger.error("保存文件失败: {}", targetPath, e);
            throw new IOException("保存文件失败: " + e.getMessage());
        }

        // 返回文件的访问URL
        return getFileUrl(directory + "/" + newFilename);
    }

    /**
     * 构建文件下载响应
     */
    public ResponseEntity<Resource> buildDownloadResponse(File file) {
        // 创建文件资源
        Resource resource = new FileSystemResource(file);

        // 获取文件名
        String fileName = file.getName();
        // URL编码文件名，避免中文乱码
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");

        // 设置响应头
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFileName);
        headers.add(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate");
        headers.add(HttpHeaders.PRAGMA, "no-cache");
        headers.add(HttpHeaders.EXPIRES, "0");

        // 根据文件扩展名设置Content-Type
        String contentType = com.tianhai.warn.utils.FileUtils.getContentType(fileName);
        headers.setContentType(MediaType.parseMediaType(contentType));

        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(file.length())
                .body(resource);
    }

    public ResponseEntity<Resource> buildPreviewResponse(File file) {
        // 创建文件资源
        Resource resource = new FileSystemResource(file);

        // 获取文件名和扩展名
        String fileName = file.getName();
        String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        logger.info("文件类型: {}", extension);

        // 设置响应头
        HttpHeaders headers = new HttpHeaders();

        // 根据文件类型设置不同的预览方式
        switch (extension) {
            case "jpg":
            case "jpeg":
            case "png":
            case "gif":
                // 图片直接显示
                String imageType = "image/" + extension;
                logger.info("设置图片Content-Type: {}", imageType);
                headers.setContentType(MediaType.parseMediaType(imageType));
                break;
            case "pdf":
                // PDF文件
                logger.info("设置PDF Content-Type: application/pdf");
                headers.setContentType(MediaType.APPLICATION_PDF);
                break;
            case "doc":
            case "docx":
            case "xls":
            case "xlsx":
                // Office文档，使用内联方式显示
                String officeType = FileUtils.getContentType(fileName);
                logger.info("设置Office文档Content-Type: {}", officeType);
                headers.setContentType(MediaType.parseMediaType(officeType));
                headers.add(HttpHeaders.CONTENT_DISPOSITION, "inline; filename*=UTF-8''" +
                        URLEncoder.encode(fileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20"));
                break;
            default:
                // 不支持预览的文件类型
                logger.error("不支持的文件类型: {}", extension);
                throw new BusinessException(ResultCode.FILE_TYPE_ERROR);
        }

        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(file.length())
                .body(resource);
    }

    @Override
    public boolean deleteFile(String fileUrl) {
        try {
            // 从URL中提取相对路径
            String relativePath = fileUrl.replace(baseUrl, "");
            Path filePath = Paths.get(basePath, relativePath);
            return Files.deleteIfExists(filePath);
        } catch (IOException e) {
            logger.error("删除文件失败: {}", fileUrl, e);
            return false;
        }
    }

    @Override
    public String getFileUrl(String relativePath) {
        return baseUrl + "/" + relativePath;
    }
}