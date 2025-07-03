package com.tianhai.warn.mapper;

import com.tianhai.warn.model.Student;
import com.tianhai.warn.query.StudentQuery;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * 学生信息Mapper接口
 */
public interface StudentMapper {

    /**
     * 根据ID查询学生信息
     * 
     * @param id 学生ID
     * @return 学生信息
     */
    Student selectById(Integer id);

    /**
     * 根据邮箱查询学生信息
     * 
     * @param email 邮箱
     * @return 学生信息
     */
    Student selectByEmail(String email);

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
     *
     * 
     * @param className 班级名称
     * @return 学生信息列表
     */
    List<Student> selectByClassName(String className);

    // 根据班级名称列表更新学生班级信息 可能有多个班级名被统一改为新的班级名
    int updateStudentClassByClassNames(@Param("classNames") List<String> classNames,
            @Param("newClassName") String newClassName);

    // 根据单个班级名称更新学生班级信息
    int updateStudentClassByClassName(@Param("className") String className,
            @Param("newClassName") String newClassName);

    /**
     * 根据查询条件搜索学生信息（支持批量查询）
     * 
     * @param studentQuery 查询条件
     * @return 学生信息列表
     */
    List<Student> searchByStudentQuery(StudentQuery studentQuery);

    void updateLastLoginTime(Integer id);

    /**
     * 删除学生信息
     * @param distinctIds   去重的学生id列表
     * @return              删除行数
     */
    int deleteByIds(@Param("distinctIds")List<Integer> distinctIds);

    /**
     * 查询所有学生邮箱
     * @return          邮箱集合
     */
    Set<String> selectAllEmail();

    /**
     * 查询所有学生学号
     * @return          学号集合
     */
    Set<String> selectAllStudentNo();
}