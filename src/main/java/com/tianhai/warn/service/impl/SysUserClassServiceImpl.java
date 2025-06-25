package com.tianhai.warn.service.impl;

import com.tianhai.warn.enums.ResultCode;
import com.tianhai.warn.exception.BusinessException;
import com.tianhai.warn.mapper.StudentMapper;
import com.tianhai.warn.mapper.SysUserClassMapper;
import com.tianhai.warn.model.SysUserClass;
import com.tianhai.warn.service.SysUserClassService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户-班级关联Service实现类
 */
@Service
public class SysUserClassServiceImpl implements SysUserClassService {
    private static final Logger logger = LoggerFactory.getLogger(SysUserClassServiceImpl.class);

    @Autowired
    private SysUserClassMapper sysUserClassMapper;

    @Autowired
    private StudentMapper studentMapper;

    @Override
    public List<String> getUserClasses(String sysUserNo) {
        return sysUserClassMapper.selectClassesByUserNo(sysUserNo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserClasses(String sysUserNo, List<String> classList) {
        try {
            //获取原有的的班级列表
            List<String> oldClassList = sysUserClassMapper.selectClassesByUserNo(sysUserNo);

            //先删除原有关联
            sysUserClassMapper.deleteByUserNo(sysUserNo);

            //添加新的关联
            if (classList != null && !classList.isEmpty()) {
                List<SysUserClass> userClasses = new ArrayList<>();
                for (String className : classList) {
                    SysUserClass userClass = new SysUserClass();
                    userClass.setSysUserNo(sysUserNo);
                    userClass.setClassName(className);
                    userClasses.add(userClass);
                }

                sysUserClassMapper.batchInsert(userClasses);
            }

            //处理学生数据
            // 1 找出被删除的班级
            List<String> removedClasses = oldClassList.stream()
                    .filter(oldClass -> !classList.contains(oldClass))
                    .collect(Collectors.toList());

            // 2 更新这些班级下学生的班级信息
            if (!removedClasses.isEmpty()) {
                studentMapper.updateStudentClassByClassNames(removedClasses, null);
            }
        } catch (Exception e) {
            logger.error("更新用户班级关联失败，用户编号：{}", sysUserNo, e);
            throw new BusinessException("更新用户班级关联失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByClassName(String className) {
        try {
            // 1. 先更新该班级下所有学生的班级信息
            studentMapper.updateStudentClassByClassName(className, null);  // 设置为null或其他默认值

            // 2. 删除班级关联
            sysUserClassMapper.deleteByClassName(className);
        } catch (Exception e) {
            logger.error("删除班级失败，班级名称：{}", className, e);
            throw new BusinessException("删除班级失败");
        }
    }

    @Override
    public boolean hasClassPermission(String sysUserNo, String className) {
        return sysUserClassMapper.countBySysUserNoAndClass(sysUserNo, className) > 0;
    }

    @Override
    public List<SysUserClass> getSysUserClassListByClassName(String className) {
        if (StringUtils.isBlank(className)) {
            logger.error("请输入正确的班级名");
            throw new BusinessException(ResultCode.VALIDATE_FAILED);
        }

        // todo 根据配置文件校验className的合法性
        return sysUserClassMapper.selectByClassName(className);
    }


}