package com.lifeshare.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private static final String UPLOAD_DIR = "uploads";
    private static final String ALLOWED_IMAGE_TYPES = "image/jpeg,image/png,image/gif,image/webp";
    private static final String ALLOWED_VIDEO_TYPES = "video/mp4,video/webm,video/ogg";
    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024; // 5MB
    private static final long MAX_VIDEO_SIZE = 100 * 1024 * 1024; // 100MB

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file, 
                                      @RequestParam(value = "type", required = false) String type, 
                                      @RequestParam(value = "id", required = false) Long id) {
        // 检查文件是否为空
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("文件不能为空");
        }

        // 获取文件类型
        String contentType = file.getContentType();
        boolean isImage = ALLOWED_IMAGE_TYPES.contains(contentType);
        boolean isVideo = ALLOWED_VIDEO_TYPES.contains(contentType);

        // 检查文件类型
        if (!isImage && !isVideo) {
            return ResponseEntity.badRequest().body("只允许上传图片文件(jpeg, png, gif, webp)或视频文件(mp4, webm, ogg)");
        }

        // 检查文件大小
        if (isImage && file.getSize() > MAX_IMAGE_SIZE) {
            return ResponseEntity.badRequest().body("图片文件大小不能超过5MB");
        }
        if (isVideo && file.getSize() > MAX_VIDEO_SIZE) {
            return ResponseEntity.badRequest().body("视频文件大小不能超过100MB");
        }

        try {
            // 构建存储目录路径
            String uploadPath;
            String fileUrl;
            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            
            if (isImage) {
                // 图片存储逻辑
                if ("user".equals(type) && id != null) {
                    // 用户头像
                    uploadPath = UPLOAD_DIR + File.separator + "images" + File.separator + "user" + File.separator + id;
                    String filename = "avatar" + extension;
                    fileUrl = "/uploads/images/user/" + id + "/" + filename;
                    
                    // 创建目录
                    File uploadDir = new File(uploadPath);
                    if (!uploadDir.exists()) {
                        uploadDir.mkdirs();
                    }
                    
                    // 保存文件
                    Path path = Paths.get(uploadPath + File.separator + filename);
                    Files.write(path, file.getBytes());
                } else if ("post".equals(type) && id != null) {
                    // 帖子图片
                    uploadPath = UPLOAD_DIR + File.separator + "images" + File.separator + "post" + File.separator + id;
                    
                    // 创建目录
                    File uploadDir = new File(uploadPath);
                    if (!uploadDir.exists()) {
                        uploadDir.mkdirs();
                    }
                    
                    // 计算自增编号
                    File[] existingFiles = uploadDir.listFiles((dir, name) -> name.matches("image_\\d+\\..*"));
                    int nextNumber = existingFiles != null ? existingFiles.length + 1 : 1;
                    String filename = "image_" + nextNumber + extension;
                    fileUrl = "/uploads/images/post/" + id + "/" + filename;
                    
                    // 保存文件
                    Path path = Paths.get(uploadPath + File.separator + filename);
                    Files.write(path, file.getBytes());
                } else {
                    return ResponseEntity.badRequest().body("图片上传需要指定类型(type)和ID");
                }
            } else {
                // 视频存储逻辑
                if ("post".equals(type) && id != null) {
                    // 帖子视频
                    uploadPath = UPLOAD_DIR + File.separator + "videos" + File.separator + "post" + File.separator + id;
                    
                    // 创建目录
                    File uploadDir = new File(uploadPath);
                    if (!uploadDir.exists()) {
                        uploadDir.mkdirs();
                    }
                    
                    // 计算自增编号
                    File[] existingFiles = uploadDir.listFiles((dir, name) -> name.matches("video_\\d+\\..*"));
                    int nextNumber = existingFiles != null ? existingFiles.length + 1 : 1;
                    String filename = "video_" + nextNumber + extension;
                    fileUrl = "/uploads/videos/post/" + id + "/" + filename;
                    
                    // 保存文件
                    Path path = Paths.get(uploadPath + File.separator + filename);
                    Files.write(path, file.getBytes());
                } else {
                    return ResponseEntity.badRequest().body("视频上传需要指定类型为'post'和帖子ID");
                }
            }

            return ResponseEntity.ok().body("{\"url\":\"" + fileUrl + "\"}");
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("文件上传失败");
        }
    }
}
