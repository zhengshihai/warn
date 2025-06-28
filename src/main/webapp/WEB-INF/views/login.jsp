<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<!DOCTYPE html>
<html>
<head>
    <!-- 页面一 登录页面 -->
    <title>学生晚归预警系统 - 登录</title>
<%--    <link href="https://cdn.bootcdn.net/ajax/libs/tailwindcss/2.2.19/tailwind.min.css" rel="stylesheet">--%>
<%--    <script src="https://cdn.bootcdn.net/ajax/libs/jquery/3.6.0/jquery.min.js"></script>--%>

    <!-- 引入 CSS 文件 -->
    <link href="${pageContext.request.contextPath}/static/css/tailwind.min.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/static/css/fontawesome.min.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/static/css/bootstrap.min.css" rel="stylesheet">

    <!-- 引入 JS 文件 -->
    <script src="${pageContext.request.contextPath}/static/js/jquery.min.js"></script>
    <script src="${pageContext.request.contextPath}/static/js/popper.min.js"></script>
    <script src="${pageContext.request.contextPath}/static/js/bootstrap.bundle.min.js"></script>
    <style>
        .login-container {
            min-height: 100vh;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        }
        .login-card {
            background: rgba(255, 255, 255, 0.95);
            border-radius: 1rem;
            box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
        }
    </style>
</head>
<body>
    <div class="login-container flex items-center justify-center p-4">
        <div class="login-card w-full max-w-md p-8">
            <div class="text-center mb-8">
                <h1 class="text-3xl font-bold text-gray-800">学生晚归预警系统</h1>
                <p class="text-gray-600 mt-2">请登录您的账号</p>
            </div>
            
            <div id="error-message" class="mb-4 p-4 text-sm text-red-700 bg-red-100 rounded-lg hidden" role="alert">
            </div>
            
            <form id="loginForm" class="space-y-6">
                <div>
                    <label class="block text-sm font-medium text-gray-700">姓名</label>
                    <input type="text" name="name" required class="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-indigo-500 focus:border-indigo-500">
                </div>
                
                <div>
                    <label class="block text-sm font-medium text-gray-700">邮箱</label>
                    <input type="email" name="email" required class="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-indigo-500 focus:border-indigo-500">
                </div>
                
                <div>
                    <label class="block text-sm font-medium text-gray-700">密码</label>
                    <input type="password" name="password" required class="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-indigo-500 focus:border-indigo-500">
                </div>
                
                <div>
                    <label class="block text-sm font-medium text-gray-700">身份选择</label>
                    <select name="role" class="mt-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-indigo-500 focus:border-indigo-500">
                        <option value="student">学生</option>
                        <option value="dormitorymanager">宿管</option>
                        <option value="systemuser">班级管理员</option>
                        <option value="superadmin">超级管理员</option>
                    </select>
                </div>
                
                <div class="flex items-center justify-between">
                    <div class="flex items-center">
                        <input type="checkbox" name="remember" class="h-4 w-4 text-indigo-600 focus:ring-indigo-500 border-gray-300 rounded">
                        <label class="ml-2 block text-sm text-gray-900">记住我</label>
                    </div>
                    <a href="#" class="text-sm text-indigo-600 hover:text-indigo-500">忘记密码？</a>
                </div>
                
                <button type="submit" class="w-full flex justify-center py-2 px-4 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500">
                    登录
                </button>
            </form>
        </div>
    </div>

    <script>
        $(document).ready(function() {
            $.ajaxSetup({
                beforeSend: function(xhr) {
                    var loginUUID = localStorage.getItem('loginUUID');
                    if (loginUUID) {
                        xhr.setRequestHeader('X-Login-UUID', loginUUID);
                    }
                }
            });

            $('#loginForm').on('submit', function(e) {
                e.preventDefault();
                
                // 隐藏之前的错误信息
                $('#error-message').addClass('hidden');
                
                // 获取表单数据
                var formData = {
                    name: $('input[name="name"]').val(),
                    email: $('input[name="email"]').val(),
                    password: $('input[name="password"]').val(),
                    role: $('select[name="role"]').val(),
                    remember: $('input[name="remember"]').is(':checked')
                };
                
                // 发送AJAX请求
                $.ajax({
                    type: 'POST',
                    url: '${pageContext.request.contextPath}/do-login',
                    data: formData,
                    xhrFields: { withCredentials: true }, // 允许跨域携带cookie（如有需要）
                    success: function(response) {
                        if (response.success) {
                            // 仅供调试，生产环境无需手动存储
                            if (response.data && response.data.token) {
                                console.log('JWT Token:', response.data.token);
                            }
                            console.log('用户角色:', response.data.role);
                            console.log('职业角色:', response.data.job_role);
                            console.log('用户数据:', response.data);

                            // 保存loginUUID 到localStorage
                            localStorage.setItem('loginUUID', response.data.loginUUID);


                            // 登录成功，根据角色跳转到不同页面
                            switch(response.data.role) {
                                case 'student':
                                    window.location.href = '${pageContext.request.contextPath}/student';
                                    break;
                                case 'dormitorymanager':
                                    window.location.href = '${pageContext.request.contextPath}/staff-dashboard';
                                    break;
                                case 'systemuser':
                                    // 根据 job_role 进行不同的页面跳转
                                    if (!response.data.job_role) {
                                        console.warn('未获取到用户职业角色信息，使用默认跳转');
                                        console.log('当前用户数据:', response.data);  // 打印当前用户数据
                                        window.location.href = '${pageContext.request.contextPath}/staff-dashboard';
                                        break;
                                    }
                                    
                                    // 将 job_role 转换为小写进行比较
                                    var jobRole = response.data.job_role.toLowerCase();
                                    switch(jobRole) {
                                        case 'counselor':
                                        case 'class_teacher':
                                            console.log('辅导员/班主任登录，跳转到 staff-dashboard');
                                            window.location.href = '${pageContext.request.contextPath}/staff-dashboard';
                                            break;
                                        case 'dean':
                                            console.log('院级领导登录，跳转到 dean');
                                            window.location.href = '${pageContext.request.contextPath}/dean';
                                            break;
                                        default:
                                            console.warn('未知的职业角色：' + response.data.job_role);
                                            window.location.href = '${pageContext.request.contextPath}/staff-dashboard';
                                    }
                                    break;
                                case 'superadmin':
                                    window.location.href = '${pageContext.request.contextPath}/super-admin';
                                    break;
                                default:
                                    console.error('未知的角色:', response.data.role);  // 添加调试日志
                                    $('#error-message').text('未知的用户角色').removeClass('hidden');
                            }
                        } else {
                            // 显示错误信息
                            $('#error-message').text(response.message || '登录失败').removeClass('hidden');
                        }
                    },
                    error: function(xhr, status, error) {
                        console.error('登录请求失败:', error);  // 添加调试日志
                        // 显示错误信息
                        $('#error-message').text('登录失败，请检查您的账号密码是否正确，或稍后重试').removeClass('hidden');
                    }
                });
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