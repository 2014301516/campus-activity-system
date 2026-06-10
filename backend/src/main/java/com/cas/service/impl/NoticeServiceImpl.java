package com.cas.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cas.entity.Notice;
import com.cas.mapper.NoticeMapper;
import com.cas.service.NoticeService;
import org.springframework.stereotype.Service;

/**
 * 公告服务实现
 */
@Service
public class NoticeServiceImpl extends ServiceImpl<NoticeMapper, Notice> implements NoticeService {

    @Override
    public Page<Notice> getNoticePage(Integer page, Integer size) {
        Page<Notice> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<Notice> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Notice::getCreatedAt);
        return this.page(pageParam, wrapper);
    }

    @Override
    public Notice publishNotice(String title, String content, Long publisherId) {
        Notice notice = new Notice();
        notice.setTitle(title);
        notice.setContent(content);
        notice.setPublisherId(publisherId);
        this.save(notice);
        return notice;
    }

    @Override
    public void deleteNotice(Long id) {
        this.removeById(id);
    }
}
