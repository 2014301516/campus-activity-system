package com.cas.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cas.dto.LoginDTO;
import com.cas.dto.RegisterDTO;
import com.cas.entity.User;
import com.cas.mapper.UserMapper;
import com.cas.service.UserService;
import com.cas.config.WeChatConfig;
import com.cas.util.JwtUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 用户服务实现
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private WeChatConfig weChatConfig;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public void register(RegisterDTO dto) {
        // 检查用户名是否已存在
        User existUser = getByUsername(dto.getUsername());
        if (existUser != null) {
            throw new RuntimeException("用户名已存在");
        }

        // 检查学号是否已存在
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getStudentId, dto.getStudentId());
        if (this.count(wrapper) > 0) {
            throw new RuntimeException("该学号已注册");
        }

        User user = new User();
        BeanUtil.copyProperties(dto, user);
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        if (!StringUtils.hasText(user.getRole())) {
            user.setRole("student");
        }
        user.setStatus(1);

        this.save(user);
    }

    @Override
    public Map<String, Object> login(LoginDTO dto) {
        // 查询用户
        User user = getByUsername(dto.getUsername());
        if (user == null) {
            throw new RuntimeException("用户名或密码错误");
        }

        // 检查账号状态
        if (user.getStatus() == 0) {
            throw new RuntimeException("账号已被禁用，请联系管理员");
        }

        // 校验密码
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }

        // 生成 Token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("userId", user.getId());
        result.put("username", user.getUsername());
        result.put("realName", user.getRealName());
        result.put("role", user.getRole());
        return result;
    }

    @Override
    public User getByUsername(String username) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        return this.getOne(wrapper);
    }

    @Override
    public void updateProfile(User user) {
        // 不允许修改角色和状态
        user.setRole(null);
        user.setStatus(null);
        user.setPassword(null);
        this.updateById(user);
    }

    @Override
    public Page<User> getUserPage(Integer page, Integer size, String keyword) {
        Page<User> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(User::getUsername, keyword)
                   .or()
                   .like(User::getRealName, keyword)
                   .or()
                   .like(User::getStudentId, keyword);
        }
        wrapper.orderByDesc(User::getCreatedAt);
        return this.page(pageParam, wrapper);
    }

    @Override
    public void updateStatus(Long operatorId, Long userId, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new RuntimeException("用户状态无效");
        }
        User user = this.getById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        if (status == 0 && operatorId.equals(userId)) {
            throw new RuntimeException("不能禁用当前登录的管理员账号");
        }
        if (status == 0 && "admin".equals(user.getRole()) && user.getStatus() == 1) {
            long activeAdminCount = this.lambdaQuery()
                    .eq(User::getRole, "admin")
                    .eq(User::getStatus, 1)
                    .count();
            if (activeAdminCount <= 1) {
                throw new RuntimeException("至少保留一个可用的管理员账号");
            }
        }
        user.setStatus(status);
        this.updateById(user);
    }

    @Override
    public Map<String, Object> wxLogin(String code) {
        // 1. 调微信接口，用 code 换 openid
        String url = "https://api.weixin.qq.com/sns/jscode2session" +
                "?appid=" + weChatConfig.getAppid() +
                "&secret=" + weChatConfig.getSecret() +
                "&js_code=" + code +
                "&grant_type=authorization_code";
        String resp = restTemplate.getForObject(url, String.class);
        JSONObject json = JSONUtil.parseObj(resp);

        String openid = json.getStr("openid");
        if (openid == null) {
            throw new RuntimeException("微信登录失败: " + json.getStr("errmsg"));
        }

        // 2. 用 openid 查用户（username = openid）
        User user = getByUsername(openid);
        if (user == null) {
            // 新用户 → 自动注册
            user = new User();
            user.setUsername(openid);
            user.setPassword(passwordEncoder.encode(UUID.randomUUID().toString())); // 随机密码
            user.setRealName("微信用户");
            user.setStudentId("wx_" + openid.substring(0, 8));
            user.setPhone("");
            user.setRole("student");
            user.setStatus(1);
            this.save(user);
        } else {
            // 检查状态
            if (user.getStatus() == 0) {
                throw new RuntimeException("账号已被禁用");
            }
        }

        // 3. 生成 JWT
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("userId", user.getId());
        result.put("username", user.getUsername());
        result.put("realName", user.getRealName());
        result.put("role", user.getRole());
        return result;
    }
}
