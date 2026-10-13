package com.example.sd009thuan.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {
    private final Path uploadRoot;

    public FileStorageService(@Value("${app.upload-dir:uploads}") String uploadDir) {
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    public String storeEmployeeImage(MultipartFile file) {
        if (file == null || file.isEmpty()) return null;
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("File tải lên phải là ảnh");
        }
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("Ảnh không được vượt quá 5MB");
        }

        String original = StringUtils.cleanPath(file.getOriginalFilename() == null ? "image" : file.getOriginalFilename());
        String extension = "";
        int dot = original.lastIndexOf('.');
        if (dot >= 0) extension = original.substring(dot).toLowerCase();
        if (!extension.matches("\\.(jpg|jpeg|png|gif|webp)$")) {
            throw new IllegalArgumentException("Chỉ chấp nhận JPG, JPEG, PNG, GIF hoặc WEBP");
        }

        try {
            Path directory = uploadRoot.resolve("nhan-vien").normalize();
            Files.createDirectories(directory);
            String filename = UUID.randomUUID() + extension;
            Path target = directory.resolve(filename).normalize();
            if (!target.startsWith(directory)) throw new IllegalArgumentException("Tên file không hợp lệ");
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return "/uploads/nhan-vien/" + filename;
        } catch (IOException e) {
            throw new IllegalStateException("Không thể lưu ảnh nhân viên", e);
        }
    }

    public void deleteIfLocal(String url) {
        if (url == null || !url.startsWith("/uploads/nhan-vien/")) return;
        try {
            String relative = url.substring("/uploads/".length());
            Path file = uploadRoot.resolve(relative).normalize();
            if (file.startsWith(uploadRoot)) Files.deleteIfExists(file);
        } catch (IOException ignored) {
        }
    }
}
