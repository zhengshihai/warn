package com.tianhai.warn.service.impl;

import com.tianhai.warn.annotation.ReceiverRole;
import com.tianhai.warn.constants.Constants;
import com.tianhai.warn.model.DormitoryManager;
import com.tianhai.warn.service.DormitoryManagerService;
import com.tianhai.warn.service.ReceiverIdProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("dormitoryManagerReceiverProvider")
@ReceiverRole(Constants.DORMITORY_MANAGER)
public class DormitoryManagerRoleBasedReceiverProvider implements ReceiverIdProvider {

    @Autowired
    private DormitoryManagerService dormitoryManagerService;

    @Override
    public List<String> getReceiverIds() {
        return dormitoryManagerService.selectAll().stream()
                .map(DormitoryManager::getManagerId)
                .toList();
    }

}
