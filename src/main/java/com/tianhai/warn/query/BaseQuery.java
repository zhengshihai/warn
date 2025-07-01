package com.tianhai.warn.query;

import lombok.*;
import lombok.experimental.SuperBuilder;

import java.io.Serial;
import java.io.Serializable;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class BaseQuery implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    // 当前页码，默认1
    private Integer pageNum = 1;
    // 每页条数，默认10
    private Integer pageSize = 10;

    private Boolean allowFullUpdate; // 是否允许全量更新

    private String sortField; // 排序字段

    private String sortOrder; // 排序顺序 ASC 或 DESC
}