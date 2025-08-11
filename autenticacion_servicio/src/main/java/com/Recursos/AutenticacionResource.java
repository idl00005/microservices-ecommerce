package com.Recursos;

import com.Servicios.AutenticacionService;
import io.quarkus.security.UnauthorizedException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.faulttolerance.Timeout;

import java.time.temporal.ChronoUnit;
import java.util.Collections;

@Path("/autenticacion")
@ApplicationScoped
public class AutenticacionResource {

    @Inject
    AutenticacionService autenticacionService;

    @POST
    @Path("/login")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Timeout(value = 3, unit = ChronoUnit.SECONDS)
    public Response login(@Valid UserCredentials credentials) {
        try {
            String token = autenticacionService.login(credentials);
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
    @Timeout(value = 3, unit = ChronoUnit.SECONDS)
    public Response register(@Valid RegisterRequest newUser) {
        try {
            String token = autenticacionService.register(newUser);
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
        } catch (AutenticacionService.ConflictException e) {
            return Response.status(Response.Status.CONFLICT)
                    .entity("El correo ya está registrado.")
                    .build();
        }
    }

    public record RegisterRequest(
            @Size(max = 50) String firstName,
            @NotBlank @Size(max = 50) String lastName,
            @Email @NotBlank String email,
            @Pattern(regexp = "\\d{9,15}") String phone,
            @NotBlank @Size(min = 8) String password) {}
    public record UserCredentials(
            @NotBlank String username,
            @NotBlank String password) {}
}

