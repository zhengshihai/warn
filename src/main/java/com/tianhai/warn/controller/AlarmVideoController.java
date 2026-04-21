package com.tianhai.warn.controller;

import com.tianhai.warn.annotation.LogOperation;
import com.tianhai.warn.annotation.RequirePermission;
import com.tianhai.warn.constants.Constants;
import com.tianhai.warn.enums.ResultCode;
import com.tianhai.warn.exception.BusinessException;
import com.tianhai.warn.model.AlarmVideo;
import com.tianhai.warn.model.AlarmVideoSlice;
import com.tianhai.warn.service.AlarmVideoService;
import com.tianhai.warn.service.AlarmVideoSliceService;
import com.tianhai.warn.utils.PageResult;
import com.tianhai.warn.utils.PageUtils;
import com.tianhai.warn.utils.Result;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 报警视频管理控制器（用于院长端视频查看）
 */
@Controller
@RequestMapping("/alarm-video")
public class AlarmVideoController {

    private static final Logger logger = LoggerFactory.getLogger(AlarmVideoController.class);

    @Autowired
    private AlarmVideoService alarmVideoService;

    @Autowired
    private AlarmVideoSliceService alarmVideoSliceService;

    @Value("${file.upload.base-path}")
    private String basePath;

    /**
     * 分页查询报警视频列表
     */
    @PostMapping("/page")
    @ResponseBody
    @RequirePermission(roles = { Constants.SYSTEM_USER, Constants.SUPER_ADMIN })
    @LogOperation("分页查询报警视频列表")
    public Result<PageResult<AlarmVideo>> page(@RequestBody AlarmVideoPageRequest request) {
        if (request == null) {
            logger.error("报警视频分页查询参数为空");
            throw new BusinessException(ResultCode.PARAMETER_ERROR);
        }

        Integer pageNum = request.getPageNum();
        Integer pageSize = request.getPageSize();

        if (pageNum == null || pageNum <= 0) {
            pageNum = Constants.DEFAULT_PAGE_NUM;
        }
        if (pageSize == null || pageSize <= 0 || pageSize > Constants.DEFAULT_PAGE_SIZE_MAX) {
            pageSize = Constants.DEFAULT_PAGE_SIZE;
        }

        List<AlarmVideo> allList = alarmVideoService.getAllAlarmVideos();

        // 按学号 / 报警编号进行简单过滤（均为可选条件，支持模糊匹配）
        String studentNo = StringUtils.trimToEmpty(request.getStudentNo());
        String alarmNo = StringUtils.trimToEmpty(request.getAlarmNo());

        if (StringUtils.isNotBlank(studentNo) || StringUtils.isNotBlank(alarmNo)) {
            allList = allList.stream().filter(v -> {
                boolean matchStudent = true;
                boolean matchAlarm = true;
                if (StringUtils.isNotBlank(studentNo)) {
                    matchStudent = v.getStudentNo() != null && v.getStudentNo().contains(studentNo);
                }
                if (StringUtils.isNotBlank(alarmNo)) {
                    matchAlarm = v.getAlarmNo() != null && v.getAlarmNo().contains(alarmNo);
                }
                return matchStudent && matchAlarm;
            }).toList();
        }

        PageResult<AlarmVideo> pageResult = PageUtils.buildPageResult(allList, pageNum, pageSize);

        return Result.success(pageResult);
    }

