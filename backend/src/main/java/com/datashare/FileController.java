package com.datashare;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file,
                                    @RequestParam(value = "password", required = false) String password,
                                    @RequestParam(value = "expiresAt", required = false) String expiresAt,
                                    HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Non authentifié");
        }

        try {
            FileEntity saved = fileService.upload(file, password, expiresAt, userId);
            String downloadLink = request.getRequestURL().toString().replace("/upload", "/" + saved.getDownloadToken() + "/download");

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "id", saved.getId(),
                    "fileName", saved.getFileName(),
                    "fileSize", saved.getFileSize(),
                    "expiresAt", saved.getExpiresAt().toString() + "Z",
                    "downloadLink", downloadLink
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<?> listFiles(HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Non authentifié");
        }

        List<FileResponse> files = fileService.getUserFiles(userId).stream()
                .map(f -> new FileResponse(
                        f.getId(),
                        f.getFileName(),
                        f.getFileSize(),
                        f.getExpiresAt().toString() + "Z",
                        f.getExpiresAt().isBefore(java.time.LocalDateTime.now()) ? "EXPIRED" : "ACTIVE",
                        f.getDownloadToken()
                ))
                .toList();
        return ResponseEntity.ok(files);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable String id, HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Non authentifié");
        }

        try {
            fileService.deleteFile(id, userId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @GetMapping("/{token}")
    public ResponseEntity<?> getFileInfo(@PathVariable String token) {
        try {
            FileEntity file = fileService.getFileByToken(token);
            return ResponseEntity.ok(Map.of(
                    "fileName", file.getFileName(),
                    "fileSize", file.getFileSize(),
                    "expiresAt", file.getExpiresAt().toString() + "Z",
                    "hasPassword", file.getPassword() != null,
                    "expired", file.getExpiresAt().isBefore(java.time.LocalDateTime.now())
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{token}/download")
    public ResponseEntity<?> downloadFile(@PathVariable String token,
                                          @RequestBody(required = false) Map<String, String> body,
                                          HttpServletRequest request) {
        try {
            String password = (body != null) ? body.get("password") : null;
            Path filePath = fileService.getFilePath(token, password);
            FileEntity file = fileService.getFileByToken(token);

            Resource resource = new UrlResource(filePath.toUri());
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(file.getMimeType() != null ? file.getMimeType() : "application/octet-stream"))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFileName() + "\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

}
