package com.tianhai.warn.controller;

import com.tianhai.warn.annotation.LogOperation;
import com.tianhai.warn.annotation.RequirePermission;
import com.tianhai.warn.constants.AlarmConstants;
import com.tianhai.warn.constants.Constants;
import com.tianhai.warn.enums.ResultCode;
import com.tianhai.warn.exception.BusinessException;
import com.tianhai.warn.exception.SystemException;
import com.tianhai.warn.model.AlarmConfig;
import com.tianhai.warn.model.DormitoryManager;
import com.tianhai.warn.query.DormitoryManagerQuery;
import com.tianhai.warn.service.AlarmConfigService;
import com.tianhai.warn.service.DormitoryManagerService;
import com.tianhai.warn.service.VerificationService;
import com.tianhai.warn.utils.PageResult;
import com.tianhai.warn.utils.Result;
import com.tianhai.warn.utils.RoleObjectCaster;
import com.tianhai.warn.utils.SessionUtils;
import jakarta.servlet.http.HttpServletRequest;
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
import java.util.Map;
import java.util.Objects;

/**
 * 宿管信息控制器
 */
@Controller
@RequestMapping("/dorman")
public class DormitoryManagerController {
    private static final Logger logger = LoggerFactory.getLogger(DormitoryManagerController.class);

    @Autowired
    private DormitoryManagerService dormitoryManagerService;

    @Autowired
    private VerificationService verificationService;

    @Autowired
    private AlarmConfigService alarmConfigService;

    @GetMapping
    public String dorMan(HttpSession session, Model model) {
        Object dorMan = session.getAttribute(Constants.SESSION_ATTRIBUTE_USER);

        if (dorMan instanceof DormitoryManager dormitoryManager) {
            model.addAttribute("name", dormitoryManager.getName());
            model.addAttribute("email", dormitoryManager.getEmail());
            model.addAttribute("role", session.getAttribute("role"));
        }

        return "staff-dashboard";
    }

    /**
     * 宿管更新宿管信息
     */
    @PostMapping("/update/per-info")
    @ResponseBody
    @RequirePermission(roles = { Constants.SYSTEM_USER, Constants.SUPER_ADMIN })
    @LogOperation("更新宿管信息")
    public Result<?> updateByOneself(@RequestBody DormitoryManager manager) {
        // 获取当前登录用户
        HttpSession session = SessionUtils.getSession(false);
        assert session != null;
        Object user = session.getAttribute(Constants.SESSION_ATTRIBUTE_USER);

        if (!(user instanceof DormitoryManager)) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }

        validateUpdateInfo(manager);

        // 设置当前用户的 ID
        DormitoryManager currentManager = RoleObjectCaster.cast(Constants.DORMITORY_MANAGER, user);
        manager.setId(currentManager.getId());

        // 调用 Service 层处理更新
        dormitoryManagerService.updatePersonalInfo(manager, currentManager.getEmail());

        manager.setPassword(null);
        session.setAttribute(Constants.SESSION_ATTRIBUTE_USER, manager);

