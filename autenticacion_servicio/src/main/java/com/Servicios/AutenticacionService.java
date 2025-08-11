package com.Servicios;

import com.Entidades.Usuario;
import com.Recursos.AutenticacionResource;
import com.Repositorios.RepositorioUsuario;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.time.Duration;
import java.util.Collections;
import java.util.Set;

@ApplicationScoped
public class AutenticacionService {

    @Inject
    RepositorioUsuario userRepository;

    @Inject
    BCryptPasswordEncoder passwordEncoder;

    public String login(AutenticacionResource.UserCredentials credentials) {
        // Buscar usuario en la base de datos
        Usuario user = userRepository.findByUsername(credentials.username());

        // Verificar que el usuario exista
        if (user == null) {
            throw new NotFoundException("Usuario no encontrado");
        }

        // Verificar que las contraseñas coincidan
        if (passwordEncoder.matches(credentials.password(), user.getPassword())) {
            // Si coinciden, generar el token JWT
            return Jwt.issuer("https://example.com")
                    .subject(user.getCorreo())
                    .claim("roles", Collections.singletonList(user.getRol()))
                    .expiresIn(Duration.ofHours(1))
                    .sign();
        } else {
            throw new UnauthorizedException("Credenciales inválidas");
        }
    }

    public String register(AutenticacionResource.RegisterRequest newUser) {
        // Validar si el correo ya está registrado
        if (userRepository.findByUsername(newUser.email()) != null) {
            throw new ConflictException("El correo ya está registrado");
        }

        // Convertir RegisterRequest en un objeto Usuario
        Usuario usuario = new Usuario(
                newUser.firstName(),
                newUser.lastName(),
                newUser.email(),
                newUser.phone(),
                newUser.password(),
                "user" // Asignar rol por defecto
        );

        // Validar las restricciones de la clase Usuario
        validateUsuario(usuario);

        // Encriptar la contraseña
        usuario.setPassword(passwordEncoder.encode(newUser.password()));

        // Guardar en la base de datos
        userRepository.save(usuario);

        // Generar el token JWT
        return Jwt.issuer("https://example.com")
                .subject(usuario.getCorreo())
                .claim("roles", Collections.singletonList(usuario.getRol()))
                .expiresIn(Duration.ofHours(1))
                .sign();
    }

    private void validateUsuario(Usuario usuario) {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        Set<ConstraintViolation<Usuario>> violations = validator.validate(usuario);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }

    public static class NotFoundException extends RuntimeException {
        public NotFoundException(String message) {
            super(message);
        }
    }

    public static class UnauthorizedException extends RuntimeException {
        public UnauthorizedException(String message) {
            super(message);
        }
    }

    public static class ConflictException extends RuntimeException {
        public ConflictException(String message) {
            super(message);
        }
    }
}