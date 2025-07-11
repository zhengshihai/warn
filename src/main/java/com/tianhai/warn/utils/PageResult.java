package com.tianhai.warn.utils;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;


public class PageResult<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<T> data; // 当前页数据

    private int total; // 总记录数

    private int pageNum; // 当前页码

    private int pageSize; // 每页条数

    public PageResult() {
        this.data = Collections.emptyList();
        this.total = 0;
        this.pageNum = 1;
        this.pageSize = 10;
    };

    public PageResult(List<T> data, int total, int pageNum, int pageSize) {
        this.data = (data == null) ? Collections.emptyList() : data;
        this.total = total;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
    }

    //总页数
    public int getTotalPages() {
        return pageSize == 0 ? 0
                : (int) Math.ceil((double) total / pageSize);
    }

    //是否还有上一页
    public boolean isHasPrev() {
        return pageNum > 1;
    }

    //是否还有下一页
    public boolean isHasNext() {
        return pageNum < getTotalPages();
    }

    // getter/setter
    public List<T> getData() {
        return data;
    }

    public void setData(List<T> data) {
        this.data = (data == null) ? Collections.emptyList() : data;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getPageNum() {
        return pageNum;
    }

    public void setPageNum(int pageNum) {
        this.pageNum = pageNum;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }



}
