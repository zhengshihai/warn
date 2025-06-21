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

        <!-- 统计卡片 -->
        <div class="grid grid-cols-1 md:grid-cols-3 gap-6 mb-6">
            <div class="card p-6">
                <h3 class="text-lg font-medium text-gray-900">今日晚归人数</h3>
                <p class="text-3xl font-bold text-indigo-600 mt-2">12</p>
                <p class="text-sm text-gray-500 mt-1">较昨日 +2</p>
            </div>
            <div class="card p-6">
                <h3 class="text-lg font-medium text-gray-900">本月累计</h3>
                <p class="text-3xl font-bold text-green-600 mt-2">156</p>
                <p class="text-sm text-gray-500 mt-1">较上月 -23</p>
            </div>
            <div class="card p-6">
                <h3 class="text-lg font-medium text-gray-900">本月晚归增幅</h3>
                <p class="text-3xl font-bold text-red-600 mt-2">%3</p>
                <p class="text-sm text-gray-500 mt-1">较上月</p>
            </div>
        </div>

        <!-- 图表和时间线 -->
        <div class="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
            <!-- 学院分布图 -->
            <div class="card p-6">
                <h3 class="text-lg font-medium text-gray-900 mb-4">学院分布</h3>
                <div id="collegeChart" style="height: 300px;"></div>
            </div>
            
            <!-- 最近预警动态 -->
            <div class="card p-6">
                <h3 class="text-lg font-medium text-gray-900 mb-4">最近预警动态</h3>
                <div class="space-y-4">
                    <div class="timeline-item">
                        <div class="timeline-dot"></div>
                        <div class="text-sm">
                            <p class="font-medium">张三 - 计算机学院</p>
                            <p class="text-gray-500">23:45 返回宿舍</p>
                        </div>
                    </div>
                    <div class="timeline-item">
                        <div class="timeline-dot"></div>
                        <div class="text-sm">
                            <p class="font-medium">李四 - 经济学院</p>
                            <p class="text-gray-500">23:30 返回宿舍</p>
                        </div>
                    </div>
                    <div class="timeline-item">
                        <div class="timeline-dot"></div>
                        <div class="text-sm">
                            <p class="font-medium">王五 - 医学院</p>
                            <p class="text-gray-500">23:15 返回宿舍</p>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <!-- 快捷操作 -->
        <div class="card p-6">
            <h3 class="text-lg font-medium text-gray-900 mb-4">快捷操作</h3>
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
    </div>

    <!-- 引入 jQuery -->
    <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
    <!-- 引入 Bootstrap JS 和 Popper.js -->
    <script src="https://cdn.jsdelivr.net/npm/@popperjs/core@2.10.2/dist/umd/popper.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/js/bootstrap.min.js"></script>
    <!-- 引入验证脚本 -->
    <script src="${pageContext.request.contextPath}/static/js/sysuser-validation.js"></script>

    <script>
        // 初始化图表
        var chartDom = document.getElementById('collegeChart');
        var myChart = echarts.init(chartDom);
        var option = {
            tooltip: {
                trigger: 'item'
            },
            legend: {
                orient: 'vertical',
                left: 'left'
            },
            series: [
                {
                    name: '晚归人数',
                    type: 'pie',
                    radius: '50%',
                    data: [
                        { value: 35, name: '计算机学院' },
                        { value: 25, name: '经济学院' },
                        { value: 20, name: '医学院' },
                        { value: 15, name: '文学院' },
                        { value: 5, name: '其他' }
                    ],
                    emphasis: {
                        itemStyle: {
                            shadowBlur: 10,
                            shadowOffsetX: 0,
                            shadowColor: 'rgba(0, 0, 0, 0.5)'
                        }
                    }
                }
            ]
        };
        myChart.setOption(option);

        // 数据验证
        <c:if test="${empty sessionScope.user}">
            window.location.href = "${pageContext.request.contextPath}/register";
        </c:if>

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
                window.location.href = '${pageContext.request.contextPath}/rule';
            });
        });

        function handleLogout() {
            if (confirm('确定要退出登录吗？')) {
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