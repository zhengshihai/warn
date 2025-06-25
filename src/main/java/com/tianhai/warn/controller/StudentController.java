package com.tianhai.warn.controller;

import com.tianhai.warn.annotation.LogOperation;
import com.tianhai.warn.annotation.RequirePermission;
import com.tianhai.warn.constants.Constants;
import com.tianhai.warn.enums.ResultCode;
import com.tianhai.warn.exception.BusinessException;
import com.tianhai.warn.model.Student;
import com.tianhai.warn.query.StudentQuery;
import com.tianhai.warn.service.StudentService;
import com.tianhai.warn.utils.EmailValidator;
import com.tianhai.warn.utils.PageResult;
import com.tianhai.warn.utils.Result;
import com.tianhai.warn.utils.SessionUtils;
import jakarta.servlet.http.HttpSession;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.session.Session;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;

import javax.swing.plaf.basic.BasicButtonUI;
import java.util.ArrayList;
import java.util.List;

/**
 * 学生信息控制器
 */
@Controller
@RequestMapping("/student")
public class StudentController {
    private static final Logger logger = LoggerFactory.getLogger(StudentController.class);

    @Autowired
    private StudentService studentService;

    @GetMapping
    public String student(HttpSession session, Model model) {
        Object user = session.getAttribute(Constants.SESSION_ATTRIBUTE_USER);
        if (user instanceof Student student) {
            model.addAttribute("name", student.getName());
            model.addAttribute("email", student.getEmail());
            model.addAttribute("role", session.getAttribute("role"));
        }

        return "student";
    }

    /**
     * 跳转到学生列表页面
     */
//    @GetMapping("/list")
//    public String list(Model model) {
//        List<Student> students = studentService.selectAll();
//        model.addAttribute("students", students);
//        return "student/list";
//    }


    @GetMapping("/page-list")
    @ResponseBody
    @RequirePermission(roles = Constants.SUPER_ADMIN)
    @LogOperation("超级管理员分页查询学生信息")
    public Result<PageResult<Student>> getStudentListPage(StudentQuery query) {
        if (query == null) {
            return Result.error(ResultCode.PARAMETER_ERROR);
        }

        // 分页参数校验
        if (query.getPageNum() == null || query.getPageNum() < 1) {
            query.setPageNum(Constants.DEFAULT_PAGE_NUM);
        }
        if (query.getPageSize() == null || query.getPageSize() < 1) {
            query.setPageSize(Constants.DEFAULT_PAGE_SIZE);
        }

        PageResult<Student> studentList = studentService.selectByPageQuery(query);
        // 没有数据时返回空列表
        if (studentList == null || studentList.getData() == null
            || studentList.getData().isEmpty()) {
            return Result.success(new PageResult<>());
        }

        return Result.success(studentList);
    }


    /**
     * 跳转到添加学生页面
     */
    @GetMapping("/add")
    public String toAdd() {
        return "student/add";
    }

    /**
     * 添加学生
     */
    @PostMapping("/add")
    @ResponseBody
    public String add(@RequestBody Student student) {
        try {
            studentService.insert(student);
            return "success";
        } catch (Exception e) {
            return "error";
        }
    }

    /**
     * 跳转到编辑学生页面
     */
    @GetMapping("/edit/{id}")
    public String toEdit(@PathVariable Integer id, Model model) {
        Student student = studentService.selectById(id);
        model.addAttribute("student", student);
        return "student/edit";
    }

    /**
     * 更新学生信息
     */
    @PostMapping("/update/per-info")
    @ResponseBody
    @RequirePermission
    @LogOperation("更新学生信息")
    public Result<?> update(@RequestBody Student student) {
        try {
            // 获取当前登录的学生信息
            HttpSession session = SessionUtils.getSession(false);
            if (session == null) {
                throw new BusinessException(ResultCode.UNAUTHORIZED);
            }
            Object user = session.getAttribute(Constants.SESSION_ATTRIBUTE_USER);
            if (!(user instanceof Student)) {
                throw new BusinessException(ResultCode.UNAUTHORIZED);
            }

            validateUpdateInfo(student);

            Student currentStudent = (Student) user;
            student.setId(currentStudent.getId());

            // 调用 Service 层处理更新
            Result<?> result = studentService.updatePersonalInfo(student, currentStudent.getEmail());

            // 如果更新成功，更新 session 中的用户信息
            if (result.isSuccess()) {
                student.setPassword(null); // 清除密码
                session.setAttribute(Constants.SESSION_ATTRIBUTE_USER, student);
            }

            return result;
        } catch (BusinessException e) {
            logger.error("修改的个人信息不正确", e);
            throw new BusinessException(ResultCode.USER_UPDATE_FAILED);
        } catch (Exception e) {
            logger.error("系统错误", e);
            return Result.error(ResultCode.ERROR);
        }
    }

