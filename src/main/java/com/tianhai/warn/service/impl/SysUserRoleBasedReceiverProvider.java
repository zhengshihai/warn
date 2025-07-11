package com.tianhai.warn.service.impl;

import com.tianhai.warn.annotation.ReceiverRole;
import com.tianhai.warn.constants.Constants;
import com.tianhai.warn.model.SysUser;
import com.tianhai.warn.service.ReceiverIdProvider;
import com.tianhai.warn.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("sysUserReceiverProvider")
@ReceiverRole(Constants.SYSTEM_USER)
public class SysUserRoleBasedReceiverProvider implements ReceiverIdProvider {

    @Autowired
    private SysUserService sysUserService;

    @Override
    public List<String> getReceiverIds() {
        return sysUserService.selectAll().stream()
                .map(SysUser::getSysUserNo)
                .toList();
    }
}
