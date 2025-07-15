package com.tianhai.warn.listeners;

import com.tianhai.warn.constants.Constants;
import com.tianhai.warn.constants.WarningMessageTemplates;
import com.tianhai.warn.events.StatsEvent;
import com.tianhai.warn.model.*;
import com.tianhai.warn.service.*;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 统计事件监听类
 */
@Component
public class StatsEventListener implements ApplicationListener<StatsEvent> {
    private static final Logger logger = LoggerFactory.getLogger(StatsEventListener.class);

    @Autowired
    private StudentLateStatsService studentLateStatsService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private LateReturnService lateReturnService;

    @Autowired
    private WarningRuleService warningRuleService;

    @Autowired
    private StudentService studentService;

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private SysUserClassService sysUserClassService;

    @Autowired
    private SmsService smsService;

    @Autowired
    private AsyncTaskExecutor asyncTaskExecutor;

    @Autowired
    private Environment environment;

    public StatsEventListener() {
        logger.info("StatsEventListener initialized");
    }

    @Override
    public void onApplicationEvent(@NonNull StatsEvent event) {
        logger.info("Received StatsEvent");
        if (asyncTaskExecutor == null) {
            logger.error("AsyncTaskExecutor is null!");
            return;
        }
        logger.info("AsyncTaskExecutor class: {}", asyncTaskExecutor.getClass().getName());
        asyncTaskExecutor.execute(() -> {
            logger.info("Starting async processing of StatsEvent");
            try {
                processStatsEventAsync(event);
            } catch (Exception e) {
                logger.error("响应统计事件失败", e);
            }
        });
    }

    /**
     * 获取当前运行环境
     * 
     * @return 运行环境
     */
    private String getCurActiveProfile() {
        String[] activeProfiles = environment.getActiveProfiles();
        String activeProfile = null;
        if (activeProfiles.length > 0) {
            activeProfile = activeProfiles[0]; // 一般只有一个环境
        }

        return activeProfile;
    }

