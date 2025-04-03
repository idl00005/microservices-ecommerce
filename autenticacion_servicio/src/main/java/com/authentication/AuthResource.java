package com.authentication;

import io.smallrye.jwt.build.Jwt;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.time.Duration;
import java.util.Set;

@Path("/auth")
public class AuthResource {

    @POST
    @Path("/login")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response login(UserCredentials credentials) {
        // Simulación de autenticación
        if ("user".equals(credentials.username()) && "password".equals(credentials.password())) {
            String token = Jwt.issuer("https://example.com")
                    .subject(credentials.username())
                    .claim("roles", Set.of("user"))  // Asigna el rol "user"
                    .expiresIn(Duration.ofHours(1))
                    .sign();
            return Response.ok(new TokenResponse(token)).build();
        }
        return Response.status(Response.Status.UNAUTHORIZED).build();
    }

    public record UserCredentials(String username, String password) {}
    public record TokenResponse(String token) {}
}
