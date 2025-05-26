<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<!-- 根据用户职位角色定义 jobRole 只有班级管理员才有定义jobRole 宿管角色没有定义 -->
<c:set var="jobRole" value="" />
<c:choose>
    <c:when test="${fn:toLowerCase(sessionScope.role) eq 'systemuser'}">
        <c:set var="jobRole" value="${sessionScope.user.jobRole}" />
    </c:when>
    <c:when test="${fn:toLowerCase(sessionScope.role) eq 'dormitorymanager'}">
        <c:set var="jobRole" value="" />
    </c:when>
</c:choose>

<!-- 根据用户角色role定义 managerId 只有宿管这个角色才有managerId 另外的班级管理员没有定义managerId -->
<c:set var="managerId" value=""/>
<c:choose>
    <c:when test="${fn:toLowerCase(sessionScope.role) eq 'dormitorymanager'}">
        <c:if test="${sessionScope.user['class'].simpleName eq 'DormitoryManager'}">
            <c:set var="managerId" value="${sessionScope.user.managerId}"/>
        </c:if>
    </c:when>
    <c:when test="${fn:toLowerCase(sessionScope.role) eq 'systemuser'}">
        <c:set var="managerId" value=""/>
    </c:when>
</c:choose>

<!DOCTYPE html>
<html>
<head>
    <!-- 基层管理人员（宿管、辅导员、班主任）处理晚归说明页面 -->
    <title>学生晚归预警系统 - 晚归说明处理</title>
    <link href="https://cdn.bootcdn.net/ajax/libs/tailwindcss/2.2.19/tailwind.min.css" rel="stylesheet">
    <link href="https://cdn.bootcdn.net/ajax/libs/font-awesome/6.0.0/css/all.min.css" rel="stylesheet">
    <script src="https://cdn.bootcdn.net/ajax/libs/jquery/3.6.0/jquery.min.js"></script>
    <script src="${pageContext.request.contextPath}/static/js/college-mapping.js"></script>
    <style>
        .detail-card {
            background: white;
            border-radius: 0.5rem;
            box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
        }
        .timeline-item {
            position: relative;
            padding-left: 1.5rem;
        }
        .timeline-item::before {
            content: '';
            position: absolute;
            left: 0;
            top: 0;
            bottom: 0;
            width: 2px;
            background-color: #e5e7eb;
        }
        .timeline-dot {
            position: absolute;
            left: -4px;
            top: 0;
            width: 10px;
            height: 10px;
            border-radius: 50%;
            background-color: #3b82f6;
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
            background-color: #DBEAFE;
            color: #1E40AF;
        }
        .status-processed {
            background-color: #D1FAE5;
            color: #065F46;
        }
        .status-rejected {
            background-color: #FEE2E2;
            color: #991B1B;
        }
    </style>
