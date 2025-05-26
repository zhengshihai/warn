-- 插入测试学生数据
INSERT INTO student (student_no, name, college, class_name, dormitory, phone, father_name, father_phone, mother_name, mother_phone, email, password) VALUES
('2021001', '张三', '计算机学院', '计算机2101', 'A-101', '13800138001', '张父', '13900139001', '张母', '13700137001', 'zhangsan2021001@example.com', '$2a$10$X/hX5k2XZQf1YQqB1YQqB1YQqB1YQqB1YQqB1YQqB1YQqB1YQqB1YQ'),
('2021002', '李四', '计算机学院', '计算机2101', 'A-101', '13800138002', '李父', '13900139002', '李母', '13700137002', 'lisi2021002@example.com', '$2a$10$X/hX5k2XZQf1YQqB1YQqB1YQqB1YQqB1YQqB1YQqB1YQqB1YQqB1YQ'),
('2021003', '王五', '机械学院', '机械2101', 'B-101', '13800138003', '王父', '13900139003', '王母', '13700137003', 'wangwu2021003@example.com', '$2a$10$X/hX5k2XZQf1YQqB1YQqB1YQqB1YQqB1YQqB1YQqB1YQqB1YQqB1YQ'),
('2021004', '赵六', '机械学院', '机械2101', 'B-101', '13800138004', '赵父', '13900139004', '赵母', '13700137004', 'zhaoliu2021004@example.com', '$2a$10$X/hX5k2XZQf1YQqB1YQqB1YQqB1YQqB1YQqB1YQqB1YQqB1YQqB1YQ'),
('2021005', '钱七', '经济学院', '经济2101', 'C-101', '13800138005', '钱父', '13900139005', '钱母', '13700137005', 'qianqi2021005@example.com', '$2a$10$X/hX5k2XZQf1YQqB1YQqB1YQqB1YQqB1YQqB1YQqB1YQqB1YQqB1YQ');

-- 插入测试晚归记录
INSERT INTO late_return (student_no, late_time, reason, process_status, process_result, process_remark, create_time, update_time) VALUES
('2420710220', '2025-03-20 23:15:00', '参加社团活动', 'PENDING', NULL, NULL, NOW(), NOW()),
('2420710220', '2025-03-25 23:30:00', '图书馆学习', 'PENDING', NULL, NULL, NOW(), NOW()),
('2420710220', '2025-03-15 23:45:00', '参加比赛', 'PROCESSING', NULL, '已提交说明，等待审核', NOW(), NOW()),
('2420710220', '2025-03-28 23:20:00', '实验课', 'PROCESSING', NULL, '已提交说明，等待审核', NOW(), NOW()),
('2420710220', '2025-03-10 23:10:00', '参加讲座', 'FINISHED', 'APPROVED', '理由充分，予以通过', NOW(), NOW()),
('2420710220', '2025-03-05 23:25:00', '参加比赛', 'FINISHED', 'APPROVED', '有证明材料，予以通过', NOW(), NOW()),
('2420710220', '2025-03-01 23:40:00', '外出游玩', 'FINISHED', 'REJECTED', '理由不充分，予以驳回', NOW(), NOW()),
('2420710220', '2025-02-28 23:35:00', '朋友聚会', 'FINISHED', 'REJECTED', '无特殊情况，予以驳回', NOW(), NOW()),
('2420710220', '2025-04-01 23:20:00', '参加比赛', 'PENDING', NULL, NULL, NOW(), NOW()),
('2420710220', '2025-04-05 23:15:00', '图书馆学习', 'PROCESSING', NULL, '已提交说明，等待审核', NOW(), NOW()),
('2420710220', '2025-04-10 23:30:00', '实验课', 'FINISHED', 'APPROVED', '有证明材料，予以通过', NOW(), NOW());

