package com.cas.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cas.entity.Category;

import java.util.List;

/**
 * 分类服务接口
 */
public interface CategoryService extends IService<Category> {

    /**
     * 获取所有分类
     */
    List<Category> getAllCategories();

    /**
     * 新增分类（管理员）
     */
    void addCategory(Category category);

    /**
     * 删除分类（管理员）
     */
    void deleteCategory(Integer id);
}
