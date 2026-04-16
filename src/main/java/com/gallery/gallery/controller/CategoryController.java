package com.gallery.gallery.controller;

import com.gallery.gallery.entity.Category;
import com.gallery.gallery.repository.CategoryRepository;
import com.gallery.gallery.repository.PhotoRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;




@RestController
@RequestMapping("/categories")
public class CategoryController 
{
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private PhotoRepository photoRepository;


    @GetMapping
    public List<Category> getAllCategories()
    {
        return categoryRepository.findAll();
    }

    @GetMapping("/{id}")
    public Category getCategoryById(@PathVariable Integer id)
    {
        return categoryRepository.findById(id).orElse(null);
    }
    
    @PostMapping
    public String postCategory(@RequestParam("title") String title)
    {
        Category existingCategory = categoryRepository.findByTitleIgnoreCase(title);
        if (existingCategory != null) {
            return "Ошибка: категория с названием '" + title + "' уже существует";
        }
        Category category = new Category();
        category.setTitle(title);
        categoryRepository.save(category);
        return "Категория успешно загружена";
    }
    
    @PutMapping("/{id}")
    public String updateCategory(@PathVariable Integer id, @RequestParam(value = "title", required = false) String title)
    {
        Category existingCategory = categoryRepository.findByTitleIgnoreCase(title);
        if (existingCategory != null) {
            return "Ошибка: категория с названием '" + title + "' уже существует";
        }
        Category category = categoryRepository.findById(id).orElse(null);
        if (category == null)
            return "Такой категории не сущестует";
        category.setTitle(title);
        categoryRepository.save(category);
        return "Категория успешно обновлена";
    }

    @DeleteMapping("/{id}")
    public String deleteCategory(@PathVariable Integer id)
    {
        Category category = categoryRepository.findById(id).orElse(null);
        if (category == null)
        {
            return "Ошибка: категория с ID " + id + " не существует";
        }
        if (id == 1) 
        {
            return "Ошибка: категорию 'По умолчанию' нельзя удалить";
        }
        photoRepository.movePhotosToDefaultCategory(id);
        categoryRepository.deleteById(id);
        return "Категория успешно удалена";
    }
}