    /**
     * 根据 videoId 获取可播放视频的预览 URL
     */
    @GetMapping("/play-url")
    @ResponseBody
    @RequirePermission(roles = { Constants.SYSTEM_USER, Constants.SUPER_ADMIN })
    @LogOperation("获取报警视频播放URL")
    public Result<Map<String, String>> getPlayUrl(@RequestParam("videoId") String videoId) {
        if (StringUtils.isBlank(videoId)) {
            logger.error("videoId 不能为空");
            throw new BusinessException(ResultCode.PARAMETER_ERROR);
        }

        // 根据 videoId 查询一条切片记录（任意一条即可）
        List<AlarmVideoSlice> sliceList = alarmVideoSliceService.getAlarmVideoSlicesByVideoId(videoId);
        if (sliceList == null || sliceList.isEmpty()) {
            logger.error("未找到该视频的切片信息, videoId: {}", videoId);
            throw new BusinessException(ResultCode.VIDEO_NOT_EXISTS);
        }

        AlarmVideoSlice slice = sliceList.get(0);
        String sliceDirPath = slice.getSliceFilePath();
        if (StringUtils.isBlank(sliceDirPath)) {
            logger.error("切片目录路径为空, videoId: {}", videoId);
            throw new BusinessException(ResultCode.VIDEO_NOT_EXISTS);
        }

        File dir = new File(sliceDirPath);
        if (!dir.exists() || !dir.isDirectory()) {
            logger.error("切片目录不存在或不是目录, path: {}", sliceDirPath);
            throw new BusinessException(ResultCode.VIDEO_NOT_EXISTS);
        }

        // 在目录下查找一个 mp4 视频文件（支持 .mp4 或 .webm.mp4 等）
        File[] candidates = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".mp4"));
        if (candidates == null || candidates.length == 0) {
            logger.error("切片目录下未找到 mp4 视频文件, path: {}", sliceDirPath);
            throw new BusinessException(ResultCode.VIDEO_NOT_EXISTS);
        }

        File videoFile = candidates[0];
        String absolutePath = videoFile.getAbsolutePath();

        // 将绝对路径转换为相对于 basePath 的相对路径
        String relativePath = buildRelativePath(absolutePath);
        // 构造 /file/preview/** 形式的预览URL（前端再拼接 contextPath）
        String previewUrl = "/file/preview/" + relativePath.replace(File.separatorChar, '/');

        Map<String, String> data = new HashMap<>();
        data.put("previewUrl", previewUrl);
        data.put("videoId", videoId);

        return Result.success(data);
    }

    /**
     * 根据 videoId 删除报警视频记录
     */
    @DeleteMapping("/delete")
    @ResponseBody
    @RequirePermission(roles = { Constants.SYSTEM_USER, Constants.SUPER_ADMIN })
    @LogOperation("删除报警视频记录")
    public Result<?> deleteByVideoId(@RequestParam("videoId") String videoId) {
        if (StringUtils.isBlank(videoId)) {
            logger.error("videoId 不能为空");
            throw new BusinessException(ResultCode.PARAMETER_ERROR);
        }

        List<AlarmVideo> allList = alarmVideoService.getAllAlarmVideos();
        AlarmVideo target = allList.stream()
                .filter(v -> videoId.equals(v.getVideoId()))
                .findFirst()
                .orElse(null);

        if (target == null || target.getId() == null) {
            logger.error("未找到对应的报警视频记录, videoId: {}", videoId);
            throw new BusinessException(ResultCode.VIDEO_NOT_EXISTS);
        }

        alarmVideoService.deleteAlarmVideoById(target.getId());

        return Result.success();
    }

    /**
     * 从绝对路径构造相对于 basePath 的相对路径
     */
    private String buildRelativePath(String absolutePath) {
        if (StringUtils.isBlank(absolutePath)) {
            throw new BusinessException(ResultCode.PARAMETER_ERROR);
        }
        String normalizedBasePath = new File(basePath).getAbsolutePath();
        String normalizedAbsPath = new File(absolutePath).getAbsolutePath();

        if (!normalizedAbsPath.startsWith(normalizedBasePath)) {
            logger.error("视频文件不在受管控的basePath目录下, basePath: {}, file: {}", normalizedBasePath, normalizedAbsPath);
            throw new BusinessException(ResultCode.FILE_NOT_EXISTS);
        }

        String relative = normalizedAbsPath.substring(normalizedBasePath.length());
        // 去掉开头的路径分隔符
        relative = relative.replaceFirst("^[\\\\/]+", "");
        return relative;
    }

    /**
     * 报警视频分页请求体
     */
    public static class AlarmVideoPageRequest {
        private Integer pageNum;
        private Integer pageSize;
        /**
         * 按学号筛选（可选）
         */
        private String studentNo;
        /**
         * 按报警编号筛选（可选）
         */
        private String alarmNo;

        public Integer getPageNum() {
            return pageNum;
        }

        public void setPageNum(Integer pageNum) {
            this.pageNum = pageNum;
        }

        public Integer getPageSize() {
            return pageSize;
        }

        public void setPageSize(Integer pageSize) {
            this.pageSize = pageSize;
        }

        public String getStudentNo() {
            return studentNo;
        }

        public void setStudentNo(String studentNo) {
            this.studentNo = studentNo;
        }

        public String getAlarmNo() {
            return alarmNo;
        }

        public void setAlarmNo(String alarmNo) {
            this.alarmNo = alarmNo;
        }
    }
}
