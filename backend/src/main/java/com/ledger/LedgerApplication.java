package com.ledger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 多人在线表格台账系统 - 启动类
 */
@SpringBootApplication
public class LedgerApplication {
    public static void main(String[] args) {
        SpringApplication.run(LedgerApplication.class, args);
        System.out.println("========================================");
        System.out.println("  多人在线表格台账系统启动成功!");
        System.out.println("  后端地址: http://localhost:8080/api");
        System.out.println("  管理员账号: admin / admin123");
        System.out.println("========================================");
    }
}
