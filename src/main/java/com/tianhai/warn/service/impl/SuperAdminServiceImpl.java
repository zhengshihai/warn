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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.web.multipart.MultipartFile;

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
            throw new BusinessException(ResultCode.VALIDATE_FAILED);
        }

        // 通过id判断是否存在该超级管理员
        SuperAdmin existingAdmin = superAdminMapper.selectById(newSuperAdminInfo.getId());
        if (existingAdmin == null) {
            throw new BusinessException("更新失败，该超级管理员账号不存在");
        }

        // 通过email查询超级管理员
        SuperAdmin query = SuperAdmin.builder().email(newSuperAdminInfo.getEmail()).build();
        List<SuperAdmin> adminListByEmail = superAdminMapper.selectByCondition(query);

        SuperAdmin adminByEmail = new SuperAdmin();
        if (adminListByEmail != null) {
            adminByEmail = adminListByEmail.get(0);
        }

        // 邮箱校验以及唯一性判断
        // emailNeedUpdate 更新信息是否包含邮箱
        boolean emailNeedUpdate = newSuperAdminInfo.getEmail() != null &&
                !newSuperAdminInfo.getEmail().equals(existingAdmin.getEmail());
        if (emailNeedUpdate) {
            boolean formatValid = checkEmailFormat(newSuperAdminInfo.getEmail());

            // emailUsed 新的邮箱是否已被使用
            boolean emailUsed = adminByEmail != null &&
                    !Objects.equals(adminByEmail.getId(), newSuperAdminInfo.getId());
            if (emailUsed) {
                logger.error("新设定的邮箱已被占用");
                throw new BusinessException(ResultCode.EMAIL_USED);
            }
        }

        if (StringUtils.isNotBlank(newSuperAdminInfo.getPassword())) {
            newSuperAdminInfo.setPassword(DigestUtils.md5DigestAsHex(
                    newSuperAdminInfo.getPassword().getBytes()));
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
            case Constants.STUDENT            ->  StudentExcelDTO.class;
            case Constants.DORMITORY_MANAGER  ->  DormitoryManagerExcelDTO.class;
            case Constants.SYSTEM_USER        ->  SysUserExcelDTO.class;
            case Constants.SUPER_ADMIN        ->  SuperAdminExcelDTO.class;
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
            case Constants.STUDENT            ->  processStudentImport((List<StudentExcelDTO>) allExcelInfoList);
            case Constants.DORMITORY_MANAGER  ->  processDormitoryManagerImport((List<DormitoryManagerExcelDTO>) allExcelInfoList);
            case Constants.SYSTEM_USER        ->  processSysUserImport((List<SysUserExcelDTO>) allExcelInfoList);
            case Constants.SUPER_ADMIN        ->  processSuperAdminImport((List<SuperAdminExcelDTO>) allExcelInfoList);

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
        resultMap.put(VALID_COUNT, 0);
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
    private List<DormitoryManager> convertDormitoryManagerExcelDTOList( List<DormitoryManagerExcelDTO> dormitoryManagerExcelDTOList) {
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
            sysUser.setPassword(DigestUtils.md5DigestAsHex(dto.getPassword().getBytes()));

            sysUserList.add(sysUser);
        }

        return sysUserList;
    }

}
