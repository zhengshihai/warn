package com.tianhai.warn.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.tianhai.warn.constants.Constants;
import com.tianhai.warn.dto.AuditActionDTO;
import com.tianhai.warn.dto.ProcessActionDTO;
import com.tianhai.warn.dto.StudentLateQueryDTO;
import com.tianhai.warn.dto.StudentLateResultDTO;
import com.tianhai.warn.enums.ResultCode;
import com.tianhai.warn.events.StatsEvent;
import com.tianhai.warn.exception.BusinessException;
import com.tianhai.warn.mapper.LateReturnMapper;
import com.tianhai.warn.model.DormitoryManager;
import com.tianhai.warn.model.LateReturn;
import com.tianhai.warn.model.Student;
import com.tianhai.warn.model.SysUser;
import com.tianhai.warn.query.LateReturnQuery;
import com.tianhai.warn.service.LateReturnService;
import com.tianhai.warn.service.StudentService;
import com.tianhai.warn.service.SysUserClassService;
import com.tianhai.warn.utils.RoleObjectCaster;
import com.tianhai.warn.utils.LateReturnIdGenerator;
import com.tianhai.warn.utils.PageResult;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * 晚归记录服务实现类
 * 提供晚归记录相关的业务逻辑实现，包括查询、统计、更新等功能
 *
 * @author tianhai
 * @since 1.0.0
 */
@Service
public class LateReturnServiceImpl implements LateReturnService {
    /**
     * 统计结果键名常量
     */
    private static final String STAT_PERIOD_COUNT = "statPeriodCount"; // 统计周期内的总记录数
    private static final String STAT_PENDING_COUNT = "statPendingCount"; // 待处理记录数
    private static final String STAT_PROCESSING_COUNT = "statProcessingCount"; // 处理中记录数
    private static final String STAT_FINISHED_COUNT = "statFinishedCount"; // 已完成记录数

    // 使用 Hash 结构存储晚归记录
    // key: late_return:unjustified:{studentNo}
    // field: lateReturnId
    // value: lateTime (时间戳)
    private static final String UNJUSTIFIED_LATE_RETURN_KEY = "late_return:unjustified:";

    // 存储所有有未正当理由晚归记录的学生学号
    private static final String UNJUSTIFIED_STUDENTS_SET = "late_return:unjustified:students";

    private static final Logger logger = LoggerFactory.getLogger(LateReturnServiceImpl.class);

    @Autowired
    private LateReturnMapper lateReturnMapper;

    @Autowired
    private StudentService studentService;

    @Autowired
    private SysUserClassService sysUserClassService;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;


    @Override
    public LateReturn getByLateReturnId(String lateReturnId) {
        return lateReturnMapper.selectByLateReturnId(lateReturnId);
    }

    @Override
    public List<LateReturn> selectAll() {
        return lateReturnMapper.selectAll();
    }

    @Override
    public List<LateReturn> selectByCondition(LateReturnQuery query) {
        return lateReturnMapper.selectByCondition(query);
    }

    @Override
    public List<LateReturn> selectByStudentNo(String studentNo) {
        return lateReturnMapper.selectByStudentNo(studentNo);
    }

    @Override
    public List<LateReturn> selectByProcessStatus(String processStatus) {
        return lateReturnMapper.selectByProcessStatus(processStatus);
    }

    @Override
    public List<LateReturn> selectByProcessResult(String processResult) {
        return lateReturnMapper.selectByProcessResult(processResult);
    }

    @Override
    public List<LateReturn> selectByTimeRange(Date startTime, Date endTime) {
        return lateReturnMapper.selectByTimeRange(startTime, endTime);
    }

    @Override
    public int insert(LateReturn lateReturn) {
        // 生成晚归记录ID
        lateReturn.setLateReturnId(LateReturnIdGenerator.generate());
        return lateReturnMapper.insert(lateReturn);
    }

    @Override
    public int update(LateReturn lateReturn) {
        return lateReturnMapper.update(lateReturn);
    }

    @Override
    public int deleteByLateReturnId(String lateReturnId) {
        LateReturn lateReturn = lateReturnMapper.selectByLateReturnId(lateReturnId);
        if (lateReturn != null) {
            return lateReturnMapper.deleteById(lateReturn.getId());
        }
        return 0;
    }

