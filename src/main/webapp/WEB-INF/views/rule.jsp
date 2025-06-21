<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<!DOCTYPE html>
<html>
<head>
    <title>学生晚归预警系统-系统管理</title>
<%--    <link href="https://cdn.bootcdn.net/ajax/libs/tailwindcss/2.2.19/tailwind.min.css" rel="stylesheet">--%>
<%--    <link href="https://cdn.bootcdn.net/ajax/libs/font-awesome/6.0.0/css/all.min.css" rel="stylesheet">--%>
    <!-- 引入 CSS 文件 -->
    <link href="${pageContext.request.contextPath}/static/css/tailwind.min.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/static/css/fontawesome.min.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/static/css/bootstrap.min.css" rel="stylesheet">

    <!-- 引入 JS 文件 -->
    <script src="${pageContext.request.contextPath}/static/js/jquery.min.js"></script>
    <script src="${pageContext.request.contextPath}/static/js/popper.min.js"></script>
    <script src="${pageContext.request.contextPath}/static/js/bootstrap.bundle.min.js"></script>
    <style>
        .admin-card {
            background: white;
            border-radius: 0.5rem;
            box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
        }
        .tab-active {
            border-bottom: 2px solid #4F46E5;
            color: #4F46E5;
        }
        .status-badge {
            padding: 0.25rem 0.75rem;
            border-radius: 9999px;
            font-size: 0.75rem;
            font-weight: 500;
        }
        .status-active {
            background-color: #D1FAE5;
            color: #065F46;
        }
        .status-inactive {
            background-color: #FEE2E2;
            color: #991B1B;
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
                    <span class="text-gray-600">欢迎，管理员</span>
                    <button class="text-sm text-red-600 hover:text-red-800">退出登录</button>
                </div>
            </div>
        </nav>

        <!-- 页面标题 -->
        <div class="mb-6">
            <h2 class="text-2xl font-bold text-gray-800">系统管理</h2>
            <p class="text-gray-600 mt-1">管理学生信息、宿管账户和系统规则</p>
        </div>

        <!-- 标签页导航 -->
        <div class="border-b border-gray-200 mb-6">
            <nav class="-mb-px flex space-x-8">
                <a href="#" class="tab-active py-4 px-1 font-medium text-sm">
                    学生信息管理
                </a>
                <a href="#" class="border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300 py-4 px-1 font-medium text-sm">
                    宿管账户管理
                </a>
                <a href="#" class="border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300 py-4 px-1 font-medium text-sm">
                    规则设置
                </a>
                <a href="#" class="border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300 py-4 px-1 font-medium text-sm">
                    系统日志
                </a>
            </nav>
        </div>

        <!-- 学生信息管理 -->
        <div class="admin-card p-6 mb-6">
            <div class="flex justify-between items-center mb-4">
                <h3 class="text-lg font-medium text-gray-900">学生信息管理</h3>
                <div class="flex space-x-2">
                    <button class="px-3 py-1 bg-indigo-600 text-white rounded-md hover:bg-indigo-700 text-sm">
                        导入学生数据
                    </button>
                    <button class="px-3 py-1 bg-green-600 text-white rounded-md hover:bg-green-700 text-sm">
                        添加学生
                    </button>
                </div>
            </div>

            <!-- 搜索和筛选 -->
            <div class="grid grid-cols-1 md:grid-cols-4 gap-4 mb-4">
                <div>
                    <input type="text" placeholder="搜索学号或姓名..." class="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-indigo-500 focus:border-indigo-500">
                </div>
                <div>
                    <select class="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-indigo-500 focus:border-indigo-500">
                        <option value="">全部学院</option>
                        <option value="computer">计算机学院</option>
                        <option value="economics">经济学院</option>
                        <option value="medical">医学院</option>
                        <option value="literature">文学院</option>
                    </select>
                </div>
                <div>
                    <select class="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-indigo-500 focus:border-indigo-500">
                        <option value="">全部年级</option>
                        <option value="2021">2021级</option>
                        <option value="2022">2022级</option>
                        <option value="2023">2023级</option>
                    </select>
                </div>
                <div>
                    <button class="w-full px-3 py-2 bg-indigo-600 text-white rounded-md hover:bg-indigo-700">
                        查询
                    </button>
                </div>
            </div>

            <!-- 学生信息表格 -->
            <div class="overflow-x-auto">
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
                                班级
                            </th>
                            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                宿舍号
                            </th>
                            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                联系电话
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
                            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">计算机2101</td>
                            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">A栋-101</td>
                            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">13800138000</td>
                            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                                <a href="#" class="text-indigo-600 hover:text-indigo-900 mr-3">编辑</a>
                                <a href="#" class="text-red-600 hover:text-red-900">删除</a>
                            </td>
                        </tr>
                        <tr>
                            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">2021002</td>
                            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">李四</td>
                            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">经济学院</td>
                            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">经济2102</td>
                            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">B栋-202</td>
                            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">13900139000</td>
                            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                                <a href="#" class="text-indigo-600 hover:text-indigo-900 mr-3">编辑</a>
                                <a href="#" class="text-red-600 hover:text-red-900">删除</a>
                            </td>
                        </tr>
                        <tr>
                            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">2021003</td>
                            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">王五</td>
                            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">医学院</td>
                            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">医学2103</td>
                            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">C栋-303</td>
                            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">13700137000</td>
                            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                                <a href="#" class="text-indigo-600 hover:text-indigo-900 mr-3">编辑</a>
                                <a href="#" class="text-red-600 hover:text-red-900">删除</a>
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>

            <!-- 分页 -->
            <div class="mt-4 flex items-center justify-between">
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
                            显示第 <span class="font-medium">1</span> 到 <span class="font-medium">3</span> 条，共 <span class="font-medium">97</span> 条记录
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

        <!-- 宿管账户管理 -->
        <div class="admin-card p-6 mb-6">
            <div class="flex justify-between items-center mb-4">
                <h3 class="text-lg font-medium text-gray-900">宿管账户管理</h3>
                <button class="px-3 py-1 bg-green-600 text-white rounded-md hover:bg-green-700 text-sm">
                    添加宿管
                </button>
            </div>

            <!-- 宿管账户表格 -->
            <div class="overflow-x-auto">
                <table class="min-w-full divide-y divide-gray-200">
                    <thead class="bg-gray-50">
                        <tr>
                            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                工号
                            </th>
                            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                姓名
                            </th>
                            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                负责宿舍楼
                            </th>
                            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                联系电话
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
                            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">D001</td>
                            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">赵六</td>
                            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">A栋</td>
                            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">13600136000</td>
                            <td class="px-6 py-4 whitespace-nowrap">
                                <span class="status-badge status-active">在职</span>
                            </td>
                            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                                <a href="#" class="text-indigo-600 hover:text-indigo-900 mr-3">编辑</a>
                                <a href="#" class="text-red-600 hover:text-red-900">停用</a>
                            </td>
                        </tr>
                        <tr>
                            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">D002</td>
                            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">钱七</td>
                            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">B栋</td>
                            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">13500135000</td>
                            <td class="px-6 py-4 whitespace-nowrap">
                                <span class="status-badge status-active">在职</span>
                            </td>
                            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                                <a href="#" class="text-indigo-600 hover:text-indigo-900 mr-3">编辑</a>
                                <a href="#" class="text-red-600 hover:text-red-900">停用</a>
                            </td>
                        </tr>
                        <tr>
                            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">D003</td>
                            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">孙八</td>
                            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">C栋</td>
                            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">13400134000</td>
                            <td class="px-6 py-4 whitespace-nowrap">
                                <span class="status-badge status-inactive">离职</span>
                            </td>
                            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                                <a href="#" class="text-indigo-600 hover:text-indigo-900 mr-3">编辑</a>
                                <a href="#" class="text-green-600 hover:text-green-900">启用</a>
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>
        </div>

        <!-- 规则设置 -->
        <div class="admin-card p-6 mb-6">
            <div class="flex justify-between items-center mb-4">
                <h3 class="text-lg font-medium text-gray-900">规则设置</h3>
                <button class="px-3 py-1 bg-indigo-600 text-white rounded-md hover:bg-indigo-700 text-sm">
                    保存设置
                </button>
            </div>

            <div class="space-y-6">
                <!-- 晚归时间定义 -->
                <div>
                    <h4 class="text-md font-medium text-gray-900 mb-2">晚归时间定义</h4>
                    <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
                        <div>
                            <label class="block text-sm font-medium text-gray-700 mb-1">工作日晚归时间</label>
                            <input type="time" value="22:30" class="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-indigo-500 focus:border-indigo-500">
                        </div>
                        <div>
                            <label class="block text-sm font-medium text-gray-700 mb-1">周末晚归时间</label>
                            <input type="time" value="23:00" class="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-indigo-500 focus:border-indigo-500">
                        </div>
                    </div>
                </div>

                <!-- 预警规则 -->
                <div>
                    <h4 class="text-md font-medium text-gray-900 mb-2">预警规则</h4>
                    <div class="space-y-4">
                        <div class="flex items-center">
                            <input type="checkbox" id="warning1" class="h-4 w-4 text-indigo-600 focus:ring-indigo-500 border-gray-300 rounded" checked>
                            <label for="warning1" class="ml-2 block text-sm text-gray-900">
                                连续晚归预警（连续两次晚归自动通知辅导员）
                            </label>
                        </div>
                        <div class="flex items-center">
                            <input type="checkbox" id="warning2" class="h-4 w-4 text-indigo-600 focus:ring-indigo-500 border-gray-300 rounded" checked>
                            <label for="warning2" class="ml-2 block text-sm text-gray-900">
                                月度晚归预警（每月晚归超过3次自动通知家长）
                            </label>
                        </div>
                        <div class="flex items-center">
                            <input type="checkbox" id="warning3" class="h-4 w-4 text-indigo-600 focus:ring-indigo-500 border-gray-300 rounded">
                            <label for="warning3" class="ml-2 block text-sm text-gray-900">
                                特殊时段预警（节假日期间晚归自动通知辅导员）
                            </label>
                        </div>
                    </div>
                </div>

                <!-- 通知模板 -->
                <div>
                    <h4 class="text-md font-medium text-gray-900 mb-2">通知模板</h4>
                    <div class="space-y-4">
                        <div>
                            <label class="block text-sm font-medium text-gray-700 mb-1">学生通知模板</label>
                            <textarea class="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-indigo-500 focus:border-indigo-500" rows="3">尊敬的{学生姓名}同学，您于{晚归时间}返回宿舍，已记录为晚归。如有特殊情况，请提交说明申请。</textarea>
                        </div>
                        <div>
                            <label class="block text-sm font-medium text-gray-700 mb-1">辅导员通知模板</label>
                            <textarea class="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-indigo-500 focus:border-indigo-500" rows="3">尊敬的辅导员，{学生姓名}（学号：{学号}）于{晚归时间}返回宿舍，已记录为晚归。请关注该学生的出勤情况。</textarea>
                        </div>
                        <div>
                            <label class="block text-sm font-medium text-gray-700 mb-1">家长通知模板</label>
                            <textarea class="w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-indigo-500 focus:border-indigo-500" rows="3">尊敬的家长，您的孩子{学生姓名}于{晚归时间}返回宿舍，已记录为晚归。请关注孩子的作息情况。</textarea>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <!-- 系统日志 -->
        <div class="admin-card p-6 mb-6">
            <div class="flex justify-between items-center mb-4">
                <h3 class="text-lg font-medium text-gray-900">系统日志</h3>
                <div class="flex space-x-2">
                    <select class="rounded-md border-gray-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500 text-sm">
                        <option value="all">全部日志</option>
                        <option value="login">登录日志</option>
                        <option value="operation">操作日志</option>
                        <option value="error">错误日志</option>
                    </select>
                    <button class="px-3 py-1 bg-indigo-600 text-white rounded-md hover:bg-indigo-700 text-sm">
                        导出日志
                    </button>
                </div>
            </div>

            <div class="overflow-x-auto">
                <table class="min-w-full divide-y divide-gray-200">
                    <thead class="bg-gray-50">
                        <tr>
                            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                时间
                            </th>
                            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                用户
                            </th>
                            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                操作
                            </th>
                            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                IP地址
                            </th>
                            <th class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                                状态
                            </th>
                        </tr>
                    </thead>
                    <tbody class="bg-white divide-y divide-gray-200">
                        <tr>
                            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">2024-04-14 10:30:15</td>
                            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">admin</td>
                            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">修改系统规则</td>
                            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">192.168.1.100</td>
                            <td class="px-6 py-4 whitespace-nowrap">
                                <span class="status-badge status-active">成功</span>
                            </td>
                        </tr>
                        <tr>
                            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">2024-04-14 09:15:22</td>
                            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">dormitory001</td>
                            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">处理晚归记录</td>
                            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">192.168.1.101</td>
                            <td class="px-6 py-4 whitespace-nowrap">
                                <span class="status-badge status-active">成功</span>
                            </td>
                        </tr>
                        <tr>
                            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">2024-04-13 23:45:10</td>
                            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">student2021001</td>
                            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">提交说明申请</td>
                            <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-900">192.168.1.102</td>
                            <td class="px-6 py-4 whitespace-nowrap">
                                <span class="status-badge status-active">成功</span>
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>

            <!-- 分页 -->
            <div class="mt-4 flex items-center justify-between">
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
                            显示第 <span class="font-medium">1</span> 到 <span class="font-medium">3</span> 条，共 <span class="font-medium">97</span> 条记录
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