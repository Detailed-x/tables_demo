package com.ledger.config;

import com.ledger.model.User;
import com.ledger.util.JsonFileUtil;
import com.ledger.util.JwtUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 应用启动初始化配置
 * 1. 初始化数据存储目录
 * 2. 初始化 JWT 参数
 * 3. 创建默认管理员账号
 */
@Configuration
public class AppConfig implements CommandLineRunner {

    @Value("${ledger.data.path}")
    private String dataPath;

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    @Value("${admin.username}")
    private String adminUsername;

    @Value("${admin.password}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        // 初始化 JSON 文件存储目录
        JsonFileUtil.setDataPath(dataPath);

        // 初始化 JWT
        JwtUtil.init(jwtSecret, jwtExpiration);

        // 初始化默认管理员账号
        initAdminUser();
    }

    private void initAdminUser() {
        List<User> users = JsonFileUtil.read("users.json", List.class, new ArrayList<>());
        boolean adminExists = false;
        for (Object u : users) {
            if (u instanceof java.util.Map) {
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> map = (java.util.Map<String, Object>) u;
                if (adminUsername.equals(map.get("username"))) {
                    adminExists = true;
                    break;
                }
            }
        }
        if (!adminExists) {
            User admin = new User();
            admin.setId(1L);
            admin.setUsername(adminUsername);
            admin.setPassword(adminPassword);
            admin.setRole("ADMIN");
            admin.setDepartmentId(null);
            admin.setCreatedAt(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            users.add(admin);
            JsonFileUtil.write("users.json", users);
            System.out.println("[初始化] 已创建默认管理员账号: " + adminUsername + " / " + adminPassword);
        }
    }
}
