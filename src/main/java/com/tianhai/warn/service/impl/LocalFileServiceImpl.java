package com.tianhai.warn.service.impl;

// import com.tianhai.warn.constants.AlarmConstants; // 已注释：不再使用 RocketMQ
import com.tianhai.warn.enums.ResultCode;
import com.tianhai.warn.exception.BusinessException;
import com.tianhai.warn.exception.SystemException;
// import com.tianhai.warn.mq.RocketMQMessageSender; // 已注释：不再使用 RocketMQ
import com.tianhai.warn.service.FileService;
import com.tianhai.warn.service.VideoProcessService;
import com.tianhai.warn.utils.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
// import org.springframework.core.env.Environment; // 未使用
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;

@Service
public class LocalFileServiceImpl implements FileService {
    private static final Logger logger = LoggerFactory.getLogger(LocalFileServiceImpl.class);

    @Value("${file.upload.base-path}")
    private String basePath;

    @Value("${file.upload.base-url}")
    private String baseUrl;

    @Value("${file.video.storage.path}")
    private String videoStoragePath;

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
        return getFileUrl(directory + File.separator + newFilename);
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
        String contentType = FileUtils.getContentType(fileName);
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
            case "mp4":
                // 视频文件（报警视频统一转码为mp4）
                logger.info("设置视频 Content-Type: video/mp4");
                headers.setContentType(MediaType.parseMediaType("video/mp4"));
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
        return baseUrl + File.separator + relativePath;
    }

    // 保存音视频分片数据
    @Override
    public void saveMediaChunk(String alarmNo, String sessionId, int chunkIndex, ByteBuffer chunkData) {
        logger.info("开始保存前端传来的.chunk格式的音视频数据，alarmNo: {}, sessionId: {}, chunkIndex: {}", alarmNo, sessionId,
                chunkIndex);
        File chunkFile = null;
        try {
            File dir = new File(videoStoragePath, alarmNo + File.separator + sessionId);
            if (!dir.exists())
                dir.mkdirs();
            chunkFile = new File(dir, chunkIndex + ".chunk");
            try (FileOutputStream fos = new FileOutputStream(chunkFile)) {
                byte[] bytes = new byte[chunkData.remaining()];
                chunkData.get(bytes);
                fos.write(bytes);
            }
        } catch (IOException e) {
            logger.error("保存音视频分片数据失败: {}, 分片索引: {}", chunkFile.getAbsolutePath(), chunkIndex, e);
            throw new SystemException(ResultCode.VIDEO_DATA_SAVE_FAILED);
        }
    }

    // 合并音视频分片数据成.webm文件
    @Override
    public void mergeMediaChunks(String alarmNo, String sessionId) {
        logger.info("开始合并音视频分片数据成.webm文件");

        File dir = new File(videoStoragePath, alarmNo + File.separator + sessionId);
        File[] chunkFiles = dir.listFiles((d, fileName) -> fileName.endsWith(".chunk"));

        if (chunkFiles == null || chunkFiles.length == 0) {
            return;
        }

        Arrays.sort(chunkFiles, Comparator.comparingInt(
                file -> Integer.parseInt(file.getName().replace(".chunk", ""))));

        // 新增：合并后文件放到 one-click/media/video/ALxxx/ 目录下
        String mergedWebmVideoFileName = alarmNo + "_" + sessionId + ".webm";
        File mergedWebmVideo = new File(videoStoragePath + File.separator + alarmNo,
                mergedWebmVideoFileName);

        try (FileOutputStream fos = new FileOutputStream(mergedWebmVideo)) {
            for (File chunk : chunkFiles) {
                Files.copy(chunk.toPath(), fos);
            }
            // 确保缓冲区刷新到磁盘
            fos.flush();
            fos.getFD().sync(); // 强制同步到磁盘，确保数据完整写入
            logger.info("音视频分片数据合并成功，合并后的文件: {}", mergedWebmVideo.getAbsolutePath());
        } catch (IOException e) {
            logger.error("合并音视频分片失败: alarmNo={}, sessionId={}", alarmNo, sessionId, e);
            throw new SystemException(ResultCode.VIDEO_CHUNK_MERGE_FAILED);
        }

        // 删除分片文件
        for (File chunk : chunkFiles) {
            if (!chunk.delete()) {
                logger.warn("删除分片文件失败: {}", chunk.getAbsolutePath());
            }
        }

        /*
         * 等待500毫秒，添加短暂延迟
         * 确保文件系统完成写入操作，这对于 Windows 系统尤其重要，因为文件句柄释放可能有延迟
         */
        try {
            Thread.sleep(500);
            logger.debug("等待文件系统同步完成: {}", mergedWebmVideo.getAbsolutePath());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("等待文件同步被中断: alarmNo={}, sessionId={}", alarmNo, sessionId);
        }

        // 验证文件是否可读
        String mergedVideoPath = mergedWebmVideo.getAbsolutePath();
        if (!mergedWebmVideo.exists()) {
            logger.error("合并后的webm文件不存在: alarmNo={}, sessionId={}, path={}",
                    alarmNo, sessionId, mergedVideoPath);
            return;
        }
        if (!mergedWebmVideo.canRead()) {
            logger.error("合并后的webm文件无法读取: alarmNo={}, sessionId={}, path={}",
                    alarmNo, sessionId, mergedVideoPath);
            return;
        }
        if (mergedWebmVideo.length() == 0) {
            logger.error("合并后的webm文件大小为0: alarmNo={}, sessionId={}, path={}",
                    alarmNo, sessionId, mergedVideoPath);
            return;
        }

        logger.info("文件验证成功: 大小={}字节, 路径={}", mergedWebmVideo.length(), mergedVideoPath);

        // 已修改：直接处理视频，不使用 RocketMQ
        // String targetVideoFormat = "mp4";
        // sendWebmVideoRQMessage(mergedVideoPath, targetVideoFormat, alarmNo,
        // sessionId);

        // 直接调用服务处理视频文件
        try {
            videoProcessService.handleVideoFile(mergedVideoPath);
            logger.info("视频处理任务已启动: alarmNo={}, sessionId={}, webmPath={}",
                    alarmNo, sessionId, mergedVideoPath);
        } catch (Exception e) {
            logger.error("视频处理失败: alarmNo={}, sessionId={}, webmPath={}, 错误信息: {}",
                    alarmNo, sessionId, mergedVideoPath, e.getMessage(), e);
            // 不抛出异常，避免影响 WebSocket 响应
        }
    }

    // 已注释：改为直接调用服务，不使用 RocketMQ
    // @Autowired
    // private RocketMQMessageSender rocketMQMessageSender;

    @Autowired
    private VideoProcessService videoProcessService;

    // 已注释：改为直接调用服务，不使用 RocketMQ
    /*
     * // 处理webm格式的视频文件
     * private void sendWebmVideoRQMessage(String mergedVideoPathStr,
     * String targetVideoFormat,
     * String alarmNo,
     * String sessionId) {
     * String topic = AlarmConstants.ROCKETMQ_TOPIC_VIDEO_PROCESS;
     * String tags = AlarmConstants.ROCKETMQ_TAG_VIDEO_PROCESS;
     * String keys = alarmNo + "_" + sessionId;
     * 
     * rocketMQMessageSender.sendMessage(topic, tags, keys, mergedVideoPathStr);
     * 
     * logger.info("发送webm视频处理MQ消息, topic={}, tags={}. keys={}, body={}",
     * topic, tags, keys, mergedVideoPathStr);
     * }
     */
}