package com.gallery.gallery.repository;

import com.gallery.gallery.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Integer>
{
    Category findByTitleIgnoreCase(String title);
    
    @Query("SELECT COUNT(p) FROM Photo p WHERE p.categoryId = :categoryId")
    long countPhotosByCategoryId(@Param("categoryId") Integer categoryId);
}
