package com.cas.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cas.common.Result;
import com.cas.dto.LoginDTO;
import com.cas.dto.RegisterDTO;
import com.cas.entity.User;
import com.cas.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Map;

/**
 * 用户控制器
 */
@RestController
@RequestMapping("/api")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 用户注册
     */
    @PostMapping("/user/register")
    public Result<?> register(@Valid @RequestBody RegisterDTO dto) {
        userService.register(dto);
        return Result.success("注册成功");
    }

    /**
     * 用户登录
     */
    @PostMapping("/user/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginDTO dto) {
        Map<String, Object> result = userService.login(dto);
        return Result.success("登录成功", result);
    }

    /**
     * 获取当前用户信息
     */
    @GetMapping("/user/info")
    public Result<User> getUserInfo(@RequestAttribute(required = false) Long userId) {
        if (userId == null) {
            return Result.unauthorized("请先登录");
        }
        User user = userService.getById(userId);
        // 不返回密码
        user.setPassword(null);
        return Result.success(user);
    }

    /**
     * 更新个人信息
     */
    @PutMapping("/user/update")
    public Result<?> updateProfile(@RequestBody User user) {
        userService.updateProfile(user);
        return Result.success("更新成功");
    }
}
