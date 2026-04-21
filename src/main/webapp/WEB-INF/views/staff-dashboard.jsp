<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<!-- 根据用户角色定义所需变量 -->
<c:set var="jobRole" value=""/>
<c:set var="managerId" value=""/>
<c:set var="targetId" value=""/>
<c:set var="building" value=""/>

<c:if test="${not empty sessionScope.user}">
  <c:choose>
    <c:when test="${sessionScope.user['class'].simpleName eq 'DormitoryManager'}">
      <c:if test="${not empty sessionScope.user.managerId}">
        <c:set var="managerId" value="${sessionScope.user.managerId}"/>
        <c:set var="targetId" value="${sessionScope.user.managerId}"/>
      </c:if>
      <c:if test="${not empty sessionScope.user.building}">
        <c:set var="building" value="${sessionScope.user.building}"/>
      </c:if>
    </c:when>
    <c:when test="${sessionScope.user['class'].simpleName eq 'SysUser'}">
      <c:if test="${not empty sessionScope.user.jobRole}">
        <c:set var="jobRole" value="${sessionScope.user.jobRole}"/>
      </c:if>
      <c:if test="${not empty sessionScope.user.sysUserNo}">
        <c:set var="targetId" value="${sessionScope.user.sysUserNo}"/>
      </c:if>
    </c:when>
  </c:choose>
</c:if>

<!DOCTYPE html>
<html>
<head>
  <!-- 基层管理人员（宿管、辅导员、班主任）主页 -->
  <!-- todo 修改个人信息需要进行根据不同角色的适配 院级领导有个按钮入口可查看全校总览数据-->
  <title>学生晚归预警系统 - 晚归管理主页</title>
  <%--    <link href="https://cdn.bootcdn.net/ajax/libs/tailwindcss/2.2.19/tailwind.min.css" rel="stylesheet">--%>
  <%--    <link href="https://cdn.bootcdn.net/ajax/libs/font-awesome/6.0.0/css/all.min.css" rel="stylesheet">--%>
  <!-- 引入 CSS 文件 -->
  <link href="${pageContext.request.contextPath}/static/css/tailwind.min.css" rel="stylesheet">
  <link href="${pageContext.request.contextPath}/static/css/fontawesome.min.css" rel="stylesheet">
  <link href="${pageContext.request.contextPath}/static/css/bootstrap.min.css" rel="stylesheet">

  <!-- 引入 JS 文件 -->
  <script src="${pageContext.request.contextPath}/static/js/jquery.min.js"></script>
  <script src="${pageContext.request.contextPath}/static/js/popper.min.js"></script>
  <script src="${pageContext.request.contextPath}/static/js/bootstrap.bundle.min.js"></script>

  <!-- 腾讯地图API -->
  <script src="https://map.qq.com/api/gljs?v=1.exp&key=7A6BZ-UC5C3-D3Z3G-OCW32-ZTILV-G6BH5"
          onerror="console.error('腾讯地图API脚本加载失败，请检查网络连接和Key配置');
          window.tencentMapScriptError = true;"></script>
  <style>
    .dashboard-container {
      min-height: 100vh;
      background-color: #f3f4f6;
    }
    .card {
      background: white;
      border-radius: 0.5rem;
      box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
    }
    .status-badge {
      padding: 0.25rem 0.75rem;
      border-radius: 9999px;
      font-size: 0.75rem;
      font-weight: 500;
    }
    .status-pending {
      background-color: #FEF3C7;
      color: #92400E;
    }
    .status-processed {
      background-color: #D1FAE5;
      color: #065F46;
    }

    #container2{
      /*地图(容器)显示大小*/
      width: 100%;
      height: 400px;
      border-radius: 0.5rem;
      overflow: hidden;
      box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
    }

  </style>
</head>
<!--页面加载后触发腾讯地图v2的加载-->
<body onload="initMap2()">
<div class="dashboard-container p-6">
  <!-- 顶部导航栏 -->
  <nav class="bg-white shadow-sm rounded-lg mb-6 p-4">
    <div class="flex justify-between items-center">
      <h1 class="text-xl font-semibold text-gray-800">学生晚归预警系统</h1>
      <div class="flex items-center space-x-4">
                    <span class="text-gray-600">欢迎，${sessionScope.user.name}（
                        <c:choose>
                          <c:when test="${sessionScope.user['class'].simpleName eq 'DormitoryManager'}">宿管</c:when>
                          <c:when test="${sessionScope.user['class'].simpleName eq 'SysUser'}">
                            <c:choose>
                              <c:when test="${not empty jobRole and fn:toLowerCase(jobRole) eq 'counselor'}">辅导员</c:when>
                              <c:when test="${not empty jobRole and fn:toLowerCase(jobRole) eq 'class_teacher'}">班主任</c:when>
                              <c:when test="${not empty jobRole and fn:toLowerCase(jobRole) eq 'dean'}">院级领导</c:when>
                              <c:otherwise>系统用户</c:otherwise>
                            </c:choose>
                          </c:when>
                        </c:choose>
                    ）</span>
        <button class="text-sm text-blue-600 hover:text-blue-800" data-bs-toggle="modal" data-bs-target="#editProfileModal">修改个人信息</button>
        <button class="text-sm text-red-600 hover:text-red-800" onclick="handleLogout()">退出登录</button>
      </div>
    </div>
  </nav>

  <!-- 定义地图显示容器 -->
  <div class="card p-6 mb-6">
    <div class="flex items-center mb-4">
      <h2 class="text-lg font-medium text-gray-900">位置信息</h2>
    </div>
    <div id="container2"></div>
    <div id="map2-location-info" class="mt-4 text-sm text-gray-600"></div>
  </div>

  <!-- 宿管个人信息修改模态框 -->
  <div class="modal fade" id="dormitoryManagerModal" tabindex="-1" aria-labelledby="dormitoryManagerModalLabel" aria-hidden="true">
    <div class="modal-dialog">
      <div class="modal-content">
        <div class="modal-header">
          <h5 class="modal-title" id="dormitoryManagerModalLabel">修改个人信息</h5>
          <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
        </div>
        <div class="modal-body">
          <form id="dormitoryManagerForm">
            <div class="mb-3">
              <label for="managerId" class="form-label">工号</label>
              <input type="text" class="form-control" id="managerId" name="managerId" value="${managerId}" required>
            </div>
            <c:if test="${sessionScope.user['class'].simpleName eq 'DormitoryManager'}">
              <div class="mb-3">
                <label for="building" class="form-label">负责宿舍楼</label>
                <input type="text" class="form-control" id="building" name="building" value="${building}" required>
              </div>
            </c:if>
            <div class="mb-3">
              <label for="dorm_name" class="form-label">姓名</label>
              <input type="text" class="form-control" id="dorm_name" name="name" value="${sessionScope.user.name}" required>
            </div>
            <div class="mb-3">
              <label for="dorm_phone" class="form-label">联系电话</label>
              <input type="tel" class="form-control" id="dorm_phone" name="phone" value="${sessionScope.user.phone}" required>
            </div>
            <div class="mb-3">
              <label for="dorm_email" class="form-label">电子邮箱</label>
              <input type="email" class="form-control" id="dorm_email" name="email" value="${sessionScope.user.email}" required>
            </div>
            <div class="mb-3">
              <label for="dorm_password" class="form-label">新密码</label>
              <input type="password" class="form-control" id="dorm_password" name="password" placeholder="不修改请留空">
            </div>
          </form>
        </div>
        <div class="modal-footer">
          <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">取消</button>
          <button type="button" class="btn btn-primary" id="saveDormitoryManagerBtn">保存</button>
        </div>
      </div>
    </div>
  </div>

  <!-- 系统用户个人信息修改模态框 -->
  <div class="modal fade" id="systemUserModal" tabindex="-1" aria-labelledby="systemUserModalLabel" aria-hidden="true">
    <div class="modal-dialog">
      <div class="modal-content">
        <div class="modal-header">
          <h5 class="modal-title" id="systemUserModalLabel">修改个人信息</h5>
          <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
        </div>
        <div class="modal-body">
          <form id="systemUserForm">
            <div class="mb-3">
              <label for="jobRole" class="form-label">职位</label>
              <input type="text" class="form-control" id="jobRole" name="jobRole" value="${jobRole}" readonly>
            </div>
            <div class="mb-3">
              <label for="sys_name" class="form-label">姓名</label>
              <input type="text" class="form-control" id="sys_name" name="name" value="${sessionScope.user.name}" required>
            </div>
            <div class="mb-3">
              <label for="sys_phone" class="form-label">联系电话</label>
              <input type="tel" class="form-control" id="sys_phone" name="phone" value="${sessionScope.user.phone}" required>
            </div>
            <div class="mb-3">
              <label for="sys_email" class="form-label">电子邮箱</label>
              <input type="email" class="form-control" id="sys_email" name="email" value="${sessionScope.user.email}" required>
            </div>
            <div class="mb-3">
              <label for="sys_password" class="form-label">新密码</label>
              <input type="password" class="form-control" id="sys_password" name="password" placeholder="不修改请留空">
            </div>
          </form>
        </div>
        <div class="modal-footer">
          <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">取消</button>
          <button type="button" class="btn btn-primary" id="saveSystemUserBtn">保存</button>
        </div>
      </div>
    </div>
  </div>

  <!-- 统计卡片的时间范围选择器 -->
  <div class="card p-4 mb-6">
    <div class="flex items-center space-x-4">
      <div class="flex items-center space-x-2">
        <label class="text-sm text-gray-600">晚归统计开始日期：</label>
        <input type="date" id="startDate" class="form-input rounded-md border-gray-300 shadow-sm focus:border-blue-300 focus:ring focus:ring-blue-200 focus:ring-opacity-50">
      </div>
      <div class="flex items-center space-x-2">
        <label class="text-sm text-gray-600">晚归统计结束日期：</label>
        <input type="date" id="endDate" class="form-input rounded-md border-gray-300 shadow-sm focus:border-blue-300 focus:ring focus:ring-blue-200 focus:ring-opacity-50">
      </div>
      <button onclick="queryStatistics()" class="px-4 py-2 bg-blue-500 text-white rounded-md hover:bg-blue-600 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-opacity-50">
        查询
      </button>
    </div>
  </div>

  <!-- 统计卡片 -->
  <div class="grid grid-cols-1 md:grid-cols-4 gap-6 mb-6">
    <div class="card p-6">
      <div class="flex items-center">
        <div class="p-3 rounded-full bg-blue-100 text-blue-600">
          <i class="fas fa-user-clock text-xl"></i>
        </div>
        <div class="ml-4">
          <h2 class="text-sm font-medium text-gray-600">晚归人数</h2>
          <p class="text-2xl font-semibold text-gray-900">5</p>
        </div>
      </div>
    </div>
    <div class="card p-6">
      <div class="flex items-center">
        <div class="p-3 rounded-full bg-yellow-100 text-yellow-600">
          <i class="fas fa-clock text-xl"></i>
        </div>
        <div class="ml-4">
          <h2 class="text-sm font-medium text-gray-600">待处理晚归</h2>
          <p class="text-2xl font-semibold text-gray-900">3</p>
        </div>
      </div>
    </div>
    <div class="card p-6">
      <div class="flex items-center">
        <div class="p-3 rounded-full bg-green-100 text-green-600">
          <i class="fas fa-check-circle text-xl"></i>
        </div>
        <div class="ml-4">
          <h2 class="text-sm font-medium text-gray-600">已处理晚归</h2>
          <p class="text-2xl font-semibold text-gray-900">2</p>
        </div>
      </div>
    </div>
    <div class="card p-6">
      <div class="flex items-center">
        <div class="p-3 rounded-full bg-purple-100 text-purple-600">
          <i class="fas fa-bell text-xl"></i>
        </div>
        <div class="ml-4">
          <h2 class="text-sm font-medium text-gray-600">未读通知</h2>
          <p class="text-2xl font-semibold text-gray-900">2</p>
        </div>
      </div>
    </div>
  </div>

  <!-- 1. 地图容器v1版本 -->
