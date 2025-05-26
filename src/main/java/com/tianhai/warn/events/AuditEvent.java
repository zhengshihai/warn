package com.tianhai.warn.events;

import com.tianhai.warn.dto.AuditActionDTO;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 审核事件定义
 */
@Getter
public class AuditEvent extends ApplicationEvent {
    private final AuditActionDTO auditActionDTO;

    public AuditEvent(Object source, AuditActionDTO auditActionDTO) {
        super(source);
        this.auditActionDTO = auditActionDTO;
    }


}
