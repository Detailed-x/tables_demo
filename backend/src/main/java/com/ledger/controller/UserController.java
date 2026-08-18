package com.ledger.controller;

import com.ledger.model.Result;
import com.ledger.model.User;
import com.ledger.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

/**
 * 用户控制器：用户列表、分配部门、当前用户信息
 */
@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 获取所有用户列表（管理员）
     */
    @GetMapping
    public Result<List<User>> getAllUsers(HttpServletRequest request) {
        String role = (String) request.getAttribute("role");
        if (!"ADMIN".equals(role)) {
            return Result.error(403, "无权限操作");
        }
        return Result.success(userService.getAllUsers());
    }

    /**
     * 获取当前登录用户信息
     */
    @GetMapping("/me")
    public Result<User> getCurrentUser(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        User user = userService.getUserById(userId);
        if (user != null) {
            user.setPassword(null);
        }
        return Result.success(user);
    }

    /**
     * 分配用户到部门（管理员）
     */
    @PutMapping("/{userId}/department")
    public Result<Map<String, Object>> assignDepartment(
            @PathVariable Long userId,
            @RequestBody Map<String, Long> body,
            HttpServletRequest request) {
        String role = (String) request.getAttribute("role");
        if (!"ADMIN".equals(role)) {
            return Result.error(403, "无权限操作");
        }
        Long departmentId = body.get("departmentId");
        Map<String, Object> result = userService.assignDepartment(userId, departmentId);
        if (Boolean.TRUE.equals(result.get("success"))) {
            return Result.success(result);
        }
        return Result.error((String) result.get("message"));
    }

    /**
     * 获取某部门的用户列表
     */
    @GetMapping("/department/{departmentId}")
    public Result<List<User>> getUsersByDepartment(@PathVariable Long departmentId) {
        return Result.success(userService.getUsersByDepartment(departmentId));
    }
}
