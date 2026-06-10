package com.cas.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cas.entity.Activity;
import com.cas.entity.Registration;
import com.cas.entity.User;
import com.cas.mapper.RegistrationMapper;
import com.cas.service.ActivityService;
import com.cas.service.RegistrationService;
import com.cas.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 报名服务实现
 */
@Service
public class RegistrationServiceImpl extends ServiceImpl<RegistrationMapper, Registration> implements RegistrationService {

    @Autowired
    private ActivityService activityService;

    @Autowired
    private UserService userService;

    @Override
    @Transactional
    public Registration register(Long userId, Long activityId) {
        Activity activity = activityService.getById(activityId);
        if (activity == null) {
            throw new RuntimeException("活动不存在");
        }

        // 检查活动状态
        if (!"approved".equals(activity.getStatus()) && !"ongoing".equals(activity.getStatus())) {
            throw new RuntimeException("该活动当前不可报名");
        }

        // 检查是否已报名
        LambdaQueryWrapper<Registration> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Registration::getUserId, userId)
               .eq(Registration::getActivityId, activityId)
               .eq(Registration::getStatus, "registered");
        if (this.count(wrapper) > 0) {
            throw new RuntimeException("您已报名该活动，请勿重复报名");
        }

        // 检查人数上限
        if (activity.getCurrentParticipants() >= activity.getMaxParticipants()) {
            throw new RuntimeException("报名人数已满");
        }

        // 创建报名记录
        Registration registration = new Registration();
        registration.setUserId(userId);
        registration.setActivityId(activityId);
        registration.setStatus("registered");
        this.save(registration);

        // 更新活动报名人数
        activity.setCurrentParticipants(activity.getCurrentParticipants() + 1);
        activityService.updateById(activity);

        return registration;
    }

    @Override
    @Transactional
    public void cancelRegister(Long userId, Long activityId) {
        LambdaQueryWrapper<Registration> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Registration::getUserId, userId)
               .eq(Registration::getActivityId, activityId)
               .eq(Registration::getStatus, "registered");
        Registration registration = this.getOne(wrapper);
        if (registration == null) {
            throw new RuntimeException("未找到报名记录");
        }

        registration.setStatus("cancelled");
        this.updateById(registration);

        // 更新活动报名人数
        Activity activity = activityService.getById(activityId);
        if (activity.getCurrentParticipants() > 0) {
            activity.setCurrentParticipants(activity.getCurrentParticipants() - 1);
            activityService.updateById(activity);
        }
    }

    @Override
    public List<Registration> getMyRegistrations(Long userId) {
        LambdaQueryWrapper<Registration> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Registration::getUserId, userId)
               .orderByDesc(Registration::getRegisteredAt);
        List<Registration> list = this.list(wrapper);

        // 填充用户和活动信息
        fillInfo(list);
        return list;
    }

    @Override
    public List<Registration> getActivityRegistrations(Long activityId) {
        LambdaQueryWrapper<Registration> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Registration::getActivityId, activityId)
               .eq(Registration::getStatus, "registered");
        List<Registration> list = this.list(wrapper);

        fillInfo(list);
        return list;
    }

    private void fillInfo(List<Registration> list) {
        if (list.isEmpty()) return;

        Map<Long, String> userMap = userService.listByIds(
                list.stream().map(Registration::getUserId).collect(Collectors.toSet())
        ).stream().collect(Collectors.toMap(User::getId, User::getRealName));

        Map<Long, String> activityMap = activityService.listByIds(
                list.stream().map(Registration::getActivityId).collect(Collectors.toSet())
        ).stream().collect(Collectors.toMap(Activity::getId, Activity::getTitle));

        for (Registration r : list) {
            r.setUserName(userMap.getOrDefault(r.getUserId(), "未知"));
            r.setActivityTitle(activityMap.getOrDefault(r.getActivityId(), "未知"));
        }
    }
}
