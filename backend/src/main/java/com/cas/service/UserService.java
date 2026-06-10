package com.cas.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cas.dto.LoginDTO;
import com.cas.dto.RegisterDTO;
import com.cas.entity.User;

import java.util.Map;

/**
 * 用户服务接口
 */
public interface UserService extends IService<User> {

    /**
     * 用户注册
     */
    void register(RegisterDTO dto);

    /**
     * 用户登录，返回 Token
     */
    Map<String, Object> login(LoginDTO dto);

    /**
     * 根据用户名查询用户
     */
    User getByUsername(String username);

    /**
     * 更新个人信息
     */
    void updateProfile(User user);

    /**
     * 分页查询用户列表（管理员）
     */
    Page<User> getUserPage(Integer page, Integer size, String keyword);

    /**
     * 修改用户状态（管理员）
     */
    void updateStatus(Long userId, Integer status);
}
