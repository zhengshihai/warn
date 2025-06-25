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

  <script src="https://webapi.amap.com/maps?v=2.0&key=c34c1fdbcbe4d043906c95993710fbcc"></script>
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
  </style>
</head>
<body>
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


  <!-- 宿管个人信息修改模态框 --> <!-- todo 有bug-->
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

  <!-- 系统用户个人信息修改模态框 --> <!-- todo 有bug-->
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

  <!-- 1. 地图容器 -->
  <div class="card p-6 mb-6">
    <div class="flex items-center mb-4">
      <h2 class="text-lg font-medium text-gray-900">
        <c:choose>
          <c:when test="${sessionScope.user['class'].simpleName eq 'DormitoryManager'}">宿管位置信息</c:when>
          <c:when test="${sessionScope.user['class'].simpleName eq 'SysUser'}">
            <c:choose>
              <c:when test="${not empty jobRole and fn:toLowerCase(jobRole) eq 'counselor'}">辅导员位置信息</c:when>
              <c:when test="${not empty jobRole and fn:toLowerCase(jobRole) eq 'class_teacher'}">班主任位置信息</c:when>
              <c:when test="${not empty jobRole and fn:toLowerCase(jobRole) eq 'dean'}">院级领导位置信息</c:when>
              <c:otherwise>系统用户位置信息</c:otherwise>
            </c:choose>
          </c:when>
        </c:choose>
      </h2>
    </div>
    <div id="map-container" style="width:100%;height:300px;"></div>
    <div id="location-info" class="mt-4 text-sm text-gray-600"></div>
  </div>

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

  <!-- 主要内容区域 -->
  <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
    <!-- 待处理晚归记录 -->
    <div class="card p-6">
      <div class="flex justify-between items-center mb-4">
        <h2 class="text-lg font-medium text-gray-900">待处理晚归记录</h2>
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

    <!-- 最近通知 -->
    <div class="card p-6">
      <div class="flex justify-between items-center mb-4">
        <h2 class="text-lg font-medium text-gray-900">最近通知</h2>
        <div class="flex items-center space-x-4">
          <select id="pageSizeSelect" class="form-select form-select-sm" onchange="changePageSize()">
            <option value="5">5条/页</option>
            <option value="10">10条/页</option>
            <option value="20">20条/页</option>
          </select>
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

<!-- 2. 高德地图API -->
<script src="https://webapi.amap.com/maps?v=2.0&key=c34c1fdbcbe4d043906c95993710fbcc"></script>

