<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<!DOCTYPE html>
<html>
<head>
    <!-- 用户注册页面 -->
    <!-- todo 超级管理员注册待完善 -->
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>学生晚归预警系统 - 用户注册</title>
<%--    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/css/bootstrap.min.css" rel="stylesheet">--%>
    <!-- 引入 CSS 文件 -->
    <link href="${pageContext.request.contextPath}/static/css/tailwind.min.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/static/css/fontawesome.min.css" rel="stylesheet">
    <link href="${pageContext.request.contextPath}/static/css/bootstrap.min.css" rel="stylesheet">

    <!-- 引入 JS 文件 -->
    <script src="${pageContext.request.contextPath}/static/js/jquery.min.js"></script>
    <script src="${pageContext.request.contextPath}/static/js/popper.min.js"></script>
    <script src="${pageContext.request.contextPath}/static/js/bootstrap.bundle.min.js"></script>
    <style>
        body {
            min-height: 100vh;
            display: flex;
            align-items: center;
            justify-content: center;
            margin: 0;
            padding: 20px;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        }
        .container {
            max-width: 500px;
            width: 100%;
        }
        .card {
            border: none;
            border-radius: 20px;
            box-shadow: 0 10px 20px rgba(0,0,0,0.1);
            background: rgba(255,255,255,0.95);
            padding: 30px;
        }
        .card-header {
            background: none;
            border-bottom: none;
            padding: 0 0 30px 0;
            text-align: center;
        }
        .card-header h3 {
            color: #333;
            font-weight: 600;
            margin-bottom: 10px;
            font-size: 24px;
        }
        .form-group {
            margin-bottom: 20px;
        }
        .form-label {
            color: #666;
            font-weight: 500;
            margin-bottom: 8px;
            display: block;
        }
        .form-control {
            border-radius: 8px;
            padding: 12px;
            border: 1px solid #ddd;
            background-color: #f8f9fa;
            width: 100%;
        }
        .form-control:focus {
            box-shadow: 0 0 0 3px rgba(102,126,234,0.2);
            border-color: #667eea;
            background-color: #fff;
        }
        .error {
            color: #dc3545;
            font-size: 12px;
            margin-top: 4px;
            display: inline-block;
            margin-left: 8px;
        }
        .input-wrapper {
            position: relative;
            margin-bottom: 10px;
        }
        .label-wrapper {
            display: flex;
            align-items: center;
            margin-bottom: 8px;
        }
        .captcha-container {
            display: flex;
            gap: 10px;
            width: 100%;
        }
        .captcha-container .form-control {
            flex: 1;
        }
        .captcha-image {
            height: 46px;
            border-radius: 8px;
            cursor: pointer;
        }
        .email-code-container {
            display: flex;
            gap: 10px;
            width: 100%;
        }
        .email-code-container .form-control {
            flex: 1;
        }
        .btn-send-code {
            white-space: nowrap;
            min-width: 120px;
            background-color: #667eea;
            color: white;
            border: none;
            padding: 12px 20px;
            border-radius: 8px;
            font-weight: 500;
        }
        .btn-send-code:hover {
            background-color: #5a6edb;
            color: white;
        }
        .btn-send-code:disabled {
            background-color: #a5aee8;
        }
        .user-type-container {
            display: flex;
            gap: 40px;
            margin-left: 80px;
            justify-content: flex-start;
            padding: 10px 0;
        }
        .form-check {
            margin: 0;
            padding: 5px 10px;
            border-radius: 6px;
            transition: background-color 0.2s;
        }
        .form-check:hover {
            background-color: rgba(102,126,234,0.1);
        }
        .form-check-input:checked {
            background-color: #667eea;
            border-color: #667eea;
        }
        .form-check-label {
            padding-left: 4px;
            cursor: pointer;
        }
        .btn-register {
            margin-left: 80px;
            width: calc(100% - 80px);
            background-color: #667eea;
            border: none;
            padding: 12px;
            border-radius: 8px;
            font-weight: 500;
            margin-top: 20px;
            font-size: 16px;
            color: white;
        }
        .btn-register:hover {
            background-color: #5a6edb;
        }
        .login-link {
            text-align: center;
            margin-top: 15px;
            margin-left: 80px;
        }
        .login-link a {
            color: #667eea;
            text-decoration: none;
            font-weight: 500;
        }
        .login-link a:hover {
            color: #5a6edb;
            text-decoration: underline;
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="card">
            <div class="card-header">
                <h3>学生晚归预警系统</h3>
                <p class="text-muted">请注册您的账号</p>
            </div>
            <div class="card-body">
                <!-- 显示服务器端验证错误 -->
                <c:if test="${not empty error}">
                    <div class="alert alert-danger">
                        ${error}
                    </div>
                </c:if>
                
                <!-- 显示成功消息 -->
                <c:if test="${not empty success}">
                    <div class="alert alert-success">
                        ${success}
                    </div>
                </c:if>

                <form id="registerForm" action="${pageContext.request.contextPath}/register/do-register" method="post">
                    <!-- CSRF Token -->
                    <!-- todo 报错 -->
                    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                    
                    <div class="form-group">
                        <div class="label-wrapper">
                            <label class="form-label" for="name">用户名</label>
                            <div id="nameError" class="error"></div>
                        </div>
                        <div class="input-wrapper">
                            <input type="text" class="form-control" id="name" name="name" 
                                   value="${param.name}" placeholder="请输入用户名" required>
                        </div>
                    </div>
                    
                    <div class="form-group">
                        <div class="label-wrapper">
                            <label class="form-label" for="email">电子邮箱</label>
                            <div id="emailError" class="error"></div>
                        </div>
                        <div class="input-wrapper">
                            <div class="email-code-container">
                                <input type="email" class="form-control" id="email" name="email" 
                                       value="${param.email}" placeholder="请输入电子邮箱" required>
                                <button type="button" class="btn btn-send-code" id="sendEmailCode">获取验证码</button>
                            </div>
                        </div>
                    </div>

                    <div class="form-group">
                        <div class="label-wrapper">
                            <label class="form-label" for="antispamCaptcha">图形验证码</label>
                            <div id="captchaError" class="error"></div>
                        </div>
                        <div class="input-wrapper">
                            <div class="captcha-container">
                                <input type="text" class="form-control" id="antispamCaptcha" name="antispamCaptcha" 
                                       placeholder="请输入图形验证码" required>
                                <img id="captchaImage" class="captcha-image" src="${pageContext.request.contextPath}/register/captcha" 
                                     alt="验证码" title="点击刷新">
                            </div>
                        </div>
                    </div>
                    
                    <div class="form-group">
                        <div class="label-wrapper">
                            <label class="form-label" for="emailCaptcha">邮箱验证码</label>
                            <div id="emailCaptchaError" class="error"></div>
                        </div>
                        <div class="input-wrapper">
                            <input type="text" class="form-control" id="emailCaptcha" name="emailCaptcha" 
                                   placeholder="请输入邮箱验证码" required>
                        </div>
                    </div>
                    
                    <div class="form-group">
                        <div class="label-wrapper">
                            <label class="form-label" for="password">密码</label>
                            <div id="passwordError" class="error"></div>
                        </div>
                        <div class="input-wrapper">
                            <input type="password" class="form-control" id="password" name="password" 
                                   placeholder="请输入密码" required>
                        </div>
                    </div>
                    
                    <div class="form-group">
                        <div class="label-wrapper">
                            <label class="form-label" for="confirmPassword">确认密码</label>
                            <div id="confirmPasswordError" class="error"></div>
                        </div>
                        <div class="input-wrapper">
                            <input type="password" class="form-control" id="confirmPassword" name="confirmPassword" 
                                   placeholder="请再次输入密码" required>
                        </div>
                    </div>

                    <!-- 用户类型选择 -->
                    <div class="form-group">
                        <label class="form-label">用户类型</label>
                        <div class="user-type-container">
                            <div class="form-check">
                                <input class="form-check-input" type="radio" name="role" id="student" value="student" checked>
                                <label class="form-check-label" for="student">学生</label>
                            </div>
                            <div class="form-check">
                                <input class="form-check-input" type="radio" name="role" id="dormitoryManager" value="dormitorymanager">
                                <label class="form-check-label" for="dormitoryManager">宿管</label>
                            </div>
                            <div class="form-check">
                                <input class="form-check-input" type="radio" name="role" id="systemUser" value="systemuser">
                                <label class="form-check-label" for="systemUser">班级管理员</label>
                            </div>
                            <div class="form-check">
                                <input class="form-check-input" type="radio" name="role" id="superAdmin" value="superadmin">
                                <label class="form-check-label" for="superAdmin">超级管理员</label>
                            </div>
                        </div>
                    </div>

                    <button type="submit" class="btn btn-register">注册账号</button>
                    <div class="login-link">
                        <a href="${pageContext.request.contextPath}/login">已有账号？立即登录</a>
                    </div>
                </form>
            </div>
        </div>
    </div>

    <script src="https://code.jquery.com/jquery-3.6.0.min.js"></script>
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.1.3/dist/js/bootstrap.bundle.min.js"></script>

    <script>
        $.ajaxSetup({
            beforeSend: function(xhr) {
                var loginUUID = localStorage.getItem('loginUUID');
                if (loginUUID) {
                    xhr.setRequestHeader('X-Login-UUID', loginUUID);
                }
            }
        });

        $(document).ready(function () {
            // 刷新验证码
            function refreshCaptcha() {
                $('#captchaImage').attr('src', '${pageContext.request.contextPath}/register/captcha?' + new Date().getTime());
            }

            // 点击验证码图片刷新
            $('#captchaImage').click(function () {
                refreshCaptcha();
            });

            // 发送邮箱验证码
            $('#sendEmailCode').click(function () {
                var email = $('#email').val();
                var antispamCaptcha = $('#antispamCaptcha').val();

                if (!email) {
                    $('#emailError').text('请先输入邮箱地址');
                    return;
                }

                if (!antispamCaptcha) {
                    $('#captchaError').text('请先输入图形验证码');
                    return;
                }

                var $btn = $(this);
                $btn.prop('disabled', true);

                $.ajax({
                    url: '${pageContext.request.contextPath}/register/send-email-code',
                    type: 'POST',
                    data: {
                        email: email,
                        antispamCaptcha: antispamCaptcha
                    },
                    success: function (response) {
                        if (response.success) {
                            var countdown = 60;
                            var timer = setInterval(function () {
                                if (countdown > 0) {
                                    $btn.text(countdown + '秒后重试');
                                    countdown--;
                                } else {
                                    clearInterval(timer);
                                    $btn.prop('disabled', false).text('获取验证码');
                                }
                            }, 1000);
                        } else {
                            alert(response.message || '发送失败，请重试');
                            $btn.prop('disabled', false);
                            refreshCaptcha();
                        }
                    },
                    error: function () {
                        alert('发送失败，请重试');
                        $btn.prop('disabled', false);
                        refreshCaptcha();
                    }
                });
            });

            // 用户名验证
            function validateUsername() {
                var username = $('#name').val();
                var usernameError = $('#nameError');

                if (!username) {
                    usernameError.text('用户名不能为空');
                    return false;
                }

                if (!/[\u4e00-\u9fa5]/.test(username)) {
                    usernameError.text('用户名必须包含中文');
                    return false;
                }

                if (username.length < 2) {
                    usernameError.text('用户名长度必须大于等于2个字符');
                    return false;
                }

                usernameError.text('');
                return true;
            }

            // 邮箱验证
            function validateEmail() {
                var email = $('#email').val();
                var emailError = $('#emailError');

                if (!email) {
                    emailError.text('邮箱不能为空');
                    return false;
                }

                var emailRegex = /^[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,6}$/;
                if (!emailRegex.test(email)) {
                    emailError.text('请输入正确的邮箱格式');
                    return false;
                }

                emailError.text('');
                return true;
            }

            // 邮箱验证码验证
            function validateEmailCaptcha() {
                var emailCaptcha = $('#emailCaptcha').val();
                var emailCaptchaError = $('#emailCaptchaError');

                if (!emailCaptcha) {
                    emailCaptchaError.text('邮箱验证码不能为空');
                    return false;
                }

                emailCaptchaError.text('');
                return true;
            }

            // 图形验证码验证
            function validateCaptcha() {
                var captcha = $('#antispamCaptcha').val();
                var captchaError = $('#captchaError');

                if (!captcha) {
                    captchaError.text('图形验证码不能为空');
                    return false;
                }

                captchaError.text('');
                return true;
            }

            // 密码验证
            function validatePassword() {
                var password = $('#password').val();
                var passwordError = $('#passwordError');

                if (!password) {
                    passwordError.text('密码不能为空');
                    return false;
                }

                if (password.length < 6) {
                    passwordError.text('密码长度必须大于等于6位');
                    return false;
                }

                if (!/[a-z]/.test(password) || !/[A-Z]/.test(password)) {
                    passwordError.text('密码必须包含大小写字母');
                    return false;
                }

                passwordError.text('');
                return true;
            }

            // 确认密码验证
            function validateConfirmPassword() {
                var confirmPassword = $('#confirmPassword').val();
                var password = $('#password').val();
                var confirmPasswordError = $('#confirmPasswordError');

                if (!confirmPassword) {
                    confirmPasswordError.text('确认密码不能为空');
                    return false;
                }

                if (confirmPassword !== password) {
                    confirmPasswordError.text('两次输入的密码不一致');
                    return false;
                }

                confirmPasswordError.text('');
                return true;
            }

            // 为每个输入框添加验证事件
            $('#name').on('input blur', validateUsername);
            $('#email').on('input blur', validateEmail);
            $('#emailCaptcha').on('input blur', validateEmailCaptcha);
            $('#antispamCaptcha').on('input blur', validateCaptcha);
            $('#password').on('input blur', validatePassword);
            $('#confirmPassword').on('input blur', validateConfirmPassword);

            // 表单提交验证
            $('#registerForm').on('submit', function (e) {
                var isUsernameValid = validateUsername();
                var isEmailValid = validateEmail();
                var isEmailCaptchaValid = validateEmailCaptcha();
                var isCaptchaValid = validateCaptcha();
                var isPasswordValid = validatePassword();
                var isConfirmPasswordValid = validateConfirmPassword();

                if (!isUsernameValid || !isEmailValid || !isEmailCaptchaValid ||
                    !isCaptchaValid || !isPasswordValid || !isConfirmPasswordValid) {
                    e.preventDefault();
                }
            });

            // 自动隐藏提示消息
            $('.alert').delay(3000).fadeOut(500);
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