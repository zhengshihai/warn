package com.tianhai.warn.service;

import com.tianhai.warn.model.Student;
import com.tianhai.warn.utils.Result;
import java.util.List;

/**
 * 学生信息服务接口
 */
public interface StudentService {

    /**
     * 根据主键ID查询学生信息
     * 
     * @param id 学生ID
     * @return 学生信息
     */
    Student selectById(Integer id);

    /**
     * 根据学号查询学生信息
     * 
     * @param studentNo 学号
     * @return 学生信息
     */
    Student selectByStudentNo(String studentNo);

    /**
     * 查询所有学生信息
     * 
     * @return 学生信息列表
     */
    List<Student> selectAll();

    /**
     * 根据条件查询学生信息
     * 
     * @param student 查询条件
     * @return 学生信息列表
     */
    List<Student> selectByCondition(Student student);

    /**
     * 插入学生信息
     * 
     * @param student 学生信息
     * @return 影响行数
     */
    int insert(Student student);

    /**
     * 更新学生信息
     * 
     * @param student 学生信息
     * @return 影响行数
     */
    int update(Student student);

    /**
     * 更新学生个人信息
     * 
     * @param student      学生信息
     * @param currentEmail 当前邮箱
     * @return 更新结果
     */
    Result<?> updatePersonalInfo(Student student, String currentEmail);

    /**
     * 删除学生信息
     * 
     * @param id 学生ID
     * @return 影响行数
     */
    int deleteById(Integer id);

    /**
     * 根据宿舍号查询学生列表
     * 
     * @param dormitory 宿舍号
     * @return 学生信息列表
     */
    List<Student> selectByDormitory(String dormitory);

    /**
     * 根据班级查询学生列表
     * 
     * @param className 班级名称
     * @return 学生信息列表
     */
    List<Student> selectByClassName(String className);

    /**
     * 根据邮箱获取学生信息
     */
    Student getStudentByEmail(String email);

    /**
     * 更新最后的登录时间
     * @param id  主键
     */
    void updateLastLoginTime(Integer id);
}