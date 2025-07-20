# 学生晚归预警系统

基于Spring MVC + MyBatis的学生晚归预警管理系统，用于协助学校管理人员和宿舍管理员及时发现和处理学生晚归情况。

## 项目结构

```
src/main/
├── java/com/tianhai/warn/
│   ├── controller/     # 控制器层
│   ├── model/         # 数据模型层
│   ├── mapper/        # MyBatis映射层
│   ├── service/       # 服务层接口
│   │   └── impl/     # 服务层实现
│   └── util/          # 工具类
├── resources/
│   ├── mapper/        # MyBatis映射文件
│   ├── spring/        # Spring配置文件
│   │   ├── spring-context.xml
│   │   └── spring-mvc.xml
│   ├── db/            # 数据库脚本
│   │   ├── schema.sql
│   │   └── data.sql
│   ├── jdbc.properties    # 数据库配置
│   └── mybatis-config.xml # MyBatis配置
└── webapp/
    ├── static/        # 静态资源
    │   ├── css/
    │   ├── js/
    │   └── images/
    ├── WEB-INF/
    │   ├── views/     # JSP页面
    │   └── web.xml    # Web配置文件
    └── index.jsp      # 首页
```

## 技术栈

- Spring Framework 5.3.20
- Spring MVC 5.3.20
- MyBatis 3.5.9
- MySQL 5.7+
- JSP + JSTL
- Druid 1.2.8 (数据库连接池)
- Maven 3.6+
- JDK 1.8+
- Tomcat 8.5+

## 主要功能

1. 用户管理
   - 管理员登录
   - 宿管登录
   - 密码修改

2. 学生信息管理
   - 学生信息录入
   - 学生信息查询
   - 学生信息修改

3. 晚归记录管理
   - 晚归登记
   - 晚归审核
   - 晚归统计

4. 预警规则管理
   - 预警规则配置
   - 规则启用/禁用
   - 通知方式设置

5. 通知管理
   - 学生通知
   - 辅导员通知
   - 家长通知

6. 统计分析
   - 晚归趋势分析
   - 学院分布统计
   - 宿舍分布统计

## 快速开始

1. 环境准备
   ```bash
   # 确保已安装以下软件
   - JDK 1.8+
   - Maven 3.6+
   - MySQL 5.7+
   - Tomcat 8.5+
   ```

2. 数据库配置
   ```bash
   # 创建数据库
   mysql -u root -p < src/main/resources/db/schema.sql
   
   # 导入测试数据
   mysql -u root -p warn < src/main/resources/db/data.sql
   ```

3. 修改配置
   - 编辑 `src/main/resources/jdbc.properties`
   ```properties
   jdbc.url=jdbc:mysql://localhost:3306/warn?useUnicode=true&characterEncoding=utf8
   jdbc.username=your_username
   jdbc.password=your_password
   ```

4. 编译打包
   ```bash
   mvn clean package
   ```

5. 部署运行
   - 将生成的war包部署到Tomcat的webapps目录
   - 启动Tomcat
   - 访问 http://localhost:8080/warn

## 默认账号

1. 管理员
   - 账号：admin
   - 密码：admin

2. 宿管
   - 账号：dormitory1
   - 密码：admin

## 开发规范

1. 代码规范
   - 遵循阿里巴巴Java开发手册
   - 使用统一的代码格式化模板

2. 提交规范
   - 每次提交前先更新代码
   - 提交信息要清晰明了
   - 避免提交无关的文件

## 部署说明

1. 开发环境
   ```bash
   mvn tomcat7:run
   ```

2. 生产环境
   - 修改生产环境配置
   - 打包：mvn clean package
   - 部署war包到Tomcat

## 维护说明

1. 日志查看
   - 系统日志：`${catalina.home}/logs/warn.log`
   - 访问日志：`${catalina.home}/logs/access.log`

2. 数据备份
   - 定期备份数据库
   - 备份配置文件

## 版本历史

- v1.0.0 (2024-01)
  - 基础功能实现
  - 晚归记录管理
  - 预警规则配置

## 许可证

MIT License 