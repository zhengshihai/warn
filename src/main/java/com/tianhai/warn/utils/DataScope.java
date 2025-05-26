package com.tianhai.warn.utils;

import java.util.List;

import org.eclipse.tags.shaded.org.apache.regexp.recompile;

import com.jhlabs.math.SCNoise;

import lombok.Data;

@Data
public class DataScope {
    private String studentNo;// 学生学号

    private List<String> dormitories; // 宿管管理的宿舍列表

    private List<String> classes;

    //学生数据范围
    public static DataScope forStudent(String studentNo) {
        DataScope scope = new DataScope();
        scope.setStudentNo(studentNo);

        return scope;
    }

    //宿管数据范围
    public static DataScope forDormitoryManager(List<String> dormitories) {
        DataScope scope = new DataScope();
        scope.setDormitories(dormitories);
        return scope;
    }

    //辅导员/班主任/院级领导管理的数据范围
    public static DataScope forOthers(List<String> classes) {
        DataScope scope = new DataScope();
        scope.setClasses(classes);
        
        return scope;
    }
}
