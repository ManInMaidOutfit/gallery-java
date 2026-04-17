package com.gallery.gallery.repository;

import com.gallery.gallery.entity.Photo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;


@Repository
public interface PhotoRepository extends JpaRepository<Photo, Integer> 
{
        @Query("SELECT p FROM Photo p WHERE " +
       "(:categoryIds IS NULL OR p.categoryId IN :categoryIds) AND " +
       "(:dateFrom IS NULL OR p.uploadDate >= :dateFrom) AND " +
       "(:dateTo IS NULL OR p.uploadDate <= :dateTo) AND " +
       "(:search IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', :search, '%')))")
        Page<Photo> findWithFilters(@Param ("categoryIds") List<Integer> categoryIds, @Param("dateFrom") LocalDate dateFrom, @Param("dateTo") LocalDate dateTo,@Param("search") String search, Pageable pageable);

        @Modifying
    @Transactional
    @Query("UPDATE Photo p SET p.categoryId = 1 WHERE p.categoryId = :categoryId")
    void movePhotosToDefaultCategory(@Param("categoryId") Integer categoryId);
    Page<Photo> findByCategoryId(Integer categoryId, Pageable pageable);
}
