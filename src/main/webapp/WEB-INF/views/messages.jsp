<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<!DOCTYPE html>
<html>
<head>
    <title>消息中心 - 学生晚归预警系统</title>
<%--    <script src="https://cdn.tailwindcss.com"></script>--%>
<%--    <link href="https://cdn.jsdelivr.net/npm/@fortawesome/fontawesome-free@6.0.0/css/all.min.css" rel="stylesheet">--%>

    <!-- 引入 CSS 文件 -->
    <link href="${pageContext.request.contextPath}/static/css/tailwind.min.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/static/css/fontawesome.min.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/static/css/bootstrap.min.css" rel="stylesheet">

    <!-- 引入 JS 文件 -->
    <script src="${pageContext.request.contextPath}/static/js/jquery.min.js"></script>
    <script src="${pageContext.request.contextPath}/static/js/popper.min.js"></script>
    <script src="${pageContext.request.contextPath}/static/js/bootstrap.bundle.min.js"></script>
</head>
<body class="bg-gray-100">
    <!-- 顶部导航栏 -->
    <nav class="bg-white shadow-sm rounded-lg mb-6 p-4">
        <div class="flex justify-between items-center">
            <h1 class="text-xl font-semibold text-gray-800">学生晚归预警系统</h1>
            <div class="flex items-center space-x-4">
                <a href="${pageContext.request.contextPath}/dean" class="text-gray-600 hover:text-gray-900">返回首页</a>
                <button class="text-sm text-red-600 hover:text-red-800" onclick="handleLogout()">退出登录</button>
            </div>
        </div>
    </nav>

    <!-- 主要内容 -->
    <div class="max-w-7xl mx-auto py-6 sm:px-6 lg:px-8">
        <!-- 标题和操作按钮 -->
        <div class="flex justify-between items-center mb-6">
            <h1 class="text-2xl font-semibold text-gray-900">消息中心</h1>
            <div class="flex space-x-4">
                <button class="bg-indigo-500 hover:bg-indigo-600 text-white px-4 py-2 rounded-md text-sm font-medium">
                    <i class="fas fa-envelope mr-2"></i>发送通知
                </button>
                <button class="bg-green-500 hover:bg-green-600 text-white px-4 py-2 rounded-md text-sm font-medium">
                    <i class="fas fa-check-double mr-2"></i>全部已读
                </button>
            </div>
        </div>

        <!-- 消息筛选和搜索 -->
        <div class="bg-white shadow rounded-lg p-6 mb-6">
            <div class="grid grid-cols-1 gap-6 sm:grid-cols-4">
                <div>
                    <label class="block text-sm font-medium text-gray-700">消息类型</label>
                    <select class="mt-1 block w-full border border-gray-300 rounded-md shadow-sm py-2 px-3">
                        <option>全部消息</option>
                        <option>系统通知</option>
                        <option>预警消息</option>
                        <option>处理反馈</option>
                    </select>
                </div>
                <div>
                    <label class="block text-sm font-medium text-gray-700">时间范围</label>
                    <select class="mt-1 block w-full border border-gray-300 rounded-md shadow-sm py-2 px-3">
                        <option>最近7天</option>
                        <option>最近30天</option>
                        <option>本学期</option>
                        <option>全部</option>
                    </select>
                </div>
                <div>
                    <label class="block text-sm font-medium text-gray-700">状态</label>
                    <select class="mt-1 block w-full border border-gray-300 rounded-md shadow-sm py-2 px-3">
                        <option>全部</option>
                        <option>未读</option>
                        <option>已读</option>
                    </select>
                </div>
                <div class="flex items-end">
                    <button class="w-full bg-indigo-500 hover:bg-indigo-600 text-white px-4 py-2 rounded-md text-sm font-medium">
                        <i class="fas fa-search mr-2"></i>查询
                    </button>
                </div>
            </div>
        </div>

        <!-- 消息列表 -->
        <div class="bg-white shadow rounded-lg divide-y divide-gray-200">
            <!-- 未读消息 -->
            <div class="p-6 hover:bg-gray-50 cursor-pointer">
                <div class="flex items-center justify-between">
                    <div class="flex items-center">
                        <div class="flex-shrink-0">
                            <span class="inline-flex items-center justify-center h-10 w-10 rounded-full bg-red-100">
                                <i class="fas fa-exclamation-circle text-red-600"></i>
                            </span>
                        </div>
                        <div class="ml-4">
                            <div class="flex items-center">
                                <h3 class="text-lg font-medium text-gray-900">紧急预警通知</h3>
                                <span class="ml-2 inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-red-100 text-red-800">未读</span>
                            </div>
                            <p class="mt-1 text-sm text-gray-500">学生张三（学号：2021001）连续三天晚归，请及时处理。</p>
                        </div>
                    </div>
                    <div class="ml-4 flex-shrink-0">
                        <span class="text-sm text-gray-500">5分钟前</span>
                    </div>
                </div>
            </div>

            <!-- 系统通知 -->
            <div class="p-6 hover:bg-gray-50 cursor-pointer">
                <div class="flex items-center justify-between">
                    <div class="flex items-center">
                        <div class="flex-shrink-0">
                            <span class="inline-flex items-center justify-center h-10 w-10 rounded-full bg-blue-100">
                                <i class="fas fa-bell text-blue-600"></i>
                            </span>
                        </div>
                        <div class="ml-4">
                            <div class="flex items-center">
                                <h3 class="text-lg font-medium text-gray-900">系统维护通知</h3>
                                <span class="ml-2 inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-gray-100 text-gray-800">已读</span>
                            </div>
                            <p class="mt-1 text-sm text-gray-500">系统将于今晚23:00-24:00进行例行维护，请提前做好相关工作。</p>
                        </div>
                    </div>
                    <div class="ml-4 flex-shrink-0">
                        <span class="text-sm text-gray-500">2小时前</span>
                    </div>
                </div>
            </div>

            <!-- 处理反馈 -->
            <div class="p-6 hover:bg-gray-50 cursor-pointer">
                <div class="flex items-center justify-between">
                    <div class="flex items-center">
                        <div class="flex-shrink-0">
                            <span class="inline-flex items-center justify-center h-10 w-10 rounded-full bg-green-100">
                                <i class="fas fa-check-circle text-green-600"></i>
                            </span>
                        </div>
                        <div class="ml-4">
                            <div class="flex items-center">
                                <h3 class="text-lg font-medium text-gray-900">预警处理完成</h3>
                                <span class="ml-2 inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-gray-100 text-gray-800">已读</span>
                            </div>
                            <p class="mt-1 text-sm text-gray-500">李四（学号：2021002）的晚归预警已被宿管员处理完成。</p>
                        </div>
                    </div>
                    <div class="ml-4 flex-shrink-0">
                        <span class="text-sm text-gray-500">昨天 14:30</span>
                    </div>
                </div>
            </div>

            <!-- 系统提醒 -->
            <div class="p-6 hover:bg-gray-50 cursor-pointer">
                <div class="flex items-center justify-between">
                    <div class="flex items-center">
                        <div class="flex-shrink-0">
                            <span class="inline-flex items-center justify-center h-10 w-10 rounded-full bg-yellow-100">
                                <i class="fas fa-clock text-yellow-600"></i>
                            </span>
                        </div>
                        <div class="ml-4">
                            <div class="flex items-center">
                                <h3 class="text-lg font-medium text-gray-900">数据备份提醒</h3>
                                <span class="ml-2 inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-gray-100 text-gray-800">已读</span>
                            </div>
                            <p class="mt-1 text-sm text-gray-500">本周数据备份已完成，可以在系统管理中查看备份记录。</p>
                        </div>
                    </div>
                    <div class="ml-4 flex-shrink-0">
                        <span class="text-sm text-gray-500">2天前</span>
                    </div>
                </div>
            </div>
        </div>

        <!-- 分页 -->
        <div class="bg-white px-4 py-3 flex items-center justify-between border-t border-gray-200 sm:px-6 mt-6">
            <div class="flex-1 flex justify-between sm:hidden">
                <a href="#" class="relative inline-flex items-center px-4 py-2 border border-gray-300 text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50">上一页</a>
                <a href="#" class="ml-3 relative inline-flex items-center px-4 py-2 border border-gray-300 text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50">下一页</a>
            </div>
            <div class="hidden sm:flex-1 sm:flex sm:items-center sm:justify-between">
                <div>
                    <p class="text-sm text-gray-700">
                        显示第 <span class="font-medium">1</span> 到 <span class="font-medium">10</span> 条，共 <span class="font-medium">97</span> 条记录
                    </p>
                </div>
                <div>
                    <nav class="relative z-0 inline-flex rounded-md shadow-sm -space-x-px" aria-label="Pagination">
                        <a href="#" class="relative inline-flex items-center px-2 py-2 rounded-l-md border border-gray-300 bg-white text-sm font-medium text-gray-500 hover:bg-gray-50">
                            <span class="sr-only">上一页</span>
                            <i class="fas fa-chevron-left"></i>
                        </a>
                        <a href="#" class="relative inline-flex items-center px-4 py-2 border border-gray-300 bg-white text-sm font-medium text-gray-700 hover:bg-gray-50">1</a>
                        <a href="#" class="relative inline-flex items-center px-4 py-2 border border-gray-300 bg-white text-sm font-medium text-gray-700 hover:bg-gray-50">2</a>
                        <a href="#" class="relative inline-flex items-center px-4 py-2 border border-gray-300 bg-white text-sm font-medium text-gray-700 hover:bg-gray-50">3</a>
                        <span class="relative inline-flex items-center px-4 py-2 border border-gray-300 bg-gray-50 text-sm font-medium text-gray-700">...</span>
                        <a href="#" class="relative inline-flex items-center px-4 py-2 border border-gray-300 bg-white text-sm font-medium text-gray-700 hover:bg-gray-50">10</a>
                        <a href="#" class="relative inline-flex items-center px-2 py-2 rounded-r-md border border-gray-300 bg-white text-sm font-medium text-gray-500 hover:bg-gray-50">
                            <span class="sr-only">下一页</span>
                            <i class="fas fa-chevron-right"></i>
                        </a>
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