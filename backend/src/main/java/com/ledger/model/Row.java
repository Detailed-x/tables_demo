package com.ledger.model;

import lombok.Data;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 表格行
 */
@Data
public class Row {
    private String id;
    /** key=列ID, value=格子内容 */
    private Map<String, Cell> cells = new LinkedHashMap<>();
}