    @Override
    public int updateProcessStatus(String lateReturnId, String processStatus, String processResult,
            String processRemark) {
        LateReturn lateReturn = lateReturnMapper.selectByLateReturnId(lateReturnId);
        if (lateReturn == null) {
            return 0;
        }

        int affectedRow = lateReturnMapper.updateProcessStatus(lateReturn.getId(), processStatus, processResult,
                processRemark);

        /* 只有在更新成功，且晚归记录被判定为不合规时,才发布事件
        *  晚归记录不合规，有两张情况：
        * 1. precessStatus为FINISHED 且 processResult为空,
        *              表示该生晚归后没有提交说明材料，故被判违规
        * 2. processStatus为FINISHED 且 processResult为REJECTED
        *              表示该生晚归后有提交说明材料，但材料不通过，故被判为违规
        * */
        if (affectedRow > 0 && isViolationTriggered(processStatus, processResult)) {
            try {
                // 创建处理动作DTO
                ProcessActionDTO processActionDTO = ProcessActionDTO.builder()
                        .studentNo(lateReturn.getStudentNo())
                        .processResult(processResult)
                        .lateReturnId(lateReturnId)
                        .build();

                // 发布事件
                applicationEventPublisher.publishEvent(new StatsEvent(this, processActionDTO));
            } catch (Exception e) {
                logger.error("发布统计事件失败", e); // 这里只记录错误，不影响主流程
            }
        }

        // 只有在更新成功且processResult为 REJECTED 时, 才更新缓存
        if (affectedRow > 0 && isViolationTriggered(processStatus, processResult)) {
            if (Constants.AUDIT_ACTION_REJECT.equalsIgnoreCase(processResult)) {
                // 添加到Redis
                addUnjustifiedLateReturnToRedis(lateReturn);
            } else {
                // 从缓存中移除
                removeLateReturnFromRedis(lateReturn.getStudentNo(), lateReturnId);
            }
        }

        return affectedRow;
    }

    /**
     * 判断是否触发违规
     * @param processStatus      处理状态：PENDING-待处理/PROCESSING-处理中/FINISHED-已完成
     * @param processResult      处理结果：APPROVED-已通过/REJECTED-已驳回
     * @return                   判断结果
     */
    private boolean isViolationTriggered(String processStatus, String processResult) {
        if (Constants.LATE_RETURN_PROCESS_STATUS_FINISHED.equalsIgnoreCase(processStatus)
                 && Constants.AUDIT_ACTION_REJECT.equalsIgnoreCase(processResult)) {
            return true;
        }

        if (Constants.LATE_RETURN_PROCESS_STATUS_FINISHED.equalsIgnoreCase(processStatus)
                 && StringUtils.isBlank(processResult)) {
            return true;
        }

        return false;
    }

    @Override
    public PageResult<LateReturn> selectByPageQuery(LateReturnQuery query) {
        // // 获取数据范围
        // DataScope dataScope =(DataScope) RequestContextHolder.getRequestAttributes()
        // .getAttribute("dataScope", RequestAttributes.SCOPE_REQUEST);
        //
        // //根据数据范围设置查询条件
        // if (dataScope != null) {
        // boolean studentOnly = dataScope.getStudentNo() != null;
        // //学生只能查看自己的数据
        // if (studentOnly) query.setStudentNo(dataScope.getStudentNo());
        //
        // boolean dormanOnly = dataScope.getDormitories() != null &&
        // !dataScope.getDormitories().isEmpty();
        // //宿管只能查看管理的宿舍数据
        // if (dormanOnly) query.setDormitories(dataScope.getDormitories());
        //
        // boolean othersOnly = dataScope.getClasses() != null &&
        // !dataScope.getClasses().isEmpty();
        // //辅导员/班主任等只能查看管理的班级数据
        // if (othersOnly) query.setClasses(dataScope.getClasses());
        // }

        // 启动分页
        PageHelper.startPage(query.getPageNum(), query.getPageSize());

        // 查询数据 只查询当前页
        List<LateReturn> list = lateReturnMapper.selectByCondition(query);

        // PageInfo 封装分页信息
        PageInfo<LateReturn> pageInfo = new PageInfo<>(list);

        // 封装自定义的PageResult
        PageResult<LateReturn> result = new PageResult<>();
        result.setData(pageInfo.getList());
        result.setTotal((int) pageInfo.getTotal());
        result.setPageNum(pageInfo.getPageNum());
        result.setPageSize(pageInfo.getPageSize());

        return result;
    }

    @Override
    public Map<String, Object> countAllProcessStatus(LateReturnQuery lateReturnQuery) {
        return lateReturnMapper.countAllProcessStatus(lateReturnQuery);
    }

