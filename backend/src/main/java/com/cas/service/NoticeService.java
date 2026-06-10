package com.cas.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cas.entity.Notice;

/**
 * 公告服务接口
 */
public interface NoticeService extends IService<Notice> {

    /**
     * 分页查询公告
     */
    Page<Notice> getNoticePage(Integer page, Integer size);

    /**
     * 发布公告
     */
    Notice publishNotice(String title, String content, Long publisherId);

    /**
     * 删除公告
     */
    void deleteNotice(Long id);
}
