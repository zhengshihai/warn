package com.tianhai.warn.mapper;

import com.tianhai.warn.model.LateReturn;
import com.tianhai.warn.query.LateReturnQuery;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Calendar;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:applicationContext-test.xml" })
@Transactional
public class LateReturnMapperTest {

    @Autowired
    private LateReturnMapper lateReturnMapper;

    @Test
    public void testInsertAndSelect() {
        // 创建测试数据
        LateReturn lateReturn = new LateReturn();
        lateReturn.setLateReturnId("LR20240321000123");
        lateReturn.setStudentNo("2420710220XSTEST");
        lateReturn.setLateTime(new Date());
        lateReturn.setReason("测试晚归原因");
        lateReturn.setProcessStatus("PENDING");

        // 执行插入
        int result = lateReturnMapper.insert(lateReturn);
        assertEquals(1, result);

        // 验证插入结果
        LateReturnQuery query = new LateReturnQuery();
        query.setLateReturnId("LR20240321000123");
        List<LateReturn> lateReturns = lateReturnMapper.selectByCondition(query);
        assertNotNull(lateReturns);
        assertFalse(lateReturns.isEmpty());
        assertEquals("LR20240321000123", lateReturns.get(0).getLateReturnId());
        assertEquals("2420710220XSTEST", lateReturns.get(0).getStudentNo());
        assertEquals("测试晚归原因", lateReturns.get(0).getReason());
        assertEquals("PENDING", lateReturns.get(0).getProcessStatus());

        // 测试按学号查询
        query = new LateReturnQuery();
        query.setStudentNo("2420710220XSTEST");
        lateReturns = lateReturnMapper.selectByCondition(query);
        assertNotNull(lateReturns);
        assertFalse(lateReturns.isEmpty());
        assertEquals("2420710220XSTEST", lateReturns.get(0).getStudentNo());

        // 测试按晚归记录ID查询
        query = new LateReturnQuery();
        query.setLateReturnId("LR20240321000123");
        lateReturns = lateReturnMapper.selectByCondition(query);
        assertNotNull(lateReturns);
        assertFalse(lateReturns.isEmpty());
        assertEquals("LR20240321000123", lateReturns.get(0).getLateReturnId());
    }

    @Test
    public void testSelectByCondition() {
        // 测试按学号查询
        LateReturnQuery query = new LateReturnQuery();
        query.setStudentNo("2420710220XS");
        List<LateReturn> lateReturns = lateReturnMapper.selectByCondition(query);
        assertNotNull(lateReturns);
        assertFalse(lateReturns.isEmpty());
        for (LateReturn lateReturn : lateReturns) {
            assertEquals("2420710220XS", lateReturn.getStudentNo());
        }

        // 测试按处理状态查询
        query = new LateReturnQuery();
        query.setProcessStatus("PENDING");
        lateReturns = lateReturnMapper.selectByCondition(query);
        assertNotNull(lateReturns);
        for (LateReturn lateReturn : lateReturns) {
            assertEquals("PENDING", lateReturn.getProcessStatus());
        }

        // 测试按处理结果查询
        query = new LateReturnQuery();
        query.setProcessResult("APPROVED");
        lateReturns = lateReturnMapper.selectByCondition(query);
        assertNotNull(lateReturns);
        for (LateReturn lateReturn : lateReturns) {
            assertEquals("APPROVED", lateReturn.getProcessResult());
        }

        // 测试按时间范围查询
        // 先查询该学生的所有晚归记录，获取实际的时间范围
        query = new LateReturnQuery();
        query.setStudentNo("2420710220XS");
        lateReturns = lateReturnMapper.selectByCondition(query);
        assertNotNull(lateReturns);
        assertFalse(lateReturns.isEmpty());

        // 找到最早和最晚的晚归时间
        Date earliestTime = lateReturns.get(0).getLateTime();
        Date latestTime = lateReturns.get(0).getLateTime();
        for (LateReturn lateReturn : lateReturns) {
            Date lateTime = lateReturn.getLateTime();
            if (lateTime.before(earliestTime)) {
                earliestTime = lateTime;
            }
            if (lateTime.after(latestTime)) {
                latestTime = lateTime;
            }
        }

        // 设置查询时间范围，稍微扩大一点范围以确保能查到记录
        Calendar cal = Calendar.getInstance();
        cal.setTime(earliestTime);
        cal.add(Calendar.DAY_OF_MONTH, -1); // 提前一天
        Date startTime = cal.getTime();

        cal.setTime(latestTime);
        cal.add(Calendar.DAY_OF_MONTH, 1); // 延后一天
        Date endTime = cal.getTime();

        query = new LateReturnQuery();
        query.setStartLateTime(startTime);
        query.setEndLateTime(endTime);
        lateReturns = lateReturnMapper.selectByCondition(query);
        assertNotNull(lateReturns);
        assertFalse(lateReturns.isEmpty());
        for (LateReturn lateReturn : lateReturns) {
            assertTrue("晚归时间应该在指定范围内",
                    !lateReturn.getLateTime().before(startTime) &&
                            !lateReturn.getLateTime().after(endTime));
        }

        // 测试组合条件查询
        query = new LateReturnQuery();
        query.setStudentNo("2420710220XS");
        query.setProcessStatus("PENDING");
        lateReturns = lateReturnMapper.selectByCondition(query);
        assertNotNull(lateReturns);
        for (LateReturn lateReturn : lateReturns) {
            assertEquals("2420710220XS", lateReturn.getStudentNo());
            assertEquals("PENDING", lateReturn.getProcessStatus());
        }
    }