    /**
     * 获取晚归统计数据
     * 根据用户角色和权限范围，统计指定时间范围内的晚归记录处理状态
     *
     * @param startDateStr   开始时间字符串，格式：yyyy-MM-dd HH:mm:ss
     * @param endTimeStr     结束时间字符串，格式：yyyy-MM-dd HH:mm:ss
     * @param userRoleStr    用户角色字符串
     * @param currentUserObj 当前用户对象
     * @return 包含各状态统计数量的Map，键为状态名称，值为对应的数量
     * @throws BusinessException 当日期格式不正确或时间范围无效时抛出
     */
    @Override
    public Map<String, Object> getStatistics(String startDateStr, String endTimeStr,
            String userRoleStr, Object currentUserObj) {
        // 获取目标学生列表
        List<Student> targetStudentList = getTargetStudentList(userRoleStr, currentUserObj);
        if (targetStudentList.isEmpty()) {
            logger.warn("没有查到符合条件的学生，数据可能有异常");
            return new HashMap<>();
        }

        // 构建查询条件
        LateReturnQuery lateReturnQuery = buildLateReturnQuery(startDateStr, endTimeStr, targetStudentList);

        // 获取统计数据
        // todo 这里可以扩展，针对不同统计需求灵活选择对应的统计实现
        Map<String, Object> statistics = countAllProcessStatus(lateReturnQuery);

        // 转换统计结果的键名，并将Object转换为Integer
        Map<String, Object> result = new HashMap<>();
        result.put(STAT_PERIOD_COUNT,
                statistics.get("total") != null
                        ? ((Number) statistics.get("total")).intValue()
                        : 0);

        result.put(STAT_PENDING_COUNT,
                statistics.get("pending") != null
                        ? ((Number) statistics.get("pending")).intValue()
                        : 0);

        result.put(STAT_PROCESSING_COUNT,
                statistics.get("processing") != null
                        ? ((Number) statistics.get("processing")).intValue()
                        : 0);

        result.put(STAT_FINISHED_COUNT,
                statistics.get("finished") != null
                        ? ((Number) statistics.get("finished")).intValue()
                        : 0);

        return result;
    }

    /**
     * 获取目标学生列表
     * 根据用户角色和权限范围，获取可查看的学生列表
     * - 宿管只能查看其管理的宿舍楼的学生
     * - 系统用户（如辅导员）只能查看其管理的班级的学生
     *
     * @param userRoleStr    用户角色字符串
     * @param currentUserObj 当前用户对象
     * @return 目标学生列表，如果没有权限或没有符合条件的学生则返回空列表
     */
    private List<Student> getTargetStudentList(String userRoleStr, Object currentUserObj) {
        List<Student> allStudentList = studentService.selectAll();
        if (allStudentList == null || allStudentList.isEmpty()) {
            return new ArrayList<>();
        }

        if (userRoleStr.equalsIgnoreCase(Constants.DORMITORY_MANAGER)) {
            DormitoryManager dormitoryManager = RoleObjectCaster.cast(userRoleStr, currentUserObj);
            String dorBuilding = dormitoryManager.getBuilding();
            return allStudentList.stream()
                    .filter(student -> student.getDormitory().startsWith(dorBuilding))
                    .toList();
        }

        if (userRoleStr.equalsIgnoreCase(Constants.SYSTEM_USER)) {
            SysUser sysUser = RoleObjectCaster.cast(userRoleStr, currentUserObj);
            String sysUserNo = sysUser.getSysUserNo();
            List<String> managedClassList = sysUserClassService.getUserClasses(sysUserNo);
            return allStudentList.stream()
                    .filter(student -> managedClassList.contains(student.getClassName()))
                    .toList();
        }

        return new ArrayList<>();
    }

