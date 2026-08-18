package com.ledger.model;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

/**
 * 表格数据（每个部门对应一份）
 */
@Data
public class TableData {
    private Long departmentId;
    private List<Column> columns = new ArrayList<>();
    private List<Row> rows = new ArrayList<>();
    private String updatedAt;
}
