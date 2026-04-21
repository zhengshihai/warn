<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<!DOCTYPE html>
<html>
<head>
    <title>学生晚归预警系统 - 系统管理员主页</title>
<%--    <link href="https://cdn.bootcdn.net/ajax/libs/font-awesome/6.0.0/css/all.min.css" rel="stylesheet">--%>

    <!-- 引入 CSS 文件 -->
    <link href="${pageContext.request.contextPath}/static/css/tailwind.min.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/static/css/fontawesome.min.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/static/css/bootstrap.min.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/static/css/all.min.css" rel="stylesheet">

    <!-- 引入 JS 文件 -->
    <script src="${pageContext.request.contextPath}/static/js/jquery.min.js"></script>
    <script src="${pageContext.request.contextPath}/static/js/popper.min.js"></script>
    <script src="${pageContext.request.contextPath}/static/js/echarts.min.js"></script>
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
    </style>
</head>
<body>
    <div class="dashboard-container p-6">
        <!-- 顶部导航栏 -->
        <nav class="bg-white shadow-sm rounded-lg mb-6 p-4">
            <div class="flex justify-between items-center">
                <h1 class="text-xl font-semibold text-gray-800">学生晚归预警系统</h1>
                <div class="flex items-center space-x-4">
                    <span class="text-gray-600">欢迎，${sessionScope.user.name} </span>
                    <button class="text-sm text-blue-600 hover:text-blue-800" data-bs-toggle="modal" data-bs-target="#editProfileModal">修改个人信息</button>
                    <button class="text-sm text-red-600 hover:text-red-800" onclick="handleLogout()">退出登录</button>
                </div>
            </div>
        </nav>

        <!-- 修改个人信息模态框 -->
        <div class="modal fade" id="editProfileModal" tabindex="-1" aria-labelledby="editProfileModalLabel" aria-hidden="true">
            <div class="modal-dialog">
                <div class="modal-content">
                    <div class="modal-header">
                        <h5 class="modal-title" id="editProfileModalLabel">修改个人信息</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                    </div>
                    <div class="modal-body">
                        <form id="editProfileForm">
                            <div class="mb-3">
                                <label for="sysUserNo" class="form-label">工号</label>
                                <input type="text" class="form-control" id="sysUserNo" name="sysUserNo" value="${sessionScope.user.sysUserNo}" required>
                            </div>
                            <div class="mb-3">
                                <label for="name" class="form-label">姓名</label>
                                <input type="text" class="form-control" id="name" name="name" value="${name}" required>
                            </div>
                            <div class="mb-3">
                                <label for="phone" class="form-label">联系电话</label>
                                <input type="tel" class="form-control" id="phone" name="phone" required>
                            </div>
                            <div class="row mb-3">
                                <div class="col-md-6">
                                    <label for="email" class="form-label">电子邮箱</label>
                                    <input type="email" class="form-control" id="email" name="email" value="${email}" required>
                                </div>
                            </div>
                            <div class="mb-3">
                                <label for="password" class="form-label">新密码</label>
                                <input type="password" class="form-control" id="password" name="password">
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

<%--        <!-- 统计卡片 -->--%>
<%--        <div class="grid grid-cols-1 md:grid-cols-3 gap-6 mb-6">--%>
<%--            <div class="card p-6">--%>
<%--                <h3 class="text-lg font-medium text-gray-900">今日晚归人数</h3>--%>
<%--                <p class="text-3xl font-bold text-indigo-600 mt-2">12</p>--%>
<%--                <p class="text-sm text-gray-500 mt-1">较昨日 +2</p>--%>
<%--            </div>--%>
<%--            <div class="card p-6">--%>
<%--                <h3 class="text-lg font-medium text-gray-900">本月累计</h3>--%>
<%--                <p class="text-3xl font-bold text-green-600 mt-2">156</p>--%>
<%--                <p class="text-sm text-gray-500 mt-1">较上月 -23</p>--%>
<%--            </div>--%>
<%--            <div class="card p-6">--%>
<%--                <h3 class="text-lg font-medium text-gray-900">本月晚归增幅</h3>--%>
<%--                <p class="text-3xl font-bold text-red-600 mt-2">3%</p>--%>
<%--                <p class="text-sm text-gray-500 mt-1">较上月</p>--%>
<%--            </div>--%>
<%--        </div>--%>

