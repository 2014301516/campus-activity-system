package com.cas.controller;

import com.cas.common.Result;
import com.cas.entity.Category;
import com.cas.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 分类控制器
 */
@RestController
@RequestMapping("/api")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    /**
     * 获取所有分类
     */
    @GetMapping("/category/list")
    public Result<List<Category>> list() {
        return Result.success(categoryService.getAllCategories());
    }
}
