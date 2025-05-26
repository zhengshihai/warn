package com.tianhai.warn.mapper;

import com.tianhai.warn.model.SuperAdmin;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.annotation.Rollback;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:applicationContext-test.xml" })
@Transactional
public class SuperAdminMapperTest {

    @Autowired
    private SuperAdminMapper superAdminMapper;

    @Test
    @Rollback(false)
    public void testInsert() {
        // 创建测试数据
        SuperAdmin admin = new SuperAdmin();
        admin.setName("测试超级管理员");
        admin.setPassword("123456");
        admin.setEmail("2020710220SA@example.com");
        admin.setEnabled(1);

        // 执行插入
        int result = superAdminMapper.insert(admin);

        // 验证插入结果
        assertTrue("插入应该成功", result > 0);
        assertNotNull("ID应该被自动生成", admin.getId());

        // 验证数据是否正确插入
        SuperAdmin query = new SuperAdmin();
        query.setEmail("2020710220SA@example.com");
        SuperAdmin insertedAdmin = superAdminMapper.selectByCondition(query).get(0);
        assertNotNull("应该能查询到插入的超级管理员", insertedAdmin);
        assertEquals("名称应该匹配", "测试超级管理员", insertedAdmin.getName());
        assertEquals("密码应该匹配", "123456", insertedAdmin.getPassword());
        assertEquals("邮箱应该匹配", "2020710220SA@example.com", insertedAdmin.getEmail());
        assertEquals("启用状态应该为1", Integer.valueOf(1), insertedAdmin.getEnabled());
        assertNotNull("创建时间应该被自动设置", insertedAdmin.getCreateTime());
        assertNotNull("更新时间应该被自动设置", insertedAdmin.getUpdateTime());
        assertEquals("版本号应该为0", Integer.valueOf(0), insertedAdmin.getVersion());
    }

    @Test(expected = Exception.class)
    public void testInsertDuplicateEmail() {
        // 第一次插入
        SuperAdmin admin1 = new SuperAdmin();
        admin1.setName("测试超级管理员1");
        admin1.setPassword("123456");
        admin1.setEmail("test@example.com");
        admin1.setEnabled(1);
        superAdminMapper.insert(admin1);

        // 尝试插入相同邮箱的超级管理员
        SuperAdmin admin2 = new SuperAdmin();
        admin2.setName("测试超级管理员2");
        admin2.setPassword("123456");
        admin2.setEmail("test@example.com");
        admin2.setEnabled(1);
        superAdminMapper.insert(admin2);
    }

    @Test
    public void testUpdate() {
        // 1. 先查询出要更新的超级管理员
        SuperAdmin superAdmin = superAdminMapper.selectById(1);
        assertNotNull("测试超级管理员数据不存在", superAdmin);

        // 2. 保存原始数据用于后续验证
        String originalName = superAdmin.getName();
        String originalPassword = superAdmin.getPassword();
        String originalEmail = superAdmin.getEmail();
        Integer originalEnabled = superAdmin.getEnabled();
        Integer originalVersion = superAdmin.getVersion();

        // 3. 准备更新数据
        superAdmin.setName("测试更新姓名");
        superAdmin.setPassword("newPassword123");
        superAdmin.setEmail("test.update@example.com");
        superAdmin.setEnabled(1); // 1表示启用

        // 4. 执行更新
        int result = superAdminMapper.update(superAdmin);
        assertEquals("更新应该返回1", 1, result);

        // 5. 重新查询验证更新结果
        SuperAdmin updatedSuperAdmin = superAdminMapper.selectById(1);
        assertNotNull("更新后查询结果不应为空", updatedSuperAdmin);
        assertEquals("姓名应该被更新", "测试更新姓名", updatedSuperAdmin.getName());
        assertEquals("密码应该被更新", "newPassword123", updatedSuperAdmin.getPassword());
        assertEquals("邮箱应该被更新", "test.update@example.com", updatedSuperAdmin.getEmail());
        assertEquals("启用状态应该被更新", Integer.valueOf(1), updatedSuperAdmin.getEnabled());
        assertEquals("版本号应该增加1", Integer.valueOf(originalVersion + 1), updatedSuperAdmin.getVersion());

        // 6. 恢复原始数据
        superAdmin.setName(originalName);
        superAdmin.setPassword(originalPassword);
        superAdmin.setEmail(originalEmail);
        superAdmin.setEnabled(originalEnabled);
        superAdmin.setVersion(originalVersion);
        superAdminMapper.update(superAdmin);
    }
}