package com.tianhai.warn.service.impl;

import com.tianhai.warn.constants.Constants;
import com.tianhai.warn.enums.ResultCode;
import com.tianhai.warn.enums.UserRole;
import com.tianhai.warn.exception.BusinessException;
import com.tianhai.warn.model.DormitoryManager;
import com.tianhai.warn.model.Student;
import com.tianhai.warn.model.SuperAdmin;
import com.tianhai.warn.model.SysUser;
import com.tianhai.warn.service.*;
import com.tianhai.warn.utils.RoleObjectCaster;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.util.*;

import static com.tianhai.warn.constants.Constants.*;
import static com.tianhai.warn.constants.Constants.SUPER_ADMIN;

/**
 * 认证服务实现类
 */
@Service
public class AuthServiceImpl implements AuthService {
    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);

    @Value("${jwt.rememberMe.secret}") // 从配置文件读取密钥，提供默认值
    private String jwtSecret;

    @Autowired
    private StudentService studentService;

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private DormitoryManagerService dormitoryManagerService;

    @Autowired
    private SuperAdminService superAdminService;



    /**
     * 项目设定在学生 班级管理员 宿管三个角色将 每个用户的邮箱具有唯一性
       但超级管理员的邮箱可出现在特定的班级管理员中
     * @param name     姓名
     * @param email    邮箱
     * @param password 密码
     * @param role     角色
     * @return         用户信息
     */
    @Override
    public Map<String, Object> login(String name, String email, String password, String role) {
        // 1. 验证角色是否有效
        if (!UserRole.isValidRole(role)) {
            throw new BusinessException(ResultCode.USER_ROLE_DISABLE);
        }

        // 2. 根据角色验证用户
        String encryptedPassword = DigestUtils.md5DigestAsHex(password.getBytes());
        Map<String, Object> infoMap = new HashMap<>();
        Object userObject = null; // 用于存储具体的用户对象
        Long userId = null; // 用户存储用户的主键ID
        String userRoleCode = null; // 用户存储用户角色编码

            switch (Objects.requireNonNull(UserRole.getByCode(role))) {
                // 学生
                case STUDENT:
                    Student student = studentService.getStudentByEmail(email);
                    if (student == null || !student.getPassword().equals(encryptedPassword)) {
                        throw new BusinessException(ResultCode.USER_NAME_PWD_FALSE);
                    }
                    student.setPassword(null);
                    userObject = student;
                    userId = student.getId().longValue();
                    userRoleCode = UserRole.STUDENT.getCode();
//                    infoMap.put(Constants.SESSION_ATTRIBUTE_USER, student);
                    infoMap.put(Constants.SESSION_ATTRIBUTE_ROLE, UserRole.STUDENT.getCode());
                    studentService.updateLastLoginTime(student.getId());
                    break;

                // 班级管理员 具体的职位角色，在注册时已经完成设置
                case SYSTEM_USER:
                    SysUser sysUser = sysUserService.getSysUserByEmail(email);
                    if (sysUser == null || !sysUser.getPassword().equals(encryptedPassword)) {
                        throw new BusinessException(ResultCode.USER_NAME_PWD_FALSE);
                    }
                    sysUser.setPassword(null);
                    userObject = sysUser;
                    userId = sysUser.getId().longValue();
                    userRoleCode = UserRole.SYSTEM_USER.getCode();
//                    infoMap.put(Constants.SESSION_ATTRIBUTE_USER, sysUser);
                    infoMap.put(Constants.SESSION_ATTRIBUTE_ROLE, UserRole.SYSTEM_USER.getCode());
                    infoMap.put(Constants.SESSION_ATTRIBUTE_JOB_ROLE, sysUser.getJobRole().toLowerCase());

                    sysUserService.updateLastLoginTime(sysUser.getId());
                    break;

                // 宿管
                case DORMITORY_MANAGER:
                    DormitoryManager manager = dormitoryManagerService.getByEmail(email);
                    if (manager == null || !manager.getPassword().equals(encryptedPassword)) {
                        throw new BusinessException(ResultCode.USER_NAME_PWD_FALSE);
                    }
                    manager.setPassword(null);
                    userObject = manager;
                    userId = manager.getId().longValue();
                    userRoleCode = UserRole.DORMITORY_MANAGER.getCode();
//                    infoMap.put(Constants.SESSION_ATTRIBUTE_USER, manager);
                    infoMap.put(Constants.SESSION_ATTRIBUTE_ROLE, UserRole.DORMITORY_MANAGER.getCode());

                    dormitoryManagerService.updateLastLoginTime(manager.getId());
                    break;

                // 超级管理员
                case SUPER_ADMIN:
                    SuperAdmin superAdmin = superAdminService.getByEmail(email);
                    if (superAdmin == null || !superAdmin.getPassword().equals(encryptedPassword)) {
                        throw new BusinessException(ResultCode.USER_NAME_PWD_FALSE);
                    }
                    superAdmin.setPassword(null);
                    userObject = superAdmin;
                    userId = superAdmin.getId().longValue();
                    userRoleCode = UserRole.SUPER_ADMIN.getCode();
//                    infoMap.put(Constants.SESSION_ATTRIBUTE_USER, superAdmin);
                    infoMap.put(Constants.SESSION_ATTRIBUTE_ROLE, UserRole.SUPER_ADMIN);

                    superAdminService.updateLastLoginTime(superAdmin.getId());
                    break;

                default:
                    throw new BusinessException(ResultCode.USER_ROLE_DISABLE);

            }

            if (userObject != null && userId != null && userRoleCode != null) {
                infoMap.put(Constants.SESSION_ATTRIBUTE_USER, userObject);
                infoMap.put("userId", userId); // 将用户ID放入map
                infoMap.put(Constants.SESSION_ATTRIBUTE_ROLE, userRoleCode); // 将角色编码放入map
            } else {
                // 如果用户对象、ID或角色获取失败，记录错误并抛出异常
                System.err.println("获取用户ID或角色失败: userObject is " + (userObject == null ? "null" : "not null") +
                        ", userId is " + userId + ", userRoleCode is " + userRoleCode); // 更详细的错误日志
                throw new BusinessException(ResultCode.USER_NAME_PWD_FALSE); // 抛出异常通知Controller
            }

            return infoMap;
    }

    @Override
    public Cookie handleRememberMe(Map<String, Object> loginInfo, boolean remember) {
        if (remember && loginInfo != null) {
            Long userId = (Long) loginInfo.get("userId");
            String userRole = (String) loginInfo.get(Constants.SESSION_ATTRIBUTE_ROLE);
            String jobRole = (String) loginInfo.get("jobRole"); // 获取jobRole

            if (userId != null && userRole != null) {
                long expirationTime = 7 * 24 * 60 * 60 * 1000; // 7天的过期时间

                // 构建 JWT
                String jwt = Jwts.builder()
                        .setSubject(userId.toString()) // 用户ID作为Subject
                        .claim("userRole", userRole) // 用户角色
                        .claim("jobRole", jobRole) // 职位角色 (可能为null)
                        .setIssuedAt(new Date())
                        .setExpiration(new Date(System.currentTimeMillis() + expirationTime))
                        .signWith(SignatureAlgorithm.HS256, jwtSecret) // 使用注入的密钥签名
                        .compact();

                // 创建 Cookie
                Cookie cookie = new Cookie("rememberMe", jwt);
                cookie.setHttpOnly(true); // 房子XSS攻击获取Cookie
                cookie.setMaxAge((int) (expirationTime / 1000)); // 设置Cookie过期时间和JWT以致
                cookie.setPath("/"); // 所有路径都可访问此Cookie
                // TODO: 如果是HTTPS，设置cookie.setSecure(true);

                return cookie;
            } else {
                // TODO: 记录警告或错误，用户ID或角色未找到，无法生成JWT
                logger.warn("无法生成记住我JWT: 用户ID或角色信息不完整. loginInfo: {}", loginInfo); // 使用日志框架
            }
        }
        return null; // 不需要记住或信息不完整，返回null
    }

    @Override
    public Map<String, Object> findUserByRememberMeToken(String token) {
        if (!StringUtils.hasText(token)) {
            return null; // Token为空
        }

        try {
            // 1. 解析并验证JWT
//            Jws<Claims> claimsJws = Jwts.parser()
//                    .setSigningKey(jwtSecret)
//                    .parseClaimsJws(token);
            Jws<Claims> claimsJws = Jwts.parserBuilder()
                    .setSigningKey(Keys.hmacShaKeyFor(Base64.getDecoder().decode(jwtSecret)))
                    .build()
                    .parseClaimsJws(token);

            Claims claims = claimsJws.getBody();
            String userIdStr = claims.getSubject(); // 获取用户ID
            String userRole = claims.get("userRole", String.class); // 获取角色
            String jobRole = claims.get("jobRole", String.class); // 获取jobRole

            if (StringUtils.hasText(userIdStr) && StringUtils.hasText(userRole)) {
                Long userId = Long.parseLong(userIdStr);
                Object userObject = searchUserFromDB(userRole, userId); // 根据用户ID和角色查找用户

                // 2. 如果用户找到且有效
                if (userObject != null) {
                    // 构建并返回包含用户对象、角色和jobRole的Map
                    Map<String, Object> userInfo = new HashMap<>();
                    userInfo.put(Constants.SESSION_ATTRIBUTE_USER, userObject);
                    userInfo.put(Constants.SESSION_ATTRIBUTE_ROLE, userRole);
                    userInfo.put(Constants.SESSION_ATTRIBUTE_JOB_ROLE, jobRole); // 将jobRole也放入map
                    return userInfo;
                } else {
                    logger.warn("记住我Token有效，但数据库中没有该用户信息 userId: {}, role: {}", userId, userRole);
                    return null; // 用户在系统中不存在或无效
                }
            } else {
                logger.warn("记住我Token有效，但JWT中缺少用户ID或角色信息");
                return null; // JWT中缺少必要信息
            }

        } catch (ExpiredJwtException e) {
            logger.warn("记住我Token已过期");
            return null; // Token过期
        } catch (MalformedJwtException | UnsupportedJwtException e) {
            logger.warn("记住我Token验证失败: 无效的签名或格式: {}", e.getMessage());
            return null; // JWT签名无效或其他解析错误
        } catch (Exception e) {
            // 其他解析或查找用户时的错误
            logger.error("处理记住我Token时发生异常: {}", e.getMessage(), e);
            return null; // 发生其他异常
        }
    }


    /**
     * 从数据库中查询用户
     * @param userRole      用户角色（非职位角色）
     * @param userId        用户主键ID（非业务标识ID）
     * @return              用户信息
     */
    private Object searchUserFromDB(String userRole, Long userId) {
        // 注意：这里的selectById方法需要确保你的Mapper/Service能够根据Long类型的ID查找
        // 并且要考虑用户状态（是否在职、启用等）
        Object userObject;

        switch (userRole) {
            case STUDENT -> userObject = studentService.selectById(userId.intValue());

            case SYSTEM_USER -> {
                userObject = sysUserService.selectById(userId.intValue());
                SysUser classManager = RoleObjectCaster.cast(SYSTEM_USER, userObject);
                if (!classManager.getStatus().equalsIgnoreCase(ENABLE_STR)) {
                    throw new BusinessException(ResultCode.USER_LOCKED);
                }
            }

            case DORMITORY_MANAGER -> {
                userObject = dormitoryManagerService.selectById(userId.intValue());
                DormitoryManager dormitoryManager = RoleObjectCaster.cast(DORMITORY_MANAGER, userObject);
                if (!dormitoryManager.getStatus().equalsIgnoreCase(ON_DUTY)) {
                    throw new BusinessException(ResultCode.USER_LOCKED);
                }
            }

            case SUPER_ADMIN -> userObject = superAdminService.selectById(userId.intValue());

            default -> throw new BusinessException(ResultCode.USER_ROLE_DISABLE);
        }

        return userObject;
    }


}
