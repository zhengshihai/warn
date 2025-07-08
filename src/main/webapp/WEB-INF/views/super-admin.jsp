<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<!DOCTYPE html>
<html>
<head>
    <title>学生晚归预警系统 - 超级管理员</title> <!--todo 宿管角色列表无法显示 简直莫名其妙-->

    <!-- 引入 CSS 文件 -->
    <link href="${pageContext.request.contextPath}/static/css/tailwind.min.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/static/css/fontawesome.min.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/static/css/bootstrap.min.css" rel="stylesheet">

    <!-- 引入 UniverJS 相关本地 CSS 文件 -->
    <link href="${pageContext.request.contextPath}/static/univer/design.css" />
    <link href="${pageContext.request.contextPath}/static/univer/ui.css" />
<%--    <link href="${pageContext.request.contextPath}/static/univer/sheets.css" />--%>
    <link href="${pageContext.request.contextPath}/static/univer/sheets-ui.css" />

    <!-- 引入 JS 文件 -->
    <script src="${pageContext.request.contextPath}/static/js/jquery.min.js"></script>
    <script src="${pageContext.request.contextPath}/static/js/popper.min.js"></script>
    <script src="${pageContext.request.contextPath}/static/js/bootstrap.bundle.min.js"></script>

    <!-- 引入 UniverJS 相关本地 JS 文件 -->
    <script src="${pageContext.request.contextPath}/static/univer/core.js"></script>
    <script src="${pageContext.request.contextPath}/static/univer/design.js"></script>
    <script src="${pageContext.request.contextPath}/static/univer/ui.js"></script>
    <script src="${pageContext.request.contextPath}/static/univer/sheets.js"></script>
    <script src="${pageContext.request.contextPath}/static/univer/sheets-ui.js"></script>

    

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
        /* 批量导入功能样式 */
        .import-section {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            border-radius: 0.5rem;
            padding: 1.5rem;
            margin-bottom: 1.5rem;
        }
        .import-section h3 {
            color: white;
            margin-bottom: 1rem;
        }
        .import-section .form-label {
            color: white;
            font-weight: 500;
        }
        .import-section .form-control,
        .import-section .form-select {
            border: none;
            border-radius: 0.375rem;
            background-color: rgba(255, 255, 255, 0.9);
        }
        .import-section .form-control:focus,
        .import-section .form-select:focus {
            background-color: white;
            box-shadow: 0 0 0 0.2rem rgba(255, 255, 255, 0.25);
        }
        .import-section .btn-primary {
            background-color: #28a745;
            border-color: #28a745;
            font-weight: 500;
        }
        .import-section .btn-primary:hover {
            background-color: #218838;
            border-color: #1e7e34;
        }
        .import-section .btn-primary:disabled {
            background-color: #6c757d;
            border-color: #6c757d;
        }
        /* 错误数据模态框样式 */
        #univer-container {
            border: 1px solid #dee2e6;
            border-radius: 0.375rem;
        }
        .error-data-alert {
            background-color: #f8d7da;
            border-color: #f5c6cb;
            color: #721c24;
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

        <!-- 批量导入用户信息功能区域 -->
        <div class="card p-6 mb-6 import-section">
            <div class="mb-4 flex justify-between items-center">
                <h3 class="text-lg font-medium">批量导入用户信息</h3>
            </div>
            <div class="row">
                <div class="col-md-4">
                    <label for="userRoleSelect" class="form-label">选择用户角色：</label>
                    <select id="userRoleSelect" class="form-select">
                        <option value="">请选择用户角色</option>
                        <option value="student">学生</option>
                        <option value="dormitorymanager">宿管</option>
                        <option value="systemuser">班级管理员</option>
                        <option value="superadmin">超级管理员</option>
                    </select>
                </div>
                <div class="col-md-4">
                    <label for="excelFile" class="form-label">选择Excel文件：</label>
                    <input type="file" id="excelFile" class="form-control" accept=".xls,.xlsx">
                </div>
                <div class="col-md-4 d-flex align-items-end">
                    <button type="button" id="importBtn" class="btn btn-primary" disabled>
                        <i class="fas fa-upload"></i> 批量导入
                    </button>
                </div>
            </div>
            <div class="mt-3">
                <small class="text-white-50">
                    支持的文件格式：.xls, .xlsx，文件大小不超过10MB
                </small>
            </div>
        </div>

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

    <div>88888888888888888888888888888</div>

    <!-- 宿管列表内容区域 -->
    <div class="card p-6 mb-6">
        <div class="mb-4 flex justify-between items-center">
            <h3 class="text-lg font-medium text-gray-900">宿管列表</h3>
            <div>
                <input type="text" id="dormManSearch" class="form-control d-inline-block w-auto" placeholder="搜索姓名...">
                <button class="btn btn-primary ml-2" id="searchDormManBtn">搜索</button>
            </div>
        </div>
        <div class="table-responsive">
            <table class="table table-hover table-striped table-bordered align-middle">
                <thead class="table-light">
                <tr>
                    <th>ID</th>
                    <th>工号</th>
                    <th>姓名</th>
                    <th>负责宿舍楼</th>
                    <th>电话</th>
                    <th>邮箱</th>
                    <th>状态</th>
                    <th>创建时间</th>
                    <th>更新时间</th>
                    <th>最后登录时间</th>
                    <th style="min-width:160px;">操作</th>
                </tr>
                </thead>
                <div>555555555555555555</div>
                <tbody id="dormManTableBody">
                <!-- 宿管数据将通过JS动态加载 -->
                </tbody>
            </table>
        </div>
        <div class="d-flex justify-content-between align-items-center mt-4">
            <div class="text-sm text-gray-500">
                显示 <span id="dormManStartRecord">1</span> 到 <span id="dormManEndRecord">10</span> 条，共 <span id="dormManTotalRecords">0</span> 条记录
            </div>
            <nav>
                <ul class="pagination" id="dormManPagination">
                    <!-- 分页按钮JS生成 -->
                </ul>
            </nav>
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

<!-- 编辑学生信息模态框 -->
<div class="modal fade" id="editStudentModal" tabindex="-1" aria-labelledby="editStudentModalLabel" aria-hidden="true">
    <div class="modal-dialog modal-lg">
            <div class="modal-content">
            <form id="editStudentForm">
                <div class="modal-header">
                    <h5 class="modal-title" id="editStudentModalLabel">编辑学生信息</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="关闭"></button>
                </div>
                <div class="modal-body">
                    <input type="hidden" id="studentId" name="id">
                    <div class="row">
                        <div class="col-md-6">
                            <div class="mb-3">
                                <label for="studentNo" class="form-label">学号</label>
                                <input type="text" class="form-control" id="studentNo" name="studentNo" required>
                            </div>
                            <div class="mb-3">
                                <label for="studentName" class="form-label">姓名</label>
                                <input type="text" class="form-control" id="studentName" name="name" required>
                            </div>
                        <div class="mb-3">
                                <label for="studentCollege" class="form-label">学院</label>
                                <input type="text" class="form-control" id="studentCollege" name="college">
                        </div>
                        <div class="mb-3">
                                <label for="studentClassName" class="form-label">班级</label>
                                <input type="text" class="form-control" id="studentClassName" name="className">
                        </div>
                        <div class="mb-3">
                                <label for="studentDormitory" class="form-label">宿舍</label>
                                <input type="text" class="form-control" id="studentDormitory" name="dormitory">
                            </div>
                        </div>
                        <div class="col-md-6">
                            <div class="mb-3">
                                <label for="studentEmail" class="form-label">邮箱</label>
                                <input type="email" class="form-control" id="studentEmail" name="email">
                            </div>
                            <div class="mb-3">
                                <label for="studentPhone" class="form-label">电话</label>
                                <input type="text" class="form-control" id="studentPhone" name="phone">
                            </div>
                            <div class="mb-3">
                                <label for="studentPassword" class="form-label">密码</label>
                                <input type="password" class="form-control" id="studentPassword" name="password" placeholder="留空则不修改">
                            </div>
                            <div class="mb-3">
                                <label for="studentFatherName" class="form-label">父亲姓名</label>
                                <input type="text" class="form-control" id="studentFatherName" name="fatherName">
                        </div>
                        <div class="mb-3">
                                <label for="studentFatherPhone" class="form-label">父亲电话</label>
                                <input type="text" class="form-control" id="studentFatherPhone" name="fatherPhone">
                            </div>
                        </div>
                    </div>
                    <div class="row">
                        <div class="col-md-6">
                        <div class="mb-3">
                                <label for="studentMotherName" class="form-label">母亲姓名</label>
                                <input type="text" class="form-control" id="studentMotherName" name="motherName">
                            </div>
                        </div>
                        <div class="col-md-6">
                        <div class="mb-3">
                                <label for="studentMotherPhone" class="form-label">母亲电话</label>
                                <input type="text" class="form-control" id="studentMotherPhone" name="motherPhone">
                            </div>
                        </div>
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

<!-- 编辑班级管理员信息模态框 -->
<div class="modal fade" id="editSysUserModal" tabindex="-1" aria-labelledby="editSysUserModalLabel" aria-hidden="true">
    <div class="modal-dialog modal-lg">
        <div class="modal-content">
            <form id="editSysUserForm">
                <div class="modal-header">
                    <h5 class="modal-title" id="editSysUserModalLabel">编辑班级管理员信息</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="关闭"></button>
                </div>
                <div class="modal-body">
                    <input type="hidden" id="sysUserId" name="id">
                    <div class="row">
                        <div class="col-md-6">
                            <div class="mb-3">
                                <label for="sysUserNo" class="form-label">工号</label>
                                <input type="text" class="form-control" id="sysUserNo" name="sysUserNo" required>
                            </div>
                            <div class="mb-3">
                                <label for="sysUserName" class="form-label">姓名</label>
                                <input type="text" class="form-control" id="sysUserName" name="name" required>
                            </div>
                            <div class="mb-3">
                                <label for="sysUserPhone" class="form-label">电话</label>
                                <input type="text" class="form-control" id="sysUserPhone" name="phone" required>
                            </div>
                            <div class="mb-3">
                                <label for="sysUserEmail" class="form-label">邮箱</label>
                                <input type="email" class="form-control" id="sysUserEmail" name="email" required>
                            </div>
                        </div>
                        <div class="col-md-6">
                            <div class="mb-3">
                                <label for="sysUserJobRole" class="form-label">职位角色</label>
                                <select class="form-control" id="sysUserJobRole" name="jobRole" required>
                                    <option value="">请选择职位角色</option>
                                    <option value="COUNSELOR">辅导员</option>
                                    <option value="CLASS_TEACHER">班主任</option>
                                    <option value="SUPER_ADMIN">超级管理员</option>
                                    <option value="DORMITORY_MANAGER">宿管</option>
                                </select>
                            </div>
                            <div class="mb-3">
                                <label for="sysUserPassword" class="form-label">密码</label>
                                <input type="password" class="form-control" id="sysUserPassword" name="password" placeholder="留空则不修改">
                            </div>
                            <div class="mb-3">
                                <label for="sysUserStatus" class="form-label">状态</label>
                                <select class="form-control" id="sysUserStatus" name="status" required>
                                    <option value="ENABLE">启用</option>
                                    <option value="DISABLE">禁用</option>
                                </select>
                            </div>
                        </div>
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

<!-- 超级管理员编辑模态框 -->
<div class="modal fade" id="editSuperAdminModal" tabindex="-1" aria-labelledby="editSuperAdminModalLabel" aria-hidden="true">
    <div class="modal-dialog modal-lg">
        <div class="modal-content">
            <form id="editSuperAdminForm">
                <div class="modal-header">
                    <h5 class="modal-title" id="editSuperAdminModalLabel">编辑超级管理员信息</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="关闭"></button>
                </div>
                <div class="modal-body">
                    <input type="hidden" id="superAdminId" name="id">
                    <div class="row">
                        <div class="col-md-6">
                            <div class="mb-3">
                                <label for="superAdminName" class="form-label">姓名</label>
                                <input type="text" class="form-control" id="superAdminName" name="name" required>
                            </div>
                            <div class="mb-3">
                                <label for="superAdminEmail" class="form-label">邮箱</label>
                                <input type="email" class="form-control" id="superAdminEmail" name="email" required>
                            </div>
                        </div>
                        <div class="col-md-6">
                            <div class="mb-3">
                                <label for="superAdminPassword" class="form-label">密码</label>
                                <input type="password" class="form-control" id="superAdminPassword" name="password" placeholder="留空则不修改">
                            </div>
                            <div class="mb-3">
                                <label for="superAdminEnabled" class="form-label">状态</label>
                                <select class="form-control" id="superAdminEnabled" name="enabled" required>
                                    <option value="1">启用</option>
                                    <option value="0">禁用</option>
                                </select>
                            </div>
                        </div>
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



<!-- 宿管编辑模态框 -->
<div class="modal fade" id="editDormManModal" tabindex="-1" aria-labelledby="editDormManModalLabel" aria-hidden="true">
    <div class="modal-dialog modal-lg">
        <div class="modal-content">
            <form id="editDormManForm">
                <div class="modal-header">
                    <h5 class="modal-title" id="editDormManModalLabel">编辑宿管信息</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="关闭"></button>
                </div>
                <div class="modal-body">
                    <input type="hidden" id="dormManId" name="id">
                    <div class="row">
                        <div class="col-md-6">
                            <div class="mb-3">
                                <label for="dormManManagerId" class="form-label">工号</label>
                                <input type="text" class="form-control" id="dormManManagerId" name="managerId" required>
                            </div>
                            <div class="mb-3">
                                <label for="dormManName" class="form-label">姓名</label>
                                <input type="text" class="form-control" id="dormManName" name="name" required>
                            </div>
                            <div class="mb-3">
                                <label for="dormManBuilding" class="form-label">负责宿舍楼</label>
                                <input type="text" class="form-control" id="dormManBuilding" name="building" required>
                            </div>
                            <div class="mb-3">
                                <label for="dormManPhone" class="form-label">电话</label>
                                <input type="text" class="form-control" id="dormManPhone" name="phone" required>
                            </div>
                        </div>
                        <div class="col-md-6">
                            <div class="mb-3">
                                <label for="dormManEmail" class="form-label">邮箱</label>
                                <input type="email" class="form-control" id="dormManEmail" name="email" required>
                            </div>
                            <div class="mb-3">
                                <label for="dormManPassword" class="form-label">密码</label>
                                <input type="password" class="form-control" id="dormManPassword" name="password" placeholder="留空则不修改">
                            </div>
                            <div class="mb-3">
                                <label for="dormManStatus" class="form-label">状态</label>
                                <select class="form-control" id="dormManStatus" name="status" required>
                                    <option value="ON_DUTY">在职</option>
                                    <option value="OFF_DUTY">离职</option>
                                </select>
                            </div>
                        </div>
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
            localStorage.removeItem('loginUUID');
                window.location.href = '${pageContext.request.contextPath}/logout';
        }
    }

    $(document).ready(function() {
        // 验证是否已经登录
        $.ajaxSetup({
            beforeSend: function(xhr) {
                var loginUUID = localStorage.getItem('loginUUID');
                if (loginUUID) {
                    xhr.setRequestHeader('X-Login-UUID', loginUUID);
                }
            }
        });

        // 填充当前信息
        $('#editProfileModal').on('show.bs.modal', function () {
            $('#editName').val('${sessionScope.user.name}');
            $('#editEmail').val('${sessionScope.user.email}');
            $('#editPassword').val('');
        });

        // 修改超级管理员个人信息表单提交
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
        var studentPageSize = 15;
        function loadStudentList(pageNum) {
            pageNum = pageNum || 1;
            var nameLike = $('#studentSearch').val().trim();
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
                        var pageSize = pageData.pageSize || 15;
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
            if(pageNum > 1) {
                pagin.append('<li class="page-item"><a class="page-link" href="#" data-page="' + (pageNum - 1) + '">上一页</a></li>');
            } else {
                pagin.append('<li class="page-item disabled"><a class="page-link" href="#">上一页</a></li>');
            }

            // 页码
            for (var i = 1; i <= totalPages; i++) {
                pagin.append('<li class="page-item' + (i === pageNum ? ' active' : '') + '"><a class="page-link" href="#" data-page="' + i + '">' + i + '</a></li>');
            }

            // 下一页
            if(pageNum < totalPages) {
                pagin.append('<li class="page-item"><a class="page-link" href="#" data-page="' + (pageNum + 1) + '">下一页</a></li>');
            } else {
                pagin.append('<li class="page-item disabled"><a class="page-link" href="#">下一页</a></li>');
            }

        }

        // 学生分页事件
        $(document).on('click', '#studentPagination a.page-link:not(.disabled)', function(e) {
            e.preventDefault();
            var page = parseInt($(this).data('page'));
            console.log('Page clicked:', page); // 调试

            if (!isNaN(page)) {
                studentPageNum = page;
                loadStudentList(studentPageNum);

                // 滚动到表格顶部
                $('html, body').animate({
                    scrollTop: $('#studentTableBody').offset().top - 20
                }, 200);
            }
        });
        // 搜索按钮
        $('#searchStudentBtn').on('click', function() {
            studentPageNum = 1;
            loadStudentList(studentPageNum);
        });
        
        // 学生搜索框回车事件
        $('#studentSearch').on('keypress', function(e) {
            if (e.which === 13) { // Enter key
                studentPageNum = 1;
                loadStudentList(studentPageNum);
            }
        });
        
        // 页面加载时自动加载第一页
        loadStudentList(1);

        // 编辑按钮事件
        // 更明确的编辑按钮事件委托
        $(document).on('click', '.edit-student-btn', function() {
            var tr = $(this).closest('tr');
            console.log('编辑按钮点击，学生ID:', $(this).data('id')); // 调试输出

            // 填充表单数据
            $('#studentId').val(tr.find('td').eq(0).text().trim());
            $('#studentNo').val(tr.find('td').eq(1).text().trim());
            $('#studentName').val(tr.find('td').eq(2).text().trim());
            $('#studentCollege').val(tr.find('td').eq(3).text().trim());
            $('#studentClassName').val(tr.find('td').eq(4).text().trim());
            $('#studentDormitory').val(tr.find('td').eq(5).text().trim());
            $('#studentEmail').val(tr.find('td').eq(6).text().trim());
            $('#studentPhone').val(tr.find('td').eq(7).text().trim());
            $('#studentFatherName').val(tr.find('td').eq(8).text().trim());
            $('#studentFatherPhone').val(tr.find('td').eq(9).text().trim());
            $('#studentMotherName').val(tr.find('td').eq(10).text().trim());
            $('#studentMotherPhone').val(tr.find('td').eq(11).text().trim());
            $('#studentPassword').val(''); // 清空密码字段

            // 显示模态框
            var editModal = new bootstrap.Modal(document.getElementById('editStudentModal'));
            editModal.show();
        });
        // 提交修改学生信息
        $('#editStudentForm').on('submit', function(e) {
            e.preventDefault();
            var formData = {
                id: $('#studentId').val(),
                studentNo: $('#studentNo').val(),
                name: $('#studentName').val(),
                college: $('#studentCollege').val(),
                className: $('#studentClassName').val(),
                dormitory: $('#studentDormitory').val(),
                email: $('#studentEmail').val(),
                phone: $('#studentPhone').val(),
                password: $('#studentPassword').val(),
                fatherName: $('#studentFatherName').val(),
                fatherPhone: $('#studentFatherPhone').val(),
                motherName: $('#studentMotherName').val(),
                motherPhone: $('#studentMotherPhone').val()
            };
            $.ajax({
                url: '${pageContext.request.contextPath}/student/super-admin/update/per-info',
                type: 'POST',
                contentType: 'application/json',
                data: JSON.stringify(formData),
                success: function(response) {
                    if (response.success) {
                        $('#editStudentModal').modal('hide');
                        alert('修改成功！');
                        loadStudentList(1);
                    } else {
                        alert('修改失败：' + (response.message || '未知错误'));
                    }
                },
                error: function(xhr) {
                    alert('请求失败：' + (xhr.responseJSON && xhr.responseJSON.message ? xhr.responseJSON.message : '服务器错误'));
                }
            });
        });
        // 删除按钮事件
        $(document).on('click', '.delete-student-btn', function() {
            var studentId = $(this).data('id');
            console.log('点击了删除按钮', studentId);
            if (confirm('确定要删除该学生吗？')) {
                // 构造请求体，适配后端 List<Integer> ids
                var data = {
                    ids: [studentId]
                };
                $.ajax({
                    url: '${pageContext.request.contextPath}/student/super-admin/delete',
                    type: 'DELETE',
                    contentType: 'application/json',
                    data: JSON.stringify(data),
                    success: function(response) {
                        if (response.success) {
                            alert('删除成功！');
                            loadStudentList(1); // 重新加载学生列表
                        } else {
                            alert('删除失败：' + (response.message || '未知错误'));
                        }
                    },
                    error: function(xhr) {
                        alert('请求失败：' + (xhr.responseJSON && xhr.responseJSON.message ? xhr.responseJSON.message : '服务器错误'));
                    }
                });
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
        
        // 班级管理员搜索框回车事件
        $('#sysUserSearch').on('keypress', function(e) {
            if (e.which === 13) { // Enter key
                sysUserPageNum = 1;
                loadSysUserList(sysUserPageNum);
            }
        });
        
        // 页面加载时自动加载第一页
        loadSysUserList(1);
        // 操作按钮事件
        $(document).on('click', '.edit-sysuser-btn', function() {
            var id = $(this).data('id');
            // 获取班级管理员信息并打开编辑模态框
            $.ajax({
                url: '${pageContext.request.contextPath}/sysuser/' + id,
                type: 'GET',
                success: function(response) {
                    if (response.success) {
                        var sysUser = response.data;
                        // 填充表单数据
                        $('#sysUserId').val(sysUser.id);
                        $('#sysUserNo').val(sysUser.sysUserNo);
                        $('#sysUserName').val(sysUser.name);
                        $('#sysUserPhone').val(sysUser.phone);
                        $('#sysUserEmail').val(sysUser.email);
                        $('#sysUserJobRole').val(sysUser.jobRole);
                        $('#sysUserStatus').val(sysUser.status);
                        $('#sysUserPassword').val(''); // 清空密码字段
                        
                        // 打开模态框
                        $('#editSysUserModal').modal('show');
                    } else {
                        alert('获取班级管理员信息失败：' + (response.message || '未知错误'));
                    }
                },
                error: function(xhr) {
                    alert('请求失败：' + (xhr.responseJSON && xhr.responseJSON.message ? xhr.responseJSON.message : '服务器错误'));
                }
            });
        });
        $(document).on('click', '.delete-sysuser-btn', function() {
            var id = $(this).data('id');
            if (confirm('确定要删除该班级管理员吗？')) {
                $.ajax({
                    url: '${pageContext.request.contextPath}/sysuser/' + id,
                    type: 'DELETE',
                    success: function(response) {
                        if (response.success) {
                            alert('删除成功！');
                            loadSysUserList(1); // 重新加载班级管理员列表
                        } else {
                            alert('删除失败：' + (response.message || '未知错误'));
                        }
                    },
                    error: function(xhr) {
                        alert('请求失败：' + (xhr.responseJSON && xhr.responseJSON.message ? xhr.responseJSON.message : '服务器错误'));
                    }
                });
            }
        });
        $(document).on('click', '.toggle-sysuser-btn', function() {
            var id = $(this).data('id');
            var $btn = $(this);
            var currentStatus = $btn.text().trim();
            var newStatus = currentStatus === '启用' ? 'ENABLE' : 'DISABLE';
            var confirmText = currentStatus === '启用' ? '确定要启用该班级管理员吗？' : '确定要禁用该班级管理员吗？';
            
            if (confirm(confirmText)) {
                $.ajax({
                    url: '${pageContext.request.contextPath}/sysuser/update-status/' + id + '/' + newStatus,
                    type: 'GET',
                    success: function(response) {
                        if (response.success) {
                            alert('操作成功！');
                            loadSysUserList(1); // 重新加载班级管理员列表
                        } else {
                            alert('操作失败：' + (response.message || '未知错误'));
                        }
                    },
                    error: function(xhr) {
                        alert('请求失败：' + (xhr.responseJSON && xhr.responseJSON.message ? xhr.responseJSON.message : '服务器错误'));
                    }
                });
            }
        });

        // 编辑班级管理员表单提交
        $('#editSysUserForm').on('submit', function(e) {
            e.preventDefault();
            var formData = {
                id: $('#sysUserId').val(),
                sysUserNo: $('#sysUserNo').val(),
                name: $('#sysUserName').val(),
                phone: $('#sysUserPhone').val(),
                email: $('#sysUserEmail').val(),
                jobRole: $('#sysUserJobRole').val(),
                status: $('#sysUserStatus').val(),
                password: $('#sysUserPassword').val()
            };
            
            $.ajax({
                url: '${pageContext.request.contextPath}/sysuser/super-admin/update/per-info',
                type: 'POST',
                contentType: 'application/json',
                data: JSON.stringify(formData),
                success: function(response) {
                    if (response.success) {
                        $('#editSysUserModal').modal('hide');
                        alert('修改成功！');
                        loadSysUserList(1); // 重新加载班级管理员列表
                    } else {
                        alert('修改失败：' + (response.message || '未知错误'));
                    }
                },
                error: function(xhr) {
                    alert('请求失败：' + (xhr.responseJSON && xhr.responseJSON.message ? xhr.responseJSON.message : '服务器错误'));
                }
            });
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
                        $('#superAdminTableBody').html('<tr><td colspan="8">加载失败：' + (response.message || '未知错误') + '</td></tr>');
                    }
                },
                error: function(xhr) {
                    $('#superAdminTableBody').html('<tr><td colspan="8">请求失败</td></tr>');
                }
            });
        }
        function updateSuperAdminTable(superAdmins) {
            var tbody = $('#superAdminTableBody');
            tbody.empty();
            if (!superAdmins || superAdmins.length === 0) {
                tbody.append('<tr><td colspan="8" class="text-center">暂无数据</td></tr>');
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
                    '<td>' +
                    '<button class="btn btn-sm btn-primary me-2 edit-superadmin-btn" data-id="' + (admin.id || '') + '">修改</button>' +
                    '<button class="btn btn-sm btn-danger me-2 delete-superadmin-btn" data-id="' + (admin.id || '') + '">删除</button>' +
                    '<button class="btn btn-sm btn-warning  me-2 toggle-superadmin-btn" data-id="' + (admin.id || '') + '">' + toggleBtnText + '</button>' +
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
        
        // 超级管理员搜索框回车事件
        $('#superAdminSearch').on('keypress', function(e) {
            if (e.which === 13) { // Enter key
                superAdminPageNum = 1;
                loadSuperAdminList(superAdminPageNum);
            }
        });
        
        // 页面加载时自动加载第一页
        loadSuperAdminList(1);
        // 操作按钮事件
        $(document).on('click', '.edit-superadmin-btn', function() {
            var id = $(this).data('id');
            // 获取超级管理员信息并打开编辑模态框
            $.ajax({
                url: '${pageContext.request.contextPath}/super-admin/' + id,
                type: 'GET',
                success: function(response) {
                    if (response.success) {
                        var superAdmin = response.data;
                        // 填充表单数据
                        $('#superAdminId').val(superAdmin.id);
                        $('#superAdminName').val(superAdmin.name);
                        $('#superAdminEmail').val(superAdmin.email);
                        $('#superAdminEnabled').val(superAdmin.enabled);
                        $('#superAdminPassword').val(''); // 清空密码字段
                        
                        // 打开模态框
                        $('#editSuperAdminModal').modal('show');
                    } else {
                        alert('获取超级管理员信息失败：' + (response.message || '未知错误'));
                    }
                },
                error: function(xhr) {
                    alert('请求失败：' + (xhr.responseJSON && xhr.responseJSON.message ? xhr.responseJSON.message : '服务器错误'));
                }
            });
        });
        $(document).on('click', '.delete-superadmin-btn', function() {
            var id = $(this).data('id');
            if (confirm('确定要删除该超级管理员吗？')) {
                $.ajax({
                    url: '${pageContext.request.contextPath}/super-admin/delete/' + id,
                    type: 'DELETE',
                    success: function(response) {
                        if (response.success) {
                            alert('删除成功！');
                            loadSuperAdminList(1); // 重新加载超级管理员列表
                        } else {
                            alert('删除失败：' + (response.message || '未知错误'));
                        }
                    },
                    error: function(xhr) {
                        alert('请求失败：' + (xhr.responseJSON && xhr.responseJSON.message ? xhr.responseJSON.message : '服务器错误'));
                    }
                });
            }
        });
        $(document).on('click', '.toggle-superadmin-btn', function() {
            var id = $(this).data('id');
            var $btn = $(this);
            var superAdminCurrentStatus = $btn.text().trim();
            var superAdminNewEnabled = superAdminCurrentStatus === '启用' ? 1 : 0; // 启用按钮发送1，禁用按钮发送0
            var superAdminConfirmText = superAdminCurrentStatus === '启用' ? '确定要启用该超级管理员吗？' : '确定要禁用该超级管理员吗？';
            
            if (confirm(superAdminConfirmText)) {
                $.ajax({
                    url: '${pageContext.request.contextPath}/super-admin/update-status/' + id + '/' + superAdminNewEnabled,
                    type: 'GET',
                    success: function(response) {
                        if (response.success) {
                            alert('操作成功！');
                            loadSuperAdminList(1); // 重新加载超级管理员列表
                        } else {
                            alert('操作失败：' + (response.message || '未知错误'));
                        }
                    },
                    error: function(xhr) {
                        alert('请求失败：' + (xhr.responseJSON && xhr.responseJSON.message ? xhr.responseJSON.message : '服务器错误'));
                    }
                });
            }
        });

        // 编辑超级管理员表单提交
        $('#editSuperAdminForm').on('submit', function(e) {
            e.preventDefault();
            var formData = {
                id: $('#superAdminId').val(),
                name: $('#superAdminName').val(),
                email: $('#superAdminEmail').val(),
                password: $('#superAdminPassword').val(),
                enabled: $('#superAdminEnabled').val()
            };
            
            $.ajax({
                url: '${pageContext.request.contextPath}/super-admin/update/other-admin',
                type: 'POST',
                contentType: 'application/json',
                data: JSON.stringify(formData),
                success: function(response) {
                    if (response.success) {
                        $('#editSuperAdminModal').modal('hide');
                        alert('修改成功！');
                        loadSuperAdminList(1); // 重新加载超级管理员列表
                    } else {
                        alert('修改失败：' + (response.message || '未知错误'));
                    }
                },
                error: function(xhr) {
                    alert('请求失败：' + (xhr.responseJSON && xhr.responseJSON.message ? xhr.responseJSON.message : '服务器错误'));
                }
            });
        });

        // 批量导入用户信息功能
        var currentImportData = null; // 存储当前导入的数据
        
        // 文件选择和角色选择变化时启用/禁用导入按钮
        $('#excelFile, #userRoleSelect').on('change', function() {
            var file = $('#excelFile')[0].files[0];
            var insertUserRole = $('#userRoleSelect').val();
            $('#importBtn').prop('disabled', !file || !insertUserRole);
        });
        
        // 批量导入按钮点击事件
        $('#importBtn').on('click', function() {
            var file = $('#excelFile')[0].files[0];
            var insertUserRole = $('#userRoleSelect').val();
            
            if (!file || !insertUserRole) {
                alert('请选择文件和用户角色！');
                return;
            }
            
            // 检查文件大小（10MB限制）
            if (file.size > 10 * 1024 * 1024) {
                alert('文件大小不能超过10MB！');
                return;
            }
            
            // 检查文件格式
            var fileName = file.name.toLowerCase();
            if (!fileName.endsWith('.xls') && !fileName.endsWith('.xlsx')) {
                alert('请选择Excel文件（.xls或.xlsx格式）！');
                return;
            }
            
            // 显示加载状态
            var $btn = $(this);
            var originalText = $btn.html();
            $btn.prop('disabled', true).html('<i class="fas fa-spinner fa-spin"></i> 导入中...');
            
            // 创建FormData对象
            var formData = new FormData();
            formData.append('file', file);
            formData.append('insertUserRole', insertUserRole);
            
            // 发送AJAX请求
            $.ajax({
                url: '${pageContext.request.contextPath}/super-admin/import-batch',
                type: 'POST',
                data: formData,
                processData: false,
                contentType: false,
                success: function(response) {
                    $btn.prop('disabled', false).html(originalText);
                    
                    if (response.success) {
                        currentImportData = response.data;
                        showImportResult(response);
                    } else {
                        alert('导入失败：' + (response.message || '未知错误'));
                    }
                },
                error: function(xhr) {
                    $btn.prop('disabled', false).html(originalText);
                    alert('请求失败：' + (xhr.responseJSON && xhr.responseJSON.message ? xhr.responseJSON.message : '服务器错误'));
                }
            });
        });
        
        // 显示导入结果
        function showImportResult(response) {
            var data = response.data;
            var totalCount = data.totalCount || 0;
            var validCount = data.validCount || 0;
            var invalidList = data.invalidList || [];
            var insertUserRole = $('#userRoleSelect').val(); // 获取当前选择的角色
            
            // 更新结果消息
            var message = '导入完成！总计：' + totalCount + '条，成功：' + validCount + '条，失败：' + invalidList.length + '条';
            $('#importResultMessage').text(message);
            
            if (invalidList.length > 0) {
                // 有错误数据，显示模态框
                showErrorDataInTable(invalidList, insertUserRole);
                $('#errorDataModal').modal('show');
            } else {
                // 没有错误数据，显示成功消息
                alert('导入成功！' + message);
                // 刷新对应的列表
                refreshUserList(insertUserRole);
            }
        }
        
        // 在HTML表格中显示错误数据
        function showErrorDataInTable(invalidList, insertUserRole) {
            var headers = getHeadersByRole(insertUserRole);
            
            // 生成表头
            var headerHtml = '';
            headers.forEach(function(header) {
                headerHtml += '<th>' + header + '</th>';
            });
            $('#errorDataTableHeader').html(headerHtml);
            
            // 生成数据行
            var tbody = $('#errorDataTableBody');
            tbody.empty();
            
            if (!invalidList || invalidList.length === 0) {
                tbody.append('<tr><td colspan="' + headers.length + '" class="text-center">暂无错误数据</td></tr>');
                return;
            }
            
            invalidList.forEach(function(item) {
                var dataObj = getDataObjectByRole(item, insertUserRole);
                var rowHtml = '';
                
                headers.forEach(function(header) {
                    if (header === '错误信息') {
                        var errorText = item.errors ? item.errors.join('; ') : '';
                        rowHtml += '<td class="text-danger bg-light">' + errorText + '</td>';
                    } else {
                        var value = getFieldValue(dataObj, header, insertUserRole) || '';
                        rowHtml += '<td class="bg-warning-light">' + value + '</td>';
                    }
                });
                
                tbody.append('<tr>' + rowHtml + '</tr>');
            });
        }
        
        // 根据角色获取表头
        function getHeadersByRole(insertUserRole) {
            switch (insertUserRole) {
                case 'student':
                    return ['学号', '密码', '姓名', '学院', '班级', '宿舍', '电话', '邮箱', '父亲姓名', '父亲电话', '母亲姓名', '母亲电话', '错误信息'];
                case 'dormitorymanager':
                    return ['工号', '密码', '姓名', '电话', '邮箱', '负责楼栋', '错误信息'];
                case 'systemuser':
                    return ['工号', '密码', '姓名', '电话', '邮箱', '职位角色', '错误信息'];
                case 'superadmin':
                    return ['密码', '姓名', '邮箱', '错误信息'];
                default:
                    return [];
            }
        }
        
        // 根据角色获取数据对象
        function getDataObjectByRole(item, insertUserRole) {
            switch (insertUserRole) {
                case 'student':
                    return item.studentExcelDTO;
                case 'dormitorymanager':
                    return item.dormitoryManagerExcelDTO;
                case 'systemuser':
                    return item.sysUserExcelDTO;
                case 'superadmin':
                    return item.superAdminExcelDTO;
                default:
                    return {};
            }
        }
        
        // 根据字段名获取值
        function getFieldValue(dataObj, fieldName, insertUserRole) {
            if (!dataObj) return '';
            
            var fieldMap = {
                '学号': 'studentNo',
                '密码': 'password',
                '姓名': 'name',
                '学院': 'college',
                '班级': 'className',
                '宿舍': 'dormitory',
                '电话': 'phone',
                '邮箱': 'email',
                '父亲姓名': 'fatherName',
                '父亲电话': 'fatherPhone',
                '母亲姓名': 'motherName',
                '母亲电话': 'motherPhone',
                '工号': insertUserRole === 'dormitorymanager' ? 'managerId' : 'sysUserNo',
                '负责楼栋': 'building',
                '职位角色': 'jobRole'
            };
            
            var field = fieldMap[fieldName];
            return field ? dataObj[field] : '';
        }
        
        // 刷新用户列表
        function refreshUserList(insertUserRole) {
            switch (insertUserRole) {
                case 'student':
                    loadStudentList(1);
                    break;
                case 'dormitorymanager':
                    loadDormManList(1); 
                    break;
                case 'systemuser':
                    loadSysUserList(1);
                    break;
                case 'superadmin':
                    loadSuperAdminList(1);
                    break;
            }
        }
        
        // 下载错误数据
        $('#downloadErrorBtn').on('click', function() {
            if (!currentImportData || !currentImportData.invalidList) {
                alert('没有错误数据可下载！');
                return;
            }
            
            // 获取当前选择的用户角色
            var insertUserRole = $('#userRoleSelect').val();
            if (!insertUserRole) {
                alert('请先选择用户角色！');
                return;
            }
            
            // 构造请求数据
            var requestData = {
                invalidList: currentImportData.invalidList,
                insertUserRole: insertUserRole
            };
            
            // 创建下载链接
            var downloadUrl = '${pageContext.request.contextPath}/super-admin/download-error-data';
            
            // 创建临时表单进行POST下载
            var form = document.createElement('form');
            form.method = 'POST';
            form.action = downloadUrl;
            form.target = '_blank';
            
            // 添加请求数据
            var dataInput = document.createElement('input');
            dataInput.type = 'hidden';
            dataInput.name = 'requestData';
            dataInput.value = JSON.stringify(requestData);
            form.appendChild(dataInput);
            
            // 添加CSRF token（如果有的话）
            var csrfToken = $('meta[name="_csrf"]').attr('content');
            if (csrfToken) {
                var csrfInput = document.createElement('input');
                csrfInput.type = 'hidden';
                csrfInput.name = '_csrf';
                csrfInput.value = csrfToken;
                form.appendChild(csrfInput);
            }
            
            // 提交表单
            document.body.appendChild(form);
            form.submit();
            document.body.removeChild(form);
        });
        
        // 重新导入
        $('#reimportBtn').on('click', function() {
            $('#errorDataModal').modal('hide');
            // 清空文件选择
            $('#excelFile').val('');
            $('#importBtn').prop('disabled', true);
        });

        // 搜索按钮
        $('#searchSuperAdminBtn').on('click', function() {
            superAdminPageNum = 1;
            loadSuperAdminList(superAdminPageNum);
        });

        // 宿管列表相关
        var dormManPageNum = 1;
        var dormManPageSize = 10;
        function loadDormManList(pageNum) {
            pageNum = pageNum || 1;
            var nameLike = $('#dormManSearch').val().trim();
            $.ajax({
                url: '${pageContext.request.contextPath}/dorman/page-list',
                type: 'GET',
                data: {
                    pageNum: pageNum,
                    pageSize: dormManPageSize,
                    nameLike: nameLike
                },
                success: function(response) {
                    if (response.success) {
                        var pageData = response.data && response.data.data ? response.data : response.data;
                        var dormMans = pageData.data || [];
                        var total = pageData.total || 0;
                        var pageNum = pageData.pageNum || 1;
                        var pageSize = pageData.pageSize || 10;
                        updateDormManTable(dormMans);
                        updateDormManPagination(pageNum, pageSize, total);
                    } else {
                        $('#dormManTableBody').html('<tr><td colspan="11">加载失败：' + (response.message || '未知错误') + '</td></tr>');
                    }
                },
                error: function(xhr) {
                    $('#dormManTableBody').html('<tr><td colspan="11">请求失败</td></tr>');
                }
            });
        }
        
        function updateDormManTable(dormMans) {
            var tbody = $('#dormManTableBody');
            tbody.empty();
            if (!dormMans || dormMans.length === 0) {
                tbody.append('<tr><td colspan="11" class="text-center">暂无数据</td></tr>');
                return;
            }
            dormMans.forEach(function(dormMan) {
                var statusBadge = dormMan.status === 'ON_DUTY' ? '<span class="badge bg-success">在职</span>' : '<span class="badge bg-secondary">离职</span>';
                var toggleBtnText = dormMan.status === 'ON_DUTY' ? '设为离职' : '设为在职';
                var newStatus = dormMan.status === 'ON_DUTY' ? 'OFF_DUTY' : 'ON_DUTY';
                
                tbody.append('<tr>' +
                    '<td>' + (dormMan.id || '-') + '</td>' +
                    '<td>' + (dormMan.managerId || '-') + '</td>' +
                    '<td>' + (dormMan.name || '-') + '</td>' +
                    '<td>' + (dormMan.building || '-') + '</td>' +
                    '<td>' + (dormMan.phone || '-') + '</td>' +
                    '<td>' + (dormMan.email || '-') + '</td>' +
                    '<td>' + statusBadge + '</td>' +
                    '<td>' + (dormMan.createTime ? formatDate(dormMan.createTime) : '-') + '</td>' +
                    '<td>' + (dormMan.updateTime ? formatDate(dormMan.updateTime) : '-') + '</td>' +
                    '<td>' + (dormMan.lastLoginTime ? formatDate(dormMan.lastLoginTime) : '-') + '</td>' +
                    '<td>' +
                    '<button class="btn btn-sm btn-primary me-2 edit-dormman-btn" data-id="' + (dormMan.id || '') + '">修改</button>' +
                    '<button class="btn btn-sm btn-danger me-2 delete-dormman-btn" data-id="' + (dormMan.id || '') + '">删除</button>' +
                    '<button class="btn btn-sm btn-warning toggle-dormman-btn" data-id="' + (dormMan.id || '') + '" data-status="' + newStatus + '">' + toggleBtnText + '</button>' +
                    '</td>' +
                    '</tr>');
            });
        }
        
        function updateDormManPagination(pageNum, pageSize, total) {
            var start = (total === 0) ? 0 : ((pageNum - 1) * pageSize + 1);
            var end = Math.min(pageNum * pageSize, total);
            $('#dormManStartRecord').text(start);
            $('#dormManEndRecord').text(end);
            $('#dormManTotalRecords').text(total);
            var totalPages = Math.ceil(total / pageSize);
            var pagin = $('#dormManPagination');
            pagin.empty();
            if (totalPages <= 1) return;
            pagin.append('<li class="page-item' + (pageNum === 1 ? ' disabled' : '') + '"><a class="page-link" href="#" data-page="' + (pageNum - 1) + '">上一页</a></li>');
            for (var i = 1; i <= totalPages; i++) {
                pagin.append('<li class="page-item' + (i === pageNum ? ' active' : '') + '"><a class="page-link" href="#" data-page="' + i + '">' + i + '</a></li>');
            }
            pagin.append('<li class="page-item' + (pageNum === totalPages ? ' disabled' : '') + '"><a class="page-link" href="#" data-page="' + (pageNum + 1) + '">下一页</a></li>');
        }
        
        // 宿管分页点击
        $(document).on('click', '#dormManPagination .page-link', function(e) {
            e.preventDefault();
            var page = parseInt($(this).data('page'));
            if (!isNaN(page)) {
                dormManPageNum = page;
                loadDormManList(dormManPageNum);
            }
        });
        
        // 宿管搜索按钮
        $('#searchDormManBtn').on('click', function() {
            dormManPageNum = 1;
            loadDormManList(dormManPageNum);
        });
        
        // 宿管搜索框回车事件
        $('#dormManSearch').on('keypress', function(e) {
            if (e.which === 13) { // Enter key
                dormManPageNum = 1;
                loadDormManList(dormManPageNum);
            }
        });
        
        // 宿管操作按钮事件
        $(document).on('click', '.edit-dormman-btn', function() {
            var id = $(this).data('id');
            // 获取宿管信息并打开编辑模态框
            $.ajax({
                url: '${pageContext.request.contextPath}/dorman/search',
                type: 'POST',
                contentType: 'application/json',
                data: JSON.stringify({id: id}),
                success: function(response) {
                    if (response && response.length > 0) {
                        var dormMan = response[0]; // 获取第一个匹配的宿管
                        // 填充表单数据
                        $('#dormManId').val(dormMan.id);
                        $('#dormManManagerId').val(dormMan.managerId);
                        $('#dormManName').val(dormMan.name);
                        $('#dormManBuilding').val(dormMan.building);
                        $('#dormManPhone').val(dormMan.phone);
                        $('#dormManEmail').val(dormMan.email);
                        $('#dormManStatus').val(dormMan.status);
                        $('#dormManPassword').val(''); // 清空密码字段
                        
                        // 打开模态框
                        $('#editDormManModal').modal('show');
                    } else {
                        alert('获取宿管信息失败：未找到对应的宿管信息');
                    }
                },
                error: function(xhr) {
                    alert('请求失败：' + (xhr.responseJSON && xhr.responseJSON.message ? xhr.responseJSON.message : '服务器错误'));
                }
            });
        });
        
        $(document).on('click', '.delete-dormman-btn', function() {
            var id = $(this).data('id');
            if (confirm('确定要删除该宿管吗？')) {
                $.ajax({
                    url: '${pageContext.request.contextPath}/dorman/delete/' + id,
                    type: 'DELETE',
                    success: function(response) {
                        if (response.success) {
                            alert('删除成功！');
                            loadDormManList(1); // 重新加载宿管列表
                        } else {
                            alert('删除失败：' + (response.message || '未知错误'));
                        }
                    },
                    error: function(xhr) {
                        alert('请求失败：' + (xhr.responseJSON && xhr.responseJSON.message ? xhr.responseJSON.message : '服务器错误'));
                    }
                });
            }
        });
        
        $(document).on('click', '.toggle-dormman-btn', function() {
            var id = $(this).data('id');
            var status = $(this).data('status');
            var confirmText = status === 'ON_DUTY' ? '确定要将该宿管设为在职吗？' : '确定要将该宿管设为离职吗？';
            
            if (confirm(confirmText)) {
                $.ajax({
                    url: '${pageContext.request.contextPath}/dorman/update-status/' + id + '/' + status,
                    type: 'GET',
                    success: function(response) {
                        if (response.success) {
                            alert('操作成功！');
                            loadDormManList(1); // 重新加载宿管列表
                        } else {
                            alert('操作失败：' + (response.message || '未知错误'));
                        }
                    },
                    error: function(xhr) {
                        alert('请求失败：' + (xhr.responseJSON && xhr.responseJSON.message ? xhr.responseJSON.message : '服务器错误'));
                    }
                });
            }
        });

        // 编辑宿管表单提交
        $('#editDormManForm').on('submit', function(e) {
            e.preventDefault();
            var formData = {
                id: $('#dormManId').val(),
                managerId: $('#dormManManagerId').val(),
                name: $('#dormManName').val(),
                building: $('#dormManBuilding').val(),
                phone: $('#dormManPhone').val(),
                email: $('#dormManEmail').val(),
                status: $('#dormManStatus').val(),
                password: $('#dormManPassword').val()
            };
            
            $.ajax({
                url: '${pageContext.request.contextPath}/dorman/super-admin/update/per-info',
                type: 'POST',
                contentType: 'application/json',
                data: JSON.stringify(formData),
                success: function(response) {
                    if (response.success) {
                        $('#editDormManModal').modal('hide');
                        alert('修改成功！');
                        loadDormManList(1); // 重新加载宿管列表
                    } else {
                        alert('修改失败：' + (response.message || '未知错误'));
                    }
                },
                error: function(xhr) {
                    alert('请求失败：' + (xhr.responseJSON && xhr.responseJSON.message ? xhr.responseJSON.message : '服务器错误'));
                }
            });
        });

        // 页面加载时自动加载第一页
        loadDormManList(1);
    });
</script>

<!-- 非Ajax形式请求的错误弹框提示 -->
<c:if test="${not empty sessionScope.errorMsg}">
    <script>
        alert("${fn:escapeXml(sessionScope.errorMsg)}");
    </script>
    <c:remove var="errorMsg" scope="session" />
</c:if>

<!-- 错误数据展示模态框 -->
<div class="modal fade" id="errorDataModal" tabindex="-1" aria-labelledby="errorDataModalLabel" aria-hidden="true">
    <div class="modal-dialog modal-xl">
        <div class="modal-content">
            <div class="modal-header">
                <h5 class="modal-title" id="errorDataModalLabel">导入错误数据详情</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body">
                <div class="alert alert-info">
                    <strong>导入结果：</strong>
                    <span id="importResultMessage"></span>
                </div>
                <div class="mb-3">
                    <button type="button" class="btn btn-success btn-sm" id="downloadErrorBtn">
                        <i class="fas fa-download"></i> 下载错误数据
                    </button>
                    <button type="button" class="btn btn-primary btn-sm" id="reimportBtn">
                        <i class="fas fa-upload"></i> 重新导入
                    </button>
                </div>
                <div class="table-responsive" style="max-height: 500px; overflow-y: auto;">
                    <table class="table table-bordered table-hover" id="errorDataTable">
                        <thead class="table-light sticky-top">
                            <tr id="errorDataTableHeader">
                                <!-- 表头将通过JavaScript动态生成 -->
                            </tr>
                        </thead>
                        <tbody id="errorDataTableBody">
                            <!-- 数据行将通过JavaScript动态生成 -->
                        </tbody>
                    </table>
                </div>
            </div>
            <div class="modal-footer">
                <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">关闭</button>
            </div>
        </div>
    </div>
</div>

<div id="univer-container" style="height: 500px; border: 1px solid #dee2e6; border-radius: 0.375rem;"></div>



</body>
</html> 