    /**
     * 构建晚归查询条件
     * 根据时间范围和目标学生列表构建查询条件
     * - 如果未指定时间范围，则使用默认时间范围（当前时间往前推指定月数）
     * - 如果指定了时间范围，则进行时间有效性验证
     *
     * @param startDateStr      开始时间字符串，格式：yyyy-MM-dd HH:mm:ss
     * @param endTimeStr        结束时间字符串，格式：yyyy-MM-dd HH:mm:ss
     * @param targetStudentList 目标学生列表
     * @return 构建好的查询条件对象
     * @throws BusinessException 当日期格式不正确或时间范围无效时抛出
     */
    private LateReturnQuery buildLateReturnQuery(String startDateStr, String endTimeStr,
            List<Student> targetStudentList) {
        LateReturnQuery query = new LateReturnQuery();

        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime parsedStartDate;
            LocalDateTime parsedEndDate;

            // 处理默认时间范围
            if (StringUtils.isBlank(startDateStr) || StringUtils.isBlank(endTimeStr)) {
                parsedEndDate = LocalDateTime.now();
                parsedStartDate = LocalDateTime.now().minusMonths(Constants.LATE_RETURN_STATISTICS_PERIOD_MONTH);
            } else {
                // 解析指定的时间范围
                parsedStartDate = LocalDateTime.parse(startDateStr, formatter);
                parsedEndDate = LocalDateTime.parse(endTimeStr, formatter);

                // 验证时间范围的有效性
                if (parsedEndDate.isAfter(LocalDateTime.now())) {
                    parsedEndDate = LocalDateTime.now();
                }

                if (parsedStartDate.isAfter(LocalDateTime.now())) {
                    logger.error("开始时间不能晚于当前时间");
                    throw new BusinessException(ResultCode.VALIDATE_FAILED);
                }

                if (parsedStartDate.isAfter(parsedEndDate)) {
                    logger.error("开始时间不能晚于结束时间");
                    throw new BusinessException(ResultCode.VALIDATE_FAILED);
                }
            }

            // 设置查询条件
            query.setStartLateTime(Timestamp.valueOf(parsedStartDate));
            query.setEndLateTime(Timestamp.valueOf(parsedEndDate));
            query.setStudentNos(targetStudentList.stream()
                    .map(Student::getStudentNo)
                    .toList());

        } catch (DateTimeParseException e) {
            logger.error("日期格式不正确，请使用yyyy-MM-dd HH:mm:ss格式");
            throw new BusinessException(ResultCode.VALIDATE_FAILED);
        }

        return query;
    }

    @Override
    public List<StudentLateResultDTO> selectByStuLateQueryInPeriod(StudentLateQueryDTO studentLateQueryDTO) {
        if (studentLateQueryDTO == null) {
            logger.error("查询参数不合法");
            throw new BusinessException(ResultCode.VALIDATE_FAILED);
        }

        Date startDate = studentLateQueryDTO.getStartDate();
        Date endDate = studentLateQueryDTO.getEndDate();
        String college = studentLateQueryDTO.getCollege();
        String className = studentLateQueryDTO.getClassName();

        return lateReturnMapper.selectByStuLateQueryInPeriod(startDate, endDate, college, className);

    }

    @Override
    public Integer countLateReturnsInPeriod(String studentNo, Date startTime, Date endTime) {
        if (StringUtils.isBlank(studentNo)) {
            logger.error("studentNo不合法：{}", studentNo);
            throw new BusinessException(ResultCode.VALIDATE_FAILED);
        }

        if (endTime.before(startTime)) {
            logger.error("时间范围不合法, startTime:{}, endTime{}", startTime, endTime);
            throw new BusinessException(ResultCode.VALIDATE_FAILED);
        }

        return lateReturnMapper.countStudentLateReturnsInPeriod(
                studentNo, startTime, endTime);
    }

    @Override
    public Integer countPeriodLateReturns(LateReturnQuery query) {
        return lateReturnMapper.countPeriodLateReturns(query);
    }

    @Override
    public Integer countPeriodLateReturnStudents(LateReturnQuery query) {
        return lateReturnMapper.countPeriodLateReturnStudents(query);
    }


    /**
     * 将未正当理由的晚归记录添加到Redis
     */
    private void addUnjustifiedLateReturnToRedis(LateReturn lateReturn) {
        String key = UNJUSTIFIED_LATE_RETURN_KEY + lateReturn.getStudentNo();
        String field = lateReturn.getLateReturnId();
        String value = String.valueOf(lateReturn.getLateTime().getTime());

        // 添加晚归记录
        stringRedisTemplate.opsForHash().put(key, field, value);
        // 将学生学号添加到Set中
        stringRedisTemplate.opsForSet().add(UNJUSTIFIED_STUDENTS_SET, lateReturn.getStudentNo());
    }

    /**
     * 从Redis中移除晚归记录
     */
    private void removeLateReturnFromRedis(String studentNo, String lateReturnId) {
        String key = UNJUSTIFIED_LATE_RETURN_KEY + studentNo;
        stringRedisTemplate.opsForHash().delete(key, lateReturnId);

        // 如果该学生没有其他未正当理由的晚归记录，则将studentNo 从Set移除
        if (stringRedisTemplate.opsForHash().size(key) == 0) {
            stringRedisTemplate.opsForSet().remove(UNJUSTIFIED_STUDENTS_SET, studentNo);
        }
    }

    /**
     *  统计指定时间段内有出现违规晚归的学生的学号
     * @param query 查询条件
     * @return      学号集合
     */
    @Override
    public Set<String> listPeriodLateReturnStudentNos(LateReturnQuery query) {
        return lateReturnMapper.listPeriodLateReturnStudentNos(query);
    }
}