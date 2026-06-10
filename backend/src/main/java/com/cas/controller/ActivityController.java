package com.cas.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cas.common.Result;
import com.cas.dto.ActivityQueryDTO;
import com.cas.dto.ActivitySaveDTO;
import com.cas.entity.Activity;
import com.cas.service.ActivityService;
import com.cas.util.SecurityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * 活动控制器
 */
@RestController
@RequestMapping("/api")
public class ActivityController {

    @Autowired
    private ActivityService activityService;

    @Autowired
    private SecurityUtil securityUtil;

    /**
     * 活动列表（分页+筛选+搜索）
     */
    @GetMapping("/activity/list")
    public Result<Page<Activity>> list(ActivityQueryDTO query) {
        Page<Activity> result = activityService.getActivityPage(query);
        return Result.success(result);
    }

    /**
     * 活动详情
     */
    @GetMapping("/activity/{id}")
    public Result<Activity> detail(@PathVariable Long id) {
        Activity activity = activityService.getActivityDetail(id);
        return Result.success(activity);
    }

    /**
     * 创建活动（组织者/管理员）
     */
    @PostMapping("/activity")
    public Result<Activity> create(@Valid @RequestBody ActivitySaveDTO dto) {
        Long userId = securityUtil.getCurrentUserId();
        Activity activity = activityService.createActivity(dto, userId);
        return Result.success("创建成功", activity);
    }

    /**
     * 更新活动（组织者/管理员）
     */
    @PutMapping("/activity/{id}")
    public Result<Activity> update(@PathVariable Long id,
                                    @Valid @RequestBody ActivitySaveDTO dto) {
        Long userId = securityUtil.getCurrentUserId();
        dto.setId(id);
        Activity activity = activityService.updateActivity(dto, userId);
        return Result.success("更新成功", activity);
    }

    /**
     * 删除活动（组织者/管理员）
     */
    @DeleteMapping("/activity/{id}")
    public Result<?> delete(@PathVariable Long id) {
        Long userId = securityUtil.getCurrentUserId();
        activityService.deleteActivity(id, userId);
        return Result.success("删除成功");
    }
}