<%--        <!-- 图表和时间线 -->--%>
<%--        <div class="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">--%>
<%--            <!-- 学院分布图 -->--%>
<%--            <div class="card p-6">--%>
<%--                <h3 class="text-lg font-medium text-gray-900 mb-4">学院分布</h3>--%>
<%--                <div id="collegeChart" style="height: 300px;"></div>--%>
<%--            </div>--%>
<%--            --%>
<%--            <!-- 最近预警动态 -->--%>
<%--            <div class="card p-6">--%>
<%--                <h3 class="text-lg font-medium text-gray-900 mb-4">最近预警动态</h3>--%>
<%--                <div class="space-y-4">--%>
<%--                    <div class="timeline-item">--%>
<%--                        <div class="timeline-dot"></div>--%>
<%--                        <div class="text-sm">--%>
<%--                            <p class="font-medium">张三 - 计算机学院</p>--%>
<%--                            <p class="text-gray-500">23:45 返回宿舍</p>--%>
<%--                        </div>--%>
<%--                    </div>--%>
<%--                    <div class="timeline-item">--%>
<%--                        <div class="timeline-dot"></div>--%>
<%--                        <div class="text-sm">--%>
<%--                            <p class="font-medium">李四 - 经济学院</p>--%>
<%--                            <p class="text-gray-500">23:30 返回宿舍</p>--%>
<%--                        </div>--%>
<%--                    </div>--%>
<%--                    <div class="timeline-item">--%>
<%--                        <div class="timeline-dot"></div>--%>
<%--                        <div class="text-sm">--%>
<%--                            <p class="font-medium">王五 - 医学院</p>--%>
<%--                            <p class="text-gray-500">23:15 返回宿舍</p>--%>
<%--                        </div>--%>
<%--                    </div>--%>
<%--                </div>--%>
<%--            </div>--%>
<%--        </div>--%>

        <!-- 操作选项 -->
        <div class="card p-6 mb-6">
            <h3 class="text-lg font-medium text-gray-900 mb-4">操作选项</h3>
            <div class="grid grid-cols-2 md:grid-cols-4 gap-4">
                <button id="btn-late-records" class="p-4 bg-indigo-50 rounded-lg hover:bg-indigo-100 transition-colors">
                    <span class="block text-indigo-600 font-medium">查看晚归名单</span>
                </button>
                <button id="btn-reports" class="p-4 bg-green-50 rounded-lg hover:bg-green-100 transition-colors">
                    <span class="block text-green-600 font-medium">导出报告</span>
                </button>
                <button id="btn-messages" class="p-4 bg-yellow-50 rounded-lg hover:bg-yellow-100 transition-colors">
                    <span class="block text-yellow-600 font-medium">发送通知</span>
                </button>
                <button id="btn-rule" class="p-4 bg-purple-50 rounded-lg hover:bg-purple-100 transition-colors">
                    <span class="block text-purple-600 font-medium">系统设置</span>
                </button>
            </div>
        </div>

        <!-- 通知管理 -->
        <div class="card p-6">
            <h3 class="text-lg font-medium text-gray-900 mb-4">通知管理</h3>

            <!-- 发布通知区域 -->
            <div class="mb-6 border-b pb-4">
                <h4 class="text-md font-semibold text-gray-800 mb-3">发布通知</h4>

                <!-- 通知类型选择 -->
                <div class="mb-3">
                    <label class="form-label block mb-1 text-sm text-gray-700">通知类型</label>
                    <div class="flex items-center space-x-4">
                        <label class="inline-flex items-center">
                            <input type="radio" name="notifyType" id="notifyTypeSingle" value="single" class="form-radio" checked>
                            <span class="ml-2 text-sm text-gray-700">单个目标用户</span>
                        </label>
                        <label class="inline-flex items-center">
                            <input type="radio" name="notifyType" id="notifyTypeAll" value="all" class="form-radio">
                            <span class="ml-2 text-sm text-gray-700">全体用户</span>
                        </label>
                    </div>
                </div>

                <!-- 目标ID -->
                <div class="mb-3" id="notifyTargetIdGroup">
                    <label for="notifyTargetId" class="form-label text-sm text-gray-700">目标ID（学号/工号等）</label>
                    <input id="notifyTargetId"
                           type="text"
                           class="form-control"
                           placeholder="请输入目标用户的业务ID">
                    <p class="text-xs text-gray-500 mt-1">当选择“单个目标用户”时必填；选择“全体用户”时会忽略。</p>
                </div>

                <!-- 标题 -->
                <div class="mb-3">
                    <label for="notifyTitle" class="form-label text-sm text-gray-700">通知标题</label>
                    <input id="notifyTitle"
                           type="text"
                           class="form-control"
                           placeholder="请输入通知标题">
                </div>

                <!-- 内容 -->
                <div class="mb-3">
                    <label for="notifyContent" class="form-label text-sm text-gray-700">通知内容</label>
                    <textarea id="notifyContent"
                              class="form-control"
                              rows="3"
                              placeholder="请输入通知内容"></textarea>
                </div>

                <div class="text-right">
                    <button id="publishNotificationBtn"
                            type="button"
                            class="px-4 py-2 bg-green-600 text-white rounded hover:bg-green-700">
                        发布通知
                    </button>
                </div>
            </div>

            <!-- 搜索区域 -->
            <div class="flex flex-col md:flex-row md:items-center md:space-x-4 mb-4 space-y-3 md:space-y-0">
                <div class="flex-1">
                    <input id="notificationSearchInput"
                           type="text"
                           class="form-control"
                           placeholder="请输入通知内容关键字进行搜索">
                </div>
                <div class="flex space-x-2">
                    <button id="notificationSearchBtn"
                            class="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700">
                        搜索
                    </button>
                    <button id="notificationResetBtn"
                            class="px-4 py-2 bg-gray-200 text-gray-700 rounded hover:bg-gray-300">
                        重置
                    </button>
                </div>
            </div>

            <!-- 通知列表表格 -->
            <div class="overflow-x-auto">
                <table class="min-w-full divide-y divide-gray-200">
                    <thead class="bg-gray-50">
                    <tr>
                        <th class="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">通知ID</th>
                        <th class="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">标题</th>
                        <th class="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">内容</th>
                        <th class="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">目标ID</th>
                        <th class="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">更新时间</th>
                        <th class="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">操作</th>
                    </tr>
                    </thead>
                    <tbody id="notificationTableBody" class="bg-white divide-y divide-gray-200">
                    <!-- 通知数据行通过JS填充 -->
                    </tbody>
                </table>
            </div>

            <!-- 分页区域 -->
            <div class="flex flex-col md:flex-row md:items-center md:justify-between mt-4 space-y-3 md:space-y-0">
                <div class="text-sm text-gray-600">
                    当前第 <span id="notificationCurrentPageText">1</span> 页，
                    共 <span id="notificationTotalPagesText">1</span> 页
                </div>
                <div class="flex items-center space-x-2">
                    <button class="px-3 py-1 border rounded text-sm"
                            onclick="changeNotificationPage('prev')">
                        上一页
                    </button>
                    <button class="px-3 py-1 border rounded text-sm"
                            onclick="changeNotificationPage('next')">
                        下一页
                    </button>
                    <select id="notificationPageSizeSelect"
                            class="form-select form-select-sm w-24"
                            onchange="changeNotificationPageSize(this.value)">
                        <option value="10">10条/页</option>
                        <option value="20">20条/页</option>
                        <option value="50">50条/页</option>
                    </select>
                </div>
            </div>
        </div>

        <!-- 报警视频列表 -->
        <div class="card p-6 mt-6">
            <h3 class="text-lg font-medium text-gray-900 mb-4">报警视频列表</h3>

            <!-- 报警视频搜索区域 -->
            <div class="flex flex-col md:flex-row md:items-center md:space-x-4 mb-4 space-y-3 md:space-y-0">
                <div class="flex-1 space-y-2">
                    <input id="alarmVideoStudentNoInput"
                           type="text"
                           class="form-control"
                           placeholder="按学号搜索（可选）">
                    <input id="alarmVideoAlarmNoInput"
                           type="text"
                           class="form-control"
                           placeholder="按报警编号搜索（可选）">
                </div>
                <div class="flex space-x-2">
                    <button id="alarmVideoSearchBtn"
                            class="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700">
                        搜索
                    </button>
                    <button id="alarmVideoResetBtn"
                            class="px-4 py-2 bg-gray-200 text-gray-700 rounded hover:bg-gray-300">
                        重置
                    </button>
                </div>
            </div>

            <div class="overflow-x-auto">
                <table class="min-w-full divide-y divide-gray-200">
                    <thead class="bg-gray-50">
                    <tr>
                        <th class="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">学号</th>
                        <th class="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">报警编号</th>
                        <th class="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">视频ID</th>
                        <th class="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">创建时间</th>
                        <th class="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">更新时间</th>
                        <th class="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">操作</th>
                    </tr>
                    </thead>
                    <tbody id="alarmVideoTableBody" class="bg-white divide-y divide-gray-200">
                    <!-- 报警视频数据行通过JS填充 -->
                    </tbody>
                </table>
            </div>

            <!-- 报警视频分页区域 -->
            <div class="flex flex-col md:flex-row md:items-center md:justify-between mt-4 space-y-3 md:space-y-0">
                <div class="text-sm text-gray-600">
                    当前第 <span id="alarmVideoCurrentPageText">1</span> 页，
                    共 <span id="alarmVideoTotalPagesText">1</span> 页
                </div>
                <div class="flex items-center space-x-2">
                    <button class="px-3 py-1 border rounded text-sm"
                            onclick="changeAlarmVideoPage('prev')">
                        上一页
                    </button>
                    <button class="px-3 py-1 border rounded text-sm"
                            onclick="changeAlarmVideoPage('next')">
                        下一页
                    </button>
                    <select id="alarmVideoPageSizeSelect"
                            class="form-select form-select-sm w-24"
                            onchange="changeAlarmVideoPageSize(this.value)">
                        <option value="10">10条/页</option>
                        <option value="20">20条/页</option>
                        <option value="50">50条/页</option>
                    </select>
                </div>
            </div>
        </div>
    </div>

    <!-- 报警视频播放模态框 -->
    <div class="modal fade" id="alarmVideoModal" tabindex="-1" aria-labelledby="alarmVideoModalLabel" aria-hidden="true">
        <div class="modal-dialog modal-lg">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title" id="alarmVideoModalLabel">报警视频播放</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <div class="modal-body">
                    <video id="alarmVideoPlayer"
                           controls
                           style="width: 100%; max-height: 480px;"
                           src="">
                        您的浏览器不支持 HTML5 视频播放。
                    </video>
                </div>
            </div>
        </div>
    </div>

    <!-- 引入 jQuery -->
