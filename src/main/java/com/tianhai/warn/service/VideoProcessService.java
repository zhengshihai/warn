package com.tianhai.warn.service;

public interface VideoProcessService {
    /**
     * 处理视频 包括但不仅限于格式转换等
     * @param videoPath   视频的绝对路径地址
     */
    void handleVideoFile(String videoPath);
}
