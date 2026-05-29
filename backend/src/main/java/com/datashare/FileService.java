package com.datashare;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class FileService {

    private final FileRepository fileRepository;
    private final StorageService storageService;
    private final PasswordEncoder passwordEncoder;

    private static final long MAX_FILE_SIZE = 1024L * 1024L * 1024L; // 1 Go
    private static final long MAX_EXPIRY_DAYS = 7;

    public FileService(FileRepository fileRepository, StorageService storageService, PasswordEncoder passwordEncoder) {
        this.fileRepository = fileRepository;
        this.storageService = storageService;
        this.passwordEncoder = passwordEncoder;
    }

    public FileEntity upload(MultipartFile file, String password, String expiresAtStr, String userId) {
        if (file.isEmpty()) {
            throw new RuntimeException("Fichier vide");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new RuntimeException("Fichier trop volumineux (max 1 Go)");
        }

        String passwordHash = null;
        if (password != null && !password.isBlank()) {
            if (password.length() < 6) {
                throw new RuntimeException("Mot de passe trop court (min 6 caractères)");
            }
            passwordHash = passwordEncoder.encode(password);
        }

        LocalDateTime expiresAt = parseExpiresAt(expiresAtStr);

        FileEntity fileEntity = new FileEntity();
        fileEntity.setFileName(file.getOriginalFilename());
        fileEntity.setFileSize(file.getSize());
        fileEntity.setMimeType(file.getContentType());
        fileEntity.setPassword(passwordHash);
        fileEntity.setExpiresAt(expiresAt);
        fileEntity.setDownloadToken(UUID.randomUUID().toString());
        fileEntity.setUserId(userId);

        fileEntity = fileRepository.save(fileEntity);

        try {
            storageService.save(file, fileEntity.getId());
        } catch (IOException e) {
            fileRepository.delete(fileEntity);
            throw new RuntimeException("Erreur lors du stockage du fichier", e);
        }

        return fileEntity;
    }

    private LocalDateTime parseExpiresAt(String str) {
        if (str == null || str.isBlank()) {
            return LocalDateTime.now().plusDays(MAX_EXPIRY_DAYS);
        }
        String cleaned = str.replace("Z", "").replace("z", "");
        if (cleaned.length() <= 10) {
            return LocalDate.parse(cleaned).atStartOfDay();
        }
        LocalDateTime parsed = LocalDateTime.parse(cleaned);
        LocalDateTime now = LocalDateTime.now();
        if (parsed.isBefore(now)) {
            throw new RuntimeException("La date d'expiration doit être dans le futur");
        }
        LocalDateTime maxExpiry = now.plusDays(MAX_EXPIRY_DAYS);
        if (parsed.isAfter(maxExpiry)) {
            throw new RuntimeException("Date d'expiration trop lointaine (max 7 jours)");
        }
        return parsed;
    }

    public List<FileEntity> getUserFiles(String userId) {
        return fileRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public void deleteFile(String fileId, String userId) {
        FileEntity file = fileRepository.findByIdAndUserId(fileId, userId)
                .orElseThrow(() -> new RuntimeException("Fichier introuvable"));
        try {
            storageService.delete(file.getId());
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors de la suppression du fichier");
        }
        fileRepository.delete(file);
    }

    private void checkExpired(FileEntity file) {
        if (file.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Ce fichier a expiré");
        }
    }

    public FileEntity getFileByToken(String token) {
        FileEntity file = fileRepository.findByDownloadToken(token)
                .orElseThrow(() -> new RuntimeException("Fichier introuvable"));
        checkExpired(file);
        return file;
    }

    public Path getFilePath(String token, String password) {
        FileEntity file = fileRepository.findByDownloadToken(token)
                .orElseThrow(() -> new RuntimeException("Fichier introuvable"));
        checkExpired(file);

        if (file.getPassword() != null) {
            if (password == null || !passwordEncoder.matches(password, file.getPassword())) {
                throw new RuntimeException("Mot de passe incorrect");
            }
        }

        return storageService.getPath(file.getId());
    }
}
