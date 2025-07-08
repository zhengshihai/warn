<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<!DOCTYPE html>
<html>
<head>
    <title>学生晚归预警系统 - 晚归记录</title>
    <!-- 引入 CSS 文件 -->
    <link href="${pageContext.request.contextPath}/static/css/tailwind.min.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/static/css/fontawesome.min.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/static/css/bootstrap.min.css" rel="stylesheet">

    <!-- 引入 JS 文件 -->
    <script src="${pageContext.request.contextPath}/static/js/echarts.min.js"></script>
    <script src="${pageContext.request.contextPath}/static/js/jquery.min.js"></script>
    <script src="${pageContext.request.contextPath}/static/js/popper.min.js"></script>
    <script src="${pageContext.request.contextPath}/static/js/bootstrap.bundle.min.js"></script>
    <style>
        .table-container {
            overflow-x: auto;
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
        .loading {
            display: none;
            text-align: center;
            padding: 20px;
        }
        .no-data {
            text-align: center;
            padding: 40px;
            color: #6B7280;
        }
    </style>
</head>
<body class="bg-gray-50">
    <div class="min-h-screen p-6">
        <!-- 顶部导航栏 -->
        <nav class="bg-white shadow-sm rounded-lg mb-6 p-4">
            <div class="flex justify-between items-center">
                <h1 class="text-xl font-semibold text-gray-800">学生晚归预警系统</h1>
                <div class="flex items-center space-x-4">
                    <a href="dean" class="text-gray-600 hover:text-gray-900">返回首页</a>
                    <span class="text-gray-600">欢迎</span>
                    <button class="text-sm text-red-600 hover:text-red-800" onclick="handleLogout()">退出登录</button>
                </div>
            </div>
        </nav>

        <!-- 页面标题和操作按钮 -->
        <div class="flex justify-between items-center mb-6">
            <h2 class="text-2xl font-bold text-gray-800">晚归记录列表</h2>
            <div class="flex space-x-4">
                <button class="px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700">
                    导出数据
                </button>
                <button class="px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700">
                    批量处理
                </button>
            </div>
        </div>

        <!-- 筛选条件 -->
        <div class="bg-white rounded-lg shadow-sm p-6 mb-6">
            <form id="searchForm" class="grid grid-cols-1 md:grid-cols-4 gap-4">
                <div>
                    <label class="block text-sm font-medium text-gray-700 mb-1">时间范围</label>
                    <div class="flex space-x-2">
                        <input type="date" id="startDate" name="startDate" class="flex-1 rounded-md border-gray-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500">
                        <span class="text-gray-500 self-center">至</span>
                        <input type="date" id="endDate" name="endDate" class="flex-1 rounded-md border-gray-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500">
                    </div>
                </div>
                <div>
                    <label class="block text-sm font-medium text-gray-700 mb-1">学院</label>
                    <select id="college" name="college" class="w-full rounded-md border-gray-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500">
                        <option value="">全部学院</option>
                        <option value="计算机学院">计算机学院</option>
                        <option value="经济学院">经济学院</option>
                        <option value="医学院">医学院</option>
                        <option value="文学院">文学院</option>
                    </select>
                </div>
                <div>
                    <label class="block text-sm font-medium text-gray-700 mb-1">处理状态</label>
                    <select id="processStatus" name="processStatus" class="w-full rounded-md border-gray-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500">
                        <option value="">全部状态</option>
                        <option value="PENDING">待处理</option>
                        <option value="PROCESSING">处理中</option>
                        <option value="FINISHED">已处理</option>
                    </select>
                </div>
                <div class="flex items-end">
                    <button type="submit" class="w-full px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700">
                        查询
                    </button>
                </div>
            </form>
        </div>

        <!-- 搜索框 -->
        <div class="mb-6">
            <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div class="relative">
                    <input type="text" id="studentNoInput" placeholder="搜索学号..." class="w-full px-4 py-2 pl-10 rounded-lg border border-gray-300 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent">
                    <div class="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                        <i class="fas fa-id-card text-gray-400"></i>
                    </div>
                </div>
                <div class="relative">
                    <input type="text" id="studentNameInput" placeholder="搜索姓名..." class="w-full px-4 py-2 pl-10 rounded-lg border border-gray-300 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent">
                    <div class="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                        <i class="fas fa-user text-gray-400"></i>
                    </div>
                </div>
            </div>
        </div>

        <!-- 数据表格 -->
        <div class="bg-white rounded-lg shadow-sm overflow-hidden">
            <div class="table-container">
                <table class="min-w-full divide-y divide-gray-200">
                    <thead class="bg-gray-50">
                        <tr>
                            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                晚归业务编号
                            </th>
                            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                学号
                            </th>
                            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                姓名
                            </th>
                            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                学院
                            </th>
                            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                宿舍号
                            </th>
                            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                晚归时间
                            </th>
                            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                状态
                            </th>
                            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                操作
                            </th>
                        </tr>
                    </thead>
                    <tbody id="tableBody" class="bg-white divide-y divide-gray-200">
                        <!-- 数据将通过JavaScript动态加载 -->
                    </tbody>
                </table>
            </div>
            
            <!-- 加载中提示 -->
            <div id="loading" class="loading">
                <i class="fas fa-spinner fa-spin text-indigo-600 text-xl"></i>
                <p class="mt-2 text-gray-600">正在加载数据...</p>
            </div>
            
            <!-- 无数据提示 -->
            <div id="noData" class="no-data" style="display: none;">
                <i class="fas fa-inbox text-gray-400 text-4xl mb-4"></i>
                <p>暂无数据</p>
            </div>
        </div>

        <!-- 分页 -->
        <div id="pagination" class="bg-white px-4 py-3 flex items-center justify-between border-t border-gray-200 sm:px-6 mt-4 rounded-lg" style="display: none;">
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
                        显示第 <span id="startRecord" class="font-medium">1</span> 到 <span id="endRecord" class="font-medium">10</span> 条，共 <span id="totalRecords" class="font-medium">0</span> 条记录
                    </p>
                </div>
                <div>
                    <nav class="relative z-0 inline-flex rounded-md shadow-sm -space-x-px" aria-label="Pagination">
                        <button id="prevPage" class="relative inline-flex items-center px-2 py-2 rounded-l-md border border-gray-300 bg-white text-sm font-medium text-gray-500 hover:bg-gray-50">
                            <span class="sr-only">上一页</span>
                            <i class="fas fa-chevron-left"></i>
                        </button>
                        <div id="pageNumbers" class="flex">
                            <!-- 页码将通过JavaScript动态生成 -->
                        </div>
                        <button id="nextPage" class="relative inline-flex items-center px-2 py-2 rounded-r-md border border-gray-300 bg-white text-sm font-medium text-gray-500 hover:bg-gray-50">
                            <span class="sr-only">下一页</span>
                            <i class="fas fa-chevron-right"></i>
                        </button>
                    </nav>
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


        // 全局变量
        var currentPage = 1;
        var pageSize = 20;
        var totalRecords = 0;
        var totalPages = 0;
        
        // 页面加载完成后执行
        $(document).ready(function() {
            // 初始化页面
            loadData();
            
            // 绑定表单提交事件
            $('#searchForm').on('submit', function(e) {
                e.preventDefault();
                currentPage = 1; // 重置到第一页
                loadData();
            });
            
            // 绑定搜索框事件（防抖）
            var searchTimer;
            $('#studentNoInput, #studentNameInput').on('input', function() {
                clearTimeout(searchTimer);
                searchTimer = setTimeout(function() {
                    currentPage = 1; // 重置到第一页
                    loadData();
                }, 500);
            });
            
            // 绑定分页事件
            $(document).on('click', '#prevPage, #prevPageMobile', function() {
                if (currentPage > 1) {
                    currentPage--;
                    loadData();
                }
            });
            
            $(document).on('click', '#nextPage, #nextPageMobile', function() {
                if (currentPage < totalPages) {
                    currentPage++;
                    loadData();
                }
            });
            
            $(document).on('click', '.page-link', function() {
                var page = parseInt($(this).data('page'));
                if (page && page !== currentPage) {
                    currentPage = page;
                    loadData();
                }
            });
        });
        
        // 加载数据
        function loadData() {
            showLoading();
            
            // 构建查询参数
            var queryParams = {
                pageNum: currentPage,
                pageSize: pageSize,
                startLateTime: formatDateForBackend($('#startDate').val()),
                endLateTime: formatDateForBackend($('#endDate').val()),
                college: $('#college').val(),
                processStatus: $('#processStatus').val(),
                studentName: $('#studentNameInput').val() || null,
                studentNo: $('#studentNoInput').val() || null
            };
            
            // 发送AJAX请求
            $.ajax({
                url: '${pageContext.request.contextPath}/late-return/pageList',
                type: 'POST',
                contentType: 'application/json',
                data: JSON.stringify(queryParams),
                success: function(response) {
                    hideLoading();
                    if (response.success) {
                        renderData(response.data);
                    } else {
                        showError(response.message || '查询失败');
                    }
                },
                error: function(xhr, status, error) {
                    hideLoading();
                    showError('网络错误，请稍后重试');
                    console.error('AJAX Error:', error);
                }
            });
        }
        
        // 渲染数据
        function renderData(pageResult) {
            var data = pageResult.data || [];
            totalRecords = pageResult.total || 0;
            totalPages = Math.ceil(totalRecords / pageSize);
            
            // 渲染表格数据
            var tbody = $('#tableBody');
            tbody.empty();
            
            if (data.length === 0) {
                $('#noData').show();
                $('#pagination').hide();
                return;
            }
            
            $('#noData').hide();
            
            data.forEach(function(item) {
                var row = '<tr>' +
                    '<td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">' + (item.lateReturnId || '-') + '</td>' +
                    '<td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">' + (item.studentNo || '-') + '</td>' +
                    '<td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">' + (item.studentName || '-') + '</td>' +
                    '<td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">' + (item.college || '-') + '</td>' +
                    '<td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">' + (item.dormitory || '-') + '</td>' +
                    '<td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">' + formatDateTime(item.lateTime) + '</td>' +
                    '<td class="px-6 py-4 whitespace-nowrap">' +
                        '<span class="status-badge ' + getStatusClass(item.processStatus) + '">' + getStatusText(item.processStatus) + '</span>' +
                    '</td>' +
                    '<td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">' +
                        '<a href="#" class="text-indigo-600 hover:text-indigo-900 mr-3" onclick="viewDetail(\'' + item.lateReturnId + '\')">查看</a>' 
                    '</td>' +
                '</tr>';
                tbody.append(row);
            });
            
            // 渲染分页
            renderPagination();
        }
        
        // 渲染分页
        function renderPagination() {
            if (totalPages <= 1) {
                $('#pagination').hide();
                return;
            }
            
            $('#pagination').show();
            
            // 更新记录信息
            var startRecord = (currentPage - 1) * pageSize + 1;
            var endRecord = Math.min(currentPage * pageSize, totalRecords);
            $('#startRecord').text(startRecord);
            $('#endRecord').text(endRecord);
            $('#totalRecords').text(totalRecords);
            
            // 更新分页按钮状态
            $('#prevPage, #prevPageMobile').prop('disabled', currentPage <= 1);
            $('#nextPage, #nextPageMobile').prop('disabled', currentPage >= totalPages);
            
            // 生成页码
            var pageNumbers = $('#pageNumbers');
            pageNumbers.empty();
            
            var maxVisiblePages = 5;
            var startPage = Math.max(1, currentPage - Math.floor(maxVisiblePages / 2));
            var endPage = Math.min(totalPages, startPage + maxVisiblePages - 1);
            
            if (endPage - startPage + 1 < maxVisiblePages) {
                startPage = Math.max(1, endPage - maxVisiblePages + 1);
            }
            
            for (var i = startPage; i <= endPage; i++) {
                var pageClass = i === currentPage ? 
                    'relative inline-flex items-center px-4 py-2 border border-indigo-500 bg-indigo-50 text-sm font-medium text-indigo-600' :
                    'relative inline-flex items-center px-4 py-2 border border-gray-300 bg-white text-sm font-medium text-gray-700 hover:bg-gray-50';
                
                var pageLink = '<button class="page-link ' + pageClass + '" data-page="' + i + '">' + i + '</button>';
                pageNumbers.append(pageLink);
            }
        }
        
        // 工具函数
        function formatDateForBackend(dateStr) {
            if (!dateStr) return null;
            return dateStr + ' 00:00:00';
        }
        
        function formatDateTime(dateStr) {
            if (!dateStr) return '-';
            var date = new Date(dateStr);
            return date.toLocaleString('zh-CN', {
                year: 'numeric',
                month: '2-digit',
                day: '2-digit',
                hour: '2-digit',
                minute: '2-digit'
            });
        }
        
        function getStatusClass(status) {
            switch (status) {
                case 'PENDING': return 'status-pending';
                case 'PROCESSING': return 'status-pending';
                case 'FINISHED': return 'status-processed';
                default: return 'status-pending';
            }
        }
        
        function getStatusText(status) {
            switch (status) {
                case 'PENDING': return '待处理';
                case 'PROCESSING': return '处理中';
                case 'FINISHED': return '已处理';
                default: return '待处理';
            }
        }
        
        function showLoading() {
            $('#loading').show();
            $('#tableBody').hide();
            $('#noData').hide();
        }
        
        function hideLoading() {
            $('#loading').hide();
            $('#tableBody').show();
        }
        
        function showError(message) {
            alert(message);
        }
        
        // 操作函数
        function viewDetail(lateReturnId) {
            // 显示加载模态框
            showDetailModal();
            
            // 获取当前行的学生学号和晚归时间
            var studentNo = '';
            var lateReturnData = null;
            $('#tableBody tr').each(function() {
                var rowLateReturnId = $(this).find('td:first').text().trim();
                if (rowLateReturnId === lateReturnId) {
                    studentNo = $(this).find('td:nth-child(2)').text().trim(); // 学号在第二列
                    lateReturnData = {
                        lateReturnId: rowLateReturnId,
                        studentNo: $(this).find('td:nth-child(2)').text().trim(),
                        studentName: $(this).find('td:nth-child(3)').text().trim(),
                        college: $(this).find('td:nth-child(4)').text().trim(),
                        dormitory: $(this).find('td:nth-child(5)').text().trim(),
                        lateTime: null // 将从AJAX响应中获取
                    };
                    return false; // 跳出循环
                }
            });
            
            // 请求1：查询晚归详情
            $.ajax({
                url: '${pageContext.request.contextPath}/late-return/' + lateReturnId,
                type: 'GET',
                success: function(response) {
                    if (response.success && response.data) {
                        lateReturnData.lateTime = response.data.lateTime; // 保存原始晚归时间
                        renderLateReturnDetail(response.data);
                    } else {
                        $('#lateReturnDetail').html('<div class="text-center text-gray-500 py-8">暂无晚归详情数据</div>');
                    }
                },
                error: function() {
                    $('#lateReturnDetail').html('<div class="text-center text-red-500 py-8">获取晚归详情失败</div>');
                }
            });
            
            // 请求2：查询晚归说明
            $.ajax({
                url: '${pageContext.request.contextPath}/explanation/late-return/' + lateReturnId,
                type: 'GET',
                success: function(response) {
                    if (response.success && response.data) {
                        renderExplanationDetail(response.data);
                    } else {
                        $('#explanationDetail').html('<div class="text-center text-gray-500 py-8">暂无晚归说明数据</div>');
                    }
                },
                error: function() {
                    $('#explanationDetail').html('<div class="text-center text-red-500 py-8">获取晚归说明失败</div>');
                }
            });
            
            // 请求3：查询晚归申请
            if (studentNo) {
                $.ajax({
                    url: '${pageContext.request.contextPath}/application/student/' + studentNo,
                    type: 'GET',
                    success: function(response) {
                        if (response && response.length > 0) {
                            // 传递晚归时间用于筛选当天申请
                            renderApplicationDetail(response, lateReturnData.lateTime);
                        } else {
                            $('#applicationDetail').html('<div class="text-center text-gray-500 py-8">暂无晚归申请数据</div>');
                        }
                    },
                    error: function() {
                        $('#applicationDetail').html('<div class="text-center text-red-500 py-8">获取晚归申请失败</div>');
                    }
                });
            } else {
                $('#applicationDetail').html('<div class="text-center text-gray-500 py-8">无法获取学生学号</div>');
            }
        }
        
        // 渲染晚归详情
        function renderLateReturnDetail(data) {
            var html = '<div class="bg-white rounded-lg p-6">' +
                '<h3 class="text-lg font-semibold text-gray-800 mb-4">晚归详情</h3>' +
                '<div class="grid grid-cols-1 md:grid-cols-2 gap-4">' +
                    '<div class="space-y-3">' +
                        '<div><span class="font-medium text-gray-700">晚归业务编号：</span><span class="text-gray-900">' + (data.lateReturnId || '-') + '</span></div>' +
                        '<div><span class="font-medium text-gray-700">学号：</span><span class="text-gray-900">' + (data.studentNo || '-') + '</span></div>' +
                        '<div><span class="font-medium text-gray-700">学生姓名：</span><span class="text-gray-900">' + (data.studentName || '-') + '</span></div>' +
                        '<div><span class="font-medium text-gray-700">学院：</span><span class="text-gray-900">' + (data.college || '-') + '</span></div>' +
                        '<div><span class="font-medium text-gray-700">宿舍号：</span><span class="text-gray-900">' + (data.dormitory || '-') + '</span></div>' +
                    '</div>' +
                    '<div class="space-y-3">' +
                        '<div><span class="font-medium text-gray-700">晚归时间：</span><span class="text-gray-900">' + formatDateTime(data.lateTime) + '</span></div>' +
                        '<div><span class="font-medium text-gray-700">晚归原因：</span><span class="text-gray-900">' + (data.reason || '-') + '</span></div>' +
                        '<div><span class="font-medium text-gray-700">处理状态：</span><span class="status-badge ' + getStatusClass(data.processStatus) + '">' + getStatusText(data.processStatus) + '</span></div>' +
                        '<div><span class="font-medium text-gray-700">处理结果：</span><span class="text-gray-900">' + (data.processResult || '-') + '</span></div>' +
                        '<div><span class="font-medium text-gray-700">处理备注：</span><span class="text-gray-900">' + (data.processRemark || '-') + '</span></div>' +
                    '</div>' +
                '</div>' +
            '</div>';
            $('#lateReturnDetail').html(html);
        }
        
        // 渲染晚归说明
        function renderExplanationDetail(data) {
            var html = '<div class="bg-white rounded-lg p-6">' +
                '<h3 class="text-lg font-semibold text-gray-800 mb-4">晚归说明</h3>' +
                '<div class="space-y-3">' +
                    '<div><span class="font-medium text-gray-700">说明编号：</span><span class="text-gray-900">' + (data.explanationId || '-') + '</span></div>' +
                    '<div><span class="font-medium text-gray-700">提交时间：</span><span class="text-gray-900">' + formatDateTime(data.submitTime) + '</span></div>' +
                    '<div><span class="font-medium text-gray-700">审核状态：</span><span class="text-gray-900">' + getAuditStatusText(data.auditStatus) + '</span></div>' +
                    '<div><span class="font-medium text-gray-700">审核人：</span><span class="text-gray-900">' + (data.auditPerson || '-') + '</span></div>' +
                    '<div><span class="font-medium text-gray-700">审核时间：</span><span class="text-gray-900">' + formatDateTime(data.auditTime) + '</span></div>' +
                    '<div><span class="font-medium text-gray-700">审核备注：</span><span class="text-gray-900">' + (data.auditRemark || '-') + '</span></div>' +
                    '<div class="mt-4">' +
                        '<span class="font-medium text-gray-700 block mb-2">说明内容：</span>' +
                        '<div class="bg-gray-50 p-4 rounded-lg text-gray-900">' + (data.description || '暂无说明内容') + '</div>' +
                    '</div>' +
                '</div>' +
            '</div>';
            $('#explanationDetail').html(html);
        }
        
        // 渲染晚归申请
        function renderApplicationDetail(data, lateReturnTime) {
            var html = '<div class="bg-white rounded-lg p-6">' +
                '<h3 class="text-lg font-semibold text-gray-800 mb-4">晚归申请记录</h3>';
            
            if (data.length === 0) {
                html += '<div class="text-center text-gray-500 py-8">暂无申请记录</div>';
            } else {
                // 添加切换按钮
                html += '<div class="flex justify-center space-x-4 mb-4">' +
                    '<button id="showTodayApplications" class="px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700">当天申请</button>' +
                    '<button id="showAllApplications" class="px-4 py-2 bg-gray-200 text-gray-800 rounded-lg hover:bg-gray-300">全部申请</button>' +
                '</div>';
                
                // 添加申请记录列表容器
                html += '<div id="applicationList" class="space-y-4">';
                
                // 默认显示当天申请
                var todayApplications = filterTodayApplications(data, lateReturnTime);
                if (todayApplications.length === 0) {
                    html += '<div class="text-center text-gray-500 py-8">当天暂无申请记录</div>';
                } else {
                    todayApplications.forEach(function(item, index) {
                        html += createApplicationItemHtml(item, index);
                    });
                }
                
                html += '</div>';
            }
            
            html += '</div>';
            $('#applicationDetail').html(html);
            
            // 绑定按钮事件
            $('#showTodayApplications').on('click', function() {
                $(this).removeClass('bg-gray-200 text-gray-800').addClass('bg-indigo-600 text-white');
                $('#showAllApplications').removeClass('bg-indigo-600 text-white').addClass('bg-gray-200 text-gray-800');
                
                var todayApplications = filterTodayApplications(data, lateReturnTime);
                updateApplicationList(todayApplications);
            });
            
            $('#showAllApplications').on('click', function() {
                $(this).removeClass('bg-gray-200 text-gray-800').addClass('bg-indigo-600 text-white');
                $('#showTodayApplications').removeClass('bg-indigo-600 text-white').addClass('bg-gray-200 text-gray-800');
                
                updateApplicationList(data);
            });
        }
        
        // 筛选当天申请
        function filterTodayApplications(data, lateReturnTime) {
            if (!lateReturnTime) return data;
            
            try {
                // 解析晚归时间，获取日期部分
                var lateReturnDate = new Date(lateReturnTime);
                if (isNaN(lateReturnDate.getTime())) {
                    console.warn('无法解析晚归时间:', lateReturnTime);
                    return data;
                }
                
                var lateReturnDateStr = lateReturnDate.toISOString().split('T')[0]; // 格式：YYYY-MM-DD
                
                return data.filter(function(item) {
                    if (!item.expectedReturnTime) return false;
                    
                    var expectedDate = new Date(item.expectedReturnTime);
                    if (isNaN(expectedDate.getTime())) {
                        console.warn('无法解析预期返校时间:', item.expectedReturnTime);
                        return false;
                    }
                    
                    var expectedDateStr = expectedDate.toISOString().split('T')[0];
                    
                    return expectedDateStr === lateReturnDateStr;
                });
            } catch (error) {
                console.error('筛选当天申请时出错:', error);
                return data;
            }
        }
        
        // 创建申请项HTML
        function createApplicationItemHtml(item, index) {
            return '<div class="border border-gray-200 rounded-lg p-4">' +
                '<div class="flex justify-between items-start mb-2">' +
                    '<span class="font-medium text-gray-700">申请 #' + (index + 1) + '</span>' +
                    '<span class="text-sm text-gray-500">' + formatDateTime(item.applyTime) + '</span>' +
                '</div>' +
                '<div class="grid grid-cols-1 md:grid-cols-2 gap-3 text-sm">' +
                    '<div><span class="font-medium text-gray-600">申请编号：</span><span class="text-gray-900">' + (item.applicationId || '-') + '</span></div>' +
                    '<div><span class="font-medium text-gray-600">预期返校时间：</span><span class="text-gray-900">' + formatDateTime(item.expectedReturnTime) + '</span></div>' +
                    '<div><span class="font-medium text-gray-600">审核状态：</span><span class="text-gray-900">' + getAuditStatusText(item.auditStatus) + '</span></div>' +
                    '<div><span class="font-medium text-gray-600">审核人：</span><span class="text-gray-900">' + (item.auditPerson || '-') + '</span></div>' +
                '</div>' +
                '<div class="mt-3">' +
                    '<span class="font-medium text-gray-600 block mb-1">申请原因：</span>' +
                    '<div class="bg-gray-50 p-3 rounded text-gray-900">' + (item.reason || '暂无原因说明') + '</div>' +
                '</div>' +
            '</div>';
        }
        
        // 更新申请列表
        function updateApplicationList(applications) {
            var html = '';
            if (applications.length === 0) {
                html = '<div class="text-center text-gray-500 py-8">暂无申请记录</div>';
            } else {
                applications.forEach(function(item, index) {
                    html += createApplicationItemHtml(item, index);
                });
            }
            $('#applicationList').html(html);
        }
        
        // 显示详情模态框
        function showDetailModal() {
            // 创建模态框HTML
            var modalHtml = '<div id="detailModal" class="fixed inset-0 bg-gray-600 bg-opacity-50 overflow-y-auto h-full w-full z-50">' +
                '<div class="relative top-20 mx-auto p-5 border w-11/12 max-w-6xl shadow-lg rounded-md bg-white">' +
                    '<div class="mt-3">' +
                        '<div class="flex justify-between items-center mb-6">' +
                            '<h2 class="text-2xl font-bold text-gray-800">晚归记录详情</h2>' +
                            '<button onclick="closeDetailModal()" class="text-gray-400 hover:text-gray-600">' +
                                '<i class="fas fa-times text-xl"></i>' +
                            '</button>' +
                        '</div>' +
                        '<div class="grid grid-cols-1 lg:grid-cols-3 gap-6">' +
                            '<div id="lateReturnDetail">' +
                                '<div class="text-center text-gray-500 py-8">正在加载晚归详情...</div>' +
                            '</div>' +
                            '<div id="explanationDetail">' +
                                '<div class="text-center text-gray-500 py-8">正在加载晚归说明...</div>' +
                            '</div>' +
                            '<div id="applicationDetail">' +
                                '<div class="text-center text-gray-500 py-8">正在加载晚归申请...</div>' +
                            '</div>' +
                        '</div>' +
                    '</div>' +
                '</div>' +
            '</div>';
            
            // 移除已存在的模态框
            $('#detailModal').remove();
            
            // 添加新模态框
            $('body').append(modalHtml);
        }
        
        // 关闭详情模态框
        function closeDetailModal() {
            $('#detailModal').remove();
        }
        
        // 获取审核状态文本
        function getAuditStatusText(status) {
            switch (status) {
                case 0: return '<span class="text-yellow-600">待审核</span>';
                case 1: return '<span class="text-green-600">已通过</span>';
                case 2: return '<span class="text-red-600">已驳回</span>';
                default: return '<span class="text-gray-600">未知状态</span>';
            }
        }
        
        function processRecord(lateReturnId) {
            // 处理记录逻辑
            console.log('处理记录:', lateReturnId);
            // 可以跳转到处理页面或打开处理模态框
        }

        function handleLogout() {
            if (confirm('确定要退出登录吗？')) {
                localStorage.removeItem('loginUUID');
                window.location.href = '${pageContext.request.contextPath}/logout';
            }
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