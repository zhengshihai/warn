package com.tianhai.warn.vo;

import com.tianhai.warn.model.Notification;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
public class NotificationVO extends Notification {
    private String readStatus;

    @Override
    public String toString() {
        return super.toString() +
                ", NotificationVO{" +
                "readStatus='" + readStatus + '\'' +
                '}';
    }
}
