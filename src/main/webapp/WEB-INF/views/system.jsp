<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<!DOCTYPE html>
<html>
<head>
    <title>系统管理 - 学生晚归预警系统</title>
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
    <!-- 导航栏 -->
    <nav class="bg-white shadow-lg">
        <div class="max-w-7xl mx-auto px-4">
            <div class="flex justify-between h-16">
                <div class="flex">
                    <div class="flex-shrink-0 flex items-center">
                        <span class="text-xl font-bold text-gray-800">学生晚归预警系统</span>
                    </div>
                    <div class="hidden sm:ml-6 sm:flex sm:space-x-8">
                        <a href="/dashboard" class="text-gray-500 hover:text-gray-700 px-3 py-2 rounded-md text-sm font-medium">首页</a>
                        <a href="/late-records" class="text-gray-500 hover:text-gray-700 px-3 py-2 rounded-md text-sm font-medium">晚归记录</a>
                        <a href="/system" class="border-indigo-500 text-gray-900 inline-flex items-center px-1 pt-1 border-b-2 text-sm font-medium">系统管理</a>
                    </div>
                </div>
                <div class="flex items-center">
                    <div class="flex-shrink-0">
                        <span class="text-gray-500 mr-4">管理员</span>
                        <button class="bg-red-500 hover:bg-red-600 text-white px-4 py-2 rounded-md text-sm font-medium">退出</button>
                    </div>
                </div>
            </div>
        </div>
    </nav>

    <!-- 主要内容 -->
    <div class="max-w-7xl mx-auto py-6 sm:px-6 lg:px-8">
        <!-- 标题和操作按钮 -->
        <div class="flex justify-between items-center mb-6">
            <h1 class="text-2xl font-semibold text-gray-900">系统管理</h1>
            <div class="flex space-x-4">
                <button class="bg-indigo-500 hover:bg-indigo-600 text-white px-4 py-2 rounded-md text-sm font-medium">
                    <i class="fas fa-plus mr-2"></i>添加用户
                </button>
                <button class="bg-green-500 hover:bg-green-600 text-white px-4 py-2 rounded-md text-sm font-medium">
                    <i class="fas fa-save mr-2"></i>保存设置
                </button>
            </div>
        </div>

        <!-- 选项卡导航 -->
        <div class="border-b border-gray-200 mb-6">
            <nav class="-mb-px flex space-x-8">
                <a href="#users" class="border-indigo-500 text-indigo-600 whitespace-nowrap py-4 px-1 border-b-2 font-medium text-sm">用户管理</a>
                <a href="#roles" class="border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300 whitespace-nowrap py-4 px-1 border-b-2 font-medium text-sm">角色权限</a>
                <a href="#settings" class="border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300 whitespace-nowrap py-4 px-1 border-b-2 font-medium text-sm">系统设置</a>
            </nav>
        </div>

        <!-- 用户管理 -->
        <div id="users" class="bg-white shadow rounded-lg p-6 mb-6">
            <div class="flex justify-between items-center mb-4">
                <h2 class="text-lg font-medium text-gray-900">用户列表</h2>
                <div class="flex space-x-4">
                    <input type="text" placeholder="搜索用户..." class="border rounded-md px-3 py-2">
                    <button class="bg-gray-100 hover:bg-gray-200 text-gray-700 px-4 py-2 rounded-md text-sm font-medium">
                        <i class="fas fa-search mr-2"></i>搜索
                    </button>
                </div>
            </div>
            <table class="min-w-full divide-y divide-gray-200">
                <thead class="bg-gray-50">
                    <tr>
                        <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">用户名</th>
                        <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">角色</th>
                        <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">状态</th>
                        <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">最后登录</th>
                        <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">操作</th>
                    </tr>
                </thead>
                <tbody class="bg-white divide-y divide-gray-200">
                    <tr>
                        <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">admin</td>
                        <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">系统管理员</td>
                        <td class="px-6 py-4 whitespace-nowrap">
                            <span class="px-2 inline-flex text-xs leading-5 font-semibold rounded-full bg-green-100 text-green-800">正常</span>
                        </td>
                        <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">2024-01-20 10:30</td>
                        <td class="px-6 py-4 whitespace-nowrap text-sm font-medium">
                            <button class="text-indigo-600 hover:text-indigo-900 mr-3">编辑</button>
                            <button class="text-red-600 hover:text-red-900">删除</button>
                        </td>
                    </tr>
                </tbody>
            </table>
        </div>

        <!-- 角色权限 -->
        <div id="roles" class="bg-white shadow rounded-lg p-6 mb-6 hidden">
            <div class="flex justify-between items-center mb-4">
                <h2 class="text-lg font-medium text-gray-900">角色列表</h2>
                <button class="bg-indigo-500 hover:bg-indigo-600 text-white px-4 py-2 rounded-md text-sm font-medium">
                    <i class="fas fa-plus mr-2"></i>添加角色
                </button>
            </div>
            <table class="min-w-full divide-y divide-gray-200">
                <thead class="bg-gray-50">
                    <tr>
                        <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">角色名称</th>
                        <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">描述</th>
                        <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">用户数</th>
                        <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">操作</th>
                    </tr>
                </thead>
                <tbody class="bg-white divide-y divide-gray-200">
                    <tr>
                        <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">系统管理员</td>
                        <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">系统最高权限管理员</td>
                        <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">1</td>
                        <td class="px-6 py-4 whitespace-nowrap text-sm font-medium">
                            <button class="text-indigo-600 hover:text-indigo-900 mr-3">编辑</button>
                            <button class="text-red-600 hover:text-red-900">删除</button>
                        </td>
                    </tr>
                </tbody>
            </table>
        </div>

        <!-- 系统设置 -->
        <div id="settings" class="bg-white shadow rounded-lg p-6 mb-6 hidden">
            <h2 class="text-lg font-medium text-gray-900 mb-4">系统设置</h2>
            <div class="space-y-6">
                <!-- 基本设置 -->
                <div class="border-b border-gray-200 pb-6">
                    <h3 class="text-md font-medium text-gray-900 mb-4">基本设置</h3>
                    <div class="grid grid-cols-1 gap-6 sm:grid-cols-2">
                        <div>
                            <label class="block text-sm font-medium text-gray-700">系统名称</label>
                            <input type="text" value="学生晚归预警系统" class="mt-1 block w-full border border-gray-300 rounded-md shadow-sm py-2 px-3 focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm">
                        </div>
                        <div>
                            <label class="block text-sm font-medium text-gray-700">系统Logo</label>
                            <input type="file" class="mt-1 block w-full">
                        </div>
                    </div>
                </div>

                <!-- 预警设置 -->
                <div class="border-b border-gray-200 pb-6">
                    <h3 class="text-md font-medium text-gray-900 mb-4">预警设置</h3>
                    <div class="grid grid-cols-1 gap-6 sm:grid-cols-2">
                        <div>
                            <label class="block text-sm font-medium text-gray-700">晚归时间阈值（分钟）</label>
                            <input type="number" value="30" class="mt-1 block w-full border border-gray-300 rounded-md shadow-sm py-2 px-3 focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm">
                        </div>
                        <div>
                            <label class="block text-sm font-medium text-gray-700">预警通知方式</label>
                            <select class="mt-1 block w-full border border-gray-300 rounded-md shadow-sm py-2 px-3 focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm">
                                <option>短信通知</option>
                                <option>邮件通知</option>
                                <option>系统消息</option>
                            </select>
                        </div>
                    </div>
                </div>

                <!-- 备份设置 -->
                <div>
                    <h3 class="text-md font-medium text-gray-900 mb-4">备份设置</h3>
                    <div class="grid grid-cols-1 gap-6 sm:grid-cols-2">
                        <div>
                            <label class="block text-sm font-medium text-gray-700">自动备份</label>
                            <select class="mt-1 block w-full border border-gray-300 rounded-md shadow-sm py-2 px-3 focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm">
                                <option>每天</option>
                                <option>每周</option>
                                <option>每月</option>
                            </select>
                        </div>
                        <div>
                            <label class="block text-sm font-medium text-gray-700">备份时间</label>
                            <input type="time" value="23:00" class="mt-1 block w-full border border-gray-300 rounded-md shadow-sm py-2 px-3 focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm">
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <script>
        // 选项卡切换
        document.querySelectorAll('nav a').forEach(link => {
            link.addEventListener('click', (e) => {
                e.preventDefault();
                // 移除所有活动状态
                document.querySelectorAll('nav a').forEach(a => {
                    a.classList.remove('border-indigo-500', 'text-indigo-600');
                    a.classList.add('border-transparent', 'text-gray-500');
                });
                // 添加当前活动状态
                link.classList.remove('border-transparent', 'text-gray-500');
                link.classList.add('border-indigo-500', 'text-indigo-600');
                
                // 隐藏所有内容
                document.querySelectorAll('#users, #roles, #settings').forEach(div => {
                    div.classList.add('hidden');
                });
                // 显示当前内容
                const targetId = link.getAttribute('href').substring(1);
                document.getElementById(targetId).classList.remove('hidden');
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