<%--  <div class="card p-6 mb-6">--%>
<%--    <div class="flex items-center mb-4">--%>
<%--      <h2 class="text-lg font-medium text-gray-900">--%>
<%--        <c:choose>--%>
<%--          <c:when test="${sessionScope.user['class'].simpleName eq 'DormitoryManager'}">宿管位置信息</c:when>--%>
<%--          <c:when test="${sessionScope.user['class'].simpleName eq 'SysUser'}">--%>
<%--            <c:choose>--%>
<%--              <c:when test="${not empty jobRole and fn:toLowerCase(jobRole) eq 'counselor'}">辅导员位置信息</c:when>--%>
<%--              <c:when test="${not empty jobRole and fn:toLowerCase(jobRole) eq 'class_teacher'}">班主任位置信息</c:when>--%>
<%--              <c:when test="${not empty jobRole and fn:toLowerCase(jobRole) eq 'dean'}">院级领导位置信息</c:when>--%>
<%--              <c:otherwise>系统用户位置信息</c:otherwise>--%>
<%--            </c:choose>--%>
<%--          </c:when>--%>
<%--        </c:choose>--%>
<%--      </h2>--%>
<%--    </div>--%>
<%--    <div id="map-container" style="width:100%;height:300px;"></div>--%>
<%--    <div id="location-info" class="mt-4 text-sm text-gray-600"></div>--%>
<%--  </div>--%>

  <!-- 报警学生信息展示区域 -->
  <div class="card p-6 mb-6">
    <div class="flex items-center mb-4">
      <div class="p-2 rounded-full bg-red-100 text-red-600 mr-3">
        <i class="fas fa-exclamation-triangle text-lg"></i>
      </div>
      <h2 class="text-lg font-medium text-gray-900">报警学生信息</h2>
    </div>
    <div id="alarmStudentsList" class="space-y-4">
      <!-- 报警学生信息将通过JavaScript动态加载 -->
    </div>
  </div>

  <!-- 最近通知 -->
  <div class="card p-6 mb-6 w-full">
    <div class="flex justify-between items-center mb-4">
      <h2 class="text-lg font-medium text-gray-900">最近通知</h2>
      <div class="flex items-center space-x-4">
        <!-- 简化版固定为每页5条，隐藏分页大小选择器 -->
        <!-- <select id="pageSizeSelect" class="form-select form-select-sm" onchange="changePageSize()">
          <option value="5">5条/页</option>
          <option value="10">10条/页</option>
          <option value="20">20条/页</option>
        </select> -->
        <!-- todo 待实现-->
        <a href="/notifications" class="text-sm text-blue-600 hover:text-blue-800">查看全部</a>
      </div>
    </div>
    <div id="notificationList" class="space-y-4">
      <!-- 通知列表将通过 JavaScript 动态加载 -->
    </div>
    <!-- 分页控件 -->
    <div id="notificationPagination" class="mt-4 flex justify-center items-center space-x-2">
      <!-- 分页按钮将通过 JavaScript 动态加载 -->
    </div>
  </div>

  <!-- 主要内容区域 -->
  <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
    <!--待审核的晚归说明-->
    <div class="card p-6">
      <div class="flex justify-between items-center mb-4">
        <h2 class="text-lg font-medium text-gray-900">待审核的晚归说明</h2>
        <div class="flex items-center space-x-4">
          <select id="lateReturnPageSizeSelect" class="form-select form-select-sm" onchange="changeLateReturnPageSize()">
            <option value="5">5条/页</option>
            <option value="10">10条/页</option>
            <option value="20">20条/页</option>
          </select>
          <!-- todo -->
          <a href="/late-records" class="text-sm text-blue-600 hover:text-blue-800">查看全部</a>
        </div>
      </div>
      <div class="overflow-x-auto">
        <table class="min-w-full divide-y divide-gray-200">
          <thead class="bg-gray-50">
          <tr>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">学号</th>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">姓名</th>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">晚归时间</th>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">操作</th>
          </tr>
          </thead>
          <tbody id="lateReturnTableBody" class="bg-white divide-y divide-gray-200">
          <!-- 数据将通过JavaScript动态加载 -->
          </tbody>
        </table>
      </div>
      <!-- 分页控件 -->
      <div id="lateReturnPagination" class="mt-4 flex justify-center items-center space-x-2">
        <!-- 分页按钮将通过JavaScript动态加载 -->
      </div>
    </div>

    <!--待审核的晚归申请-->
    <div class="card p-6">
      <div class="flex justify-between items-center mb-4">
        <h2 class="text-lg font-medium text-gray-900">待审核的晚归申请</h2>
        <div class="flex items-center space-x-4">
          <select id="applicationPageSizeSelect" class="form-select form-select-sm" onchange="changeApplicationPageSize()">
            <option value="5">5条/页</option>
            <option value="10">10条/页</option>
            <option value="20">20条/页</option>
          </select>
          <!-- todo -->
          <a href="/applications" class="text-sm text-blue-600 hover:text-blue-800">查看全部</a>
        </div>
      </div>
      <div class="overflow-x-auto">
        <table class="min-w-full divide-y divide-gray-200">
          <thead class="bg-gray-50">
          <tr>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">学号</th>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">申请时间</th>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">预计返校时间</th>
            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">操作</th>
          </tr>
          </thead>
          <tbody id="applicationTableBody" class="bg-white divide-y divide-gray-200">
          <!-- 数据将通过JavaScript动态加载 -->
          </tbody>
        </table>
      </div>
      <!-- 分页控件 -->
      <div id="applicationPagination" class="mt-4 flex justify-center items-center space-x-2">
        <!-- 分页按钮将通过JavaScript动态加载 -->
      </div>
    </div>
  </div>


</div>

<!-- 引入 Bootstrap CSS -->
<link href="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/css/bootstrap.min.css" rel="stylesheet">
<!-- 引入 jQuery -->
<script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
<!-- 引入 Bootstrap JS 和 Popper.js -->
<script src="https://cdn.jsdelivr.net/npm/@popperjs/core@2.10.2/dist/umd/popper.min.js"></script>
<script src="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/js/bootstrap.min.js"></script>
<!-- 引入验证脚本 -->
<script type="text/javascript" src="${pageContext.request.contextPath}/static/js/dorman-validation.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/static/js/sysuser-validation.js"></script>

<!-- 腾讯地图API已在页面头部引入 -->

