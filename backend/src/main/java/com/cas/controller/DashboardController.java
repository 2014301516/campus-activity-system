package com.cas.controller;

import com.cas.common.Result;
import com.cas.entity.Activity;
import com.cas.service.ActivityService;
import com.cas.service.CategoryService;
import com.cas.service.RegistrationService;
import com.cas.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 数据统计控制器
 */
@RestController
@RequestMapping("/api")
public class DashboardController {

    @Autowired
    private ActivityService activityService;

    @Autowired
    private UserService userService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private RegistrationService registrationService;

    /**
     * 首页统计数据
     */
    @GetMapping("/dashboard/stats")
    public Result<Map<String, Object>> getStats() {
        Map<String, Object> stats = new HashMap<>();

        // 总活动数
        stats.put("totalActivities", activityService.count());

        // 进行中活动数
        stats.put("ongoingActivities", activityService.lambdaQuery()
                .eq(Activity::getStatus, "ongoing").count());

        // 总用户数
        stats.put("totalUsers", userService.count());

        // 各分类活动数（统计全部活动）
        List<Map<String, Object>> categoryStats = categoryService.getAllCategories().stream().map(cat -> {
            Map<String, Object> m = new HashMap<>();
            m.put("name", cat.getName());
            m.put("count", activityService.lambdaQuery()
                    .eq(Activity::getCategoryId, cat.getId()).count());
            return m;
        }).collect(Collectors.toList());
        stats.put("categoryStats", categoryStats);

        // 各状态活动数（含全部状态）
        Map<String, Long> statusStats = new HashMap<>();
        for (String status : new String[]{"draft", "pending", "approved", "rejected", "ongoing", "ended", "cancelled"}) {
            statusStats.put(status, activityService.lambdaQuery()
                    .eq(Activity::getStatus, status).count());
        }
        stats.put("statusStats", statusStats);

        // 报名总数
        stats.put("totalRegistrations", registrationService.lambdaQuery()
                .eq(com.cas.entity.Registration::getStatus, "registered").count());

        return Result.success(stats);
    }
}
