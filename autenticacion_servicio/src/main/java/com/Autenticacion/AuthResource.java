package com.Autenticacion;

import com.Entidades.Usuario;
import com.Repositorios.RepositorioUsuario;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.*;
import jakarta.ws.rs.*;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Duration;
import java.util.Collections;
import java.util.Set;

@Path("/auth")
@ApplicationScoped
public class AuthResource {

    @Inject
    RepositorioUsuario userRepository;
    @Inject
    BCryptPasswordEncoder passwordEncoder;

    private static final Logger logger = Logger.getLogger(AuthResource.class);

    @POST
    @Path("/login")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response login(UserCredentials credentials) {
        // Buscar usuario en la base de datos
        Usuario user = userRepository.findByUsername(credentials.username());

        // Verificar que el usuario exista
        if (user == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        // Verificar que las contraseñas coincidan
        if (passwordEncoder.matches(credentials.password(), user.getPassword())) {
            // Si coinciden, generar el token JWT
            String token = Jwt.issuer("https://example.com")
                    .subject(user.getCorreo())
                    .claim("roles", Collections.singletonList(user.getRol()))
                    .expiresIn(Duration.ofHours(1))
                    .sign();

            return Response.ok(token).build();
        } else {
            // Si no coinciden, retornar error
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
    }

    @POST
    @Path("/register")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response register(@Valid RegisterRequest newUser) {
        try {
            // Validar si el correo ya está registrado
            if (userRepository.findByUsername(newUser.email()) != null) {
                return Response.status(Response.Status.CONFLICT)
                        .entity("El correo ya está registrado.")
                        .build();
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

            String token = Jwt.issuer("https://example.com")
                    .subject(usuario.getCorreo())
                    .claim("roles", Collections.singletonList(usuario.getRol()))
                    .expiresIn(Duration.ofHours(1))
                    .sign();
            return Response.status(Response.Status.CREATED)
                    .entity(Collections.singletonMap("token", token)) // Retornar el token como JSON
                    .build();
        } catch (ConstraintViolationException e) {
            // Si hay problemas de validación, devolver error 400
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getConstraintViolations()
                            .stream()
                            .map(violation -> violation.getMessage()) // Obtenemos solo los mensajes de error
                            .reduce((a, b) -> a + ", " + b) // Combinar todos los errores en un string
                            .orElse("Validación de usuario fallida."))
                    .build();
        } catch (Exception e) {
            logger.error("Error durante el registro del usuario", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Ocurrió un error al registrar el usuario.")
                    .build();
        }
    }

    private void validateUsuario(Usuario usuario) {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();

        // Validamos el objeto Usuario
        Set<ConstraintViolation<Usuario>> violations = validator.validate(usuario);

        // Si hay violaciones a las restricciones, lanzamos una excepción
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }

    public record RegisterRequest(String firstName, String lastName, String email, String phone, String password) {}
    public record UserCredentials(String username, String password) {}
}

