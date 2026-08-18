package com.ledger.model;

import lombok.Data;

/**
 * 表格格子
 */
@Data
public class Cell {
    private String value;
    private String editor;
    private String updatedAt;
}