<script>
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

  // 全局变量存储分页信息
  var currentPage = 1;
  var pageSize = 5;
  var totalPages = 1;

  function loadNotifications(page) {
    if (page) {
      currentPage = page;
    }

    var query = {
      pageNum: currentPage,
      pageSize: pageSize,
      status: 'UNREAD',
      targetId: '${targetId}'  // 使用根据角色设置的targetId
    };

    $.ajax({
      url: '${pageContext.request.contextPath}/notification/unread-page-list',
      type: 'POST',
      contentType: 'application/json',
      data: JSON.stringify(query),
      success: function(response) {
        if (response.success) {
          var notifications = response.data.data;
          var total = response.data.total;
          totalPages = Math.ceil(total / pageSize);

          updateNotificationList(notifications);
          updatePagination(currentPage, totalPages);
          updateTotalCount(total);
        } else {
          showError('获取通知失败: ' + response.message);
        }
      },
      error: function(xhr, status, error) {
        showError('获取通知失败，请稍后重试');
      }
    });
  }

  function updateTotalCount(total) {
    var totalText = '共 ' + total + ' 条通知';
    if (!$('#totalCount').length) {
      $('#notificationPagination').prepend('<span id="totalCount" class="text-sm text-gray-600 mr-4"></span>');
    }
    $('#totalCount').text(totalText);
  }

  function changePageSize() {
    pageSize = parseInt($('#pageSizeSelect').val());
    currentPage = 1;
    loadNotifications();
  }

  function updatePagination(currentPage, totalPages) {
    var container = $('#notificationPagination');
    var totalCount = $('#totalCount').detach();
    container.empty();
    container.prepend(totalCount);

    // 只有在没有数据时才不显示分页
    if (totalPages === 0) {
      return;
    }

    // 始终显示上一页按钮（除非是第一页）
    if (currentPage > 1) {
      container.append(
              '<button onclick="loadNotifications(' + (currentPage - 1) + ')" ' +
              'class="px-3 py-1 rounded bg-gray-200 hover:bg-gray-300">' +
              '上一页' +
              '</button>'
      );
    }

    var startPage = Math.max(1, currentPage - 2);
    var endPage = Math.min(totalPages, startPage + 4);
    if (endPage - startPage < 4) {
      startPage = Math.max(1, endPage - 4);
    }

    if (startPage > 1) {
      container.append(
              '<button onclick="loadNotifications(1)" ' +
              'class="px-3 py-1 rounded bg-gray-200 hover:bg-gray-300">' +
              '1' +
              '</button>'
      );
      if (startPage > 2) {
        container.append('<span class="px-2">...</span>');
      }
    }

    for (var i = startPage; i <= endPage; i++) {
      if (i === currentPage) {
        container.append(
                '<button class="px-3 py-1 rounded bg-blue-500 text-white">' +
                i +
                '</button>'
        );
      } else {
        container.append(
                '<button onclick="loadNotifications(' + i + ')" ' +
                'class="px-3 py-1 rounded bg-gray-200 hover:bg-gray-300">' +
                i +
                '</button>'
        );
      }
    }

    if (endPage < totalPages) {
      if (endPage < totalPages - 1) {
        container.append('<span class="px-2">...</span>');
      }
      container.append(
              '<button onclick="loadNotifications(' + totalPages + ')" ' +
              'class="px-3 py-1 rounded bg-gray-200 hover:bg-gray-300">' +
              totalPages +
              '</button>'
      );
    }

    // 始终显示下一页按钮（除非是最后一页）
    if (currentPage < totalPages) {
      container.append(
              '<button onclick="loadNotifications(' + (currentPage + 1) + ')" ' +
              'class="px-3 py-1 rounded bg-gray-200 hover:bg-gray-300">' +
              '下一页' +
              '</button>'
      );
    }
  }

  /**
   * 更新通知列表
   * @param {Array} notifications 通知数据数组
   */
  function updateNotificationList(notifications) {
    const container = $('#notificationList');
    container.empty();

    if (notifications.length === 0) {
      container.html('<p class="text-gray-500 text-center">暂无通知</p>');
      return;
    }

    notifications.forEach(function(notification) {
      var notificationHtml =
              '<div class="border-l-4 border-' + getStatusColor(notification.status) + ' pl-4 py-2">' +
              '<h3 class="text-sm font-medium text-gray-900">' + notification.title + '</h3>' +
              '<p class="text-sm text-gray-600 mt-1">' + notification.content + '</p>' +
              '<p class="text-xs text-gray-500 mt-1">' + formatDate(notification.noticeTime) + '</p>' +
              '</div>';
      container.append(notificationHtml);
    });
  }

  /**
   * 获取通知状态对应的颜色
   * @param {string} status 通知状态
   * @returns {string} 颜色类名
   */
  function getStatusColor(status) {
    switch (status) {
      case 'UNREAD':
        return 'blue-500';
      case 'READ':
        return 'green-500';
      default:
        return 'gray-500';
    }
  }

  /**
   * 格式化日期
   * @param {string} dateStr 日期字符串 (MySQL datetime格式: YYYY-MM-DD HH:mm:ss)
   * @returns {string} 格式化后的日期字符串
   */
  function formatDate(dateStr) {
    if (!dateStr) {
      return '';
    }

    // MySQL datetime格式直接使用
    var date = new Date(dateStr);

    if (isNaN(date.getTime())) {
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
    const date = new Date(dateStr);
    return date.toLocaleString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  // 1. 显示班级管理员自己的位置
  AMap.plugin('AMap.Geolocation', function() {
    var map = new AMap.Map('map-container', { resizeEnable: true, zoom: 16 });
    var geolocation = new AMap.Geolocation({
      enableHighAccuracy: true,
      timeout: 10000,
      buttonPosition: 'RB',
      zoomToAccuracy: true,
      needAddress: true
    });
    map.addControl(geolocation);
    
    // 存储标记的数组，避免重复创建
    var markers = [];
    
    // 清除所有标记的函数
    function clearAllMarkers() {
      markers.forEach(function(marker) {
        map.remove(marker);
      });
      markers = [];
    }
    
    // 获取用户角色和显示名称
    var userRole = '${sessionScope.user["class"].simpleName}';
    var roleDisplayName = '';
    if (userRole === 'DormitoryManager') {
      roleDisplayName = '宿管';
    } else if (userRole === 'SysUser') {
      var jobRole = '${jobRole}';
      if (jobRole === 'counselor') {
        roleDisplayName = '辅导员';
      } else if (jobRole === 'class_teacher') {
        roleDisplayName = '班主任';
      } else if (jobRole === 'dean') {
        roleDisplayName = '院级领导';
      } else {
        roleDisplayName = '系统用户';
      }
    }
    
    geolocation.getCurrentPosition();

    geolocation.on('complete', function(data) {
      var lng = data.position.lng, lat = data.position.lat;
      var marker = new AMap.Marker({
        position: [lng, lat],
        icon: 'https://webapi.amap.com/theme/v1.3/markers/n/mark_b.png', // 蓝色
        title: roleDisplayName + '当前位置'
      });
      map.add(marker);
      markers.push(marker); // 添加到标记数组
      map.setCenter([lng, lat]);
    });

    geolocation.on('error', function(err) {
      document.getElementById('location-info').innerHTML = '定位失败: ' + err.message;
    });

    // 2. 查询学生报警位置
    function showStudentLocation(alarmNo) {
      // 清除之前的学生标记
      markers.forEach(function(marker, index) {
        if (marker.getTitle() && marker.getTitle().includes('学生报警位置')) {
          map.remove(marker);
          markers.splice(index, 1);
        }
      });
      
      $.ajax({
        url: '${pageContext.request.contextPath}/alarm/location?alarmNo=' + alarmNo,
        type: 'GET',
        success: function(res) {
          if (res && res.longitude && res.latitude) {
            var marker = new AMap.Marker({
              position: [res.longitude, res.latitude],
              icon: 'https://webapi.amap.com/theme/v1.3/markers/n/mark_r.png', // 红色
              title: '学生报警位置'
            });
            map.add(marker);
            markers.push(marker); // 添加到标记数组
          }
        }
      });
    }

    // 将showStudentLocation函数暴露到全局作用域，以便其他函数调用
    window.showStudentLocation = showStudentLocation;
    window.clearAllMarkers = clearAllMarkers;

    // TODO: 通过下拉框/按钮选择报警编号，调用 showStudentLocation(alarmNo)
  });

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