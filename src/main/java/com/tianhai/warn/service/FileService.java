package com.tianhai.warn.service;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Date;

/**
 * 文件服务接口
 */
public interface FileService {
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

    /**
     * 保存音视频分片数据
     * @param alarmNo       报警号
     * @param sessionId     会话ID
     * @param chunkIndex    分片索引
     * @param chunkData     分片数据的字节缓冲区
     */
    void saveMediaChunk(String alarmNo, String sessionId, int chunkIndex, ByteBuffer chunkData);

    /**
     * 合并音视频分片数据
     * @param alarmNo       报警号
     * @param sessionId     会话ID
     */
    void mergeMediaChunks(String alarmNo, String sessionId);
}