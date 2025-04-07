package com.Otros;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.stream.Collectors;

@Provider
public class ValidationExceptionHandler implements ExceptionMapper<ConstraintViolationException> {

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        // Construir la lista de errores
        var errors = exception.getConstraintViolations()
                .stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toList());

        // Enviar respuesta detallada
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(errors)
                .build();
    }
}
