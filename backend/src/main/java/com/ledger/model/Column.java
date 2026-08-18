package com.ledger.model;

import lombok.Data;

/**
 * 表格列
 */
@Data
public class Column {
    private String id;
    private String name;
    private Integer width;
}