-- 插入系统管理员
INSERT INTO sys_user (sys_user_no, password, name, role, phone, email, status) VALUES
('sysuser1', '$2a$10$X/hX5k2XZQf1YQqB1YQqB1YQqB1YQqB1YQqB1YQqB1YQqB1YQqB1YQ', '系统管理员', 'ADMIN', '13800138000', 'admin@example.com', 'ENABLE'),
('sysuser2', '$2a$10$X/hX5k2XZQf1YQqB1YQqB1YQqB1YQqB1YQqB1YQqB1YQqB1YQqB1YQ', '张管理', 'DORMITORY', '13800138001', 'dormitory1@example.com', 'ENABLE'),
('sysuser3', '$2a$10$X/hX5k2XZQf1YQqB1YQqB1YQqB1YQqB1YQqB1YQqB1YQqB1YQqB1YQ', '李管理', 'DORMITORY', '13800138002', 'dormitory2@example.com', 'ENABLE');
-- 插入预警规则测试数据
INSERT INTO warning_rule (rule_name, time_range_days, max_late_times, notify_target, notify_method, status, description) VALUES
('一周三次晚归预警', 7, 3, 'STUDENT', 'WECHAT', 'ENABLED', '学生一周内晚归三次，发送微信提醒'),
('连续晚归预警', 2, 2, 'COUNSELOR', 'EMAIL', 'ENABLED', '学生连续两天晚归，通知辅导员'),
('月度晚归预警', 30, 5, 'PARENT', 'SMS', 'ENABLED', '学生一个月内晚归五次，短信通知家长'),
('特殊时期预警', 1, 1, 'COUNSELOR', 'SMS', 'DISABLED', '特殊时期一次晚归即通知辅导员');

-- 插入宿管信息测试数据
INSERT INTO dormitory_manager (manager_id, name, building, phone, status, password) VALUES
('DM001', '张宿管', 'A栋', '13811112222', 'ON_DUTY', '$2a$10$X/hX5k2XZQf1YQqB1YQqB1YQqB1YQqB1YQqB1YQqB1YQqB1YQqB1YQ'),
('DM002', '李宿管', 'B栋', '13822223333', 'ON_DUTY', '$2a$10$X/hX5k2XZQf1YQqB1YQqB1YQqB1YQqB1YQqB1YQqB1YQqB1YQqB1YQ'),
('DM003', '王宿管', 'C栋', '13833334444', 'ON_DUTY', '$2a$10$X/hX5k2XZQf1YQqB1YQqB1YQqB1YQqB1YQqB1YQqB1YQqB1YQqB1YQ');

-- 通知信息表模拟数据
INSERT INTO notification (title, content, type, target_type, target_id, status, create_time, update_time) VALUES
('系统维护通知', '系统将于本周六凌晨2点进行维护，请提前保存数据。', '系统通知', 'ALL', NULL, 'UNREAD', NOW(), NOW()),
('晚归提醒', '你昨日晚归已被记录，请注意遵守宿舍管理规定。', '晚归通知', 'STUDENT', '2021001', 'UNREAD', NOW(), NOW()),
('预警提醒', '你本周晚归次数已达3次，请及时与辅导员沟通。', '预警通知', 'STUDENT', '2021002', 'UNREAD', NOW(), NOW()),
('新公告', '本学期评优评先活动开始报名，请同学们关注。', '系统通知', 'ALL', NULL, 'READ', NOW(), NOW()),
('晚归处理结果', '你的晚归申请已被宿管审批通过。', '晚归通知', 'STUDENT', '2021003', 'READ', NOW(), NOW()),
('宿管通知', '请本栋宿管本周五下午参加安全培训。', '系统通知', 'DORM_MANAGER', 'DM001', 'UNREAD', NOW(), NOW()),
('管理员提醒', '有新的用户注册待审核。', '系统通知', 'ADMIN', 'admin001', 'UNREAD', NOW(), NOW()),
('晚归提醒', '你昨日晚归已被记录，请注意遵守宿舍管理规定。', '晚归通知', 'STUDENT', '2021004', 'READ', NOW(), NOW());

