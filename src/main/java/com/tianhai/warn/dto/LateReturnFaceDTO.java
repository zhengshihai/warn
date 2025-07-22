package com.tianhai.warn.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LateReturnFaceDTO {
    private String studentNo;

    private String name;

    private Date lateTime;

    private String photoUrl; // 人脸照片地址
}
