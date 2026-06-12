package com.cas.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cas.dto.ActivityQueryDTO;
import com.cas.dto.ActivitySaveDTO;
import com.cas.entity.Activity;

/**
 * 活动服务接口
 */
public interface ActivityService extends IService<Activity> {

    /**
     * 分页查询活动列表
     */
    Page<Activity> getActivityPage(ActivityQueryDTO query);

    /**
     * 获取活动详情
     */
    Activity getActivityDetail(Long id);

    /**
     * 按当前时间刷新活动状态
     */
    void refreshActivityStatuses();

    /**
     * 创建活动（组织者/管理员）
     */
    Activity createActivity(ActivitySaveDTO dto, Long userId);

    /**
     * 更新活动（组织者/管理员）
     */
    Activity updateActivity(ActivitySaveDTO dto, Long userId);

    /**
     * 删除活动（组织者/管理员）
     */
    void deleteActivity(Long id, Long userId);

    /**
     * 审核活动（管理员）
     */
    void auditActivity(Long id, String status);

    /**
     * 取消活动（管理员）
     */
    void cancelActivity(Long id);
}