<script>
  $.ajaxSetup({
    beforeSend: function(xhr) {
      var loginUUID = localStorage.getItem('loginUUID');
      if (loginUUID) {
        xhr.setRequestHeader('X-Login-UUID', loginUUID);
      }
    }
  });

  // 数据验证
  <c:if test="${empty sessionScope.user}">
  window.location.href = "${pageContext.request.contextPath}/register";
  </c:if>

  $(document).ready(function() {
    // 添加CSS动画
    $('<style>')
      .prop('type', 'text/css')
      .html(`
        @keyframes slideInUp {
          from {
            opacity: 0;
            transform: translateY(30px);
          }
          to {
            opacity: 1;
            transform: translateY(0);
          }
        }
      `)
      .appendTo('head');

    // 页面初始化
    initializePage();
    
    // 修改个人信息按钮点击事件
    $('button[data-bs-toggle="modal"]').click(function() {
      <c:choose>
      <c:when test="${sessionScope.user['class'].simpleName eq 'DormitoryManager'}">
      $('#dormitoryManagerModal').modal('show');
      </c:when>
      <c:when test="${sessionScope.user['class'].simpleName eq 'SysUser'}">
      $('#systemUserModal').modal('show');
      </c:when>
      </c:choose>
    });

    // 宿管保存按钮点击事件
    $('#saveDormitoryManagerBtn').click(function() {
      if (!validateDormanForm()) {
        return;
      }

      var formData = {};
      $('#dormitoryManagerForm').serializeArray().forEach(function(item) {
        formData[item.name] = item.value;
      });

      $.ajax({
        url: '${pageContext.request.contextPath}/dorman/update/per-info',
        type: 'POST',
        contentType: 'application/json',
        data: JSON.stringify(formData),
        success: function(response) {
          if (response.success) {
            alert('个人信息更新成功');
            $('#dormitoryManagerModal').modal('hide');
            location.reload();
          } else {
            alert(response.message || '更新失败，请重试');
          }
        },
        error: function() {
          alert('更新失败，请重试');
        }
      });
    });

    // 系统用户保存按钮点击事件
    $('#saveSystemUserBtn').click(function() {
      // 验证表单
      if (!validateSysUserForm()) {
        return;
      }

      var formData = {};
      $('#systemUserForm').serializeArray().forEach(function(item) {
        formData[item.name] = item.value;
      });

      $.ajax({
        url: '${pageContext.request.contextPath}/sysuser/update/per-info',
        type: 'POST',
        contentType: 'application/json',
        data: JSON.stringify(formData),
        success: function(response) {
          if (response.success) {
            alert('个人信息更新成功');
            $('#systemUserModal').modal('hide');
            location.reload();
          } else {
            alert(response.message || '更新失败，请重试');
          }
        },
        error: function() {
          alert('更新失败，请重试');
        }
      });
    });

    // 取消按钮关闭模态框
    $('.btn-secondary').click(function() {
      $('#dormitoryManagerModal, #systemUserModal').modal('hide');
    });

    // 右上角关闭按钮关闭模态框
    $('.btn-close').click(function() {
      $('#dormitoryManagerModal, #systemUserModal').modal('hide');
    });
  });

  function handleLogout() {
    if (confirm('确定要退出登录吗？')) {
      localStorage.removeItem('loginUUID');
      window.location.href = '${pageContext.request.contextPath}/logout';
    }
  }

  /**
   * 加载晚归统计数据
   * 从后端获取统计数据并更新到页面上
   */
  function loadStatistics(startDate, endDate) {
    // 如果没有传入日期参数，使用默认值（当天）
    if (!startDate) {
      const now = new Date();
      endDate = now.toISOString().slice(0, 19).replace('T', ' ');

      const today = new Date(now);
      today.setHours(0, 0, 0, 0);
      startDate = today.toISOString().slice(0, 19).replace('T', ' ');
    }

    $.ajax({
      url: '${pageContext.request.contextPath}/late-return/stats',
      type: 'POST',
      data: {
        startDateStr: startDate,
        endTimeStr: endDate
      },
      success: function(response) {
        if (response.success) {
          const data = response.data;
          $('.card:nth-child(1) .text-2xl').text(Number(data.statPeriodCount) || 0);
          $('.card:nth-child(2) .text-2xl').text(Number(data.statPendingCount) || 0);
          $('.card:nth-child(3) .text-2xl').text(Number(data.statProcessingCount) || 0);
          $('.card:nth-child(4) .text-2xl').text(Number(data.statFinishedCount) || 0);
        } else {
          showError('获取统计数据失败: ' + response.message);
        }
      },
      error: function(xhr, status, error) {
        showError('获取统计数据失败，请稍后重试');
      }
    });
  }

  /**
   * 查询按钮点击事件处理函数
   */
  function queryStatistics() {
    const startDate = $('#startDate').val();
    const endDate = $('#endDate').val();

    if (!startDate || !endDate) {
      showError('请选择开始日期和结束日期');
      return;
    }

    // 转换日期格式为后端需要的格式 (yyyy-MM-dd HH:mm:ss)
    var startDateTime = startDate + ' 00:00:00';
    var endDateTime = endDate + ' 23:59:59';

    loadStatistics(startDateTime, endDateTime);
  }

  /**
   * 显示错误提示
   * @param {string} message 错误信息
   */
  function showError(message) {
    // 如果页面上有错误提示容器，则显示错误信息
    const errorContainer = $('#errorContainer');
    if (errorContainer.length) {
      errorContainer.text(message).show();
      // 3秒后自动隐藏
      setTimeout(() => {
        errorContainer.hide();
      }, 3000);
    } else {
      // 如果没有错误提示容器，则使用alert
      alert(message);
    }
  }

  // v1版本的通知业务前端代码
  <%--// 全局变量存储分页信息--%>
  <%--var currentPage = 1;--%>
  <%--var pageSize = 5;--%>
  <%--var totalPages = 1;--%>

  <%--function loadNotifications(page) {--%>
  <%--  if (page) {--%>
  <%--    currentPage = page;--%>
  <%--  }--%>

  <%--  var query = {--%>
  <%--    pageNum: currentPage,--%>
  <%--    pageSize: pageSize,--%>
  <%--    status: 'UNREAD',--%>
  <%--    targetId: '${targetId}'  // 使用根据角色设置的targetId--%>
  <%--  };--%>

  <%--  $.ajax({--%>
  <%--    url: '${pageContext.request.contextPath}/notification/unread-page-list',--%>
  <%--    type: 'POST',--%>
  <%--    contentType: 'application/json',--%>
  <%--    data: JSON.stringify(query),--%>
  <%--    success: function(response) {--%>
  <%--      if (response.success) {--%>
  <%--        var notifications = response.data.data;--%>
  <%--        var total = response.data.total;--%>
  <%--        totalPages = Math.ceil(total / pageSize);--%>

  <%--        updateNotificationList(notifications);--%>
  <%--        updatePagination(currentPage, totalPages);--%>
  <%--        updateTotalCount(total);--%>
  <%--      } else {--%>
  <%--        showError('获取通知失败: ' + response.message);--%>
  <%--      }--%>
  <%--    },--%>
  <%--    error: function(xhr, status, error) {--%>
  <%--      showError('获取通知失败，请稍后重试');--%>
  <%--    }--%>
  <%--  });--%>
  <%--}--%>

  <%--function updateTotalCount(total) {--%>
  <%--  var totalText = '共 ' + total + ' 条通知';--%>
  <%--  if (!$('#totalCount').length) {--%>
  <%--    $('#notificationPagination').prepend('<span id="totalCount" class="text-sm text-gray-600 mr-4"></span>');--%>
  <%--  }--%>
  <%--  $('#totalCount').text(totalText);--%>
  <%--}--%>

  <%--function changePageSize() {--%>
  <%--  pageSize = parseInt($('#pageSizeSelect').val());--%>
  <%--  currentPage = 1;--%>
  <%--  loadNotifications();--%>
  <%--}--%>

  <%--function updatePagination(currentPage, totalPages) {--%>
  <%--  var container = $('#notificationPagination');--%>
  <%--  var totalCount = $('#totalCount').detach();--%>
  <%--  container.empty();--%>
  <%--  container.prepend(totalCount);--%>

  <%--  // 只有在没有数据时才不显示分页--%>
  <%--  if (totalPages === 0) {--%>
  <%--    return;--%>
  <%--  }--%>

  <%--  // 始终显示上一页按钮（除非是第一页）--%>
  <%--  if (currentPage > 1) {--%>
  <%--    container.append(--%>
  <%--            '<button onclick="loadNotifications(' + (currentPage - 1) + ')" ' +--%>
  <%--            'class="px-3 py-1 rounded bg-gray-200 hover:bg-gray-300">' +--%>
  <%--            '上一页' +--%>
  <%--            '</button>'--%>
  <%--    );--%>
  <%--  }--%>

  <%--  var startPage = Math.max(1, currentPage - 2);--%>
  <%--  var endPage = Math.min(totalPages, startPage + 4);--%>
  <%--  if (endPage - startPage < 4) {--%>
  <%--    startPage = Math.max(1, endPage - 4);--%>
  <%--  }--%>

  <%--  if (startPage > 1) {--%>
  <%--    container.append(--%>
  <%--            '<button onclick="loadNotifications(1)" ' +--%>
  <%--            'class="px-3 py-1 rounded bg-gray-200 hover:bg-gray-300">' +--%>
  <%--            '1' +--%>
  <%--            '</button>'--%>
  <%--    );--%>
  <%--    if (startPage > 2) {--%>
  <%--      container.append('<span class="px-2">...</span>');--%>
  <%--    }--%>
  <%--  }--%>

  <%--  for (var i = startPage; i <= endPage; i++) {--%>
  <%--    if (i === currentPage) {--%>
  <%--      container.append(--%>
  <%--              '<button class="px-3 py-1 rounded bg-blue-500 text-white">' +--%>
  <%--              i +--%>
  <%--              '</button>'--%>
  <%--      );--%>
  <%--    } else {--%>
  <%--      container.append(--%>
  <%--              '<button onclick="loadNotifications(' + i + ')" ' +--%>
  <%--              'class="px-3 py-1 rounded bg-gray-200 hover:bg-gray-300">' +--%>
  <%--              i +--%>
  <%--              '</button>'--%>
  <%--      );--%>
  <%--    }--%>
  <%--  }--%>

  <%--  if (endPage < totalPages) {--%>
  <%--    if (endPage < totalPages - 1) {--%>
  <%--      container.append('<span class="px-2">...</span>');--%>
  <%--    }--%>
  <%--    container.append(--%>
  <%--            '<button onclick="loadNotifications(' + totalPages + ')" ' +--%>
  <%--            'class="px-3 py-1 rounded bg-gray-200 hover:bg-gray-300">' +--%>
  <%--            totalPages +--%>
  <%--            '</button>'--%>
  <%--    );--%>
  <%--  }--%>

  <%--  // 始终显示下一页按钮（除非是最后一页）--%>
  <%--  if (currentPage < totalPages) {--%>
  <%--    container.append(--%>
  <%--            '<button onclick="loadNotifications(' + (currentPage + 1) + ')" ' +--%>
  <%--            'class="px-3 py-1 rounded bg-gray-200 hover:bg-gray-300">' +--%>
  <%--            '下一页' +--%>
  <%--            '</button>'--%>
  <%--    );--%>
  <%--  }--%>
  <%--}--%>

  <%--/**--%>
  <%-- * 更新通知列表--%>
  <%-- * @param {Array} notifications 通知数据数组--%>
  <%-- */--%>
  <%--function updateNotificationList(notifications) {--%>
  <%--  const container = $('#notificationList');--%>
  <%--  container.empty();--%>

  <%--  if (notifications.length === 0) {--%>
  <%--    container.html('<p class="text-gray-500 text-center">暂无通知</p>');--%>
  <%--    return;--%>
  <%--  }--%>

  <%--  notifications.forEach(function(notification) {--%>
  <%--    var notificationHtml =--%>
  <%--            '<div class="border-l-4 border-' + getStatusColor(notification.status) + ' pl-4 py-2">' +--%>
  <%--            '<h3 class="text-sm font-medium text-gray-900">' + notification.title + '</h3>' +--%>
  <%--            '<p class="text-sm text-gray-600 mt-1">' + notification.content + '</p>' +--%>
  <%--            '<p class="text-xs text-gray-500 mt-1">' + formatDate(notification.noticeTime) + '</p>' +--%>
  <%--            '</div>';--%>
  <%--    container.append(notificationHtml);--%>
  <%--  });--%>
  <%--}--%>

  <%--/**--%>
  <%-- * 获取通知状态对应的颜色--%>
  <%-- * @param {string} status 通知状态--%>
  <%-- * @returns {string} 颜色类名--%>
  <%-- */--%>
  <%--function getStatusColor(status) {--%>
  <%--  switch (status) {--%>
  <%--    case 'UNREAD':--%>
  <%--      return 'blue-500';--%>
  <%--    case 'READ':--%>
  <%--      return 'green-500';--%>
  <%--    default:--%>
  <%--      return 'gray-500';--%>
  <%--  }--%>
  <%--}--%>

  // ========== 简化版通知系统（仅宿管） ==========
  // 通知分页相关变量
  let notificationCurrentPage = 1;
  const notificationPageSize = 5; // 固定每页5条

  // 加载简化版通知消息（宿管）
  function loadNotifications() {
    // 获取宿管工号作为 targetId
    var targetId = '${sessionScope.user.managerId}';
    console.log("targetId" + targetId);
    
    if (!targetId) {
      console.error('无法获取宿管工号');
      $('#notificationList').html('<p class="text-gray-500 text-center">无法加载通知</p>');
      return;
    }

    $.ajax({
      url: '${pageContext.request.contextPath}/notification/simple/page',
      type: 'POST',
      contentType: 'application/json',
      data: JSON.stringify({
        targetId: targetId,
        pageNum: notificationCurrentPage,
        pageSize: notificationPageSize
      }),
      success: function(response) {
        if (response.success) {
          const pageResult = response.data;
          updateNotificationList(pageResult.data);
          updateNotificationPagination(pageResult);
        } else {
          console.error('加载通知失败:', response.message);
          $('#notificationList').html(`
            <div class="text-center text-red-500 py-4">
              加载失败：${response.message || '未知错误'}
            </div>
          `);
        }
      },
      error: function(xhr, status, error) {
        console.error('加载通知消息失败:', {
          status: status,
          error: error,
          response: xhr.responseText
        });
        $('#notificationList').html(`
          <div class="text-center text-red-500 py-4">
            加载失败，请稍后重试
          </div>
        `);
      }
    });
  }

  // 更新通知列表显示
  function updateNotificationList(notifications) {
    const notificationList = $('#notificationList');
    notificationList.empty();
    
    if (!notifications || notifications.length === 0) {
      notificationList.append(`
        <div class="text-center text-gray-500 py-4">
          暂无通知
        </div>
      `);
      return;
    }

    notifications.forEach(function(notification) {
      // 格式化日期时间
      var createTime = formatNotificationDateTime(notification.createTime);
      
      var notificationHtml = 
        '<div class="border-l-4 border-gray-300 pl-4 py-3 mb-4 hover:bg-gray-50">' +
          '<div class="flex justify-between items-start">' +
            '<div class="flex-1">' +
              '<p class="text-sm text-gray-700 mt-1">' + (notification.content || '-') + '</p>' +
              '<p class="text-xs text-gray-400 mt-2">' + createTime + '</p>' +
            '</div>' +
          '</div>' +
        '</div>';
      notificationList.append(notificationHtml);
    });
  }

  // 将后端返回的本地时间字符串解析为本地 Date，避免浏览器按 UTC 解释导致时区偏移
  function parseLocalDateTime(dateTime) {
    if (!dateTime) return null;
    if (typeof dateTime === 'number') {
      const timestampDate = new Date(dateTime);
      return isNaN(timestampDate.getTime()) ? null : timestampDate;
    }
    if (dateTime instanceof Date) {
      return isNaN(dateTime.getTime()) ? null : dateTime;
    }

    const normalized = String(dateTime).trim().replace('T', ' ');
    const match = normalized.match(/^(\d{4})-(\d{2})-(\d{2})\s+(\d{2}):(\d{2})(?::(\d{2}))?$/);
    if (match) {
      const year = Number(match[1]);
      const month = Number(match[2]) - 1;
      const day = Number(match[3]);
      const hour = Number(match[4]);
      const minute = Number(match[5]);
      const second = Number(match[6] || 0);
      return new Date(year, month, day, hour, minute, second);
    }

    const fallbackDate = new Date(dateTime);
    return isNaN(fallbackDate.getTime()) ? null : fallbackDate;
  }

  // 日期时间格式化函数（通知专用）
  function formatNotificationDateTime(dateTime) {
    const date = parseLocalDateTime(dateTime);
    if (!date) return dateTime || '';
    return date.toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
      hour12: false
    });
  }

  // 更新通知分页控件
  function updateNotificationPagination(pageResult) {
    const total = pageResult.total;
    const totalPages = Math.ceil(total / notificationPageSize);
    
    // 保存总数到 notificationList 元素，用于分页计算
    $('#notificationList').data('total', total);
    
    const pagination = $('#notificationPagination');
    pagination.empty();
    
    if (totalPages === 0) {
      return;
    }

    // 上一页按钮
    var prevButton = '<button onclick="changeNotificationPage(' + (notificationCurrentPage - 1) + ')" ' +
            'class="px-3 py-1 rounded ' + (notificationCurrentPage === 1 ? 'bg-gray-200 text-gray-500 cursor-not-allowed' : 'bg-blue-500 text-white hover:bg-blue-600') + '" ' +
            (notificationCurrentPage === 1 ? 'disabled' : '') + '>' +
            '上一页' +
            '</button>';
    pagination.append(prevButton);

    // 当前页/总页数
    var pageInfo = '<span class="px-3 py-1 text-sm text-gray-600">' +
            notificationCurrentPage + ' / ' + totalPages +
            '</span>';
    pagination.append(pageInfo);

    // 下一页按钮
    var nextButton = '<button onclick="changeNotificationPage(' + (notificationCurrentPage + 1) + ')" ' +
            'class="px-3 py-1 rounded ' + (notificationCurrentPage >= totalPages ? 'bg-gray-200 text-gray-500 cursor-not-allowed' : 'bg-blue-500 text-white hover:bg-blue-600') + '" ' +
            (notificationCurrentPage >= totalPages ? 'disabled' : '') + '>' +
            '下一页' +
            '</button>';
    pagination.append(nextButton);
  }

  // 切换通知页码
  function changeNotificationPage(page) {
    if (page < 1) return;
    
    notificationCurrentPage = page;
    loadNotifications();
  }

  // 修改通知每页显示数量（固定为5条，此函数保留但不改变大小）
  function changePageSize() {
    // 简化版固定为5条，不执行任何操作
    // 保留函数以避免报错
    console.log('简化版通知固定为每页5条');
  }
  // ========== 简化版通知系统结束 ==========

  /**
   * 格式化日期
   * @param {string} dateStr 日期字符串 (MySQL datetime格式: YYYY-MM-DD HH:mm:ss)
   * @returns {string} 格式化后的日期字符串
   */
  function formatDate(dateStr) {
    if (!dateStr) {
      return '';
    }

    // 以本地时间解析，避免时区偏移
    var date = parseLocalDateTime(dateStr);

    if (!date) {
      return '';
    }

    var year = date.getFullYear();
    var month = ('0' + (date.getMonth() + 1)).slice(-2);
    var day = ('0' + date.getDate()).slice(-2);
    var hours = ('0' + date.getHours()).slice(-2);
    var minutes = ('0' + date.getMinutes()).slice(-2);

    return year + '-' + month + '-' + day + ' ' + hours + ':' + minutes;
  }

  // 页面加载时初始化日期选择器
  $(document).ready(function() {
    // 设置默认日期为今天
    const today = new Date().toISOString().split('T')[0];
    $('#startDate').val(today);
    $('#endDate').val(today);

    // 初始化分页大小
    lateReturnPageSize = parseInt($('#lateReturnPageSizeSelect').val());

    // 加载初始数据
    loadStatistics();
    loadNotifications();  // 加载第一页通知
    loadLateReturns();    // 加载晚归记录
    loadApplications();   // 加载晚归申请
    loadAlarmContacts();  // 页面加载时获取报警学生信息

    // 每10分钟刷新一次统计数据
    setInterval(loadStatistics, 10 * 60 * 1000);
  });

  // 晚归记录分页相关变量
  let lateReturnCurrentPage = 1;
  let lateReturnPageSize = 5;
  let lateReturnTotalPages = 1;

  // 加载晚归记录
  function loadLateReturns() {
    // 获取时间范围
    var startDate = $('#startDate').val();
    var endDate = $('#endDate').val();

    // 如果没有选择日期，使用默认值（当天）
    if (!startDate) {
      var today = new Date();
      startDate = today.toISOString().split('T')[0];
      $('#startDate').val(startDate);
    }
    if (!endDate) {
      var today = new Date();
      endDate = today.toISOString().split('T')[0];
      $('#endDate').val(endDate);
    }

    // 转换日期格式为后端需要的格式 (yyyy-MM-dd HH:mm:ss)
    var startDateTime = startDate + ' 00:00:00';
    var endDateTime = endDate + ' 23:59:59';

    const query = {
      pageNum: lateReturnCurrentPage,
      pageSize: lateReturnPageSize,
      processStatus: 'PENDING', // 只获取待处理的记录
      startLateTime: startDateTime,
      endLateTime: endDateTime
    };

    $.ajax({
      url: '${pageContext.request.contextPath}/late-return/pageList',
      type: 'POST',
      contentType: 'application/json',
      data: JSON.stringify(query),
      success: function(response) {
        if (response.success) {
          const data = response.data;
          renderLateReturns(data.data);
          renderLateReturnPagination(data.total, data.totalPages);
        } else {
          alert('加载数据失败：' + response.message);
        }
      },
      error: function(xhr, status, error) {
        alert('加载数据失败：' + error);
      }
    });
  }

  // 渲染晚归记录表格
  function renderLateReturns(records) {
    const tbody = $('#lateReturnTableBody');
    tbody.empty();

    records.forEach(function(record) {
      var row = '<tr>' +
              '<td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">' + record.studentNo + '</td>' +
              '<td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">' + record.studentName + '</td>' +
              '<td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">' + formatDateTime(record.lateTime) + '</td>' +
              '<td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">' +
              '<button onclick="handleLateReturn(\'' + record.lateReturnId + '\')" class="text-blue-600 hover:text-blue-800">处理</button>' +
              '</td>' +
              '</tr>';
      tbody.append(row);
    });
  }

  // 渲染晚归记录分页控件
  function renderLateReturnPagination(total, pages) {
    lateReturnTotalPages = pages;
    const pagination = $('#lateReturnPagination');
    pagination.empty();

    // 上一页按钮
    var prevButton = '<button onclick="changeLateReturnPage(' + (lateReturnCurrentPage - 1) + ')" ' +
            'class="px-3 py-1 rounded ' + (lateReturnCurrentPage === 1 ? 'bg-gray-200 text-gray-500 cursor-not-allowed' : 'bg-blue-500 text-white hover:bg-blue-600') + '" ' +
            (lateReturnCurrentPage === 1 ? 'disabled' : '') + '>' +
            '上一页' +
            '</button>';
    pagination.append(prevButton);

    // 页码按钮
    for (var i = 1; i <= pages; i++) {
      var pageButton = '<button onclick="changeLateReturnPage(' + i + ')" ' +
              'class="px-3 py-1 rounded ' + (lateReturnCurrentPage === i ? 'bg-blue-500 text-white' : 'bg-gray-200 text-gray-700 hover:bg-gray-300') + '">' +
              i +
              '</button>';
      pagination.append(pageButton);
    }

    // 下一页按钮
    var nextButton = '<button onclick="changeLateReturnPage(' + (lateReturnCurrentPage + 1) + ')" ' +
            'class="px-3 py-1 rounded ' + (lateReturnCurrentPage === pages ? 'bg-gray-200 text-gray-500 cursor-not-allowed' : 'bg-blue-500 text-white hover:bg-blue-600') + '" ' +
            (lateReturnCurrentPage === pages ? 'disabled' : '') + '>' +
            '下一页' +
            '</button>';
    pagination.append(nextButton);
  }

  // 切换晚归记录页码
  function changeLateReturnPage(page) {
    if (page < 1 || page > lateReturnTotalPages) return;
    lateReturnCurrentPage = page;
    loadLateReturns();
  }

  // 修改晚归记录每页显示数量
  function changeLateReturnPageSize() {
    lateReturnPageSize = parseInt($('#lateReturnPageSizeSelect').val());
    lateReturnCurrentPage = 1; // 重置到第一页
    loadLateReturns();
  }

  // 处理晚归记录
  function handleLateReturn(lateReturnId) {
    // 跳转到处理页面，并传递晚归记录ID
    window.location.href = '${pageContext.request.contextPath}/late-return/handle/' + lateReturnId;
  }

  // 格式化日期时间
  function formatDateTime(dateStr) {
    const date = parseLocalDateTime(dateStr);
    if (!date) return dateStr || '';
    return date.toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
      hour12: false
    });
  }

  // ========== 晚归申请分页相关变量 ==========
  let applicationCurrentPage = 1;
  let applicationPageSize = 5;
  let applicationTotalPages = 1;

  // 加载晚归申请
  function loadApplications() {
    // 获取时间范围
    var startDate = $('#startDate').val();
    var endDate = $('#endDate').val();

    // 如果没有选择日期，使用默认值（当天）
    if (!startDate) {
      var today = new Date();
      startDate = today.toISOString().split('T')[0];
      $('#startDate').val(startDate);
    }
    if (!endDate) {
      var today = new Date();
      endDate = today.toISOString().split('T')[0];
      $('#endDate').val(endDate);
    }

    // 转换日期格式为后端需要的格式 (yyyy-MM-dd HH:mm:ss)
    var startDateTime = startDate + ' 00:00:00';
    var endDateTime = endDate + ' 23:59:59';

    const query = {
      pageNum: applicationCurrentPage,
      pageSize: applicationPageSize,
      auditStatus: 0, // 只获取待审核的记录 (0待审核,1通过,2驳回)
      applyTimeStart: startDateTime,
      applyTimeEnd: endDateTime
    };

    $.ajax({
      url: '${pageContext.request.contextPath}/application/pageList',
      type: 'POST',
      contentType: 'application/json',
      data: JSON.stringify(query),
      success: function(response) {
        if (response.success) {
          const data = response.data;
          renderApplications(data.data);
          renderApplicationPagination(data.total, data.totalPages);
        } else {
          alert('加载数据失败：' + response.message);
        }
      },
      error: function(xhr, status, error) {
        alert('加载数据失败：' + error);
      }
    });
  }

  // 渲染晚归申请表格
  function renderApplications(applications) {
    const tbody = $('#applicationTableBody');
    tbody.empty();

    if (!applications || applications.length === 0) {
      tbody.append('<tr><td colspan="4" class="px-6 py-4 text-center text-sm text-gray-500">暂无待审核的晚归申请</td></tr>');
      return;
    }

    applications.forEach(function(application) {
      var row = '<tr>' +
              '<td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">' + (application.studentNo || '') + '</td>' +
              '<td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">' + (application.applyTime ? formatDateTime(application.applyTime) : '') + '</td>' +
              '<td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">' + (application.expectedReturnTime ? formatDateTime(application.expectedReturnTime) : '') + '</td>' +
              '<td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">' +
              '<button onclick="showApplicationDetail(\'' + application.applicationId + '\')" class="text-blue-600 hover:text-blue-800 mr-3">详情</button>' +
              '<button onclick="showApplicationAuditModal(\'' + application.applicationId + '\')" class="text-green-600 hover:text-green-800">审核</button>' +
              '</td>' +
              '</tr>';
      tbody.append(row);
    });
  }

  // 渲染晚归申请分页控件
  function renderApplicationPagination(total, pages) {
    applicationTotalPages = pages;
    const pagination = $('#applicationPagination');
    pagination.empty();

    if (pages <= 1) {
      return; // 只有一页或没有数据时不显示分页
    }

    // 上一页按钮
    var prevButton = '<button onclick="changeApplicationPage(' + (applicationCurrentPage - 1) + ')" ' +
            'class="px-3 py-1 rounded ' + (applicationCurrentPage === 1 ? 'bg-gray-200 text-gray-500 cursor-not-allowed' : 'bg-blue-500 text-white hover:bg-blue-600') + '" ' +
            (applicationCurrentPage === 1 ? 'disabled' : '') + '>' +
            '上一页' +
            '</button>';
    pagination.append(prevButton);

    // 页码按钮
    for (var i = 1; i <= pages; i++) {
      var pageButton = '<button onclick="changeApplicationPage(' + i + ')" ' +
              'class="px-3 py-1 rounded ' + (applicationCurrentPage === i ? 'bg-blue-500 text-white' : 'bg-gray-200 text-gray-700 hover:bg-gray-300') + '">' +
              i +
              '</button>';
      pagination.append(pageButton);
    }

    // 下一页按钮
    var nextButton = '<button onclick="changeApplicationPage(' + (applicationCurrentPage + 1) + ')" ' +
            'class="px-3 py-1 rounded ' + (applicationCurrentPage === pages ? 'bg-gray-200 text-gray-500 cursor-not-allowed' : 'bg-blue-500 text-white hover:bg-blue-600') + '" ' +
            (applicationCurrentPage === pages ? 'disabled' : '') + '>' +
            '下一页' +
            '</button>';
    pagination.append(nextButton);
  }

  // 切换晚归申请页码
  function changeApplicationPage(page) {
    if (page < 1 || page > applicationTotalPages) return;
    applicationCurrentPage = page;
    loadApplications();
  }

  // 修改晚归申请每页显示数量
  function changeApplicationPageSize() {
    applicationPageSize = parseInt($('#applicationPageSizeSelect').val());
    applicationCurrentPage = 1; // 重置到第一页
    loadApplications();
  }

  // 显示晚归申请详情
  function showApplicationDetail(applicationId) {
    // 通过AJAX获取申请详情
    $.ajax({
      url: '${pageContext.request.contextPath}/application/' + applicationId,
      type: 'GET',
      success: function(response) {
        if (response.success && response.data) {
          const application = response.data;
          // 显示详情弹框
          showApplicationDetailModal(application);
      } else {
          alert('获取申请详情失败：' + (response.message || '未知错误'));
        }
      },
      error: function(xhr, status, error) {
        alert('获取申请详情失败：' + error);
      }
    });
  }

  // 处理附件URL，转换为预览URL
  function getFilePreviewUrl(attachmentUrl) {
    if (!attachmentUrl) return null;
    
    // 移除开头的 /uploads 或 uploads 前缀
    var relativePath = attachmentUrl.trim();
    var uploadsPrefix = '/uploads/';
    var uploadsPrefixNoSlash = 'uploads/';
    var uploadsPrefixOnly = '/uploads';
    
    if (relativePath.startsWith(uploadsPrefix)) {
      relativePath = relativePath.substring(uploadsPrefix.length);
    } else if (relativePath.startsWith(uploadsPrefixNoSlash)) {
      relativePath = relativePath.substring(uploadsPrefixNoSlash.length);
    } else if (relativePath.startsWith(uploadsPrefixOnly)) {
      relativePath = relativePath.substring(uploadsPrefixOnly.length);
      // 移除可能的前导斜杠
      if (relativePath.startsWith('/')) {
        relativePath = relativePath.substring(1);
      }
    }
    
    // 移除前导斜杠（如果有）
    relativePath = relativePath.replace(/^\/+/, '');
    
    // 构建预览URL
    var contextPath = '${pageContext.request.contextPath}';
    return contextPath + '/file/preview/' + relativePath;
  }

  // 判断文件类型
  function getFileType(fileUrl) {
    if (!fileUrl) return 'unknown';
    
    var extension = fileUrl.toLowerCase().substring(fileUrl.lastIndexOf('.') + 1);
    var imageTypes = ['jpg', 'jpeg', 'png', 'gif', 'webp', 'bmp'];
    var pdfType = 'pdf';
    
    if (imageTypes.indexOf(extension) !== -1) {
      return 'image';
    } else if (extension === pdfType) {
      return 'pdf';
    }
    return 'other';
  }

  // 显示晚归申请详情弹框
  function showApplicationDetailModal(application) {
    // 构建弹框HTML
    var modalHtml = '<div class="fixed inset-0 bg-gray-600 bg-opacity-50 overflow-y-auto h-full w-full z-50" id="applicationDetailModal">' +
            '<div class="relative top-20 mx-auto p-5 border w-11/12 md:w-3/4 lg:w-1/2 shadow-lg rounded-md bg-white max-h-[90vh] overflow-y-auto">' +
            '<div class="mt-3">' +
            '<div class="flex justify-between items-center mb-4">' +
            '<h3 class="text-lg font-medium text-gray-900">晚归申请详情</h3>' +
            '<button onclick="closeApplicationDetailModal()" class="text-gray-400 hover:text-gray-600">' +
            '<svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">' +
            '<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path>' +
            '</svg>' +
            '</button>' +
            '</div>' +
            '<div class="space-y-4">' +
            '<div><label class="text-sm font-medium text-gray-700">学号：</label><span class="ml-2 text-gray-900">' + (application.studentNo || '') + '</span></div>' +
            '<div><label class="text-sm font-medium text-gray-700">申请时间：</label><span class="ml-2 text-gray-900">' + (application.applyTime ? formatDateTime(application.applyTime) : '') + '</span></div>' +
            '<div><label class="text-sm font-medium text-gray-700">预计返校时间：</label><span class="ml-2 text-gray-900">' + (application.expectedReturnTime ? formatDateTime(application.expectedReturnTime) : '') + '</span></div>' +
            '<div><label class="text-sm font-medium text-gray-700">晚归原因：</label><div class="mt-1 text-gray-900">' + (application.reason || '') + '</div></div>';

    // 如果有附件，显示附件
    if (application.attachmentUrl) {
      var previewUrl = getFilePreviewUrl(application.attachmentUrl);
      var fileType = getFileType(application.attachmentUrl);
      
      modalHtml += '<div><label class="text-sm font-medium text-gray-700">证明材料：</label>' +
              '<div class="mt-2">';
      
      if (fileType === 'image') {
        // 图片类型：显示图片预览
        modalHtml += '<div class="border border-gray-200 rounded-lg p-2 bg-gray-50">' +
                '<img src="' + previewUrl + '" alt="证明材料" ' +
                'class="max-w-full h-auto mx-auto rounded cursor-pointer hover:opacity-90" ' +
                'onclick="window.open(\'' + previewUrl + '\', \'_blank\')" ' +
                'onerror="this.onerror=null; this.src=\'\'; this.parentElement.innerHTML=\'<p class=\\\'text-red-500 text-sm\\\'>图片加载失败</p>\';" ' +
                'style="max-height: 500px;">' +
                '</div>' +
                '<div class="mt-2 text-center">' +
                '<a href="' + previewUrl + '" target="_blank" class="text-sm text-blue-600 hover:text-blue-800">' +
                '在新窗口打开' +
                '</a>' +
                '</div>';
      } else if (fileType === 'pdf') {
        // PDF类型：使用iframe显示
        modalHtml += '<div class="border border-gray-200 rounded-lg overflow-hidden">' +
                '<iframe src="' + previewUrl + '" ' +
                'class="w-full" ' +
                'style="height: 600px;" ' +
                'onerror="this.onerror=null; this.parentElement.innerHTML=\'<p class=\\\'text-red-500 text-sm p-4\\\'>PDF加载失败，请<a href=\\\'' + previewUrl + '\\\' target=\\\'_blank\\\' class=\\\'text-blue-600\\\'>点击下载</a>查看</p>\';"></iframe>' +
                '</div>' +
                '<div class="mt-2 text-center">' +
                '<a href="' + previewUrl + '" target="_blank" class="text-sm text-blue-600 hover:text-blue-800">' +
                '在新窗口打开或下载' +
                '</a>' +
                '</div>';
      } else {
        // 其他类型：提供下载链接
        modalHtml += '<div class="border border-gray-200 rounded-lg p-4 bg-gray-50 text-center">' +
                '<p class="text-sm text-gray-600 mb-2">文件类型：' + (application.attachmentUrl.substring(application.attachmentUrl.lastIndexOf('.') + 1).toUpperCase() || '未知') + '</p>' +
                '<a href="' + previewUrl + '" target="_blank" ' +
                'class="inline-block px-4 py-2 bg-blue-500 text-white rounded hover:bg-blue-600">' +
                '下载文件' +
                '</a>' +
                '</div>';
      }
      
      modalHtml += '</div></div>';
    }

    modalHtml += '</div>' +
            '<div class="mt-6 flex justify-end">' +
            '<button onclick="closeApplicationDetailModal()" class="px-4 py-2 bg-gray-300 text-gray-700 rounded hover:bg-gray-400">关闭</button>' +
            '</div>' +
            '</div>' +
            '</div>' +
            '</div>';

    // 移除已存在的弹框
    $('#applicationDetailModal').remove();

    // 添加弹框到页面
    $('body').append(modalHtml);
  }

  // 关闭晚归申请详情弹框
  function closeApplicationDetailModal() {
    $('#applicationDetailModal').remove();
  }

  // 显示晚归申请审核弹框
  function showApplicationAuditModal(applicationId) {
    // 构建弹框HTML
    var modalHtml = '<div class="fixed inset-0 bg-gray-600 bg-opacity-50 overflow-y-auto h-full w-full z-50" id="applicationAuditModal">' +
            '<div class="relative top-20 mx-auto p-5 border w-11/12 md:w-3/4 lg:w-1/2 shadow-lg rounded-md bg-white max-h-[90vh] overflow-y-auto">' +
            '<div class="mt-3">' +
            '<div class="flex justify-between items-center mb-4">' +
            '<h3 class="text-lg font-medium text-gray-900">审核晚归申请</h3>' +
            '<button onclick="closeApplicationAuditModal()" class="text-gray-400 hover:text-gray-600">' +
            '<svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">' +
            '<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path>' +
            '</svg>' +
            '</button>' +
            '</div>' +
            '<div class="space-y-4">' +
            '<input type="hidden" id="auditApplicationId" value="' + applicationId + '">' +
            '<div>' +
            '<label class="text-sm font-medium text-gray-700 mb-2 block">审核结果：</label>' +
            '<div class="flex space-x-4">' +
            '<button id="auditApproveBtn" onclick="selectAuditStatus(1)" class="px-4 py-2 bg-green-500 text-white rounded hover:bg-green-600">通过</button>' +
            '<button id="auditRejectBtn" onclick="selectAuditStatus(2)" class="px-4 py-2 bg-red-500 text-white rounded hover:bg-red-600">拒绝</button>' +
            '</div>' +
            '<input type="hidden" id="auditStatusValue" value="">' +
            '</div>' +
            '<div>' +
            '<label class="text-sm font-medium text-gray-700 mb-2 block">审核人：</label>' +
            '<input type="text" id="auditPersonInput" class="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500" placeholder="请输入审核人姓名">' +
            '</div>' +
            '<div>' +
            '<label class="text-sm font-medium text-gray-700 mb-2 block">审核备注：</label>' +
            '<textarea id="auditRemarkInput" class="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500" rows="4" placeholder="请输入审核备注"></textarea>' +
            '</div>' +
            '</div>' +
            '<div class="mt-6 flex justify-end space-x-3">' +
            '<button onclick="closeApplicationAuditModal()" class="px-4 py-2 bg-gray-300 text-gray-700 rounded hover:bg-gray-400">取消</button>' +
            '<button onclick="submitApplicationAudit()" class="px-4 py-2 bg-blue-500 text-white rounded hover:bg-blue-600">确认</button>' +
            '</div>' +
            '</div>' +
            '</div>' +
            '</div>';

    // 移除已存在的弹框
    $('#applicationAuditModal').remove();

    // 添加弹框到页面
    $('body').append(modalHtml);
  }

  // 选择审核状态
  function selectAuditStatus(status) {
    $('#auditStatusValue').val(status);
    if (status === 1) {
      $('#auditApproveBtn').removeClass('bg-green-500').addClass('bg-green-700');
      $('#auditRejectBtn').removeClass('bg-red-700').addClass('bg-red-500');
    } else {
      $('#auditApproveBtn').removeClass('bg-green-700').addClass('bg-green-500');
      $('#auditRejectBtn').removeClass('bg-red-500').addClass('bg-red-700');
    }
  }

  // 提交审核
  function submitApplicationAudit() {
    var applicationId = $('#auditApplicationId').val();
    var auditStatus = $('#auditStatusValue').val();
    var auditPerson = $('#auditPersonInput').val().trim();
    var auditRemark = $('#auditRemarkInput').val().trim();

    // 校验
    if (!auditStatus) {
      alert('请选择审核结果（通过或拒绝）');
      return;
    }
    if (!auditPerson) {
      alert('请输入审核人');
      return;
    }

    // 构建请求数据
    var requestData = {
      applicationId: applicationId,
      auditStatus: parseInt(auditStatus),
      auditPerson: auditPerson,
      auditRemark: auditRemark
    };

    // 发送AJAX请求
    $.ajax({
      url: '${pageContext.request.contextPath}/application/audit-by-id',
      type: 'POST',
      contentType: 'application/json',
      data: JSON.stringify(requestData),
      success: function(response) {
        if (response && response.success) {
          alert('审核成功');
          closeApplicationAuditModal();
          loadApplications(); // 刷新列表
        } else {
          var msg = (response && (response.message || response.msg)) ? (response.message || response.msg) : '审核失败';
          alert(msg);
        }
      },
      error: function(xhr, status, error) {
        var msg = '请求失败';
        if (xhr && xhr.responseText) {
          try {
            var errorResponse = JSON.parse(xhr.responseText);
            msg = errorResponse.message || errorResponse.msg || msg;
          } catch (e) {
            msg = xhr.responseText || error;
          }
        }
        alert(msg);
      }
    });
  }

  // 关闭晚归申请审核弹框
  function closeApplicationAuditModal() {
    $('#applicationAuditModal').remove();
  }

  // ========== 腾讯地图定位功能 v1版本（已注释，现在只使用container2）==========
  <%--
  var tencentMap = null;
  var currentMarker = null;
  var markers = []; // 存储所有标记的数组

  // 初始化腾讯地图
  function initTencentMap() {
    // 检查腾讯地图API是否加载（支持多种命名空间）
    var maps = null;
    var LatLng = null;
    var Map = null;
    var Marker = null;
    var MarkerAnimation = null;
    var Circle = null;
    var Geocoder = null;
    var GeocoderStatus = null;
    var MapTypeId = null;
    
    // 尝试获取腾讯地图API对象
    var isGLVersion = false;
    
    if (typeof TMap !== 'undefined') {
      // GL版本使用 TMap（优先检测GL版本）
      isGLVersion = true;
      maps = TMap;
      LatLng = TMap.LatLng;
      Map = TMap.Map;
      Marker = TMap.Marker;
      // GL版本可能没有MarkerAnimation，需要检查
      MarkerAnimation = TMap.MarkerAnimation || null;
      Circle = TMap.Circle;
      Geocoder = TMap.Geocoder;
      GeocoderStatus = TMap.GeocoderStatus;
      // GL版本可能没有MapTypeId，使用null
      MapTypeId = TMap.MapTypeId || null;
      console.log('使用GL版腾讯地图API (TMap)');
    } else if (typeof qq !== 'undefined' && typeof qq.maps !== 'undefined') {
      // 标准版本使用 qq.maps
      isGLVersion = false;
      maps = qq.maps;
      LatLng = qq.maps.LatLng;
      Map = qq.maps.Map;
      Marker = qq.maps.Marker;
      MarkerAnimation = qq.maps.MarkerAnimation;
      Circle = qq.maps.Circle;
      Geocoder = qq.maps.Geocoder;
      GeocoderStatus = qq.maps.GeocoderStatus;
      MapTypeId = qq.maps.MapTypeId;
      console.log('使用标准版腾讯地图API (qq.maps)');
    } else {
      console.error('腾讯地图API未加载，请检查Key是否正确');
      document.getElementById('location-info').innerHTML = 
        '<span class="text-red-500">地图加载失败：请检查腾讯地图Key配置和网络连接</span>';
      return;
    }

    // 保存API对象到全局变量，供其他函数使用
    window.tencentMapsAPI = {
      maps: maps,
      LatLng: LatLng,
      Map: Map,
      Marker: Marker,
      MarkerAnimation: MarkerAnimation,
      Circle: Circle,
      Geocoder: Geocoder,
      GeocoderStatus: GeocoderStatus,
      MapTypeId: MapTypeId,
      isGLVersion: isGLVersion
    };

    // 默认中心点（北京天安门，如果定位失败则显示此位置）
    var defaultCenter = new LatLng(39.916527, 116.397128);

    try {
      // 创建地图实例
      var mapOptions = {
        center: defaultCenter,
        zoom: 15
      };
      
      // GL版本和标准版本的参数不同
      if (isGLVersion) {
        // GL版本的参数
        mapOptions.pitch = 0; // 俯仰角
        mapOptions.rotation = 0; // 旋转角度
        // GL版本可能不支持这些参数，先不设置
      } else {
        // 标准版本的参数
        if (MapTypeId && MapTypeId.ROADMAP) {
          mapOptions.mapTypeId = MapTypeId.ROADMAP; // 路网图（导航模式）
        }
        mapOptions.disableDefaultUI = false; // 显示默认控件
        mapOptions.zoomControl = true; // 显示缩放控件
        mapOptions.mapTypeControl = true; // 显示地图类型控件
        mapOptions.scaleControl = true; // 显示比例尺控件
      }
      
      tencentMap = new Map(document.getElementById('map-container'), mapOptions);

      console.log('腾讯地图初始化成功');

      // 显示加载状态
      document.getElementById('location-info').innerHTML = 
        '<span class="text-blue-500">正在获取位置信息...</span>';

      // 开始定位
      getCurrentLocation();
    } catch (error) {
      console.error('腾讯地图初始化失败:', error);
      document.getElementById('location-info').innerHTML = 
        '<span class="text-red-500">地图初始化失败: ' + error.message + '</span>';
    }
  }

  // 获取当前位置（使用浏览器Geolocation API）
  function getCurrentLocation() {
    if (!navigator.geolocation) {
      // 浏览器不支持Geolocation API，使用IP定位
      console.warn('浏览器不支持Geolocation API，尝试使用IP定位');
      getLocationByIP();
      return;
    }

    // 使用浏览器Geolocation API获取位置
    navigator.geolocation.getCurrentPosition(
      function(position) {
        // 定位成功
        var lat = position.coords.latitude;
        var lng = position.coords.longitude;
        var accuracy = position.coords.accuracy || 0;

        console.log('定位成功:', { lat: lat, lng: lng, accuracy: accuracy + '米' });

        // 在地图上显示位置
        showLocationOnMap(lat, lng, accuracy);
        
        // 获取地址信息（逆地理编码）
        getAddressByLocation(lat, lng);
      },
      function(error) {
        // 定位失败，尝试使用IP定位
        console.warn('Geolocation定位失败:', error.message);
        document.getElementById('location-info').innerHTML = 
          '<span class="text-yellow-500">GPS定位失败，尝试使用IP定位...</span>';
        getLocationByIP();
      },
      {
        enableHighAccuracy: true, // 启用高精度定位
        timeout: 10000, // 超时时间10秒
        maximumAge: 60000 // 缓存时间60秒
      }
    );
  }

  // 使用IP定位（通过后端接口调用腾讯地图IP定位服务）
  function getLocationByIP() {
    // 显示加载状态
    document.getElementById('location-info').innerHTML = 
      '<span class="text-blue-500">正在通过IP定位...</span>';

    // 调用后端IP定位接口
    $.ajax({
      url: '${pageContext.request.contextPath}/dorman/location/ip',
      type: 'GET',
      timeout: 10000, // 10秒超时
      success: function(response) {
        if (response && response.success && response.data) {
          var data = response.data;
          var lat = data.lat;
          var lng = data.lng;
          
          if (lat && lng) {
            console.log('IP定位成功: lat=' + lat + ', lng=' + lng);
            showLocationOnMap(lat, lng, 0);
            getAddressByLocation(lat, lng);
          } else {
            // 数据格式错误
            console.warn('IP定位返回数据格式错误:', response);
            fallbackToDefaultLocation('IP定位返回数据格式错误');
          }
        } else {
          // 响应格式错误
          console.warn('IP定位响应格式错误:', response);
          fallbackToDefaultLocation('IP定位服务返回错误');
        }
      },
      error: function(xhr, status, error) {
        console.error('IP定位请求失败:', {
          status: status,
          error: error,
          responseText: xhr.responseText
        });
        fallbackToDefaultLocation('IP定位服务不可用，请检查网络连接');
      }
    });
  }

  // 回退到默认位置
  function fallbackToDefaultLocation(message) {
    var defaultLat = 39.916527; // 北京天安门
    var defaultLng = 116.397128;
    showLocationOnMap(defaultLat, defaultLng, 0);
    var msg = message || '无法获取当前位置';
    document.getElementById('location-info').innerHTML = 
      '<span class="text-yellow-500">' + msg + '，显示默认位置（北京天安门）</span>';
  }

  // 在地图上显示位置
  function showLocationOnMap(lat, lng, accuracy) {
    if (!tencentMap || !window.tencentMapsAPI) {
      console.error('地图未初始化或API对象不存在');
      return;
    }

    var api = window.tencentMapsAPI;
    var position = new api.LatLng(lat, lng);

    // 清除之前的标记
    if (currentMarker) {
      currentMarker.setMap(null);
    }

    // 创建标记
    try {
      var markerOptions = {
        position: position,
        map: tencentMap,
        title: '当前位置'
      };
      
      // GL版本和标准版本的Marker创建方式可能不同
      if (api.isGLVersion) {
        // GL版本可能需要不同的参数
        // 尝试创建Marker
        if (typeof api.Marker === 'function') {
          currentMarker = new api.Marker(markerOptions);
        } else if (api.maps && api.maps.Marker) {
          // 如果Marker在maps下
          currentMarker = new api.maps.Marker(markerOptions);
        } else {
          console.warn('GL版本Marker创建方式未知，尝试使用TMap.Marker');
          currentMarker = new TMap.Marker(markerOptions);
        }
      } else {
        // 标准版本
        // 如果支持动画，添加动画效果
        if (api.MarkerAnimation && api.MarkerAnimation.DROP) {
          markerOptions.animation = api.MarkerAnimation.DROP; // 标记下落动画
        }
        currentMarker = new api.Marker(markerOptions);
      }
    } catch (error) {
      console.error('创建Marker失败:', error);
      console.log('尝试使用备用方式创建Marker');
      // 备用方式：直接使用TMap或qq.maps
      if (typeof TMap !== 'undefined' && TMap.Marker) {
        currentMarker = new TMap.Marker({
          position: position,
          map: tencentMap,
          title: '当前位置'
        });
      } else if (typeof qq !== 'undefined' && qq.maps && qq.maps.Marker) {
        currentMarker = new qq.maps.Marker({
          position: position,
          map: tencentMap,
          title: '当前位置'
        });
      } else {
        console.error('无法创建Marker，API对象不存在');
        return;
      }
    }

    // 如果有精度信息，显示精度圆圈
    if (accuracy > 0) {
      try {
        var circleOptions = {
          center: position,
          radius: accuracy, // 精度半径（米）
          fillColor: '#4285F4',
          fillOpacity: 0.2,
          strokeColor: '#4285F4',
          strokeOpacity: 0.8,
          strokeWeight: 2,
          map: tencentMap
        };
        
        // GL版本和标准版本的Circle创建方式可能不同
        if (api.isGLVersion && typeof TMap !== 'undefined' && TMap.Circle) {
          var circle = new TMap.Circle(circleOptions);
          markers.push(circle);
        } else if (typeof api.Circle === 'function') {
          var circle = new api.Circle(circleOptions);
          markers.push(circle);
        } else {
          console.warn('Circle创建方式未知，跳过精度圆圈显示');
        }
      } catch (error) {
        console.warn('创建精度圆圈失败:', error);
      }
    }

    markers.push(currentMarker);

    // 设置地图中心点和缩放级别
    tencentMap.setCenter(position);
    tencentMap.setZoom(16);
  }

  // 根据坐标获取地址信息（逆地理编码）
  function getAddressByLocation(lat, lng) {
    if (!window.tencentMapsAPI) {
      console.warn('腾讯地图API对象不存在，无法获取地址信息');
      document.getElementById('location-info').innerHTML = 
        '<span class="text-gray-500">坐标：' + lat.toFixed(6) + ', ' + lng.toFixed(6) + '</span>';
      return;
    }

    var api = window.tencentMapsAPI;
    var geocoder = new api.Geocoder();
    var latLng = new api.LatLng(lat, lng);

    geocoder.getAddress(latLng, function(status, result) {
      if (status === api.GeocoderStatus.OK) {
        var address = result.detail.address || result.detail.formatted_addresses?.recommend || '未知地址';
        var locationInfo = 
          '<div class="text-sm">' +
            '<p class="font-medium text-gray-800">当前位置：</p>' +
            '<p class="text-gray-600">' + address + '</p>' +
            '<p class="text-xs text-gray-500 mt-1">坐标：' + lat.toFixed(6) + ', ' + lng.toFixed(6) + '</p>' +
          '</div>';
        document.getElementById('location-info').innerHTML = locationInfo;
      } else {
        console.warn('逆地理编码失败，状态码:', status);
        document.getElementById('location-info').innerHTML = 
          '<span class="text-gray-500">坐标：' + lat.toFixed(6) + ', ' + lng.toFixed(6) + '</span>';
      }
    });
  }

  // 清除所有标记
  function clearAllMarkers() {
    if (markers && markers.length > 0) {
      markers.forEach(function(marker) {
        if (marker.setMap) {
          marker.setMap(null);
        }
      });
      markers = [];
      currentMarker = null;
    }
  }

  // 将函数暴露到全局作用域
    window.clearAllMarkers = clearAllMarkers;

  // 检查腾讯地图API是否加载
  function isTencentMapAPILoaded() {
    // 检查脚本是否加载失败
    if (window.tencentMapScriptError) {
      return false;
    }
    
    // 腾讯地图GL版本可能使用不同的命名空间
    // 方式1：标准版本 qq.maps
    if (typeof qq !== 'undefined' && typeof qq.maps !== 'undefined') {
      return true;
    }
    // 方式2：GL版本可能使用 TMap
    if (typeof TMap !== 'undefined') {
      return true;
    }
    // 方式3：检查 window.qq
    if (window.qq && window.qq.maps) {
      return true;
    }
    return false;
  }

  // 页面加载完成后初始化地图（v1版本已注释，现在只使用container2）
  <%--$(document).ready(function() {--%>
  <%--  // 显示加载状态--%>
  <%--  document.getElementById('location-info').innerHTML = --%>
  <%--    '<span class="text-blue-500">正在加载地图API...</span>';--%>
  <%----%>
  <%--  // 立即检测一次--%>
  <%--  if (isTencentMapAPILoaded()) {--%>
  <%--    console.log('腾讯地图API已加载');--%>
  <%--    initTencentMap();--%>
  <%--    return;--%>
  <%--  }--%>
  <%----%>
  <%--  // 如果API还未加载，等待一段时间后重试--%>
  <%--  var retryCount = 0;--%>
  <%--  var maxRetries = 100; // 最多重试100次（10秒）--%>
  <%--  --%>
  <%--  var checkInterval = setInterval(function() {--%>
  <%--    retryCount++;--%>
  <%--    --%>
  <%--    if (isTencentMapAPILoaded()) {--%>
  <%--      clearInterval(checkInterval);--%>
  <%--      console.log('腾讯地图API加载成功，重试次数:', retryCount);--%>
  <%--      initTencentMap();--%>
  <%--    } else if (retryCount >= maxRetries) {--%>
  <%--      clearInterval(checkInterval);--%>
  <%--      --%>
  <%--      // 输出详细的调试信息--%>
  <%--      console.error('腾讯地图API加载失败');--%>
  <%--      console.log('调试信息:', {--%>
  <%--        'typeof qq': typeof qq,--%>
  <%--        'typeof qq.maps': typeof qq !== 'undefined' ? typeof qq.maps : 'qq未定义',--%>
  <%--        'typeof TMap': typeof TMap,--%>
  <%--        'window.qq': window.qq,--%>
  <%--        '重试次数': retryCount--%>
  <%--      });--%>
  <%--      --%>
  <%--      // 显示详细的错误信息--%>
  <%--      var errorMsg = '<div class="text-red-500">' +--%>
  <%--        '<p class="font-medium">地图API加载超时</p>' +--%>
  <%--        '<p class="text-sm mt-2">可能的原因：</p>' +--%>
  <%--        '<ul class="list-disc list-inside text-sm mt-1 space-y-1">' +--%>
  <%--        '<li>网络连接问题，无法访问腾讯地图API</li>' +--%>
  <%--        '<li>Key配置错误或未绑定当前域名</li>' +--%>
  <%--        '<li>API脚本加载失败（请按F12查看控制台错误）</li>' +--%>
  <%--        '</ul>' +--%>
  <%--        '<p class="text-xs mt-2 text-gray-500">提示：请检查浏览器控制台的Network标签，查看API脚本是否成功加载</p>' +--%>
  <%--        '</div>';--%>
  <%--      document.getElementById('location-info').innerHTML = errorMsg;--%>
  <%--    }--%>
  <%--  }, 100);--%>
  // });
  // --%>
  // ========== 腾讯地图定位功能v1版本结束 ==========

  // ========== 腾讯地图定位功能 v2版本==========
  function initMap2() {
    // 显示加载状态
    document.getElementById('map2-location-info').innerHTML = 
      '<span class="text-blue-500">正在获取位置信息...</span>';

    // 通过Ajax向后端获取IP定位的经纬度
      $.ajax({
      url: '${pageContext.request.contextPath}/dorman/location/ip',
        type: 'GET',
      timeout: 10000, // 10秒超时
      success: function(response) {
        if (response && response.success && response.data) {
          var data = response.data;
          var lat = data.lat;
          var lng = data.lng;
          
          if (lat && lng) {
            console.log('IP定位成功: lat=' + lat + ', lng=' + lng);
            
            // 使用获取到的经纬度创建地图中心点
            var center = new TMap.LatLng(lat, lng);
            
            // 定义map变量，调用 TMap.Map() 构造函数创建地图
            var map = new TMap.Map(document.getElementById('container2'), {
              center: center, // 使用IP定位获取的坐标
              zoom: 15,       // 设置地图缩放级别
              pitch: 0,       // 设置俯仰角（0度，正常视角）
              rotation: 0     // 设置地图旋转角度（0度，正北方向）
            });
            
            // 显示位置信息
            document.getElementById('map2-location-info').innerHTML = 
              '<span class="text-gray-600">坐标：' + lat.toFixed(6) + ', ' + lng.toFixed(6) + '</span>';
          } else {
            // 数据格式错误，使用默认位置
            initMap2WithDefaultLocation('IP定位返回数据格式错误');
          }
        } else {
          // 响应格式错误，使用默认位置
          initMap2WithDefaultLocation('IP定位服务返回错误');
        }
      },
      error: function(xhr, status, error) {
        console.error('IP定位请求失败:', {
          status: status,
          error: error,
          responseText: xhr.responseText
        });
        // IP定位失败，使用默认位置
        initMap2WithDefaultLocation('IP定位服务不可用，显示默认位置');
      }
    });
  }

  // 使用默认位置初始化地图
  function initMap2WithDefaultLocation(message) {
    // 默认中心点坐标（北京天安门）
    var center = new TMap.LatLng(39.916527, 116.397128);
    
    // 定义map变量，调用 TMap.Map() 构造函数创建地图
    var map = new TMap.Map(document.getElementById('container2'), {
      center: center, // 使用默认坐标
      zoom: 15,       // 设置地图缩放级别
      pitch: 0,       // 设置俯仰角
      rotation: 0     // 设置地图旋转角度
    });
    
    // 显示提示信息
    if (message) {
      document.getElementById('map2-location-info').innerHTML = 
        '<span class="text-yellow-500">' + message + '（北京天安门）</span>';
    }
  }

  // ========== 获取报警学生信息 ==========  
  function loadAlarmContacts() {
       // 获取helperNo和role
    // var role = '${sessionScope.user["class"].simpleName}';
    // var helperNo = '';
    // if (role === 'DormitoryManager') {
    //   helperNo = '${managerId}';
    // } else if (role === 'SysUser') {
    //   helperNo = '${targetId}';
    // }

    var userRole = '${sessionScope.user["class"].simpleName}';
    var helperNo = '';
    if (userRole === 'DormitoryManager') {
      userRole = 'dormitorymanager';
      helperNo = '${managerId}';
    } else if (userRole === 'SysUser') {
      userRole = 'systemuser';
      helperNo = '${targetId}';
    }


    if (!helperNo || !userRole) {
      console.error('无法获取当前用户工号或角色');
      return;
    }

    $.ajax({
      url: '${pageContext.request.contextPath}/alarm/stu-alarm-contact',
      type: 'GET',
      data: {
        helperNo: helperNo,
        role: userRole
      },
      success: function(res) {
        if (res && res.success && Array.isArray(res.data)) {
          // res.data 是报警学生的List<StudentAlarmContactsVO>
          console.log('报警学生信息:', res.data);
          // 渲染报警学生信息到页面
          renderAlarmStudents(res.data);
        } else {
          console.error('获取报警学生信息失败', res);
        }
      },
      error: function(xhr, status, error) {
        console.error('请求报警学生信息出错', error);
      }
    });
  }

  // 渲染报警学生信息
  function renderAlarmStudents(alarmStudents) {
    const container = $('#alarmStudentsList');
    container.empty();

    if (alarmStudents.length === 0) {
      container.html(
        '<div class="text-center py-8">' +
          '<div class="inline-flex items-center justify-center w-16 h-16 rounded-full bg-green-100 text-green-600 mb-4">' +
            '<i class="fas fa-check-circle text-2xl"></i>' +
          '</div>' +
          '<h3 class="text-lg font-medium text-gray-900 mb-2">暂无报警信息</h3>' +
          '<p class="text-gray-500">当前没有需要处理的报警学生</p>' +
        '</div>'
      );
      return;
    }

    alarmStudents.forEach(function(student, index) {
      var studentHtml = 
        '<div class="border border-gray-200 rounded-lg p-6 bg-white shadow-md hover:shadow-lg transition-all duration-300 transform hover:-translate-y-1" style="animation: slideInUp 0.5s ease-out ' + (index * 0.1) + 's both;">' +
          '<div class="flex justify-between items-start mb-4">' +
            '<div class="flex items-center">' +
              '<div class="p-3 rounded-full bg-red-100 text-red-600 mr-4">' +
                '<i class="fas fa-user-graduate text-xl"></i>' +
              '</div>' +
              '<div>' +
                '<h3 class="text-xl font-semibold text-gray-900">' + (student.studentName || '未知') + '</h3>' +
                '<p class="text-sm text-gray-600 mt-1">学号：' + (student.studentNo || '未知') + '</p>' +
                '<p class="text-sm text-gray-600">报警编号：' + (student.alarmNo || '未知') + '</p>' +
              '</div>' +
            '</div>' +
            '<span class="inline-flex items-center px-3 py-1 rounded-full text-sm font-medium bg-red-100 text-red-800 animate-pulse">' +
              '<i class="fas fa-exclamation-circle mr-1"></i>' +
              '报警中' +
            '</span>' +
          '</div>' +
          '<div class="flex justify-between items-center mb-4">' +
            '<div class="flex space-x-2">' +
              '<button onclick="showStudentLocation(\'' + (student.alarmNo || '') + '\')" class="bg-blue-500 hover:bg-blue-600 text-white text-xs px-3 py-2 rounded-md transition-colors duration-200 flex items-center">' +
                '<i class="fas fa-map-marker-alt mr-1"></i>' +
                '查看位置' +
              '</button>' +
              '<button onclick="clearAllMarkers()" class="bg-gray-500 hover:bg-gray-600 text-white text-xs px-3 py-2 rounded-md transition-colors duration-200 flex items-center">' +
                '<i class="fas fa-times mr-1"></i>' +
                '清除标记' +
              '</button>' +
            '</div>' +
          '</div>' +
          '<div class="grid grid-cols-1 md:grid-cols-2 gap-4">' +
            '<div class="bg-gradient-to-r from-blue-50 to-blue-100 p-4 rounded-lg border border-blue-200">' +
              '<div class="flex items-center justify-between mb-2">' +
                '<h4 class="text-sm font-medium text-gray-700 flex items-center">' +
                  '<i class="fas fa-male text-blue-600 mr-2"></i>' +
                  '父亲联系方式' +
                '</h4>' +
              '</div>' +
              '<p class="text-sm text-gray-600 mb-3">' + (student.fatherPhone || '暂无') + '</p>' +
              '<div class="flex space-x-2">' +
                '<button onclick="copyToClipboard(\'' + (student.fatherPhone || '') + '\')" class="flex-1 bg-blue-500 hover:bg-blue-600 text-white text-xs px-3 py-2 rounded-md transition-colors duration-200 flex items-center justify-center">' +
                  '<i class="fas fa-copy mr-1"></i>' +
                  '复制' +
                '</button>' +
                '<button onclick="makePhoneCall(\'' + (student.fatherPhone || '') + '\')" class="flex-1 bg-green-500 hover:bg-green-600 text-white text-xs px-3 py-2 rounded-md transition-colors duration-200 flex items-center justify-center">' +
                  '<i class="fas fa-phone mr-1"></i>' +
                  '拨打' +
                '</button>' +
              '</div>' +
            '</div>' +
            '<div class="bg-gradient-to-r from-pink-50 to-pink-100 p-4 rounded-lg border border-pink-200">' +
              '<div class="flex items-center justify-between mb-2">' +
                '<h4 class="text-sm font-medium text-gray-700 flex items-center">' +
                  '<i class="fas fa-female text-pink-600 mr-2"></i>' +
                  '母亲联系方式' +
                '</h4>' +
              '</div>' +
              '<p class="text-sm text-gray-600 mb-3">' + (student.motherPhone || '暂无') + '</p>' +
              '<div class="flex space-x-2">' +
                '<button onclick="copyToClipboard(\'' + (student.motherPhone || '') + '\')" class="flex-1 bg-pink-500 hover:bg-pink-600 text-white text-xs px-3 py-2 rounded-md transition-colors duration-200 flex items-center justify-center">' +
                  '<i class="fas fa-copy mr-1"></i>' +
                  '复制' +
                '</button>' +
                '<button onclick="makePhoneCall(\'' + (student.motherPhone || '') + '\')" class="flex-1 bg-green-500 hover:bg-green-600 text-white text-xs px-3 py-2 rounded-md transition-colors duration-200 flex items-center justify-center">' +
                  '<i class="fas fa-phone mr-1"></i>' +
                  '拨打' +
                '</button>' +
              '</div>' +
            '</div>' +
          '</div>' +
        '</div>';
      container.append(studentHtml);
    });
  }

  // 复制到剪贴板功能
  function copyToClipboard(text) {
    if (!text || text === '暂无') {
      showToast('没有可复制的电话号码', 'warning');
      return;
    }
    
    if (navigator.clipboard && window.isSecureContext) {
      navigator.clipboard.writeText(text).then(function() {
        showToast('电话号码已复制到剪贴板', 'success');
      }).catch(function() {
        fallbackCopyTextToClipboard(text);
      });
    } else {
      fallbackCopyTextToClipboard(text);
    }
  }

  // 备用复制方法
  function fallbackCopyTextToClipboard(text) {
    var textArea = document.createElement("textarea");
    textArea.value = text;
    textArea.style.top = "0";
    textArea.style.left = "0";
    textArea.style.position = "fixed";
    document.body.appendChild(textArea);
    textArea.focus();
    textArea.select();
    
    try {
      var successful = document.execCommand('copy');
      if (successful) {
        showToast('电话号码已复制到剪贴板', 'success');
      } else {
        showToast('复制失败，请手动复制', 'error');
      }
    } catch (err) {
      showToast('复制失败，请手动复制', 'error');
    }
    
    document.body.removeChild(textArea);
  }

  // 拨打电话功能
  function makePhoneCall(phoneNumber) {
    if (!phoneNumber || phoneNumber === '暂无') {
      showToast('没有可拨打的电话号码', 'warning');
      return;
    }
    
    if (confirm('是否要拨打 ' + phoneNumber + '？')) {
      window.location.href = 'tel:' + phoneNumber;
    }
  }

  // 显示提示信息
  function showToast(message, type) {
    // 创建toast元素
    var toast = $('<div class="fixed top-4 right-4 z-50 px-6 py-3 rounded-lg shadow-lg text-white transform transition-all duration-300 translate-x-full">' + message + '</div>');
    
    // 根据类型设置颜色
    var bgColor = 'bg-gray-800';
    if (type === 'success') bgColor = 'bg-green-500';
    else if (type === 'error') bgColor = 'bg-red-500';
    else if (type === 'warning') bgColor = 'bg-yellow-500';
    
    toast.addClass(bgColor);
    
    // 添加到页面
    $('body').append(toast);
    
    // 显示动画
    setTimeout(function() {
      toast.removeClass('translate-x-full');
    }, 100);
    
    // 3秒后隐藏
    setTimeout(function() {
      toast.addClass('translate-x-full');
      setTimeout(function() {
        toast.remove();
      }, 300);
    }, 3000);
  }

  // 页面初始化函数
  function initializePage() {
    // 加载报警学生信息
    loadAlarmContacts();
    
    // 加载统计数据
    loadStatistics();
    
    // 加载通知列表
    loadNotifications();
    
    // 加载晚归记录
    loadLateReturns();
    
    // 加载晚归申请
    loadApplications();
  }
</script>



<!-- 非Ajax形式请求的错误弹框提示 -->
<c:if test="${not empty sessionScope.errorMsg}">
  <script>
    alert("${fn:escapeXml(sessionScope.errorMsg)}");
  </script>
  <c:remove var="errorMsg" scope="session" />
</c:if>
</body>
</html>