    @Test
    public void testUpdate() {
        // 1. 先查询一条待处理的晚归记录
        LateReturnQuery query = new LateReturnQuery();
        query.setStudentNo("2420710220XS");
        query.setProcessStatus("PENDING");
        List<LateReturn> lateReturns = lateReturnMapper.selectByCondition(query);
        assertNotNull(lateReturns);
        assertFalse(lateReturns.isEmpty());

        // 2. 保存原始数据用于后续恢复
        LateReturn originalLateReturn = lateReturns.get(0);
        String originalReason = originalLateReturn.getReason();
        String originalProcessStatus = originalLateReturn.getProcessStatus();
        String originalProcessResult = originalLateReturn.getProcessResult();
        String originalProcessRemark = originalLateReturn.getProcessRemark();

        // 3. 准备更新数据
        LateReturn lateReturnToUpdate = new LateReturn();
        lateReturnToUpdate.setId(originalLateReturn.getId());
        lateReturnToUpdate.setLateReturnId(originalLateReturn.getLateReturnId());
        lateReturnToUpdate.setReason("更新后的晚归原因");
        lateReturnToUpdate.setProcessStatus("PROCESSING");
        lateReturnToUpdate.setProcessResult("APPROVED");
        lateReturnToUpdate.setProcessRemark("测试更新备注");

        // 4. 执行更新
        int result = lateReturnMapper.update(lateReturnToUpdate);
        assertEquals(1, result);

        // 5. 验证更新结果
        query = new LateReturnQuery();
        query.setLateReturnId(originalLateReturn.getLateReturnId());
        lateReturns = lateReturnMapper.selectByCondition(query);
        assertNotNull(lateReturns);
        assertFalse(lateReturns.isEmpty());

        LateReturn updatedLateReturn = lateReturns.get(0);
        assertEquals("更新后的晚归原因", updatedLateReturn.getReason());
        assertEquals("PROCESSING", updatedLateReturn.getProcessStatus());
        assertEquals("APPROVED", updatedLateReturn.getProcessResult());
        assertEquals("测试更新备注", updatedLateReturn.getProcessRemark());

        // 6. 恢复原始数据
        LateReturn restoreLateReturn = new LateReturn();
        restoreLateReturn.setId(originalLateReturn.getId());
        restoreLateReturn.setLateReturnId(originalLateReturn.getLateReturnId());
        restoreLateReturn.setReason(originalReason);
        restoreLateReturn.setProcessStatus(originalProcessStatus);
        restoreLateReturn.setProcessResult(originalProcessResult);
        restoreLateReturn.setProcessRemark(originalProcessRemark);

        result = lateReturnMapper.update(restoreLateReturn);
        assertEquals(1, result);

        // 7. 验证恢复结果
        lateReturns = lateReturnMapper.selectByCondition(query);
        assertNotNull(lateReturns);
        assertFalse(lateReturns.isEmpty());

        LateReturn restoredLateReturn = lateReturns.get(0);
        assertEquals(originalReason, restoredLateReturn.getReason());
        assertEquals(originalProcessStatus, restoredLateReturn.getProcessStatus());
        assertEquals(originalProcessResult, restoredLateReturn.getProcessResult());
        assertEquals(originalProcessRemark, restoredLateReturn.getProcessRemark());
    }

}