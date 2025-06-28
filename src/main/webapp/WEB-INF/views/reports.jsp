<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<!DOCTYPE html>
<html>
<head>
    <title>报表统计 - 学生晚归预警系统</title>
    <script src="https://cdn.tailwindcss.com"></script>
<%--    <link href="https://cdn.jsdelivr.net/npm/@fortawesome/fontawesome-free@6.0.0/css/all.min.css" rel="stylesheet">--%>
<%--    <script src="https://cdn.jsdelivr.net/npm/echarts@5.4.3/dist/echarts.min.js"></script>--%>
<%--    <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>--%>
    <!-- 引入 CSS 文件 -->
    <link href="${pageContext.request.contextPath}/static/css/tailwind.min.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/static/css/fontawesome.min.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/static/css/bootstrap.min.css" rel="stylesheet">

    <!-- 引入 JS 文件 -->
    <script src="${pageContext.request.contextPath}/static/js/echarts.min.js"></script>
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
        <div class="grid grid-cols-1 gap-6">
            <!-- 宿舍门牌号统计图 -->
            <div class="bg-white shadow rounded-lg p-6">
                <h3 class="text-lg font-medium text-gray-900 mb-4">宿舍门牌号晚归统计</h3>
                <div id="dormRoomChart" style="width: 100%; height: 400px;"></div>
            </div>
            
            <!-- 其他图表区域 -->
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

        // 退出登录
        function handleLogout() {
            if (confirm('确定要退出登录吗？')) {
                localStorage.removeItem('loginUUID');
                window.location.href = '${pageContext.request.contextPath}/logout';
            }
        }

        let startDate, endDate, rangeDays;

        // 定义图表实例变量，使其可在外部访问
        let trendChart = null;
        let collegeChart = null;
        let timeChart = null;
        let dormChart = null;
        let dormRoomChart = null;

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

            // --- 添加这里：在页面加载完毕和初始日期设置好后，触发图表数据加载 ---
            // 初始化图表实例并赋值给全局变量
            trendChart = echarts.init(document.getElementById('trendChart'));
            collegeChart = echarts.init(document.getElementById('collegeChart'));
            timeChart = echarts.init(document.getElementById('timeChart'));
            dormChart = echarts.init(document.getElementById('dormChart'));
            dormRoomChart = echarts.init(document.getElementById('dormRoomChart'));

            // 调用加载函数
            loadTrendData();
            loadCollegeData();
            loadTimeData();
            loadDormData();
            loadDormRoomData(); // 加载宿舍门牌号数据
            // --- 结束添加 ---
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
            // !!! 添加这里：触发图表加载 !!!
            loadTrendData();
            loadCollegeData();
            loadTimeData();
            loadDormData();
            loadDormRoomData();
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
            } else {
                 // 如果有任何一个日期被清空，将全局日期变量设置为无效状态
                startDate = undefined;
                endDate = undefined;
                rangeDays = undefined;
            }
        });

        // 监听学院和宿舍楼选择变化
        $('#collegeSelect, #dormitorySelect').change(function() {
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

        // 定义 Ajax 请求函数
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
                    // 检查 Result 的 success 状态和 data 状态
                    if (!response.success || (response.data && response.data.status === 'FAILED')) {
                        $('#totalLateCount').text('待修复');
                        $('#lateStudentCount').text('待修复');
                        $('#completionRate').text('待修复');
                        return;
                    }

                    const data = response.data;

                    // 更新总晚归次数
                    $('#totalLateCount').text(data.totalLateReturns !== undefined && data.totalLateReturns !== null ? data.totalLateReturns.toLocaleString() : '-');

                    // 更新晚归学生数
                    $('#lateStudentCount').text(data.lateStudentCount !== undefined && data.lateStudentCount !== null ? data.lateStudentCount.toLocaleString() : '-');

                    // 更新处理完成率（后端已经加了百分号）
                    $('#completionRate').text(data.completionRate !== undefined && data.completionRate !== null ? data.completionRate : '-');
                },
                error: function() {
                    // 显示错误状态
                    $('#totalLateCount').text('待修复');
                    $('#lateStudentCount').text('待修复');
                    $('#completionRate').text('待修复');
                }
            });
        }

        // 晚归趋势图配置和加载数据函数
        function loadTrendData() {
            var option = {
            title: { text: '' },
            tooltip: { trigger: 'axis' },
            legend: { data: ['晚归人数'] },
            xAxis: {
                type: 'category',
                data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
            },
            yAxis: { type: 'value' },
            series: [
                {
                    name: '晚归人数',
                    type: 'line',
                        data: []
                    }
                ]
            };

            if (trendChart) { // 检查图表实例是否存在
                trendChart.setOption(option);

                $.ajax({
                    url: '${pageContext.request.contextPath}/reports/chart/week/late-return',
                    type: 'GET',
                    data: {
                        startDate: $('#startDate').val(),
                        endDate: $('#endDate').val(),
                        college: $('#collegeSelect').val() === '全部' ? 'ALL' : $('#collegeSelect').val(),
                        dormitoryBuilding: $('#dormitorySelect').val() === '全部' ? 'ALL' : $('#dormitorySelect').val()
                    },
                    dataType: 'json',  // 明确指定返回数据类型为 JSON
                    success: function(response) {
                        if (response.code === 200) {
                            var data = response.data;
                            var weekdays = ['周一', '周二', '周三', '周四', '周五', '周六', '周日'];
                            var counts = new Array(7).fill(0);

                            // 将后端返回的数据映射到对应的星期
                            data.forEach(function(item) {
                                var index = getWeekdayIndex(item.weekday);
                                if (index !== -1) {
                                    counts[index] = item.lateReturnCount;
                                }
                            });

                            trendChart.setOption({
                                series: [{
                                    data: counts
                                }]
                            });
                        } else {
                            console.error('获取趋势数据失败:', response.msg);
                        }
                    },
                    error: function(xhr, status, error) {
                        console.error('获取趋势数据失败:', error);
                        // 添加更详细的错误信息
                        console.error('Status:', status);
                        console.error('Response:', xhr.responseText);
                    }
                });
            }
        }

        // 将英文星期转换为索引 (保留此函数)
        function getWeekdayIndex(weekday) {
            var weekdayMap = {
                'Mon': 0, 'Tue': 1, 'Wed': 2, 'Thu': 3,
                'Fri': 4, 'Sat': 5, 'Sun': 6
            };
            return weekdayMap[weekday] !== undefined ? weekdayMap[weekday] : -1;
        }

        // 学院分布图配置和加载数据函数
        function loadCollegeData() {
            var option = {
                tooltip: {
                    trigger: 'item',
                    formatter: function(params) {
                        return params.name + '<br/>' +
                               '晚归人数: ' + params.value + '<br/>' +
                               '占比: ' + params.data.percentage + '%';
                    }
                },
                legend: {
                    orient: 'vertical',
                    left: 'left',
                    data: []
                },
            series: [
                {
                        name: '晚归人数',
                    type: 'pie',
                    radius: '50%',
                        data: []
                    }
                ]
            };

            if (collegeChart) { // 检查图表实例是否存在
                collegeChart.setOption(option);

                $.ajax({
                    url: '${pageContext.request.contextPath}/reports/chart/college',
                    type: 'GET',
                    data: {
                        startDate: $('#startDate').val(),
                        endDate: $('#endDate').val(),
                        college: $('#collegeSelect').val() === '全部' ? 'ALL' : $('#collegeSelect').val(),
                        dormitoryBuilding: $('#dormitorySelect').val() === '全部' ? 'ALL' : $('#dormitorySelect').val()
                    },
                    dataType: 'json',
                    success: function(response) {
                        if (response.code === 200) {
                            var data = response.data;
                            var chartData = data.map(function(item) {
                                return {
                                    name: item.college,
                                    value: item.count,
                                    percentage: item.percentage
                                };
                            });

                            collegeChart.setOption({
                                tooltip: {
                                    trigger: 'item',
                                    formatter: function(params) {
                                        return params.name + '<br/>' +
                                               '晚归人数: ' + params.value + '<br/>' +
                                               '占比: ' + params.data.percentage + '%';
                                    }
                                },
                                legend: {
                                    orient: 'vertical',
                                    left: 'left',
                                    data: data.map(function(item) { return item.college; })
                                },
                                series: [{
                                    name: '晚归人数',
                                    type: 'pie',
                                    radius: '50%',
                                    data: chartData
                                }]
                            });
                        } else {
                            console.error('获取学院分布数据失败:', response.msg);
                        }
                    },
                    error: function(xhr, status, error) {
                        console.error('获取学院分布数据失败:', error);
                        console.error('Status:', status);
                        console.error('Response:', xhr.responseText);
                    }
                });
            }
        }

        // 时间段分布图配置和加载数据函数
        function loadTimeData() {
             var option = {
                tooltip: {
                    trigger: 'axis',
                    axisPointer: {
                        type: 'shadow'
                    }
                },
            xAxis: {
                type: 'category',
                    data: ['22:00-23:00', '23:00-00:00', '00:00-01:00', '01:00-02:00', '02:00-03:00', '03:00-04:00']
            },
                yAxis: {
                    type: 'value',
                    name: '晚归人数'
                },
            series: [
                {
                        name: '晚归人数',
                    type: 'bar',
                        data: []
                    }
                ]
            };

            if (timeChart) { // 检查图表实例是否存在
                timeChart.setOption(option);

                $.ajax({
                    url: '${pageContext.request.contextPath}/reports/chart/time',
                    type: 'GET',
                    data: {
                        startDate: $('#startDate').val(),
                        endDate: $('#endDate').val(),
                        college: $('#collegeSelect').val() === '全部' ? 'ALL' : $('#collegeSelect').val(),
                        dormitoryBuilding: $('#dormitorySelect').val() === '全部' ? 'ALL' : $('#dormitorySelect').val()
                    },
                    dataType: 'json',
                    success: function(response) {
                        if (response.code === 200) {
                            var data = response.data;
                            var chartData = data.map(function(item) {
                                return {
                                    name: item.hourRange,
                                    value: item.lateReturnCount
                                };
                            });

                            timeChart.setOption({
                                tooltip: {
                                    formatter: function(params) {
                                        var data = params[0];
                                        return data.name + '<br/>' +
                                               '晚归人数: ' + data.value;
                                    }
                                },
                                series: [{
                                    data: chartData
                                }]
        });
                        } else {
                            console.error('获取时间段分布数据失败:', response.msg);
                        }
                    },
                    error: function(xhr, status, error) {
                        console.error('获取时间段分布数据失败:', error);
                        console.error('Status:', status);
                        console.error('Response:', xhr.responseText);
                    }
                });
            }
        }

        // 宿舍楼统计图配置和加载数据函数
        function loadDormData() {
             var option = {
                tooltip: {
                    trigger: 'axis',
                    axisPointer: {
                        type: 'shadow'
                    }
                },
                legend: {
                    data: ['晚归次数']
                },
            xAxis: {
                type: 'category',
                    data: []
            },
                yAxis: {
                    type: 'value',
                    name: '晚归次数'
                },
            series: [
                {
                        name: '晚归次数',
                    type: 'bar',
                        data: []
                    }
                ]
            };

            if (dormChart) { // 检查图表实例是否存在
                dormChart.setOption(option);

                $.ajax({
                    url: '${pageContext.request.contextPath}/reports/chart/dormitory',
                    type: 'GET',
                    data: {
                        startDate: $('#startDate').val(),
                        endDate: $('#endDate').val(),
                        college: $('#collegeSelect').val() === '全部' ? 'ALL' : $('#collegeSelect').val(),
                        dormitoryBuilding: $('#dormitorySelect').val() === '全部' ? 'ALL' : $('#dormitorySelect').val()
                    },
                    dataType: 'json',
                    success: function(response) {
                        if (response.code === 200) {
                            var buildingNames = [];
                            var buildingCounts = [];
                            var buildingList = response.data.building || [];
                            buildingList.forEach(function(item) {
                                if (item.dormitoryBuilding && item.totalCountByBuilding != null) {
                                    buildingNames.push(item.dormitoryBuilding);
                                    buildingCounts.push(item.totalCountByBuilding);
                                }
                            });
                            dormChart.setOption({
                                xAxis: {
                                    data: buildingNames
                                },
                                tooltip: {
                                    formatter: function(params) {
                                        var data = params[0];
                                        return data.name + '<br/>' +
                                               '晚归次数: ' + data.value;
                                    }
                                },
                                series: [{
                                    data: buildingCounts
                                }]
        });
                        } else {
                            console.error('获取宿舍楼统计数据失败:', response.msg);
                        }
                    },
                    error: function(xhr, status, error) {
                        console.error('获取宿舍楼统计数据失败:', error);
                        console.error('Status:', status);
                        console.error('Response:', xhr.responseText);
                    }
                });
            }
        }

        // 宿舍门牌号统计图配置和加载数据函数
        function loadDormRoomData() {
            var option = {
                tooltip: {
                    trigger: 'axis',
                    axisPointer: {
                        type: 'shadow'
                    }
                },
                grid: {
                    left: '3%',
                    right: '4%',
                    bottom: '15%',
                    containLabel: true
                },
                xAxis: {
                    type: 'category',
                    data: [],
                    axisLabel: {
                        interval: 0,
                        rotate: 45,
                        textStyle: {
                            fontSize: 12
                        }
                    }
                },
                yAxis: {
                    type: 'value',
                    name: '晚归次数'
                },
                dataZoom: [
                    {
                        type: 'slider',
                        show: true,
                        xAxisIndex: [0],
                        start: 0,
                        end: 100
                    },
                    {
                        type: 'inside',
                        xAxisIndex: [0],
                        start: 0,
                        end: 100
                    }
                ],
                series: [
                    {
                        name: '晚归次数',
                        type: 'line',
                        data: [],
                        markPoint: {
                            data: [
                                { type: 'max', name: '最大值' },
                                { type: 'min', name: '最小值' }
                            ]
                        },
                        markLine: {
                            data: [
                                { type: 'average', name: '平均值' }
                            ]
                        }
                    }
                ]
            };

             if (dormRoomChart) { // 检查图表实例是否存在
                dormRoomChart.setOption(option);

                $.ajax({
                    url: '${pageContext.request.contextPath}/reports/chart/dormitory',
                    type: 'GET',
                    data: {
                        startDate: $('#startDate').val(),
                        endDate: $('#endDate').val(),
                        college: $('#collegeSelect').val() === '全部' ? 'ALL' : $('#collegeSelect').val(),
                        dormitoryBuilding: $('#dormitorySelect').val() === '全部' ? 'ALL' : $('#dormitorySelect').val()
                    },
                    dataType: 'json',
                    success: function(response) {
                        console.log('宿舍门牌号数据接口返回:', response); // 添加日志
                        if (response.code === 200) {
                            var dormRooms = [];
                            var dormCounts = [];

                            // 处理宿舍门牌号数据
                            if (response.data && response.data.dormitory) {
                                response.data.dormitory.forEach(function(dorm) {
                                    // 确保 dormitory 和 totalCountByDormitory 存在且有效
                                    if (dorm.dormitory && dorm.totalCountByDormitory != null) {
                                        dormRooms.push(dorm.dormitory);
                                        dormCounts.push(dorm.totalCountByDormitory);
                                    }
                                });
                            }

                            console.log('提取的宿舍门牌号:', dormRooms); // 添加日志
                            console.log('提取的晚归次数:', dormCounts); // 添加日志

                            dormRoomChart.setOption({
                                xAxis: {
                                    data: dormRooms
                                },
                                series: [{
                                    data: dormCounts
                                }]
                            });
                        } else {
                            console.error('获取宿舍门牌号统计数据失败:', response.msg);
                        }
                    },
                    error: function(xhr, status, error) {
                        console.error('获取宿舍门牌号统计数据失败:', error);
                        console.error('Status:', status);
                        console.error('Response:', xhr.responseText);
                    }
                });
            }
        }

        // 响应窗口大小变化
        window.addEventListener('resize', function() {
            if (collegeChart) collegeChart.resize();
            if (timeChart) timeChart.resize();
            if (dormChart) dormChart.resize();
            if (dormRoomChart) dormRoomChart.resize();
            if (trendChart) trendChart.resize();
        });

        // 导出Excel按钮点击事件
        $('.bg-green-500').click(function() {
            // 获取当前筛选条件
            const dateRange = $('#dateRangeSelect').val();
            let startDate, endDate;

            if (dateRange === 'custom') {
                startDate = $('#startDate').val();
                endDate = $('#endDate').val();
            } else {
                const today = new Date();
                if (dateRange === '7') {
                    startDate = formatDate(new Date(today.getTime() - 7 * 24 * 60 * 60 * 1000));
                    endDate = formatDate(today);
                } else if (dateRange === '30') {
                    startDate = formatDate(new Date(today.getTime() - 30 * 24 * 60 * 60 * 1000));
                    endDate = formatDate(today);
                } else if (dateRange === 'semester') {
                    // 这里需要根据实际情况设置学期开始和结束日期
                    startDate = '2024-02-26'; // 示例：本学期开始日期
                    endDate = formatDate(today);
                }
            }

            const college = $('#collegeSelect').val() === '全部' ? 'ALL' : $('#collegeSelect').val();
            const dormitoryBuilding = $('#dormitorySelect').val() === '全部' ? 'ALL' : $('#dormitorySelect').val();

            // 显示加载提示
            const loadingToast = showLoadingToast('正在生成Excel文件，请稍候...');

            // 发起AJAX请求
            $.ajax({
                url: '${pageContext.request.contextPath}/reports/export/excel',
                method: 'GET',
                data: {
                    startDate: startDate,
                    endDate: endDate,
                    college: college,
                    dormitoryBuilding: dormitoryBuilding
                },
                xhrFields: {
                    responseType: 'blob' // 设置响应类型为blob
                },
                success: function(response, status, xhr) {
                    // 关闭加载提示
                    loadingToast.close();

                    // 从响应头中获取文件名
                    const contentDisposition = xhr.getResponseHeader('Content-Disposition');
                    let filename = '报表统计.xlsx';
                    if (contentDisposition) {
                        const filenameMatch = contentDisposition.match(/filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/);
                        if (filenameMatch && filenameMatch[1]) {
                            // 解码文件名
                            filename = decodeURIComponent(filenameMatch[1].replace(/['"]/g, ''));
                        }
                    }

                    // 创建下载链接
                    const blob = new Blob([response], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' });
                    const url = window.URL.createObjectURL(blob);
                    const a = document.createElement('a');
                    a.href = url;
                    a.download = filename;
                    document.body.appendChild(a);
                    a.click();
                    window.URL.revokeObjectURL(url);
                    document.body.removeChild(a);

                    // 显示成功提示
                    showSuccessToast('Excel文件导出成功！');
                },
                error: function(xhr, status, error) {
                    // 关闭加载提示
                    loadingToast.close();

                    // 显示错误提示
                    showErrorToast('导出Excel文件失败：' + (xhr.responseText || error));
                }
            });
        });

        // 显示加载提示
        function showLoadingToast(message) {
            const toast = $('<div>')
                .addClass('fixed top-4 right-4 bg-blue-500 text-white px-6 py-3 rounded-lg shadow-lg z-50')
                .text(message);
            $('body').append(toast);
            return {
                close: function() {
                    toast.fadeOut(300, function() {
                        $(this).remove();
                    });
                }
            };
        }

        // 显示成功提示
        function showSuccessToast(message) {
            const toast = $('<div>')
                .addClass('fixed top-4 right-4 bg-green-500 text-white px-6 py-3 rounded-lg shadow-lg z-50')
                .text(message);
            $('body').append(toast);
            setTimeout(() => {
                toast.fadeOut(300, function() {
                    $(this).remove();
                });
            }, 3000);
        }

        // 显示错误提示
        function showErrorToast(message) {
            const toast = $('<div>')
                .addClass('fixed top-4 right-4 bg-red-500 text-white px-6 py-3 rounded-lg shadow-lg z-50')
                .text(message);
            $('body').append(toast);
            setTimeout(() => {
                toast.fadeOut(300, function() {
                    $(this).remove();
                });
            }, 5000);
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