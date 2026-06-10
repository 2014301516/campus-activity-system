package com.cas.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cas.entity.Category;
import com.cas.mapper.CategoryMapper;
import com.cas.service.CategoryService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 分类服务实现
 */
@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

    @Override
    public List<Category> getAllCategories() {
        return this.list();
    }

    @Override
    public void addCategory(Category category) {
        this.save(category);
    }

    @Override
    public void deleteCategory(Integer id) {
        this.removeById(id);
    }
}
