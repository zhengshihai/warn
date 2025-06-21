<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<!DOCTYPE html>
<html>
<head>
    <title>学生晚归预警系统 - 晚归记录</title>
<%--    <link href="https://cdn.bootcdn.net/ajax/libs/tailwindcss/2.2.19/tailwind.min.css" rel="stylesheet">--%>
<%--    <link href="https://cdn.bootcdn.net/ajax/libs/font-awesome/6.0.0/css/all.min.css" rel="stylesheet">--%>
    <!-- 引入 CSS 文件 -->
    <link href="${pageContext.request.contextPath}/static/css/tailwind.min.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/static/css/fontawesome.min.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/static/css/bootstrap.min.css" rel="stylesheet">

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
    </style>
</head>
<body class="bg-gray-50">
    <div class="min-h-screen p-6">
        <!-- 顶部导航栏 -->
        <nav class="bg-white shadow-sm rounded-lg mb-6 p-4">
            <div class="flex justify-between items-center">
                <h1 class="text-xl font-semibold text-gray-800">学生晚归预警系统</h1>
                <div class="flex items-center space-x-4">
                    <a href="dashboard.jsp" class="text-gray-600 hover:text-gray-900">返回首页</a>
                    <span class="text-gray-600">欢迎 班级管理员</span>
                    <button class="text-sm text-red-600 hover:text-red-800">退出登录</button>
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
            <form class="grid grid-cols-1 md:grid-cols-4 gap-4">
                <div>
                    <label class="block text-sm font-medium text-gray-700 mb-1">时间范围</label>
                    <div class="flex space-x-2">
                        <input type="date" class="flex-1 rounded-md border-gray-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500">
                        <span class="text-gray-500 self-center">至</span>
                        <input type="date" class="flex-1 rounded-md border-gray-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500">
                    </div>
                </div>
                <div>
                    <label class="block text-sm font-medium text-gray-700 mb-1">学院</label>
                    <select class="w-full rounded-md border-gray-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500">
                        <option value="">全部学院</option>
                        <option value="computer">计算机学院</option>
                        <option value="economics">经济学院</option>
                        <option value="medical">医学院</option>
                        <option value="literature">文学院</option>
                    </select>
                </div>
                <div>
                    <label class="block text-sm font-medium text-gray-700 mb-1">处理状态</label>
                    <select class="w-full rounded-md border-gray-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500">
                        <option value="">全部状态</option>
                        <option value="pending">待处理</option>
                        <option value="processed">已处理</option>
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
            <div class="relative">
                <input type="text" placeholder="搜索学号或姓名..." class="w-full px-4 py-2 pl-10 rounded-lg border border-gray-300 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent">
                <div class="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                    <i class="fas fa-search text-gray-400"></i>
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
                    <tbody class="bg-white divide-y divide-gray-200">
                        <tr>
                            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">2021001</td>
                            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">张三</td>
                            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">计算机学院</td>
                            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">A栋-101</td>
                            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">2024-04-14 23:45</td>
                            <td class="px-6 py-4 whitespace-nowrap">
                                <span class="status-badge status-pending">待处理</span>
                            </td>
                            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                                <a href="#" class="text-indigo-600 hover:text-indigo-900 mr-3">查看</a>
                                <a href="#" class="text-green-600 hover:text-green-900">处理</a>
                            </td>
                        </tr>
                        <tr>
                            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">2021002</td>
                            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">李四</td>
                            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">经济学院</td>
                            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">B栋-202</td>
                            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">2024-04-14 23:30</td>
                            <td class="px-6 py-4 whitespace-nowrap">
                                <span class="status-badge status-processed">已处理</span>
                            </td>
                            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                                <a href="#" class="text-indigo-600 hover:text-indigo-900 mr-3">查看</a>
                                <a href="#" class="text-green-600 hover:text-green-900">处理</a>
                            </td>
                        </tr>
                        <!-- 更多记录... -->
                    </tbody>
                </table>
            </div>
        </div>

        <!-- 分页 -->
        <div class="bg-white px-4 py-3 flex items-center justify-between border-t border-gray-200 sm:px-6 mt-4 rounded-lg">
            <div class="flex-1 flex justify-between sm:hidden">
                <a href="#" class="relative inline-flex items-center px-4 py-2 border border-gray-300 text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50">
                    上一页
                </a>
                <a href="#" class="ml-3 relative inline-flex items-center px-4 py-2 border border-gray-300 text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50">
                    下一页
                </a>
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
                        <a href="#" class="relative inline-flex items-center px-4 py-2 border border-gray-300 bg-white text-sm font-medium text-gray-700 hover:bg-gray-50">
                            1
                        </a>
                        <a href="#" class="relative inline-flex items-center px-4 py-2 border border-gray-300 bg-white text-sm font-medium text-gray-700 hover:bg-gray-50">
                            2
                        </a>
                        <a href="#" class="relative inline-flex items-center px-4 py-2 border border-gray-300 bg-white text-sm font-medium text-gray-700 hover:bg-gray-50">
                            3
                        </a>
                        <a href="#" class="relative inline-flex items-center px-2 py-2 rounded-r-md border border-gray-300 bg-white text-sm font-medium text-gray-500 hover:bg-gray-50">
                            <span class="sr-only">下一页</span>
                            <i class="fas fa-chevron-right"></i>
                        </a>
                    </nav>
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
</body>
</html> 