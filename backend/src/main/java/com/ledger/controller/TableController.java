package com.ledger.controller;

import com.ledger.model.*;
import com.ledger.service.LockService;
import com.ledger.service.TableService;
import com.ledger.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * 表格控制器：表格获取、格子编辑、行列管理、格子锁
 */
@RestController
@RequestMapping("/table")
public class TableController {

    @Autowired
    private TableService tableService;

    @Autowired
    private LockService lockService;

    @Autowired
    private UserService userService;

    /**
     * 获取当前用户部门的表格
     */
    @GetMapping
    public Result<TableData> getMyTable(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        User user = userService.getUserById(userId);
        if (user == null || user.getDepartmentId() == null) {
            return Result.error(400, "您尚未分配部门，请联系管理员分配");
        }
        return Result.success(tableService.getTableByDepartment(user.getDepartmentId()));
    }

    /**
     * 获取指定部门的表格（管理员可查看任意部门）
     */
    @GetMapping("/department/{departmentId}")
    public Result<TableData> getTableByDepartment(
            @PathVariable Long departmentId,
            HttpServletRequest request) {
        String role = (String) request.getAttribute("role");
        Long userId = (Long) request.getAttribute("userId");
        User user = userService.getUserById(userId);

        // 普通用户只能看自己部门的
        if (!"ADMIN".equals(role)) {
            if (user == null || !departmentId.equals(user.getDepartmentId())) {
                return Result.error(403, "无权限查看其他部门表格");
            }
        }
        return Result.success(tableService.getTableByDepartment(departmentId));
    }

    /**
     * 更新格子内容
     */
    @PutMapping("/cell")
    public Result<Map<String, Object>> updateCell(
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String username = (String) request.getAttribute("username");
        User user = userService.getUserById(userId);
        if (user == null || user.getDepartmentId() == null) {
            return Result.error(400, "您尚未分配部门");
        }

        String rowId = body.get("rowId");
        String colId = body.get("colId");
        String value = body.get("value");

        Map<String, Object> result = tableService.updateCell(
                user.getDepartmentId(), rowId, colId, value, userId, username);
        if (Boolean.TRUE.equals(result.get("success"))) {
            return Result.success(result);
        }
        return Result.error((String) result.get("message"));
    }

    /**
     * 尝试获取格子编辑锁
     * 返回 null 表示获取成功，否则返回当前持锁人信息
     */
    @PostMapping("/cell/lock")
    public Result<Map<String, Object>> tryLock(
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String username = (String) request.getAttribute("username");
        User user = userService.getUserById(userId);
        if (user == null || user.getDepartmentId() == null) {
            return Result.error(400, "您尚未分配部门");
        }

        String rowId = body.get("rowId");
        String colId = body.get("colId");

        LockInfo lockInfo = lockService.tryLock(
                user.getDepartmentId(), rowId, colId, userId, username);

        Map<String, Object> result = new HashMap<>();
        if (lockInfo == null) {
            result.put("locked", true);
            result.put("message", "已获得编辑权");
        } else {
            result.put("locked", false);
            result.put("message", "该格子正在被 \"" + lockInfo.getUsername() + "\" 编辑，请稍后再试");
            result.put("lockUser", lockInfo.getUsername());
            result.put("lockTime", lockInfo.getLockedAt());
        }
        return Result.success(result);
    }

    /**
     * 释放格子编辑锁
     */
    @PostMapping("/cell/unlock")
    public Result<Map<String, Object>> unlock(
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        User user = userService.getUserById(userId);
        if (user == null || user.getDepartmentId() == null) {
            return Result.error(400, "您尚未分配部门");
        }

        String rowId = body.get("rowId");
        String colId = body.get("colId");
        lockService.unlock(user.getDepartmentId(), rowId, colId, userId);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "已释放编辑权");
        return Result.success(result);
    }

    /**
     * 续租锁（编辑中定时调用，防止超时释放）
     */
    @PostMapping("/cell/renew")
    public Result<Map<String, Object>> renewLock(
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        User user = userService.getUserById(userId);
        if (user == null || user.getDepartmentId() == null) {
            return Result.error(400, "您尚未分配部门");
        }

        String rowId = body.get("rowId");
        String colId = body.get("colId");
        boolean success = lockService.renewLock(user.getDepartmentId(), rowId, colId, userId);

        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        if (!success) {
            result.put("message", "锁已失效，可能已被超时释放或被他人获取");
        }
        return Result.success(result);
    }

    /**
     * 查询当前部门所有被锁定的格子（用于前端高亮显示）
     */
    @GetMapping("/locks")
    public Result<Map<String, LockInfo>> getLocks(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        User user = userService.getUserById(userId);
        if (user == null || user.getDepartmentId() == null) {
            return Result.success(new HashMap<>());
        }
        return Result.success(lockService.getLocksByDepartment(user.getDepartmentId()));
    }

    // ==================== 行列管理 ====================

    /**
     * 添加行
     */
    @PostMapping("/row")
    public Result<TableData> addRow(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        User user = userService.getUserById(userId);
        if (user == null || user.getDepartmentId() == null) {
            return Result.error(400, "您尚未分配部门");
        }
        return Result.success(tableService.addRow(user.getDepartmentId()));
    }

    /**
     * 删除行
     */
    @DeleteMapping("/row/{rowId}")
    public Result<TableData> deleteRow(@PathVariable String rowId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        User user = userService.getUserById(userId);
        if (user == null || user.getDepartmentId() == null) {
            return Result.error(400, "您尚未分配部门");
        }
        return Result.success(tableService.deleteRow(user.getDepartmentId(), rowId));
    }

    /**
     * 添加列
     */
    @PostMapping("/column")
    public Result<TableData> addColumn(
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        User user = userService.getUserById(userId);
        if (user == null || user.getDepartmentId() == null) {
            return Result.error(400, "您尚未分配部门");
        }
        String name = body.get("name");
        return Result.success(tableService.addColumn(user.getDepartmentId(), name));
    }

    /**
     * 删除列
     */
    @DeleteMapping("/column/{colId}")
    public Result<TableData> deleteColumn(@PathVariable String colId, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        User user = userService.getUserById(userId);
        if (user == null || user.getDepartmentId() == null) {
            return Result.error(400, "您尚未分配部门");
        }
        return Result.success(tableService.deleteColumn(user.getDepartmentId(), colId));
    }

    /**
     * 重命名列
     */
    @PutMapping("/column/{colId}")
    public Result<TableData> renameColumn(
            @PathVariable String colId,
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        User user = userService.getUserById(userId);
        if (user == null || user.getDepartmentId() == null) {
            return Result.error(400, "您尚未分配部门");
        }
        String name = body.get("name");
        return Result.success(tableService.renameColumn(user.getDepartmentId(), colId, name));
    }
}
