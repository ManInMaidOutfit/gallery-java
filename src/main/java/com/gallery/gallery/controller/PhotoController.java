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
    public Page<Photo> getAllPhotos(@PageableDefault(size = 20, sort = "uploadDate", direction = Sort.Direction.DESC) Pageable pageable, @RequestParam(required = false) List<Integer> categoryIds,
        @RequestParam(required = false) LocalDate dateFrom,
        @RequestParam(required = false) LocalDate dateTo,
        @RequestParam(required = false) String search)
    {
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

        if(title != null)
            photo.setTitle(title);

        if (description != null)
            photo.setDescription(description);
    
        if (categoryId != null)
            photo.setCategoryId(categoryId);
    
        if (isVisible != null)
            photo.setIsVisible(isVisible);

        if(file != null && !file.isEmpty())
        {
            
            try
            {
                Path oldPath = Paths.get(photo.getFilePath());
                if (Files.exists(oldPath)) 
                {
                    try 
                    {
                        Files.delete(oldPath);
                    } catch (IOException e) 
                    {
                        System.err.println("Не удалось удалить старый файл: " + e.getMessage());
                    }
                }
                String uploadDir = "uploads/";
                Path uploadPath = Paths.get(uploadDir);
                if(!Files.exists(uploadPath))
                {
                    Files.createDirectories(uploadPath);
                }

                String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
                Path filePath = uploadPath.resolve(fileName);

                Files.copy(file.getInputStream(), filePath);
                photo.setFilePath(filePath.toString());
                photo.setFileName(fileName);
                photo.setFileSize((int) file.getSize());
                photo.setUploadDate(java.time.LocalDate.now());

            }
            catch(IOException e)
            {
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
    public ResponseEntity<String> getPhotoImageUrl(@PathVariable Integer id) 
    {
        Photo photo = photoRepository.findById(id).orElse(null);
        if (photo == null) 
        {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(photo.getFilePath());
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
