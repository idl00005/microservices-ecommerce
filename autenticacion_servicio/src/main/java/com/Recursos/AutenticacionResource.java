package com.Recursos;

import com.Servicios.AutenticacionService;
import io.quarkus.security.UnauthorizedException;
import jakarta.annotation.PostConstruct;
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
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricUnits;
import org.eclipse.microprofile.metrics.annotation.Counted;
import org.eclipse.microprofile.metrics.annotation.RegistryType;
import org.eclipse.microprofile.metrics.annotation.Timed;

import java.time.temporal.ChronoUnit;
import java.util.Collections;

@Path("/autenticacion")
@ApplicationScoped
public class AutenticacionResource {

    @Inject
    AutenticacionService autenticacionService;

    @Inject
    @RegistryType(type = MetricRegistry.Type.APPLICATION)
    MetricRegistry registry;

    private Counter errorCounter;

    @PostConstruct
    public void init() {
        errorCounter = registry.counter("Aplication_AutenticacionResource_primality_errors_total");
    }

    @POST
    @Path("/login")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Timeout(value = 3, unit = ChronoUnit.SECONDS)
    @Timed(name = "checksTimer", unit = MetricUnits.MILLISECONDS)
    @Counted(name = "performedChecks")
    public Response login(@Valid UserCredentials credentials) {
        try {
            String token = autenticacionService.login(credentials);
            return Response.ok(token).build();
        } catch (NotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        } catch (UnauthorizedException e) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        } catch (Exception e) {
            errorCounter.inc();
            throw e;
        }
    }

    @POST
    @Path("/register")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Timeout(value = 3, unit = ChronoUnit.SECONDS)
    @Timed(name = "checksTimer", unit = MetricUnits.MILLISECONDS)
    @Counted(name = "performedChecks")
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
        } catch (Exception e) {
            errorCounter.inc();
            throw e;
        }
    }

    public record RegisterRequest(
            @NotBlank(message = "El nombre no puede quedar vacío")
            @Size(max = 50, message = "El nombre debe tener un máximo de 50 caracteres")
            String firstName,

            @NotBlank(message = "El apellido no puede quedar vacío")
            @Size(max = 50, message = "El apellido debe tener un máximo de 50 caracteres")
            String lastName,

            @Email(message = "El correo debe tener un formato válido")
            @NotBlank(message = "El email no puede quedar vacío")
            String email,

            @Pattern(regexp = "\\d{9,15}")
            String phone,

            @NotBlank(message = "La contraseña no puede quedar vacía")
            @Size(min = 8, message = "La contraseña no tener una longitud menor a 8 caracteres")
            String password
    ) {}

    public record UserCredentials(
            @NotBlank(message = "El usuario no puede quedar vacío")
            String username,
            @NotBlank
            @NotBlank(message = "La contraseña no puede quedar vacía")
            String password) {}
}

