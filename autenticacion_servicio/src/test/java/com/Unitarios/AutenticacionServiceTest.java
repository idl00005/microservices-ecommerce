package com.Unitarios;

import com.Entidades.Usuario;
import com.Recursos.AutenticacionResource;
import com.Repositorios.RepositorioUsuario;
import com.Servicios.AutenticacionService;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class AutenticacionServiceTest {

    @InjectMocks
    private AutenticacionService autenticacionService;

    @Mock
    private RepositorioUsuario userRepository;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testLoginSuccess() {
        String username = "test@example.com";
        String password = "password123";
        String encodedPassword = "$2a$10$4fgTal3TLY.CJHMYS.BHJueVlnrkMSJoVl.WNm/AS2SCpmd.g/R0e"; // Hash simulado
        Usuario mockUser = new Usuario("Test", "User", username, "1234567890", encodedPassword, "user");

        when(userRepository.findByUsername(username)).thenReturn(mockUser);
        when(passwordEncoder.matches(password, encodedPassword)).thenReturn(true);

        String token = autenticacionService.login(new AutenticacionResource.UserCredentials(username, password));

        assertNotNull(token);
        verify(userRepository, times(1)).findByUsername(username);
        verify(passwordEncoder, times(1)).matches(password, encodedPassword);
    }

    @Test
    public void testLoginFailureInvalidPassword() {
        String username = "test@example.com";
        String password = "wrongPassword";
        Usuario mockUser = new Usuario("Test", "User", username, "1234567890", "$2a$10$encodedPassword", "user");

        when(userRepository.findByUsername(username)).thenReturn(mockUser);
        when(passwordEncoder.matches(password, mockUser.getPassword())).thenReturn(false);

        assertThrows(AutenticacionService.UnauthorizedException.class, () -> {
            autenticacionService.login(new AutenticacionResource.UserCredentials(username, password));
        });
        verify(userRepository, times(1)).findByUsername(username);
        verify(passwordEncoder, times(1)).matches(password, mockUser.getPassword());
    }

    @Test
    public void testLoginFailureUserNotFound() {
        String username = "nonexistent@example.com";
        String password = "password123";

        when(userRepository.findByUsername(username)).thenReturn(null);

        assertThrows(AutenticacionService.NotFoundException.class, () -> {
            autenticacionService.login(new AutenticacionResource.UserCredentials(username, password));
        });
        verify(userRepository, times(1)).findByUsername(username);
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    public void testRegisterSuccess() {
        String email = "newuser@example.com";
        String password = "password123";
        AutenticacionResource.RegisterRequest newUser = new AutenticacionResource.RegisterRequest("New", "User", email, "1234567890", password);

        when(userRepository.findByUsername(email)).thenReturn(null);
        when(passwordEncoder.encode(password)).thenReturn("$2a$10$encodedPassword");

        String token = autenticacionService.register(newUser);

        assertNotNull(token);
        verify(userRepository, times(1)).findByUsername(email);
        verify(passwordEncoder, times(1)).encode(password);
        verify(userRepository, times(1)).save(any(Usuario.class));
    }

    @Test
    public void testRegisterConflict() {
        String email = "existinguser@example.com";
        AutenticacionResource.RegisterRequest newUser = new AutenticacionResource.RegisterRequest("Existing", "User", email, "1234567890", "password123");

        when(userRepository.findByUsername(email)).thenReturn(new Usuario());

        assertThrows(AutenticacionService.ConflictException.class, () -> {
            autenticacionService.register(newUser);
        });
        verify(userRepository, times(1)).findByUsername(email);
        verify(userRepository, never()).save(any(Usuario.class));
    }

    @Test
    public void testRegisterValidationFailure() {
        String email = "invalidemail";
        String password = "password123";
        AutenticacionResource.RegisterRequest newUser = new AutenticacionResource.RegisterRequest("", "User", email, "1234567890", password); // Nombre vacío

        when(userRepository.findByUsername(email)).thenReturn(null);

        assertThrows(ConstraintViolationException.class, () -> {
            autenticacionService.register(newUser);
        });
        verify(userRepository, times(1)).findByUsername(email);
        verify(userRepository, never()).save(any(Usuario.class));
    }
}
