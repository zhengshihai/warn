
const Validation = {
    // 检查是否为空
    isEmpty: function(value) {
        return value === null || value === undefined || value.trim() === '';
    },

    // 检查是否为中文
    isChinese: function(value) {
        return /^[\u4e00-\u9fa5]+$/.test(value);
    },

    // 检查邮箱格式
    isEmail: function(value) {
        return /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/.test(value);
    },

    // 检查手机号格式
    isPhone: function(value) {
        return /^1[3-9]\d{9}$/.test(value);
    },

    // 检查密码长度
    isPasswordValid: function(value) {
        return value.length >= 6;
    }
};

// 宿管信息表单验证
function validateDormanForm() {
    const form = $('#dormitoryManagerForm');
    const name = form.find('#dorm_name').val();
    const building = form.find('#building').val();
    const phone = form.find('#dorm_phone').val();
    const email = form.find('#dorm_email').val();
    const password = form.find('#dorm_password').val();

    // 清除之前的错误提示
    $('.error-message').remove();

    // 验证姓名
    if (!name) {
        showError('dorm_name', '姓名不能为空');
        return false;
    }
    if (name.length > 50) {
        showError('dorm_name', '姓名长度不能超过50个字符');
        return false;
    }

    // 验证宿舍楼
    if (!building) {
        showError('building', '负责宿舍楼不能为空');
        return false;
    }

    // 验证手机号
    if (!phone) {
        showError('dorm_phone', '手机号不能为空');
        return false;
    }
    if (!/^1[3-9]\d{9}$/.test(phone)) {
        showError('dorm_phone', '请输入有效的手机号码');
        return false;
    }

    // 验证邮箱
    if (!email) {
        showError('dorm_email', '邮箱不能为空');
        return false;
    }
    if (!/^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/.test(email)) {
        showError('dorm_email', '请输入有效的邮箱地址');
        return false;
    }

    // 验证密码（如果填写了的话）
    if (password && password.length < 6) {
        showError('dorm_password', '密码长度不能少于6个字符');
        return false;
    }

    return true;
}

function showError(fieldId, message) {
    const field = $(`#${fieldId}`);
    const errorDiv = $('<div>')
        .addClass('error-message text-danger mt-1')
        .text(message);
    field.after(errorDiv);
    field.addClass('is-invalid');
} 