-- 插入系统规则测试数据
INSERT INTO system_rule (rule_key, rule_value, description) VALUES
('LATE_RETURN_TIME', '23:00:00', '晚归判定时间，超过该时间视为晚归'),
('MAX_CONTINUOUS_LATE', '3', '连续晚归最大次数，超过将通知辅导员'),
('NOTIFICATION_TEMPLATE_STUDENT', '同学你好，系统检测到你已经晚归{count}次，请注意遵守宿舍规定。', '学生通知模板'),
('NOTIFICATION_TEMPLATE_COUNSELOR', '您负责的学生{name}已经出现{count}次晚归，请及时关注。', '辅导员通知模板'),
('NOTIFICATION_TEMPLATE_PARENT', '您好，您的孩子{name}已经出现{count}次晚归情况，请知悉。', '家长通知模板'); 


-- 晚归报备申请数据
INSERT INTO late_return_application 
(student_no, expected_return_time, reason, destination, apply_time, audit_status, audit_time, audit_person, audit_remark) VALUES
-- 已通过的申请
('2420710220', '2025-03-10 23:30:00', '参加学术讲座', '图书馆报告厅', '2025-03-10 18:00:00', 1, '2025-03-10 22:00:00', '张宿管', '已核实，予以通过'),
('2420710220', '2025-03-05 23:30:00', '参加编程比赛', '计算机学院实验室', '2025-03-05 19:00:00', 1, '2025-03-05 22:30:00', '李宿管', '有比赛证明，予以通过'),
('2420710220', '2025-04-10 23:45:00', '参加实验课', '物理实验室', '2025-04-10 20:00:00', 1, '2025-04-10 22:00:00', '王宿管', '已与任课教师确认，予以通过'),

-- 待审核的申请
('2420710220', '2025-03-20 23:30:00', '参加社团活动', '学生活动中心', '2025-03-20 18:30:00', 0, NULL, NULL, NULL),
('2420710220', '2025-03-25 23:45:00', '图书馆学习', '图书馆', '2025-03-25 19:00:00', 0, NULL, NULL, NULL),
('2420710220', '2025-04-01 23:30:00', '参加比赛', '体育馆', '2025-04-01 20:00:00', 0, NULL, NULL, NULL),

-- 已驳回的申请
('2420710220', '2025-03-01 23:45:00', '外出游玩', '市中心', '2025-03-01 18:00:00', 2, '2025-03-01 22:00:00', '张宿管', '非必要外出，予以驳回'),
('2420710220', '2025-02-28 23:45:00', '朋友聚会', '校外餐厅', '2025-02-28 17:00:00', 2, '2025-02-28 21:00:00', '李宿管', '无特殊情况，予以驳回');

-- 晚归情况说明数据
INSERT INTO late_return_explanation 
(late_return_id, student_no, description, submit_time, audit_status, audit_time, audit_person, audit_remark) VALUES
-- 已通过的说明
(5, '2420710220', '参加学术讲座，讲座结束后与老师讨论问题，导致晚归。', '2025-03-10 23:20:00', 1, '2025-03-11 09:00:00', '张宿管', '已核实，予以通过'),
(6, '2420710220', '参加编程比赛，比赛结束后需要整理设备，导致晚归。', '2025-03-05 23:35:00', 1, '2025-03-06 10:00:00', '李宿管', '有比赛证明，予以通过'),
(11, '2420710220', '参加实验课，实验结束后需要整理实验数据，导致晚归。', '2025-04-10 23:40:00', 1, '2025-04-11 09:30:00', '王宿管', '已与任课教师确认，予以通过'),

