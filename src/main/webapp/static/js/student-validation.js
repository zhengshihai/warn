// 学生修改个人信息表单验证工具类
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

    // 检查学号格式（假设学号是8-12位数字）
    isStudentNo: function(value) {
        return /^\d{8,12}$/.test(value);
    },

    // 检查宿舍号格式（假设格式为：楼号-房间号，如：1-101）
    isDormitory: function(value) {
        // return /^\d+-\d+$/.test(value);
        //todo 宿舍校验格式完善
        return true;
    }
};

// 学生信息表单验证
function validateStudentForm() {
    const form = $('#editProfileForm');
    const studentNo = form.find('#studentNo').val();
    const name = form.find('#name').val();
    const college = form.find('#college').val();
    const className = form.find('#className').val();
    const dormitory = form.find('#dormitory').val();
    const phone = form.find('#phone').val();
    const email = form.find('#email').val();
    const fatherName = form.find('#fatherName').val();
    const fatherPhone = form.find('#fatherPhone').val();
    const motherName = form.find('#motherName').val();
    const motherPhone = form.find('#motherPhone').val();

    // 检查必填字段
    if (Validation.isEmpty(studentNo)) {
        alert('学号不能为空');
        return false;
    }
    if (Validation.isEmpty(name)) {
        alert('姓名不能为空');
        return false;
    }
    if (Validation.isEmpty(college)) {
        alert('学院不能为空');
        return false;
    }
    if (Validation.isEmpty(className)) {
        alert('班级不能为空');
        return false;
    }
    if (Validation.isEmpty(dormitory)) {
        alert('宿舍号不能为空');
        return false;
    }
    if (Validation.isEmpty(phone)) {
        alert('联系电话不能为空');
        return false;
    }
    if (Validation.isEmpty(email)) {
        alert('电子邮箱不能为空');
        return false;
    }

    // 检查父母信息至少填写一项
    if (Validation.isEmpty(fatherName) && Validation.isEmpty(motherName)) {
        alert('父亲和母亲姓名不能同时为空');
        return false;
    }

    // 检查姓名是否为中文
    if (!Validation.isChinese(name)) {
        alert('学生姓名必须为中文');
        return false;
    }
    if (!Validation.isEmpty(fatherName) && !Validation.isChinese(fatherName)) {
        alert('父亲姓名必须为中文');
        return false;
    }
    if (!Validation.isEmpty(motherName) && !Validation.isChinese(motherName)) {
        alert('母亲姓名必须为中文');
        return false;
    }

    // 检查邮箱格式
    if (!Validation.isEmail(email)) {
        alert('电子邮箱格式不正确');
        return false;
    }

    // 检查手机号格式
    if (!Validation.isPhone(phone)) {
        alert('联系电话格式不正确');
        return false;
    }
    if (!Validation.isEmpty(fatherPhone) && !Validation.isPhone(fatherPhone)) {
        alert('父亲联系电话格式不正确');
        return false;
    }
    if (!Validation.isEmpty(motherPhone) && !Validation.isPhone(motherPhone)) {
        alert('母亲联系电话格式不正确');
        return false;
    }

    // 检查学号格式
    // if (!Validation.isStudentNo(studentNo)) {
    //     alert('学号格式不正确（8-12位数字）');
    //     return false;
    // }

    // 检查宿舍号格式
    // if (!Validation.isDormitory(dormitory)) {
    //     alert('宿舍号格式不正确（如：1-101）');
    //     return false;
    // }

    return true;
} 