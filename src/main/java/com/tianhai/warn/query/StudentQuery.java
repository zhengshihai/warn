package com.tianhai.warn.query;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class StudentQuery extends BaseQuery{
    private Integer id; // 主键ID
    private String studentNo; // 学号
    private String name; // 姓名
    private String college; // 学院
    private String className; // 班级
    private String dormitory; // 宿舍号
    private String phone; // 联系电话
    private String email; // 电子邮箱
    private String fatherName; // 父亲姓名
    private String fatherPhone; // 父亲电话
    private String motherName; // 母亲姓名
    private String motherPhone; // 母亲电话

    private String nameLike; // 姓名模糊查询

    private List<String> studentNos; // 批量学号查询
    private List<String> colleges; // 批量学院查询
    private List<String> classesNames; // 批量班级查询
    private List<String> dormitories; // 批量宿舍查询

}
