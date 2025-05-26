package com.tianhai.warn.mapper;

import com.tianhai.warn.model.SysUser;
import com.tianhai.warn.query.SysUserQuery;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.querydsl.QuerydslUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.annotation.Rollback;

import java.util.List;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:applicationContext-test.xml" })
@Transactional
public class SysUserMapperTest {

    @Autowired
    private SysUserMapper sysUserMapper;

    @Test
    @Rollback(false)
    public void testInsert() {
        // 创建测试数据
        SysUser sysUser = new SysUser();
        sysUser.setSysUserNo("2420710220FDYTEST");
        sysUser.setPassword("123456");
        sysUser.setName("测试系统用户");
        sysUser.setJobRole("辅导员");
        sysUser.setPhone("13800138000");
        sysUser.setEmail("2420710220FDYTEST@example.com");
        sysUser.setStatus("ENABLE");

        // 执行插入
        int result = sysUserMapper.insert(sysUser);

        // 验证插入结果
        assertTrue("插入应该成功", result > 0);
        assertNotNull("ID应该被自动生成", sysUser.getId());

        // 验证数据是否正确插入
        SysUserQuery query = new SysUserQuery();
        query.setSysUserNo("2420710220FDYTEST");
        SysUser insertedUser = sysUserMapper.selectByCondition(query).get(0);
        assertNotNull("应该能查询到插入的系统用户", insertedUser);
        assertEquals("工号应该匹配", "2420710220FDYTEST", insertedUser.getSysUserNo());
        assertEquals("密码应该匹配", "123456", insertedUser.getPassword());
        assertEquals("姓名应该匹配", "测试系统用户", insertedUser.getName());
        assertEquals("职位角色应该匹配", "辅导员", insertedUser.getJobRole());
        assertEquals("电话应该匹配", "13800138000", insertedUser.getPhone());
        assertEquals("邮箱应该匹配", "2420710220FDYTEST@example.com", insertedUser.getEmail());
        assertEquals("状态应该匹配", "ENABLE", insertedUser.getStatus());
    }

    @Test(expected = Exception.class)
    public void testInsertDuplicateSysUserNo() {
        // 第一次插入
        SysUser user1 = new SysUser();
        user1.setSysUserNo("2420710220SYS");
        user1.setPassword("123456");
        user1.setName("测试系统用户1");
        user1.setJobRole("辅导员");
        user1.setPhone("13800138001");
        user1.setEmail("test1@example.com");
        user1.setStatus("ENABLE");
        sysUserMapper.insert(user1);

        // 尝试插入相同工号的系统用户
        SysUser user2 = new SysUser();
        user2.setSysUserNo("2420710220SYS");
        user2.setPassword("123456");
        user2.setName("测试系统用户2");
        user2.setJobRole("班主任");
        user2.setPhone("13800138002");
        user2.setEmail("test2@example.com");
        user2.setStatus("ENABLE");
        sysUserMapper.insert(user2);
    }

    @Test(expected = Exception.class)
    public void testInsertDuplicateEmail() {
        // 第一次插入
        SysUser user1 = new SysUser();
        user1.setSysUserNo("2420710220SYS1");
        user1.setPassword("123456");
        user1.setName("测试系统用户1");
        user1.setJobRole("辅导员");
        user1.setPhone("13800138001");
        user1.setEmail("test@example.com");
        user1.setStatus("ENABLE");
        sysUserMapper.insert(user1);

        // 尝试插入相同邮箱的系统用户
        SysUser user2 = new SysUser();
        user2.setSysUserNo("2420710220SYS2");
        user2.setPassword("123456");
        user2.setName("测试系统用户2");
        user2.setJobRole("班主任");
        user2.setPhone("13800138002");
        user2.setEmail("test@example.com");
        user2.setStatus("ENABLE");
        sysUserMapper.insert(user2);
    }

    @Test
    public void testUpdate() {
        // 1. 先查询出要更新的辅导员
        SysUserQuery condition = new SysUserQuery();
        condition.setSysUserNo("2420710220FDYTEST");
        List<SysUser> users = sysUserMapper.selectByCondition(condition);
        assertFalse("测试辅导员数据不存在", users.isEmpty());
        SysUser sysUser = users.get(0);

        // 2. 保存原始数据用于后续验证
        String originalName = sysUser.getName();
        String originalPhone = sysUser.getPhone();
        String originalEmail = sysUser.getEmail();

        // 3. 准备更新数据
        sysUser.setName("测试更新姓名");
        sysUser.setPhone("13900000000");
        sysUser.setEmail("test.update@example.com");

        // 4. 执行更新
        int result = sysUserMapper.update(sysUser);
        assertEquals("更新应该返回1", 1, result);

        // 5. 重新查询验证更新结果
        users = sysUserMapper.selectByCondition(condition);
        assertFalse("更新后查询结果不应为空", users.isEmpty());
        SysUser updatedSysUser = users.get(0);
        assertEquals("姓名应该被更新", "测试更新姓名", updatedSysUser.getName());
        assertEquals("电话应该被更新", "13900000000", updatedSysUser.getPhone());
        assertEquals("邮箱应该被更新", "test.update@example.com", updatedSysUser.getEmail());

        // 6. 恢复原始数据
        sysUser.setName(originalName);
        sysUser.setPhone(originalPhone);
        sysUser.setEmail(originalEmail);
        sysUserMapper.update(sysUser);
    }
}