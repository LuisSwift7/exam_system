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
    public ApiResponse<?> uploadImage(@RequestParam("file") MultipartFile file, Authentication authentication) {
        try {
            // 获取当前用户ID
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            Long userId = userPrincipal.getUserId();
            
            // 保存图片
            String baseDir = System.getProperty("user.dir") + "/src/main/resources/static";
            String relativePath = ImageUtils.saveImage(file, baseDir, userId);
            
            // 保存到数据库
            Image image = new Image();
            image.setFileName(relativePath.substring(relativePath.lastIndexOf('/') + 1));
            image.setFilePath(relativePath);
            image.setContentType(file.getContentType());
            image.setFileSize(file.getSize());
            image.setOriginalName(file.getOriginalFilename());
            image.setCreatedTime(LocalDateTime.now());
            image.setCreateBy(userId);
            imageMapper.insert(image);
            
            // 返回图片URL
            String imageUrl = "/api/images/" + image.getId();
            java.util.Map<String, String> result = new java.util.HashMap<>();
            result.put("url", imageUrl);
            return ApiResponse.ok(result);
        } catch (IOException e) {
            return ApiResponse.fail(500, "上传失败: " + e.getMessage());
        }
    }
    
    @GetMapping("/images/{id}")
    public ResponseEntity<Resource> getImage(@PathVariable Long id) {
        try {
            // 从数据库获取图片信息
            Image image = imageMapper.selectById(id);
            if (image == null) {
                return ResponseEntity.notFound().build();
            }
            
            // 构建图片文件路径
            String baseDir = System.getProperty("user.dir") + "/src/main/resources/static";
            Path filePath = Paths.get(baseDir, image.getFilePath());
            
            // 检查文件是否存在
            if (!Files.exists(filePath)) {
                return ResponseEntity.notFound().build();
            }
            
            // 创建文件资源
            Resource resource = new FileSystemResource(filePath);
            
            // 设置响应头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(image.getContentType()));
            headers.setContentDispositionFormData("attachment", image.getFileName());
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
