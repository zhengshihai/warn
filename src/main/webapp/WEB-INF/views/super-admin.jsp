<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<!DOCTYPE html>
<html>
<head>
    <title>学生晚归预警系统 - 超级管理员</title>
    <link href="https://cdn.bootcdn.net/ajax/libs/tailwindcss/2.2.19/tailwind.min.css" rel="stylesheet">
    <link href="https://cdn.bootcdn.net/ajax/libs/font-awesome/6.0.0/css/all.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/css/bootstrap.min.css" rel="stylesheet">
    <script src="https://cdn.jsdelivr.net/npm/@popperjs/core@2.10.2/dist/umd/popper.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/js/bootstrap.min.js"></script>
    <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
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
                    <button class="text-sm text-red-600 hover:text-red-800" onclick="handleLogout()">退出登录</button>
                </div>
            </div>
        </nav>

        <!-- 主要内容区域 -->
        <div class="card p-6">
            <!-- 标签页导航 -->
            <ul class="nav nav-tabs mb-4" id="adminTabs" role="tablist">
                <li class="nav-item" role="presentation">
                    <button class="nav-link active" id="users-tab" data-bs-toggle="tab" data-bs-target="#users" type="button" role="tab">用户管理</button>
                </li>
                <li class="nav-item" role="presentation">
                    <button class="nav-link" id="logs-tab" data-bs-toggle="tab" data-bs-target="#logs" type="button" role="tab">操作日志</button>
                </li>
            </ul>

            <!-- 标签页内容 -->
            <div class="tab-content" id="adminTabsContent">
                <!-- 用户管理标签页 -->
                <div class="tab-pane fade show active" id="users" role="tabpanel">
                    <div class="mb-4 flex justify-between items-center">
                        <h3 class="text-lg font-medium text-gray-900">用户列表</h3>
                        <button class="btn btn-primary" data-bs-toggle="modal" data-bs-target="#addUserModal">
                            <i class="fas fa-plus"></i> 添加用户
                        </button>
                    </div>
                    
                    <!-- 用户搜索和筛选 -->
                    <div class="mb-4 grid grid-cols-1 md:grid-cols-4 gap-4">
                        <div class="col-span-2">
                            <input type="text" class="form-control" placeholder="搜索用户..." id="userSearch">
                        </div>
                        <div>
                            <select class="form-select" id="userRole">
                                <option value="">所有角色</option>
                                <option value="ADMIN">管理员</option>
                                <option value="STAFF">工作人员</option>
                                <option value="USER">普通用户</option>
                            </select>
                        </div>
                        <div>
                            <select class="form-select" id="userStatus">
                                <option value="">所有状态</option>
                                <option value="ACTIVE">正常</option>
                                <option value="INACTIVE">禁用</option>
                            </select>
                        </div>
                    </div>

                    <!-- 用户列表表格 -->
                    <div class="table-responsive">
                        <table class="table table-hover">
                            <thead>
                                <tr>
                                    <th>ID</th>
                                    <th>用户名</th>
                                    <th>姓名</th>
                                    <th>角色</th>
                                    <th>状态</th>
                                    <th>最后登录</th>
                                    <th>操作</th>
                                </tr>
                            </thead>
                            <tbody id="userTableBody">
                                <!-- 用户数据将通过JavaScript动态加载 -->
                            </tbody>
                        </table>
                    </div>

                    <!-- 分页控件 -->
                    <div class="d-flex justify-content-between align-items-center mt-4">
                        <div class="text-sm text-gray-500">
                            显示 <span id="startRecord">1</span> 到 <span id="endRecord">10</span> 条，共 <span id="totalRecords">100</span> 条记录
                        </div>
                        <nav>
                            <ul class="pagination">
                                <li class="page-item"><a class="page-link" href="#">上一页</a></li>
                                <li class="page-item active"><a class="page-link" href="#">1</a></li>
                                <li class="page-item"><a class="page-link" href="#">2</a></li>
                                <li class="page-item"><a class="page-link" href="#">3</a></li>
                                <li class="page-item"><a class="page-link" href="#">下一页</a></li>
                            </ul>
                        </nav>
                    </div>
                </div>

                <!-- 操作日志标签页 -->
                <div class="tab-pane fade" id="logs" role="tabpanel">
                    <div class="mb-4">
                        <h3 class="text-lg font-medium text-gray-900">操作日志</h3>
                    </div>

                    <!-- 日志筛选 -->
                    <div class="mb-4 grid grid-cols-1 md:grid-cols-4 gap-4">
                        <div class="col-span-2">
                            <input type="text" class="form-control" placeholder="搜索操作内容..." id="logSearch">
                        </div>
                        <div>
                            <select class="form-select" id="logType">
                                <option value="">所有类型</option>
                                <option value="LOGIN">登录</option>
                                <option value="CREATE">创建</option>
                                <option value="UPDATE">更新</option>
                                <option value="DELETE">删除</option>
                            </select>
                        </div>
                        <div>
                            <input type="date" class="form-control" id="logDate">
                        </div>
                    </div>

                    <!-- 日志列表表格 -->
                    <div class="table-responsive">
                        <table class="table table-hover">
                            <thead>
                                <tr>
                                    <th>时间</th>
                                    <th>用户</th>
                                    <th>操作类型</th>
                                    <th>操作内容</th>
                                    <th>IP地址</th>
                                </tr>
                            </thead>
                            <tbody id="logTableBody">
                                <!-- 日志数据将通过JavaScript动态加载 -->
                            </tbody>
                        </table>
                    </div>

                    <!-- 分页控件 -->
                    <div class="d-flex justify-content-between align-items-center mt-4">
                        <div class="text-sm text-gray-500">
                            显示 <span id="logStartRecord">1</span> 到 <span id="logEndRecord">10</span> 条，共 <span id="logTotalRecords">100</span> 条记录
                        </div>
                        <nav>
                            <ul class="pagination">
                                <li class="page-item"><a class="page-link" href="#">上一页</a></li>
                                <li class="page-item active"><a class="page-link" href="#">1</a></li>
                                <li class="page-item"><a class="page-link" href="#">2</a></li>
                                <li class="page-item"><a class="page-link" href="#">3</a></li>
                                <li class="page-item"><a class="page-link" href="#">下一页</a></li>
                            </ul>
                        </nav>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <!-- 添加用户模态框 -->
    <div class="modal fade" id="addUserModal" tabindex="-1">
        <div class="modal-dialog">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title">添加新用户</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                </div>
                <div class="modal-body">
                    <form id="addUserForm">
                        <div class="mb-3">
                            <label class="form-label">用户名</label>
                            <input type="text" class="form-control" name="username" required>
                        </div>
                        <div class="mb-3">
                            <label class="form-label">姓名</label>
                            <input type="text" class="form-control" name="name" required>
                        </div>
                        <div class="mb-3">
                            <label class="form-label">密码</label>
                            <input type="password" class="form-control" name="password" required>
                        </div>
                        <div class="mb-3">
                            <label class="form-label">角色</label>
                            <select class="form-select" name="role" required>
                                <option value="ADMIN">管理员</option>
                                <option value="STAFF">工作人员</option>
                                <option value="USER">普通用户</option>
                            </select>
                        </div>
                        <div class="mb-3">
                            <label class="form-label">邮箱</label>
                            <input type="email" class="form-control" name="email" required>
                        </div>
                        <div class="mb-3">
                            <label class="form-label">手机号</label>
                            <input type="tel" class="form-control" name="phone" required>
                        </div>
                    </form>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">取消</button>
                    <button type="button" class="btn btn-primary" id="saveUserBtn">保存</button>
                </div>
            </div>
        </div>
    </div>

    <!-- 编辑用户模态框 -->
    <div class="modal fade" id="editUserModal" tabindex="-1">
        <div class="modal-dialog">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title">编辑用户</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                </div>
                <div class="modal-body">
                    <form id="editUserForm">
                        <input type="hidden" name="userId">
                        <div class="mb-3">
                            <label class="form-label">用户名</label>
                            <input type="text" class="form-control" name="username" readonly>
                        </div>
                        <div class="mb-3">
                            <label class="form-label">姓名</label>
                            <input type="text" class="form-control" name="name" required>
                        </div>
                        <div class="mb-3">
                            <label class="form-label">角色</label>
                            <select class="form-select" name="role" required>
                                <option value="ADMIN">管理员</option>
                                <option value="STAFF">工作人员</option>
                                <option value="USER">普通用户</option>
                            </select>
                        </div>
                        <div class="mb-3">
                            <label class="form-label">状态</label>
                            <select class="form-select" name="status" required>
                                <option value="ACTIVE">正常</option>
                                <option value="INACTIVE">禁用</option>
                            </select>
                        </div>
                        <div class="mb-3">
                            <label class="form-label">邮箱</label>
                            <input type="email" class="form-control" name="email" required>
                        </div>
                        <div class="mb-3">
                            <label class="form-label">手机号</label>
                            <input type="tel" class="form-control" name="phone" required>
                        </div>
                    </form>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">取消</button>
                    <button type="button" class="btn btn-primary" id="updateUserBtn">更新</button>
                </div>
            </div>
        </div>
    </div>

    <script>
        // 页面加载完成后执行
        $(document).ready(function() {
            // 加载用户列表
            loadUsers();
            // 加载操作日志
            loadLogs();

            // 用户搜索和筛选事件
            $('#userSearch, #userRole, #userStatus').on('change keyup', function() {
                loadUsers();
            });

            // 日志搜索和筛选事件
            $('#logSearch, #logType, #logDate').on('change keyup', function() {
                loadLogs();
            });

            // 保存新用户
            $('#saveUserBtn').click(function() {
                const formData = $('#addUserForm').serialize();
                $.ajax({
                    url: '${pageContext.request.contextPath}/admin/users',
                    type: 'POST',
                    data: formData,
                    success: function(response) {
                        $('#addUserModal').modal('hide');
                        loadUsers();
                        showToast('用户添加成功');
                    },
                    error: function(xhr) {
                        showToast('添加失败：' + xhr.responseText, 'error');
                    }
                });
            });

            // 更新用户信息
            $('#updateUserBtn').click(function() {
                const formData = $('#editUserForm').serialize();
                $.ajax({
                    url: '${pageContext.request.contextPath}/admin/users',
                    type: 'PUT',
                    data: formData,
                    success: function(response) {
                        $('#editUserModal').modal('hide');
                        loadUsers();
                        showToast('用户更新成功');
                    },
                    error: function(xhr) {
                        showToast('更新失败：' + xhr.responseText, 'error');
                    }
                });
            });
        });

        // 加载用户列表
        function loadUsers() {
            const searchParams = {
                search: $('#userSearch').val(),
                role: $('#userRole').val(),
                status: $('#userStatus').val()
            };

            $.ajax({
                url: '${pageContext.request.contextPath}/admin/users',
                type: 'GET',
                data: searchParams,
                success: function(response) {
                    updateUserTable(response);
                },
                error: function(xhr) {
                    showToast('加载用户列表失败：' + xhr.responseText, 'error');
                }
            });
        }

        // 加载操作日志
        function loadLogs() {
            const searchParams = {
                search: $('#logSearch').val(),
                type: $('#logType').val(),
                date: $('#logDate').val()
            };

            $.ajax({
                url: '${pageContext.request.contextPath}/admin/logs',
                type: 'GET',
                data: searchParams,
                success: function(response) {
                    updateLogTable(response);
                },
                error: function(xhr) {
                    showToast('加载操作日志失败：' + xhr.responseText, 'error');
                }
            });
        }

        // 更新用户表格
        function updateUserTable(data) {
            const tbody = $('#userTableBody');
            tbody.empty();

            data.users.forEach(user => {
                tbody.append(`
                    <tr>
                        <td>${user.id}</td>
                        <td>${user.username}</td>
                        <td>${user.name}</td>
                        <td>${user.role}</td>
                        <td>
                            <span class="badge ${user.status == 'ACTIVE' ? 'bg-success' : 'bg-danger'}">
                                ${user.status == 'ACTIVE' ? '正常' : '禁用'}
                            </span>
                        </td>
                        <td><fmt:formatDate value="${user.lastLoginTime}" pattern="yyyy-MM-dd HH:mm:ss"/></td>
                        <td>
                            <button class="btn btn-sm btn-primary" onclick="editUser(${user.id})">
                                <i class="fas fa-edit"></i>
                            </button>
                            <button class="btn btn-sm btn-danger" onclick="deleteUser(${user.id})">
                                <i class="fas fa-trash"></i>
                            </button>
                        </td>
                    </tr>
                `);
            });
        }

        // 更新日志表格
        function updateLogTable(data) {
            const tbody = $('#logTableBody');
            tbody.empty();

            data.logs.forEach(log => {
                tbody.append(`
                    <tr>
                        <td>${formatDate(log.operationTime)}</td>
                        <td>${log.username}</td>
                        <td>${log.operationType}</td>
                        <td>${log.operationContent}</td>
                        <td>${log.ipAddress}</td>
                    </tr>
                `);
            });
        }

        // 编辑用户
        function editUser(userId) {
            $.ajax({
                url: '${pageContext.request.contextPath}/admin/users/' + userId,
                type: 'GET',
                success: function(user) {
                    const form = $('#editUserForm');
                    form.find('[name=userId]').val(user.id);
                    form.find('[name=username]').val(user.username);
                    form.find('[name=name]').val(user.name);
                    form.find('[name=role]').val(user.role);
                    form.find('[name=status]').val(user.status);
                    form.find('[name=email]').val(user.email);
                    form.find('[name=phone]').val(user.phone);
                    $('#editUserModal').modal('show');
                },
                error: function(xhr) {
                    showToast('获取用户信息失败：' + xhr.responseText, 'error');
                }
            });
        }

        // 删除用户
        function deleteUser(userId) {
            if (confirm('确定要删除这个用户吗？')) {
                $.ajax({
                    url: '${pageContext.request.contextPath}/admin/users/' + userId,
                    type: 'DELETE',
                    success: function() {
                        loadUsers();
                        showToast('用户删除成功');
                    },
                    error: function(xhr) {
                        showToast('删除失败：' + xhr.responseText, 'error');
                    }
                });
            }
        }

        // 格式化日期
        function formatDate(dateString) {
            if (!dateString) return '-';
            const date = new Date(dateString);
            return date.toLocaleString();
        }

        // 显示提示消息
        function showToast(message, type = 'success') {
            // 这里可以实现一个简单的提示消息显示功能
            alert(message);
        }

        // 退出登录
        function handleLogout() {
            if (confirm('确定要退出登录吗？')) {
                window.location.href = '${pageContext.request.contextPath}/logout';
            }
        }
    </script>
</body>
</html> 