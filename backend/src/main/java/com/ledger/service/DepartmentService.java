package com.ledger.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ledger.model.Department;
import com.ledger.util.IdGenerator;
import com.ledger.util.JsonFileUtil;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 部门服务：增删改查
 */
@Service
public class DepartmentService {

    private static final String FILE = "departments.json";
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * 获取所有部门
     */
    public List<Department> getAllDepartments() {
        return JsonFileUtil.read(FILE, new TypeReference<List<Department>>() {}, new ArrayList<>());
    }

    /**
     * 根据ID获取部门
     */
    public Department getDepartmentById(Long id) {
        for (Department d : getAllDepartments()) {
            if (d.getId().equals(id)) {
                return d;
            }
        }
        return null;
    }

    /**
     * 创建部门
     */
    public Map<String, Object> createDepartment(String name) {
        Map<String, Object> result = new HashMap<>();
        if (name == null || name.trim().isEmpty()) {
            result.put("success", false);
            result.put("message", "部门名称不能为空");
            return result;
        }
        List<Department> departments = getAllDepartments();
        for (Department d : departments) {
            if (d.getName().equals(name)) {
                result.put("success", false);
                result.put("message", "部门名称已存在");
                return result;
            }
        }

        Department dept = new Department();
        dept.setId(IdGenerator.nextId());
        dept.setName(name);
        dept.setCreatedAt(now());
        departments.add(dept);
        JsonFileUtil.write(FILE, departments);

        result.put("success", true);
        result.put("message", "部门创建成功");
        result.put("data", dept);
        return result;
    }

    /**
     * 更新部门名称
     */
    public Map<String, Object> updateDepartment(Long id, String name) {
        Map<String, Object> result = new HashMap<>();
        List<Department> departments = getAllDepartments();
        boolean found = false;
        for (Department d : departments) {
            if (d.getId().equals(id)) {
                d.setName(name);
                found = true;
                break;
            }
        }
        if (!found) {
            result.put("success", false);
            result.put("message", "部门不存在");
            return result;
        }
        JsonFileUtil.write(FILE, departments);
        result.put("success", true);
        result.put("message", "部门更新成功");
        return result;
    }

    /**
     * 删除部门
     */
    public Map<String, Object> deleteDepartment(Long id) {
        Map<String, Object> result = new HashMap<>();
        List<Department> departments = getAllDepartments();
        boolean removed = departments.removeIf(d -> d.getId().equals(id));
        if (!removed) {
            result.put("success", false);
            result.put("message", "部门不存在");
            return result;
        }
        JsonFileUtil.write(FILE, departments);
        result.put("success", true);
        result.put("message", "部门删除成功");
        return result;
    }

    private String now() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}
