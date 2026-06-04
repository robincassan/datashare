package com.datashare;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtService jwtService;

    private User createUser(String email) {
        User u = new User();
        u.setId(java.util.UUID.randomUUID().toString());
        u.setEmail(email);
        u.setPassword("hashed");
        return u;
    }

    @Test
    void register_shouldReturn201() throws Exception {
        User user = createUser("new@test.com");
        when(userService.register("new@test.com", "12345678")).thenReturn(user);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"new@test.com\",\"password\":\"12345678\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("new@test.com"));
    }

    @Test
    void register_shouldReturn400WhenEmailInvalid() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"invalid\",\"password\":\"12345678\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_shouldReturn400WhenPasswordTooShort() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"test@test.com\",\"password\":\"12345\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_shouldReturn409WhenEmailExists() throws Exception {
        when(userService.register(anyString(), anyString()))
                .thenThrow(new RuntimeException("Email déjà utilisé"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"existing@test.com\",\"password\":\"12345678\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void login_shouldReturnToken() throws Exception {
        User user = createUser("test@test.com");
        when(userService.login("test@test.com", "12345678")).thenReturn(user);
        when(jwtService.generateToken(user.getId())).thenReturn("jwt-token");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"test@test.com\",\"password\":\"12345678\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"));
    }

    @Test
    void login_shouldReturn401WhenInvalid() throws Exception {
        when(userService.login("test@test.com", "wrong"))
                .thenThrow(new RuntimeException("Email ou mot de passe incorrect"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"test@test.com\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized());
    }
}
