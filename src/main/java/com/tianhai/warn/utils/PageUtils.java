package com.tianhai.warn.utils;

import com.github.pagehelper.PageInfo;
import com.tianhai.warn.constants.Constants;
import com.tianhai.warn.query.BaseQuery;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * 分页结果构建工具类
 */
public class PageUtils {

    /**
     * 构建分页结果（用于PageHelper分页后的结果）
     * 
     * @param list PageHelper返回的列表
     * @return 分页结果
     * @param <T> 被分页的实体类
     */
    public static <T> PageResult<T> buildPageResult(List<T> list) {
        if (list == null || list.isEmpty()) {
            return emptyPageResult();
        }

        PageInfo<T> pageInfo = new PageInfo<>(list);

        PageResult<T> result = new PageResult<>();
        result.setData(pageInfo.getList());
        result.setTotal((int) pageInfo.getTotal());
        result.setPageNum(pageInfo.getPageNum());
        result.setPageSize(pageInfo.getPageSize());

        return result;
    }

    /**
     * 构建分页结果（手动分页）
     * 
     * @param list     完整的数据列表
     * @param pageNum  当前页码
     * @param pageSize 每页大小
     * @return 分页结果
     * @param <T> 被分页的实体类
     */
    public static <T> PageResult<T> buildPageResult(List<T> list, int pageNum, int pageSize) {
        if (list == null || list.isEmpty()) {
            return emptyPageResult();
        }

        int total = list.size();
        int start = (pageNum - 1) * pageSize;
        int end = start + pageSize;

        if (start >= total) {
            return emptyPageResult();
        }
        end = Math.min(end, total);

        List<T> pageData = list.subList(start, end);
        return new PageResult<>(pageData, total, pageNum, pageSize);
    }

    // 空页返回默认结构
    public static <T> PageResult<T> emptyPageResult() {
        PageResult<T> result = new PageResult<>();
        result.setData(Collections.emptyList());
        result.setTotal(0);
        result.setPageNum(Constants.DEFAULT_PAGE_NUM);
        result.setPageSize(Constants.DEFAULT_PAGE_SIZE);

        return result;
    }

    // 校正分页参数（防止非法页码或页大小）
    public static void normalizePageNums(BaseQuery query) {
        if (query.getPageNum() == null || query.getPageNum() < 1) {
            query.setPageNum(Constants.DEFAULT_PAGE_NUM);
        }
        if (query.getPageSize() == null || query.getPageSize() <= 0) {
            query.setPageSize(Constants.DEFAULT_PAGE_SIZE);
        }
        if (query.getPageSize() != null &&
                query.getPageSize() > Constants.DEFAULT_PAGE_SIZE_MAX) {
            query.setPageSize(Constants.DEFAULT_PAGE_SIZE);
        }
    }

    /**
     * 过滤排序字段（防止 SQL 注入）
     * 
     * @param orderField    用户传入字段
     * @param allowedFields 允许的字段名（数据库字段名，非 Java 字段名）
     * @return 合法的字段或 null
     */
    public static String sanitizeOrderField(String orderField, Set<String> allowedFields) {
        if (orderField == null || orderField.trim().isEmpty()) {
            return null;
        }
        String cleanField = orderField.trim().toLowerCase();

        return allowedFields.contains(cleanField) ? cleanField : null;
    }
}
