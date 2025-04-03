package com.Autenticacion;

import com.Entidades.Usuario;
import com.Repositorios.RepositorioUsuario;
import io.smallrye.jwt.build.Jwt;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.time.Duration;
import java.util.Collections;

@Path("/auth")
public class AuthResource {

    @Inject
    RepositorioUsuario userRepository;

    @POST
    @Path("/login")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response login(UserCredentials credentials) {
        // Buscar usuario en la base de datos
        Usuario user = userRepository.findByUsername(credentials.username());

        // Verificar que el usuario exista y que la contraseña coincida
        if (user != null && user.getPassword().equals(credentials.password())) {
            // Generar el token JWT con el rol extraído de la base de datos
            String token = Jwt.issuer("https://example.com")
                    .subject(user.getCorreo())
                    .claim("roles", Collections.singletonList(user.getRol()))
                    .expiresIn(Duration.ofHours(1))
                    .sign();

            // Retornar el token JWT como respuesta
            return Response.ok(new TokenResponse(token)).build();
        }

        // Si las credenciales son incorrectas
        return Response.status(Response.Status.UNAUTHORIZED).build();
    }

    public record UserCredentials(String username, String password) {}
    public record TokenResponse(String token) {}
}

