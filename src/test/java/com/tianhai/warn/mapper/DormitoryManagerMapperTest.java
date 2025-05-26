package com.tianhai.warn.mapper;

import com.tianhai.warn.model.DormitoryManager;
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
public class DormitoryManagerMapperTest {

    @Autowired
    private DormitoryManagerMapper dormitoryManagerMapper;

    @Test
    @Rollback(false)
    public void testInsert() {
        // 创建测试数据
        DormitoryManager manager = new DormitoryManager();
        manager.setManagerId("2420710220SGTEST");
        manager.setName("测试宿管");
        manager.setBuilding("测试楼");
        manager.setPhone("13800138000");
        manager.setStatus("ON_DUTY");
        manager.setPassword("123456");
        manager.setEmail("test@example.com");

        // 执行插入
        int result = dormitoryManagerMapper.insert(manager);

        // 验证插入结果
        assertTrue("插入应该成功", result > 0);
        assertNotNull("ID应该被自动生成", manager.getId());

        // 验证数据是否正确插入
        DormitoryManager insertedManager = dormitoryManagerMapper.selectByManagerId("2420710220SGTEST");
        assertNotNull("应该能查询到插入的宿管", insertedManager);
        assertEquals("工号应该匹配", "2420710220SGTEST", insertedManager.getManagerId());
        assertEquals("姓名应该匹配", "测试宿管", insertedManager.getName());
        assertEquals("负责宿舍楼应该匹配", "测试楼", insertedManager.getBuilding());
        assertEquals("电话应该匹配", "13800138000", insertedManager.getPhone());
        assertEquals("状态应该匹配", "ON_DUTY", insertedManager.getStatus());
        assertEquals("密码应该匹配", "123456", insertedManager.getPassword());
        assertEquals("邮箱应该匹配", "test@example.com", insertedManager.getEmail());
    }

    @Test(expected = Exception.class)
    public void testInsertDuplicateManagerId() {
        // 第一次插入
        DormitoryManager manager1 = new DormitoryManager();
        manager1.setManagerId("2420710220SGTEST");
        manager1.setName("测试宿管1");
        manager1.setBuilding("测试楼1");
        manager1.setPhone("13800138001");
        manager1.setStatus("ON_DUTY");
        manager1.setPassword("123456");
        manager1.setEmail("test1@example.com");
        dormitoryManagerMapper.insert(manager1);

        // 尝试插入相同工号的宿管
        DormitoryManager manager2 = new DormitoryManager();
        manager2.setManagerId("2420710220SGTEST");
        manager2.setName("测试宿管2");
        manager2.setBuilding("测试楼2");
        manager2.setPhone("13800138002");
        manager2.setStatus("ON_DUTY");
        manager2.setPassword("123456");
        manager2.setEmail("test2@example.com");
        dormitoryManagerMapper.insert(manager2);
    }

    @Test(expected = Exception.class)
    public void testInsertDuplicateEmail() {
        // 第一次插入
        DormitoryManager manager1 = new DormitoryManager();
        manager1.setManagerId("2420710220SGTEST1");
        manager1.setName("测试宿管1");
        manager1.setBuilding("测试楼1");
        manager1.setPhone("13800138001");
        manager1.setStatus("ON_DUTY");
        manager1.setPassword("123456");
        manager1.setEmail("test@example.com");
        dormitoryManagerMapper.insert(manager1);

        // 尝试插入相同邮箱的宿管
        DormitoryManager manager2 = new DormitoryManager();
        manager2.setManagerId("2420710220SGTEST2");
        manager2.setName("测试宿管2");
        manager2.setBuilding("测试楼2");
        manager2.setPhone("13800138002");
        manager2.setStatus("ON_DUTY");
        manager2.setPassword("123456");
        manager2.setEmail("test@example.com");
        dormitoryManagerMapper.insert(manager2);
    }

    @Test
    public void testUpdate() {
        // 1. 先查询出要更新的宿管
        DormitoryManager manager = dormitoryManagerMapper.selectByManagerId("2420710220SGTEST");
        assertNotNull("测试宿管数据不存在", manager);

        // 2. 保存原始数据用于后续验证
        String originalName = manager.getName();
        String originalPhone = manager.getPhone();
        String originalEmail = manager.getEmail();

        // 3. 准备更新数据
        manager.setName("测试更新姓名");
        manager.setPhone("13900000000");
        manager.setEmail("test.update@example.com");

        // 4. 执行更新
        int result = dormitoryManagerMapper.update(manager);
        assertEquals("更新应该返回1", 1, result);

        // 5. 重新查询验证更新结果
        DormitoryManager updatedManager = dormitoryManagerMapper.selectByManagerId("2420710220SGTEST");
        assertNotNull("更新后查询结果不应为空", updatedManager);
        assertEquals("姓名应该被更新", "测试更新姓名", updatedManager.getName());
        assertEquals("电话应该被更新", "13900000000", updatedManager.getPhone());
        assertEquals("邮箱应该被更新", "test.update@example.com", updatedManager.getEmail());

        // 6. 恢复原始数据
        manager.setName(originalName);
        manager.setPhone(originalPhone);
        manager.setEmail(originalEmail);
        dormitoryManagerMapper.update(manager);
    }
}