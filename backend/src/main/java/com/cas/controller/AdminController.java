package com.cas.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cas.common.Result;
import com.cas.entity.Category;
import com.cas.entity.Notice;
import com.cas.entity.User;
import com.cas.service.ActivityService;
import com.cas.service.CategoryService;
import com.cas.service.NoticeService;
import com.cas.service.UserService;
import com.cas.util.SecurityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 管理员控制器
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private UserService userService;

    @Autowired
    private ActivityService activityService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private NoticeService noticeService;

    @Autowired
    private SecurityUtil securityUtil;

    // ==================== 用户管理 ====================

    /**
     * 用户列表（分页+搜索）
     */
    @GetMapping("/users")
    public Result<Page<User>> userList(@RequestParam(defaultValue = "1") Integer page,
                                        @RequestParam(defaultValue = "10") Integer size,
                                        @RequestParam(required = false) String keyword) {
        Page<User> result = userService.getUserPage(page, size, keyword);
        // 脱敏：移除密码
        result.getRecords().forEach(u -> u.setPassword(null));
        return Result.success(result);
    }

    /**
     * 修改用户状态
     */
    @PutMapping("/users/{id}/status")
    public Result<?> updateUserStatus(@PathVariable Long id, @RequestBody Map<String, Integer> body) {
        userService.updateStatus(id, body.get("status"));
        return Result.success("状态更新成功");
    }

    // ==================== 活动审核 ====================

    /**
     * 审核活动
     */
    @PutMapping("/activity/{id}/audit")
    public Result<?> auditActivity(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String status = body.get("status"); // approved / rejected → pending
        activityService.auditActivity(id, status);
        return Result.success("审核完成");
    }

    // ==================== 分类管理 ====================

    /**
     * 新增分类
     */
    @PostMapping("/category")
    public Result<Category> addCategory(@RequestBody Category category) {
        categoryService.addCategory(category);
        return Result.success("添加成功", category);
    }

    /**
     * 删除分类
     */
    @DeleteMapping("/category/{id}")
    public Result<?> deleteCategory(@PathVariable Integer id) {
        categoryService.deleteCategory(id);
        return Result.success("删除成功");
    }

    // ==================== 公告管理 ====================

    /**
     * 发布公告
     */
    @PostMapping("/notice")
    public Result<Notice> publishNotice(@RequestBody Map<String, String> body) {
        Long userId = securityUtil.getCurrentUserId();
        Notice notice = noticeService.publishNotice(body.get("title"), body.get("content"), userId);
        return Result.success("发布成功", notice);
    }

    /**
     * 删除公告
     */
    @DeleteMapping("/notice/{id}")
    public Result<?> deleteNotice(@PathVariable Long id) {
        noticeService.deleteNotice(id);
        return Result.success("删除成功");
    }
}
