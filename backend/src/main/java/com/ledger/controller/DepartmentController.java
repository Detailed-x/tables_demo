package com.ledger.controller;

import com.ledger.model.Department;
import com.ledger.model.Result;
import com.ledger.service.DepartmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * 部门控制器：增删改查
 */
@RestController
@RequestMapping("/departments")
public class DepartmentController {

    @Autowired
    private DepartmentService departmentService;

    /**
     * 获取所有部门
     */
    @GetMapping
    public Result<List<Department>> getAllDepartments() {
        return Result.success(departmentService.getAllDepartments());
    }

    /**
     * 创建部门（管理员）
     */
    @PostMapping
    public Result<Map<String, Object>> createDepartment(
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {
        String role = (String) request.getAttribute("role");
        if (!"ADMIN".equals(role)) {
            return Result.error(403, "无权限操作");
        }
        String name = body.get("name");
        Map<String, Object> result = departmentService.createDepartment(name);
        if (Boolean.TRUE.equals(result.get("success"))) {
            return Result.success(result);
        }
        return Result.error((String) result.get("message"));
    }

    /**
     * 更新部门（管理员）
     */
    @PutMapping("/{id}")
    public Result<Map<String, Object>> updateDepartment(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {
        String role = (String) request.getAttribute("role");
        if (!"ADMIN".equals(role)) {
            return Result.error(403, "无权限操作");
        }
        String name = body.get("name");
        Map<String, Object> result = departmentService.updateDepartment(id, name);
        if (Boolean.TRUE.equals(result.get("success"))) {
            return Result.success(result);
        }
        return Result.error((String) result.get("message"));
    }

    /**
     * 删除部门（管理员）
     */
    @DeleteMapping("/{id}")
    public Result<Map<String, Object>> deleteDepartment(
            @PathVariable Long id,
            HttpServletRequest request) {
        String role = (String) request.getAttribute("role");
        if (!"ADMIN".equals(role)) {
            return Result.error(403, "无权限操作");
        }
        Map<String, Object> result = departmentService.deleteDepartment(id);
        if (Boolean.TRUE.equals(result.get("success"))) {
            return Result.success(result);
        }
        return Result.error((String) result.get("message"));
    }
}
