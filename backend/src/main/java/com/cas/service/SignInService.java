package com.cas.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cas.entity.SignIn;

import java.util.List;

/**
 * 签到服务接口
 */
public interface SignInService extends IService<SignIn> {

    /**
     * 签到
     */
    SignIn signIn(Long userId, Long activityId);

    /**
     * 签退
     */
    void signOut(Long signInId, Long userId);

    /**
     * 查询某活动的签到记录
     */
    List<SignIn> getActivitySignIns(Long activityId);

    /**
     * 查询用户在某活动的签到状态
     */
    SignIn getUserSignIn(Long userId, Long activityId);
}