    /**
     * 删除学生
     */
    @PostMapping("/delete/{id}")
    @ResponseBody
    public String delete(@PathVariable Integer id) {
        try {
            studentService.deleteById(id);
            return "success";
        } catch (Exception e) {
            return "error";
        }
    }

    /**
     * 根据条件查询学生
     */
    @PostMapping("/search")
    @ResponseBody
    public List<Student> search(@RequestBody Student student) {
        return studentService.selectByCondition(student);
    }

    @GetMapping("/search/{studentNo}")
    @ResponseBody
    @RequirePermission
    @LogOperation("查询学生信息")
    public Result<Student> searchByStudentNo(@RequestParam String studentNo) {
        Student student = studentService.selectByStudentNo(studentNo);

        return Result.success(student);
    }

    /**
     * 根据宿舍号查询学生
     */
    @GetMapping("/dormitory/{dormitory}")
    @ResponseBody
    public List<Student> getByDormitory(@PathVariable String dormitory) {
        return studentService.selectByDormitory(dormitory);
    }

    /**
     * 根据班级查询学生
     */
    @GetMapping("/class/{className}")
    @ResponseBody
    public List<Student> getByClassName(@PathVariable String className) {
        return studentService.selectByClassName(className);
    }

    /**
     * 校验学生的修改信息
     * 
     * @param student 修改信息
     */
    private void validateUpdateInfo(Student student) {
        // 创建错误信息列表
        List<String> errors = new ArrayList<>();

        // todo 宿舍楼的校验 家长联系方式不能和学生一样

        // 验证必填字段
        if (StringUtils.isBlank(student.getStudentNo())) {
            errors.add("学号不能为空");
        }
        if (StringUtils.isBlank(student.getName())) {
            errors.add("姓名不能为空");
        }
        if (StringUtils.isBlank(student.getCollege())) {
            errors.add("学院不能为空");
        }
        if (StringUtils.isBlank(student.getClassName())) {
            errors.add("班级不能为空");
        }
        if (StringUtils.isBlank(student.getDormitory())) {
            errors.add("宿舍不能为空");
        }
        if (StringUtils.isBlank(student.getPhone())) {
            errors.add("手机号不能为空");
        } else if (!student.getPhone().matches("^1[3-9]\\d{9}$")) {
            errors.add("手机号格式不正确");
        }
        if (StringUtils.isBlank(student.getEmail())) {
            errors.add("邮箱不能为空");
        } else if (!student.getEmail().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            errors.add("邮箱格式不正确");
        }

        // 验证家长信息（至少有一方）
        boolean hasFatherInfo = StringUtils.isNotBlank(student.getFatherName()) &&
                StringUtils.isNotBlank(student.getFatherPhone());
        boolean hasMotherInfo = StringUtils.isNotBlank(student.getMotherName()) &&
                StringUtils.isNotBlank(student.getMotherPhone());

        if (!hasFatherInfo && !hasMotherInfo) {
            errors.add("至少需要填写一方家长信息（姓名和电话）");
        }

        // 如果家长信息已填写，验证手机号格式
        if (StringUtils.isNotBlank(student.getFatherPhone()) &&
                !student.getFatherPhone().matches("^1[3-9]\\d{9}$")) {
            errors.add("父亲手机号格式不正确");
        }
        if (StringUtils.isNotBlank(student.getMotherPhone()) &&
                !student.getMotherPhone().matches("^1[3-9]\\d{9}$")) {
            errors.add("母亲手机号格式不正确");
        }

        // 如果有错误，抛出业务异常
        if (!errors.isEmpty()) {
            throw new BusinessException(String.join("; ", errors));
        }
    }
}