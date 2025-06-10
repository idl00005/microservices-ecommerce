package com.Recursos;

import com.Entidades.Usuario;
import com.Servicios.AuthService;
import io.quarkus.security.UnauthorizedException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Collections;

@Path("/auth")
@ApplicationScoped
public class AuthResource {

    @Inject
    AuthService authService;

    @POST
    @Path("/login")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response login(UserCredentials credentials) {
        try {
            String token = authService.login(credentials);
            return Response.ok(token).build();
        } catch (NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        } catch (UnauthorizedException e) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }
    }

    @POST
    @Path("/register")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response register(RegisterRequest newUser) {
        try {
            String token = authService.register(newUser);
            return Response.status(Response.Status.CREATED)
                    .entity(Collections.singletonMap("token", token))
                    .build();
        } catch (ConstraintViolationException e) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(e.getConstraintViolations()
                            .stream()
                            .map(ConstraintViolation::getMessage)
                            .reduce((a, b) -> a + ", " + b)
                            .orElse("Validación de usuario fallida."))
                    .build();
        } catch (AuthService.ConflictException e) {
            return Response.status(Response.Status.CONFLICT)
                    .entity("El correo ya está registrado.")
                    .build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Ocurrió un error al registrar el usuario.")
                    .build();
        }
    }

    public record RegisterRequest(String firstName, String lastName, String email, String phone, String password) {}
    public record UserCredentials(String username, String password) {}
}

