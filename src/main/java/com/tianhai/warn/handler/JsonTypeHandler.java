package com.tianhai.warn.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * Json <-> List<String> 类型转换处理类
 *自定义 MyBatis TypeHandler，用于将 MySQL 中的 JSON 字段和 Java 的 List<String> 类型进行互相转换。

 * 适用于字段类型为 JSON，Java 实体类字段类型为 List<String> 的场景。
 */
public class JsonTypeHandler extends BaseTypeHandler<List<String>> {

    // 使用JackSon 的 ObjectMapper 进行JSON 序列换和反序列化
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 在将参数设置到 PreparedStatement（如插入或更新语句）时，
     * 将 List<String> 转为 JSON 字符串。
     *
     * @param ps        PreparedStatement 对象
     * @param i         参数索引
     * @param parameter Java 中传入的 List<String> 参数
     * @param jdbcType  JDBC 类型（通常为 JdbcType.VARCHAR）
     * @throws SQLException 如果序列化出错或数据库操作出错
     */
    @Override
    public void setNonNullParameter(PreparedStatement ps,
                                    int i,
                                    List<String> parameter,
                                    JdbcType jdbcType) throws SQLException {
        try {
            ps.setString(i, objectMapper.writeValueAsString(parameter));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 从 ResultSet 中获取列名对应的字段值（用于查询时），
     * 并将其从 JSON 字符串反序列化为 List<String>
     */
    @Override
    public List<String> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        String json = rs.getString(columnName);
        return parseJosn(json);
    }

    /**
     * 与上面类似，不过是通过列索引获取（适用于少数场景，如无列名或性能优化）
     */
    @Override
    public List<String> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        String json = rs.getString(columnIndex);
        return parseJosn(json);
    }

    @Override
    public List<String> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        String json = cs.getString(columnIndex);
        return parseJosn(json);
    }

    /**
     * 将 JSON 字符串解析为 Java 的 List<String>
     *
     * @param json 从数据库中读出的 JSON 字符串
     * @return 反序列化后的 List<String>，如果为空或格式错误，返回 null 或抛出异常
     */
    private List<String> parseJosn(String json) {
        if (json == null || json.trim().isEmpty()) return null;

        try {
            // 使用 Jackson 反序列化为 List<String>，通过 TypeReference 保留泛型类型
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            // 异常时抛出运行时异常，避免吞掉错误信息
            throw new RuntimeException("JSON反序列化失败: " + json, e);
        }
    }

}
