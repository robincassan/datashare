package com.datashare;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Service
public class StorageService {

    @Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    @PostConstruct
    public void init() throws IOException {
        Files.createDirectories(Path.of(uploadDir));
    }

    public void save(MultipartFile file, String fileId) throws IOException {
        Path targetPath = Path.of(uploadDir, fileId);
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
    }

    public Path getPath(String fileId) {
        return Path.of(uploadDir, fileId);
    }

    public void delete(String fileId) throws IOException {
        Files.deleteIfExists(Path.of(uploadDir, fileId));
    }
}
