package com.datashare;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class FileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FileService fileService;

    private FileEntity createSampleFile() {
        FileEntity f = new FileEntity();
        f.setId(UUID.randomUUID().toString());
        f.setFileName("test.txt");
        f.setFileSize(1024L);
        f.setMimeType("text/plain");
        f.setExpiresAt(LocalDateTime.now().plusDays(5));
        f.setDownloadToken("token-123");
        f.setUserId("user-1");
        f.setPassword(null);
        return f;
    }

    @Test
    void upload_shouldReturn201() throws Exception {
        FileEntity file = createSampleFile();
        when(fileService.upload(any(), any(), any(), any())).thenReturn(file);

        MockMultipartFile multipart = new MockMultipartFile("file", "test.txt", "text/plain", "hello".getBytes());

        mockMvc.perform(multipart("/api/files/upload")
                        .file(multipart)
                        .with(user("user-1"))
                        .requestAttr("userId", "user-1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fileName").value("test.txt"));
    }

    @Test
    void upload_shouldReturn403WhenNotAuthenticated() throws Exception {
        MockMultipartFile multipart = new MockMultipartFile("file", "test.txt", "text/plain", "hello".getBytes());

        mockMvc.perform(multipart("/api/files/upload")
                        .file(multipart))
                .andExpect(status().isForbidden());
    }

    @Test
    void upload_shouldReturn400WhenServiceThrows() throws Exception {
        when(fileService.upload(any(), any(), any(), any()))
                .thenThrow(new RuntimeException("Fichier vide"));

        MockMultipartFile multipart = new MockMultipartFile("file", "empty.txt", "text/plain", new byte[0]);

        mockMvc.perform(multipart("/api/files/upload")
                        .file(multipart)
                        .with(user("user-1"))
                        .requestAttr("userId", "user-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listFiles_shouldReturn200() throws Exception {
        FileEntity file = createSampleFile();
        when(fileService.getUserFiles("user-1")).thenReturn(List.of(file));

        mockMvc.perform(get("/api/files")
                        .with(user("user-1"))
                        .requestAttr("userId", "user-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fileName").value("test.txt"))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));
    }

    @Test
    void listFiles_shouldReturn403WhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/files"))
                .andExpect(status().isForbidden());
    }

    @Test
    void delete_shouldReturn204() throws Exception {
        doNothing().when(fileService).deleteFile("file-1", "user-1");

        mockMvc.perform(delete("/api/files/file-1")
                        .with(user("user-1"))
                        .requestAttr("userId", "user-1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_shouldReturn404WhenNotFound() throws Exception {
        doThrow(new RuntimeException("Fichier introuvable"))
                .when(fileService).deleteFile("unknown", "user-1");

        mockMvc.perform(delete("/api/files/unknown")
                        .with(user("user-1"))
                        .requestAttr("userId", "user-1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getFileInfo_shouldReturn200() throws Exception {
        FileEntity file = createSampleFile();
        when(fileService.getFileByToken("token-123")).thenReturn(file);

        mockMvc.perform(get("/api/files/token-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fileName").value("test.txt"))
                .andExpect(jsonPath("$.hasPassword").value(false));
    }

    @Test
    void getFileInfo_shouldReturn404WhenNotFound() throws Exception {
        when(fileService.getFileByToken("invalid")).thenThrow(new RuntimeException("Not found"));

        mockMvc.perform(get("/api/files/invalid"))
                .andExpect(status().isNotFound());
    }

    @Test
    void download_shouldReturn400WhenWrongPassword() throws Exception {
        when(fileService.getFilePath("token-123", "wrong"))
                .thenThrow(new RuntimeException("Mot de passe incorrect"));

        mockMvc.perform(post("/api/files/token-123/download")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"wrong\"}"))
                .andExpect(status().isBadRequest());
    }
}
