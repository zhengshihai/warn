package com.tianhai.warn.service;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.Date;

/**
 * 文件存储服务接口
 */
public interface FileStorageService {
    /**
     * 存储文件
     * 
     * @param file      上传的文件
     * @param directory 存储目录
     * @param studentNo 学号
     * @param timestamp 时间戳
     * @param time      时间
     * @return 文件的访问URL
     * @throws IOException 如果文件存储失败
     */
    String storeFile(MultipartFile file, String directory, String studentNo, String timestamp, Date time)
            throws IOException;

    /**
     * 删除文件
     * 
     * @param fileUrl 文件的访问URL
     * @return 是否删除成功
     */
    boolean deleteFile(String fileUrl);

    /**
     * 获取文件的完整URL
     * 
     * @param relativePath 文件的相对路径
     * @return 完整的访问URL
     */
    String getFileUrl(String relativePath);
}