package com.cas.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cas.entity.Registration;

import java.util.List;

/**
 * 报名服务接口
 */
public interface RegistrationService extends IService<Registration> {

    /**
     * 学生报名活动
     */
    Registration register(Long userId, Long activityId);

    /**
     * 取消报名
     */
    void cancelRegister(Long userId, Long activityId);

    /**
     * 查询用户的报名记录
     */
    List<Registration> getMyRegistrations(Long userId);

    /**
     * 查询活动的报名名单
     */
    List<Registration> getActivityRegistrations(Long activityId);
}
