<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<!DOCTYPE html>
<html>
<head>
    <title>学生晚归预警系统 - 学生主页</title>
    <!-- 引入 CSS 文件 -->
    <link href="${pageContext.request.contextPath}/static/css/tailwind.min.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/static/css/fontawesome.min.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/static/css/bootstrap.min.css" rel="stylesheet">
    
    <!-- 引入 JS 文件 -->
    <script src="${pageContext.request.contextPath}/static/js/jquery.min.js"></script>
    <script src="${pageContext.request.contextPath}/static/js/popper.min.js"></script>
    <script src="${pageContext.request.contextPath}/static/js/bootstrap.bundle.min.js"></script>

    <!-- 引入验证脚本 -->
    <script src="${pageContext.request.contextPath}/static/js/student-validation.js"></script>

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
        .portal-card {
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
        .status-processing {
            background-color: #E0E7FF;
            color: #3730A3;
        }
        .status-processed {
            background-color: #D1FAE5;
            color: #065F46;
        }
        .status-rejected {
            background-color: #FEE2E2;
            color: #991B1B;
        }
        .tab-active {
            border-bottom: 2px solid #4F46E5;
            color: #4F46E5;
        }
        .notification-system {
            border-left-color: #3B82F6;  /* blue-500 */
        }
        .notification-late-return {
            border-left-color: #EAB308;  /* yellow-500 */
        }
        .notification-warning {
            border-left-color: #EF4444;  /* red-500 */
        }
        @keyframes spin {
            to { transform: rotate(360deg); }
        }
        .animate-spin {
            animation: spin 1s linear infinite;
        }
        .cursor-not-allowed {
            cursor: not-allowed;
        }
        
        /* 禁用状态的按钮样式 */
        button:disabled {
            opacity: 0.5;
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
                    <span class="text-gray-600">欢迎，${sessionScope.user.name}（学生）</span>
                    <button class="text-sm text-blue-600 hover:text-blue-800" data-bs-toggle="modal" data-bs-target="#editProfileModal">修改个人信息</button>
                    <button class="text-sm text-red-600 hover:text-red-800" onclick="handleLogout()">退出登录</button>
                </div>
            </div>
        </nav>

        <!-- 修改个人信息模态框 -->
        <div class="modal fade" id="editProfileModal" tabindex="-1" aria-labelledby="editProfileModalLabel" aria-hidden="true">
            <div class="modal-dialog modal-lg">
                <div class="modal-content">
                    <div class="modal-header">
                        <h5 class="modal-title" id="editProfileModalLabel">修改个人信息</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                    </div>
                    <div class="modal-body">
                        <form id="editProfileForm">
                            <div class="row mb-3">
                                <div class="col-md-6">
                                    <label for="studentNo" class="form-label">学号</label>
                                    <input type="text" class="form-control" id="studentNo" name="studentNo" required>
                                </div>
                                <div class="col-md-6">
                                    <label for="name" class="form-label">姓名</label>
                                    <input type="text" class="form-control" id="name" name="name" value="${sessionScope.user.name}" required>
                                </div>
                            </div>
                            <div class="row mb-3">
                                <div class="col-md-6">
                                    <label for="college" class="form-label">学院</label>
                                    <input type="text" class="form-control" id="college" name="college" required>
                                </div>
                                <div class="col-md-6">
                                    <label for="className" class="form-label">班级</label>
                                    <input type="text" class="form-control" id="className" name="className" required>
                                </div>
                            </div>
                            <div class="row mb-3">
                                <div class="col-md-6">
                                    <label for="dormitory" class="form-label">宿舍号</label>
                                    <input type="text" class="form-control" id="dormitory" name="dormitory" required>
                                </div>
                                <div class="col-md-6">
                                    <label for="phone" class="form-label">联系电话</label>
                                    <input type="tel" class="form-control" id="phone" name="phone" required>
                                </div>
                            </div>
                            <div class="row mb-3">
                                <div class="col-md-6">
                                    <label for="email" class="form-label">电子邮箱</label>
                                    <input type="email" class="form-control" id="email" name="email" value="${sessionScope.user.email}" required>
                                </div>
                                <div class="col-md-6">
                                    <label for="password" class="form-label">新密码</label>
                                    <input type="password" class="form-control" id="password" name="password" placeholder="不修改请留空">
                                </div>
                            </div>
                            <div class="row mb-3">
                                <div class="col-md-6">
                                    <label for="fatherName" class="form-label">父亲姓名</label>
                                    <input type="text" class="form-control" id="fatherName" name="fatherName" required>
                                </div>
                                <div class="col-md-6">
                                    <label for="fatherPhone" class="form-label">父亲电话</label>
                                    <input type="tel" class="form-control" id="fatherPhone" name="fatherPhone" required>
                                </div>
                            </div>
                            <div class="row mb-3">
                                <div class="col-md-6">
                                    <label for="motherName" class="form-label">母亲姓名</label>
                                    <input type="text" class="form-control" id="motherName" name="motherName" required>
                                </div>
                                <div class="col-md-6">
                                    <label for="motherPhone" class="form-label">母亲电话</label>
                                    <input type="tel" class="form-control" id="motherPhone" name="motherPhone" required>
                                </div>
                            </div>
                        </form>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">取消</button>
                        <button type="button" class="btn btn-primary" id="saveProfileBtn">保存</button>
                    </div>
                </div>
            </div>
        </div>

        <!-- 页面内容 -->
        <div class="container mx-auto">
            <!-- 学生信息卡片 -->
            <div class="portal-card p-6 mb-6">
                <div class="flex flex-col md:flex-row md:items-center md:justify-between">
                    <div class="mb-4 md:mb-0">
                        <h2 class="text-xl font-bold text-gray-800">个人信息</h2>
                        <p class="text-gray-600 mt-1">学号：${sessionScope.user.studentNo} | ${sessionScope.user.college} | ${sessionScope.user.dormitory}</p>
                    </div>
                    <div class="flex space-x-4">
                        <div class="text-center">
                            <p class="text-2xl font-bold text-indigo-600" id="monthlyCount">-</p>
                            <p class="text-sm text-gray-500">本月晚归次数</p>
                        </div>
                        <div class="text-center">
                            <p class="text-2xl font-bold text-green-600" id="processedCount">-</p>
                            <p class="text-sm text-gray-500">已处理</p>
                        </div>
                        <div class="text-center">
                            <p class="text-2xl font-bold text-yellow-600" id="pendingCount">-</p>
                            <p class="text-sm text-gray-500">待处理</p>
                        </div>
                    </div>
                </div>
            </div>

            <!-- 通知消息 -->
            <div class="portal-card p-6 mb-6">
                <div class="flex justify-between items-center mb-4">
                    <h3 class="text-lg font-medium text-gray-900">通知消息</h3>
                    <div class="flex items-center space-x-3">
                        <select id="notificationStatusFilter" class="rounded-md border-gray-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500 text-sm">
                            <option value="ALL">全部</option>
                            <option value="UNREAD">未读</option>
                            <option value="READ">已读</option>
                        </select>
                        
                    </div>
                
                </div>
                <div class="space-y-4" id="notificationList">
                    <!-- 通知内容将通过 JavaScript 动态加载 -->
                </div>
                <!-- 添加加载状态提示 -->
                <div id="notificationLoading" class="text-center py-4 hidden">
                    <div class="inline-block animate-spin rounded-full h-6 w-6 border-b-2 border-gray-900"></div>
                    <span class="ml-2 text-gray-600">加载中...</span>
                </div>
            </div>

            <!-- 一键报警区域 -->
            <div class="portal-card p-6 mb-6" id="alarm-section">
                <div class="flex items-center space-x-4 mb-4">
                    <button id="normal-alarm-btn"
                            class="bg-blue-500 hover:bg-blue-700 text-white font-bold py-2 px-4 rounded">
                        普通报警
                    </button>
                    <button id="emergency-alarm-btn"
                            class="bg-red-500 hover:bg-red-700 text-white font-bold py-2 px-4 rounded">
                        紧急报警
                    </button>
                    <button id="cancel-alarm-btn"
                            class="bg-gray-500 hover:bg-gray-700 text-white font-bold py-2 px-4 rounded hidden">
                        取消报警
                    </button>
                </div>
                <div id="map-container" style="width:100%;height:300px;"></div>
                <div id="location-info" class="mt-2 text-gray-600"></div>
            </div>

            <!-- 音视频采集预览区域 -->
            <div class="portal-card p-6 mb-6">
                <h3 class="text-lg font-medium text-gray-900 mb-4">录像录音区域</h3>
                <div class="flex justify-center">
                    <video id="videoPreview" autoplay muted style="max-width: 100%; height: auto; background: #000; border-radius: 0.5rem; border: 1px solid #e5e7eb;"></video>
                </div>
            </div>


            <!-- 标签页导航 -->
            <div class="border-b border-gray-200 mb-6">
                <nav class="-mb-px flex space-x-8">
                    <a href="#" class="tab-active py-4 px-1 font-medium text-sm">
                        
                    </a>
                    <a href="#" class="border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300 py-4 px-1 font-medium text-sm">
                        
                    </a>
                    <a href="#" class="border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300 py-4 px-1 font-medium text-sm">
                        通知消息
                    </a>
                </nav>
            </div>

            <!-- 晚归记录列表 -->
            <div class="portal-card p-6 mb-6">
                <div class="flex justify-between items-center mb-4">
                    <h3 class="text-lg font-medium text-gray-900">晚归记录列表</h3>
                    <div class="flex space-x-2">
                        <select id="statusFilter" class="rounded-md border-gray-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500 text-sm">
                            <option value="all">全部记录</option>
                            <option value="PENDING">待处理</option>
                            <option value="PROCESSING">处理中</option>
                            <option value="FINISHED">已完成</option>
                        </select>
                        <button id="filterBtn" class="px-3 py-1 bg-indigo-600 text-white rounded-md hover:bg-indigo-700 text-sm">
                            筛选
                        </button>
                    </div>
                </div>

                <div class="overflow-x-auto">
                    <table class="min-w-full divide-y divide-gray-200">
                        <thead class="bg-gray-50">
                            <tr>
                                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                    日期
                                </th>
                                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                    晚归时间
                                </th>
                                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                    状态
                                </th>
                                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                    处理结果
                                </th>
                                <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                    处理备注
                                </th>
                            </tr>
                        </thead>
                        <tbody id="lateReturnList" class="bg-white divide-y divide-gray-200">
                            <!-- 数据将通过 JS 动态渲染 -->
                        </tbody>
                    </table>
                </div>

                <!-- 分页 -->
                <div class="mt-4 flex items-center justify-between">
                    <div class="flex-1 flex justify-between sm:hidden">
                        <button id="prevPageMobile" class="relative inline-flex items-center px-4 py-2 border border-gray-300 text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50">
                            上一页
                        </button>
                        <button id="nextPageMobile" class="ml-3 relative inline-flex items-center px-4 py-2 border border-gray-300 text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50">
                            下一页
                        </button>
                    </div>
                    <div class="hidden sm:flex-1 sm:flex sm:items-center sm:justify-between">
                        <div>
                            <p class="text-sm text-gray-700">
                                显示第 <span id="startRecord" class="font-medium">1</span> 到 <span id="endRecord" class="font-medium">0</span> 条，共 <span id="totalRecords" class="font-medium">0</span> 条记录
                            </p>
                        </div>
                        <div>
                            <nav class="relative z-0 inline-flex rounded-md shadow-sm -space-x-px" aria-label="Pagination">
                                <button id="prevPage" class="relative inline-flex items-center px-2 py-2 rounded-l-md border border-gray-300 bg-white text-sm font-medium text-gray-500 hover:bg-gray-50">
                                    <span class="sr-only">上一页</span>
                                    <i class="fas fa-chevron-left"></i>
                                </button>
                                <span id="currentPage" class="relative inline-flex items-center px-4 py-2 border border-gray-300 bg-white text-sm font-medium text-gray-700">
                                    1
                                </span>
                                <button id="nextPage" class="relative inline-flex items-center px-2 py-2 rounded-r-md border border-gray-300 bg-white text-sm font-medium text-gray-500 hover:bg-gray-50">
                                    <span class="sr-only">下一页</span>
                                    <i class="fas fa-chevron-right"></i>
                                </button>
                            </nav>
                        </div>
                    </div>
                </div>
            </div>

            <!-- 晚归申请和说明表单 -->
            <div class="portal-card p-6 mb-6">
                <div class="border-b border-gray-200 mb-6">
                    <nav class="-mb-px flex space-x-8">
                        <a href="#" class="tab-active py-4 px-1 font-medium text-sm" data-form="application">
                            晚归申请报备
                        </a>
                        <a href="#" class="border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300 py-4 px-1 font-medium text-sm" data-form="explanation">
                            晚归情况说明
                        </a>
                    </nav>
                </div>

                <!-- 晚归申请报备表单 -->
                <form class="space-y-6" id="applicationForm" style="display: block;">
                    <div>
                        <label class="block text-sm font-medium text-gray-700 mb-1">预计返校日期时间</label>
                        <div class="relative">
                            <input type="datetime-local" id="expectedReturnTime" name="expectedReturnTime"
                                   class="peer w-full rounded-md border-gray-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500 pt-6"
                                   required step="60" />
                        </div>
                    </div>
                    <div>
                        <label class="block text-sm font-medium text-gray-700 mb-1">晚归原因</label>
                        <select name="reason" class="w-full rounded-md border-gray-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500" required>
                            <option value="">请选择晚归原因</option>
                            <option value="学习/实验">学习/实验</option>
                            <option value="社团活动">社团活动</option>
                            <option value="就医">就医</option>
                            <option value="其他原因">其他原因</option>
                        </select>
                    </div>
                    <div>
                        <label class="block text-sm font-medium text-gray-700 mb-1">出行目的地</label>
                        <input type="text" name="destination" class="w-full rounded-md border-gray-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500" required>
                    </div>
                    <div>
                        <label class="block text-sm font-medium text-gray-700 mb-1">详细说明</label>
                        <textarea name="description" class="w-full rounded-md border-gray-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500" rows="4" placeholder="请详细说明晚归原因..." required></textarea>
                    </div>
                    <div>
                        <label class="block text-sm font-medium text-gray-700 mb-1">证明材料</label>
                        <div class="mt-1 flex justify-center px-6 pt-5 pb-6 border-2 border-gray-300 border-dashed rounded-md" id="application-upload-area">
                            <div class="space-y-1 text-center" id="application-upload-placeholder">
                                <i class="fas fa-cloud-upload-alt text-gray-400 text-3xl mb-3"></i>
                                <div class="flex text-sm text-gray-600">
                                    <label for="application-file-upload" class="relative cursor-pointer bg-white rounded-md font-medium text-indigo-600 hover:text-indigo-500 focus-within:outline-none focus-within:ring-2 focus-within:ring-offset-2 focus-within:ring-indigo-500">
                                        <span>上传文件</span>
                                        <input id="application-file-upload" name="file" type="file" class="sr-only" accept=".jpg,.jpeg,.png,.pdf">
                                    </label>
                                    <p class="pl-1">或拖放文件到此处</p>
                                </div>
                                <p class="text-xs text-gray-500">支持 JPG, PNG, PDF 格式，最大 5MB</p>
                            </div>
                            <div class="hidden" id="application-file-preview">
                                <div class="flex items-center space-x-4">
                                    <div class="flex-shrink-0">
                                        <img id="application-file-thumbnail" class="h-24 w-24 object-cover rounded mx-auto" src="" alt="" style="display:none;">
                                        <i id="application-file-icon" class="fas fa-file-pdf text-5xl text-red-500 hidden" style="display:none;"></i>
                                    </div>
                                    <div class="flex-1 min-w-0">
                                        <p id="application-file-name" class="text-sm font-medium text-gray-900 truncate"></p>
                                        <p id="application-file-size" class="text-sm text-gray-500"></p>
                                    </div>
                                    <button type="button" class="text-red-600 hover:text-red-800" onclick="removeFile('application')">
                                        <i class="fas fa-times"></i>
                                    </button>
                                </div>
                            </div>
                        </div>
                    </div>
                    <div class="flex justify-end">
                        <button type="submit" class="px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700">
                            提交申请
                        </button>
                    </div>
                </form>

                <!-- 晚归情况说明表单 -->
                <form class="space-y-6" id="explanationForm" style="display: none;">
                    <div>
                        <label class="block text-sm font-medium text-gray-700 mb-1">晚归日期时间</label>
                        <div class="relative">
                            <input type="datetime-local" name="lateReturnDate" class="w-full rounded-md border-gray-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500" required step="60" />
                        </div>
                    </div>
                    <div>
                        <label class="block text-sm font-medium text-gray-700 mb-1">晚归原因</label>
                        <select name="reason" class="w-full rounded-md border-gray-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500" required>
                            <option value="">请选择晚归原因</option>
                            <option value="学习/实验">学习/实验</option>
                            <option value="社团活动">社团活动</option>
                            <option value="就医">就医</option>
                            <option value="其他原因">其他原因</option>
                        </select>
                    </div>
                    <div>
                        <label class="block text-sm font-medium text-gray-700 mb-1">详细说明</label>
                        <textarea name="description" class="w-full rounded-md border-gray-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500" rows="4" placeholder="请详细说明晚归原因..." required></textarea>
                    </div>
                    <div>
                        <label class="block text-sm font-medium text-gray-700 mb-1">证明材料</label>
                        <div class="mt-1 flex justify-center px-6 pt-5 pb-6 border-2 border-gray-300 border-dashed rounded-md" id="explanation-upload-area">
                            <div class="space-y-1 text-center" id="explanation-upload-placeholder">
                                <i class="fas fa-cloud-upload-alt text-gray-400 text-3xl mb-3"></i>
                                <div class="flex text-sm text-gray-600">
                                    <label for="explanation-file-upload" class="relative cursor-pointer bg-white rounded-md font-medium text-indigo-600 hover:text-indigo-500 focus-within:outline-none focus-within:ring-2 focus-within:ring-offset-2 focus-within:ring-indigo-500">
                                        <span>上传文件</span>
                                        <input id="explanation-file-upload" name="file" type="file" class="sr-only" accept=".jpg,.jpeg,.png,.pdf">
                                    </label>
                                    <p class="pl-1">或拖放文件到此处</p>
                                </div>
                                <p class="text-xs text-gray-500">支持 JPG, PNG, PDF 格式，最大 5MB</p>
                            </div>
                            <div class="hidden" id="explanation-file-preview">
                                <div class="flex items-center space-x-4">
                                    <div class="flex-shrink-0">
                                        <img id="explanation-file-thumbnail" class="h-24 w-24 object-cover rounded mx-auto" src="" alt="" style="display:none;">
                                        <i id="explanation-file-icon" class="fas fa-file-pdf text-5xl text-red-500 hidden" style="display:none;"></i>
                                    </div>
                                    <div class="flex-1 min-w-0">
                                        <p id="explanation-file-name" class="text-sm font-medium text-gray-900 truncate"></p>
                                        <p id="explanation-file-size" class="text-sm text-gray-500"></p>
                                    </div>
                                    <button type="button" class="text-red-600 hover:text-red-800" onclick="removeFile('explanation')">
                                        <i class="fas fa-times"></i>
                                    </button>
                                </div>
                            </div>
                        </div>
                    </div>
                    <div class="flex justify-end">
                        <button type="submit" class="px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700">
                            提交说明
                        </button>
                    </div>
                </form>
            </div>


        </div>
    </div>

    <!-- 晚归记录详情模态框 -->
    <div class="modal fade" id="lateReturnDetailModal" tabindex="-1" aria-labelledby="lateReturnDetailModalLabel" aria-hidden="true">
        <div class="modal-dialog modal-lg">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title" id="lateReturnDetailModalLabel">晚归记录详情</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <div class="modal-body">
                    <div class="space-y-4">
                        <!-- 晚归记录信息 -->
                        <div class="bg-gray-50 p-4 rounded-lg">
                            <h6 class="text-lg font-medium text-gray-900 mb-3">晚归记录信息</h6>
                            <div class="grid grid-cols-2 gap-4">
                                <div>
                                    <p class="text-sm text-gray-500">晚归时间</p>
                                    <p class="text-sm font-medium text-gray-900" id="detailLateTime"></p>
                                </div>
                                <div>
                                    <p class="text-sm text-gray-500">处理状态</p>
                                    <p class="text-sm font-medium text-gray-900" id="detailProcessStatus"></p>
                                </div>
                                <div>
                                    <p class="text-sm text-gray-500">处理结果</p>
                                    <p class="text-sm font-medium text-gray-900" id="detailProcessResult"></p>
                                </div>
                                <div>
                                    <p class="text-sm text-gray-500">处理备注</p>
                                    <p class="text-sm font-medium text-gray-900" id="detailProcessRemark"></p>
                                </div>
                            </div>
                        </div>

                        <!-- 晚归说明信息 -->
                        <div class="bg-gray-50 p-4 rounded-lg mt-4" id="explanationSection" style="display: none;">
                            <h6 class="text-lg font-medium text-gray-900 mb-3">晚归说明</h6>
                            <div class="grid grid-cols-2 gap-4">
                                <div>
                                    <p class="text-sm text-gray-500">提交时间</p>
                                    <p class="text-sm font-medium text-gray-900" id="detailSubmitTime"></p>
                                </div>
                                <div>
                                    <p class="text-sm text-gray-500">审核状态</p>
                                    <p class="text-sm font-medium text-gray-900" id="detailAuditStatus"></p>
                                </div>
                                <div class="col-span-2">
                                    <p class="text-sm text-gray-500">说明内容</p>
                                    <p class="text-sm font-medium text-gray-900" id="detailDescription"></p>
                                </div>
                                <div class="col-span-2">
                                    <p class="text-sm text-gray-500">审核备注</p>
                                    <p class="text-sm font-medium text-gray-900" id="detailAuditRemark"></p>
                                </div>
                            </div>
                        </div>

                    </div>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">关闭</button>
                </div>
            </div>
        </div>
    </div>



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
            // 初始化个人信息表单
            $('#studentNo').val('${sessionScope.user.studentNo}');
            $('#college').val('${sessionScope.user.college}');
            $('#className').val('${sessionScope.user.className}');
            $('#dormitory').val('${sessionScope.user.dormitory}');
            $('#phone').val('${sessionScope.user.phone}');
            $('#fatherName').val('${sessionScope.user.fatherName}');
            $('#fatherPhone').val('${sessionScope.user.fatherPhone}');
            $('#motherName').val('${sessionScope.user.motherName}');
            $('#motherPhone').val('${sessionScope.user.motherPhone}');

            // 保存个人信息
            $('#saveProfileBtn').click(function() {
                if (!validateStudentForm()) {
                    console.log('表单验证失败');
                    return;
                }
                
                // 将表单数据转换为 JSON 对象
                var formData = {};
                $('#editProfileForm').serializeArray().forEach(function(item) {
                    formData[item.name] = item.value;
                });
                
                $.ajax({
                    url: '${pageContext.request.contextPath}/student/update/per-info',
                    type: 'POST',
                    contentType: 'application/json',
                    data: JSON.stringify(formData),
                    success: function(response) {
                        if (response.success) {
                            alert('个人信息更新成功');
                            $('#editProfileModal').modal('hide');
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
                $('#editProfileModal').modal('hide');
            });

            // 右上角关闭按钮关闭模态框
            $('.btn-close').click(function() {
                $('#editProfileModal').modal('hide');
            });

            // 初始加载数据
            loadLateReturns();
            loadNotifications();

            // 筛选按钮点击事件
            $('#filterBtn').click(function() {
                loadLateReturns();
                loadStatistics();
            });

            // 分页按钮点击事件
            $('#prevPage, #prevPageMobile').click(function() {
                if (currentPage > 1) {
                    currentPage--;
                    loadLateReturns();
                }
            });

            $('#nextPage, #nextPageMobile').click(function() {
                if (currentPage < totalPages) {
                    currentPage++;
                    loadLateReturns();
                }
            });

            // 加载统计数据
            loadStatistics();
            
            // 表单切换
            $('[data-form]').click(function(e) {
                e.preventDefault();
                const formType = $(this).data('form');
                
                // 更新标签样式
                $('[data-form]').removeClass('tab-active').addClass('border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300');
                $(this).addClass('tab-active').removeClass('border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300');
                
                // 切换表单显示
                if (formType === 'application') {
                    $('#applicationForm').show();
                    $('#explanationForm').hide();
                } else {
                    $('#applicationForm').hide();
                    $('#explanationForm').show();
                }
            });

            // 晚归申请报备表单提交
            $('#applicationForm').submit(function(e) {
                e.preventDefault();
                
                const formData = new FormData(this);
                formData.append('studentNo', '${sessionScope.user.studentNo}');
                
                // 格式化时间，确保保留时分
                const expectedReturnTime = formData.get('expectedReturnTime');
                if (expectedReturnTime) {
                    // 将时间字符串转换为Date对象
                    const date = new Date(expectedReturnTime);
                    // 格式化为MySQL datetime格式 (YYYY-MM-DD HH:mm:ss)
                    const formattedDate = date.getFullYear() + '-' +
                        String(date.getMonth() + 1).padStart(2, '0') + '-' +
                        String(date.getDate()).padStart(2, '0') + ' ' +
                        String(date.getHours()).padStart(2, '0') + ':' +
                        String(date.getMinutes()).padStart(2, '0') + ':00';
                    
                    formData.set('expectedReturnTime', formattedDate);
                }
                
                $.ajax({
                    url: '${pageContext.request.contextPath}/application/add',
                    type: 'POST',
                    data: formData,
                    processData: false,
                    contentType: false,
                    success: function(response) {
                        if (response.success) {
                            alert('晚归申请提交成功');
                            $('#applicationForm')[0].reset();
                            // 重置文件预览
                            $('#application-file-preview').css('display', 'none');
                            $('#application-upload-placeholder').css('display', 'block');
                            // 清除文件预览内容
                            $('#application-file-thumbnail').attr('src', '').css('display', 'none');
                            $('#application-file-icon').css('display', 'none');
                            $('#application-file-name').text('');
                            $('#application-file-size').text('');
                            removeFile('application');
                        } else {
                            alert(response.message || '提交失败，请重试');
                        }
                    },
                    error: function() {
                        alert('提交失败，请重试');
                    }
                });
            });

            // 晚归情况说明表单提交
            $('#explanationForm').submit(function(e) {
                e.preventDefault();
                
                const formData = new FormData(this);
                formData.append('studentNo', '${sessionScope.user.studentNo}');
                
                // 格式化时间，确保保留时分
                const lateReturnDate = formData.get('lateReturnDate');
                if (lateReturnDate) {
                    // 将时间字符串转换为Date对象
                    const date = new Date(lateReturnDate);
                    // 格式化为MySQL datetime格式 (YYYY-MM-DD HH:mm:ss)
                    const formattedDate = date.getFullYear() + '-' +
                        String(date.getMonth() + 1).padStart(2, '0') + '-' +
                        String(date.getDate()).padStart(2, '0') + ' ' +
                        String(date.getHours()).padStart(2, '0') + ':' +
                        String(date.getMinutes()).padStart(2, '0') + ':00';
                    
                    formData.set('lateReturnDate', formattedDate);
                }
                
                $.ajax({
                    url: '${pageContext.request.contextPath}/explanation/add',
                    type: 'POST',
                    data: formData,
                    processData: false,
                    contentType: false,
                    success: function(response) {
                        if (response.success) {
                            alert('晚归说明提交成功');
                            $('#explanationForm')[0].reset();
                            removeFile('explanation');
                        } else {
                            alert(response.message || '提交失败，请重试');
                        }
                    },
                    error: function() {
                        alert('提交失败，请重试');
                    }
                });
            });

            // 文件上传预览
            function handleFileSelect(input) {
                const file = input.files[0];
                if (!file) return;

                // 检查文件大小
                if (file.size > 5 * 1024 * 1024) {
                    alert('文件大小不能超过5MB');
                    input.value = '';
                    return;
                }
                // 检查文件类型
                const validTypes = ['image/jpeg', 'image/png', 'application/pdf'];
                if (!validTypes.includes(file.type)) {
                    alert('只支持JPG、PNG和PDF格式的文件');
                    input.value = '';
                    return;
                }

                // 获取 formId
                const formId = input.id.replace('-file-upload', '');

                // 获取所有相关元素
                const previewArea = document.getElementById(formId + '-file-preview');
                const placeholderArea = document.getElementById(formId + '-upload-placeholder');
                const thumbnail = document.getElementById(formId + '-file-thumbnail');
                const fileIcon = document.getElementById(formId + '-file-icon');
                const fileName = document.getElementById(formId + '-file-name');
                const fileSize = document.getElementById(formId + '-file-size');

                // 检查元素是否都存在
                if (!previewArea || !placeholderArea || !thumbnail || !fileIcon || !fileName || !fileSize) {
                    alert('页面元素未正确加载，请刷新页面或联系管理员。');
                    console.error('元素缺失', {previewArea, placeholderArea, thumbnail, fileIcon, fileName, fileSize});
                    return;
                }

                // 显示预览区域，隐藏占位符
                previewArea.style.display = 'block';
                placeholderArea.style.display = 'none';

                // 设置文件名和大小
                fileName.textContent = file.name;
                fileSize.textContent = (file.size / 1024).toFixed(2) + ' KB';

                // 根据文件类型显示预览
                if (file.type.startsWith('image/')) {
                    thumbnail.style.display = 'block';
                    fileIcon.style.display = 'none';
                    const reader = new FileReader();
                    reader.onload = function(e) {
                        thumbnail.src = e.target.result;
                    };
                    reader.readAsDataURL(file);
                } else {
                    thumbnail.style.display = 'none';
                    fileIcon.style.display = 'block';
                    // 可根据文件类型切换icon样式
                    if (file.type === 'application/pdf') {
                        fileIcon.className = 'fas fa-file-pdf text-5xl text-red-500';
                    } else {
                        fileIcon.className = 'fas fa-file-alt text-5xl text-blue-500';
                    }
                }
            }

            // 移除文件
            window.removeFile = function(formId) {
                const input = document.getElementById(`${formId}-file-upload`);
                const previewArea = document.getElementById(`${formId}-file-preview`);
                const placeholderArea = document.getElementById(`${formId}-upload-placeholder`);
                const thumbnail = document.getElementById(`${formId}-file-thumbnail`);
                const fileIcon = document.getElementById(`${formId}-file-icon`);
                const fileName = document.getElementById(`${formId}-file-name`);
                const fileSize = document.getElementById(`${formId}-file-size`);

                console.log('removeFile called', {input, previewArea, placeholderArea, thumbnail, fileIcon, fileName, fileSize});

                if (input) { input.value = ''; console.log('input cleared'); }
                if (thumbnail) { thumbnail.src = ''; thumbnail.style.display = 'none'; console.log('thumbnail cleared'); }
                if (fileIcon) { fileIcon.style.display = 'none'; console.log('fileIcon hidden'); }
                if (fileName) { fileName.textContent = ''; console.log('fileName cleared'); }
                if (fileSize) { fileSize.textContent = ''; console.log('fileSize cleared'); }
                if (previewArea) { previewArea.style.display = 'none'; console.log('previewArea hidden'); }
                if (placeholderArea) { placeholderArea.style.display = 'block'; console.log('placeholderArea shown'); }
            }

            // 格式化文件大小
            function formatFileSize(bytes) {
                if (bytes === 0) return '0 Bytes';
                const k = 1024;
                const sizes = ['Bytes', 'KB', 'MB', 'GB'];
                const i = Math.floor(Math.log(bytes) / Math.log(k));
                return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
            }

            // 添加拖放功能
            function setupDragAndDrop(formId) {
                const dropArea = document.getElementById(`${formId}-upload-area`);
                const input = document.getElementById(`${formId}-file-upload`);

                if (!dropArea || !input) return;

                ['dragenter', 'dragover', 'dragleave', 'drop'].forEach(eventName => {
                    dropArea.addEventListener(eventName, preventDefaults, false);
                });

                function preventDefaults(e) {
                    e.preventDefault();
                    e.stopPropagation();
                }

                dropArea.addEventListener('drop', function(e) {
                    const dt = e.dataTransfer;
                    const files = dt.files;
                    if (files && files.length > 0) {
                        // 兼容性更好：直接用 handleFileSelect 处理
                        input.files = files; // 现代浏览器支持
                        handleFileSelect(input);
                    }
                }, false);
            }

            // 绑定文件上传事件
            $('#application-file-upload, #explanation-file-upload').change(function() {
                handleFileSelect(this);
            });

            // 初始化拖放功能
            setupDragAndDrop('application');
            setupDragAndDrop('explanation');

            // 在页面加载完成后应用状态样式和文本
            applyStatusStyles();

            // 绑定分页按钮事件
            $(document).on('click', '#prevNotificationBtn', function() {
                if (notificationCurrentPage > 1) {
                    notificationCurrentPage--;
                    loadNotifications();
                }
            });

            $(document).on('click', '#nextNotificationBtn', function() {
                const totalPages = Math.ceil($('#notificationList').data('total') / notificationPageSize);
                if (notificationCurrentPage < totalPages) {
                    notificationCurrentPage++;
                    loadNotifications();
                }
            });
        });

        // 应用状态样式
        function applyStatusStyles() {
                $('.status-badge').each(function() {
                    const status = $(this).data('status');
                    $(this).addClass(getStatusClass(status));
                    $(this).text(getStatusText(status));
                });
            }

        function handleLogout() {
            if (confirm('确定要退出登录吗？')) {
                localStorage.removeItem('loginUUID');
                window.location.href = '${pageContext.request.contextPath}/logout';
            }
        }

        // 全局变量
        let currentPage = 1;
        let totalPages = 1;
        const pageSize = 10;

        // 加载晚归记录数据
        function loadLateReturns() {
            const status = $('#statusFilter').val();
            const studentNo = '${sessionScope.user.studentNo}';

            $.ajax({
                url: '${pageContext.request.contextPath}/late-return/pageList',
                type: 'POST',
                contentType: 'application/json',
                data: JSON.stringify({
                    studentNo: studentNo,
                    processStatus: status === 'all' ? '' : status,
                    pageNum: currentPage,
                    pageSize: pageSize
                }),
                success: function(response) {
                    if (response.success) { 
                        const pageResult = response.data;
                        updateLateReturnList(pageResult.data);
                        updatePagination(pageResult.total, pageResult.pageNum, pageResult.pageSize);
                    } else {
                        alert(response.message || '加载数据失败');
                    }
                },
                error: function() {
                    alert('加载晚归记录数据失败，请稍后重试');
                }
            });
        }
        
        
        // 更新晚归记录列表
        function updateLateReturnList(lateReturns) {
            // console.log('收到的数据:', lateReturns);
            const tbody = $('#lateReturnList');
            tbody.empty();

            // 检查数据格式
            if (!lateReturns || !Array.isArray(lateReturns)) {
                console.error('数据格式错误:', lateReturns);
                return;
            }

            if (lateReturns.length === 0) {
                tbody.append(`
                    <tr>
                        <td colspan="5" class="px-6 py-4 text-center text-gray-500">
                            暂无晚归记录
                        </td>
                    </tr>
                `);
                return;
            }

            // 按时间降序排序
            lateReturns.sort((a, b) => new Date(b.lateTime) - new Date(a.lateTime));
            
            lateReturns.forEach(function(record) {
                try {
                    // console.log('渲染行：', record);
                    var date = new Date(record.lateTime);
                    var formattedDate = date.toLocaleDateString('zh-CN', {year: 'numeric', month: '2-digit', day: '2-digit'})
                    var formattedTime = date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });

                    var row = '<tr>' +
                        '<td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">' +
                        // record.formattedDate +
                        formattedDate +
                        '</td>' +

                        '<td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">' +
                        formattedTime +
                        '</td>' +

                        '<td class="px-6 py-4 whitespace-nowrap">' +
                        '<span class="status-badge" data-status="' + record.processStatus + '">' +
                        record.processStatus +
                        '</span>' +
                        '</td>' +

                        '<td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">' +
                        getResultText(record.processResult) +
                        '</td>' +

                        '<td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">' +
                        getRemarkText(record.processRemark) +
                        '</td>' +

                        '<td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">' +
                        '<a href="#" class="text-indigo-600 hover:text-indigo-900" onclick="viewDetail(\'' + record.lateReturnId + '\')">查看详情</a>' +
                        '</td>' +
                        '</tr>';

                    tbody.append(row);
                } catch (error) {
                    console.error('处理记录时出错:', error, record);
                }
            });

            // 更新分页信息
            updatePagination(lateReturns.length);
            
            // 应用状态样式
            applyStatusStyles();
        }

        // 获取状态样式类
        function getStatusClass(status) {
            switch(status) {
                case 'PENDING':
                    return 'status-pending';
                case 'PROCESSING':
                    return 'status-processing';
                case 'FINISHED':
                    return 'status-processed';
                default:
                    return '';
            }
        }

        // 获取状态文本
        function getStatusText(status) {
            switch(status) {
                case 'PENDING':
                    return '待处理';
                case 'PROCESSING':
                    return '处理中';
                case 'FINISHED':
                    return '已处理';
                default:
                    return status;
            }
        }

        // 获取处理结果文本
        function getResultText(result) {
            switch(result) {
                case 'APPROVED':
                    return '已批准';
                case 'REJECTED':
                    return '已驳回';
                default:
                    return "-";
            }
        }

        // 获取处理备注文本
        function getRemarkText(remark) {
            if (!remark) {
                return "-";
            }
            return remark;
        }

        // 更新分页信息
        function updatePagination(total) {
            const start = (currentPage - 1) * pageSize + 1;
            const end = Math.min(start + pageSize - 1, total);
            
            $('#startRecord').text(start);
            $('#endRecord').text(end);
            $('#totalRecords').text(total);
            $('#currentPage').text(currentPage);
            
            totalPages = Math.ceil(total / pageSize);
            
            // 更新分页按钮状态
            $('#prevPage, #prevPageMobile').prop('disabled', currentPage === 1);
            $('#nextPage, #nextPageMobile').prop('disabled', currentPage === totalPages);
        }

        // 查看详情
        function viewDetail(lateReturnId) { // lateReturnId为晚归记录表的lateReturnId
            // 获取晚归记录详情
            $.ajax({
                url: '${pageContext.request.contextPath}/late-return/' + lateReturnId,
                type: 'GET',
                success: function(response) {
                    // 从Result 对象中获取数据
                    const lateReturn = response.data;

                    // 显示晚归记录信息
                    var date = new Date(lateReturn.lateTime);
                    $('#detailLateTime').text(date.toLocaleString('zh-CN'));
                    $('#detailProcessStatus').text(getStatusText(lateReturn.processStatus));
                    $('#detailProcessResult').text(getResultText(lateReturn.processResult));
                    $('#detailProcessRemark').text(lateReturn.processRemark || '-');

                    // 获取晚归说明
                    $.ajax({
                        url: '${pageContext.request.contextPath}/explanation/late-return/' + lateReturnId,
                        type: 'GET',
                        success: function(response) {
                            // 从Result 对象中获取数据
                            const explanation = response.data;

                            if (explanation) {
                                $('#explanationSection').show();
                                $('#detailSubmitTime').text(new Date(explanation.submitTime).toLocaleString('zh-CN'));
                                $('#detailAuditStatus').text(getAuditStatusText(explanation.auditStatus));
                                $('#detailDescription').text(explanation.description || '-');
                                $('#detailAuditRemark').text(explanation.auditRemark || '-');
                            } else {
                                $('#explanationSection').hide();
                            }
                        },
                        error: function() {
                            $('#explanationSection').hide();
                        }
                    });

                    // 显示模态框
                    $('#lateReturnDetailModal').modal('show');
                },
                error: function() {
                    alert('获取详情失败，请稍后重试');
                }
            });
        }

        // 获取审核状态文本
        function getAuditStatusText(status) {
            switch(status) {
                case 0:
                    return '待审核';
                case 1:
                    return '已通过';
                case 2:
                    return '已驳回';
                default:
                    return '-';
            }
        }

        // 加载统计数据
        function loadStatistics() {
            const now = new Date();
            const firstDayOfMonth = new Date(now.getFullYear(), now.getMonth(), 1);

            $.ajax({
                url: '${pageContext.request.contextPath}/late-return/time',
                type: 'POST',
                contentType: 'application/json',
                data: JSON.stringify({
                    // startLateTime: firstDayOfMonth,
                    // endLateTime: now,
                    startLateTime: firstDayOfMonth.toISOString().replace('T', ' ').substring(0, 19),
                    endLateTime: now.toISOString().replace('T', ' ').substring(0, 19),
                    studentNo: '${sessionScope.user.studentNo}'
                }),
                success: function(response) {
                    if (response.success) {
                        const lateReturns = response.data || [];
                        // 统计本月晚归次数
                        const monthlyCount = lateReturns.length;
                        // 统计已处理数量
                        const processedCount = lateReturns.filter(record => record.processStatus === 'FINISHED').length;
                        // 统计待处理数量
                        const pendingCount = lateReturns.filter(record => record.processStatus === 'PENDING').length;

                        // 更新显示
                        $('#monthlyCount').text(monthlyCount);
                        $('#processedCount').text(processedCount);
                        $('#pendingCount').text(pendingCount);
                    } else {
                        console.error('加载统计数据失败:', response.msg);
                    }
                },
                error: function() {
                    console.error('加载统计数据失败');
                }
            });
        }

        // 添加全局变量
        let notificationCurrentPage = 1;
        const notificationPageSize = 3; // 每页显示3条通知

        $(document).ready(function() {
            $('#notificationStatusFilter').on('change', function() {
                // 筛选条件改变时，重置到第一页
                notificationCurrentPage = 1;
                loadNotifications();
            });
        });
        
        // 加载通知消息
        function loadNotifications() {
            // 显示加载状态
            $('#notificationLoading').removeClass('hidden');

             // 获取状态筛选值
             var readStatus = $('#notificationStatusFilter').val();

            $.ajax({
                url: '${pageContext.request.contextPath}/notification/special-user/page',
                type: 'POST',
                contentType: 'application/json',
                data: JSON.stringify({
                    pageNum: notificationCurrentPage,
                    pageSize: notificationPageSize,
                    readStatus: readStatus,
                    targetId: '${sessionScope.user.studentNo}',
                    targetType: 'STUDENT'
                }),
                success: function(response) {
                    if (response.success) {
                        const pageResult = response.data;
                    
                        // 保存总数到 notificationList 元素
                        $('#notificationList').data('total', pageResult.total);
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
                },
                complete: function() {
                    // 隐藏加载状态
                    $('#notificationLoading').addClass('hidden');
                }
            });
        }

        // 更新通知列表
        function updateNotificationList(notificationVOs) {
            const notificationList = $('#notificationList');
            notificationList.empty();
            
            if (!notificationVOs || notificationVOs.length === 0) {
                notificationList.append(`
                    <div class="text-center text-gray-500 py-4">
                        暂无通知
                    </div>
                `);
                return;
            }

            notificationVOs.forEach(function(notificationVO) {
                // 格式化日期时间
                var createTime = formatDateTime(notificationVO.createTime);
                
                // 根据阅读状态决定按钮显示
                var buttonHtml = '';
                if (notificationVO.readStatus === 'READ') {
                    // 已读状态：显示"已读"文本，不可点击
                    buttonHtml = '<span class="text-xs text-gray-400">已读</span>';
                } else {
                    // 未读状态：显示"标记已读"按钮，可点击
                    buttonHtml = '<button onclick="markAsRead(\'' + notificationVO.noticeId + '\')" ' +
                                'class="text-xs text-blue-600 hover:text-blue-800">' +
                                '标记已读' +
                                '</button>';
                }

                var notificationHtml = 
                    '<div class="border-l-4 pl-4 py-2 mb-4 hover:bg-gray-50" data-type="' + notificationVO.noticeType + '">' +
                        '<div class="flex justify-between items-start">' +
                            '<div>' +
                                '<p class="text-sm font-medium text-gray-900">' + (notificationVO.title || '-') + '</p>' +
                                '<p class="text-sm text-gray-500 mt-1">' + (notificationVO.content || '-') + '</p>' +
                                '<p class="text-xs text-gray-400 mt-1">' + createTime + '</p>' +
                            '</div>' +
                            '<div>' +
                                buttonHtml +
                            '</div>' +
                        '</div>' +
                    '</div>';
                notificationList.append(notificationHtml);
            });

            // 应用通知类型样式
            applyNotificationStyles();
        }

        // 添加日期时间格式化函数
        function formatDateTime(dateTime) {
            if (!dateTime) return '';
            let date;
            // 兼容数字时间戳和字符串
            if (typeof dateTime === 'number') {
                date = new Date(dateTime);
            } else {
                date = new Date(dateTime);
            }
            if (isNaN(date.getTime())) return dateTime;
            return date.toLocaleString('zh-CN', {
                year: 'numeric',
                month: '2-digit',
                day: '2-digit',
                hour: '2-digit',
                minute: '2-digit',
                second: '2-digit'
            });
        }

        // 更新通知分页
        function updateNotificationPagination(pageResult) {
            const total = pageResult.total;
            const totalPages = Math.ceil(total / notificationPageSize);
            
            // 先移除已存在的分页控件
            $('#notificationPagination').remove();
            
            // 创建新的分页控件
            var paginationHtml =
                '<div id="notificationPagination" class="flex justify-between items-center mt-4">' +
                    '<span class="text-sm text-gray-500">' +
                        '共 ' + total + ' 条通知' +
                    '</span>' +
                    '<div class="flex space-x-2">' +
                        '<button id="prevNotificationBtn" ' +
                                'class="px-3 py-1 text-sm text-blue-600 hover:text-blue-800">' +
                            '上一页' +
                        '</button>' +
                        '<span class="text-sm text-gray-500">' +
                            notificationCurrentPage + ' / ' + totalPages +
                        '</span>' +
                        '<button id="nextNotificationBtn" ' +
                                'class="px-3 py-1 text-sm text-blue-600 hover:text-blue-800">' +
                            '下一页' +
                        '</button>' +
                    '</div>' +
                '</div>';
            
            $('#notificationList').append(paginationHtml);

            // 更新按钮状态
            updatePaginationButtonStates();
        }

        // 更新分页按钮状态
        function updatePaginationButtonStates() {
            const totalPages = Math.ceil($('#notificationList').data('total') / notificationPageSize);
            
            // 更新上一页按钮状态
            if (notificationCurrentPage === 1) {
                $('#prevNotificationBtn')
                    .addClass('text-gray-400 cursor-not-allowed')
                    .removeClass('text-blue-600 hover:text-blue-800')
                    .prop('disabled', true);
            } else {
                $('#prevNotificationBtn')
                    .removeClass('text-gray-400 cursor-not-allowed')
                    .addClass('text-blue-600 hover:text-blue-800')
                    .prop('disabled', false);
            }
            
            // 更新下一页按钮状态
            if (notificationCurrentPage === totalPages) {
                $('#nextNotificationBtn')
                    .addClass('text-gray-400 cursor-not-allowed')
                    .removeClass('text-blue-600 hover:text-blue-800')
                    .prop('disabled', true);
            } else {
                $('#nextNotificationBtn')
                    .removeClass('text-gray-400 cursor-not-allowed')
                    .addClass('text-blue-600 hover:text-blue-800')
                    .prop('disabled', false);
            }
        }

        // 标记通知为已读
        function markAsRead(noticeId) {
            // 获取当前学生学号
            const studentNo = '${sessionScope.user.studentNo}';
            
            // 构造请求数据
            const requestData = {
                noticeIdList: [noticeId],  // 将单个noticeId封装成List
                receiverId: studentNo,     // 从session获取的studentNo
                targetType: 'STUDENT',     // 目标类型为STUDENT
                readStatus: 'READ'         // 阅读状态为READ
            };
            
            $.ajax({
                url: '${pageContext.request.contextPath}/notification/mark-read',
                type: 'POST',
                contentType: 'application/json',
                data: JSON.stringify(requestData),
                success: function(response) {
                    if (response.success) {
                        // 重新加载通知列表
                        loadNotifications();
                    } else {
                        alert('标记已读失败：' + response.message);
                    }
                },
                error: function() {
                    alert('标记已读失败，请稍后重试');
                }
            });
        }

        // 应用通知类型样式
        function applyNotificationStyles() {
            $('[data-type]').each(function() {
                const type = $(this).data('type');
                let borderColor;
                switch (type) {
                    case '系统通知':
                        borderColor = 'border-blue-500';
                        break;
                    case '晚归通知':
                        borderColor = 'border-yellow-500';
                        break;
                    case '预警通知':
                        borderColor = 'border-red-500';
                        break;
                    default:
                        borderColor = 'border-gray-500';
                }
                $(this).addClass(borderColor);
            });
        }

    

        // 页面加载完成后初始化
        $(document).ready(function() {
            // 初始加载通知
            loadNotifications();
            
            // 定时刷新通知（每5分钟）
            setInterval(loadNotifications, 5 * 60 * 1000);
        });

        //  生成alarmNo
        function generateAlarmNo() {
            var now = new Date();
            var pad = function(n) {
                return n < 10 ? '0' + n : n;
            };
            var dateStr = now.getFullYear() + 
                         pad(now.getMonth() + 1) + 
                         pad(now.getDate()) + 
                         pad(now.getHours()) + 
                         pad(now.getMinutes()) + 
                         pad(now.getSeconds());
            var randomStr = Math.floor(1000 + Math.random() * 9000);
            return 'AL' + dateStr + randomStr;
        }




        // // 页面加载时初始化地图
        // $(document).ready(function() {
        //     initMapAndLocation();
        // });

        AMap.plugin('AMap.Geolocation', function() {
          var map = new AMap.Map('map-container', {
            resizeEnable: true,
            zoom: 16
          });

          var geolocation = new AMap.Geolocation({
            enableHighAccuracy: true,
            timeout: 10000,
            buttonPosition: 'RB',
            zoomToAccuracy: true,
            needAddress: true
          });

          map.addControl(geolocation);
          geolocation.getCurrentPosition();

          // 用 geolocation.on 绑定事件
          geolocation.on('complete', function(data) {
            var lng = data.position.lng;
            var lat = data.position.lat;
            var address = data.formattedAddress || '当前位置';

            // 保存当前位置信息
            window.currentPosition = {
              latitude: lat,
              longitude: lng,
              accuracy: data.accuracy // 新增精度字段
            };

            // 2. 清空地图上的覆盖物
            map.clearMap();

            // 3. 添加当前位置标记
            var marker = new AMap.Marker({
              position: [lng, lat],
              icon: 'https://webapi.amap.com/theme/v1.3/markers/n/mark_b.png',
              title: address,
              animation: 'AMAP_ANIMATION_DROP'
            });
            map.add(marker);

            // 4. 添加信息窗体
            var infoWindow = new AMap.InfoWindow({
              content: '<div class="map-info-window">' +
                '<h4>当前位置</h4>' +
                '<p>' + address + '</p>' +
                '</div>',
              offset: new AMap.Pixel(0, -30)
            });
            infoWindow.open(map, [lng, lat]);

            // 5. 点击标记时显示信息窗体
            marker.on('click', function() {
              infoWindow.open(map, marker.getPosition());
            });

            // 6. 可选：地图中心定位到当前位置
            map.setCenter([lng, lat]);

            document.getElementById('location-info').innerHTML =
              '地址：' + (data.formattedAddress || '-') + '<br>' +
              '经度：' + data.position.lng + '<br>' +
              '纬度：' + data.position.lat + '<br>' +
              '定位精度：' + (data.accuracy ? data.accuracy + '米' : '-');
          });

          geolocation.on('error', function(err) {
            document.getElementById('location-info').innerHTML = '定位失败: ' + err.message;
            window.currentPosition = null;
          });
        });

        // 全局变量
        let alarmNo = null;
        let currentWebSocket = null;

        // 发送报警请求
        $('#normal-alarm-btn').click(function() {
            if (!window.currentPosition) {
                alert('定位信息获取中，请稍后重试');
                return;
            }
            alarmNo = generateAlarmNo();
            const studentNo = '${sessionScope.user.studentNo}';
            $.ajax({
                url: '${pageContext.request.contextPath}/alarm/one-click/trigger',
                type: 'POST',
                contentType: 'application/json',
                data: JSON.stringify({
                    studentNo: studentNo,
                    alarmLevel: 'NORMAL',
                    alarmNo: alarmNo,
                    latitude: window.currentPosition.latitude,
                    longitude: window.currentPosition.longitude
                }),
                success: function(res) {
                    // 1. 启动位置信息WebSocket
                    startLocationWebSocket(alarmNo);
                    // 2. 启动音视频WebSocket
                    startMediaWebSocket(alarmNo);
                    // 3. todo 其他处理


                },
                error: function(err) {
                    alert('报警失败，请重试');
                }
            });
        });

        // 成功触发报警后使用ws处理位置信息
        function startLocationWebSocket(alarmNo) {
            alert('报警成功！');
            // 显示取消报警按钮
            $('#cancel-alarm-btn').removeClass('hidden');
            // 禁用报警按钮
            $('#normal-alarm-btn, #emergency-alarm-btn').prop('disabled', true);

            // 建立WebSocket连接
            const wsUrl = 'ws://' + window.location.host + '${pageContext.request.contextPath}/ws/location?alarmNo=' + alarmNo + '&businessType=ALARM_LOCATION';
            currentWebSocket = new WebSocket(wsUrl);

            // 连接建立时的处理
            currentWebSocket.onopen = function() {
                console.log('WebSocket连接已建立');
                // 开始定时发送位置更新
                const locationInterval = setInterval(function() {
                    if (currentWebSocket.readyState === WebSocket.OPEN && window.currentPosition) {
                        const locationData = {
                            alarmNo: alarmNo,
                            latitude: window.currentPosition.latitude,
                            longitude: window.currentPosition.longitude,
                            locationTime: new Date().toLocaleString('zh-CN', {
                                year: 'numeric',
                                month: '2-digit',
                                day: '2-digit',
                                hour: '2-digit',
                                minute: '2-digit',
                                second: '2-digit',
                                hour12: false
                            }).replace(/\//g, '-'),
                            speed: 0,
                            direction: 0,
                            locationAccuracy: window.currentPosition.accuracy
                        };
                        currentWebSocket.send(JSON.stringify(locationData));
                    } else {
                        clearInterval(locationInterval);
                    }
                }, 3000); // 每3秒发送一次

                // 保存interval ID，以便在连接关闭时清除
                currentWebSocket.locationInterval = locationInterval;
            };

            // 接收消息的处理
            currentWebSocket.onmessage = function(event) {
                console.log('收到消息:', event.data);
            };

            // 连接关闭时的处理
            currentWebSocket.onclose = function() {
                console.log('WebSocket连接已关闭');
                if (currentWebSocket.locationInterval) {
                    clearInterval(currentWebSocket.locationInterval);
                }
            };

            // 连接错误时的处理
            currentWebSocket.onerror = function(error) {
                console.error('WebSocket错误:', error);
                if (currentWebSocket.locationInterval) {
                    clearInterval(currentWebSocket.locationInterval);
                }
            };
        }

        // 成功触发报警后使用ws处理音视频信息
        async function startMediaWebSocket(alarmNo) {
            sessionId = generateSessionId();
            await startMedia(); // 等待摄像头/麦克风准备好
            connectVideoWebSocket(alarmNo, sessionId); // 传递参数更安全
            startRecording();
        }

        // 取消一键报警请求
        $('#cancel-alarm-btn').click(function() {
            if (!alarmNo) {
                alert('没有正在进行的报警');
                return;
            }

            const studentNo = '${sessionScope.user.studentNo}';
            const name = '${sessionScope.user.name}';
            $.ajax({
                url: '${pageContext.request.contextPath}/alarm/one-click/cancel',
                type: 'POST',
                contentType: 'application/json',
                data: JSON.stringify({
                    studentNo: studentNo,
                    name: name,
                    alarmNo: alarmNo
                }),
                success: function(res) {
                    alert('报警已取消');
                    // 关闭WebSocket连接
                    if (currentWebSocket) {
                        currentWebSocket.close();
                        currentWebSocket = null;
                    }
                    // 隐藏取消报警按钮
                    $('#cancel-alarm-btn').addClass('hidden');
                    // 启用报警按钮
                    $('#normal-alarm-btn, #emergency-alarm-btn').prop('disabled', false);
                    // 清除当前报警编号
                    alarmNo = null;

                    //停止录音录像
                    stopRecording();
                },
                error: function(err) {
                    alert('取消报警失败，请重试');
                }
            });
        });

    // -----------------------------------------------------------------------------
        let ws, mediaRecorder, mediaStream;
        let sessionId = generateSessionId();
        let chunkIndex = 0;
        let chunkCache = []; // 本地缓存分片

        // 开始录音和录像
        async function startMedia() {
            mediaStream = await navigator.mediaDevices.getUserMedia({ video: true, audio: true });
            document.querySelector("#videoPreview").srcObject = mediaStream;
        }

        function generateSessionId() {
            return 'xxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
                var r = Math.random() * 16 | 0, v = c === 'x' ? r : (r & 0x3 | 0x8);
                return v.toString(16);
            });
        }

        // 建立发送音视频数据的ws链接
        function connectVideoWebSocket(alarmNo, sessionId) {
            ws = new WebSocket('ws://' + window.location.host + '${pageContext.request.contextPath}/ws/media?alarmNo=' + alarmNo + "&sessionId=" + sessionId);
            ws.binaryType = "arraybuffer";

            ws.onopen = () => {
                startRecording();
            };

            ws.onmessage = (event) => {
                // 后端返回已收到chunkIndex，前端据此删除本地缓存
                let resp = JSON.parse(event.data);
                if (resp.type === "ACK") {
                    // 删除已确认分片
                    chunkCache = chunkCache.filter(chunk => chunk.chunkIndex > resp.chunkIndex);
                }
            };

            ws.onclose = () => { stopRecording(); };
            ws.onerror = (e) => { stopRecording(); };
        }

        // 开始录音和录像
        function startRecording() {
            if (!mediaStream) return;
            console.log("开始录像和录音")
            mediaRecorder = new MediaRecorder(mediaStream, { mimeType: "video/webm; codecs=vp8,opus" });

            mediaRecorder.ondataavailable = function(event) {
                if (event.data && event.data.size > 0) {
                    let chunk = {
                        alarmNo,
                        sessionId,
                        chunkIndex: chunkIndex,
                        isLastChunk: false // 结束时再置true
                    };
                    chunkCache.push({ ...chunk, data: event.data }); // 本地缓存
                    sendChunk(chunk, event.data);
                    chunkIndex++;
                }
            };

            // todo 根据网络和后端接受策略动态调整单个分片的时间长度
            mediaRecorder.start(5000); // 每5秒一个分片
        }

        // 通过ws发送音视频分片数据
        // function sendChunk(chunk, blob) {
        //     console.log("开始传输音视频分片数据");
        //     if (ws && ws.readyState === WebSocket.OPEN) {
        //         // 先发JSON头，再发二进制体
        //         let header = JSON.stringify(chunk);
        //         let encoder = new TextEncoder();
        //         let headerBytes = encoder.encode(header);
        //         let headerLength = new Uint32Array([headerBytes.length]);
        //         blob.arrayBuffer().then(dataBuffer => {
        //             // 拼接：4字节头长 + 头 + 数据
        //             let total = new Uint8Array(4 + headerBytes.length + dataBuffer.byteLength);
        //             total.set(new Uint8Array(headerLength.buffer), 0);
        //             total.set(headerBytes, 4);
        //             total.set(new Uint8Array(dataBuffer), 4 + headerBytes.length);
        //             ws.send(total.buffer);
        //         });
        //     }
        // }

        function sendChunk(chunk, blob) {
            console.log("开始传输音视频分片数据");
            if (ws && ws.readyState === WebSocket.OPEN) {
                let header = JSON.stringify(chunk);
                let encoder = new TextEncoder();
                let headerBytes = encoder.encode(header);

                // 用 DataView 明确写入大端字节序
                let totalLength = 4 + headerBytes.length + blob.size;
                let total = new Uint8Array(totalLength);
                let view = new DataView(total.buffer);

                // 写入4字节大端header长度
                view.setUint32(0, headerBytes.length, false); // false = big-endian

                // 写入header
                total.set(headerBytes, 4);

                // 写入二进制体
                blob.arrayBuffer().then(dataBuffer => {
                    total.set(new Uint8Array(dataBuffer), 4 + headerBytes.length);
                    ws.send(total.buffer);
                });
            }
        }

        // 断线重连时重发未确认分片
        function resendChunks() {
            chunkCache.forEach(chunkObj => {
                sendChunk(chunkObj, chunkObj.data);
            });
        }

        // 停止录音和录像
        function stopRecording() {
            console.log("停止录音和录像")
            if (mediaRecorder && mediaRecorder.state !== "inactive") {
                mediaRecorder.stop();
            }
        }

        // -----------------------------------------------------------------------------
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