package com.tianhai.warn.vo;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LatestLocationVO {
    private Double latitude;

    private Double longitude;

    private Double locationAccuracy;
}
