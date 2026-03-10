package com.examsystem.controller;

import com.examsystem.common.ApiResponse;
import com.examsystem.common.ImageUtils;
import com.examsystem.entity.Image;
import com.examsystem.mapper.ImageMapper;
import com.examsystem.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api")
public class ImageController {
    
    @Autowired
    private ImageMapper imageMapper;
    
    @PostMapping("/upload/image")
    public ApiResponse<?> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            // 保存图片
            // Use absolute path for storage
            String uploadDir = "D:/examSystem/uploads";
            java.io.File dir = new java.io.File(uploadDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            
            String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
            String filePath = "/uploads/" + fileName;
            java.io.File dest = new java.io.File(uploadDir + "/" + fileName);
            file.transferTo(dest);
            
            // 保存到数据库
            Image image = new Image();
            image.setName(fileName);
            image.setPath(filePath);
            image.setType(file.getContentType());
            image.setSize(file.getSize());
            image.setCreatedTime(LocalDateTime.now());
            imageMapper.insert(image);
            
            // 返回图片URL
            java.util.Map<String, Object> result = new java.util.HashMap<>();
            result.put("url", "http://localhost:8080/api/images/" + image.getId());
            result.put("id", image.getId());
            return ApiResponse.ok(result);
        } catch (IOException e) {
             return ApiResponse.fail(500, "上传失败: " + e.getMessage());
        }
    }
    
    @GetMapping("/images/{id}")
    public ResponseEntity<Resource> getImage(@PathVariable Long id) {
        try {
            Image image = imageMapper.selectById(id);
            if (image == null) {
                return ResponseEntity.notFound().build();
            }
            
            String filePath = "D:/examSystem" + image.getPath();
            java.io.File file = new java.io.File(filePath);
            
            if (!file.exists()) {
                return ResponseEntity.notFound().build();
            }
            
            Resource resource = new FileSystemResource(file);
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(image.getType()))
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