</head>
<body class="bg-gray-50">
    <c:if test="${empty lateReturn}">
        <div class="alert alert-danger">
            未找到晚归记录信息
        </div>
    </c:if>

    <c:if test="${empty student}">
        <div class="alert alert-warning">
            未找到学生信息
        </div>
    </c:if>

    <c:if test="${empty dormitorymanager}"> 
        <div class="alert alert-danger">
            未找到宿管信息，请重新登录
        </div>
    </c:if>
    
    <div class="min-h-screen p-6">
        <!-- 顶部导航栏 -->
        <nav class="bg-white shadow-sm rounded-lg mb-6 p-4">
            <div class="flex justify-between items-center">
                <h1 class="text-xl font-semibold text-gray-800">学生晚归预警系统</h1>
                <div class="flex items-center space-x-4">
                    <a href="${pageContext.request.contextPath}/staff-dashboard" class="text-gray-600 hover:text-gray-900">返回列表</a>
                    <span class="text-gray-600">欢迎， ${sessionScope.user.name}
                         <c:choose>
                             <c:when test="${fn:toLowerCase(sessionScope.role) eq 'dormitorymanager'}">宿管</c:when>
                             <c:when test="${fn:toLowerCase(sessionScope.role) eq 'systemuser'}">
                                 <c:choose>
                                     <c:when test="${fn:toLowerCase(sessionScope.user.jobRole) eq 'counselor'}">辅导员</c:when>
                                     <c:when test="${fn:toLowerCase(sessionScope.user.jobRole) eq 'class_teacher'}">班主任</c:when>
                                     <c:when test="${fn:toLowerCase(sessionScope.user.jobRole) eq 'dean'}">院级领导</c:when>
                                     <c:otherwise>领导</c:otherwise>
                                 </c:choose>
                             </c:when>
                         </c:choose>
                    </span>
                    <button onclick="logout()" class="text-sm text-red-600 hover:text-red-800">退出登录</button>
                </div>
            </div>
        </nav>

        <!-- 页面标题和操作按钮 -->
        <div class="flex justify-between items-center mb-6">
            <h2 class="text-2xl font-bold text-gray-800">处理详情</h2>
            <div class="flex space-x-4">
                <button class="px-4 py-2 bg-indigo-600 text-white rounded-lg hover:bg-indigo-700">
                    发送通知
                </button>
                <button id="markAsProcessed" class="px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700">
                    标记为已处理
                </button>
            </div>
        </div>

        <!-- 学生基本信息 -->
        <div class="detail-card p-6 mb-6">
            <h3 class="text-lg font-medium text-gray-900 mb-4">学生基本信息</h3>
            <div class="grid grid-cols-1 md:grid-cols-3 gap-6" id="studentInfo">
                <div>
                    <p class="text-sm text-gray-500">学号</p>
                    <p class="text-base font-medium">${student.studentNo != null ? student.studentNo : '-'}</p>
                </div>
                <div>
                    <p class="text-sm text-gray-500">姓名</p>
                    <p class="text-base font-medium">${student.name != null ? student.name : '-'}</p>
                </div>
                <div>
                    <p class="text-sm text-gray-500">学院</p>
                    <p class="text-base font-medium">${student.college != null ? student.college : '-'}</p>
                </div>
                <div>
                    <p class="text-sm text-gray-500">宿舍号</p>
                    <p class="text-base font-medium">${student.dormitory != null ? student.dormitory : '-'}</p>
                </div>
                <div>
                    <p class="text-sm text-gray-500">班级</p>
                    <p class="text-base font-medium">${student.className != null ? student.className : '-'}</p>
                </div>
                <div>
                    <p class="text-sm text-gray-500">联系电话</p>
                    <p class="text-base font-medium">${student.phone != null ? student.phone : '-'}</p>
                </div>
            </div>
        </div>

         <!-- 家长联系方式 -->
         <div class="detail-card p-6 mb-6">
            <h3 class="text-lg font-medium text-gray-900 mb-4">家长联系方式</h3>
            <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div>
                    <p class="text-sm text-gray-500">父亲姓名</p>
                    <p class="text-base font-medium">${student.fatherName != null ? student.fatherName : '-'}</p>
                </div>
                <div>
                    <p class="text-sm text-gray-500">父亲电话</p>
                    <p class="text-base font-medium">${student.fatherPhone != null ? student.fatherPhone : '-'}</p>
                </div>
                <div>
                    <p class="text-sm text-gray-500">母亲姓名</p>
                    <p class="text-base font-medium">${student.motherName != null ? student.motherName : '-'}</p>
                </div>
                <div>
                    <p class="text-sm text-gray-500">母亲电话</p>
                    <p class="text-base font-medium">${student.motherPhone != null ? student.motherPhone : '-'}</p>
                </div>
            </div>
        </div>

        <!-- 晚归详情 -->
        <div class="detail-card p-6 mb-6">
            <h3 class="text-lg font-medium text-gray-900 mb-4">晚归详情</h3>
            <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div>
                    <p class="text-sm text-gray-500">晚归时间</p>
                    <p class="text-base font-medium">
                        <fmt:formatDate value="${lateReturn.lateTime}" pattern="yyyy-MM-dd HH:mm:ss"/>
                    </p>
                </div>
                <div>
                    <p class="text-sm text-gray-500">处理状态</p>
                    <span class="status-badge 
                        ${lateReturn.processStatus == 'PENDING' ? 'status-pending' : 
                          lateReturn.processStatus == 'PROCESSING' ? 'status-processing' : 
                          'status-processed'}">
                        ${lateReturn.processStatus == 'PENDING' ? '待处理' : 
                          lateReturn.processStatus == 'PROCESSING' ? '处理中' : 
                          '已完成'}
                    </span>
                </div>
                <div>
                    <p class="text-sm text-gray-500">晚归原因</p>
                    <p class="text-base">${lateReturn.reason != null ? lateReturn.reason : '未提供'}</p>
                </div>
                <div>
                    <p class="text-sm text-gray-500">处理结果</p>
                    <p class="text-base">
                        ${lateReturn.processResult == null ? '-' : 
                          lateReturn.processResult == 'APPROVED' ? '通过' : 
                          lateReturn.processResult == 'REJECTED' ? '拒绝' : '？'}
                    </p>
                </div>
            </div>
        </div>

        <!-- 门禁记录 -->
        <div class="detail-card p-6 mb-6">
            <h3 class="text-lg font-medium text-gray-900 mb-4">门禁记录</h3>
            <div class="mb-4">
                <p class="text-sm text-gray-500 mb-2">门禁截图</p>
                <div class="border border-gray-200 rounded-lg p-4 bg-gray-50">
                    <div class="flex items-center justify-center h-40">
                        <p class="text-gray-400">门禁记录截图</p>
                    </div>
                </div>
            </div>
            <div>
                <p class="text-sm text-gray-500 mb-2">门禁记录时间线</p>
                <div class="space-y-4">
                    <div class="timeline-item">
                        <div class="timeline-dot"></div>
                        <div class="text-sm">
                            <p class="font-medium">进入宿舍楼</p>
                            <p class="text-gray-500">2024-04-14 23:45:12</p>
                        </div>
                    </div>
                    <div class="timeline-item">
                        <div class="timeline-dot"></div>
                        <div class="text-sm">
                            <p class="font-medium">离开宿舍楼</p>
                            <p class="text-gray-500">2024-04-14 18:30:05</p>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <!-- 历史晚归记录 -->
        <div class="detail-card p-6 mb-6">
            <h3 class="text-lg font-medium text-gray-900 mb-4">历史晚归记录</h3>
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
                        </tr>
                    </thead>
                    <tbody id="historyTableBody" class="bg-white divide-y divide-gray-200">
                        <!-- 数据将通过AJAX动态加载 -->
                    </tbody>
                </table>
                <!-- 分页控件 -->
                <div class="flex justify-between items-center mt-4">
                    <div class="text-sm text-gray-500">
                        共 <span id="totalCount">0</span> 条记录
                    </div>
                    <div class="flex space-x-2">
                        <button id="prevPage" class="px-3 py-1 border rounded hover:bg-gray-100">上一页</button>
                        <span id="currentPage" class="px-3 py-1">1</span>
                        <button id="nextPage" class="px-3 py-1 border rounded hover:bg-gray-100">下一页</button>
                    </div>
                </div>
            </div>
        </div>

        <!-- 晚归情况说明 -->
        <div class="detail-card p-6 mb-6 relative">
            <h3 class="text-lg font-medium text-gray-900 mb-4">晚归情况说明</h3>
            <div class="absolute right-6 top-6 flex space-x-2 z-10">
                <button id="approveBtn" class="px-4 py-2 rounded-md bg-blue-100 hover:bg-blue-200 text-blue-700 font-medium transition-colors duration-200 border border-blue-200 disabled:opacity-50 disabled:cursor-not-allowed"
                        onclick="handleAuditAction('APPROVE')">
                    通过
                </button>
                <button id="rejectBtn" class="px-4 py-2 rounded-md bg-red-100 hover:bg-red-200 text-red-700 font-medium transition-colors duration-200 border border-red-200 disabled:opacity-50 disabled:cursor-not-allowed"
                        onclick="handleAuditAction('REJECT')">
                    拒绝
                </button>
                <button id="forwardBtn" class="px-4 py-2 rounded-md bg-amber-100 hover:bg-amber-200 text-amber-700 font-medium transition-colors duration-200 border border-amber-200 disabled:opacity-50 disabled:cursor-not-allowed"
                        onclick="handleAuditAction('FORWARD')">
                    转发
                </button>
                <div id="actionStatus" class="hidden ml-4 px-4 py-2 rounded-md bg-green-100 text-green-700">
                    操作成功！
                </div>
            </div>
            <div id="explanationContent">
                <div class="animate-pulse">
                    <div class="h-4 bg-gray-200 rounded w-3/4 mb-4"></div>
                    <div class="h-4 bg-gray-200 rounded w-1/2"></div>
                </div>
            </div>
        </div>

          <!-- 审核备注 -->
          <div class="detail-card p-6 mb-6">
            <h3 class="text-lg font-medium text-gray-900 mb-4">审核备注</h3>
            <div class="mb-4">
                <textarea id="auditRemark" maxlength="150" class="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-indigo-500 focus:border-indigo-500" rows="4" placeholder="请输入审核备注...（最多150字）" oninput="updateRemarkCount()"></textarea>
                <div class="text-right text-sm text-gray-500 mt-1">
                    <span id="remarkCount">0</span>/150 字
                </div>
            </div>
          </div>
          <!-- 处理备注 -->
          <div class="detail-card p-6 mb-6">
            <h3 class="text-lg font-medium text-gray-900 mb-4">处理备注</h3>
            <div class="mb-4">
                <textarea id="processRemark" maxlength="150" class="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-indigo-500 focus:border-indigo-500" rows="4" placeholder="请输入处理备注...（最多150字）" oninput="updateProcessRemarkCount()"></textarea>
                <div class="text-right text-sm text-gray-500 mt-1">
                    <span id="processRemarkCount">0</span>/150 字
                </div>
            </div>
          </div>
       
    </div>

    <!-- 非Ajax形式请求的错误弹框提示 -->
    <c:if test="${not empty sessionScope.errorMsg}">
        <script>
            alert("${fn:escapeXml(sessionScope.errorMsg)}");
        </script>
        <c:remove var="errorMsg" scope="session" />
    </c:if>

    <script>
        // 添加调试信息
        console.log('Context Path:', '${pageContext.request.contextPath}');
        
        function logout() {
            if (confirm('确定要退出登录吗？')) {
                window.location.href = '${pageContext.request.contextPath}/logout';
            }
        }

        // 标记为已处理
        document.getElementById('markAsProcessed').addEventListener('click', function() {
            const button = this;
            if (confirm('确定要标记为已处理吗？')) {
                // 禁用按钮并显示加载状态
                button.disabled = true;
                button.textContent = '处理中...';

                const lateReturnId = '${sessionScope.lateReturn.lateReturnId}';
                const params = new URLSearchParams({
                    lateReturnId: lateReturnId,
                    processStatus: 'FINISHED',
                    processResult: '',
                    processRemark: ''
                });

                fetch('${pageContext.request.contextPath}/late-return/update', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/x-www-form-urlencoded',
                    },
                    body: params
                })
                    .then(response => response.text())
                    .then(result => {
                        if (result === 'success') {
                            alert('标记成功');
                            window.location.reload();
                        } else {
                            alert('标记失败，请重试');
                            // 恢复按钮状态
                            button.disabled = false;
                            button.textContent = '标记为已处理';
                        }
                    })
                    .catch(error => {
                        console.error('Error:', error);
                        alert('操作失败，请重试');
                        // 恢复按钮状态
                        button.disabled = false;
                        button.textContent = '标记为已处理';
                    });
            }
        });

        // 获取状态样式类
        function getStatusClass(status) {
            switch(status) {
                case 0: return 'status-pending';
                case 1: return 'status-processed';
                case 2: return 'status-rejected';
                default: return '';
            }
        }

        // 获取状态文本
        function getStatusText(status) {
            switch(status) {
                case 0: return '待审核';
                case 1: return '已通过';
                case 2: return '已驳回';
                default: return '-';
            }
        }

        // 格式化时间戳为 yyyy-MM-dd HH:mm:ss
        function formatTimestamp(timestamp) {
            if (!timestamp) return '-';
            
            // 如果已经是日期字符串格式，直接返回
            if (typeof timestamp === 'string' && timestamp.match(/^\d{4}-\d{2}-\d{2}/)) {
                return timestamp;
            }
            
            // 处理时间戳
            var date;
            if (typeof timestamp === 'string') {
                // 如果是字符串格式的时间戳，确保使用完整的毫秒数
                date = new Date(parseInt(timestamp));
            } else {
                date = new Date(timestamp);
            }
            
            // 检查日期是否有效
            if (isNaN(date.getTime())) {
                return '-';
            }
            
            var year = date.getFullYear();
            var month = String(date.getMonth() + 1).padStart(2, '0');
            var day = String(date.getDate()).padStart(2, '0');
            var hours = String(date.getHours()).padStart(2, '0');
            var minutes = String(date.getMinutes()).padStart(2, '0');
            var seconds = String(date.getSeconds()).padStart(2, '0');
            return year + '-' + month + '-' + day + ' ' + hours + ':' + minutes + ':' + seconds;
        }

        // 构建说明内容HTML
        function buildExplanationContent(explanation) {
            var html = '';
            
            // 说明内容
            html += '<div class="space-y-4">';
            html += '<div>';
            html += '<p class="text-sm text-gray-500 mb-2">情况说明</p>';
            html += '<p class="text-base">' + (explanation.description || '-') + '</p>';
            html += '</div>';
            
            // 提交信息
            html += '<div>';
            html += '<p class="text-sm text-gray-500 mb-2">提交时间</p>';
            html += '<p class="text-base">' + formatTimestamp(explanation.submitTime) + '</p>';
            html += '</div>';
            
            // 附件
            if (explanation.attachmentUrl) {
                var newUrl = explanation.attachmentUrl;
            
                // 构建下载和预览链接
                var downloadUrl = '${pageContext.request.contextPath}/file/download' + newUrl;
                var previewUrl = '${pageContext.request.contextPath}/file/preview' + newUrl;
                
                html += '<div>';
                html += '<p class="text-sm text-gray-500 mb-2">附件</p>';
                html += '<div class="flex space-x-4">';
                // 预览按钮
                html += '<a href="javascript:void(0)" onclick="previewFile(\'' + previewUrl + '\')" class="text-blue-600 hover:text-blue-800">';
                html += '<i class="fas fa-eye mr-1"></i>预览附件';
                html += '</a>';
                // 下载按钮
                html += '<a href="' + downloadUrl + '" class="text-indigo-600 hover:text-indigo-800">';
                html += '<i class="fas fa-download mr-1"></i>下载附件';
                html += '</a>';
                html += '</div>';
                html += '</div>';
            }
            
            // 审核信息
            html += '<div class="border-t pt-4">';
            html += '<p class="text-sm text-gray-500 mb-2">审核状态</p>';
            html += '<span class="status-badge ' + getStatusClass(explanation.auditStatus) + '">';
            html += getStatusText(explanation.auditStatus);
            html += '</span>';
            html += '</div>';
            
            // 审核详情
            html += '<div class="grid grid-cols-1 md:grid-cols-2 gap-4">';
            html += '<div>';
            html += '<p class="text-sm text-gray-500">审核人</p>';
            html += '<p class="text-base">' + (explanation.auditPerson || '-') + '</p>';
            html += '</div>';
            html += '<div>';
            html += '<p class="text-sm text-gray-500">审核时间</p>';
            html += '<p class="text-base">' + formatTimestamp(explanation.auditTime) + '</p>';
            html += '</div>';
            html += '<div class="md:col-span-2">';
            html += '<p class="text-sm text-gray-500">审核备注</p>';
            html += '<p class="text-base">' + (explanation.auditRemark || '-') + '</p>';
            html += '</div>';
            html += '</div>';
            
            html += '</div>';
            return html;
        }

        // 加载晚归情况说明
        function loadExplanation(lateReturnId) {
            $.ajax({
                url: '${pageContext.request.contextPath}/explanation/late-return/' + lateReturnId,
                type: 'GET',
                success: function(response) {
                    if (response.success) {
                        var explanation = response.data;
                        if (explanation) {
                            window.currentExplanation = explanation;
                            window.currentExplanationId = explanation.explanationId;
                            $('#explanationContent').html(buildExplanationContent(explanation));
                        } else {
                            $('#explanationContent').html('<p class="text-gray-500">暂无说明</p>');
                        }
                    } else {
                        $('#explanationContent').html('<p class="text-red-500">获取说明信息失败：' + response.message + '</p>');
                    }
                },
                error: function(xhr, status, error) {
                    $('#explanationContent').html('<p class="text-red-500">获取说明信息失败：' + error + '</p>');
                }
            });
        }

        // 页面加载完成后执行
        $(document).ready(function() {
            var lateReturnId = '${lateReturn.lateReturnId}';
            if (lateReturnId) {
                loadExplanation(lateReturnId);
            }
            
            // 加载历史晚归记录
            loadHistoryRecords(1);
            
            // 绑定分页按钮事件
            $('#prevPage').click(function() {
                var currentPage = parseInt($('#currentPage').text());
                if (currentPage > 1) {
                    loadHistoryRecords(currentPage - 1);
                }
            });
            
            $('#nextPage').click(function() {
                var currentPage = parseInt($('#currentPage').text());
                loadHistoryRecords(currentPage + 1);
            });
        });

        // 获取本月开始和结束时间
        function getCurrentMonthRange() {
            var now = new Date();
            var firstDay = new Date(now.getFullYear(), now.getMonth(), 1);
            var lastDay = new Date(now.getFullYear(), now.getMonth() + 1, 0);
            
            // 设置时间为当天的开始和结束
            firstDay.setHours(0, 0, 0, 0);
            lastDay.setHours(23, 59, 59, 999);
            
            // 格式化日期为 yyyy-MM-dd HH:mm:ss
            function formatDate(date) {
                var year = date.getFullYear();
                var month = String(date.getMonth() + 1).padStart(2, '0');
                var day = String(date.getDate()).padStart(2, '0');
                var hours = String(date.getHours()).padStart(2, '0');
                var minutes = String(date.getMinutes()).padStart(2, '0');
                var seconds = String(date.getSeconds()).padStart(2, '0');
                return year + '-' + month + '-' + day + ' ' + hours + ':' + minutes + ':' + seconds;
            }
            
            return {
                start: formatDate(firstDay),
                end: formatDate(lastDay)
            };
        }

        // 构建状态标签HTML
        function buildStatusBadge(status) {
            var statusClass = '';
            var statusText = '';
            
            switch(status) {
                case 'PENDING':
                    statusClass = 'status-pending';
                    statusText = '待处理';
                    break;
                case 'PROCESSING':
                    statusClass = 'status-processing';
                    statusText = '处理中';
                    break;
                case 'FINISHED':
                    statusClass = 'status-processed';
                    statusText = '已完成';
                    break;
                default:
                    statusClass = '';
                    statusText = '-';
            }
            
            return '<span class="status-badge ' + statusClass + '">' + statusText + '</span>';
        }

        // 构建处理结果文本
        function getProcessResultText(result) {
            switch(result) {
                case 'APPROVED':
                    return '通过';
                case 'REJECTED':
                    return '拒绝';
                default:
                    return '-';
            }
        }

        // 格式化日期为 yyyy-MM-dd HH:mm:ss 星期几
        function formatDateTime(dateStr) {
            var date = new Date(dateStr);
            var year = date.getFullYear();
            var month = String(date.getMonth() + 1).padStart(2, '0');
            var day = String(date.getDate()).padStart(2, '0');
            var hours = String(date.getHours()).padStart(2, '0');
            var minutes = String(date.getMinutes()).padStart(2, '0');
            var seconds = String(date.getSeconds()).padStart(2, '0');
            
            // 获取星期几
            var weekdays = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六'];
            var weekday = weekdays[date.getDay()];
            
            return year + '-' + month + '-' + day + ' ' + hours + ':' + minutes + ':' + seconds + ' ' + weekday;
        }

        // 加载历史晚归记录
        <!-- todo 这里没有显示processStatus为FINISHED, 而processResult为null的晚归记录-->
        function loadHistoryRecords(pageNum) {
            var monthRange = getCurrentMonthRange();
            var query = {
                studentNo: '${student.studentNo}',
                startLateTime: monthRange.start,
                endLateTime: monthRange.end,
                pageNum: pageNum,
                pageSize: 10,
                processStatus: 'FINISHED',
                processResults: ['REJECTED', null]
            };

            $.ajax({
                url: '${pageContext.request.contextPath}/late-return/pageList',
                type: 'POST',
                contentType: 'application/json',
                data: JSON.stringify(query),
                success: function(response) {
                    if (response.success) {
                        var pageResult = response.data;  // 获取第一层data
                        var html = '';
                        
                        // 更新分页信息
                        $('#totalCount').text(pageResult.total);
                        $('#currentPage').text(pageNum);
                        
                        // 更新表格内容
                        if (pageResult.data && pageResult.data.length > 0) {  // 使用第二层data
                            pageResult.data.forEach(function(record) {
                                html += '<tr>';
                                html += '<td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">' + 
                                       formatDateTime(record.lateTime) + '</td>';
                                html += '<td class="px-6 py-4 whitespace-nowrap">' + 
                                       buildStatusBadge(record.processStatus) + '</td>';
                                html += '<td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">' + 
                                       getProcessResultText(record.processResult) + '</td>';
                                html += '</tr>';
                            });
                        } else {
                            html = '<tr><td colspan="3" class="px-6 py-4 text-center text-gray-500">暂无记录</td></tr>';
                        }
                        
                        $('#historyTableBody').html(html);
                        
                        // 更新分页按钮状态
                        $('#prevPage').prop('disabled', !pageResult.hasPrev);
                        $('#nextPage').prop('disabled', !pageResult.hasNext);
                    } else {
                        $('#historyTableBody').html('<tr><td colspan="3" class="px-6 py-4 text-center text-red-500">' + 
                                                  '加载失败：' + response.message + '</td></tr>');
                    }
                },
                error: function(xhr, status, error) {
                    $('#historyTableBody').html('<tr><td colspan="3" class="px-6 py-4 text-center text-red-500">' + 
                                              '加载失败：' + error + '</td></tr>');
                }
            });
        }

        // 修改预览文件的函数
        function previewFile(url) {
            // 创建模态框
            var modalHtml = 
                '<div id="previewModal" class="fixed inset-0 bg-gray-600 bg-opacity-50 overflow-y-auto h-full w-full">' +
                    '<div class="relative top-20 mx-auto p-5 border w-4/5 shadow-lg rounded-md bg-white" style="min-height:75vh;">' +
                        '<div class="flex justify-between items-center mb-4">' +
                            '<h3 class="text-lg font-medium">文件预览</h3>' +
                            '<button onclick="closePreviewModal()" class="text-gray-400 hover:text-gray-500">' +
                                '<i class="fas fa-times"></i>' +
                            '</button>' +
                        '</div>' +
                        '<div id="previewContent" class="w-full" style="height:70vh;">' +
                            '<div class="flex items-center justify-center h-full">' +
                                '<div class="animate-spin rounded-full h-12 w-12 border-b-2 border-gray-900"></div>' +
                            '</div>' +
                        '</div>' +
                    '</div>' +
                '</div>';
            
            // 添加模态框到页面
            document.body.insertAdjacentHTML('beforeend', modalHtml);
            
            // 加载预览内容
            var previewContent = document.getElementById('previewContent');
            // 使用img标签预览图片
            if (url.toLowerCase().match(/\.(jpg|jpeg|png|gif)$/)) {
                previewContent.innerHTML = '<img src="' + url + '" style="max-width:100%;max-height:100%;object-contain;height:68vh;display:block;margin:auto;" alt="预览图片">';
            } else {
                // 其他文件类型使用iframe
                previewContent.innerHTML = '<iframe src="' + url + '" class="w-full" style="height:68vh;border:none;" frameborder="0"></iframe>';
            }
        }

        // 关闭预览模态框
        function closePreviewModal() {
            var modal = document.getElementById('previewModal');
            if (modal) {
                modal.remove();
            }
        }

        function handleAuditAction(action) {
            // 禁用所有按钮
            document.getElementById('approveBtn').disabled = true;
            document.getElementById('rejectBtn').disabled = true;
            document.getElementById('forwardBtn').disabled = true;

            // 获取审核备注
            var auditRemark = document.getElementById('auditRemark').value.trim();
            if (auditRemark.length > 150) {
                alert('审核备注不能超过150字');
                // 恢复按钮状态
                document.getElementById('approveBtn').disabled = false;
                document.getElementById('rejectBtn').disabled = false;
                document.getElementById('forwardBtn').disabled = false;
                return;
            }
            // 获取处理备注
            var processRemark = document.getElementById('processRemark').value.trim();
            if (processRemark.length > 150) {
                alert('处理备注不能超过150字');
                // 恢复按钮状态
                document.getElementById('approveBtn').disabled = false;
                document.getElementById('rejectBtn').disabled = false;
                document.getElementById('forwardBtn').disabled = false;
                return;
            }

            // 获取当前用户工号，忽略role大小写
            var role = '${sessionScope.role}'.toLowerCase();
            var jobRole = '${jobRole}';  // 使用 JSP 中定义的 jobRole 变量
            var auditPerson = '${auditPerson}';

            
            // 审核状态
            var auditStatus = 0;
            var processStatus = '';
            var processResult = '';
            if (action === 'APPROVE') {
                auditStatus = 1;
                processStatus = 'FINISHED';
                processResult = 'APPROVED';
            } else if (action === 'REJECT') {
                auditStatus = 2;
                processStatus = 'FINISHED';
                processResult = 'REJECTED';
            } else if (action === 'FORWARD') {
                auditStatus = 0;
                processStatus = 'PROCESSING';
                processResult = '';
            }
            // 获取当前说明ID和explanationId
            var lateReturnId = '${lateReturn.lateReturnId}';
            var explanationId = window.currentExplanationId || null;
            if (!explanationId) {
                if (window.currentExplanation && window.currentExplanation.explanationId) {
                    explanationId = window.currentExplanation.explanationId;
                }
            }
            if (!explanationId) {
                alert('未获取到晚归情况说明ID（explanationId）');
                // 恢复按钮状态
                document.getElementById('approveBtn').disabled = false;
                document.getElementById('rejectBtn').disabled = false;
                document.getElementById('forwardBtn').disabled = false;
                return;
            }
            // 一次性封装所有参数
            var auditActionData = {
                auditPerson: auditPerson,
                auditRemark: auditRemark,
                auditStatus: auditStatus,
                lateReturnId: lateReturnId,
                explanationId: explanationId,
                processStatus: processStatus,
                processResult: processResult,
                processRemark: processRemark,
                studentNo: '${student.studentNo}',
                jobRole: jobRole // 获取登录该页面的审核者的职位角色 例如辅导员或班主任 如果是宿管则为空
            };
            // 只发一次请求到新的/audit-action接口
            $.ajax({
                url: '${pageContext.request.contextPath}/explanation/audit-action',
                type: 'POST',
                contentType: 'application/json',
                data: JSON.stringify(auditActionData),
                success: function(response) {
                    if (response.success) {
                        // 显示成功提示
                        var statusDiv = document.getElementById('actionStatus');
                        statusDiv.classList.remove('hidden');
                        statusDiv.textContent = '操作成功！';
                        statusDiv.className = 'ml-4 px-4 py-2 rounded-md bg-green-100 text-green-700';
                        
                        // 3秒后刷新页面
                        setTimeout(function() {
                            location.reload();
                        }, 3000);
                    } else {
                        alert('操作失败：' + (response.message || '未知错误'));
                        // 恢复按钮状态
                        document.getElementById('approveBtn').disabled = false;
                        document.getElementById('rejectBtn').disabled = false;
                        document.getElementById('forwardBtn').disabled = false;
                    }
                },
                error: function(xhr, status, error) {
                    alert('请求失败：' + error);
                    // 恢复按钮状态
                    document.getElementById('approveBtn').disabled = false;
                    document.getElementById('rejectBtn').disabled = false;
                    document.getElementById('forwardBtn').disabled = false;
                }
            });
        }

        function updateRemarkCount() {
            var textarea = document.getElementById('auditRemark');
            var count = textarea.value.length;
            document.getElementById('remarkCount').innerText = count;
        }

        function updateProcessRemarkCount() {
            var textarea = document.getElementById('processRemark');
            var count = textarea.value.length;
            document.getElementById('processRemarkCount').innerText = count;
        }
    </script>
</body>
</html> 