    /*
     * 异步方式处理
     */
    private void processStatsEventAsync(StatsEvent event) {
        String studentNo = null;

        // 根据事件类型获取学生学号 todo 此处待优化
        if (event.getAuditActionDTO() != null) {
            studentNo = event.getAuditActionDTO().getStudentNo();
        } else if (event.getProcessActionDTO() != null) {
            studentNo = event.getProcessActionDTO().getStudentNo();
        }

        if (studentNo == null) {
            logger.error("所有事件都未包含学生学号信息");
            return;
        }

        logger.info("Processing StatsEvent for student: {}", studentNo);

        // 获取当前运行环境
        String activeProfile = getCurActiveProfile();

        // 获取学生基本信息
        Student student = studentService.selectByStudentNo(studentNo);
        if (student == null) {
            logger.error("未找到学号为{}的学生信息", studentNo);
            return;
        }

        // 并发获取班级管理员信息
        CompletableFuture<List<SysUser>> managersFuture = CompletableFuture
                .supplyAsync(() -> getActiveClassManagers(student.getClassName()), asyncTaskExecutor);

        // 并发获取预警规则
        CompletableFuture<Map<Integer, WarningRule>> warnRuleFuture = CompletableFuture
                .supplyAsync(this::getWarningRule, asyncTaskExecutor);

        // 预警规则
        Map<Integer, WarningRule> warningRuleMap;
        try {
            warningRuleMap = warnRuleFuture.get();

            // 去除不合规的预警规则
            warningRuleMap.entrySet().removeIf(entry -> {
                WarningRule rule = entry.getValue();
                return rule.getTimeRangeDays() == null || rule.getTimeRangeDays() < 1
                        || rule.getMaxLateTimes() == null || rule.getMaxLateTimes() < 1
                        || StringUtils.isBlank(rule.getNotifyTarget().trim());
            });

            if (warningRuleMap.isEmpty()) {
                logger.warn("未设置有效的预警规则");
                return;
            }
        } catch (Exception e) {
            logger.error("获取晚归预警规则信息失败", e);
            return;
        }

        // 获取该生触发的预警规则
        // 获取预警规则中定义的天数规则，例如规定了7天 2天 30天等
        List<Integer> timeRangeDaysList = new ArrayList<>(warningRuleMap.keySet());
        // 键为时间天数范围， 值为该时间范围不合理的晚归次数
        Map<Integer, Integer> lateReturnStatsMap = studentLateStatsService.getLateReturnCountsInDaysRange(studentNo,
                timeRangeDaysList);
        // 该生触发的晚归预警规则
        List<WarningRule> violatedRules = lateReturnStatsMap.entrySet().stream()
                .map(entry -> {
                    Integer timeRangeDays = entry.getKey();
                    Integer lateCounts = entry.getValue();
                    WarningRule rule = warningRuleMap.get(timeRangeDays);
                    if (rule != null && lateCounts >= rule.getMaxLateTimes()) {
                        return rule;
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .toList();

        // 非生产环境下输出violateRules便于调试
        if (!activeProfile.equalsIgnoreCase("prod")) {
            logger.info("该生触发了预警规则：{}", violatedRules);
        }

        // 发送邮件或短信通知
        List<SysUser> classManagers;
        try {
            classManagers = managersFuture.get();
            if (classManagers.isEmpty()) {
                logger.warn("未设置该生的班级管理员信息");
                return;
            }
        } catch (Exception e) {
            logger.error("获取学生班级管理员信息异常，该生学号为: {}", studentNo);
            return;
        }

        // 生成具体预警消息内容并发送站外预警消息
        CompletableFuture.supplyAsync(() -> generateWarnMessages(student, violatedRules), asyncTaskExecutor)
                .thenAcceptAsync(messages -> sendWarningMessages(student, violatedRules, messages, classManagers));

        // todo 发送站内通知

        // todo 加入重点关注名单
    }

    /**
     * 获取预警规则
     */
    private Map<Integer, WarningRule> getWarningRule() {
        // 获取预警规则
        WarningRule condition = WarningRule.builder()
                .status(Constants.ENABLE_STR)
                .build();
        List<WarningRule> warnRules = warningRuleService.selectByCondition(condition);

        // 以timeRangeDays作为键
        Map<Integer, WarningRule> warnRulesMap = new HashMap<>();
        for (WarningRule rule : warnRules) {
            warnRulesMap.put(rule.getTimeRangeDays(), rule);
        }

        return warnRulesMap;
    }

    /**
     * 构建预警消息
     */
    private static final String STUDENT = "STUDENT";
    private static final String COUNSELOR = "COUNSELOR";
    private static final String PARENT = "PARENT";
    private static final String CLASS_TEACHER = "CLASSTEACHER";

    /**
     * 生成预警消息
     * 如果只触发一条预警规则，则使用直接使用具体的通知模板，
     * 反之 将发给同一通知接收对象的多条通知合并
     *
     * @param student       学生信息
     * @param violatedRules 触发的预警规则列表
     * @return 不同通知目标对应的预警消息
     */
    private Map<String, String> generateWarnMessages(Student student,
            List<WarningRule> violatedRules) {
        Map<String, String> messages = new HashMap<>();
        String studentNo = student.getStudentNo();
        String studentName = student.getName();
        String parentName = StringUtils.isBlank(student.getFatherName())
                ? student.getMotherName()
                : student.getFatherName();
        if (StringUtils.isBlank(parentName)) {
            parentName = studentName + "的家长";
        }

        // 处理只触发单个预警规则的情况
        // eg. "请注意，你在最近7天内的不正当晚归次数超过规定次数，请遵守学校制度"
        if (violatedRules.size() == 1) {
            WarningRule rule = violatedRules.get(0);
            String notifyTarget = rule.getNotifyTarget();
            int timeRangeDays = rule.getTimeRangeDays();

            // 将具体数据填装入消息模板
            switch (notifyTarget) {
                case STUDENT -> messages.put(STUDENT,
                        String.format(WarningMessageTemplates.STUDENT_SINGLE_TEMPLATE, timeRangeDays));

                case COUNSELOR -> messages.put(COUNSELOR,
                        String.format(WarningMessageTemplates.COUNSELOR_SINGLE_TEMPLATE, studentNo, timeRangeDays));

                case PARENT -> messages.put(PARENT,
                        String.format(WarningMessageTemplates.PARENT_SINGLE_TEMPLATE, parentName, studentName,
                                timeRangeDays));

                case CLASS_TEACHER -> messages.put(CLASS_TEACHER,
                        String.format(WarningMessageTemplates.CLASS_TEACHER_SINGLE_TEMPLATE, studentNo, timeRangeDays));

                default -> logger.warn("未知的通知目标: {}", notifyTarget);
            }
            return messages;
        }

        // 处理多个规则的情况
        // eg. "请注意，你在最近3、7内的不正当晚归次数均超过规定次数，请严格遵守学校制度"
        String timeRangeStr = violatedRules.stream()
                .map(WarningRule::getTimeRangeDays)
                .map(String::valueOf)
                .collect(Collectors.joining("天、")) + "天"; // 避免最后一个"天"为"天、"

        // 将具体数据填装入消息模板
        String studentMsg = String.format(WarningMessageTemplates.STUDENT_MULTIPLE_TEMPLATE,
                timeRangeStr);

        String counselorMsg = String.format(WarningMessageTemplates.COUNSELOR_MULTIPLE_TEMPLATE,
                studentNo, timeRangeStr);

        String parentMsg = String.format(WarningMessageTemplates.PARENT_MULTIPLE_TEMPLATE,
                parentName, studentName, timeRangeStr);

        String classTeacherMsg = String.format(WarningMessageTemplates.CLASS_TEACHER_MULTIPLE_TEMPLATE,
                studentNo, timeRangeStr);

        messages.put(STUDENT, studentMsg);
        messages.put(COUNSELOR, counselorMsg);
        messages.put(PARENT, parentMsg);
        messages.put(CLASS_TEACHER, classTeacherMsg);

        return messages;
    }

    /**
     * 发送预警消息
     * 如果触发多条预警规则，而且不同预警规则有不同发送方式，则会使用合并的预警消息，使用全部发送方式发送
     *
     * @param student       学生信息
     * @param violatedRules 触发的预警规则列表
     * @param messages      不同角色的消息内容
     * @param classManagers 管理该学生的辅导员 班主任
     */
    private void sendWarningMessages(Student student,
            List<WarningRule> violatedRules,
            Map<String, String> messages,
            List<SysUser> classManagers) {
        // 获取辅导员 班主任信息 (每个学生只有被一个辅导员 一个班主任管理）
        SysUser counselor = null, classTeacher = null;
        for (SysUser classManager : classManagers) {
            if (classManager.getJobRole().equalsIgnoreCase(Constants.JOB_ROLE_COUNSELOR)) {
                counselor = classManager;
            }

            if (classManager.getJobRole().equalsIgnoreCase(Constants.JOB_ROLE_CLASS_TEACHER)) {
                classTeacher = classManager;
            }

            // 目前仅支持向辅导员 班主任发送站外预警信息
            if (counselor != null && classTeacher != null) {
                break;
            }
        }

        // 按通知目标分组，收集所有通知方式
        Map<String, Set<String>> targetToMethods = new HashMap<>();

        // 收集每个通知目标的所有通知方式
        for (WarningRule rule : violatedRules) {
            String notifyTarget = rule.getNotifyTarget();
            String notifyMethod = rule.getNotifyMethod();

            targetToMethods.computeIfAbsent(notifyTarget, key -> new HashSet<>())
                    .add(notifyMethod);
        }

        // 对每个通知目标，使用所有收集到的通知方式发送消息
        for (Map.Entry<String, Set<String>> entry : targetToMethods.entrySet()) {
            String notifyTarget = entry.getKey();
            Set<String> notifyMethods = entry.getValue();
            String message = messages.getOrDefault(notifyTarget, null);

            if (StringUtils.isBlank(message)) {
                logger.warn("未找到通知目标[{}]对应的消息内容", notifyTarget);
                continue;
            }

            try {
                switch (notifyTarget) {
                    case STUDENT -> sendMessageToStudent(student, notifyMethods, message);

                    case COUNSELOR -> sendMessageToClassManager(counselor, notifyMethods, message);

                    case PARENT -> sendMessageToParent(student, notifyMethods, message);

                    case CLASS_TEACHER -> sendMessageToClassManager(classTeacher, notifyMethods, message);

                    default -> logger.warn("未知的通知目标: {}", notifyTarget);
                }
            } catch (Exception e) {
                logger.error("发送消息失败 - 目标: {}, 方式: {}, 错误: {}",
                        notifyTarget, notifyMethods, e.getMessage(), e);
            }
        }
    }

    /**
     * 通用消息发送处理
     *
     * @param receiverId    接收者唯一标识（如studentNo, sysUserNo）
     * @param phone         接收者手机号
     * @param email         接收者邮箱
     * @param notifyMethods 通知方式集合（如 SMS、EMAIL）
     * @param message       消息内容
     * @param logTag        日志标识（学生、家长、辅导员等）
     */
    private void sendMessage(String receiverId, String phone, String email,
            Set<String> notifyMethods, String message, String logTag) {
        for (String method : notifyMethods) {
            try {
                switch (method) {
                    case Constants.EMAIL -> {
                        if (StringUtils.isNotBlank(email)) {
                            emailService.send(email, "晚归预警通知", message, false);
                        } else {
                            logger.warn("{} 邮箱为空，无法发送邮件", logTag);
                        }
                    }

                    case Constants.SMS -> {
                        if (StringUtils.isNotBlank(phone)) {
                            smsService.sendMessage(phone, message);
                        } else {
                            logger.warn("{} 手机号为空，无法发送短信", logTag);
                        }
                    }

                    default -> logger.warn("{} 暂不支持的通知方式: {}", logTag, method);
                }
            } catch (Exception e) {
                logger.error("发送消息失败 - {} [{}] - 方式: {}, 错误: {}",
                        logTag, receiverId, method, e.getMessage());
            }
        }
    }

    /**
     * 非生产环境替换接收邮箱
     * 
     * @param prodEmailAddress 生产环境的接收邮箱
     * @return 目标邮箱
     */
    private String getTargetEmail(String prodEmailAddress) {
        String targetEmailAddress = prodEmailAddress;

        // 非生产环境替换接受邮箱地址
        if (getCurActiveProfile().equalsIgnoreCase("dev")) {
            targetEmailAddress = Constants.EMAIL_DEV_RECEIVER;
        }
        if (getCurActiveProfile().equalsIgnoreCase("test")) {
            targetEmailAddress = Constants.EMAIL_DEV_RECEIVER;
        }

        return targetEmailAddress;
    }

    /**
     * 发送消息给学生
     */
    private void sendMessageToStudent(Student student, Set<String> methods, String message) {
        String targetEmail = getTargetEmail(student.getEmail());
        sendMessage(student.getStudentNo(), student.getPhone(), targetEmail,
                methods, message, STUDENT);
    }

    /**
     * 发送消息给辅导员或班主任
     */
    private void sendMessageToClassManager(SysUser classManager, Set<String> methods, String message) {
        if (classManager == null) {
            logger.error("没有为该学生设置辅导员或班主任信息");
            return;
        }

        String targetEmailAddress = getTargetEmail(classManager.getEmail());
        sendMessage(classManager.getSysUserNo(), classManager.getPhone(), targetEmailAddress,
                methods, message, classManager.getJobRole());
    }

    /**
     * 发送消息给家长（目前仅支持短信）
     */
    private void sendMessageToParent(Student student, Set<String> methods, String message) {
        String phone = StringUtils.defaultIfBlank(student.getFatherPhone(), student.getMotherPhone());
        if (StringUtils.isBlank(phone)) {
            logger.error("学号为 {} 的学生没有留家长电话", student.getStudentNo());
            return;
        }
        if (methods.contains(Constants.SMS)) {
            sendMessage(student.getStudentNo(), phone, null, Set.of(Constants.SMS), message, "家长");
        } else {
            logger.warn("家长通知暂不支持除短信外的其他方式");
        }
    }

    /**
     * 获取该班级活跃的班级管理员（仅限于辅导员和班主任两种角色）
     */
    private List<SysUser> getActiveClassManagers(String className) {
        logger.info("Getting active class managers for class: {}", className);

        // 获取sysUserClass列表
        List<SysUserClass> sysUserClassList = sysUserClassService.getSysUserClassListByClassName(className);
        logger.info("Found {} sysUserClass records", sysUserClassList.size());
        if (!sysUserClassList.isEmpty()) {
            logger.info("First sysUserClass record: {}", sysUserClassList.get(0));
        }

        // 从sysUserClass列表中获取sysUserNo列表
        List<String> sysUserNoList = sysUserClassList.stream()
                .map(SysUserClass::getSysUserNo)
                .toList();
        logger.info("sysUserNoList size: {}, content: {}", sysUserNoList.size(), sysUserNoList);

        // 利用sysUserNo列表获取sysUser完整信息
        List<SysUser> result = sysUserService.selectBySysUserNos(sysUserNoList)
                .stream()
                .filter(sysUser -> sysUser != null
                        && Constants.ENABLE_STR.equals(sysUser.getStatus())
                        && (Constants.JOB_ROLE_COUNSELOR.equals(sysUser.getJobRole())
                                || Constants.JOB_ROLE_CLASS_TEACHER.equals(sysUser.getJobRole())))
                .toList();
        logger.info("Found {} active class managers after filtering", result.size());
        if (!result.isEmpty()) {
            logger.info("First result user: {}", result.get(0));
        }

        return result;
    }

}
