package com.ledger.model;

import lombok.Data;

/**
 * 用户模型
 */
@Data
public class User {
    private Long id;
    private String username;
    private String password;
    /** ADMIN - 管理员, USER - 普通用户 */
    private String role;
    /** 所属部门ID，管理员可为null */
    private Long departmentId;
    private String createdAt;
}
