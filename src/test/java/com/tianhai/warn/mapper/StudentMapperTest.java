package com.tianhai.warn.mapper;

import com.tianhai.warn.model.Student;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.annotation.Rollback;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:applicationContext-test.xml" })
@Transactional
public class StudentMapperTest {

    @Autowired
    private StudentMapper studentMapper;

    @Test
    @Rollback(false)
    public void testInsert() {
        // 创建测试数据
        Student student = new Student();
        student.setStudentNo("2420710220TEST");
        student.setName("测试学生");
        student.setCollege("测试学院");
        student.setClassName("测试班级");
        student.setDormitory("测试宿舍");
        student.setPhone("13800138000");
        student.setEmail("test@example.com");
        student.setPassword("123456");
        student.setFatherName("测试父亲");
        student.setFatherPhone("13900139000");
        student.setMotherName("测试母亲");
        student.setMotherPhone("13700137000");

        // 执行插入
        int result = studentMapper.insert(student);

        // 验证插入结果
        assertTrue("插入应该成功", result > 0);
        assertNotNull("ID应该被自动生成", student.getId());

        // 验证数据是否正确插入
        Student insertedStudent = studentMapper.selectByStudentNo("2420710220TEST");
        assertNotNull("应该能查询到插入的学生", insertedStudent);
        assertEquals("学号应该匹配", "2420710220TEST", insertedStudent.getStudentNo());
        assertEquals("姓名应该匹配", "测试学生", insertedStudent.getName());
        assertEquals("学院应该匹配", "测试学院", insertedStudent.getCollege());
        assertEquals("班级应该匹配", "测试班级", insertedStudent.getClassName());
        assertEquals("宿舍应该匹配", "测试宿舍", insertedStudent.getDormitory());
        assertEquals("电话应该匹配", "13800138000", insertedStudent.getPhone());
        assertEquals("邮箱应该匹配", "test@example.com", insertedStudent.getEmail());
        assertEquals("密码应该匹配", "123456", insertedStudent.getPassword());
        assertEquals("父亲姓名应该匹配", "测试父亲", insertedStudent.getFatherName());
        assertEquals("父亲电话应该匹配", "13900139000", insertedStudent.getFatherPhone());
        assertEquals("母亲姓名应该匹配", "测试母亲", insertedStudent.getMotherName());
        assertEquals("母亲电话应该匹配", "13700137000", insertedStudent.getMotherPhone());
    }

    @Test(expected = Exception.class)
    public void testInsertDuplicateStudentNo() {
        // 第一次插入
        Student student1 = new Student();
        student1.setStudentNo("2420710220TEST");
        student1.setName("测试学生1");
        student1.setCollege("测试学院");
        student1.setClassName("测试班级");
        student1.setDormitory("测试宿舍");
        student1.setEmail("test1@example.com");
        student1.setPassword("123456");
        studentMapper.insert(student1);

        // 尝试插入相同学号的学生
        Student student2 = new Student();
        student2.setStudentNo("2420710220TEST");
        student2.setName("测试学生2");
        student2.setCollege("测试学院");
        student2.setClassName("测试班级");
        student2.setDormitory("测试宿舍");
        student2.setEmail("test2@example.com");
        student2.setPassword("123456");
        studentMapper.insert(student2);
    }

    @Test
    public void testUpdate() {
        // 1. 先查询出要更新的学生
        Student student = studentMapper.selectByStudentNo("2420710220XSTEST");
        assertNotNull("测试学生数据不存在", student);

        // 2. 保存原始数据用于后续验证
        String originalName = student.getName();
        String originalCollege = student.getCollege();
        String originalClassName = student.getClassName();

        // 3. 准备更新数据
        student.setName("测试更新姓名");
        student.setCollege("测试更新学院");
        student.setClassName("测试更新班级");

        // 4. 执行更新
        int result = studentMapper.update(student);
        assertEquals("更新应该返回1", 1, result);

        // 5. 重新查询验证更新结果
        Student updatedStudent = studentMapper.selectByStudentNo("2420710220XSTEST");
        assertNotNull("更新后查询结果不应为空", updatedStudent);
        assertEquals("姓名应该被更新", "测试更新姓名", updatedStudent.getName());
        assertEquals("学院应该被更新", "测试更新学院", updatedStudent.getCollege());
        assertEquals("班级应该被更新", "测试更新班级", updatedStudent.getClassName());

        // 6. 恢复原始数据
        student.setName(originalName);
        student.setCollege(originalCollege);
        student.setClassName(originalClassName);
        studentMapper.update(student);
    }
}