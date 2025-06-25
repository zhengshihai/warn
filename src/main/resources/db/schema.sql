-- 创建数据库
CREATE DATABASE IF NOT EXISTS warn DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE warn;

-- 学生表
CREATE TABLE IF NOT EXISTS student (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    student_no VARCHAR(20) NOT NULL COMMENT '学号',
    name VARCHAR(50) NOT NULL COMMENT '姓名',
    college VARCHAR(100) NOT NULL COMMENT '学院',
    email VARCHAR(50) NOT NULL COMMENT '学生邮箱',
    class_name VARCHAR(50) NOT NULL COMMENT '班级',
    dormitory VARCHAR(50) NOT NULL COMMENT '宿舍号',
    phone VARCHAR(20) COMMENT '联系电话',
    password VARCHAR(100) NOT NULL COMMENT '登录密码',
    father_name VARCHAR(50) COMMENT '父亲姓名',
    father_phone VARCHAR(20) COMMENT '父亲电话',
    mother_name VARCHAR(50) COMMENT '母亲姓名',
    mother_phone VARCHAR(20) COMMENT '母亲电话',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    last_login_time DATETIME COMMENT '最后登录时间',
    UNIQUE KEY uk_student_no (student_no),
    UNIQUE KEY idx_student_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生信息表';

-- 晚归记录表
CREATE TABLE IF NOT EXISTS late_return (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    late_return_id VARCHAR(32) NOT NULL COMMENT '晚归记录ID', -- 额外设计的晚归记录id 格式：LR + 年月日 + 6位随机数
    student_no VARCHAR(20) NOT NULL COMMENT '学号',
    late_time DATETIME NOT NULL COMMENT '晚归时间',
    reason VARCHAR(500) COMMENT '晚归原因',
    process_status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '处理状态：PENDING-待处理/PROCESSING-处理中/FINISHED-已处理',
    process_result VARCHAR(20) COMMENT '处理结果：APPROVED-已通过/REJECTED-已驳回',
    process_remark VARCHAR(500) COMMENT '处理备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_late_return_id (late_return_id),
    KEY idx_student_no (student_no),
    KEY idx_late_time (late_time),
    UNIQUE KEY uk_late_return_id (late_return_id)
    -- CONSTRAINT fk_late_return_student FOREIGN KEY (student_no) REFERENCES student (student_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='晚归记录表';

-- 系统用户表（准确来说应该叫班级管理员表）
CREATE TABLE IF NOT EXISTS sys_user (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    password VARCHAR(100) NOT NULL COMMENT '密码',
    name VARCHAR(50) NOT NULL COMMENT '姓名',
    sys_user_no VARCHAR(40) NOT NULL COMMENT '工号',
    role VARCHAR(20) NOT NULL COMMENT '角色：辅导员/班主任/院级领导/其他角色 COUNSELOR/CLASS_TEACHER/DEAN/OTHER',
    phone VARCHAR(20) COMMENT '联系电话',
    email VARCHAR(100) COMMENT '邮箱',
    status VARCHAR(20) NOT NULL DEFAULT 'ENABLE' COMMENT '状态：ENABLE-启用/DISABLE-禁用',
    last_login_time DATETIME COMMENT '最后登录时间',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_email (email),
    UNIQUE KEY uk_sys_user_no (sys_user_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

-- 系统日志表
CREATE TABLE `system_log` (
    `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `log_id` VARCHAR(40) NOT NULL COMMENT '系统日志唯一ID',
    `user_no` VARCHAR(40) NOT NULL COMMENT '用户编号（学号 工号 等）',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名',
    `user_role` VARCHAR(32) NOT NULL COMMENT '用户角色',
    `operation` VARCHAR(255) NOT NULL COMMENT '操作内容',
    `method` VARCHAR(255) NOT NULL COMMENT '请求方法',
    `params` TEXT COMMENT '请求参数',
    `ip` VARCHAR(50) NOT NULL COMMENT 'IP地址',
    `status` VARCHAR(20) NOT NULL COMMENT '状态：成功/失败',
    `error_msg` TEXT COMMENT '错误信息',
    `create_time` DATETIME NOT NULL COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_log_id` (`log_id`),
    KEY `idx_user_no` (`user_no`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统日志表';

-- 预警规则表
CREATE TABLE IF NOT EXISTS warning_rule (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    rule_name VARCHAR(100) NOT NULL COMMENT '规则名称',
    time_range_days INT NOT NULL COMMENT '统计时间范围（天）',
    max_late_times INT NOT NULL COMMENT '最大晚归次数',
    notify_target VARCHAR(20) NOT NULL COMMENT '通知对象（STUDENT/COUNSELOR/PARENT）',
    notify_method VARCHAR(20) NOT NULL COMMENT '通知方式（SMS/EMAIL/WECHAT）',
    status VARCHAR(20) NOT NULL DEFAULT 'ENABLED' COMMENT '规则状态（ENABLED/DISABLED）',
    description VARCHAR(500) COMMENT '规则描述',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预警规则表';

-- 通知记录表
CREATE TABLE IF NOT EXISTS notification (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    notice_id VARCHAR(32) NOT NULL COMMENT '通知ID', -- 额外设计的通知id 格式：NT + 年月日 + 6位随机数
    title VARCHAR(200) NOT NULL COMMENT '通知标题',
    content TEXT NOT NULL COMMENT '通知内容',
    type VARCHAR(50) NOT NULL COMMENT '通知类型（系统通知/晚归通知/预警通知等）',
    target_type VARCHAR(20) NOT NULL COMMENT '目标类型（ALL/STUDENT/DORM_MANAGER/SYSTEM_USER等）',
    target_id VARCHAR(50) DEFAULT NULL COMMENT '目标ID（特定用户的唯一标识，如学号、工号等）',
    status VARCHAR(20) NOT NULL DEFAULT 'UNREAD' COMMENT '状态 已读/未读',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_target (target_type, target_id),
    UNIQUE KEY uk_notice_id (notice_id),
    KEY idx_type (type),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知记录表';

-- 宿管信息表
CREATE TABLE IF NOT EXISTS dormitory_manager (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    manager_id VARCHAR(20) NOT NULL COMMENT '工号',
    name VARCHAR(50) NOT NULL COMMENT '姓名',
    building VARCHAR(50) NOT NULL COMMENT '负责宿舍楼',
    phone VARCHAR(20) COMMENT '联系电话',
    email VARCHAR(100) COMMENT '电子邮箱',
    status VARCHAR(20) NOT NULL DEFAULT 'ON_DUTY' COMMENT '状态：ON_DUTY-在职/OFF_DUTY-离职',
    password VARCHAR(100) NOT NULL COMMENT '登录密码',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    last_login_time DATETIME COMMENT '最后登录时间',
    UNIQUE KEY uk_manager_id (manager_id),
    UNIQUE KEY uk_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='宿管信息表';

-- 系统规则表
CREATE TABLE IF NOT EXISTS system_rule (
    id INT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    rule_key VARCHAR(50) NOT NULL COMMENT '规则键',
    rule_value VARCHAR(500) NOT NULL COMMENT '规则值',
    description VARCHAR(500) COMMENT '规则描述',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_rule_key (rule_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统规则表';

-- 晚归报备申请表
CREATE TABLE application (
    id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    application_id VARCHAR(32) NOT NULL COMMENT '晚归报备申请ID', -- 额外设计的晚归报备申请id 格式：AP + 年月日 + 6位随机数
    student_no VARCHAR(20) NOT NULL COMMENT '学号',
    expected_return_time DATETIME DEFAULT NULL COMMENT '预期回校时间',
    reason VARCHAR(50) DEFAULT NULL COMMENT '外出简要原因',
    description VARCHAR(500) DEFAULT NULL COMMENT '外出详细原因',
    destination VARCHAR(255) DEFAULT NULL COMMENT '外出地点',
    attachment_url VARCHAR(500) DEFAULT NULL COMMENT '附件URL',
    apply_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    audit_status TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '审核状态(0待审核,1通过,2驳回)',
    audit_time DATETIME DEFAULT NULL COMMENT '审核时间',
    audit_person VARCHAR(50) DEFAULT NULL COMMENT '审核人(宿管 辅导员 班主任等）',
    audit_remark VARCHAR(500) DEFAULT NULL COMMENT '审核备注',
    UNIQUE KEY uk_application_id (application_id),
    INDEX idx_student_id (student_no),
    INDEX idx_expected_date (expected_return_time),
    INDEX idx_audit_status (audit_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='晚归报备表';

-- 晚归情况说明表
CREATE TABLE explanation (
     id BIGINT UNSIGNED AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
     explanation_id VARCHAR(32) NOT NULL COMMENT '晚归情况说明ID', -- 额外设计的晚归情况说明id 格式：ES + 年月日 + 6位随机数
     late_return_id BIGINT UNSIGNED NOT NULL COMMENT '晚归记录ID',
     student_no VARCHAR(20) NOT NULL COMMENT '学号',
     description VARCHAR(1000) NOT NULL COMMENT '情况说明内容',
     attachment_url VARCHAR(500) DEFAULT NULL COMMENT '附件URL',
     submit_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
     audit_status TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '审核状态(0待审核,1通过,2驳回)',
     audit_time DATETIME DEFAULT NULL COMMENT '审核时间',
     audit_person VARCHAR(50) DEFAULT NULL COMMENT '审核人(宿管 辅导员 班主任 院级主任等）',
     audit_remark VARCHAR(500) DEFAULT NULL COMMENT '审核备注',
     
     UNIQUE KEY uk_application_id (explanation_id),
     INDEX idx_late_warning_id (late_return_id),
     INDEX idx_student_id (student_no),
     INDEX idx_audit_status (audit_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='晚归情况说明表';

-- 系统用户-班级关联表
CREATE TABLE `sys_user_class` (
      `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
      `sys_user_no` VARCHAR(50) NOT NULL COMMENT '用户编号',
      `class_name` VARCHAR(50) NOT NULL COMMENT '班级',
      `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
      `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
      PRIMARY KEY (`id`),
      UNIQUE KEY `uk_user_class` (`sys_user_no`, `class_name`),
      KEY `idx_sys_user_no` (`sys_user_no`),
      KEY `idx_student_class` (`class_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户-班级关联表';


-- 学生晚归统计表
CREATE TABLE `student_late_stats` (
      `id` INT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
      `stats_id` VARCHAR(32) NOT NULL COMMENT '统计ID',
      `student_no` VARCHAR(20) NOT NULL COMMENT '学生学号',
      `late_return_count` INT NOT NULL DEFAULT 0 COMMENT '统计周期内的晚归次数',
      `stats_period_start_date` DATETIME NOT NULL COMMENT '统计周期的开始日期',
      `stats_period_end_date` DATETIME NOT NULL COMMENT '统计周期的结束日期',
      `stats_period_type` VARCHAR(20) NOT NULL COMMENT '统计周期类型，分固定月份和滑动月份等例如 "FIXED_MONTHLY", "LAST_30_DAYS", ',
      `last_updated_time` DATETIME NOT NULL COMMENT '最后更新时间',
      `school_year` VARCHAR(20) NOT NULL COMMENT '学年，例如 "2023-2024"',
      `semester` VARCHAR(20) NOT NULL COMMENT '学期，例如 "FALL"（下学期）, "SPRING"（上学期）',
      `active_status` TINYINT NOT NULL DEFAULT 0 COMMENT '标记这条统计是否为当前"活跃"或"最新"的统计 0表示不活跃 1表示活跃',
      PRIMARY KEY (`id`),
      UNIQUE KEY `uk_stats_id` (`stats_id`),
      KEY `idx_student_no` (`student_no`),
      KEY `idx_stats_period` (`stats_period_start_date`, `stats_period_end_date`),
      KEY `idx_school_year_semester` (`school_year`, `semester`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生晚归统计表';

-- 超级管理员表
CREATE TABLE super_admin (
     `id` INT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
     `name` VARCHAR(64) NOT NULL COMMENT '管理员名称',
     `password` VARCHAR(100) NOT NULL COMMENT '密码（加密存储）',
     `email` VARCHAR(128) NOT NULL COMMENT '邮箱地址',
     `enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用（0：禁用，1：启用）',
     `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
     `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
     `last_login_time` DATETIME COMMENT '最后登录时间',
     `version` INT DEFAULT 0 COMMENT '乐观锁版本号',
    UNIQUE KEY `uk_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4  COMMENT='超级管理员表';



-- 报警记录表
CREATE TABLE alarm_record (
      id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
      alarm_no VARCHAR(20) NOT NULL COMMENT '报警记录ID',
      student_no VARCHAR(20) NOT NULL COMMENT '学生ID',
      alarm_type TINYINT NOT NULL COMMENT '报警类型：1-一键报警，2-定时报警，3-区域报警',
      alarm_level TINYINT NOT NULL COMMENT '报警级别：1-普通，2-紧急',
      alarm_status TINYINT NOT NULL DEFAULT 0 COMMENT '处理状态：0-未处理，1-处理中，2-已处理，3-已关闭',
      alarm_time DATETIME NOT NULL COMMENT '报警时间',
      latitude DECIMAL(10,6) COMMENT '纬度',
      longitude DECIMAL(10,6) COMMENT '经度',
      location_address VARCHAR(255) COMMENT '位置描述',
      description TEXT COMMENT '报警描述',
      media_urls JSON COMMENT '媒体文件URL列表',
      created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
      updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
     -- FOREIGN KEY (student_no) REFERENCES student(student_no),
      UNIQUE KEY uk_alarm_no (alarm_no),
      INDEX idx_student_no (student_no),
      INDEX idx_alarm_status (alarm_status),
      INDEX idx_alarm_time (alarm_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报警记录表';

-- 报警处理记录表
CREATE TABLE alarm_process_record (
      id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
      alarm_no VARCHAR(20) NOT NULL COMMENT '报警记录ID',
      handler_type TINYINT NOT NULL COMMENT '处理方类型：1-学校安保，2-警方，3-医疗',
      handler_id VARCHAR(50) COMMENT '处理方ID',
      handler_name VARCHAR(50) COMMENT '处理人姓名',
      process_status TINYINT NOT NULL COMMENT '处理状态：0-待处理，1-处理中，2-已处理，3-已关闭',
      process_result TEXT COMMENT '处理结果',
      process_time DATETIME COMMENT '处理时间',
      created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
      updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
      -- FOREIGN KEY (alarm_no) REFERENCES alarm_record(alarm_no),
      INDEX idx_alarm_no (alarm_no),
      INDEX idx_handler_type (handler_type),
      INDEX idx_handler_id (handler_id),
      INDEX idx_process_status (process_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报警处理记录表';

-- 位置轨迹表
CREATE TABLE location_track (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    alarm_no VARCHAR(20) NOT NULL COMMENT '报警记录ID',
    latitude DECIMAL(10,6) NOT NULL COMMENT '纬度',
    longitude DECIMAL(10,6) NOT NULL COMMENT '经度',
    location_accuracy DECIMAL(10,2) COMMENT '精确度(米)',
    speed DECIMAL(10,2) COMMENT '速度(米/秒)',
    direction DECIMAL(10,2) COMMENT '方向(度)',
    location_time DATETIME NOT NULL COMMENT '时间戳',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    first_location_time DATETIME COMMENT '静止段起始时间',
    end_location_time DATETIME COMMENT '静止段结束时间',
    change_unit INT COMMENT '前端上报间隔（秒）',
   -- FOREIGN KEY (alarm_no) REFERENCES alarm_record(alarm_no),
    INDEX idx_alarm_no (alarm_no),
    INDEX idx_alarm_no_timestamp (alarm_no, location_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='位置轨迹表';

-- 报警配置表
CREATE TABLE alarm_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    api_provider VARCHAR(50) NOT NULL COMMENT 'API提供商标识',
    api_key VARCHAR(100) NOT NULL COMMENT 'API密钥',
    api_secret TEXT NOT NULL COMMENT 'API密钥',
    description VARCHAR(255) COMMENT '配置描述',
    is_active TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用：0-禁用，1-启用',
    version INT NOT NULL DEFAULT 1 COMMENT '版本号',
    last_modified_by VARCHAR(50) COMMENT '最后修改人',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_api_provider (api_provider)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报警配置表';

-- 报警处理方配置表
CREATE TABLE alarm_handler_config (
      id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
      handler_type TINYINT NOT NULL COMMENT '处理方类型：1-学校安保，2-警方，3-医疗',
      handler_name VARCHAR(50) NOT NULL COMMENT '处理方名称',
      api_url VARCHAR(255) NOT NULL COMMENT '接口地址',
      api_key VARCHAR(100) COMMENT '接口密钥',
      timeout INT NOT NULL DEFAULT 5000 COMMENT '超时时间(毫秒)',
      priority INT NOT NULL DEFAULT 0 COMMENT '优先级',
      is_active TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用：0-禁用，1-启用',
      created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
      updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
      UNIQUE KEY uk_handler_type (handler_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报警处理方配置表';
