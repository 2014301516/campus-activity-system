package com.cas.controller;

import com.cas.common.Result;
import com.cas.entity.SignIn;
import com.cas.service.SignInService;
import com.cas.util.SecurityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 签到控制器
 */
@RestController
@RequestMapping("/api")
public class SignInController {

    @Autowired
    private SignInService signInService;

    @Autowired
    private SecurityUtil securityUtil;

    /**
     * 签到
     */
    @PostMapping("/sign-in")
    public Result<SignIn> signIn(@RequestBody Map<String, Long> body) {
        Long userId = securityUtil.getCurrentUserId();
        Long activityId = body.get("activityId");
        SignIn signIn = signInService.signIn(userId, activityId);
        return Result.success("签到成功", signIn);
    }

    /**
     * 签退
     */
    @PutMapping("/sign-in/{id}")
    public Result<?> signOut(@PathVariable Long id) {
        Long userId = securityUtil.getCurrentUserId();
        signInService.signOut(id, userId);
        return Result.success("签退成功");
    }

    /**
     * 我的签到状态
     */
    @GetMapping("/sign-in/status")
    public Result<SignIn> getSignInStatus(@RequestParam Long activityId) {
        Long userId = securityUtil.getCurrentUserId();
        SignIn signIn = signInService.getUserSignIn(userId, activityId);
        return Result.success(signIn);
    }

    /**
     * 活动签到记录列表
     */
    @GetMapping("/sign-in/activity/{activityId}")
    public Result<List<SignIn>> activitySignIns(@PathVariable Long activityId) {
        return Result.success(signInService.getActivitySignIns(activityId));
    }
}
