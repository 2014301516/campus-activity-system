package com.cas.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cas.entity.Activity;
import com.cas.entity.Registration;
import com.cas.entity.SignIn;
import com.cas.entity.User;
import com.cas.mapper.SignInMapper;
import com.cas.service.ActivityService;
import com.cas.service.RegistrationService;
import com.cas.service.SignInService;
import com.cas.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 签到服务实现
 */
@Service
public class SignInServiceImpl extends ServiceImpl<SignInMapper, SignIn> implements SignInService {

    @Autowired
    private RegistrationService registrationService;

    @Autowired
    private ActivityService activityService;

    @Autowired
    private UserService userService;

    @Override
    @Transactional
    public SignIn signIn(Long userId, Long activityId) {
        Activity activity = activityService.getById(activityId);
        if (activity == null) {
            throw new RuntimeException("活动不存在");
        }
        if (!"approved".equals(activity.getStatus()) && !"ongoing".equals(activity.getStatus())) {
            throw new RuntimeException("该活动当前不可签到");
        }

        // 检查是否已报名
        LambdaQueryWrapper<Registration> rWrapper = new LambdaQueryWrapper<>();
        rWrapper.eq(Registration::getUserId, userId)
                .eq(Registration::getActivityId, activityId)
                .eq(Registration::getStatus, "registered");
        Registration registration = registrationService.getOne(rWrapper);
        if (registration == null) {
            throw new RuntimeException("您未报名该活动，无法签到");
        }

        // 检查是否已签到
        LambdaQueryWrapper<SignIn> sWrapper = new LambdaQueryWrapper<>();
        sWrapper.eq(SignIn::getUserId, userId)
                .eq(SignIn::getActivityId, activityId);
        if (this.count(sWrapper) > 0) {
            throw new RuntimeException("您已签到，请勿重复签到");
        }

        // 检查活动时间
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(activity.getStartTime().minusHours(1))) {
            throw new RuntimeException("签到时间未到，活动开始前1小时方可签到");
        }
        if (now.isAfter(activity.getEndTime())) {
            throw new RuntimeException("活动已结束，不能再签到");
        }

        SignIn signIn = new SignIn();
        signIn.setRegistrationId(registration.getId());
        signIn.setUserId(userId);
        signIn.setActivityId(activityId);
        signIn.setSignInTime(now);
        this.save(signIn);

        return signIn;
    }

    @Override
    public void signOut(Long signInId, Long userId) {
        SignIn signIn = this.getById(signInId);
        if (signIn == null) {
            throw new RuntimeException("签到记录不存在");
        }
        if (!signIn.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作");
        }
        if (signIn.getSignOutTime() != null) {
            throw new RuntimeException("已签退，请勿重复操作");
        }
        signIn.setSignOutTime(LocalDateTime.now());
        this.updateById(signIn);
    }

    @Override
    public List<SignIn> getActivitySignIns(Long activityId) {
        LambdaQueryWrapper<SignIn> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SignIn::getActivityId, activityId)
               .orderByAsc(SignIn::getSignInTime);
        List<SignIn> list = this.list(wrapper);
        fillUserNames(list);
        return list;
    }

    @Override
    public SignIn getUserSignIn(Long userId, Long activityId) {
        LambdaQueryWrapper<SignIn> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SignIn::getUserId, userId)
               .eq(SignIn::getActivityId, activityId);
        return this.getOne(wrapper);
    }

    private void fillUserNames(List<SignIn> list) {
        if (list.isEmpty()) return;
        Map<Long, String> userMap = userService.listByIds(
                list.stream().map(SignIn::getUserId).collect(Collectors.toSet())
        ).stream().collect(Collectors.toMap(User::getId, User::getRealName));
        for (SignIn s : list) {
            s.setUserName(userMap.getOrDefault(s.getUserId(), "未知"));
        }
    }
}
