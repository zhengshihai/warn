<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<!DOCTYPE html>
<html>
<head>
    <title>学生晚归预警系统 - 超级管理员</title>

    <!-- 引入 CSS 文件 -->
    <link href="${pageContext.request.contextPath}/static/css/tailwind.min.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/static/css/fontawesome.min.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/static/css/bootstrap.min.css" rel="stylesheet">

    <!-- 引入 JS 文件 -->
    <script src="${pageContext.request.contextPath}/static/js/jquery.min.js"></script>
    <script src="${pageContext.request.contextPath}/static/js/popper.min.js"></script>
    <script src="${pageContext.request.contextPath}/static/js/bootstrap.bundle.min.js"></script>
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
        .nav-tabs .nav-link {
            color: #4b5563;
        }
        .nav-tabs .nav-link.active {
            color: #3b82f6;
            border-color: #3b82f6;
        }
        /* 新增：表格字体缩小，横向滚动 */
        .table-responsive {
            overflow-x: auto;
        }
        .table {
            font-size: 14px;
            white-space: nowrap;
        }
        .table th, .table td {
            vertical-align: middle;
            text-align: center;
        }
        .table thead th {
            background-color: #f8fafc;
            font-weight: 600;
        }
    </style>
</head>
<body>
    <div class="dashboard-container p-6">
        <!-- 顶部导航栏 -->
        <nav class="bg-white shadow-sm rounded-lg mb-6 p-4">
            <div class="flex justify-between items-center">
                <h1 class="text-xl font-semibold text-gray-800">学生晚归预警系统 - 超级管理员</h1>
                <div class="flex items-center space-x-4">
                    <span class="text-gray-600">欢迎，${sessionScope.user.name}（超级管理员）</span>
                    <button class="text-sm text-blue-600 hover:text-blue-800" data-bs-toggle="modal" data-bs-target="#editProfileModal">修改个人信息</button>
                    <button class="text-sm text-red-600 hover:text-red-800" onclick="handleLogout()" >退出登录</button>
                </div>
            </div>
        </nav>

        <!-- 学生列表内容区域 -->
        <div class="card p-6 mb-6">
            <div class="mb-4 flex justify-between items-center">
                <h3 class="text-lg font-medium text-gray-900">学生列表</h3>
                <div>
                    <input type="text" id="studentSearch" class="form-control d-inline-block w-auto" placeholder="搜索姓名...">
                    <button class="btn btn-primary ml-2" id="searchStudentBtn">搜索</button>
                </div>
            </div>
            <div class="table-responsive">
                <table class="table table-hover table-striped table-bordered align-middle">
                    <thead class="table-light">
                        <tr>
                            <th>ID</th>
                            <th>学号</th>
                            <th>姓名</th>
                            <th>学院</th>
                            <th>班级</th>
                            <th>宿舍</th>
                            <th>邮箱</th>
                            <th>电话</th>
                            <th>父亲姓名</th>
                            <th>父亲电话</th>
                            <th>母亲姓名</th>
                            <th>母亲电话</th>
                            <th>创建时间</th>
                            <th>更新时间</th>
                            <th>最后登录时间</th>
                            <th style="min-width:120px;">操作</th>
                        </tr>
                    </thead>
                    <tbody id="studentTableBody">
                        <!-- 学生数据将通过JS动态加载 -->
                    </tbody>
                </table>
            </div>
            <div class="d-flex justify-content-between align-items-center mt-4">
                <div class="text-sm text-gray-500">
                    显示 <span id="studentStartRecord">1</span> 到 <span id="studentEndRecord">10</span> 条，共 <span id="studentTotalRecords">0</span> 条记录
                </div>
                <nav>
                    <ul class="pagination" id="studentPagination">
                        <!-- 分页按钮JS生成 -->
                    </ul>
                </nav>
            </div>
        </div>

        <!-- 班级管理员列表内容区域 -->
        <div class="card p-6 mb-6">
            <div class="mb-4 flex justify-between items-center">
                <h3 class="text-lg font-medium text-gray-900">班级管理员列表</h3>
                <div>
                    <input type="text" id="sysUserSearch" class="form-control d-inline-block w-auto" placeholder="搜索姓名...">
                    <button class="btn btn-primary ml-2" id="searchSysUserBtn">搜索</button>
                </div>
            </div>
            <div class="table-responsive">
                <table class="table table-hover table-striped table-bordered align-middle">
                    <thead class="table-light">
                        <tr>
                            <th>ID</th>
                            <th>工号</th>
                            <th>姓名</th>
                            <th>电话</th>
                            <th>邮箱</th>
                            <th>职位角色</th>
                            <th>状态</th>
                            <th>最后登录时间</th>
                            <th>创建时间</th>
                            <th>更新时间</th>
                            <th style="min-width:160px;">操作</th>
                        </tr>
                    </thead>
                    <tbody id="sysUserTableBody">
                        <!-- 班级管理员数据将通过JS动态加载 -->
                    </tbody>
                </table>
            </div>
            <div class="d-flex justify-content-between align-items-center mt-4">
                <div class="text-sm text-gray-500">
                    显示 <span id="sysUserStartRecord">1</span> 到 <span id="sysUserEndRecord">10</span> 条，共 <span id="sysUserTotalRecords">0</span> 条记录
                </div>
                <nav>
                    <ul class="pagination" id="sysUserPagination">
                        <!-- 分页按钮JS生成 -->
                    </ul>
                </nav>
            </div>
        </div>

        <!-- 超级管理员列表内容区域 -->
        <div class="card p-6 mb-6">
            <div class="mb-4 flex justify-between items-center">
                <h3 class="text-lg font-medium text-gray-900">超级管理员列表</h3>
                <div>
                    <input type="text" id="superAdminSearch" class="form-control d-inline-block w-auto" placeholder="搜索姓名...">
                    <button class="btn btn-primary ml-2" id="searchSuperAdminBtn">搜索</button>
                </div>
            </div>
            <div class="table-responsive">
                <table class="table table-hover table-striped table-bordered align-middle">
                    <thead class="table-light">
                        <tr>
                            <th>ID</th>
                            <th>姓名</th>
                            <th>邮箱</th>
                            <th>状态</th>
                            <th>创建时间</th>
                            <th>更新时间</th>
                            <th>最后登录时间</th>
                            <th>版本号</th>
                            <th style="min-width:160px;">操作</th>
                        </tr>
                    </thead>
                    <tbody id="superAdminTableBody">
                        <!-- 超级管理员数据将通过JS动态加载 -->
                    </tbody>
                </table>
            </div>
            <div class="d-flex justify-content-between align-items-center mt-4">
                <div class="text-sm text-gray-500">
                    显示 <span id="superAdminStartRecord">1</span> 到 <span id="superAdminEndRecord">10</span> 条，共 <span id="superAdminTotalRecords">0</span> 条记录
                </div>
                <nav>
                    <ul class="pagination" id="superAdminPagination">
                        <!-- 分页按钮JS生成 -->
                    </ul>
                </nav>
            </div>
        </div>
    </div>

    <!-- 修改个人信息模态框 -->
    <div class="modal fade" id="editProfileModal" tabindex="-1" aria-labelledby="editProfileModalLabel" aria-hidden="true">
      <div class="modal-dialog">
        <div class="modal-content">
          <form id="editProfileForm">
            <div class="modal-header">
              <h5 class="modal-title" id="editProfileModalLabel">修改个人信息</h5>
              <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="关闭"></button>
            </div>
            <div class="modal-body">
              <div class="mb-3">
                <label for="editName" class="form-label">姓名</label>
                <input type="text" class="form-control" id="editName" name="name" required>
              </div>
              <div class="mb-3">
                <label for="editEmail" class="form-label">邮箱</label>
                <input type="email" class="form-control" id="editEmail" name="email" required>
              </div>
              <div class="mb-3">
                <label for="editPassword" class="form-label">新密码</label>
                <input type="password" class="form-control" id="editPassword" name="password" placeholder="如不修改请留空">
              </div>
            </div>
            <div class="modal-footer">
              <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">取消</button>
              <button type="submit" class="btn btn-primary">保存修改</button>
            </div>
          </form>
        </div>
      </div>
    </div>

    <script>
        function formatDate(dateStr) {
            if (!dateStr) return '-';
            // 兼容 yyyy-MM-dd HH:mm:ss 格式
            // 替换为 yyyy/MM/dd HH:mm:ss，保证所有浏览器都能解析
            var safeStr = dateStr.replace(/-/g, '/');
            var d = new Date(safeStr);
            if (isNaN(d.getTime())) return dateStr; // 解析失败就原样返回
            var y = d.getFullYear();
            var m = ('0' + (d.getMonth() + 1)).slice(-2);
            var day = ('0' + d.getDate()).slice(-2);
            var h = ('0' + d.getHours()).slice(-2);
            var min = ('0' + d.getMinutes()).slice(-2);
            var s = ('0' + d.getSeconds()).slice(-2);
            return y + '-' + m + '-' + day + ' ' + h + ':' + min + ':' + s;
        }

        function handleLogout() {
            if (confirm('确定要退出登录吗？')) {
                window.location.href = '${pageContext.request.contextPath}/logout';
            }
        }
        
        $(document).ready(function() {
            // 填充当前信息
            $('#editProfileModal').on('show.bs.modal', function () {
                $('#editName').val('${sessionScope.user.name}');
                $('#editEmail').val('${sessionScope.user.email}');
                $('#editPassword').val('');
            });

            // 表单提交
            $('#editProfileForm').on('submit', function(e) {
                e.preventDefault();
                var formData = {
                    name: $('#editName').val(),
                    email: $('#editEmail').val(),
                    password: $('#editPassword').val()
                };
                $.ajax({
                    url: '${pageContext.request.contextPath}/super-admin/update/per-info',
                    type: 'POST',
                    contentType: 'application/json',
                    data: JSON.stringify(formData),
                    success: function(response) {
                        if (response.success) {
                            $('#editProfileModal').modal('hide');
                            alert('修改成功，请重新登录或刷新页面！');
                            location.reload();
                        } else {
                            alert('修改失败：' + (response.message || '未知错误'));
                        }
                    },
                    error: function(xhr) {
                        alert('请求失败：' + (xhr.responseJSON && xhr.responseJSON.message ? xhr.responseJSON.message : '服务器错误'));
                    }
                });
            });

            // 学生列表相关
            var studentPageNum = 1;
            var studentPageSize = 10;
            function loadStudentList(pageNum) {
                pageNum = pageNum || 1;
                var nameLike = $('#studentSearch').val();
                $.ajax({
                    url: '${pageContext.request.contextPath}/student/page-list',
                    type: 'GET',
                    data: {
                        pageNum: pageNum,
                        pageSize: studentPageSize,
                        nameLike: nameLike
                    },
                    success: function(response) {
                        if (response.success) {
                            // 兼容Result<PageResult<Student>>结构
                            var pageData = response.data && response.data.data ? response.data : response.data;
                            var students = pageData.data || [];
                            var total = pageData.total || 0;
                            var pageNum = pageData.pageNum || 1;
                            var pageSize = pageData.pageSize || 10;
                            updateStudentTable(students);
                            updateStudentPagination(pageNum, pageSize, total);
                        } else {
                            $('#studentTableBody').html('<tr><td colspan="16">加载失败：' + (response.message || '未知错误') + '</td></tr>');
                        }
                    },
                    error: function(xhr) {
                        $('#studentTableBody').html('<tr><td colspan="16">请求失败</td></tr>');
                    }
                });
            }
            function updateStudentTable(students) {
                var tbody = $('#studentTableBody');
                tbody.empty();
                if (!students || students.length === 0) {
                    tbody.append('<tr><td colspan="16" class="text-center">暂无数据</td></tr>');
                    return;
                }
                students.forEach(function(stu) {
                    tbody.append('<tr>' +
                        '<td>' + (stu.id || '-') + '</td>' +
                        '<td>' + (stu.studentNo || '-') + '</td>' +
                        '<td>' + (stu.name || '-') + '</td>' +
                        '<td>' + (stu.college || '-') + '</td>' +
                        '<td>' + (stu.className || '-') + '</td>' +
                        '<td>' + (stu.dormitory || '-') + '</td>' +
                        '<td>' + (stu.email || '-') + '</td>' +
                        '<td>' + (stu.phone || '-') + '</td>' +
                        '<td>' + (stu.fatherName || '-') + '</td>' +
                        '<td>' + (stu.fatherPhone || '-') + '</td>' +
                        '<td>' + (stu.motherName || '-') + '</td>' +
                        '<td>' + (stu.motherPhone || '-') + '</td>' +
                        '<td>' + (stu.createTime ? formatDate(stu.createTime) : '-') + '</td>' +
                        '<td>' + (stu.updateTime ? formatDate(stu.updateTime) : '-') + '</td>' +
                        '<td>' + (stu.lastLoginTime ? formatDate(stu.lastLoginTime) : '-') + '</td>' +
                        '<td>' +
                            '<button class="btn btn-sm btn-primary me-2 edit-student-btn" data-id="' + (stu.id || '') + '">修改</button>' +
                            '<button class="btn btn-sm btn-danger delete-student-btn" data-id="' + (stu.id || '') + '">删除</button>' +
                        '</td>' +
                    '</tr>');
                });
            }
            function updateStudentPagination(pageNum, pageSize, total) {
                var start = (total === 0) ? 0 : ((pageNum - 1) * pageSize + 1);
                var end = Math.min(pageNum * pageSize, total);
                $('#studentStartRecord').text(start);
                $('#studentEndRecord').text(end);
                $('#studentTotalRecords').text(total);
                var totalPages = Math.ceil(total / pageSize);
                var pagin = $('#studentPagination');
                pagin.empty();
                if (totalPages <= 1) return;
                // 上一页
                pagin.append('<li class="page-item' + (pageNum === 1 ? ' disabled' : '') + '"><a class="page-link" href="#" data-page="' + (pageNum - 1) + '">上一页</a></li>');
                for (var i = 1; i <= totalPages; i++) {
                    pagin.append('<li class="page-item' + (i === pageNum ? ' active' : '') + '"><a class="page-link" href="#" data-page="' + i + '">' + i + '</a></li>');
                }
                // 下一页
                pagin.append('<li class="page-item' + (pageNum === totalPages ? ' disabled' : '') + '"><a class="page-link" href="#" data-page="' + (pageNum + 1) + '">下一页</a></li>');
            }
            // 分页点击
            $(document).on('click', '#studentPagination .page-link', function(e) {
                e.preventDefault();
                var page = parseInt($(this).data('page'));
                if (!isNaN(page)) {
                    studentPageNum = page;
                    loadStudentList(studentPageNum);
                }
            });
            // 搜索按钮
            $('#searchStudentBtn').on('click', function() {
                studentPageNum = 1;
                loadStudentList(studentPageNum);
            });
            // 页面加载时自动加载第一页
            loadStudentList(1);

            // 编辑按钮事件
            $(document).on('click', '.edit-student-btn', function() {
                var studentId = $(this).data('id');
                // TODO: 打开编辑模态框，填充学生信息
                alert('点击了修改，学生ID: ' + studentId);
            });
            // 删除按钮事件
            $(document).on('click', '.delete-student-btn', function() {
                var studentId = $(this).data('id');
                if (confirm('确定要删除该学生吗？')) {
                    // TODO: 发送删除请求
                    alert('已确认删除，学生ID: ' + studentId);
                }
            });

            // 班级管理员列表相关
            var sysUserPageNum = 1;
            var sysUserPageSize = 10;
            function loadSysUserList(pageNum) {
                pageNum = pageNum || 1;
                var nameLike = $('#sysUserSearch').val();
                $.ajax({
                    url: '${pageContext.request.contextPath}/sysuser/page-list',
                    type: 'GET',
                    data: {
                        pageNum: pageNum,
                        pageSize: sysUserPageSize,
                        nameLike: nameLike
                    },
                    success: function(response) {
                        if (response.success) {
                            var pageData = response.data && response.data.data ? response.data : response.data;
                            var sysUsers = pageData.data || [];
                            var total = pageData.total || 0;
                            var pageNum = pageData.pageNum || 1;
                            var pageSize = pageData.pageSize || 10;
                            updateSysUserTable(sysUsers);
                            updateSysUserPagination(pageNum, pageSize, total);
                        } else {
                            $('#sysUserTableBody').html('<tr><td colspan="11">加载失败：' + (response.message || '未知错误') + '</td></tr>');
                        }
                    },
                    error: function(xhr) {
                        $('#sysUserTableBody').html('<tr><td colspan="11">请求失败</td></tr>');
                    }
                });
            }
            function updateSysUserTable(sysUsers) {
                var tbody = $('#sysUserTableBody');
                tbody.empty();
                if (!sysUsers || sysUsers.length === 0) {
                    tbody.append('<tr><td colspan="11" class="text-center">暂无数据</td></tr>');
                    return;
                }
                sysUsers.forEach(function(user) {
                    var statusBadge = user.status === 'ENABLE' || user.status === '启用' ? '<span class="badge bg-success">启用</span>' : '<span class="badge bg-secondary">禁用</span>';
                    var toggleBtnText = (user.status === 'ENABLE' || user.status === '启用') ? '禁用' : '启用';
                    tbody.append('<tr>' +
                        '<td>' + (user.id || '-') + '</td>' +
                        '<td>' + (user.sysUserNo || '-') + '</td>' +
                        '<td>' + (user.name || '-') + '</td>' +
                        '<td>' + (user.phone || '-') + '</td>' +
                        '<td>' + (user.email || '-') + '</td>' +
                        '<td>' + (user.jobRole || '-') + '</td>' +
                        '<td>' + statusBadge + '</td>' +
                        '<td>' + (user.lastLoginTime ? formatDate(user.lastLoginTime) : '-') + '</td>' +
                        '<td>' + (user.createTime ? formatDate(user.createTime) : '-') + '</td>' +
                        '<td>' + (user.updateTime ? formatDate(user.updateTime) : '-') + '</td>' +
                        '<td>' +
                            '<button class="btn btn-sm btn-primary me-2 edit-sysuser-btn" data-id="' + (user.id || '') + '">修改</button>' +
                            '<button class="btn btn-sm btn-danger me-2 delete-sysuser-btn" data-id="' + (user.id || '') + '">删除</button>' +
                            '<button class="btn btn-sm btn-warning toggle-sysuser-btn" data-id="' + (user.id || '') + '">' + toggleBtnText + '</button>' +
                        '</td>' +
                    '</tr>');
                });
            }
            function updateSysUserPagination(pageNum, pageSize, total) {
                var start = (total === 0) ? 0 : ((pageNum - 1) * pageSize + 1);
                var end = Math.min(pageNum * pageSize, total);
                $('#sysUserStartRecord').text(start);
                $('#sysUserEndRecord').text(end);
                $('#sysUserTotalRecords').text(total);
                var totalPages = Math.ceil(total / pageSize);
                var pagin = $('#sysUserPagination');
                pagin.empty();
                if (totalPages <= 1) return;
                pagin.append('<li class="page-item' + (pageNum === 1 ? ' disabled' : '') + '"><a class="page-link" href="#" data-page="' + (pageNum - 1) + '">上一页</a></li>');
                for (var i = 1; i <= totalPages; i++) {
                    pagin.append('<li class="page-item' + (i === pageNum ? ' active' : '') + '"><a class="page-link" href="#" data-page="' + i + '">' + i + '</a></li>');
                }
                pagin.append('<li class="page-item' + (pageNum === totalPages ? ' disabled' : '') + '"><a class="page-link" href="#" data-page="' + (pageNum + 1) + '">下一页</a></li>');
            }
            // 分页点击
            $(document).on('click', '#sysUserPagination .page-link', function(e) {
                e.preventDefault();
                var page = parseInt($(this).data('page'));
                if (!isNaN(page)) {
                    sysUserPageNum = page;
                    loadSysUserList(sysUserPageNum);
                }
            });
            // 搜索按钮
            $('#searchSysUserBtn').on('click', function() {
                sysUserPageNum = 1;
                loadSysUserList(sysUserPageNum);
            });
            // 页面加载时自动加载第一页
            loadSysUserList(1);
            // 操作按钮事件
            $(document).on('click', '.edit-sysuser-btn', function() {
                var id = $(this).data('id');
                alert('点击了修改，班级管理员ID: ' + id);
            });
            $(document).on('click', '.delete-sysuser-btn', function() {
                var id = $(this).data('id');
                if (confirm('确定要删除该班级管理员吗？')) {
                    alert('已确认删除，班级管理员ID: ' + id);
                }
            });
            $(document).on('click', '.toggle-sysuser-btn', function() {
                var id = $(this).data('id');
                alert('点击了启用/禁用，班级管理员ID: ' + id);
            });

            // 超级管理员列表相关
            var superAdminPageNum = 1;
            var superAdminPageSize = 10;
            function loadSuperAdminList(pageNum) {
                pageNum = pageNum || 1;
                var nameLike = $('#superAdminSearch').val();
                $.ajax({
                    url: '${pageContext.request.contextPath}/super-admin/page-list',
                    type: 'GET',
                    data: {
                        pageNum: pageNum,
                        pageSize: superAdminPageSize,
                        nameLike: nameLike
                    },
                    success: function(response) {
                        if (response.success) {
                            var pageData = response.data && response.data.data ? response.data : response.data;
                            var superAdmins = pageData.data || [];
                            var total = pageData.total || 0;
                            var pageNum = pageData.pageNum || 1;
                            var pageSize = pageData.pageSize || 10;
                            updateSuperAdminTable(superAdmins);
                            updateSuperAdminPagination(pageNum, pageSize, total);
                        } else {
                            $('#superAdminTableBody').html('<tr><td colspan="9">加载失败：' + (response.message || '未知错误') + '</td></tr>');
                        }
                    },
                    error: function(xhr) {
                        $('#superAdminTableBody').html('<tr><td colspan="9">请求失败</td></tr>');
                    }
                });
            }
            function updateSuperAdminTable(superAdmins) {
                var tbody = $('#superAdminTableBody');
                tbody.empty();
                if (!superAdmins || superAdmins.length === 0) {
                    tbody.append('<tr><td colspan="9" class="text-center">暂无数据</td></tr>');
                    return;
                }
                superAdmins.forEach(function(admin) {
                    var statusBadge = admin.enabled === 1 ? '<span class="badge bg-success">启用</span>' : '<span class="badge bg-secondary">禁用</span>';
                    var toggleBtnText = admin.enabled === 1 ? '禁用' : '启用';
                    tbody.append('<tr>' +
                        '<td>' + (admin.id || '-') + '</td>' +
                        '<td>' + (admin.name || '-') + '</td>' +
                        '<td>' + (admin.email || '-') + '</td>' +
                        '<td>' + statusBadge + '</td>' +
                        '<td>' + (admin.createTime ? formatDate(admin.createTime) : '-') + '</td>' +
                        '<td>' + (admin.updateTime ? formatDate(admin.updateTime) : '-') + '</td>' +
                        '<td>' + (admin.lastLoginTime ? formatDate(admin.lastLoginTime) : '-') + '</td>' +
                        '<td>' + (admin.version != null ? admin.version : '-') + '</td>' +
                        '<td>' +
                            '<button class="btn btn-sm btn-primary me-2 edit-superadmin-btn" data-id="' + (admin.id || '') + '">修改</button>' +
                            '<button class="btn btn-sm btn-danger me-2 delete-superadmin-btn" data-id="' + (admin.id || '') + '">删除</button>' +
                            '<button class="btn btn-sm btn-warning toggle-superadmin-btn" data-id="' + (admin.id || '') + '">' + toggleBtnText + '</button>' +
                        '</td>' +
                    '</tr>');
                });
            }
            function updateSuperAdminPagination(pageNum, pageSize, total) {
                var start = (total === 0) ? 0 : ((pageNum - 1) * pageSize + 1);
                var end = Math.min(pageNum * pageSize, total);
                $('#superAdminStartRecord').text(start);
                $('#superAdminEndRecord').text(end);
                $('#superAdminTotalRecords').text(total);
                var totalPages = Math.ceil(total / pageSize);
                var pagin = $('#superAdminPagination');
                pagin.empty();
                if (totalPages <= 1) return;
                pagin.append('<li class="page-item' + (pageNum === 1 ? ' disabled' : '') + '"><a class="page-link" href="#" data-page="' + (pageNum - 1) + '">上一页</a></li>');
                for (var i = 1; i <= totalPages; i++) {
                    pagin.append('<li class="page-item' + (i === pageNum ? ' active' : '') + '"><a class="page-link" href="#" data-page="' + i + '">' + i + '</a></li>');
                }
                pagin.append('<li class="page-item' + (pageNum === totalPages ? ' disabled' : '') + '"><a class="page-link" href="#" data-page="' + (pageNum + 1) + '">下一页</a></li>');
            }
            // 分页点击
            $(document).on('click', '#superAdminPagination .page-link', function(e) {
                e.preventDefault();
                var page = parseInt($(this).data('page'));
                if (!isNaN(page)) {
                    superAdminPageNum = page;
                    loadSuperAdminList(superAdminPageNum);
                }
            });
            // 搜索按钮
            $('#searchSuperAdminBtn').on('click', function() {
                superAdminPageNum = 1;
                loadSuperAdminList(superAdminPageNum);
            });
            // 页面加载时自动加载第一页
            loadSuperAdminList(1);
            // 操作按钮事件
            $(document).on('click', '.edit-superadmin-btn', function() {
                var id = $(this).data('id');
                alert('点击了修改，超级管理员ID: ' + id);
            });
            $(document).on('click', '.delete-superadmin-btn', function() {
                var id = $(this).data('id');
                if (confirm('确定要删除该超级管理员吗？')) {
                    alert('已确认删除，超级管理员ID: ' + id);
                }
            });
            $(document).on('click', '.toggle-superadmin-btn', function() {
                var id = $(this).data('id');
                alert('点击了启用/禁用，超级管理员ID: ' + id);
            });
        });
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