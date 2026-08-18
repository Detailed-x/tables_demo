package com.ledger.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.ledger.model.*;
import com.ledger.util.IdGenerator;
import com.ledger.util.JsonFileUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 表格服务：每个部门对应一份在线表格
 * 支持获取表格、更新格子、增删行列
 */
@Service
public class TableService {

    private static final String FILE = "tables.json";

    @Autowired
    private LockService lockService;

    /**
     * 获取所有部门的表格数据
     */
    private Map<Long, TableData> getAllTables() {
        return JsonFileUtil.read(FILE,
                new TypeReference<Map<Long, TableData>>() {}, new HashMap<>());
    }

    /**
     * 获取指定部门的表格，如果不存在则创建默认表格
     */
    public TableData getTableByDepartment(Long departmentId) {
        Map<Long, TableData> tables = getAllTables();
        TableData table = tables.get(departmentId);
        if (table == null) {
            table = createDefaultTable(departmentId);
            tables.put(departmentId, table);
            JsonFileUtil.write(FILE, tables);
        }
        return table;
    }

    /**
     * 创建默认表格（3列5行的空台账模板）
     */
    private TableData createDefaultTable(Long departmentId) {
        TableData table = new TableData();
        table.setDepartmentId(departmentId);
        table.setUpdatedAt(now());

        // 默认3列
        List<Column> columns = new ArrayList<>();
        String[] colNames = {"序号", "事项", "备注"};
        for (int i = 0; i < 3; i++) {
            Column col = new Column();
            col.setId(IdGenerator.nextColId());
            col.setName(colNames[i]);
            col.setWidth(150);
            columns.add(col);
        }
        table.setColumns(columns);

        // 默认5行
        List<Row> rows = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            Row row = new Row();
            row.setId(IdGenerator.nextRowId());
            Map<String, Cell> cells = new LinkedHashMap<>();
            for (Column col : columns) {
                Cell cell = new Cell();
                cell.setValue("");
                cells.put(col.getId(), cell);
            }
            // 第一列自动填序号
            Cell firstCell = cells.get(columns.get(0).getId());
            if (firstCell != null) {
                firstCell.setValue(String.valueOf(i + 1));
            }
            row.setCells(cells);
            rows.add(row);
        }
        table.setRows(rows);
        return table;
    }

    /**
     * 更新格子内容（需要先持有锁）
     */
    public Map<String, Object> updateCell(Long departmentId, String rowId, String colId,
                                           String value, Long userId, String username) {
        Map<String, Object> result = new HashMap<>();

        // 检查是否持有锁
        LockInfo lock = lockService.getLockStatus(departmentId, rowId, colId);
        if (lock == null || !lock.getUserId().equals(userId)) {
            result.put("success", false);
            result.put("message", "您未持有该格子的编辑锁，请先点击获取编辑权");
            return result;
        }

        Map<Long, TableData> tables = getAllTables();
        TableData table = tables.get(departmentId);
        if (table == null) {
            result.put("success", false);
            result.put("message", "表格不存在");
            return result;
        }

        for (Row row : table.getRows()) {
            if (row.getId().equals(rowId)) {
                Cell cell = row.getCells().get(colId);
                if (cell == null) {
                    cell = new Cell();
                    row.getCells().put(colId, cell);
                }
                cell.setValue(value);
                cell.setEditor(username);
                cell.setUpdatedAt(now());
                break;
            }
        }
        table.setUpdatedAt(now());
        tables.put(departmentId, table);
        JsonFileUtil.write(FILE, tables);

        result.put("success", true);
        result.put("message", "保存成功");
        return result;
    }

    /**
     * 添加行
     */
    public TableData addRow(Long departmentId) {
        Map<Long, TableData> tables = getAllTables();
        TableData table = tables.get(departmentId);
        if (table == null) {
            table = createDefaultTable(departmentId);
        }

        Row row = new Row();
        row.setId(IdGenerator.nextRowId());
        Map<String, Cell> cells = new LinkedHashMap<>();
        for (Column col : table.getColumns()) {
            Cell cell = new Cell();
            cell.setValue("");
            cells.put(col.getId(), cell);
        }
        // 序号列自动递增
        if (!table.getColumns().isEmpty()) {
            Cell firstCell = cells.get(table.getColumns().get(0).getId());
            if (firstCell != null) {
                firstCell.setValue(String.valueOf(table.getRows().size() + 1));
            }
        }
        row.setCells(cells);
        table.getRows().add(row);
        table.setUpdatedAt(now());
        tables.put(departmentId, table);
        JsonFileUtil.write(FILE, tables);
        return table;
    }

    /**
     * 删除行
     */
    public TableData deleteRow(Long departmentId, String rowId) {
        Map<Long, TableData> tables = getAllTables();
        TableData table = tables.get(departmentId);
        if (table != null) {
            table.getRows().removeIf(r -> r.getId().equals(rowId));
            // 重新编号序号列
            if (!table.getColumns().isEmpty()) {
                String firstColId = table.getColumns().get(0).getId();
                for (int i = 0; i < table.getRows().size(); i++) {
                    Cell cell = table.getRows().get(i).getCells().get(firstColId);
                    if (cell != null) {
                        cell.setValue(String.valueOf(i + 1));
                    }
                }
            }
            table.setUpdatedAt(now());
            tables.put(departmentId, table);
            JsonFileUtil.write(FILE, tables);
        }
        return table;
    }

    /**
     * 添加列
     */
    public TableData addColumn(Long departmentId, String name) {
        Map<Long, TableData> tables = getAllTables();
        TableData table = tables.get(departmentId);
        if (table == null) {
            table = createDefaultTable(departmentId);
        }

        Column col = new Column();
        col.setId(IdGenerator.nextColId());
        col.setName(name != null && !name.isEmpty() ? name : "新列");
        col.setWidth(150);
        table.getColumns().add(col);

        // 给已有行添加该列的空格子
        for (Row row : table.getRows()) {
            Cell cell = new Cell();
            cell.setValue("");
            row.getCells().put(col.getId(), cell);
        }

        table.setUpdatedAt(now());
        tables.put(departmentId, table);
        JsonFileUtil.write(FILE, tables);
        return table;
    }

    /**
     * 删除列
     */
    public TableData deleteColumn(Long departmentId, String colId) {
        Map<Long, TableData> tables = getAllTables();
        TableData table = tables.get(departmentId);
        if (table != null) {
            table.getColumns().removeIf(c -> c.getId().equals(colId));
            for (Row row : table.getRows()) {
                row.getCells().remove(colId);
            }
            table.setUpdatedAt(now());
            tables.put(departmentId, table);
            JsonFileUtil.write(FILE, tables);
        }
        return table;
    }

    /**
     * 重命名列
     */
    public TableData renameColumn(Long departmentId, String colId, String name) {
        Map<Long, TableData> tables = getAllTables();
        TableData table = tables.get(departmentId);
        if (table != null) {
            for (Column col : table.getColumns()) {
                if (col.getId().equals(colId)) {
                    col.setName(name);
                    break;
                }
            }
            table.setUpdatedAt(now());
            tables.put(departmentId, table);
            JsonFileUtil.write(FILE, tables);
        }
        return table;
    }

    private String now() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}
