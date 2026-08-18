package com.ledger.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ledger.model.User;
import com.ledger.util.IdGenerator;
import com.ledger.util.JsonFileUtil;
import com.ledger.util.JwtUtil;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户服务：注册、登录、查询、分配部门
 */
@Service
public class UserService {

    private static final String FILE = "users.json";
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * 获取所有用户
     */
    public List<User> getAllUsers() {
        List<User> users = JsonFileUtil.read(FILE, new TypeReference<List<User>>() {}, new ArrayList<>());
        // 脱敏：不返回密码
        for (User u : users) {
            u.setPassword(null);
        }
        return users;
    }

    /**
     * 根据ID获取用户
     */
    public User getUserById(Long id) {
        List<User> users = readUsers();
        for (User u : users) {
            if (u.getId().equals(id)) {
                return u;
            }
        }
        return null;
    }

    /**
     * 根据用户名获取用户
     */
    public User getUserByUsername(String username) {
        List<User> users = readUsers();
        for (User u : users) {
            if (u.getUsername().equals(username)) {
                return u;
            }
        }
        return null;
    }

    /**
     * 用户注册
     */
    public Map<String, Object> register(String username, String password) {
        Map<String, Object> result = new HashMap<>();
        if (username == null || username.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "用户名不能为空");
            return result;
        }
        if (password == null || password.length() < 3) {
            result.put("success", false);
            result.put("message", "密码长度不能少于3位");
            return result;
        }
        if (getUserByUsername(username) != null) {
            result.put("success", false);
            result.put("message", "用户名已存在");
            return result;
        }

        User user = new User();
        user.setId(IdGenerator.nextId());
        user.setUsername(username);
        user.setPassword(password);
        user.setRole("USER");
        user.setDepartmentId(null);
        user.setCreatedAt(now());

        List<User> users = readUsers();
        users.add(user);
        JsonFileUtil.write(FILE, users);

        result.put("success", true);
        result.put("message", "注册成功，请等待管理员分配部门");
        return result;
    }

    /**
     * 用户登录
     */
    public Map<String, Object> login(String username, String password) {
        Map<String, Object> result = new HashMap<>();
        User user = getUserByUsername(username);
        if (user == null || !user.getPassword().equals(password)) {
            result.put("success", false);
            result.put("message", "用户名或密码错误");
            return result;
        }

        String token = JwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        result.put("success", true);
        result.put("token", token);
        result.put("userId", user.getId());
        result.put("username", user.getUsername());
        result.put("role", user.getRole());
        result.put("departmentId", user.getDepartmentId());
        return result;
    }

    /**
     * 分配用户到部门（管理员操作）
     */
    public Map<String, Object> assignDepartment(Long userId, Long departmentId) {
        Map<String, Object> result = new HashMap<>();
        List<User> users = readUsers();
        boolean found = false;
        for (User u : users) {
            if (u.getId().equals(userId)) {
                u.setDepartmentId(departmentId);
                found = true;
                break;
            }
        }
        if (!found) {
            result.put("success", false);
            result.put("message", "用户不存在");
            return result;
        }
        JsonFileUtil.write(FILE, users);
        result.put("success", true);
        result.put("message", "部门分配成功");
        return result;
    }

    /**
     * 获取某部门的所有用户
     */
    public List<User> getUsersByDepartment(Long departmentId) {
        List<User> users = readUsers();
        List<User> result = new ArrayList<>();
        for (User u : users) {
            if (departmentId.equals(u.getDepartmentId())) {
                u.setPassword(null);
                result.add(u);
            }
        }
        return result;
    }

    private List<User> readUsers() {
        return JsonFileUtil.read(FILE, new TypeReference<List<User>>() {}, new ArrayList<>());
    }

    private String now() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}
