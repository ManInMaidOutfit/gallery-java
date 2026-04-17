package com.gallery.gallery.controller;

import com.gallery.gallery.entity.Photo;
import com.gallery.gallery.repository.PhotoRepository;
import com.gallery.gallery.service.ImgbbService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Map;


@RestController
@RequestMapping("/photos")
public class PhotoController 
{
    @Autowired
    private PhotoRepository photoRepository;

    @Autowired
    private ImgbbService imgbbService;



    @GetMapping
    
    public Page<Photo> getAllPhotos(
        @PageableDefault(size = 20, sort = "uploadDate", direction = Sort.Direction.DESC) Pageable pageable, 
        @RequestParam(required = false) List<Integer> categoryIds,
        @RequestParam(required = false) String categoryId,  // ← Добавьте это
        @RequestParam(required = false) LocalDate dateFrom,
        @RequestParam(required = false) LocalDate dateTo,
        @RequestParam(required = false) String search)
    {
        // Если categoryIds пустой, но categoryId есть
        if ((categoryIds == null || categoryIds.isEmpty()) && categoryId != null && !categoryId.isEmpty()) {
            categoryIds = List.of(Integer.parseInt(categoryId));
        }
        
        if (categoryIds != null || dateFrom != null || dateTo != null || search != null) 
        {
            return photoRepository.findWithFilters(categoryIds, dateFrom, dateTo, search, pageable);
        }
        return photoRepository.findAll(pageable);
    }

    @GetMapping("/")
    public String redirectToIndex() 
    {
        return "forward:/index.html";
    }

    @GetMapping("/{id}")
    public Photo getPhotoById(@PathVariable Integer id)
    {
        Photo photo = photoRepository.findById(id).orElse(null);
        if(photo != null)
        {
            photo.setViews(photo.getViews() + 1);
            photoRepository.save(photo);
        }
        return photo;
    }

    
    @PatchMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
public String updatePhoto(@PathVariable Integer id,
        @RequestParam(value = "title", required = false) String title,
        @RequestParam(value = "description", required = false) String description,
        @RequestParam(value = "categoryId", required = false) Integer categoryId,
        @RequestParam(value = "isVisible", required = false) Boolean isVisible,
        @RequestParam(value = "file", required = false) MultipartFile file)
{
    Photo photo = photoRepository.findById(id).orElse(null);
    if (photo == null)
        return "Фото не найдено";

    if(title != null) photo.setTitle(title);
    if (description != null) photo.setDescription(description);
    if (categoryId != null) photo.setCategoryId(categoryId);
    if (isVisible != null) photo.setIsVisible(isVisible);

    if(file != null && !file.isEmpty()) {
        try {
            // Загружаем новый файл в ImgBB
            String imageUrl = imgbbService.uploadImage(file);
            photo.setFilePath(imageUrl);  // Обновляем URL
            photo.setFileName(file.getOriginalFilename());
            photo.setFileSize((int) file.getSize());
            photo.setUploadDate(java.time.LocalDate.now());
        } catch(Exception e) {
            return "Ошибка загрузки: " + e.getMessage();
        }
    }

    photoRepository.save(photo);
    return "Фото обновлено! ID: " + photo.getId();           
}

    @DeleteMapping("/{id}")
    public void deletePhoto(@PathVariable Integer id)
    {
        photoRepository.deleteById(id);
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String uploadPhoto(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "categoryId", required = false) Integer categoryId,
            @RequestParam(value = "isVisible", defaultValue = "true") Boolean isVisible)
    {
        try {
            // Загружаем фото в ImgBB
            String imageUrl = imgbbService.uploadImage(file);
            
            Photo photo = new Photo();
            photo.setTitle(title);
            photo.setDescription(description);
            photo.setFilePath(imageUrl);  // ← теперь это URL из ImgBB
            photo.setFileName(file.getOriginalFilename());
            photo.setFileSize((int) file.getSize());
            photo.setUploadDate(java.time.LocalDate.now());
            photo.setIsVisible(isVisible);
            photo.setCategoryId(categoryId);
            photo.setViews(0);
            photoRepository.save(photo);
            
            return "Фото загружено! ID: " + photo.getId() + "\nURL: " + imageUrl;
        } catch (Exception e) {
            return "Ошибка загрузки: " + e.getMessage();
        }
    }

    @GetMapping("/image/{id}")
    public ResponseEntity<?> getPhotoImageUrl(@PathVariable Integer id) 
    {
        Photo photo = photoRepository.findById(id).orElse(null);
        if (photo == null) 
        {
            return ResponseEntity.notFound().build();
        }
        // Перенаправляем на реальный URL изображения в ImgBB
        return ResponseEntity.status(302).header("Location", photo.getFilePath()).build();
    }

@PostMapping("/{id}/view")
public ResponseEntity<?> incrementViews(@PathVariable Integer id) {
    Photo photo = photoRepository.findById(id).orElse(null);
    if (photo == null) {
        return ResponseEntity.notFound().build();
    }
    photo.setViews(photo.getViews() + 1);
    photoRepository.save(photo);
    return ResponseEntity.ok().build();
}

@GetMapping("/debug")
public ResponseEntity<Map<String, Object>> debug() {
    Map<String, Object> info = new HashMap<>();
    try {
        long count = photoRepository.count();
        info.put("totalPhotos", count);
        info.put("status", "OK");
        return ResponseEntity.ok(info);
    } catch (Exception e) {
        info.put("status", "ERROR");
        info.put("error", e.getMessage());
        return ResponseEntity.status(500).body(info);
    }
}

}
