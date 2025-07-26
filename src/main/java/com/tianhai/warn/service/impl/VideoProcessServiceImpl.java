package com.tianhai.warn.service.impl;

import com.tianhai.warn.enums.ResultCode;
import com.tianhai.warn.exception.SystemException;
import com.tianhai.warn.model.*;
import com.tianhai.warn.service.*;
import com.tianhai.warn.utils.UniqueIdGenerator;
import org.apache.commons.lang3.StringUtils;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Service;
import org.bytedeco.ffmpeg.global.avcodec;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class VideoProcessServiceImpl implements VideoProcessService {

    private static final Logger logger = LoggerFactory.getLogger(VideoProcessServiceImpl.class);

    @Autowired
    private AlarmRecordService alarmRecordService;

    @Autowired
    private StudentService studentService;

    @Autowired
    private AlarmVideoService alarmVideoService;

    @Autowired
    private AsyncTaskExecutor asyncTaskExecutor;

    @Autowired
    private AlarmVideoSliceService alarmVideoSliceService;

    // 设定每个视频切片时间为10s
    private static final Long SLICE_DURATION_SECONDS = 10L;

    /**
     * 处理视频
     * @param webmVideoPathStr   webm格式源视频的绝对路径地址
     */
    @Override
    public void handleVideoFile(String webmVideoPathStr) {
        logger.info("开始处理视频文件, videoPath:{}", webmVideoPathStr);

        // 校验webm视频是否存在
        File sourceFile = new File(webmVideoPathStr);
        if (!sourceFile.exists()) {
            logger.error("原视频不存在，webmVideoPath：{}", webmVideoPathStr);
            throw new SystemException(ResultCode.VIDEO_NOT_EXISTS);
        }
        if (StringUtils.isBlank(webmVideoPathStr)) {
            logger.error("webmVideoPath不合规, {}", webmVideoPathStr);
            return;
        }

        // 获取webm视频文件名
        String webmVideoName = null;
        int lastSeparatorIndex = webmVideoPathStr.lastIndexOf(File.separator);
        if (lastSeparatorIndex >= 0 && lastSeparatorIndex < webmVideoPathStr.length() - 1) {
            webmVideoName = webmVideoPathStr.substring(lastSeparatorIndex + 1);
        }

        assert webmVideoName != null;
        int underscoreIndex = webmVideoName.indexOf('_');
        String alarmNo = (underscoreIndex != -1)
                ? webmVideoName.substring(0, underscoreIndex)
                : webmVideoName;
        logger.info("从webm格式视频获取到对应alarmNo为：{}", alarmNo);

        // 获取报警号对应的学号
        AlarmRecord alarmRecord = alarmRecordService.selectByAlarmNo(alarmNo);
        if (alarmRecord == null) {
            logger.error("alarm_record表中没有该报警记录，alarmNo: {}", alarmNo);
            return;
        }
        if (alarmRecord.getStudentNo() == null) {
            logger.error("该alarmRecord记录没有学生学号, alarmRecord: {}", alarmRecord);
            return;
        }
        String studentNo = alarmRecord.getStudentNo();

        // 使用JavaCV将webm格式的源视频转成mp4格式
        String mp4VideoPathStr = convertWebmToMp4(alarmNo, studentNo, webmVideoPathStr, webmVideoName);

        // 保存转码后mp4格式视频的信息到alarm_videos表 todo 根据视频大小使用线程池优化切片
        VideoMetaData videoMetaData = saveMp4AlarmVideoInfo(alarmNo, studentNo, mp4VideoPathStr);

        // 保存视频切片信息到mysql （此处切片的fileSizeBytes在切片完成后才更新）
        String outputFileDirStr = new File(mp4VideoPathStr).getParent();
        List<AlarmVideoSlice> alarmVideoSliceList = saveSlicedVideoInfo(alarmNo, studentNo, outputFileDirStr, videoMetaData);

        // 使用HLS开始异步切片
        asyncTaskExecutor.execute(() -> {
            try {
                splitVideoIntoChunksHls(mp4VideoPathStr, alarmNo, studentNo);
            } catch (Exception e) {
                logger.error("处理视频切片失败，源视频路径：{}, 开始删除数据库的视频切片信息", mp4VideoPathStr, e);

                // 为防止后续重新进行切片时发生冲突，删除可能已经产生的ts文件和m3u8.文件
                deleteTsAndM3u8File(alarmNo, outputFileDirStr);

                throw new SystemException(ResultCode.VIDEO_SPLICE_FAILED);
            }
        });

        // 切片操作完成后，更新切片的fileSizeBytes属性
        updateFileSizeBytes(alarmVideoSliceList, outputFileDirStr, alarmNo);

        // 保存列表到mysql todo
        alarmVideoSliceService.addAlarmVideoSliceBatch(alarmVideoSliceList);
    }

    /**
     * 转换视频格式并保存在新路径 todo 这里有bug无法转成mp4格式
     * @param alarmNo              报警号
     * @param studentNo            学号
     * @param webmVideoPathStr     原webm视频绝对路径
     * @param webmVideoName        原始webm视频文件名（不含后缀）
     * @return                     转换后的mp4视频的绝对路径
     */
    private String convertWebmToMp4(String alarmNo,
                                    String studentNo,
                                    String webmVideoPathStr,
                                    String webmVideoName) {
        // 构造输出目录：在webm原视频所在目录下，新建studentNo + "_" + alarmNo形式的子目录
        File sourceFile = new File(webmVideoPathStr);
        File parentDir = sourceFile.getParentFile();
        File targetDir = new File(parentDir, studentNo + "_" + alarmNo);
        if (!targetDir.exists()) {
            boolean created = targetDir.mkdirs();
            if (!created) {
                logger.error("创建mp4格式视频的目标目录失败, targetDir: {}", targetDir);
                throw new SystemException(ResultCode.VIDEO_CHUNK_MERGE_FAILED);
            }
        }

        // 构造mp4输出路径
        File outputMp4File = new File(targetDir, webmVideoName + ".mp4");
        String outputMp4Path = outputMp4File.getAbsolutePath();

        // 开始进行转码
        String repWebmVideoPathStr = webmVideoPathStr.replace("\\", "/");
        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(repWebmVideoPathStr);
             FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(outputMp4Path, 0)) {

            grabber.setFormat("webm");
            grabber.start();

            recorder.setFormat("mp4");
            recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264); // 使用H.264编码
            recorder.setFrameRate(grabber.getFrameRate());
            recorder.setImageWidth(grabber.getImageWidth());
            recorder.setImageHeight(grabber.getImageHeight());
            recorder.setAudioChannels(grabber.getAudioChannels());
            recorder.setSampleRate(grabber.getSampleRate());

            recorder.start();

            Frame frame;
            while ((frame = grabber.grab()) != null) {
                recorder.record(frame);
            }

            recorder.stop();
            grabber.stop();

        } catch (Exception e) {
            logger.error("转换webm视频到mp4格式失败， repWebmVideoPathStr: {}, 错误信息: {}", repWebmVideoPathStr, e.getMessage());
            throw new SystemException(ResultCode.VIDEO_CHANGE_FORMAT_FAILED);
        }

        logger.info("视频格式转换成功，原视频绝对路径 repWebmVideoPathStr: {}, 转换后视频绝对路径: {}", webmVideoPathStr, outputMp4Path);
        return outputMp4Path;
    }


    /**
     * 保存mp4视频信息到alarm_videos表
     * @param alarmNo       报警号
     * @param studentNo     学号
     * @param videoPathStr  视频的绝对路径（此处是指mp4格式的视频）
     * @return              返回保存的视频元数据对象
     */
    private VideoMetaData saveMp4AlarmVideoInfo(String alarmNo, String studentNo, String videoPathStr) {
        Student student = studentService.selectByStudentNo(studentNo);
        String studentName = null;
        if (student == null) {
            logger.warn("查不到该学号对应的学生信息, studentNo: {}", studentNo);
            studentName = "<未知学生>";
        }

        String titleTemplate = "学生%s的报警视频 学号%s 报警时间%s";
        String title = String.format(titleTemplate, studentName, studentNo, new Date());

        // 分析视频元数据
        VideoMetaData videoMetaData = analyseVideoMetaData(videoPathStr);
        String videoId = UniqueIdGenerator.generate("VI");
        videoMetaData.setVideoId(videoId);
        Date now = new Date();

        // 构造AlarmVideo对象
        AlarmVideo alarmVideo = AlarmVideo.builder()
                .videoId(videoId) // VI为video前缀
                .alarmNo(alarmNo)
                .studentNo(studentNo)
                .title(title)
                .filePath(videoPathStr)
                .durationSec(videoMetaData.getDurationSec())
                .fileSizeBytes(videoMetaData.getFileSizeBytes())
                .format("mp4") // 项目设定转换后的视频格式为mp4
                .resolution(videoMetaData.getResolution())
                .bitrate(videoMetaData.getBitrate())
                .createdAt(now)
                .updatedAt(now)
                .build();

        // 保存原视频信息到数据库
        alarmVideoService.addAlarmVideo(alarmVideo);
        logger.info("保存mp4视频信息到alarm_videos表成功，alarmNo: {}, studentNo: {}, videoPath: {}", alarmNo, studentNo, videoPathStr);

        return videoMetaData;
    }

    /**
     * 分析视频元数据
     * @param videoPathStr   视频的绝对路径
     * @return               视频元数据对象
     */
    private VideoMetaData analyseVideoMetaData(String videoPathStr) {
        File videoFile = new File(videoPathStr);
        if (!videoFile.exists()) {
            throw new IllegalArgumentException("视频文件不存在: " + videoPathStr);
        }

        VideoMetaData videoMetaData = new VideoMetaData();
        videoMetaData.setFileSizeBytes(videoFile.length());

        // 使用FFMpegFrameGrabber获取视频信息
        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(videoFile)) {
            grabber.start();

            long durationInMircoSeconds = grabber.getLengthInTime();
            videoMetaData.setDurationSec(durationInMircoSeconds / 1000000); // 转换为秒

            int width = grabber.getImageWidth();
            int height = grabber.getImageHeight();
            videoMetaData.setResolution(width + "x" + height);

            // 计算视频码率（bps）
            if (videoMetaData.getDurationSec() > 0) {
                double bitrate = (videoMetaData.getFileSizeBytes() * 8.0) / videoMetaData.getDurationSec();
                videoMetaData.setBitrate((long) bitrate);
            } else {
                videoMetaData.setBitrate(0L);
            }

            grabber.stop();
        } catch (Exception e) {
            logger.error("分析视频元数据失败，视频路径: {}, 错误信息: {}", videoPathStr, e.getMessage());
            throw new SystemException(ResultCode.VIDEO_META_DATA_ANALYSIS_FAILED);
        }

        return videoMetaData;
    }

    /**
     * 将视频切片成HLS格式
     * @param sourceVideoPathStr        原视频的绝对路径
     * @param alarmNo                   报警号
     * @param studentNo                 学号
     */
    private void splitVideoIntoChunksHls(String sourceVideoPathStr, String alarmNo, String studentNo) {
        // 校验原视频
        File sourceFile = new File(sourceVideoPathStr);
        if (!sourceFile.exists() || !sourceFile.isFile()) {
            logger.error("原视频文件不存在或不是文件，sourceVideoPathStr: {}", sourceVideoPathStr);
            throw new SystemException(ResultCode.VIDEO_NOT_EXISTS);
        }

        // 检查alarmNo/studentNo
        if (!alarmNo.matches("\\w+")) {
            logger.error("alarmNo不合规: {}", alarmNo);
            throw new SystemException(ResultCode.PARAMETER_ERROR);
        }

        // 构建输出目录 在源视频目录下创建文件夹用于保存ts和m3u8文件
        File parentDir = sourceFile.getParentFile();
        String sliceDirName = alarmNo + "_slices";
        File outputDir = new File(parentDir, sliceDirName);
        if (!outputDir.exists()) {
            boolean created = outputDir.mkdirs();
            if (!created) {
                logger.error("创建HLS切片输出目录失败, outputDir: {}", outputDir);
                throw new SystemException(ResultCode.DIRECTORY_CREATION_FAILED);
            }
        }

        // 初始化视频读取器
        double frameRate; long durationMicro; int totalSlices;
        try(FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(sourceFile)){
            grabber.start();

            frameRate = grabber.getFrameRate();
            durationMicro = grabber.getLengthInTime();
            totalSlices = (int) Math.ceil(TimeUnit.MICROSECONDS.toSeconds(durationMicro) * 1.0 / SLICE_DURATION_SECONDS);
            logger.info("预计会产生{}个视频切片文件", totalSlices);
        } catch (Exception e) {
            logger.error("分析视频元数据失败，视频路径: {}", sourceVideoPathStr, e);
            throw new SystemException(ResultCode.VIDEO_META_DATA_ANALYSIS_FAILED);
        }

        // 格式化序号为AlarmNo + 五位数 形式搭配
        DecimalFormat formatter = new DecimalFormat("00000");

        // M3U8内容生成器
        StringBuilder m3u8Builder = new StringBuilder();
        m3u8Builder.append("#EXTM3U\n");
        m3u8Builder.append("#EXT-X-VERSION:3\n");
        m3u8Builder.append("#EXT-X-TARGETDURATION:").append(SLICE_DURATION_SECONDS).append("\n");
        m3u8Builder.append("#EXT-X-MEDIA-SEQUENCE:0\n");

        Frame frame;
        int currentSliceIndex = 0;
        long sliceStartTime = 0;

        FFmpegFrameRecorder recorder = null;
        FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(sourceFile);

        try {
            grabber.start();
        } catch (Exception e) {
            logger.error("视频解码器启动失败", e);
            throw new SystemException(ResultCode.VIDEO_DECODER_START_FAILED);
        }

        try {
            while ((frame = grabber.grabFrame()) != null) {
                long currentTime = grabber.getTimestamp();

                // 切换到新切片
                if (recorder == null || (currentTime - sliceStartTime) > TimeUnit.SECONDS.toMicros(SLICE_DURATION_SECONDS)) {
                    // 关闭旧的切片
                    try {
                        if (recorder != null) {
                            recorder.stop();
                            recorder.release();
                        }
                    } catch (Exception e) {
                        logger.warn("关闭旧切片失败: {}", e.getMessage());
                    }

                    // 创建新切片
                    String tsFileName = alarmNo + formatter.format(currentSliceIndex + 1) + ".ts";
                    File tsFile = new File(outputDir, tsFileName);

                    try {
                        recorder = new FFmpegFrameRecorder(tsFile, grabber.getImageWidth(),
                                grabber.getImageHeight(), grabber.getAudioChannels());
                        recorder.setFormat("mpegts");
                        recorder.setVideoCodec(grabber.getVideoCodec());
                        recorder.setAudioCodec(grabber.getAudioCodec());
                        recorder.setFrameRate(frameRate);
                        recorder.start();
                    } catch (Exception e) {
                        logger.error("启动切片写入器失败: {}", e.getMessage(), e);
                        throw new SystemException(ResultCode.VIDEO_SEGMENT_CREATE_FAILED);
                    }

                    // 写 m3u8 索引
                    m3u8Builder.append("#EXTINF:").append(SLICE_DURATION_SECONDS).append(",\n");
                    m3u8Builder.append(tsFileName).append("\n");
                    m3u8Builder.append("#EXT-X-ENDLIST\n");

                    currentSliceIndex++;
                    sliceStartTime = currentTime;
                }

                try {
                    recorder.record(frame);
                } catch (Exception e) {
                    logger.error("写入帧失败: {}", e.getMessage(), e);
                    throw new SystemException(ResultCode.VIDEO_FRAME_RECORD_FAILED);
                }
            }
        } catch (Exception e) {
            logger.error("视频读取或切片过程失败: {}", e.getMessage(), e);
            throw new SystemException(ResultCode.VIDEO_SPLICE_FAILED);

        } finally {
            try {
                if (recorder != null) {
                    recorder.stop();
                    recorder.release();
                }
                grabber.stop();
                grabber.release();
            } catch (Exception e) {
                logger.warn("资源释放失败: {}", e.getMessage());
            }
        }

        // 写m3u8文件
        File m3u8File = new File(outputDir, alarmNo + ".m3u8");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(m3u8File))) {
            m3u8Builder.append("#EXT-X-ENDLIST\n");
            writer.write(m3u8Builder.toString());
        } catch (IOException e) {
            logger.error("写入 m3u8 文件失败: {}", e.getMessage(), e);
            throw new SystemException(ResultCode.M3U8_WRITE_FAILED);
        }

        logger.info("HLS切片处理完成，切片目录: {}, m3u8文件: {}",
                outputDir.getAbsolutePath(), new File(outputDir, alarmNo + ".m3u8").getAbsolutePath());
    }

    /**
     * 保存切片视频信息到数据库 (此处每个列表的后续时间需要进行处理)
     * @param alarmNo           报警号
     * @param studentNo         学号
     * @param outputFileDir     切片视频输出的目录（此处是指mp4格式的视频）
     * @param videoMetaData     源视频元数据对象
     * @return                  切片信息列表（未设置每个切片对象的fileSizeBytes属性）
     */
    private List<AlarmVideoSlice> saveSlicedVideoInfo(String alarmNo, String studentNo, String outputFileDir, VideoMetaData videoMetaData) {

        //todo
        return buildVideoSliceList(alarmNo, videoMetaData, outputFileDir);
    }

    /**
     * 生成切片视频信息列表
     * @param alarmNo           报警号
     * @param videoMetaData     视频元数据对象
     * @param outputPathStr     切片视频输出的绝对路径
     * @return                  切片信息列表
     */
    private List<AlarmVideoSlice> buildVideoSliceList(String alarmNo, VideoMetaData videoMetaData, String outputPathStr) {
        List<AlarmVideoSlice> sliceList = new ArrayList<>();
        Long durationSec = videoMetaData.getDurationSec();
        long fullSliceCount = durationSec / SLICE_DURATION_SECONDS;
        long remainder = durationSec % SLICE_DURATION_SECONDS;

        long startTimeSec = 0L;
        Date now = new Date();

        // 添加完整切片
        int count = 1;
        for (int i = 0; i < fullSliceCount; i++) { //处理第一个到倒数第二个的切片对象
            String sliceId = alarmNo + String.format("%05d", count++);

            AlarmVideoSlice slice = AlarmVideoSlice.builder()
                    .videoId(videoMetaData.getVideoId())
                    .sliceId(sliceId)
                    .startTimeSec(startTimeSec)
                    .durationSec(SLICE_DURATION_SECONDS)
                    .sliceFilePath(outputPathStr)
                    .fileSizeBytes(0L) // todo 这里后续需要进行处理
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            sliceList.add(slice);

            startTimeSec += SLICE_DURATION_SECONDS;
        }

        // 处理最后一个切片对象
        if (remainder > 0) {
            String sliceId = alarmNo + String.format("%05d", count);

            AlarmVideoSlice slice = AlarmVideoSlice.builder()
                    .videoId(videoMetaData.getVideoId())
                    .sliceId(sliceId)
                    .startTimeSec(startTimeSec)
                    .durationSec(remainder)
                    .sliceFilePath(outputPathStr)
                    .fileSizeBytes(0L) // todo 这里后续需要进行处理
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            sliceList.add(slice);
        }

        return sliceList;
    }

    /**
     * 删除已经存在的ts视频切片和m3u8文件
     * @param alarmNo               报警号
     * @param videoParentPathStr    切片所在目录
     */
    private void deleteTsAndM3u8File(String alarmNo, String videoParentPathStr) {
        File directory = new File(videoParentPathStr);
        if (!directory.exists() || !directory.isDirectory()) {
            logger.info("不存在该目录, videoParentPathStr:{}", videoParentPathStr);
            return;
        }

        File[] filesToDelete = directory.listFiles(file -> {
            String fileName = file.getName();
            return file.isFile() && fileName.startsWith(alarmNo) &&
                    fileName.endsWith(".ts") || fileName.endsWith(".ts");
        });

        if (filesToDelete != null) {
            for (File file : filesToDelete) {
                boolean deleted = file.delete();
                if (!deleted) {
                    logger.error("该文件删除失败：{}", file.getAbsolutePath());
                }
            }
        }
    }

    /**
     * 更新切片列表中的文件大小属性
     * @param alarmVideoSliceList           切片视频信息列表
     * @param outputFileDirStr              切片视频输出的目录
     * @param alarmNo                       报警号
     */
    private void updateFileSizeBytes(List<AlarmVideoSlice> alarmVideoSliceList,
                                     String outputFileDirStr,
                                     String alarmNo) {
        if (alarmVideoSliceList == null || alarmVideoSliceList.isEmpty()) {
            logger.warn("切片视频信息列表为空");
            return;
        }

        for (AlarmVideoSlice slice : alarmVideoSliceList) {
            String fileName = slice.getSliceId() + ".ts";
            File file = new File(outputFileDirStr, fileName);
            if (file.exists() && file.isFile()) {
                slice.setFileSizeBytes(file.length());
            } else {
                logger.error("该切片视频文件不存在或不是文件, file: {}, alarmNo: {}", file.getAbsolutePath(), alarmNo);
            }
        }
    }
}
