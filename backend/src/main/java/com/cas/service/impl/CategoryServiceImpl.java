package com.cas.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cas.entity.Activity;
import com.cas.entity.Category;
import com.cas.mapper.ActivityMapper;
import com.cas.mapper.CategoryMapper;
import com.cas.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 分类服务实现
 */
@Service
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements CategoryService {

    @Autowired
    private ActivityMapper activityMapper;

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
        LambdaQueryWrapper<Activity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Activity::getCategoryId, id);
        Long activityCount = activityMapper.selectCount(wrapper);
        if (activityCount > 0) {
            throw new RuntimeException("该分类下仍有关联活动，请先调整活动分类");
        }
        this.removeById(id);
    }
}
