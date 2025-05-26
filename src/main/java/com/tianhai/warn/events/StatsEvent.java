package com.tianhai.warn.events;

import com.tianhai.warn.dto.AuditActionDTO;
import com.tianhai.warn.dto.ProcessActionDTO;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 统计事件
 */
@Getter
public class StatsEvent extends ApplicationEvent {
    private final AuditActionDTO auditActionDTO;
    private final ProcessActionDTO processActionDTO;

    /**
     * 使用审核晚归说明材料动作创建事件
     */
    public StatsEvent(Object source, AuditActionDTO auditActionDTO) {
        super(source);
        this.auditActionDTO = auditActionDTO;
        this.processActionDTO = null;
    }

    /**
     * 使用更新晚归记录处理结果动作创建事件
     */
    public StatsEvent(Object source, ProcessActionDTO processActionDTO) {
        super(source);
        this.auditActionDTO = null;
        this.processActionDTO = processActionDTO;
    }
}
