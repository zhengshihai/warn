package com.tianhai.warn.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 晚归记录实体类
 */
@Data
public class LateReturn implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id; // 主键ID

    /**
     * 晚归记录ID
     * 格式：LR + 年月日 + 6位随机数
     * 示例：LR20240321000123
     */
    private String lateReturnId;

    private String studentNo; // 学号
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lateTime; // 晚归时间
    private String reason; // 晚归原因
    private String processStatus; // 处理状态：PENDING-待处理/PROCESSING-处理中/FINISHED-已完成
    private String processResult; // 处理结果：APPROVED-已通过/REJECTED-已驳回
    private String processRemark; // 处理备注
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime; // 创建时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime; // 更新时间

    // 非数据库字段
    private String studentName; // 学生姓名
    private String college; // 学院
    private String dormitory; // 宿舍号

    @Override
    public String toString() {
        return "LateReturn{" +
                "id=" + id +
                ", studentNo='" + studentNo + '\'' +
                ", lateTime=" + lateTime +
                ", reason='" + reason + '\'' +
                ", processStatus='" + processStatus + '\'' +
                ", processResult='" + processResult + '\'' +
                ", processRemark='" + processRemark + '\'' +
                ", createTime=" + createTime +
                ", updateTime=" + updateTime +
                ", studentName='" + studentName + '\'' +
                ", college='" + college + '\'' +
                ", dormitory='" + dormitory + '\'' +
                '}';
    }
}