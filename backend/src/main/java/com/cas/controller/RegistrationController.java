package com.cas.controller;

import com.cas.common.Result;
import com.cas.entity.Registration;
import com.cas.service.RegistrationService;
import com.cas.util.SecurityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 报名控制器
 */
@RestController
@RequestMapping("/api")
public class RegistrationController {

    @Autowired
    private RegistrationService registrationService;

    @Autowired
    private SecurityUtil securityUtil;

    /**
     * 报名活动
     */
    @PostMapping("/activity/{activityId}/register")
    public Result<Registration> register(@PathVariable Long activityId) {
        securityUtil.requireStudent();
        Long userId = securityUtil.getCurrentUserId();
        Registration registration = registrationService.register(userId, activityId);
        return Result.success("报名成功", registration);
    }

    /**
     * 取消报名
     */
    @DeleteMapping("/activity/{activityId}/register")
    public Result<?> cancelRegister(@PathVariable Long activityId) {
        securityUtil.requireStudent();
        Long userId = securityUtil.getCurrentUserId();
        registrationService.cancelRegister(userId, activityId);
        return Result.success("已取消报名");
    }

    /**
     * 我的报名列表
     */
    @GetMapping("/my-registrations")
    public Result<List<Registration>> myRegistrations() {
        securityUtil.requireStudent();
        Long userId = securityUtil.getCurrentUserId();
        return Result.success(registrationService.getMyRegistrations(userId));
    }

    /**
     * 活动报名名单
     */
    @GetMapping("/activity/{activityId}/registrations")
    public Result<List<Registration>> activityRegistrations(@PathVariable Long activityId) {
        return Result.success(registrationService.getActivityRegistrations(activityId));
    }
}
