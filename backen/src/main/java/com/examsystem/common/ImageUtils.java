package com.examsystem.common;

import net.coobird.thumbnailator.Thumbnails;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class ImageUtils {
    
    public static final String IMAGE_DIR = "images";
    
    public static String saveImage(MultipartFile file, String baseDir, Long userId) throws IOException {
        // 创建目录结构
        String dateDir = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        Path dirPath = Paths.get(baseDir, IMAGE_DIR, dateDir);
        Files.createDirectories(dirPath);
        
        // 生成唯一文件名
        String originalName = file.getOriginalFilename();
        String extension = originalName != null ? originalName.substring(originalName.lastIndexOf('.')) : ".jpg";
        String fileName = UUID.randomUUID().toString() + extension;
        Path filePath = dirPath.resolve(fileName);
        
        // 压缩图片并保存
        File outputFile = filePath.toFile();
        Thumbnails.of(file.getInputStream())
                .size(800, 800)
                .outputQuality(0.8)
                .toFile(outputFile);
        
        // 返回相对路径
        return IMAGE_DIR + "/" + dateDir + "/" + fileName;
    }
    
    public static boolean deleteImage(String relativePath, String baseDir) {
        Path filePath = Paths.get(baseDir, relativePath);
        File file = filePath.toFile();
        return file.exists() && file.delete();
    }
}
