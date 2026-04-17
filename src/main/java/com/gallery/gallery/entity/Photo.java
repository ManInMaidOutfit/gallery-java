package com.gallery.gallery.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "\"Photos\"")
public class Photo
{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "Title", columnDefinition = "TEXT")
    private String title;

    @Column(name = "Description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "\"FilePath\"", columnDefinition = "TEXT")
    private String filePath;
    
    @Column(name = "\"FileName\"", columnDefinition = "TEXT")
    private String fileName;
    
    @Column(name = "\"FileSize\"")
    private Integer fileSize;
    
    @Column(name = "Views")
    private Integer views = 0;
    
    @Column(name = "\"UploadDate\"")
    private LocalDate uploadDate;
    
    @Column(name = "\"IsVisible\"")
    private Boolean isVisible = true;

    @Column(name = "\"CategoryId\"")
    private Integer categoryId;

    public Photo() {}

    public Photo(String title, String filePath)
    {
        this.title = title;
        this.filePath = filePath;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public Integer getViews() { return views; }
    public void setViews(Integer views) { this.views = views; }

    public LocalDate getUploadDate() { return uploadDate; }
    public void setUploadDate(LocalDate createdAt) { this.uploadDate = createdAt; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public Integer getFileSize() { return fileSize; }
    public void setFileSize(Integer fileSize) { this.fileSize = fileSize; }

    public Integer getCategoryId() { return categoryId; }
    public void setCategoryId(Integer categoryId) { this.categoryId = categoryId; }

    public Boolean getIsVisible() { return isVisible; }
    public void setIsVisible(Boolean isVisible) { this.isVisible = isVisible; }
}


