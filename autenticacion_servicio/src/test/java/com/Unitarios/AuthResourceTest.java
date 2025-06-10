package com.Unitarios;

import com.Recursos.AuthResource;
import com.Entidades.Usuario;
import com.Repositorios.RepositorioUsuario;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class AuthResourceTest {

    @InjectMocks
    private AuthResource authResource;

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
        // Arrange
        String username = "test@example.com";
        String password = "password123";
        String encodedPassword = "$2a$10$4fgTal3TLY.CJHMYS.BHJueVlnrkMSJoVl.WNm/AS2SCpmd.g/R0e"; // Mocked hash
        Usuario mockUser = new Usuario("Test", "User", username, "1234567890", encodedPassword, "user");

        // Configurar el mock para devolver el usuario
        when(userRepository.findByUsername(username)).thenReturn(mockUser);
        when(passwordEncoder.matches(password, encodedPassword)).thenReturn(true);

        // Act
        Response response = authResource.login(new AuthResource.UserCredentials(username, password));

        // Assert
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertNotNull(response.getEntity());
        verify(userRepository, times(1)).findByUsername(username);
        verify(passwordEncoder, times(1)).matches(password, encodedPassword);
    }

    @Test
    public void testLoginFailureInvalidPassword() {
        // Arrange
        String username = "test@example.com";
        String password = "wrongPassword";
        Usuario mockUser = new Usuario("Test", "User", username, "1234567890", "$2a$10$encodedPassword", "user");

        when(userRepository.findByUsername(username)).thenReturn(mockUser);
        when(passwordEncoder.matches(password, mockUser.getPassword())).thenReturn(false);

        // Act
        Response response = authResource.login(new AuthResource.UserCredentials(username, password));

        // Assert
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
        verify(userRepository, times(1)).findByUsername(username);
        verify(passwordEncoder, times(1)).matches(password, mockUser.getPassword());
    }

    @Test
    public void testRegisterSuccess() {
        // Arrange
        String email = "newuser@example.com";
        String password = "password123";
        AuthResource.RegisterRequest newUser = new AuthResource.RegisterRequest("New", "User", email, "1234567890", password);

        when(userRepository.findByUsername(email)).thenReturn(null);
        when(passwordEncoder.encode(password)).thenReturn("$2a$10$encodedPassword");

        // Act
        Response response = authResource.register(newUser);

        // Assert
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        assertNotNull(response.getEntity());
        verify(userRepository, times(1)).findByUsername(email);
        verify(passwordEncoder, times(1)).encode(password);
        verify(userRepository, times(1)).save(any(Usuario.class));
    }

    @Test
    public void testRegisterConflict() {
        // Arrange
        String email = "existinguser@example.com";
        AuthResource.RegisterRequest newUser = new AuthResource.RegisterRequest("Existing", "User", email, "1234567890", "password123");

        when(userRepository.findByUsername(email)).thenReturn(new Usuario());

        // Act
        Response response = authResource.register(newUser);

        // Assert
        assertEquals(Response.Status.CONFLICT.getStatusCode(), response.getStatus());
        verify(userRepository, times(1)).findByUsername(email);
        verify(userRepository, never()).persist(any(Usuario.class));
    }
}
