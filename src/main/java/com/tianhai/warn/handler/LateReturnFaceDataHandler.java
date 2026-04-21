package com.tianhai.warn.handler;

import com.tianhai.warn.dto.LateReturnFaceDTO;
import com.tianhai.warn.enums.ResultCode;
import com.tianhai.warn.exception.BusinessException;
import com.tianhai.warn.exception.SystemException;
import com.tianhai.warn.model.LateReturn;
import com.tianhai.warn.service.LateReturnService;
import com.tianhai.warn.utils.LateReturnIdGenerator;
import com.xxl.job.core.handler.IJobHandler;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import com.xxl.job.core.biz.model.ReturnT;


@Component
public class LateReturnFaceDataHandler extends IJobHandler{

    private static final Logger logger = LoggerFactory.getLogger(LateReturnFaceDataHandler.class);

    @Autowired
    private WebClient webClient;

    @Autowired
    private Environment environment;

    @Autowired
    private LateReturnService lateReturnService;

    @Override
    public void execute() throws Exception {
        String lateStartTime = environment.getProperty("warn.face.late.start.time");
        String lateEndTime = environment.getProperty("warn.face.late.end.time");

        fetchSaveLateReturnData(lateStartTime, lateEndTime);
    }

    // 从云端获取学生晚归的人脸数据并保存在数据库
    public void fetchSaveLateReturnData(String lateStartTime, String lateEndTime) {
        // 从云端获取数据
        String url = environment.getProperty("warn.face.cloud.url");
        String secret = environment.getProperty("warn.face.cloud.secret");
        String responseTimeoutStr = environment.getProperty("warn.face.cloud.response.timeout", "10");
        long responseTimeout = Long.parseLong(responseTimeoutStr);

        if (StringUtils.isBlank(lateStartTime) || StringUtils.isBlank(lateEndTime)) {
            logger.error("晚归时间范围不能为空");
            throw new BusinessException(ResultCode.VALIDATE_FAILED);
        }

        if (StringUtils.isBlank(url)) {
            logger.error("未配置云端人脸数据获取URL或Token");
            throw new BusinessException(ResultCode.VALIDATE_FAILED);
        }
        String faceDataReqUrl = String.format(url, lateStartTime, lateEndTime);

        List<LateReturnFaceDTO> lateReturnFaceDTOList;
        try {
            lateReturnFaceDTOList = webClient.get()
                    .uri(faceDataReqUrl)
                    .header("face-api-secret", secret)
                    .retrieve()
                    .bodyToFlux(LateReturnFaceDTO.class)
                    .collectList()
                    .block(Duration.ofSeconds(responseTimeout));
        } catch (Exception e) {
            logger.error("从云端获取晚归人脸数据失败, lateStartTime: {}, lateEndTime: {}", lateStartTime, lateEndTime, e);
            throw new SystemException(ResultCode.ERROR);
        }

        // 将晚归人脸数据保存在late_return表
        if (lateReturnFaceDTOList == null || lateReturnFaceDTOList.isEmpty()) {
            logger.info("没有获取到晚归人脸数据");
            return;
        } else {
            logger.info("获取到晚归人脸数据，数量: {}", lateReturnFaceDTOList.size());
        }

        int saveLateReturnRows = saveLateReturnData(lateReturnFaceDTOList);
        logger.info("成功保存晚归人脸数据到数据库，保存行数: {}", saveLateReturnRows);
    }

    // 保存晚归人脸数据到数据库
    private int saveLateReturnData(List<LateReturnFaceDTO> lateReturnFaceDTOList) {
        Date now = new Date();
        List<LateReturn> lateReturnList = lateReturnFaceDTOList.stream()
                .map(lateReturnFaceDTO -> LateReturn.builder()
                       .lateReturnId(LateReturnIdGenerator.generate())
                       .studentNo(lateReturnFaceDTO.getStudentNo())
                       .studentName(lateReturnFaceDTO.getName())
                       .lateTime(lateReturnFaceDTO.getLateTime())
                       .createTime(now)
                       .updateTime(now)
                       .build())
                .toList();

        // 批量插入晚归记录
        return lateReturnService.insertBatch(lateReturnList);
    }
}