        return Result.success(ResultCode.SUCCESS);
    }

    /**
     * 超级管理员更新宿管信息
     */
    @PostMapping("/super-admin/update/per-info")
    @ResponseBody
    @RequirePermission(roles = Constants.SUPER_ADMIN)
    @LogOperation("超级管理员更新宿管信息")
    public Result<?> updateBySuperAdmin(@RequestBody DormitoryManager newDorManInfo) {
        // 检查超级管理员状态
        verificationService.checkSuperAdminStatus();

        if (newDorManInfo.getId() == null || newDorManInfo.getId() <= 0) {
            // 密码脱敏
            newDorManInfo.setPassword(null);
            logger.error("提交的宿管信息缺少id或者id不合法，newDorManInfo: {}", newDorManInfo);
            throw new BusinessException(ResultCode.PARAMETER_ERROR);
        }

        int affectRow = dormitoryManagerService.updatePersonalInfoBySuperAdmin(newDorManInfo);
        if (affectRow <= 0) {
            logger.error("超级管理员更新宿管信息失败， newDorManInfo: {}", newDorManInfo);
            throw new SystemException(ResultCode.ERROR);
        }

        return Result.success();
    }

    /**
     * 超级管理员删除宿管
     */
    @DeleteMapping("delete/{id}")
    @ResponseBody
    @RequirePermission(roles = Constants.SUPER_ADMIN)
    @LogOperation("超级管理员删除宿管信息")
    public Result<?> deleteDormitoryManager(@PathVariable Integer id) {
        // 校验超级管理员状态
        verificationService.checkSuperAdminStatus();

        if (id == null || id <= 0) {
            logger.error("提交的宿管id不合法，id：{}", id);
            throw new BusinessException(ResultCode.PARAMETER_ERROR);
        }

        dormitoryManagerService.deleteById(id);

        return Result.success();
    }

    /**
     * 修改宿管状态
     */
    @GetMapping("update-status/{id}/{status}")
    @ResponseBody
    @RequirePermission(roles = Constants.SUPER_ADMIN)
    @LogOperation("超级管理员修改宿管状态")
    public Result<Void> updateStatus(@PathVariable Integer id, @PathVariable String status) {
        // 校验超级管理员状态
        verificationService.checkSuperAdminStatus();

        if (id == null || id <= 0) {
            logger.error("提交的宿管id不合法，id：{}", id);
            throw new BusinessException(ResultCode.PARAMETER_ERROR);
        }
        if (StringUtils.isBlank(status) || !(status.equals("ON_DUTY") || status.equals("OFF_DUTY"))) {
            logger.error("状态不合规, status: {}", status);
            throw new BusinessException(ResultCode.PARAMETER_ERROR);
        }

        DormitoryManager dormitoryManager = DormitoryManager.builder().id(id).status(status).build();
        dormitoryManagerService.updateStatus(dormitoryManager);

        return Result.success();
    }

    /**
     * 获取所有宿管分页列表
     */
    @GetMapping("/page-list")
    @ResponseBody
    @RequirePermission(roles = Constants.SUPER_ADMIN)
    @LogOperation("超级管理员分页查询宿管信息")
    public Result<PageResult<DormitoryManager>> getDorManListPage(DormitoryManagerQuery query) {
        if (query == null) {
            logger.error("查询条件不合规， query: {}", query);
            throw new BusinessException(ResultCode.PARAMETER_ERROR);
        }

        // 分页参数校验
        if (query.getPageNum() == null || query.getPageNum() < 1) {
            query.setPageNum(Constants.DEFAULT_PAGE_NUM);
        }
        if (query.getPageSize() == null || query.getPageSize() < 1) {
            query.setPageSize(Constants.DEFAULT_PAGE_SIZE);
        }

        PageResult<DormitoryManager> dorManList = dormitoryManagerService.selectByPageQuery(query);
        // 没有结果时返回空列表
        if (dorManList == null || dorManList.getData() == null
                || dorManList.getData().isEmpty()) {
            return Result.success(new PageResult<>());
        }

        return Result.success(dorManList);
    }

    /**
     * 根据条件查询宿管
     */
    @PostMapping("/search")
    @ResponseBody
    @RequirePermission(roles = { Constants.SUPER_ADMIN, Constants.DORMITORY_MANAGER })
    @LogOperation("根据条件查询宿管信息")
    public List<DormitoryManager> search(@RequestBody DormitoryManager manager) {
        if (manager == null) {
            logger.error("查询条件不合规, manager: {}", manager);
            throw new BusinessException(ResultCode.PARAMETER_ERROR);
        }

        return dormitoryManagerService.selectByCondition(manager);
    }

    /**
     * 校验修改信息
     * 
     * @param manager 修改信息
     */
    private void validateUpdateInfo(DormitoryManager manager) {
        // 创建错误信息列表
        List<String> errors = new ArrayList<>();

        // 验证必填字段
        if (StringUtils.isBlank(manager.getName())) {
            errors.add("姓名不能为空");
        }

        if (StringUtils.isBlank(manager.getBuilding())) {
            errors.add("宿舍楼不能为空");
        }

        if (StringUtils.isBlank(manager.getPhone())) {
            errors.add("手机号不能为空");
        } else if (!manager.getPhone().matches("^1[3-9]\\d{9}$")) {
            errors.add("手机号格式不正确");
        }

        if (StringUtils.isBlank(manager.getEmail())) {
            errors.add("邮箱不能为空");
        } else if (!manager.getEmail().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            errors.add("邮箱格式不正确");
        }

        if (!errors.isEmpty()) {
            throw new BusinessException(String.join("; ", errors));
        }
    }

    /**
     * 获取IP定位信息（通过腾讯地图IP定位API）
     * 用于前端地图定位功能
     */
    @GetMapping("/location/ip")
    @ResponseBody
    @RequirePermission(roles = { Constants.DORMITORY_MANAGER, Constants.SYSTEM_USER })
    @LogOperation("获取IP定位信息")
    public Result<Map<String, Object>> getLocationByIP(HttpServletRequest request) {
        try {
            // 获取客户端IP地址
            String clientIP = getClientIP(request);
            logger.info("获取IP定位，客户端IP: {}", clientIP);

            // 从报警配置表中获取腾讯地图LBS配置
            AlarmConfig mapConfig = alarmConfigService.selectByApiProvider(AlarmConstants.TENCENT_ALARM_LBS_MAP);
            if (mapConfig == null || !Objects.equals(mapConfig.getIsActive(), AlarmConstants.ALARM_CONFIG_ACTIVE)) {
                logger.error("腾讯地图API配置未启用或不存在, apiProvider: {}", AlarmConstants.TENCENT_ALARM_LBS_MAP);
                throw new SystemException(ResultCode.ALARM_CONFIG_NOT_FOUNT);
            }

            String tencentMapKey = mapConfig.getApiKey();
            if (StringUtils.isBlank(tencentMapKey)) {
                logger.error("腾讯地图API配置中的 apiKey 为空, apiProvider: {}", AlarmConstants.TENCENT_ALARM_LBS_MAP);
                throw new SystemException(ResultCode.ALARM_CONFIG_NOT_FOUNT);
            }

            // 调用腾讯地图IP定位API
            String apiUrl = "https://apis.map.qq.com/ws/location/v1/ip?key=" + tencentMapKey + "&ip=" + clientIP;

            // 使用Spring的RestTemplate或HttpClient调用API
            // 这里简化处理，实际项目中建议使用RestTemplate
            java.net.URL url = new java.net.URL(apiUrl);
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(connection.getInputStream(), "UTF-8"));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                // 解析JSON响应
                com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
                @SuppressWarnings("unchecked")
                Map<String, Object> result = objectMapper.readValue(response.toString(), Map.class);

                // 检查响应状态
                Integer status = (Integer) result.get("status");
                if (status != null && status == 0) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> resultData = (Map<String, Object>) result.get("result");
                    if (resultData != null) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> location = (Map<String, Object>) resultData.get("location");
                        if (location != null) {
                            Double lat = ((Number) location.get("lat")).doubleValue();
                            Double lng = ((Number) location.get("lng")).doubleValue();

                            Map<String, Object> locationResult = new java.util.HashMap<>();
                            locationResult.put("lat", lat);
                            locationResult.put("lng", lng);
                            locationResult.put("ip", clientIP);

                            return Result.success(locationResult);
                        }
                    }
                }

                logger.warn("腾讯地图IP定位API返回错误，状态码: {}, 响应: {}", status, response.toString());
            } else {
                logger.error("调用腾讯地图IP定位API失败，HTTP状态码: {}", responseCode);
            }

            connection.disconnect();

        } catch (Exception e) {
            logger.error("获取IP定位信息失败", e);
        }

        // 定位失败，返回默认位置（北京）
        Map<String, Object> defaultLocation = new java.util.HashMap<>();
        defaultLocation.put("lat", 39.916527);
        defaultLocation.put("lng", 116.397128);
        defaultLocation.put("ip", "unknown");

        return Result.success(defaultLocation);
    }

    /**
     * 获取客户端真实IP地址
     */
    private String getClientIP(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // 处理多个IP的情况，取第一个IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        // 如果是本地IP，返回127.0.0.1（开发环境）
        if ("0:0:0:0:0:0:0:1".equals(ip) || "127.0.0.1".equals(ip)) {
            // 开发环境可能无法获取真实IP，返回空让API使用默认IP定位
            return "";
        }

        return ip;
    }
}