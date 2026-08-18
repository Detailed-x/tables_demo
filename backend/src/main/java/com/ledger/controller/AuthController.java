package com.ledger.controller;

import com.ledger.model.Result;
import com.ledger.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 认证控制器：登录、注册
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public Result<Map<String, Object>> register(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        Map<String, Object> result = userService.register(username, password);
        if (Boolean.TRUE.equals(result.get("success"))) {
            return Result.success(result);
        }
        return Result.error((String) result.get("message"));
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        Map<String, Object> result = userService.login(username, password);
        if (Boolean.TRUE.equals(result.get("success"))) {
            return Result.success(result);
        }
        return Result.error(401, (String) result.get("message"));
    }
}
