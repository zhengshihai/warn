<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<!DOCTYPE html>
<html>
<head>
    <title>报表统计 - 学生晚归预警系统</title>
    <script src="https://cdn.tailwindcss.com"></script>
    <link href="https://cdn.jsdelivr.net/npm/@fortawesome/fontawesome-free@6.0.0/css/all.min.css" rel="stylesheet">
    <script src="https://cdn.jsdelivr.net/npm/echarts@5.4.3/dist/echarts.min.js"></script>
    <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
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
                        <a href="/reports" class="border-indigo-500 text-gray-900 inline-flex items-center px-1 pt-1 border-b-2 text-sm font-medium">报表统计</a>
                        <a href="/messages" class="text-gray-500 hover:text-gray-700 px-3 py-2 rounded-md text-sm font-medium">消息中心</a>
                    </div>
                </div>
                <div class="flex items-center">
                    <div class="flex-shrink-0">
                        <span class="text-gray-500 mr-4">管理员</span>
                        <button class="bg-red-500 hover:bg-red-600 text-white px-4 py-2 rounded-md text-sm font-medium"  onclick="handleLogout()">退出</button>
                    </div>
                </div>
            </div>
        </div>
    </nav>

    <!-- 主要内容 -->
    <div class="max-w-7xl mx-auto py-6 sm:px-6 lg:px-8">
        <!-- 标题和导出按钮 -->
        <div class="flex justify-between items-center mb-6">
            <h1 class="text-2xl font-semibold text-gray-900">报表统计</h1>
            <div class="flex space-x-4">
                <button class="bg-green-500 hover:bg-green-600 text-white px-4 py-2 rounded-md text-sm font-medium">
                    <i class="fas fa-file-excel mr-2"></i>导出Excel
                </button>
                <button class="bg-red-500 hover:bg-red-600 text-white px-4 py-2 rounded-md text-sm font-medium">
                    <i class="fas fa-file-pdf mr-2"></i>导出PDF
                </button>
            </div>
        </div>

        <!-- 筛选条件 -->
        <div class="bg-white shadow rounded-lg p-6 mb-6">
            <div class="grid grid-cols-1 gap-6 sm:grid-cols-4">
                <div>
                    <label class="block text-sm font-medium text-gray-700">日期</label>
                    <select id="dateRangeSelect" class="mt-1 block w-full border border-gray-300 rounded-md shadow-sm py-2 px-3">
                        <option value="7">最近7天</option>
                        <option value="30">最近30天</option>
                        <option value="semester">本学期</option>
                        <option value="custom">自定义</option>
                    </select>
                    <!-- 自定义日期选择器，默认隐藏 -->
                    <div id="customDateRange" class="mt-2 hidden">
                        <div class="grid grid-cols-2 gap-4">
                            <div>
                                <label class="block text-sm font-medium text-gray-700">开始日期</label>
                                <input type="date" id="startDate" class="mt-1 block w-full border border-gray-300 rounded-md shadow-sm py-2 px-3">
                            </div>
                            <div>
                                <label class="block text-sm font-medium text-gray-700">结束日期</label>
                                <input type="date" id="endDate" class="mt-1 block w-full border border-gray-300 rounded-md shadow-sm py-2 px-3">
                            </div>
                        </div>
                    </div>
                </div>
                <div>
                    <label class="block text-sm font-medium text-gray-700">学院</label>
                    <select id="collegeSelect" class="mt-1 block w-full border border-gray-300 rounded-md shadow-sm py-2 px-3">
                        <option>全部</option>
                        <option>计算机学院</option>
                        <option>机械工程学院</option>
                        <option>经济管理学院</option>
                    </select>
                </div>
                <div>
                    <label class="block text-sm font-medium text-gray-700">宿舍楼</label>
                    <select id="dormitorySelect" class="mt-1 block w-full border border-gray-300 rounded-md shadow-sm py-2 px-3">
                        <option>全部</option>
                        <option>A栋</option>
                        <option>B栋</option>
                        <option>C栋</option>
                    </select>
                </div>
                <div class="flex items-end">
                    <button id="searchButton" class="w-full bg-indigo-500 hover:bg-indigo-600 text-white px-4 py-2 rounded-md text-sm font-medium">
                        <i class="fas fa-search mr-2"></i>查询
                    </button>
                </div>
            </div>
        </div>

        <!-- 统计卡片 -->
        <div class="grid grid-cols-1 gap-6 sm:grid-cols-4 mb-6">
            <div class="bg-white overflow-hidden shadow rounded-lg">
                <div class="p-5">
                    <div class="flex items-center">
                        <div class="flex-shrink-0 bg-indigo-500 rounded-md p-3">
                            <i class="fas fa-clock text-white text-xl"></i>
                        </div>
                        <div class="ml-5 w-0 flex-1">
                            <dl>
                                <dt class="text-sm font-medium text-gray-500 truncate">总晚归次数</dt>
                                <dd id="totalLateCount" class="text-3xl font-semibold text-gray-900">-</dd>
                            </dl>
                        </div>
                    </div>
                </div>
            </div>
            <div class="bg-white overflow-hidden shadow rounded-lg">
                <div class="p-5">
                    <div class="flex items-center">
                        <div class="flex-shrink-0 bg-yellow-500 rounded-md p-3">
                            <i class="fas fa-user-clock text-white text-xl"></i>
                        </div>
                        <div class="ml-5 w-0 flex-1">
                            <dl>
                                <dt class="text-sm font-medium text-gray-500 truncate">晚归学生数</dt>
                                <dd id="lateStudentCount" class="text-3xl font-semibold text-gray-900">-</dd>
                            </dl>
                        </div>
                    </div>
                </div>
            </div>
            <div class="bg-white overflow-hidden shadow rounded-lg">
                <div class="p-5">
                    <div class="flex items-center">
                        <div class="flex-shrink-0 bg-red-500 rounded-md p-3">
                            <i class="fas fa-exclamation-triangle text-white text-xl"></i>
                        </div>
                        <div class="ml-5 w-0 flex-1">
                            <dl>
                                <dt class="text-sm font-medium text-gray-500 truncate">高危预警数</dt>
                                <dd id="highRiskCount" class="text-3xl font-semibold text-gray-900">-</dd>
                            </dl>
                        </div>
                    </div>
                </div>
            </div>
            <div class="bg-white overflow-hidden shadow rounded-lg">
                <div class="p-5">
                    <div class="flex items-center">
                        <div class="flex-shrink-0 bg-green-500 rounded-md p-3">
                            <i class="fas fa-check-circle text-white text-xl"></i>
                        </div>
                        <div class="ml-5 w-0 flex-1">
                            <dl>
                                <dt class="text-sm font-medium text-gray-500 truncate">处理完成率</dt>
                                <dd id="completionRate" class="text-3xl font-semibold text-gray-900">-</dd>
                            </dl>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <!-- 图表区域 -->
        <div class="grid grid-cols-1 gap-6 sm:grid-cols-2">
            <!-- 晚归趋势图 -->
            <div class="bg-white shadow rounded-lg p-6">
                <h3 class="text-lg font-medium text-gray-900 mb-4">晚归趋势分析</h3>
                <div id="trendChart" style="width: 100%; height: 400px;"></div>
            </div>
            <!-- 学院分布图 -->
            <div class="bg-white shadow rounded-lg p-6">
                <h3 class="text-lg font-medium text-gray-900 mb-4">学院分布统计</h3>
                <div id="collegeChart" style="width: 100%; height: 400px;"></div>
            </div>
            <!-- 时间段分布图 -->
            <div class="bg-white shadow rounded-lg p-6">
                <h3 class="text-lg font-medium text-gray-900 mb-4">晚归时间段分布</h3>
                <div id="timeChart" style="width: 100%; height: 400px;"></div>
            </div>
            <!-- 宿舍楼统计图 -->
            <div class="bg-white shadow rounded-lg p-6">
                <h3 class="text-lg font-medium text-gray-900 mb-4">宿舍楼晚归统计</h3>
                <div id="dormChart" style="width: 100%; height: 400px;"></div>
            </div>
        </div>
    </div>

    <script>
        // 退出登录
        function handleLogout() {
            if (confirm('确定要退出登录吗？')) {
                window.location.href = '${pageContext.request.contextPath}/logout';
            }
        }

        let startDate, endDate, rangeDays;

        // 初始化日期选择器
        $(document).ready(function() {
            // 设置默认日期范围（最近30天）
            const today = new Date();
            const thirtyDaysAgo = new Date(today.getTime() - 30 * 24 * 60 * 60 * 1000);
            
            // 格式化日期
            const formattedToday = formatDate(today);
            const formattedThirtyDaysAgo = formatDate(thirtyDaysAgo);
            
            // 设置日期输入框的默认值
            $('#startDate').val(formattedThirtyDaysAgo);
            $('#endDate').val(formattedToday);
            
            // 初始化全局变量
            startDate = formattedThirtyDaysAgo;
            endDate = formattedToday;
            rangeDays = 30; // 初始化默认的 rangeDays
            
            // 再次确认日期变量是否为有效格式
            if (!startDate || !endDate || !/^\d{4}-\d{2}-\d{2}$/.test(startDate) || !/^\d{4}-\d{2}-\d{2}$/.test(endDate)) {
                 startDate = '2000-01-01'; // 设置一个非常早的默认开始日期
                 endDate = formattedToday; // 结束日期仍然用今天
                 alert("日期初始化失败，已设置为默认范围。"); // 提示用户使用了默认范围
            }
            
            // 获取默认选中的学院和宿舍楼值
            const initialCollege = $('#collegeSelect').val() === '全部' ? 'ALL' : $('#collegeSelect').val();
            const initialDormitory = $('#dormitorySelect').val() === '全部' ? 'ALL' : $('#dormitorySelect').val();
            
            // 获取初始数据，传递所有必需参数
            getReportCardData(startDate, endDate, initialCollege, initialDormitory);
            getHighRiskCount(startDate, endDate, initialCollege, initialDormitory);
        });

        // 添加查询按钮点击事件监听
        $('#searchButton').click(function() {
            if (!startDate || !endDate || !/^\d{4}-\d{2}-\d{2}$/.test(startDate) || !/^\d{4}-\d{2}-\d{2}$/.test(endDate)) {
                alert('请选择有效的日期范围');
                return;
            }
            const selectedDateRange = $('#dateRangeSelect').val();

            // 如果选择了自定义日期，并且 startDate 或 endDate 未定义（用户未选择）
            if (selectedDateRange === 'custom' && (!startDate || !endDate)) {
                 alert('请选择自定义日期范围');
                 return; // 不发起请求
            }

            // 如果选择了非自定义日期，但全局日期变量未定义（理论上不会发生，但作为双重检查）
             if (selectedDateRange !== 'custom' && (rangeDays === undefined)) {
                 alert('日期范围未正确设置，请重新选择');
                 return; // 不发起请求
            }

            // 获取学院和宿舍楼的值
            const selectedCollege = $('#collegeSelect').val() === '全部' ? 'ALL' : $('#collegeSelect').val();
            const selectedDormitory = $('#dormitorySelect').val() === '全部' ? 'ALL' : $('#dormitorySelect').val();

            // 调用函数发起 Ajax 请求，并传递学院和宿舍楼参数
            getReportCardData(startDate, endDate, selectedCollege, selectedDormitory);
            getHighRiskCount(startDate, endDate, selectedCollege, selectedDormitory);
        });

        // 监听日期范围选择变化
        $('#dateRangeSelect').change(function() {
            const value = $(this).val();
            const today = new Date();

            if (value === 'custom') {
                // 显示自定义日期选择器
                $('#customDateRange').removeClass('hidden');
                // 当切换到自定义日期时，清空日期输入框，避免显示旧的日期
                $('#startDate').val('');
                $('#endDate').val('');
                startDate = undefined;
                endDate = undefined;
            } else {
                // 隐藏自定义日期选择器
                $('#customDateRange').addClass('hidden');

                // 设置日期范围
                let startDateObj;
                if (value === 'semester') {
                    // 这里需要根据实际情况设置学期开始日期
                    startDateObj = getSemesterStartDate();
                } else {
                    const days = parseInt(value);
                    startDateObj = new Date(today.getTime() - days * 24 * 60 * 60 * 1000);
                }
                // 检查startDateObj和today是否有效
                if (isNaN(startDateObj.getTime()) || isNaN(today.getTime())) {
                    alert('日期计算出错，请刷新页面');
                    return;
                }
                startDate = formatDate(startDateObj);
                endDate = formatDate(today);
            
                $('#startDate').val(startDate);
                $('#endDate').val(endDate);
            }
        });

        // 监听自定义日期变化
        $('#startDate, #endDate').change(function() {
            const startVal = $('#startDate').val();
            const endVal = $('#endDate').val();

            // 只有当两个日期都选择了才进行验证和更新全局变量
            if (startVal && endVal) {
                const start = new Date(startVal);
                const end = new Date(endVal);

                // 验证日期范围
                if (start > end) {
                    alert('开始日期不能大于结束日期');
                    // 清空当前输入框或进行其他错误提示处理
                     $(this).val(''); // 可以选择清空当前输入框
                    // 同时将全局日期变量设置为无效状态，防止查询按钮使用错误日期
                    startDate = undefined;
                    endDate = undefined;
                    rangeDays = undefined;
                    return;
                }

                // 更新全局变量
                startDate = startVal;
                endDate = endVal;
                rangeDays = Math.ceil((end - start) / (24 * 60 * 60 * 1000));

                // !!! 移除这里的 getReportCardData() 和 getHighRiskCount() 调用 !!!
                // getReportCardData();
                // getHighRiskCount(startDate, endDate);
            } else {
                 // 如果有任何一个日期被清空，将全局日期变量设置为无效状态
                startDate = undefined;
                endDate = undefined;
                rangeDays = undefined;
            }
        });

        // 监听学院和宿舍楼选择变化
        $('#collegeSelect, #dormitorySelect').change(function() {
            // !!! 移除这里的 getReportCardData() 和 getHighRiskCount() 调用 !!!
            // getReportCardData();
            // getHighRiskCount(startDate, endDate);

            // 这里可以根据需要执行一些与数据请求无关的逻辑，比如更新全局变量
            // 但不应该在这里发起后端请求
        });

        // 格式化日期为 YYYY-MM-DD
        function formatDate(date) {
            if (!date || isNaN(date.getTime())) {
                return '';
            }
            var year = date.getFullYear();
            var month = String(date.getMonth() + 1).padStart(2, '0');
            var day = String(date.getDate()).padStart(2, '0');
            return year + '-' + month + '-' + day;
        }

        // todo 获取学期开始日期（需要根据实际情况实现）
        function getSemesterStartDate() {
            // 这里需要根据实际情况返回学期开始日期
            // 示例：假设当前学期从2024年2月1日开始
            return new Date('2024-02-01');
        }

        // 定义 Ajax 请求函数 todo 需要适配学院 宿舍楼条件
        function getHighRiskCount(startDate, endDate, college, dormitoryBuilding) {
            $.ajax({
                url: '${pageContext.request.contextPath}/reports/high-risk-count',
                type: 'GET',
                data: {
                    startDate: startDate,
                    endDate: endDate,
                    college: college,
                    dormitoryBuilding: dormitoryBuilding
                },
                success: function(response) {
                    // 获取 Result 中的 data
                    const result = response.data;

                    // 检查 Result 的 success 状态
                    if (!response.success) {
                        $('#highRiskCount').text('待修复');
                        return;
                    }

                    if (result.status === 'CALCULATING') {
                        // 显示计算中状态
                        $('#highRiskCount').text('计算中');

                        // 轮询检查任务状态
                        checkTaskStatus(result.taskId);
                    } else if (result.status === 'COMPLETED') {
                        // 直接显示结果
                        $('#highRiskCount').text(result.count);
                    } else {
                        // 显示错误状态
                        $('#highRiskCount').text('待修复');
                    }
                },
                error: function() {
                    $('#highRiskCount').text('待修复');
                }
            });
        }

        // 轮询检查任务状态
        function checkTaskStatus(taskId) {
            $.ajax({
                url:'${pageContext.request.contextPath}/reports/task-status/' + taskId,
                type: 'GET',
                success: function(response) {
                    // 获取 Result 中的 data
                    const status = response.data;

                    // 检查 Result 的 success 状态
                    if (!response.success) {
                        $('#highRiskCount').text('待修复');
                        return;
                    }

                    if (status.status === 'COMPLETED') {
                        // 显示计算结果
                        $('#highRiskCount').text(status.count);
                    } else if (status.status === 'FAILED') {
                        // 显示错误状态
                        $('#highRiskCount').text('待修复');
                    } else {
                        // 继续轮询
                        setTimeout(function() {
                            checkTaskStatus(taskId);
                        }, 1000);
                    }
                },
                error: function() {
                    $('#highRiskCount').text('待修复');
                }
            });
        }

        // 定义获取统计卡片数据的函数
        function getReportCardData(startDate, endDate, college, dormitoryBuilding) {
            // 获取学院和宿舍楼的选择值
            // const college = $('#collegeSelect').val() === '全部' ? 'ALL' : $('#collegeSelect').val();
            // const dormitoryBuilding = $('#dormitorySelect').val() === '全部' ? 'ALL' : $('#dormitorySelect').val();
            
            $.ajax({
                url: '${pageContext.request.contextPath}/reports/cardData',
                type: 'GET',
                data: {
                    startDate: startDate,
                    endDate: endDate,
                    college: college,
                    dormitoryBuilding: dormitoryBuilding
                },
                success: function(response) {
                    if (!response.data.status === 'FAILED') {
                        $('#totalLateCount').text('待修复');
                        $('#lateStudentCount').text('待修复');
                        $('#completionRate').text('待修复');
                        return;
                    }
                    
                    const data = response.data;
                    
                    // 更新总晚归次数
                    $('#totalLateCount').text(data.totalLateReturns.toLocaleString());
                    
                    // 更新晚归学生数
                    $('#lateStudentCount').text(data.lateStudentCount.toLocaleString());
                    
                    // 更新处理完成率（后端已经加了百分号）
                    $('#completionRate').text(data.completionRate);
                },
                error: function() {
                    // 显示错误状态
                    $('#totalLateCount').text('待修复');
                    $('#lateStudentCount').text('待修复');
                    $('#completionRate').text('待修复');
                }
            });
        }

        // 初始化图表
        const trendChart = echarts.init(document.getElementById('trendChart'));
        const collegeChart = echarts.init(document.getElementById('collegeChart'));
        const timeChart = echarts.init(document.getElementById('timeChart'));
        const dormChart = echarts.init(document.getElementById('dormChart'));

        // 晚归趋势图配置
        trendChart.setOption({
            title: { text: '' },
            tooltip: { trigger: 'axis' },
            legend: { data: ['晚归人数', '预警次数'] },
            xAxis: {
                type: 'category',
                data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
            },
            yAxis: { type: 'value' },
            series: [
                {
                    name: '晚归人数',
                    type: 'line',
                    data: [120, 132, 101, 134, 90, 230, 210]
                },
                {
                    name: '预警次数',
                    type: 'line',
                    data: [20, 32, 11, 34, 10, 30, 20]
                }
            ]
        });

        // 学院分布图配置
        collegeChart.setOption({
            tooltip: { trigger: 'item' },
            legend: { orient: 'vertical', left: 'left' },
            series: [
                {
                    type: 'pie',
                    radius: '50%',
                    data: [
                        { value: 235, name: '计算机学院' },
                        { value: 274, name: '机械工程学院' },
                        { value: 310, name: '经济管理学院' },
                        { value: 335, name: '外国语学院' },
                        { value: 400, name: '其他学院' }
                    ]
                }
            ]
        });

        // 时间段分布图配置
        timeChart.setOption({
            tooltip: { trigger: 'axis' },
            xAxis: {
                type: 'category',
                data: ['22:00', '23:00', '00:00', '01:00', '02:00', '03:00']
            },
            yAxis: { type: 'value' },
            series: [
                {
                    type: 'bar',
                    data: [150, 230, 224, 218, 135, 47]
                }
            ]
        });

        // 宿舍楼统计图配置
        dormChart.setOption({
            tooltip: { trigger: 'axis' },
            xAxis: {
                type: 'category',
                data: ['1号楼', '2号楼', '3号楼', '4号楼', '5号楼']
            },
            yAxis: { type: 'value' },
            series: [
                {
                    type: 'bar',
                    data: [120, 200, 150, 80, 70]
                }
            ]
        });

        // 响应窗口大小变化
        window.addEventListener('resize', function() {
            trendChart.resize();
            collegeChart.resize();
            timeChart.resize();
            dormChart.resize();
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