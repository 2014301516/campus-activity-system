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

import com.cas.entity.Registration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
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
        activityService.refreshActivityStatuses();

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

    /**
     * 报名趋势（近30天每日报名数）
     */
    @GetMapping("/dashboard/registration-trend")
    public Result<List<Map<String, Object>>> getRegistrationTrend() {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(29);

        // 初始化30天全为0
        Map<String, Long> dayCount = new LinkedHashMap<>();
        for (LocalDate d = startDate; !d.isAfter(endDate); d = d.plusDays(1)) {
            dayCount.put(d.format(DateTimeFormatter.ISO_LOCAL_DATE), 0L);
        }

        // 查实际报名数据
        List<Registration> registrations = registrationService.lambdaQuery()
                .eq(Registration::getStatus, "registered")
                .ge(Registration::getRegisteredAt, startDate.atStartOfDay())
                .list();

        for (Registration r : registrations) {
            String day = r.getRegisteredAt().toLocalDate().toString();
            dayCount.merge(day, 1L, Long::sum);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, Long> e : dayCount.entrySet()) {
            Map<String, Object> m = new HashMap<>();
            m.put("date", e.getKey().substring(5)); // MM-DD
            m.put("count", e.getValue());
            result.add(m);
        }
        return Result.success(result);
    }

    /**
     * 月度活动数量对比
     */
    @GetMapping("/dashboard/monthly-activities")
    public Result<List<Map<String, Object>>> getMonthlyActivities() {
        List<Activity> activities = activityService.lambdaQuery()
                .orderByAsc(Activity::getStartTime)
                .list();

        // 按月统计
        Map<String, Long> monthCount = new LinkedHashMap<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM");
        for (Activity a : activities) {
            String month = a.getStartTime().toLocalDate().format(fmt);
            monthCount.merge(month, 1L, Long::sum);
        }

        // 只取最近12个月
        List<Map.Entry<String, Long>> entries = new ArrayList<>(monthCount.entrySet());
        if (entries.size() > 12) {
            entries = entries.subList(entries.size() - 12, entries.size());
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, Long> e : entries) {
            Map<String, Object> m = new HashMap<>();
            m.put("month", e.getKey());
            m.put("count", e.getValue());
            result.add(m);
        }
        return Result.success(result);
    }
}
