package com.tianhai.warn.mq;

import com.tianhai.warn.dto.LocationUpdateDTO;
import lombok.Data;
import org.w3c.dom.stylesheets.LinkStyle;

import java.util.List;
import java.util.Map;

/**
 * 报警上下文
 */
@Data
public class AlarmContext {
    private String studentNo;

    private List<String> mediaUrls;

    private LocationUpdateDTO locationUpdateDTO;

    private Map<String, Object> extraInfo; //存储额外信息
}
