package com.tianhai.warn.controller;

import com.alibaba.excel.EasyExcel;
import com.tianhai.warn.annotation.LogOperation;
import com.tianhai.warn.annotation.RequirePermission;
import com.tianhai.warn.constants.Constants;
import com.tianhai.warn.dto.StudentExcelDTO;
import com.tianhai.warn.dto.StudentInfoValidateResult;
import com.tianhai.warn.enums.ResultCode;
import com.tianhai.warn.exception.BusinessException;
import com.tianhai.warn.model.SuperAdmin;
import com.tianhai.warn.query.SuperAdminQuery;
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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 超级管理员信息控制器
 */
@Controller
@RequestMapping("/super-admin")
public class SuperAdminController {
    private static final Logger logger = LoggerFactory.getLogger(SuperAdminController.class);

    @Autowired
    private SuperAdminService superAdminService;

    @Autowired
    private VerificationService verificationService;

    @GetMapping
    public String superAdmin() {
        return "super-admin";
    }

    /**
     * 查找全部超级管理员
     * @return   超级管理员列表
     */
    @GetMapping("/list")
    @ResponseBody
    @RequirePermission(roles = Constants.SUPER_ADMIN)
    @LogOperation("查找全部管理员信息")
    public Result<List<SuperAdmin>> searchAllSuperAdmin() {
        List<SuperAdmin> allSuperAdmins = superAdminService.listAll();

        return Result.success(allSuperAdmins);
    }

    /**
     * 更新超级管理员信息
     * 这里允许超级管理员和非超级管理员的邮箱发生重叠
     * @param updateSuperAdmin        更新信息
     * @return                  更新结果
     */
    @PostMapping("/update/per-info")
    @ResponseBody
    @RequirePermission(roles = Constants.SUPER_ADMIN)
    @LogOperation("超级管理员更新个人信息")
    public Result<?> update(@RequestBody SuperAdmin updateSuperAdmin) {
        HttpSession session = SessionUtils.getSession(false);
        if (session == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        Object user = session.getAttribute(Constants.SESSION_ATTRIBUTE_USER);
        if (!(user instanceof SuperAdmin)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        validateUpdateInfo(updateSuperAdmin);

        SuperAdmin sessionSuperAdmin = RoleObjectCaster.cast(Constants.SUPER_ADMIN, user);

        if (StringUtils.isNotBlank(updateSuperAdmin.getName())) {
            sessionSuperAdmin.setName(updateSuperAdmin.getName());
        }
        if (StringUtils.isNotBlank(updateSuperAdmin.getEmail())) {
            sessionSuperAdmin.setEmail(updateSuperAdmin.getEmail());
        }
        if (StringUtils.isNotBlank(updateSuperAdmin.getPassword())) {
            sessionSuperAdmin.setPassword(updateSuperAdmin.getPassword());
        }

        superAdminService.update(sessionSuperAdmin);

        return Result.success();
    }

    // 校验修改信息是否符合要求
    private void validateUpdateInfo(SuperAdmin superAdmin) {
        if (StringUtils.isNotBlank(superAdmin.getName())) {
            if (superAdmin.getName().length() > 20) {
                logger.error("用户名超过规定长度，name:{}", superAdmin.getName());
                throw new BusinessException(ResultCode.PARAMETER_ERROR);
            }
        }

        if (StringUtils.isNotBlank(superAdmin.getEmail())) {
            if (!superAdmin.getEmail().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
                logger.error("邮箱格式不正确，email{}", superAdmin.getEmail());
                throw new BusinessException(ResultCode.PARAMETER_ERROR);
            }
        }
    }

    @GetMapping("/page-list")
    @ResponseBody
    @RequirePermission(roles = Constants.SUPER_ADMIN)
    @LogOperation("超级管理员分页查询超级管理员信息")
    public Result<PageResult<SuperAdmin>> getSuperAdminListPage(SuperAdminQuery query) {
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

        PageResult<SuperAdmin> superAdminPageResult = superAdminService.selectByPageQuery(query);
        // 没有数据时返回空列表
        if (superAdminPageResult == null || superAdminPageResult.getData() == null
                || superAdminPageResult.getData().isEmpty()) {
            return Result.success(new PageResult<>());
        }

        return Result.success(superAdminPageResult);

    }

    @PostMapping("/import-students")
    @ResponseBody
    @RequirePermission(roles = Constants.SUPER_ADMIN)
    @LogOperation("超级管理员批量导入用户信息") //为了检查当前实现的内容，暂时把返回值设置为空
    public void importStudents(@RequestParam("file") MultipartFile file,
                               @RequestParam("fileRole") String fileRole) {
        Map<String, Object> resultMap = new HashMap<>();
        List<StudentExcelDTO> allExcelStudentInfoList = new ArrayList<>();
        try (InputStream is = file.getInputStream()) {
            allExcelStudentInfoList = EasyExcel.read(is)
                    .head(StudentExcelDTO.class)
                    .sheet()
                    .doReadSync();
        } catch (Exception e) {
            logger.error("文件解析失败", e);
//            return Result.error(ResultCode.FILE_PARSE_FAIL);
        }

        // 校验Excel中的学生信息是否合规
        List<StudentInfoValidateResult> validateResultList =
                verificationService.validateStudentExcelInfo(allExcelStudentInfoList);

        // 分离合规的学生信息数据
        List<StudentExcelDTO> validStudentList = validateResultList.stream()
                .filter(StudentInfoValidateResult::isValid)
                .map(StudentInfoValidateResult::getStudentExcelDTO)
                .toList();
        logger.info("本次上传的学生数据，合规的学生数据条数为：{}", validStudentList.size());

        // 分离不合规且有错误信息的学生信息数据
        List<StudentInfoValidateResult> inValidateResultList = validateResultList.stream()
                .filter(validateResult -> !validateResult.isValid())
                .toList();
        logger.info("本次上传的学生数据，不合规的学生数据条数为：{}", inValidateResultList.size());

        

    }
}