<%--    <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>--%>
    <!-- 引入 Bootstrap JS 和 Popper.js -->
    <script src="https://cdn.jsdelivr.net/npm/@popperjs/core@2.10.2/dist/umd/popper.min.js"></script>
<%--    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/js/bootstrap.min.js"></script>--%>
    <!-- 引入验证脚本 -->
    <script src="${pageContext.request.contextPath}/static/js/sysuser-validation.js"></script>

    <script>
        $.ajaxSetup({
            beforeSend: function(xhr) {
                var loginUUID = localStorage.getItem('loginUUID');
                if (loginUUID) {
                    xhr.setRequestHeader('X-Login-UUID', loginUUID);
                }
            }
        });

        // // 初始化图表
        // var chartDom = document.getElementById('collegeChart');
        // var myChart = echarts.init(chartDom);
        // var option = {
        //     tooltip: {
        //         trigger: 'item'
        //     },
        //     legend: {
        //         orient: 'vertical',
        //         left: 'left'
        //     },
        //     series: [
        //         {
        //             name: '晚归人数',
        //             type: 'pie',
        //             radius: '50%',
        //             data: [
        //                 { value: 35, name: '计算机学院' },
        //                 { value: 25, name: '经济学院' },
        //                 { value: 20, name: '医学院' },
        //                 { value: 15, name: '文学院' },
        //                 { value: 5, name: '其他' }
        //             ],
        //             emphasis: {
        //                 itemStyle: {
        //                     shadowBlur: 10,
        //                     shadowOffsetX: 0,
        //                     shadowColor: 'rgba(0, 0, 0, 0.5)'
        //                 }
        //             }
        //         }
        //     ]
        // };
        // myChart.setOption(option);

        // 数据验证
        <c:if test="${empty sessionScope.user}">
            window.location.href = "${pageContext.request.contextPath}/register";
        </c:if>

        // 通知管理相关变量
        var notificationCurrentPage = 1;
        var notificationPageSize = 10;
        var notificationTotalPages = 1;

        // 报警视频列表相关变量
        var alarmVideoCurrentPage = 1;
        var alarmVideoPageSize = 10;
        var alarmVideoTotalPages = 1;

        $(document).ready(function() {
            // 保存个人信息
            $('#saveProfileBtn').click(function() {
                if (!validateSysUserForm()) {
                    return;
                }

                // 将表单数据转换为 JSON 对象
                var formData = {};
                $('#editProfileForm').serializeArray().forEach(function(item) {
                    formData[item.name] = item.value;
                });

                $.ajax({
                    url: '${pageContext.request.contextPath}/sysuser/update/per-info',
                    type: 'POST',
                    contentType: 'application/json',
                    data: JSON.stringify(formData),
                    success: function(response) {
                        if (response === 'success') {
                            alert('个人信息更新成功');
                            $('#editProfileModal').modal('hide');
                            location.reload();
                        } else {
                            alert('更新失败，请重试');
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

            // 快捷操作按钮跳转
            $('#btn-late-records').click(function() {
                window.location.href = '${pageContext.request.contextPath}/late-records';
            });
            $('#btn-reports').click(function() {
                window.location.href = '${pageContext.request.contextPath}/reports';
            });
            $('#btn-messages').click(function() {
                window.location.href = '${pageContext.request.contextPath}/messages';
            });
            $('#btn-rule').click(function() {
                showSystemRuleModal();
            });

            // 报警视频搜索
            $('#alarmVideoSearchBtn').click(function() {
                alarmVideoCurrentPage = 1;
                loadAlarmVideos(alarmVideoCurrentPage);
            });

            $('#alarmVideoResetBtn').click(function() {
                $('#alarmVideoStudentNoInput').val('');
                $('#alarmVideoAlarmNoInput').val('');
                alarmVideoCurrentPage = 1;
                loadAlarmVideos(alarmVideoCurrentPage);
            });

            $('#alarmVideoStudentNoInput, #alarmVideoAlarmNoInput').on('keyup', function(e) {
                if (e.key === 'Enter') {
                    alarmVideoCurrentPage = 1;
                    loadAlarmVideos(alarmVideoCurrentPage);
                }
            });

            // 发布通知类型切换，控制目标ID输入框显示
            $('input[name="notifyType"]').change(function() {
                updateNotifyTargetIdVisibility();
            });
            updateNotifyTargetIdVisibility();

            // 发布通知按钮
            $('#publishNotificationBtn').click(function() {
                publishNotification();
            });

            // 通知搜索按钮
            $('#notificationSearchBtn').click(function() {
                searchNotifications();
            });

            // 重置搜索
            $('#notificationResetBtn').click(function() {
                $('#notificationSearchInput').val('');
                searchNotifications();
            });

            // 回车搜索
            $('#notificationSearchInput').on('keyup', function(e) {
                if (e.key === 'Enter') {
                    searchNotifications();
                }
            });

            // 初始加载通知列表
            loadNotifications(1);
            // 初始加载报警视频列表
            loadAlarmVideos(1);
        });

        // 根据类型显示或隐藏目标ID输入框
        function updateNotifyTargetIdVisibility() {
            var type = $('input[name="notifyType"]:checked').val();
            if (type === 'all') {
                $('#notifyTargetIdGroup').hide();
            } else {
                $('#notifyTargetIdGroup').show();
            }
        }

        // 发布通知
        function publishNotification() {
            var type = $('input[name="notifyType"]:checked').val();
            var title = ($('#notifyTitle').val() || '').trim();
            var content = ($('#notifyContent').val() || '').trim();
            var targetId = type === 'single' ? ($('#notifyTargetId').val() || '').trim() : '';

            if (!title) {
                alert('通知标题不能为空');
                return;
            }
            if (!content) {
                alert('通知内容不能为空');
                return;
            }
            if (type === 'single' && !targetId) {
                alert('请填写目标用户的业务ID');
                return;
            }

            var payload = {
                title: title,
                content: content,
                targetId: targetId || null
            };

            $.ajax({
                url: '${pageContext.request.contextPath}/notification/admin/publish',
                type: 'POST',
                contentType: 'application/json',
                data: JSON.stringify(payload),
                success: function(response) {
                    if (response && response.success) {
                        alert('发布成功');
                        // 清空表单
                        $('#notifyTitle').val('');
                        $('#notifyContent').val('');
                        if (type === 'single') {
                            $('#notifyTargetId').val('');
                        }
                        // 重新加载通知列表，回到第一页
                        notificationCurrentPage = 1;
                        loadNotifications(notificationCurrentPage);
                    } else {
                        var msg = (response && (response.message || response.msg)) ? (response.message || response.msg) : '发布失败';
                        alert(msg);
                    }
                },
                error: function(xhr) {
                    var msg = '请求失败';
                    if (xhr && xhr.responseText) {
                        msg = xhr.responseText;
                    }
                    alert(msg);
                }
            });
        }

        // 加载报警视频列表
        function loadAlarmVideos(pageNum) {
            if (!pageNum || pageNum < 1) {
                pageNum = 1;
            }

            var query = {
                pageNum: pageNum,
                pageSize: alarmVideoPageSize,
                studentNo: ($('#alarmVideoStudentNoInput').val() || '').trim(),
                alarmNo: ($('#alarmVideoAlarmNoInput').val() || '').trim()
            };

            $.ajax({
                url: '${pageContext.request.contextPath}/alarm-video/page',
                type: 'POST',
                contentType: 'application/json',
                data: JSON.stringify(query),
                success: function(response) {
                    if (response && response.success && response.data) {
                        var pageResult = response.data;
                        renderAlarmVideoTable(pageResult.data || []);
                        renderAlarmVideoPagination(pageResult);
                    } else {
                        var msg = (response && (response.message || response.msg)) ? (response.message || response.msg) : '加载报警视频失败';
                        alert(msg);
                    }
                },
                error: function(xhr) {
                    var msg = '请求失败';
                    if (xhr && xhr.responseText) {
                        msg = xhr.responseText;
                    }
                    alert(msg);
                }
            });
        }

        // 渲染报警视频表格
        function renderAlarmVideoTable(list) {
            var tbody = $('#alarmVideoTableBody');
            tbody.empty();

            if (!list || list.length === 0) {
                tbody.append('<tr><td colspan="6" class="px-4 py-4 text-center text-sm text-gray-500">暂无报警视频</td></tr>');
                return;
            }

            list.forEach(function(v) {
                var studentNo = v.studentNo || '';
                var alarmNo = v.alarmNo || '';
                var videoId = v.videoId || '';
                var createdAt = v.createdAt ? formatDateTime(v.createdAt) : '';
                var updatedAt = v.updatedAt ? formatDateTime(v.updatedAt) : '';

                var row =
                    '<tr>' +
                    '<td class="px-4 py-2 whitespace-nowrap text-sm text-gray-900" title="' + studentNo + '">' + studentNo + '</td>' +
                    '<td class="px-4 py-2 whitespace-nowrap text-sm text-gray-900" title="' + alarmNo + '">' + alarmNo + '</td>' +
                    '<td class="px-4 py-2 whitespace-nowrap text-sm text-gray-900" title="' + videoId + '">' + videoId + '</td>' +
                    '<td class="px-4 py-2 whitespace-nowrap text-sm text-gray-900">' + createdAt + '</td>' +
                    '<td class="px-4 py-2 whitespace-nowrap text-sm text-gray-900">' + updatedAt + '</td>' +
                    '<td class="px-4 py-2 whitespace-nowrap text-sm text-gray-900">' +
                    '<button class="text-blue-600 hover:text-blue-800 mr-3" onclick="playAlarmVideo(\'' + videoId + '\')">播放</button>' +
                    '<button class="text-red-600 hover:text-red-800" onclick="deleteAlarmVideo(\'' + videoId + '\')">删除</button>' +
                    '</td>' +
                    '</tr>';

                tbody.append(row);
            });
        }

        // 渲染报警视频分页信息
        function renderAlarmVideoPagination(pageResult) {
            alarmVideoCurrentPage = pageResult.pageNum || 1;
            alarmVideoTotalPages = pageResult.totalPages || 1;

            $('#alarmVideoCurrentPageText').text(alarmVideoCurrentPage);
            $('#alarmVideoTotalPagesText').text(alarmVideoTotalPages);
        }

        // 切换报警视频页码
        function changeAlarmVideoPage(direction) {
            if (direction === 'prev') {
                if (alarmVideoCurrentPage > 1) {
                    alarmVideoCurrentPage--;
                    loadAlarmVideos(alarmVideoCurrentPage);
                }
            } else if (direction === 'next') {
                if (alarmVideoCurrentPage < alarmVideoTotalPages) {
                    alarmVideoCurrentPage++;
                    loadAlarmVideos(alarmVideoCurrentPage);
                }
            }
        }

        // 修改报警视频每页条数
        function changeAlarmVideoPageSize(size) {
            size = parseInt(size, 10);
            if (!size || size <= 0) {
                size = 10;
            }
            alarmVideoPageSize = size;
            alarmVideoCurrentPage = 1;
            loadAlarmVideos(alarmVideoCurrentPage);
        }

        // 播放报警视频
        function playAlarmVideo(videoId) {
            if (!videoId) {
                alert('视频ID不存在，无法播放');
                return;
            }

            $.ajax({
                url: '${pageContext.request.contextPath}/alarm-video/play-url',
                type: 'GET',
                data: { videoId: videoId },
                success: function(response) {
                    if (response && response.success && response.data && response.data.previewUrl) {
                        var previewUrl = response.data.previewUrl;
                        showAlarmVideoModal(previewUrl);
                    } else {
                        var msg = (response && (response.message || response.msg)) ? (response.message || response.msg) : '获取视频播放地址失败';
                        alert(msg);
                    }
                },
                error: function(xhr) {
                    var msg = '请求失败';
                    if (xhr && xhr.responseText) {
                        msg = xhr.responseText;
                    }
                    alert(msg);
                }
            });
        }

        // 显示报警视频播放模态框
        function showAlarmVideoModal(previewUrl) {
            var contextPath = '${pageContext.request.contextPath}';
            $('#alarmVideoPlayer').attr('src', contextPath + previewUrl);

            var modalElement = document.getElementById('alarmVideoModal');
            if (modalElement) {
                var modal = new bootstrap.Modal(modalElement);
                modal.show();
            }
        }

        // 删除报警视频
        function deleteAlarmVideo(videoId) {
            if (!videoId) {
                alert('视频ID不存在，无法删除');
                return;
            }

            if (!confirm('确定要删除该报警视频记录吗？')) {
                return;
            }

            $.ajax({
                url: '${pageContext.request.contextPath}/alarm-video/delete?videoId=' + encodeURIComponent(videoId),
                type: 'DELETE',
                success: function(response) {
                    if (response && response.success) {
                        alert('删除成功');
                        loadAlarmVideos(alarmVideoCurrentPage);
                    } else {
                        var msg = (response && (response.message || response.msg)) ? (response.message || response.msg) : '删除失败';
                        alert(msg);
                    }
                },
                error: function(xhr) {
                    var msg = '请求失败';
                    if (xhr && xhr.responseText) {
                        msg = xhr.responseText;
                    }
                    alert(msg);
                }
            });
        }

        // 搜索通知（从第一页开始）
        function searchNotifications() {
            notificationCurrentPage = 1;
            loadNotifications(notificationCurrentPage);
        }

        // 加载通知列表
        function loadNotifications(pageNum) {
            if (!pageNum || pageNum < 1) {
                pageNum = 1;
            }

            var contentLike = $('#notificationSearchInput').val() || '';

            var query = {
                pageNum: pageNum,
                pageSize: notificationPageSize,
                contentLike: contentLike.trim()
            };

            $.ajax({
                url: '${pageContext.request.contextPath}/notification/admin/page',
                type: 'POST',
                contentType: 'application/json',
                data: JSON.stringify(query),
                success: function(response) {
                    if (response && response.success && response.data) {
                        var pageResult = response.data;
                        // PageResult 使用 data 字段存放当前页列表
                        renderNotificationTable(pageResult.data || []);
                        renderNotificationPagination(pageResult);
                    } else {
                        var msg = (response && (response.message || response.msg)) ? (response.message || response.msg) : '加载通知失败';
                        alert(msg);
                    }
                },
                error: function(xhr) {
                    var msg = '请求失败';
                    if (xhr && xhr.responseText) {
                        msg = xhr.responseText;
                    }
                    alert(msg);
                }
            });
        }

        // 渲染通知表格
        function renderNotificationTable(list) {
            var tbody = $('#notificationTableBody');
            tbody.empty();

            if (!list || list.length === 0) {
                tbody.append('<tr><td colspan="6" class="px-4 py-4 text-center text-sm text-gray-500">暂无通知</td></tr>');
                return;
            }

            list.forEach(function(n) {
                var noticeId = n.noticeId || '';
                var title = n.title || '';
                var content = n.content || '';
                var targetId = n.targetId || '';
                var updateTime = n.updateTime ? formatDateTime(n.updateTime) : '';

                // 对内容进行简单截断展示
                var displayContent = content;
                if (displayContent.length > 40) {
                    displayContent = displayContent.substring(0, 40) + '...';
                }

                var row =
                    '<tr>' +
                    '<td class="px-4 py-2 whitespace-nowrap text-sm text-gray-900" title="' + noticeId + '">' + noticeId + '</td>' +
                    '<td class="px-4 py-2 whitespace-nowrap text-sm text-gray-900" title="' + title + '">' + title + '</td>' +
                    '<td class="px-4 py-2 whitespace-nowrap text-sm text-gray-900" title="' + content.replace(/"/g, '&quot;') + '">' + displayContent + '</td>' +
                    '<td class="px-4 py-2 whitespace-nowrap text-sm text-gray-900" title="' + targetId + '">' + targetId + '</td>' +
                    '<td class="px-4 py-2 whitespace-nowrap text-sm text-gray-900">' + updateTime + '</td>' +
                    '<td class="px-4 py-2 whitespace-nowrap text-sm text-gray-900">' +
                    '<button class="text-red-600 hover:text-red-800" onclick="deleteNotification(\'' + noticeId + '\')">删除</button>' +
                    '</td>' +
                    '</tr>';

                tbody.append(row);
            });
        }

        // 渲染分页信息
        function renderNotificationPagination(pageResult) {
            notificationCurrentPage = pageResult.pageNum || 1;
            // PageResult 通过 totalPages 字段暴露总页数
            notificationTotalPages = pageResult.totalPages || 1;

            $('#notificationCurrentPageText').text(notificationCurrentPage);
            $('#notificationTotalPagesText').text(notificationTotalPages);
        }

        // 切换页码
        function changeNotificationPage(direction) {
            if (direction === 'prev') {
                if (notificationCurrentPage > 1) {
                    notificationCurrentPage--;
                    loadNotifications(notificationCurrentPage);
                }
            } else if (direction === 'next') {
                if (notificationCurrentPage < notificationTotalPages) {
                    notificationCurrentPage++;
                    loadNotifications(notificationCurrentPage);
                }
            }
        }

        // 修改每页条数
        function changeNotificationPageSize(size) {
            size = parseInt(size, 10);
            if (!size || size <= 0) {
                size = 10;
            }
            notificationPageSize = size;
            notificationCurrentPage = 1;
            loadNotifications(notificationCurrentPage);
        }

        // 删除通知
        function deleteNotification(noticeId) {
            if (!noticeId) {
                alert('通知ID不存在，无法删除');
                return;
            }

            if (!confirm('确定要删除该通知吗？')) {
                return;
            }

            var dto = {
                noticeIdList: [noticeId]
            };

            $.ajax({
                url: '${pageContext.request.contextPath}/notification/del',
                type: 'DELETE',
                contentType: 'application/json',
                data: JSON.stringify(dto),
                success: function(response) {
                    if (response && response.success) {
                        alert('删除成功');
                        loadNotifications(notificationCurrentPage);
                    } else {
                        var msg = (response && (response.message || response.msg)) ? (response.message || response.msg) : '删除失败';
                        alert(msg);
                    }
                },
                error: function(xhr) {
                    var msg = '请求失败';
                    if (xhr && xhr.responseText) {
                        msg = xhr.responseText;
                    }
                    alert(msg);
                }
            });
        }

        // 时间格式化工具（与其他页面保持风格，可按需精简）
        function formatDateTime(time) {
            if (!time) return '';
            try {
                // 兼容后端返回时间字符串或时间戳
                var date = (typeof time === 'string' || typeof time === 'number') ? new Date(time) : time;
                if (isNaN(date.getTime())) {
                    return '';
                }
                var y = date.getFullYear();
                var m = ('0' + (date.getMonth() + 1)).slice(-2);
                var d = ('0' + date.getDate()).slice(-2);
                var h = ('0' + date.getHours()).slice(-2);
                var mi = ('0' + date.getMinutes()).slice(-2);
                var s = ('0' + date.getSeconds()).slice(-2);
                return y + '-' + m + '-' + d + ' ' + h + ':' + mi + ':' + s;
            } catch (e) {
                return '';
            }
        }

        function handleLogout() {
            if (confirm('确定要退出登录吗？')) {
                localStorage.removeItem('loginUUID');
                window.location.href = '${pageContext.request.contextPath}/logout';
            }
        }

        // 显示系统设置弹框
        function showSystemRuleModal() {
            // 先获取当前规则值
            $.ajax({
                url: '${pageContext.request.contextPath}/system-rule/key/LARGE_TIMES_LATERETURN_MONTH',
                type: 'GET',
                success: function(response) {
                    var currentValue = '';
                    if (response && response.ruleValue) {
                        currentValue = response.ruleValue;
                    }
                    
                    // 构建弹框HTML
                    var modalHtml = '<div class="fixed inset-0 bg-gray-600 bg-opacity-50 overflow-y-auto h-full w-full z-50" id="systemRuleModal">' +
                            '<div class="relative top-20 mx-auto p-5 border w-11/12 md:w-3/4 lg:w-1/2 shadow-lg rounded-md bg-white max-h-[90vh] overflow-y-auto">' +
                            '<div class="mt-3">' +
                            '<div class="flex justify-between items-center mb-4">' +
                            '<h3 class="text-lg font-medium text-gray-900">系统设置</h3>' +
                            '<button onclick="closeSystemRuleModal()" class="text-gray-400 hover:text-gray-600">' +
                            '<svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">' +
                            '<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path>' +
                            '</svg>' +
                            '</button>' +
                            '</div>' +
                            '<div class="space-y-4">' +
                            '<div>' +
                            '<label class="text-sm font-medium text-gray-700 mb-2 block">家长通知机制—每月最大晚归次数阈值：</label>' +
                            '<input type="number" id="ruleValueInput" min="1" step="1" value="' + currentValue + '" ' +
                            'class="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500" ' +
                            'placeholder="请输入正整数（大于0）">' +
                            '<p class="mt-1 text-xs text-gray-500">请输入一个大于0的正整数</p>' +
                            '</div>' +
                            '</div>' +
                            '<div class="mt-6 flex justify-end space-x-3">' +
                            '<button onclick="closeSystemRuleModal()" class="px-4 py-2 bg-gray-300 text-gray-700 rounded hover:bg-gray-400">取消</button>' +
                            '<button onclick="submitSystemRule()" class="px-4 py-2 bg-blue-500 text-white rounded hover:bg-blue-600">确定</button>' +
                            '</div>' +
                            '</div>' +
                            '</div>' +
                            '</div>';

                    // 移除已存在的弹框
                    $('#systemRuleModal').remove();

                    // 添加弹框到页面
                    $('body').append(modalHtml);
                },
                error: function() {
                    // 即使获取失败也显示弹框，只是没有默认值
                    var modalHtml = '<div class="fixed inset-0 bg-gray-600 bg-opacity-50 overflow-y-auto h-full w-full z-50" id="systemRuleModal">' +
                            '<div class="relative top-20 mx-auto p-5 border w-11/12 md:w-3/4 lg:w-1/2 shadow-lg rounded-md bg-white max-h-[90vh] overflow-y-auto">' +
                            '<div class="mt-3">' +
                            '<div class="flex justify-between items-center mb-4">' +
                            '<h3 class="text-lg font-medium text-gray-900">系统设置</h3>' +
                            '<button onclick="closeSystemRuleModal()" class="text-gray-400 hover:text-gray-600">' +
                            '<svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">' +
                            '<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path>' +
                            '</svg>' +
                            '</button>' +
                            '</div>' +
                            '<div class="space-y-4">' +
                            '<div>' +
                            '<label class="text-sm font-medium text-gray-700 mb-2 block">家长通知机制—每月最大晚归次数阈值：</label>' +
                            '<input type="number" id="ruleValueInput" min="1" step="1" ' +
                            'class="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500" ' +
                            'placeholder="请输入正整数（大于0）">' +
                            '<p class="mt-1 text-xs text-gray-500">请输入一个大于0的正整数</p>' +
                            '</div>' +
                            '</div>' +
                            '<div class="mt-6 flex justify-end space-x-3">' +
                            '<button onclick="closeSystemRuleModal()" class="px-4 py-2 bg-gray-300 text-gray-700 rounded hover:bg-gray-400">取消</button>' +
                            '<button onclick="submitSystemRule()" class="px-4 py-2 bg-blue-500 text-white rounded hover:bg-blue-600">确定</button>' +
                            '</div>' +
                            '</div>' +
                            '</div>' +
                            '</div>';

                    $('#systemRuleModal').remove();
                    $('body').append(modalHtml);
                }
            });
        }

        // 提交系统规则
        function submitSystemRule() {
            var ruleValue = $('#ruleValueInput').val().trim();
            
            // 校验
            if (!ruleValue) {
                alert('请输入阈值');
                return;
            }
            
            var numValue = parseInt(ruleValue, 10);
            if (isNaN(numValue) || numValue <= 0) {
                alert('请输入一个大于0的正整数');
                return;
            }

            // 构建请求数据
            var requestData = {
                ruleKey: 'LARGE_TIMES_LATERETURN_MONTH',
                ruleValue: String(numValue),
                description: '家长通知机制—每月最大晚归次数阈值'
            };

            // 发送AJAX请求
            $.ajax({
                url: '${pageContext.request.contextPath}/system-rule/save-or-update',
                type: 'POST',
                contentType: 'application/json',
                data: JSON.stringify(requestData),
                success: function(response) {
                    if (response && response.success) {
                        alert('设置成功');
                        closeSystemRuleModal();
                    } else {
                        var msg = (response && (response.message || response.msg)) ? (response.message || response.msg) : '设置失败';
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

        // 关闭系统设置弹框
        function closeSystemRuleModal() {
            $('#systemRuleModal').remove();
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