-- 待审核的说明
(1, '2420710220', '参加社团活动，活动结束后需要收拾场地，导致晚归。', '2025-03-20 23:25:00', 0, NULL, NULL, NULL),
(2, '2420710220', '在图书馆学习，忘记时间，导致晚归。', '2025-03-25 23:40:00', 0, NULL, NULL, NULL),
(3, '2420710220', '参加比赛，比赛结束后需要收拾器材，导致晚归。', '2025-03-15 23:55:00', 0, NULL, NULL, NULL),
(4, '2420710220', '参加实验课，实验结束后需要整理实验数据，导致晚归。', '2025-03-28 23:30:00', 0, NULL, NULL, NULL),
(9, '2420710220', '参加比赛，比赛结束后需要收拾器材，导致晚归。', '2025-04-01 23:30:00', 0, NULL, NULL, NULL),
(10, '2420710220', '在图书馆学习，忘记时间，导致晚归。', '2025-04-05 23:25:00', 0, NULL, NULL, NULL),

-- 已驳回的说明
(7, '2420710220', '外出游玩，没有及时返回宿舍。', '2025-03-01 23:50:00', 2, '2025-03-02 10:00:00', '张宿管', '理由不充分，予以驳回'),
(8, '2420710220', '参加朋友聚会，没有及时返回宿舍。', '2025-02-28 23:45:00', 2, '2025-03-01 09:00:00', '李宿管', '无特殊情况，予以驳回');

-- 为宿管(2420710220SG)添加通知数据
INSERT INTO notification (notice_id, title, content, type, target_type, target_id, status, create_time) VALUES
('NT202505100001', '系统维护通知', '系统将于2025年5月15日凌晨2:00-4:00进行例行维护，请提前做好相关工作安排。', 'SYSTEM', 'DORM_MANAGER', '2420710220SG', 'UNREAD', '2025-05-10 10:00:00'),
('NT202505050001', '安全培训通知', '请于本周五下午2:00在行政楼会议室参加宿舍安全管理工作培训。', 'TRAINING', 'DORM_MANAGER', '2420710220SG', 'UNREAD', '2025-05-05 15:30:00'),
('NT202504300001', '晚归预警提醒', '4月30日晚有5名学生晚归，请及时处理相关记录。', 'LATE_RETURN', 'DORM_MANAGER', '2420710220SG', 'UNREAD', '2025-04-30 23:45:00'),
('NT202504250001', '宿舍检查通知', '本周三上午9:00将进行宿舍卫生检查，请做好相关准备工作。', 'INSPECTION', 'DORM_MANAGER', '2420710220SG', 'UNREAD', '2025-04-25 14:20:00'),
('NT202504200001', '设备报修提醒', '3号楼热水器出现故障，请及时联系维修人员处理。', 'MAINTENANCE', 'DORM_MANAGER', '2420710220SG', 'UNREAD', '2025-04-20 09:15:00'),
('NT202504150001', '工作例会通知', '请于下周一上午10:00在宿管办公室参加月度工作例会。', 'MEETING', 'DORM_MANAGER', '2420710220SG', 'UNREAD', '2025-04-15 16:40:00'),
('NT202504100001', '学生投诉处理', '收到学生关于宿舍噪音的投诉，请及时调查处理。', 'COMPLAINT', 'DORM_MANAGER', '2420710220SG', 'UNREAD', '2025-04-10 11:25:00'),
('NT202504050001', '假期值班安排', '劳动节假期值班表已发布，请查看并确认值班时间。', 'DUTY', 'DORM_MANAGER', '2420710220SG', 'UNREAD', '2025-04-05 13:50:00'),
('NT202504030001', '防疫物资补充', '防疫物资库存不足，请及时申请补充。', 'SUPPLY', 'DORM_MANAGER', '2420710220SG', 'UNREAD', '2025-04-03 10:30:00'),
('NT202504010001', '系统使用培训', '新版本宿舍管理系统已上线，请参加本周五的培训。', 'TRAINING', 'DORM_MANAGER', '2420710220SG', 'UNREAD', '2025-04-01 15:00:00');