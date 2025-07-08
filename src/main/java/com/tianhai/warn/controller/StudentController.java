package com.tianhai.warn.controller;

import com.tianhai.warn.annotation.LogOperation;
import com.tianhai.warn.annotation.RequirePermission;
import com.tianhai.warn.constants.Constants;
import com.tianhai.warn.enums.ResultCode;
import com.tianhai.warn.exception.BusinessException;
import com.tianhai.warn.exception.SystemException;
import com.tianhai.warn.model.Student;
import com.tianhai.warn.model.SuperAdmin;
import com.tianhai.warn.query.StudentQuery;
import com.tianhai.warn.service.StudentService;
import com.tianhai.warn.service.SuperAdminService;
import com.tianhai.warn.service.VerificationService;
import com.tianhai.warn.utils.PageResult;
import com.tianhai.warn.utils.Result;
import com.tianhai.warn.utils.RoleObjectCaster;
import com.tianhai.warn.utils.SessionUtils;
import jakarta.servlet.http.HttpSession;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 学生信息控制器
 */
@Controller
@RequestMapping("/student")
public class StudentController {
    private static final Logger logger = LoggerFactory.getLogger(StudentController.class);

    @Autowired
    private StudentService studentService;

    @Autowired
    private SuperAdminService superAdminService;

    @Autowired
    private VerificationService verificationService;

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

    @GetMapping("/page-list")
    @ResponseBody
    @RequirePermission(roles = Constants.SUPER_ADMIN)
    @LogOperation("超级管理员分页查询学生信息")
    public Result<PageResult<Student>> getStudentListPage(StudentQuery query) {
        if (query == null) {
            logger.error("查询条件不合规， query: {}", query);
            throw new BusinessException(ResultCode.PARAMETER_ERROR);
        }

        verificationService.checkSuperAdminStatus();

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
     * 学生更新学生信息
     */
    @PostMapping("/update/per-info")
    @ResponseBody
    @RequirePermission(roles = Constants.STUDENT)
    @LogOperation("学生更新学生信息")
    public Result<?> updateByOneself(@RequestBody Student newStudentInfo) {
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

            validateUpdateInfo(newStudentInfo);

            Student sessionStudent = RoleObjectCaster.cast(Constants.STUDENT, user);
            newStudentInfo.setId(sessionStudent.getId());

            // 调用 Service 层处理更新
            Result<?> result = studentService.updatePersonalInfoByStudent(newStudentInfo, sessionStudent.getEmail());

            // 如果更新成功，更新 session 中的用户信息
            if (result.isSuccess()) {
                newStudentInfo.setPassword(null); // 清除密码
                session.setAttribute(Constants.SESSION_ATTRIBUTE_USER, newStudentInfo);
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

    @PostMapping("/super-admin/update/per-info")
    @ResponseBody
    @RequirePermission(roles = Constants.SUPER_ADMIN)
    @LogOperation("超级管理员更新学生信息")
    public Result<?> updateBySuperAdmin(@RequestBody Student newStudentInfo) {
        // 检查超级管理员的状态
        verificationService.checkSuperAdminStatus();

        if (newStudentInfo.getId() == null) {
            // 密码脱敏
            newStudentInfo.setPassword(null);
            logger.error("提交的学生信息缺少id，newStudentInfo: {}", newStudentInfo);
            throw new BusinessException(ResultCode.PARAMETER_ERROR);
        }
        if (newStudentInfo.getId() <= 0) {
            // 密码脱敏
            newStudentInfo.setPassword(null);
            logger.error("提交的学生的id不合法， newStudentInfo: {}",newStudentInfo);
            throw new BusinessException(ResultCode.PARAMETER_ERROR);
        }

        int affectedRow = studentService.updatePersonalInfoBySuperAdmin(newStudentInfo);
        if (affectedRow <= 0) {
            logger.error("超级管理员更新学生信息失败， newStudentInfo: {}",newStudentInfo);
            throw new SystemException(ResultCode.ERROR);
        }

        return Result.success();
    }

    /**
     * 删除学生
     */
    @DeleteMapping("/super-admin/delete")
    @ResponseBody
    @RequirePermission(roles = Constants.SUPER_ADMIN)
    @LogOperation("超级管理员删除学生信息")
    public Result<?> deleteBySuperAdmin(@RequestBody StudentQuery query) {
        if (query == null || query.getIds() == null || query.getIds().isEmpty()) {
            logger.error("没提供要删除学生的学号列表");
            throw new BusinessException(ResultCode.PARAMETER_ERROR);
        }

        List<Integer> ids = query.getIds();
        if (ids.stream().anyMatch(id -> id == null || id < 0)) {
            logger.error("提供的学生ID列表中包含无效的ID");
            throw new BusinessException(ResultCode.PARAMETER_ERROR);
        }

        verificationService.checkSuperAdminStatus();

        List<Integer> distinctIds = ids.stream().distinct().toList();

        int deletedRows = studentService.deleteByIds(distinctIds);
        if (deletedRows < distinctIds.size()) {
            logger.error("删除学生信息出错");
            throw new SystemException(ResultCode.ERROR);
        }

        return Result.success();
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