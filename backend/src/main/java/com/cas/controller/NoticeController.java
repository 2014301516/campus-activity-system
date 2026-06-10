package com.cas.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cas.common.Result;
import com.cas.entity.Notice;
import com.cas.service.NoticeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 公告控制器（公开接口）
 */
@RestController
@RequestMapping("/api")
public class NoticeController {

    @Autowired
    private NoticeService noticeService;

    /**
     * 公告列表（公开）
     */
    @GetMapping("/notice/list")
    public Result<Page<Notice>> list(@RequestParam(defaultValue = "1") Integer page,
                                      @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(noticeService.getNoticePage(page, size));
    }
}
