package com.tianhai.warn.component;

import com.tianhai.warn.annotation.ReceiverRole;
import com.tianhai.warn.constants.Constants;
import com.tianhai.warn.model.Student;
import com.tianhai.warn.service.ReceiverIdProvider;
import com.tianhai.warn.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("studentReceiverProvider")
@ReceiverRole(Constants.STUDENT)
public class StudentRoleBasedReceiverProvider implements ReceiverIdProvider {

    @Autowired
    private StudentService studentService;

    @Override
    public List<String> getReceiverIds() {
        return studentService.selectAll().stream()
                .map(Student::getStudentNo)
                .toList();
    }


}
