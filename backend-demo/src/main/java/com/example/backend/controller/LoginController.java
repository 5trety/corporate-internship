package com.example.backend.controller;

import com.example.backend.entity.Result;
import com.example.backend.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class LoginController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostMapping("/login")
    public Result<Map<String, String>> login(@RequestParam String username,
                                             @RequestParam String password,
                                             HttpSession session) {
        System.out.println("登录请求: " + username);
        try {
            String sql = "SELECT * FROM user WHERE username = ?";
            List<User> users = jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(User.class), username);

            if (users.isEmpty()) {
                // 自动注册
                String insertSql = "INSERT INTO user (username, password) VALUES (?, ?)";
                int rows = jdbcTemplate.update(insertSql, username, password);
                if (rows > 0) {
                    session.setAttribute("user", username);
                    Map<String, String> data = new HashMap<>();
                    data.put("username", username);
                    return Result.success(data);
                } else {
                    return Result.error("注册失败");
                }
            } else {
                User user = users.get(0);
                if (password.equals(user.getPassword())) {
                    session.setAttribute("user", username);
                    Map<String, String> data = new HashMap<>();
                    data.put("username", username);
                    return Result.success(data);
                } else {
                    return Result.error("密码错误");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("登录失败: " + e.getMessage());
        }
    }

    @PostMapping("/logout")
    public Result<String> logout(HttpSession session) {
        session.invalidate();
        return Result.success("已退出");
    }
}