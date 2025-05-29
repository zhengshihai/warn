package com.tianhai.warn.utils;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalTime;

@Data
@AllArgsConstructor
public class TimeRange {

    private String label; // 时间段标签 "23:00 - 01:30"

    private LocalTime start;

    private LocalTime end;

    public boolean contains(LocalTime time) {
        if (end.isAfter(start) || end.equals(start)) {
            // 处理非跨天时间段 例如 23:00-23:50
            return !time.isBefore(start) && time.isBefore(end);
        } else {
            // 处理跨天时间段 例如 23:00-01:30
            return !time.isBefore(start) || time.isBefore(end);
        }
    }
}
