package com.datashare;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {

    @Mock
    private FileRepository fileRepository;

    @Mock
    private StorageService storageService;

    private PasswordEncoder passwordEncoder;
    private FileService fileService;

    @TempDir
    Path tempDir;

    private final String userId = UUID.randomUUID().toString();
    private FileEntity sampleFile;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        fileService = new FileService(fileRepository, storageService, passwordEncoder);

        sampleFile = new FileEntity();
        sampleFile.setId(UUID.randomUUID().toString());
        sampleFile.setFileName("test.txt");
        sampleFile.setFileSize(1024L);
        sampleFile.setMimeType("text/plain");
        sampleFile.setExpiresAt(LocalDateTime.now().plusDays(5));
        sampleFile.setDownloadToken(UUID.randomUUID().toString());
        sampleFile.setUserId(userId);
        sampleFile.setPassword(null);
    }

    @Test
    void upload_shouldSucceed() throws Exception {
        MultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "hello".getBytes());
        when(fileRepository.save(any())).thenReturn(sampleFile);
        doNothing().when(storageService).save(any(), anyString());

        FileEntity result = fileService.upload(file, null, null, userId);

        assertNotNull(result);
        assertEquals("test.txt", result.getFileName());
        verify(fileRepository).save(any());
        verify(storageService).save(any(), anyString());
    }

    @Test
    void upload_shouldRejectEmptyFile() {
        MultipartFile file = new MockMultipartFile("file", "empty.txt", "text/plain", new byte[0]);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> fileService.upload(file, null, null, userId));
        assertEquals("Fichier vide", ex.getMessage());
    }

    @Test
    void upload_shouldRejectOversizedFile() {
        byte[] huge = new byte[1025 * 1024 * 1024];
        MultipartFile file = new MockMultipartFile("file", "huge.bin", "application/octet-stream", huge);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> fileService.upload(file, null, null, userId));
        assertEquals("Fichier trop volumineux (max 1 Go)", ex.getMessage());
    }

    @Test
    void upload_shouldRejectShortPassword() {
        MultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "data".getBytes());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> fileService.upload(file, "abc", null, userId));
        assertEquals("Mot de passe trop court (min 6 caractères)", ex.getMessage());
    }

    @Test
    void upload_shouldRejectPastExpirationDate() {
        MultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "data".getBytes());
        String pastDate = LocalDateTime.now().minusDays(1).toString();

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> fileService.upload(file, null, pastDate, userId));
        assertEquals("La date d'expiration doit être dans le futur", ex.getMessage());
    }

    @Test
    void upload_shouldRejectTooFarExpirationDate() {
        MultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "data".getBytes());
        String futureDate = LocalDateTime.now().plusDays(10).toString();

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> fileService.upload(file, null, futureDate, userId));
        assertEquals("Date d'expiration trop lointaine (max 7 jours)", ex.getMessage());
    }

    @Test
    void getFileByToken_shouldReturnFile() {
        when(fileRepository.findByDownloadToken("valid-token")).thenReturn(Optional.of(sampleFile));

        FileEntity result = fileService.getFileByToken("valid-token");

        assertNotNull(result);
        assertEquals("test.txt", result.getFileName());
    }

    @Test
    void getFileByToken_shouldThrowWhenNotFound() {
        when(fileRepository.findByDownloadToken("invalid")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> fileService.getFileByToken("invalid"));
    }

    @Test
    void getFileByToken_shouldThrowWhenExpired() {
        sampleFile.setExpiresAt(LocalDateTime.now().minusDays(1));
        when(fileRepository.findByDownloadToken("expired")).thenReturn(Optional.of(sampleFile));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> fileService.getFileByToken("expired"));
        assertEquals("Ce fichier a expiré", ex.getMessage());
    }

    @Test
    void getFilePath_shouldSucceedWithoutPassword() {
        when(fileRepository.findByDownloadToken("token")).thenReturn(Optional.of(sampleFile));
        when(storageService.getPath(sampleFile.getId())).thenReturn(tempDir.resolve("file"));

        Path result = fileService.getFilePath("token", null);

        assertNotNull(result);
    }

    @Test
    void getFilePath_shouldRejectWrongPassword() {
        sampleFile.setPassword(passwordEncoder.encode("secret123"));
        when(fileRepository.findByDownloadToken("token")).thenReturn(Optional.of(sampleFile));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> fileService.getFilePath("token", "wrong"));
        assertEquals("Mot de passe incorrect", ex.getMessage());
    }

    @Test
    void getFilePath_shouldSucceedWithCorrectPassword() {
        sampleFile.setPassword(passwordEncoder.encode("secret123"));
        when(fileRepository.findByDownloadToken("token")).thenReturn(Optional.of(sampleFile));
        when(storageService.getPath(sampleFile.getId())).thenReturn(tempDir.resolve("file"));

        Path result = fileService.getFilePath("token", "secret123");

        assertNotNull(result);
    }

    @Test
    void deleteFile_shouldDelete() throws IOException {
        String fileId = sampleFile.getId();
        when(fileRepository.findByIdAndUserId(fileId, userId)).thenReturn(Optional.of(sampleFile));
        doNothing().when(storageService).delete(fileId);

        fileService.deleteFile(fileId, userId);

        verify(fileRepository).delete(sampleFile);
        verify(storageService).delete(fileId);
    }

    @Test
    void deleteFile_shouldThrowWhenNotFound() {
        when(fileRepository.findByIdAndUserId("unknown", userId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> fileService.deleteFile("unknown", userId));
    }

    @Test
    void getUserFiles_shouldReturnUserFiles() {
        when(fileRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(List.of(sampleFile));

        List<FileEntity> files = fileService.getUserFiles(userId);

        assertEquals(1, files.size());
        assertEquals("test.txt", files.getFirst().getFileName());
    }
}
