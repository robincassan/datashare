package com.datashare;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    private PasswordEncoder passwordEncoder;
    private UserService userService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        userService = new UserService(userRepository, passwordEncoder);
    }

    @Test
    void register_shouldSucceed() {
        when(userRepository.findByEmail("new@test.com")).thenReturn(Optional.empty());
        when(userRepository.save(any())).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId("user-id");
            return u;
        });

        User result = userService.register("new@test.com", "12345678");

        assertNotNull(result);
        assertEquals("new@test.com", result.getEmail());
        assertTrue(passwordEncoder.matches("12345678", result.getPassword()));
        verify(userRepository).save(any());
    }

    @Test
    void register_shouldThrowWhenEmailExists() {
        when(userRepository.findByEmail("existing@test.com")).thenReturn(Optional.of(new User()));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> userService.register("existing@test.com", "12345678"));
        assertEquals("Email déjà utilisé", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void login_shouldSucceed() {
        User user = new User();
        user.setEmail("test@test.com");
        user.setPassword(passwordEncoder.encode("12345678"));
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));

        User result = userService.login("test@test.com", "12345678");

        assertNotNull(result);
        assertEquals("test@test.com", result.getEmail());
    }

    @Test
    void login_shouldThrowWhenEmailNotFound() {
        when(userRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> userService.login("unknown@test.com", "12345678"));
        assertEquals("Email ou mot de passe incorrect", ex.getMessage());
    }

    @Test
    void login_shouldThrowWhenWrongPassword() {
        User user = new User();
        user.setEmail("test@test.com");
        user.setPassword(passwordEncoder.encode("12345678"));
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> userService.login("test@test.com", "wrong"));
        assertEquals("Email ou mot de passe incorrect", ex.getMessage());
    }
}
