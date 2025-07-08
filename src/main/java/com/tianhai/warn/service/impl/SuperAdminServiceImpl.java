package com.tianhai.warn.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.alibaba.excel.EasyExcel;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.tianhai.warn.constants.Constants;
import com.tianhai.warn.dto.*;
import com.tianhai.warn.enums.ResultCode;
import com.tianhai.warn.exception.BusinessException;
import com.tianhai.warn.exception.SystemException;
import com.tianhai.warn.mapper.SuperAdminMapper;
import com.tianhai.warn.model.DormitoryManager;
import com.tianhai.warn.model.Student;
import com.tianhai.warn.model.SuperAdmin;
import com.tianhai.warn.model.SysUser;
import com.tianhai.warn.query.SuperAdminQuery;
import com.tianhai.warn.service.*;
import com.tianhai.warn.utils.PageResult;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class SuperAdminServiceImpl implements SuperAdminService {

    private static final Logger logger = LoggerFactory.getLogger(SuperAdminServiceImpl.class);

    @Autowired
    private SuperAdminMapper superAdminMapper;

    @Autowired
    private VerificationService verificationService;

    @Autowired
    private StudentService studentService;

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private DormitoryManagerService dormitoryManagerService;

    private static final String VALID_COUNT = "validCount";
    private static final String INVALID_LIST = "invalidList";
    private static final String TOTAL_COUNT = "totalCount";

    // 查询超级管理员的的信息（不含密码）
    @Override
    public SuperAdmin selectByIdWithoutPassword(Integer id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ResultCode.PARAMETER_ERROR);
        }

        SuperAdmin superAdmin = superAdminMapper.selectById(id);
        superAdmin.setPassword(null);

        return superAdmin;
    }

    // 查询超级管理员的信息（含密码）
    @Override
    public SuperAdmin getByIdWithPassword(Integer id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ResultCode.PARAMETER_ERROR);
        }

        SuperAdmin superAdmin = superAdminMapper.selectById(id);
        if (superAdmin == null) {
            throw new BusinessException(ResultCode.SUPER_ADMIN_NOT_FOUND);
        }

        return superAdmin;
    }

    @Override
    public SuperAdmin getByEmail(String email) {
        if (!checkEmailFormat(email)) {
            throw new BusinessException(ResultCode.PARAMETER_ERROR);
        }

        SuperAdmin query = SuperAdmin.builder().email(email).build();

        List<SuperAdmin> superAdmins = selectByCondition(query);
        if (superAdmins.isEmpty()) {
            // return new SuperAdmin();
            logger.warn("没有找到该email对应的超级管理员，email:{}", email);
            return null;
        }

        return superAdmins.get(0);
    }

    @Override
    public void updateLastLoginTime(Integer id) {
        if (id == null || id <= 0) {
            logger.error("该超级管理员id不合法，id：{}", id);
            throw new BusinessException(ResultCode.PARAMETER_ERROR);
        }
        SuperAdmin superAdmin = superAdminMapper.selectById(id);
        if (superAdmin == null) {
            logger.error("找不到对应的超级管理员：id: {}", id);
            throw new BusinessException(ResultCode.PARAMETER_ERROR);
        }

        superAdmin.setLastLoginTime(new Date());
        int affectedRows = superAdminMapper.update(superAdmin);
        if (affectedRows <= 0) {
            logger.error("超级管理员更新出现异常，superAdmin:{}", superAdmin);
            throw new SystemException(ResultCode.ERROR);
        }
    }

    @Override
    public List<SuperAdmin> selectAll() {
        List<SuperAdmin> adminList = superAdminMapper.selectAll();
        if (adminList.isEmpty()) {
            logger.warn("不存在任何超级管理员");
        }

        return adminList;
    }

    @Override
    public Integer insert(SuperAdmin admin) {
        if (admin == null) {
            throw new BusinessException("超级管理员信息不能为空");
        }
        if (StringUtils.isBlank(admin.getEmail())) {
            throw new BusinessException("邮箱不能为空");
        }

        SuperAdmin query = SuperAdmin.builder().email(admin.getEmail()).build();
        List<SuperAdmin> dbSuperAdminList = superAdminMapper.selectByCondition(query);

        if (dbSuperAdminList != null && !dbSuperAdminList.isEmpty()) {
            throw new BusinessException("邮箱已被注册");
        }

        admin.setPassword(DigestUtils.md5DigestAsHex(admin.getPassword().getBytes()));

        if (admin.getEnabled() == null) {
            admin.setEnabled(1); // 默认启用
        }
        admin.setCreateTime(new Date());
        admin.setUpdateTime(new Date());
        admin.setLastLoginTime(new Date());

        try {
            int affectedRow = superAdminMapper.insert(admin);
            if (affectedRow <= 0) {
                throw new SystemException(ResultCode.ERROR);
            }
            return affectedRow;
        } catch (DuplicateKeyException e) {
            throw new BusinessException("该邮箱已被注册");
        }
    }

    /**
     * 更新信息
     * 本项目设定超级管理员邮箱可以和班级管理员（SysUser)的邮箱相同，
     * 而宿管 班级管理员 学生三者之间的邮箱不能相同
     * 
     * @param newSuperAdminInfo 更新信息
     * @return 成功更新的行数
     */
    @Override
    public Integer update(SuperAdmin newSuperAdminInfo) {
        if (newSuperAdminInfo == null || newSuperAdminInfo.getId() == null) {
            logger.error("superAdmin的id不能为空");
            throw new BusinessException(ResultCode.PARAMETER_ERROR);
        }

        // 通过id判断是否存在该超级管理员
        SuperAdmin existingAdmin = superAdminMapper.selectById(newSuperAdminInfo.getId());
        if (existingAdmin == null) {
            logger.error("更新失败，该超级管理员账号不存在");
            throw new BusinessException(ResultCode.USER_NOT_EXISTS);
        }

        // 通过email查询超级管理员
        SuperAdmin query = SuperAdmin.builder().email(newSuperAdminInfo.getEmail()).build();
        List<SuperAdmin> adminListByEmail = superAdminMapper.selectByCondition(query);

        SuperAdmin adminByEmail = new SuperAdmin();
        if (!adminListByEmail.isEmpty()) {
            adminByEmail = adminListByEmail.get(0);
        }

        // 邮箱校验以及唯一性判断
        // emailNeedUpdate 更新信息是否包含邮箱
        boolean emailNeedUpdate = newSuperAdminInfo.getEmail() != null &&
                !newSuperAdminInfo.getEmail().equals(existingAdmin.getEmail());
        if (emailNeedUpdate) {
            // 校验邮箱格式是否正确
            boolean formatValid = checkEmailFormat(newSuperAdminInfo.getEmail());
            if (!formatValid) {
                logger.error("超级管理员新提交的邮箱不合规，email: {}", newSuperAdminInfo.getEmail());
                throw new BusinessException(ResultCode.PARAMETER_ERROR);
            }

            // emailUsed 新的邮箱是否已被使用
            boolean emailUsed = adminByEmail != null &&
                    !Objects.equals(adminByEmail.getId(), newSuperAdminInfo.getId());
            if (emailUsed) {
                logger.error("新设定的邮箱已被占用");
                throw new BusinessException(ResultCode.EMAIL_USED);
            }
        }

        if (StringUtils.isNotBlank(newSuperAdminInfo.getPassword())) {
            newSuperAdminInfo.setPassword(DigestUtils.md5DigestAsHex(newSuperAdminInfo.getPassword().getBytes()));
        }

        // 因项目未设置单设备登录等原因 故此处使用乐观锁安全并发更新
        newSuperAdminInfo.setUpdateTime(new Date());
        newSuperAdminInfo.setVersion(existingAdmin.getVersion());

        int affectedRows = superAdminMapper.update(newSuperAdminInfo);
        if (affectedRows == 0) {
            throw new ConcurrentModificationException("更新失败，数据可能被其他用户更改");
        }

        return affectedRows;
    }

    /**
     * 校验邮箱格式是否正确
     * 
     * @param email 邮箱格式
     * @return 校验结果
     */
    private boolean checkEmailFormat(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }

        return email.matches(Constants.EMAIL_REGEX);
    }

    /**
     * 删除信息
     * 
     * @param id 主键id
     * @return 删除结果
     */
    @Override
    public Integer deleteById(Integer id) {
        if (id == null || id <= 0) {
            logger.error("无效的超级管理员id");
            throw new BusinessException(ResultCode.VALIDATE_FAILED);
        }

        SuperAdmin existingAdmin = superAdminMapper.selectById(id);
        if (existingAdmin == null) {
            throw new BusinessException(ResultCode.SUPER_ADMIN_NOT_FOUND);
        }

        // 防止删除唯一的超级管理员
        int adminCount = superAdminMapper.countAll();
        if (adminCount <= 1) {
            logger.error("不能删除唯一的超级管理员");
            throw new SystemException(ResultCode.SUPER_ADMIN_DELETE_FAILED);
        }

        int affectedRows = superAdminMapper.deleteById(id);
        if (affectedRows == 0) {
            throw new SystemException(ResultCode.ERROR);
        }

        return affectedRows;
    }

    @Override
    public List<SuperAdmin> selectByCondition(SuperAdmin superAdmin) {
        if (superAdmin == null || isEmptyCondition(superAdmin)) {
            logger.error("请至少提供一个查询条件");
            throw new BusinessException(ResultCode.VALIDATE_FAILED);
        }

        List<SuperAdmin> adminList = superAdminMapper.selectByCondition(superAdmin);

        if (adminList.isEmpty()) {
            logger.info("不存在满足该条件的超级管理员");
            return new ArrayList<>();
        }

        return adminList;
    }

    /**
     * 判断筛选条件是否全为空（version属性例外）
     * 
     * @param admin 筛选条件
     * @return 条件是否全为空
     */
    private boolean isEmptyCondition(SuperAdmin admin) {
        return Stream.of(
                admin.getId(),
                StringUtils.stripToNull(admin.getName()),
                StringUtils.stripToNull(admin.getEmail()),
                admin.getEnabled(),
                admin.getCreateTime(),
                admin.getUpdateTime()).allMatch(Objects::isNull);
    }

    @Override
    public PageResult<SuperAdmin> selectByPageQuery(SuperAdminQuery query) {
        List<SuperAdmin> superAdminList;
        PageResult<SuperAdmin> result;

        try (Page<SuperAdmin> page = PageHelper.startPage(query.getPageNum(), query.getPageSize())) {
            superAdminList = superAdminMapper.selectAll();
            result = buildPageResult(superAdminList);
        }

        return result;
    }

    // 构建分页结果
    private PageResult<SuperAdmin> buildPageResult(List<SuperAdmin> superAdminList) {
        PageInfo<SuperAdmin> pageInfo = new PageInfo<>(superAdminList);

        PageResult<SuperAdmin> result = new PageResult<>();
        result.setData(pageInfo.getList());
        result.setTotal((int) pageInfo.getTotal());
        result.setPageNum(pageInfo.getPageNum());
        result.setPageSize(pageInfo.getPageSize());

        return result;
    }

    // 从Excel中批量导入用户数据
    @Override
    public Map<String, Object> importExcelInfoBatch(MultipartFile file, String insertUserRole) {
        List<?> allExcelInfoList;

        // 根据角色获取对应的ExcelDTO类
        Class<?> excelDTOClass = getExcelDTOClassByRole(insertUserRole);
        if (excelDTOClass == null) {
            logger.error("不支持该用户角色导入，insertUserRole: {}", insertUserRole);
            throw new BusinessException(ResultCode.USER_ROLE_DISABLE);
        }

        // 读取Excel数据
        try (InputStream is = file.getInputStream()) {
            allExcelInfoList = EasyExcel.read(is)
                    .head(excelDTOClass)
                    .sheet()
                    .doReadSync();
        } catch (Exception e) {
            logger.error("文件解析失败", e);
            throw new BusinessException(ResultCode.FILE_PARSE_FAIL);
        }

        // 根据角色处理不同的导入逻辑
        return processImportByRole(allExcelInfoList, insertUserRole);
    }

    /**
     * 根据用户角色获取对应的Excel DTO类
     */
    private Class<?> getExcelDTOClassByRole(String insertUserRole) {
        return switch (insertUserRole.toLowerCase()) {
            case Constants.STUDENT -> StudentExcelDTO.class;
            case Constants.DORMITORY_MANAGER -> DormitoryManagerExcelDTO.class;
            case Constants.SYSTEM_USER -> SysUserExcelDTO.class;
            case Constants.SUPER_ADMIN -> SuperAdminExcelDTO.class;
            default -> null;
        };
    }

    /**
     * 根据角色处理不同的导入逻辑
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> processImportByRole(List<?> allExcelInfoList, String insertUserRole) {
        // 校验Excel数据是否为空
        if (allExcelInfoList.isEmpty()) {
            logger.warn("上传的用户信息Excel文件中没有用户信息");

            Map<String, Object> resultMap = new HashMap<>();
            resultMap.put(VALID_COUNT, 0);
            resultMap.put(INVALID_LIST, new ArrayList<>());
            resultMap.put(TOTAL_COUNT, 0);

            return resultMap;
        }

        return switch (insertUserRole.toLowerCase()) {
            case Constants.STUDENT -> processStudentImport((List<StudentExcelDTO>) allExcelInfoList);
            case Constants.DORMITORY_MANAGER ->
                processDormitoryManagerImport((List<DormitoryManagerExcelDTO>) allExcelInfoList);
            case Constants.SYSTEM_USER -> processSysUserImport((List<SysUserExcelDTO>) allExcelInfoList);
            case Constants.SUPER_ADMIN -> processSuperAdminImport((List<SuperAdminExcelDTO>) allExcelInfoList);

            default -> {
                logger.error("暂时不支持该用户角色导入，insertUserRole: {}", insertUserRole);
                throw new BusinessException(ResultCode.USER_ROLE_DISABLE);
            }
        };
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertBatch(List<SuperAdmin> superAdminList) {
        if (superAdminList == null || superAdminList.isEmpty()) {
            logger.error("批量插入的超级管理员信息为空");
            return 0;
        }

        return superAdminMapper.insertBatch(superAdminList);
    }

    /**
     * 处理学生信息导入
     */
    private Map<String, Object> processStudentImport(List<StudentExcelDTO> allExcelStudentInfoList) {
        Map<String, Object> resultMap = new HashMap<>();

        // 校验Excel中的学生信息是否合规
        List<StudentInfoValidateResult> validateResultList = verificationService
                .validateStudentExcelInfo(allExcelStudentInfoList);

        // 分离合规的学生信息数据
        List<StudentExcelDTO> validExcelStudentList = validateResultList.stream()
                .filter(StudentInfoValidateResult::isValid)
                .map(StudentInfoValidateResult::getStudentExcelDTO)
                .toList();
        logger.info("本次上传的学生数据，合规的学生数据条数为：{}", validExcelStudentList.size());

        // 分离不合规且有错误信息的学生信息数据
        List<StudentInfoValidateResult> invalidateExcelResultList = validateResultList.stream()
                .filter(validateResult -> !validateResult.isValid())
                .toList();
        logger.info("本次上传的学生数据，不合规的学生数据条数为：{}", invalidateExcelResultList.size());

        // 将Excel的学生信息转成完整的学生信息 + 密码加密
        List<Student> studentList = convertStudentExcelDTOList(validExcelStudentList);

        // 分批次批量插入学生表
        int insertRows = studentService.insertBatch(studentList);
        if (insertRows != validExcelStudentList.size()) {
            logger.error("批量插入学生信息出现异常");
            throw new SystemException(ResultCode.ERROR);
        }

        // 构造返回结果
        resultMap.put(VALID_COUNT, insertRows);
        resultMap.put(INVALID_LIST, invalidateExcelResultList);
        resultMap.put(TOTAL_COUNT, allExcelStudentInfoList.size());

        return resultMap;
    }

    /**
     * 处理宿管信息导入
     */
    private Map<String, Object> processDormitoryManagerImport(
            List<DormitoryManagerExcelDTO> allExcelDormitoryManagerInfoList) {
        Map<String, Object> resultMap = new HashMap<>();

        // 校验Excel中的宿管信息是否合规
        List<DorManInfoValidateResult> validateResultList = verificationService
                .validateDorManExcelInfo(allExcelDormitoryManagerInfoList);

        // 分离合规的宿管信息数据
        List<DormitoryManagerExcelDTO> validExcelDormitoryManagerList = validateResultList.stream()
                .filter(DorManInfoValidateResult::isValid)
                .map(DorManInfoValidateResult::getDormitoryManagerExcelDTO)
                .toList();
        logger.info("本次上传的宿管数据，合规的宿管数据条数为：{}", validExcelDormitoryManagerList.size());

        // 分离不合规且有错误信息的宿管信息数据
        List<DorManInfoValidateResult> inValidateExcelResultList = validateResultList.stream()
                .filter(validateResult -> !validateResult.isValid())
                .toList();
        logger.info("本次上传的宿管数据，不合规的宿管数据条数为：{}", inValidateExcelResultList.size());

        // 将Excel的宿管信息转成完整的宿管信息数据
        List<DormitoryManager> dormitoryManagerList = convertDormitoryManagerExcelDTOList(
                validExcelDormitoryManagerList);

        // 批量插入宿管表
        int insertRows = dormitoryManagerService.insertBatch(dormitoryManagerList);
        if (insertRows != validExcelDormitoryManagerList.size()) {
            logger.error("批量插入宿管信息出现异常");
            throw new SystemException(ResultCode.ERROR);
        }

        // 构造返回结果
        resultMap.put(VALID_COUNT, insertRows);
        resultMap.put(INVALID_LIST, inValidateExcelResultList);
        resultMap.put(TOTAL_COUNT, allExcelDormitoryManagerInfoList.size());

        return resultMap;
    }

    /**
     * 处理班级管理员信息导入
     */
    private Map<String, Object> processSysUserImport(List<SysUserExcelDTO> allExcelSysUserInfoList) {
        Map<String, Object> resultMap = new HashMap<>();

        // 校验Excel中超级管理员信息是否合规
        List<SysUserInfoValidateResult> validateResultList = verificationService
                .validateSysUserExcelInfo(allExcelSysUserInfoList);

        // 分离合规的班级管理员信息数据
        List<SysUserExcelDTO> validExcelSysUserList = validateResultList.stream()
                .filter(SysUserInfoValidateResult::isValid)
                .map(SysUserInfoValidateResult::getSysUserExcelDTO)
                .toList();
        logger.info("本次上传的班级管理员数据，合规的班级管理员数据条数为：{}", validExcelSysUserList.size());

        // 分离不合规且有错误信息的班级管理员信息数据
        List<SysUserInfoValidateResult> invalidateExcelResultList = validateResultList.stream()
                .filter(validateResult -> !validateResult.isValid())
                .toList();
        logger.info("本次上传的班级管理员数据，不合规的班级管理员数据条数为：{}", invalidateExcelResultList.size());

        // 将Excel的班级管理员信息转成完整的班级管理员信息数据
        List<SysUser> sysUserList = convertSysUserExcelDTOList(validExcelSysUserList);

        // 批量插入班级管理员表
        int insertRows = sysUserService.insertBatch(sysUserList);
        if (insertRows != validExcelSysUserList.size()) {
            logger.error("批量插入班级管理员信息出现异常");
            throw new SystemException(ResultCode.ERROR);
        }

        // 构造返回结果
        resultMap.put(VALID_COUNT, insertRows);
        resultMap.put(INVALID_LIST, invalidateExcelResultList);
        resultMap.put(TOTAL_COUNT, allExcelSysUserInfoList.size());

        return resultMap;
    }

    /**
     * 处理超级管理员信息导入（示例方法，需要根据实际DTO类实现）
     */
    private Map<String, Object> processSuperAdminImport(List<SuperAdminExcelDTO> allExcelSuperAdminInfoList) {
        Map<String, Object> resultMap = new HashMap<>();

        // 校验Excel中的学生信息是否合规
        List<SuperAdminInfoValidateResult> validateResultList = verificationService
                .validateSuperAdminExcelInfo(allExcelSuperAdminInfoList);

        // 分离合规的超级管理员信息数据
        List<SuperAdminExcelDTO> validExcelSuperAdminList = validateResultList.stream()
                .filter(SuperAdminInfoValidateResult::isValid)
                .map(SuperAdminInfoValidateResult::getSuperAdminExcelDTO)
                .toList();
        logger.info("本次上传的超级管理员数据，合规的超级管理员数据条数为：{}", validExcelSuperAdminList.size());

        // 分离不合规且带错误信息的超级管理员信息数据
        List<SuperAdminInfoValidateResult> invalidateExcelResultList = validateResultList.stream()
                .filter(validateResult -> !validateResult.isValid())
                .toList();
        logger.info("本次上传的超级管理员数据，不合规的超级管理员数据条数为：{}", invalidateExcelResultList.size());

        // 对密码进行加密处理
        for (SuperAdminExcelDTO superAdminExcelDTO : validExcelSuperAdminList) {
            String rawPwd = superAdminExcelDTO.getPassword();
            if (rawPwd != null) {
                superAdminExcelDTO.setPassword(DigestUtils.md5DigestAsHex(rawPwd.getBytes()));
            }
        }

        // 将Excel的超级管理员信息转成完整的超级管理员信息
        List<SuperAdmin> superAdminList = convertSuperAdminExcelDTOList(validExcelSuperAdminList);

        // 批量插入超级管理员表
        int insertRows = superAdminMapper.insertBatch(superAdminList);
        if (insertRows != validExcelSuperAdminList.size()) {
            logger.error("批量插入超级管理员出现异常");
            throw new SystemException(ResultCode.ERROR);
        }

        // 构造返回结果
        resultMap.put(VALID_COUNT, insertRows);
        resultMap.put(INVALID_LIST, invalidateExcelResultList);
        resultMap.put(TOTAL_COUNT, allExcelSuperAdminInfoList.size());

        return resultMap;
    }

    // 将StudentExcelDTO转成Student
    private List<Student> convertStudentExcelDTOList(List<StudentExcelDTO> studentExcelDTOList) {
        Date now = new Date();
        final int threshold = 2000; // 设置学生列表阈值为2000

        if (studentExcelDTOList.size() < threshold) {
            List<Student> studentList = new ArrayList<>(studentExcelDTOList.size());
            for (StudentExcelDTO dto : studentExcelDTOList) {
                Student student = new Student();
                BeanUtil.copyProperties(dto, student);
                student.setCreateTime(now);
                student.setUpdateTime(now);
                student.setPassword(DigestUtils.md5DigestAsHex(dto.getPassword().getBytes()));

                studentList.add(student);
            }

            return studentList;
        }

        return studentExcelDTOList.stream()
                .map(dto -> {
                    Student student = new Student();
                    BeanUtil.copyProperties(dto, student);
                    student.setCreateTime(now);
                    student.setUpdateTime(now);
                    student.setPassword(DigestUtils.md5DigestAsHex(dto.getPassword().getBytes()));
                    return student;
                })
                .collect(Collectors.toList());
    }

    // 将SuperAdminExcelDTO转成SuperAdmin
    private List<SuperAdmin> convertSuperAdminExcelDTOList(List<SuperAdminExcelDTO> superAdminExcelDTOList) {
        Date now = new Date();
        Integer enabled = Constants.ENABLE_INT;

        List<SuperAdmin> superAdminList = new ArrayList<>(superAdminExcelDTOList.size());
        for (SuperAdminExcelDTO dto : superAdminExcelDTOList) {
            SuperAdmin superAdmin = new SuperAdmin();
            BeanUtil.copyProperties(dto, superAdmin);
            superAdmin.setCreateTime(now);
            superAdmin.setUpdateTime(now);
            superAdmin.setEnabled(enabled);
            superAdmin.setVersion(0);
            superAdmin.setPassword(DigestUtils.md5DigestAsHex(dto.getPassword().getBytes()));

            superAdminList.add(superAdmin);
        }

        return superAdminList;
    }

    // 将DormitoryManagerExcelDTO转成DormitoryManager
    private List<DormitoryManager> convertDormitoryManagerExcelDTOList(
            List<DormitoryManagerExcelDTO> dormitoryManagerExcelDTOList) {
        Date now = new Date();
        String status = Constants.ON_DUTY;

        List<DormitoryManager> dormitoryManagerList = new ArrayList<>(dormitoryManagerExcelDTOList.size());
        for (DormitoryManagerExcelDTO dto : dormitoryManagerExcelDTOList) {
            DormitoryManager dormitoryManager = new DormitoryManager();
            BeanUtil.copyProperties(dto, dormitoryManager);
            dormitoryManager.setStatus(status);
            dormitoryManager.setCreateTime(now);
            dormitoryManager.setUpdateTime(now);
            dormitoryManager.setPassword(DigestUtils.md5DigestAsHex(dto.getPassword().getBytes()));

            dormitoryManagerList.add(dormitoryManager);
        }

        return dormitoryManagerList;
    }

    // 将SysUserExcelDTO转成SysUser
    private List<SysUser> convertSysUserExcelDTOList(List<SysUserExcelDTO> sysUserExcelDTOList) {
        Date now = new Date();
        String enabled = Constants.ENABLE_STR;

        List<SysUser> sysUserList = new ArrayList<>(sysUserExcelDTOList.size());
        for (SysUserExcelDTO dto : sysUserExcelDTOList) {
            SysUser sysUser = new SysUser();
            BeanUtil.copyProperties(dto, sysUser);
            sysUser.setStatus(enabled);
            sysUser.setCreateTime(now);
            sysUser.setUpdateTime(now);
            sysUser.setJobRole(convertJobRole(dto.getJobRole()));
            sysUser.setPassword(DigestUtils.md5DigestAsHex(dto.getPassword().getBytes()));

            sysUserList.add(sysUser);
        }

        return sysUserList;
    }

    // 将中文形式的jobRole转为英文
    private String convertJobRole(String jobRoleCN) {
        switch (jobRoleCN) {
            case "辅导员" -> {
                return Constants.JOB_ROLE_COUNSELOR;
            }

            case "班主任" -> {
                return Constants.JOB_ROLE_CLASS_TEACHER;
            }

            case "院级领导" -> {
                return Constants.JOB_ROLE_DEAN;
            }

            case "其他角色" -> {
                return Constants.JOB_ROLE_OTHER;
            }

            default -> logger.error("暂时不支持该职位角色");

        }

        return jobRoleCN;
    }

    /**
     * 生成包含错误数据的Excel工作簿
     * 
     * @param headers         表头列表，包含所有需要显示的字段名称
     * @param insertUserRole  用户角色，用于确定数据结构和字段映射
     * @param invalidDataList 错误数据列表，每个元素包含原始数据和错误信息
     * @return 包含错误数据的SXSSFWorkbook对象
     * @throws SystemException 当生成工作簿过程中发生异常时抛出
     */
    @Override
    public SXSSFWorkbook generateWorkbook(List<String> headers,
            String insertUserRole,
            List<Map<String, Object>> invalidDataList) {
        SXSSFWorkbook workbook = new SXSSFWorkbook(100);
        try {
            SXSSFSheet sheet = workbook.createSheet("错误数据");

            // 创建表头样式 - 灰色背景，粗体字体
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // 创建错误信息样式 - 珊瑚色背景，突出显示错误
            CellStyle errorStyle = workbook.createCellStyle();
            errorStyle.setFillForegroundColor(IndexedColors.CORAL.getIndex());
            errorStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // 写入表头行
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers.get(i));
                cell.setCellStyle(headerStyle);
            }

            // 写入数据行
            for (int i = 0; i < invalidDataList.size(); i++) {
                Row dataRow = sheet.createRow(i + 1);
                Map<String, Object> item = invalidDataList.get(i);

                // 根据角色获取对应的数据对象
                Map<String, Object> dataObj = getDataObjectByRole(item, insertUserRole);

                // 遍历表头，填充对应的数据
                for (int j = 0; j < headers.size(); j++) {
                    Cell cell = dataRow.createCell(j);
                    String header = headers.get(j);

                    if ("错误信息".equals(header)) {
                        // 错误信息列特殊处理：合并所有错误信息并用分号分隔
                        @SuppressWarnings("unchecked")
                        List<String> errors = (List<String>) item.get("errors");
                        String errorText = errors != null ? String.join("; ", errors) : "";
                        cell.setCellValue(errorText);
                        cell.setCellStyle(errorStyle); // 应用错误样式
                    } else {
                        // 普通字段：根据字段名获取对应的值
                        String value = getFieldValue(dataObj, header, insertUserRole);
                        cell.setCellValue(value != null ? value : "");
                    }
                }
            }

            // 设置列宽 - 使用安全的列宽设置方法
            setColumnWidths(sheet, headers);

            return workbook;

        } catch (Exception e) {
            logger.error("生成Excel工作簿失败", e);
            throw new SystemException(ResultCode.ERROR);
        }

    }

    /**
     * 安全地设置Excel列宽
     * 
     * @param sheet   Excel工作表
     * @param headers 表头列表
     */
    private void setColumnWidths(SXSSFSheet sheet, List<String> headers) {
        for (int i = 0; i < headers.size(); i++) {
            try {
                // 尝试自动调整列宽
                sheet.autoSizeColumn(i);
            } catch (Exception e) {
                // 如果自动调整失败，使用默认列宽
                logger.warn("自动调整列宽失败，使用默认列宽，列索引: {}", i);
                sheet.setColumnWidth(i, 15 * 256); // 15个字符宽度
            }
        }
    }

    /**
     * 根据用户角色从错误数据项中提取对应的数据对象
     * 
     * @param item           错误数据项，包含不同角色的ExcelDTO对象
     * @param insertUserRole 用户角色，用于确定提取哪个DTO对象
     * @return 对应角色的数据对象Map，如果角色不匹配则返回空Map
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> getDataObjectByRole(Map<String, Object> item, String insertUserRole) {
        return switch (insertUserRole.toLowerCase()) {
            case Constants.STUDENT -> (Map<String, Object>) item.get("studentExcelDTO");
            case Constants.DORMITORY_MANAGER -> (Map<String, Object>) item.get("dormitoryManagerExcelDTO");
            case Constants.SYSTEM_USER -> (Map<String, Object>) item.get("sysUserExcelDTO");
            case Constants.SUPER_ADMIN -> (Map<String, Object>) item.get("superAdminExcelDTO");
            default -> new HashMap<>();
        };
    }

    /**
     * 根据字段名从数据对象中获取对应的值
     * 
     * @param dataObj        数据对象，包含字段名和值的映射
     * @param fieldName      字段的中文名称（表头显示的名称）
     * @param insertUserRole 用户角色，用于确定某些字段的特殊映射规则
     * @return 字段对应的值，如果字段不存在或值为null则返回空字符串
     */
    private String getFieldValue(Map<String, Object> dataObj, String fieldName, String insertUserRole) {
        if (dataObj == null) {
            return "";
        }

        // 字段名映射表：中文表头 -> 英文字段名
        Map<String, String> fieldMap = new HashMap<>();
        fieldMap.put("学号", "studentNo");
        fieldMap.put("密码", "password");
        fieldMap.put("姓名", "name");
        fieldMap.put("学院", "college");
        fieldMap.put("班级", "className");
        fieldMap.put("宿舍", "dormitory");
        fieldMap.put("电话", "phone");
        fieldMap.put("邮箱", "email");
        fieldMap.put("父亲姓名", "fatherName");
        fieldMap.put("父亲电话", "fatherPhone");
        fieldMap.put("母亲姓名", "motherName");
        fieldMap.put("母亲电话", "motherPhone");
        fieldMap.put("负责楼栋", "building");
        fieldMap.put("职位角色", "jobRole");
        // 工号字段根据角色有不同的映射
        fieldMap.put("工号", insertUserRole.equals("dormitorymanager") ? "managerId" : "sysUserNo");

        String field = fieldMap.get(fieldName);
        Object value = field != null ? dataObj.get(field) : null;
        return value != null ? value.toString() : "";
    }

    /**
     * 根据用户角色获取对应的Excel表头列表
     * 
     * @param insertUserRole 用户角色，用于确定需要显示哪些字段
     * @return 对应角色的表头列表，包含所有需要显示的字段名称和"错误信息"列
     */
    @Override
    public List<String> getHeadersByRole(String insertUserRole) {
        return switch (insertUserRole.toLowerCase()) {
            case Constants.STUDENT -> Arrays.asList("学号", "密码", "姓名", "学院",
                    "班级", "宿舍", "电话", "邮箱",
                    "父亲姓名", "父亲电话", "母亲姓名", "母亲电话", "错误信息");
            case Constants.DORMITORY_MANAGER -> Arrays.asList("工号", "密码", "姓名", "电话", "邮箱", "负责楼栋", "错误信息");
            case Constants.SYSTEM_USER -> Arrays.asList("工号", "密码", "姓名", "电话", "邮箱", "职位角色", "错误信息");
            case Constants.SUPER_ADMIN -> Arrays.asList("密码", "姓名", "邮箱", "错误信息");
            default -> new ArrayList<>();
        };
    }

}
